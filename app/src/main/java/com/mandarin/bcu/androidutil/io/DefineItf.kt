package com.mandarin.bcu.androidutil.io

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.util.Log
import com.mandarin.bcu.androidutil.StaticStore
import com.mandarin.bcu.androidutil.battle.sound.SoundHandler
import com.mandarin.bcu.androidutil.fakeandroid.FIBM
import com.mandarin.bcu.androidutil.music.OggDataSource
import com.mandarin.bcu.androidutil.pack.AACLoader
import com.mandarin.bcu.androidutil.pack.AImageReader
import com.mandarin.bcu.androidutil.pack.AMusicLoader
import common.CommonStatic
import common.CommonStatic.Itf
import common.io.InStream
import common.io.OutStream
import common.pack.Source
import common.system.VImg
import common.system.files.VFile
import common.util.stage.Music
import java.io.*
import java.util.*
import java.util.function.Function

class DefineItf : Itf {
    companion object {
        var dir: String = ""
        val packPath = ArrayList<String>()

        fun check(c: Context) {
            if(dir == "") {
                dir = StaticStore.getExternalPath(c)
            }
        }
    }

    override fun loadAnim(ins: InStream?, r: CommonStatic.ImgReader?): Source.AnimLoader {
        var name = ""

        return if(r != null) {
            name = (r as AImageReader).name

            if(r.isNull) {
                AACLoader(ins, dir, name)
            } else {
                AACLoader(ins, r)
            }
        } else {
            AACLoader(ins, dir, name)
        }
    }

    override fun exit(save: Boolean) {}

    override fun getMusicLength(f: Music?): Long {
        f ?: return -1

        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(OggDataSource(f.data))

        return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
    }

    override fun readBytes(fi: File): InStream? {
        return InStream.getIns(fi)
    }

    override fun route(path: String?): File {
        val realPath = path?.replace("./",dir) ?: ""

        return File(realPath)
    }

    override fun setSE(ind: Int) {
        SoundHandler.setSE(ind)
    }

    override fun getMusicReader(pid: Int, mid: Int): CommonStatic.ImgReader? {
        return AMusicLoader(pid, mid)
    }

    override fun getReader(f: File?): CommonStatic.ImgReader? {
        val path = f?.absolutePath ?: ""

        println(f?.name)

        return when {

            path.endsWith(".bcupack") -> {
                packPath.add(f?.absolutePath ?: "")
                AImageReader(f?.name?.replace(".bcupack", "")?.replace(".bcudata", "")
                        ?: "", true)
            }

            path.endsWith(".bcudata") -> {
                AImageReader(f?.name?.replace(".bcupack", "")?.replace(".bcudata", "")
                        ?: "", false)
            }
            else -> {
                null
            }
        }
    }

    fun init(c: Context) {
        dir = StaticStore.getExternalPath(c)

        CommonStatic.def = this
    }
}