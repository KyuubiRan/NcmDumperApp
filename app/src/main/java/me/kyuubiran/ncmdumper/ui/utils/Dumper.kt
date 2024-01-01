package me.kyuubiran.ncmdumper.ui.utils

object Dumper {
    @JvmStatic
    external fun dumpNcmFile(inFilePath: String, outFilePath: String): Int

    const val DEFAULT_NETEASE_MUSIC_PATH = "/storage/emulated/0/netease/cloudmusic/Music"
}

