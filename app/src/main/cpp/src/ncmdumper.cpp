#include <jni.h>
#include <string>
#include <fstream>
#include "ncm.h"

extern "C" {
JNIEXPORT jint JNICALL
Java_me_kyuubiran_ncmdumper_ui_utils_Dumper_dumpNcmFile(JNIEnv *env, jclass clazz,
                                                        jstring in_file_path,
                                                        jstring out_file_path) {

    std::string inPath;
    std::string outPath;

    jboolean isCopy = false;

    inPath = env->GetStringUTFChars(in_file_path, &isCopy);
    outPath = env->GetStringUTFChars(out_file_path, &isCopy);

    std::ifstream ifs(inPath, std::ios::in | std::ios::binary);
    std::filesystem::path out(outPath);
    NcmDumpError err = ncm_dump(ifs, out);

    return static_cast<jint>(err);
}
}
