#include "AES.h"

AES::AES(const AESKeyLength keyLength) {
    switch (keyLength) {
        case AESKeyLength::AES_128:
            this->Nk = 4;
            this->Nr = 10;
            break;
        case AESKeyLength::AES_192:
            this->Nk = 6;
            this->Nr = 12;
            break;
        case AESKeyLength::AES_256:
            this->Nk = 8;
            this->Nr = 14;
            break;
    }
}

std::vector<uint8_t> AES::EncryptECB(const uint8_t in[], size_t inLen,
                                     const uint8_t key[]) {
    CheckLength(inLen);
    auto out = std::vector<uint8_t>(inLen);
    auto roundKeys = std::vector<uint8_t>(4 * Nb * (Nr + 1));
    KeyExpansion(key, roundKeys.data());
    for (size_t i = 0; i < inLen; i += blockBytesLen) {
        EncryptBlock(in + i, out.data() + i, roundKeys.data());
    }

    return out;
}

std::vector<uint8_t> AES::DecryptECB(const uint8_t in[], size_t inLen,
                                     const uint8_t key[]) {
    CheckLength(inLen);
    auto out = std::vector<uint8_t>(inLen);
    auto roundKeys = std::vector<uint8_t>(4 * Nb * (Nr + 1));
    KeyExpansion(key, roundKeys.data());
    for (size_t i = 0; i < inLen; i += blockBytesLen) {
        DecryptBlock(in + i, out.data() + i, roundKeys.data());
    }

    return out;
}

std::vector<uint8_t> AES::EncryptCBC(const uint8_t in[], size_t inLen,
                                     const uint8_t key[],
                                     const uint8_t *iv) {
    CheckLength(inLen);
    auto out = std::vector<uint8_t>(inLen);
    uint8_t block[blockBytesLen];
    auto roundKeys = std::vector<uint8_t>(4 * Nb * (Nr + 1));
    KeyExpansion(key, roundKeys.data());
    memcpy(block, iv, blockBytesLen);
    for (size_t i = 0; i < inLen; i += blockBytesLen) {
        XorBlocks(block, in + i, block, blockBytesLen);
        EncryptBlock(block, out.data() + i, roundKeys.data());
        memcpy(block, out.data() + i, blockBytesLen);
    }

    return out;
}

std::vector<uint8_t> AES::DecryptCBC(const uint8_t in[], size_t inLen,
                                     const uint8_t key[],
                                     const uint8_t *iv) {
    CheckLength(inLen);
    auto out = std::vector<uint8_t>(inLen);
    uint8_t block[blockBytesLen];
    auto roundKeys = std::vector<uint8_t>(4 * Nb * (Nr + 1));
    KeyExpansion(key, roundKeys.data());
    memcpy(block, iv, blockBytesLen);
    for (size_t i = 0; i < inLen; i += blockBytesLen) {
        DecryptBlock(in + i, out.data() + i, roundKeys.data());
        XorBlocks(block, out.data() + i, out.data() + i, blockBytesLen);
        memcpy(block, in + i, blockBytesLen);
    }

    return out;
}

std::vector<uint8_t> AES::EncryptCFB(const uint8_t in[], size_t inLen,
                                     const uint8_t key[],
                                     const uint8_t *iv) {
    CheckLength(inLen);
    auto out = std::vector<uint8_t>(inLen);
    uint8_t block[blockBytesLen];
    auto roundKeys = std::vector<uint8_t>(4 * Nb * (Nr + 1));
    KeyExpansion(key, roundKeys.data());
    memcpy(block, iv, blockBytesLen);
    for (size_t i = 0; i < inLen; i += blockBytesLen) {
        uint8_t encryptedBlock[blockBytesLen];
        EncryptBlock(block, encryptedBlock, roundKeys.data());
        XorBlocks(in + i, encryptedBlock, out.data() + i, blockBytesLen);
        memcpy(block, out.data() + i, blockBytesLen);
    }

    return out;
}

std::vector<uint8_t> AES::DecryptCFB(const uint8_t in[], size_t inLen,
                                     const uint8_t key[],
                                     const uint8_t *iv) {
    CheckLength(inLen);
    auto out = std::vector<uint8_t>(inLen);
    uint8_t block[blockBytesLen];
    auto roundKeys = std::vector<uint8_t>(4 * Nb * (Nr + 1));
    KeyExpansion(key, roundKeys.data());
    memcpy(block, iv, blockBytesLen);
    for (size_t i = 0; i < inLen; i += blockBytesLen) {
        uint8_t encryptedBlock[blockBytesLen];
        EncryptBlock(block, encryptedBlock, roundKeys.data());
        XorBlocks(in + i, encryptedBlock, out.data() + i, blockBytesLen);
        memcpy(block, in + i, blockBytesLen);
    }

    return out;
}

void AES::CheckLength(const size_t len) {
    if (len % blockBytesLen != 0) {
        throw std::length_error("Plaintext length must be divisible by " +
                                std::to_string(blockBytesLen));
    }
}

void AES::EncryptBlock(const uint8_t in[], uint8_t out[], uint8_t *roundKeys) const {
    uint8_t state[4][Nb];
    size_t i, j, round;

    for (i = 0; i < 4; i++) {
        for (j = 0; j < Nb; j++) {
            state[i][j] = in[i + 4 * j];
        }
    }

    AddRoundKey(state, roundKeys);

    for (round = 1; round <= Nr - 1; round++) {
        SubBytes(state);
        ShiftRows(state);
        MixColumns(state);
        AddRoundKey(state, roundKeys + round * 4 * Nb);
    }

    SubBytes(state);
    ShiftRows(state);
    AddRoundKey(state, roundKeys + Nr * 4 * Nb);

    for (i = 0; i < 4; i++) {
        for (j = 0; j < Nb; j++) {
            out[i + 4 * j] = state[i][j];
        }
    }
}

void AES::DecryptBlock(const uint8_t in[], uint8_t out[],
                       uint8_t *roundKeys) const {
    uint8_t state[4][Nb];
    size_t i, j, round;

    for (i = 0; i < 4; i++) {
        for (j = 0; j < Nb; j++) {
            state[i][j] = in[i + 4 * j];
        }
    }

    AddRoundKey(state, roundKeys + Nr * 4 * Nb);

    for (round = Nr - 1; round >= 1; round--) {
        InvSubBytes(state);
        InvShiftRows(state);
        AddRoundKey(state, roundKeys + round * 4 * Nb);
        InvMixColumns(state);
    }

    InvSubBytes(state);
    InvShiftRows(state);
    AddRoundKey(state, roundKeys);

    for (i = 0; i < 4; i++) {
        for (j = 0; j < Nb; j++) {
            out[i + 4 * j] = state[i][j];
        }
    }
}

void AES::SubBytes(uint8_t state[4][Nb]) {
    size_t i, j;
    uint8_t t;
    for (i = 0; i < 4; i++) {
        for (j = 0; j < Nb; j++) {
            t = state[i][j];
            state[i][j] = sbox[t / 16][t % 16];
        }
    }
}

void AES::ShiftRow(uint8_t state[4][Nb], size_t i,
                   size_t n) // shift row i on n positions
{
    uint8_t tmp[Nb];
    for (size_t j = 0; j < Nb; j++) {
        tmp[j] = state[i][(j + n) % Nb];
    }
    memcpy(state[i], tmp, Nb * sizeof(uint8_t));
}

void AES::ShiftRows(uint8_t state[4][Nb]) {
    ShiftRow(state, 1, 1);
    ShiftRow(state, 2, 2);
    ShiftRow(state, 3, 3);
}

uint8_t AES::xtime(uint8_t b) // multiply on x
{
    return (b << 1) ^ (((b >> 7) & 1) * 0x1b);
}

void AES::MixColumns(uint8_t state[4][Nb]) {
    uint8_t temp_state[4][Nb];

    for (auto &i: temp_state) {
        memset(i, 0, 4);
    }

    for (size_t i = 0; i < 4; ++i) {
        for (size_t k = 0; k < 4; ++k) {
            for (size_t j = 0; j < 4; ++j) {
                if (CMDS[i][k] == 1)
                    temp_state[i][j] ^= state[k][j];
                else
                    temp_state[i][j] ^= GF_MUL_TABLE[CMDS[i][k]][state[k][j]];
            }
        }
    }

    for (size_t i = 0; i < 4; ++i) {
        memcpy(state[i], temp_state[i], 4);
    }
}

void AES::AddRoundKey(uint8_t state[4][Nb], const uint8_t *key) {
    size_t i, j;
    for (i = 0; i < 4; i++) {
        for (j = 0; j < Nb; j++) {
            state[i][j] = state[i][j] ^ key[i + 4 * j];
        }
    }
}

void AES::SubWord(uint8_t *a) {
    int i;
    for (i = 0; i < 4; i++) {
        a[i] = sbox[a[i] / 16][a[i] % 16];
    }
}

void AES::RotWord(uint8_t *a) {
    uint8_t c = a[0];
    a[0] = a[1];
    a[1] = a[2];
    a[2] = a[3];
    a[3] = c;
}

void AES::XorWords(const uint8_t *a, const uint8_t *b, uint8_t *c) {
    int i;
    for (i = 0; i < 4; i++) {
        c[i] = a[i] ^ b[i];
    }
}

void AES::Rcon(uint8_t *a, const size_t n) {
    size_t i;
    uint8_t c = 1;
    for (i = 0; i < n - 1; i++) {
        c = xtime(c);
    }

    a[0] = c;
    a[1] = a[2] = a[3] = 0;
}

void AES::KeyExpansion(const uint8_t key[], uint8_t w[]) const {
    uint8_t temp[4];
    uint8_t rcon[4];

    size_t i = 0;
    while (i < 4 * Nk) {
        w[i] = key[i];
        i++;
    }

    i = 4 * Nk;
    while (i < 4 * Nb * (Nr + 1)) {
        temp[0] = w[i - 4 + 0];
        temp[1] = w[i - 4 + 1];
        temp[2] = w[i - 4 + 2];
        temp[3] = w[i - 4 + 3];

        if (i / 4 % Nk == 0) {
            RotWord(temp);
            SubWord(temp);
            Rcon(rcon, i / (Nk * 4));
            XorWords(temp, rcon, temp);
        } else if (Nk > 6 && i / 4 % Nk == 4) {
            SubWord(temp);
        }

        w[i + 0] = w[i - 4 * Nk] ^ temp[0];
        w[i + 1] = w[i + 1 - 4 * Nk] ^ temp[1];
        w[i + 2] = w[i + 2 - 4 * Nk] ^ temp[2];
        w[i + 3] = w[i + 3 - 4 * Nk] ^ temp[3];
        i += 4;
    }
}

void AES::InvSubBytes(uint8_t state[4][Nb]) {
    size_t i, j;
    uint8_t t;
    for (i = 0; i < 4; i++) {
        for (j = 0; j < Nb; j++) {
            t = state[i][j];
            state[i][j] = inv_sbox[t / 16][t % 16];
        }
    }
}

void AES::InvMixColumns(uint8_t state[4][Nb]) {
    uint8_t temp_state[4][Nb];

    for (auto &i: temp_state) {
        memset(i, 0, 4);
    }

    for (size_t i = 0; i < 4; ++i) {
        for (size_t k = 0; k < 4; ++k) {
            for (size_t j = 0; j < 4; ++j) {
                temp_state[i][j] ^= GF_MUL_TABLE[INV_CMDS[i][k]][state[k][j]];
            }
        }
    }

    for (size_t i = 0; i < 4; ++i) {
        memcpy(state[i], temp_state[i], 4);
    }
}

void AES::InvShiftRows(uint8_t state[4][Nb]) {
    ShiftRow(state, 1, Nb - 1);
    ShiftRow(state, 2, Nb - 2);
    ShiftRow(state, 3, Nb - 3);
}

void AES::XorBlocks(const uint8_t *a, const uint8_t *b,
                    uint8_t *c, const size_t len) {
    for (size_t i = 0; i < len; i++) {
        c[i] = a[i] ^ b[i];
    }
}

void AES::printHexArray(uint8_t a[], const size_t n) {
    for (size_t i = 0; i < n; i++) {
        printf("%02x ", a[i]);
    }
}

void AES::printHexVector(const std::vector<uint8_t> &a) {
    for (const auto i: a) {
        printf("%02x ", i);
    }
}

std::vector<uint8_t> AES::ArrayToVector(uint8_t *a,
                                        const size_t len) {
    std::vector v(a, a + len * sizeof(uint8_t));
    return v;
}

uint8_t* AES::VectorToArray(std::vector<uint8_t> &a) {
    return a.data();
}

std::vector<uint8_t> AES::EncryptECB(std::vector<uint8_t> in,
                                     std::vector<uint8_t> key) {
    return EncryptECB(VectorToArray(in), in.size(), VectorToArray(key));
}

std::vector<uint8_t> AES::DecryptECB(std::vector<uint8_t> in,
                                     std::vector<uint8_t> key) {
    return DecryptECB(VectorToArray(in), in.size(), VectorToArray(key));
}

std::vector<uint8_t> AES::EncryptCBC(std::vector<uint8_t> in,
                                     std::vector<uint8_t> key,
                                     std::vector<uint8_t> iv) {
    return EncryptCBC(VectorToArray(in), in.size(), VectorToArray(key), VectorToArray(iv));
}

std::vector<uint8_t> AES::DecryptCBC(std::vector<uint8_t> in,
                                     std::vector<uint8_t> key,
                                     std::vector<uint8_t> iv) {
    return DecryptCBC(VectorToArray(in), in.size(), VectorToArray(key), VectorToArray(iv));
}

std::vector<uint8_t> AES::EncryptCFB(std::vector<uint8_t> in,
                                     std::vector<uint8_t> key,
                                     std::vector<uint8_t> iv) {
    return EncryptCFB(VectorToArray(in), in.size(), VectorToArray(key), VectorToArray(iv));
}

std::vector<uint8_t> AES::DecryptCFB(std::vector<uint8_t> in,
                                     std::vector<uint8_t> key,
                                     std::vector<uint8_t> iv) {
    return DecryptCFB(VectorToArray(in), in.size(), VectorToArray(key), VectorToArray(iv));
}
