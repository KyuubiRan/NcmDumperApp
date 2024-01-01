#pragma once

#include <filesystem>

enum class NcmDumpError : uint8_t {
    Success = 0,
    InvalidInputStream,
    InvalidOutputFolder,
    InvalidNcmFileHeader,
    UnknownFileFormat,
    CannotReadMusicInfo,
    CannotReadMusicCover,
    CannotReadMusicData,
    CannotSaveOutputFile,
};

[[nodiscard]] NcmDumpError ncm_dump(std::ifstream &input, std::filesystem::path &outputFolder);
