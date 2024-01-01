#include "AES.h"
#include "ncm.h"
#include "json.hpp"
#include <fstream>
#include <memory>

#ifndef  READ_DATA_BUFFER_SIZE
#define READ_DATA_BUFFER_SIZE 8192
#endif

constexpr uint8_t MAGIC_HEADER[] = {0X43, 0X54, 0X45, 0X4E, 0X46, 0X44, 0X41, 0X4D};
constexpr uint8_t CORE_KEY[] = {
        0x68, 0x7A, 0x48, 0x52, 0x41, 0x6D, 0x73, 0x6F, 0x35, 0x6B, 0x49, 0x6E, 0x62, 0x61, 0x78,
        0x57
};
constexpr uint8_t META_KEY[] = {
        0x23, 0x31, 0x34, 0x6C, 0x6A, 0x6B, 0x5F, 0x21, 0x5C, 0x5D, 0x26, 0x30, 0x55, 0x3C, 0x27,
        0x28
};

std::string base64_decode(const std::string_view in) {
    const static uint8_t TABLE[] = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 62, 0, 0, 0,
            63, 52, 53, 54, 55, 56, 57, 58,
            59, 60, 61, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
            13, 14, 15, 16, 17, 18, 19, 20, 21,
            22, 23, 24, 25, 0, 0, 0, 0, 0, 0, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51
    };

    std::string out;
    out.reserve(in.size() * 3 / 4);

    uint32_t buf{};
    int n{};
    for (const auto c: in) {
        if (c == '=') {
            break;
        }
        buf = buf << 6 | TABLE[c];
        n += 6;
        if (n >= 8) {
            n -= 8;
            out.push_back(static_cast<char>(buf >> n & 0xFF));
        }
    }
    return out;
}

NcmDumpError ncm_dump(std::ifstream &input, std::filesystem::path &outputFolder) {
    if (!input) {
        return NcmDumpError::InvalidInputStream;
    }
    if (!exists(outputFolder) && !create_directories(outputFolder)) {
        return NcmDumpError::InvalidOutputFolder;
    }

    char header[8]{0};
    input.read(header, 8);
    if (memcmp(header, MAGIC_HEADER, 8) != 0) {
        return NcmDumpError::InvalidNcmFileHeader;
    }

    input.seekg(2, std::ios::cur); // skip 2 bytes

    uint8_t lenBuf[4]{0};
    input.read(reinterpret_cast<char *>(lenBuf), 4);
    uint32_t len = (lenBuf[3] << 8 | lenBuf[2]) << 16 | (lenBuf[1] << 8 | lenBuf[0]);

    memset(lenBuf, 0, sizeof(lenBuf)); // reset buffer

    std::vector<uint8_t> _key(len);
    input.read(reinterpret_cast<char *>(_key.data()), len);
    for (size_t i{}; i < len; ++i) {
        _key[i] ^= 0x64;
    }

    // AES-ECB Pkcs7padding
    AES aes(AESKeyLength::AES_128);

    auto keyBox(aes.DecryptECB(_key.data(), _key.size(), CORE_KEY));
    keyBox.erase(keyBox.begin(), keyBox.begin() + 17); // substring `neteasecloudmusic`
    keyBox.resize(256);
    std::fill(std::find(keyBox.begin(), keyBox.end(), '\r'), keyBox.end(), 0); // fill 0
    { // RC4 init
        size_t keylen = strlen(reinterpret_cast<char const *>(keyBox.data()));
        uint8_t ch[256]{0};
        uint8_t j = 0;
        for (size_t i{}; i < 256; ++i) {
            ch[i] = static_cast<uint8_t>(i);
        }
        for (size_t i{}; i < 256; ++i) {
            j = j + ch[i] + keyBox[i % keylen] & 0xFF;
            std::swap(ch[i], ch[j]);
        }
        keyBox = std::vector(ch, ch + 256);
    }

    // read music info
    input.read(reinterpret_cast<char *>(lenBuf), 4);
    len = (lenBuf[3] << 8 | lenBuf[2]) << 16 | (lenBuf[1] << 8 | lenBuf[0]);
    memset(lenBuf, 0, sizeof(lenBuf)); // reset buffer

    std::vector<uint8_t> metadata(len);
    input.read(reinterpret_cast<char *>(metadata.data()), len);
    if (!input) {
        return NcmDumpError::CannotReadMusicInfo;
    }

    for (size_t i{}; i < len; ++i) {
        metadata[i] ^= 0x63;
    }
    std::string meta(metadata.begin(), metadata.end());

    // decrypt music info
    meta = meta.substr(22); // substring `163 key(Don't modify):`
    const std::string decBase64 = base64_decode(meta);
    const auto decMeta(aes.DecryptECB(reinterpret_cast<const unsigned char *>(decBase64.c_str()),
                                      decBase64.length(),
                                      META_KEY));

    std::string metaJsonString(decMeta.begin(), decMeta.end());
    metaJsonString = metaJsonString.substr(6); // substring `music:`
    auto metaJson = nlohmann::json::parse(metaJsonString);
    std::string fileFormat = metaJson["format"];
    std::transform(fileFormat.begin(), fileFormat.end(), fileFormat.begin(),
                   [](const char c) { return std::tolower(c); });
    // make file format to lower case
    if (fileFormat.empty()) {
        return NcmDumpError::UnknownFileFormat;
    }

    // read music cover data
    input.seekg(9, std::ios::cur); // seek crc & gap
    if (!input) {
        return NcmDumpError::CannotReadMusicCover;
    }

    input.read(reinterpret_cast<char *>(lenBuf), 4);
    len = (lenBuf[3] << 8 | lenBuf[2]) << 16 | (lenBuf[1] << 8 | lenBuf[0]);
    memset(lenBuf, 0, sizeof(lenBuf)); // reset buffer
    // std::vector<uint8_t> coverData(len);
    // input.read(reinterpret_cast<char *>(coverData.data()), len);
    input.seekg(len, std::ios::cur); // skip cover data

    std::string musicName = metaJson["musicName"];
    std::string album = metaJson["album"];
    std::vector<std::vector<nlohmann::json>> artist = metaJson["artist"]; // [["Name", uid], ...]
    // uint32_t bitrate = metaJson["bitrate"];
    // uint64_t duration = metaJson["duration"];

    std::string artists;
    for (auto &v: artist) {
        for (auto &jobj: v) {
            if (jobj.is_string()) {
                artists.append(jobj.get<std::string>());
                artists.append(" ");
            }
        }
    }
    // trim end
    if (artists.ends_with(' '))
        artists.erase(artists.end() - 1);

    // combine file name
    std::string fileName = artists + " - " + musicName + "." + fileFormat;
    std::ofstream output(outputFolder / fileName, std::ios::binary | std::ios::ate);

    // check stream valid
    if (!output) {
        return NcmDumpError::CannotSaveOutputFile;
    }
    if (!input) {
        return NcmDumpError::CannotReadMusicData;
    }

    // process music data
    auto buf = std::make_unique<uint8_t[]>(READ_DATA_BUFFER_SIZE);
    // decode data
    while (!input.eof()) {
        input.read(reinterpret_cast<char *>(buf.get()), READ_DATA_BUFFER_SIZE);
        std::streamsize cnt = input.gcount();

        for (size_t i{}; i < cnt; ++i) {
            uint8_t j = (i + 1) & 0xff;
            buf[i] ^= keyBox[(keyBox[j] + keyBox[(keyBox[j] + j) & 0xff]) & 0xff];
        }

        output.write(reinterpret_cast<const char *>(buf.get()), cnt);
        if (!output) {
            return NcmDumpError::CannotSaveOutputFile;
        }
    }

    output.flush();
    output.close();

    return NcmDumpError::Success;
}
