package com.mandarin.bcu.androidutil.pack.asynchs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import com.mandarin.bcu.MainActivity
import com.mandarin.bcu.PackConflictSolve
import com.mandarin.bcu.R
import com.mandarin.bcu.androidutil.StaticStore
import com.mandarin.bcu.androidutil.fakeandroid.FIBM
import com.mandarin.bcu.androidutil.pack.AImageWriter
import com.mandarin.bcu.androidutil.io.DefferedLoader
import com.mandarin.bcu.androidutil.io.DefineItf
import com.mandarin.bcu.androidutil.io.ErrorLogWriter
import com.mandarin.bcu.androidutil.pack.PackConflict
import common.system.fake.FakeImage
import common.util.Data
import common.util.pack.Pack
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.ref.WeakReference
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.ArrayList

class PackExtract(ac: Activity, private val config: Boolean) : AsyncTask<Void, String, Void>() {
    private val pack = "1"
    private val image = "2"
    private val castle = "3"
    private val bg = "4"
    private val packext = "5"

    private val errInvlaid = "100"

    private var paused = false
    private var destroy = false

    private var stopper = Object()

    val a = WeakReference(ac)

    override fun doInBackground(vararg params: Void?): Void? {
        if(!config) {
            val ac = a.get() ?: return null

            publishProgress(pack)

            checkValidPack()
            handlePack()
            removeIfDifferent()

            synchronized(stopper) {
                while(paused) {
                    try {
                        stopper.wait()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        break
                    }
                }
            }

            try {
                if(!StaticStore.packread && Pack.map.size == 1) {
                    Pack.read()
                    StaticStore.packread = true
                }
                DefferedLoader.clearPending("Context", ac)
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorLogWriter.writeLog(e, StaticStore.upload, ac)
            }

            for(path in DefineItf.packPath) {
                val f = File(path)

                val fname = f.name

                if(fname.endsWith(".bcupack")) {
                    if(!checkPack(f)) {
                        val p = findPack(f)

                        val name = f.name.replace(".bcupack", "")

                        val shared = ac.getSharedPreferences(name, Context.MODE_PRIVATE)
                        val ed = shared.edit()

                        ed.putString(name, StaticStore.fileToMD5(f))
                        ed.apply()

                        val resimg = StaticStore.getExternalRes(ac) + "img/$name/"

                        val g = File(resimg)

                        val glit = g.listFiles() ?: continue

                        for (gs in glit) {
                            val pngname = gs.name

                            publishProgress(image, pngname.replace(".png",""))

                            if (pngname.endsWith(".png")) {
                                val md5 = StaticStore.fileToMD5(gs)

                                StaticStore.encryptPNG(gs.absolutePath, md5, StaticStore.IV, true)

                                ed.putString(gs.absolutePath.replace(".png",".bcuimg"), md5)
                                ed.apply()                            }
                        }

                        p ?: continue

                        val bpathList = ArrayList<String>()

                        for(i in p.bg.list) {
                            val img = i.img?.bimg ?: continue

                            val bpath = StaticStore.getExternalRes(ac) + "img/$name/"
                            val bname = findBgName(bpath)

                            val info = extractImage(ac, img, bpath, bname, false)

                            if(info.size != 2)
                                continue

                            val result = info[0]+"\\"+info[1]

                            (i.img.bimg as FIBM).reference = result

                            bpathList.add(result)
                        }

                        for(i in bpathList) {
                            val info = i.split("\\")

                            val bf = File(info[0].replace(".bcuimg", ".png"))

                            if(!bf.exists())
                                continue

                            publishProgress(bg, bf.name.replace(".png", ""))

                            if(info.size != 2)
                                continue

                            StaticStore.encryptPNG(info[0].replace(".bcuimg",".png"), info[1], StaticStore.IV, true)
                        }

                        val cpathList = ArrayList<String>()

                        for(i in p.cs.list) {
                            val img = i.img ?: continue

                            val cpath = StaticStore.getExternalRes(ac) + "img/$name/"
                            val cname = findCsName(cpath)

                            val info = extractImage(ac, img, cpath, cname, false)

                            if(info.size != 2)
                                continue

                            val result = info[0] + "\\" + info[1]

                            (i.img as FIBM).reference = result

                            cpathList.add(result)
                        }

                        for(i in cpathList) {
                            val info = i.split("\\")

                            val cf = File(info[0].replace(".bcuimg",".png"))

                            if(!cf.exists())
                                continue

                            publishProgress(castle, cf.name.replace(".png", ""))

                            if(info.size != 2)
                                continue

                            StaticStore.encryptPNG(info[0].replace(".bcuimg",".png"), info[1], StaticStore.IV, true)
                        }

                        publishProgress(packext, f.name.replace(".bcupack",""))

                        p.packData(AImageWriter())
                    }
                }
            }
        }

        DefineItf.packPath.clear()

        return null
    }

    override fun onProgressUpdate(vararg values: String?) {
        val ac = a.get() ?: return

        val text = ac.findViewById<TextView>(R.id.mainstup) ?: return

        when(values[0]) {
            pack -> {
                text.text = ac.getString(R.string.main_pack)
            }

            image -> {
                val name = ac.getString(R.string.main_pack_img)+ (values[1] ?: "")

                text.text = name
            }

            bg -> {
                val name = ac.getString(R.string.main_pack_bg) + (values[1] ?: "")

                text.text = name
            }

            castle -> {
                val name = ac.getString(R.string.main_pack_castle) + (values[1] ?: "")

                text.text = name
            }

            packext -> {
                val name = ac.getString(R.string.main_pack_ext)+ (values[1] ?: "")

                text.text = name
            }

            errInvlaid -> {
                paused = true

                if(values.size < 3) {
                    return
                }

                val builder = AlertDialog.Builder(ac)
                val inflator = LayoutInflater.from(ac)
                val v = inflator.inflate(R.layout.checkbox_dialog, null)

                builder.setView(v)
                builder.setCancelable(false)

                val content = v.findViewById<TextView>(R.id.checkdialogcont)
                val check = v.findViewById<CheckBox>(R.id.checkdialogcheck)
                val conf = v.findViewById<Button>(R.id.checkdialogconf)

                val cont = ac.getString(R.string.pack_invalid_content).replace("_",values[1] ?: "")

                content.text = cont

                var removePack = false

                check.setOnCheckedChangeListener { _: CompoundButton, c: Boolean ->
                    removePack = c
                    println(removePack)
                }

                val dialog = builder.create()
                dialog.show()

                conf.setOnClickListener {
                    if(removePack) {
                        val path = values[2]

                        if(path == null) {
                            dialog.dismiss()
                            return@setOnClickListener
                        }

                        val f = File(path)

                        if(f.exists()) {
                            if(!f.delete()) {
                                Log.e("PackExtract", "Cannot delete file "+f.absolutePath)
                                destroy = true
                                ac.finish()
                            }
                        }

                        synchronized(stopper) {
                            paused = false
                            stopper.notifyAll()
                        }

                        dialog.dismiss()
                    } else {
                        dialog.dismiss()
                        ac.finish()
                        destroy = true
                    }
                }
            }
        }
    }

    override fun onPostExecute(result: Void?) {
        val activity = a.get() ?: return

        StaticStore.filterEntityList = BooleanArray(Pack.map.size)

        if(PackConflict.conflicts.isEmpty()) {
            if (!MainActivity.isRunning && !destroy) {
                val intent = Intent(activity, MainActivity::class.java)
                intent.putExtra("config", config)
                activity.startActivity(intent)
                activity.finish()
            }
        } else {
            val intent = Intent(activity, PackConflictSolve::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
    }

    private fun findBgName(path: String) : String {
        var i = 0

        while(true) {
            val name = "bg-"+ Data.trio(i)+".png"

            val f = File(path, name)

            if(f.exists()) {
                i++
            } else {
                println(name)
                return name
            }
        }
    }

    private fun findCsName(path: String) : String {
        var i = 0

        while(true) {
            val name = "castle-"+ Data.trio(i)+".png"

            val f = File(path, name)

            if(f.exists()) {
                i++
            } else {
                return name
            }
        }
    }

    private fun checkPack(f: File) : Boolean {
        val ac = a.get() ?: return false

        val name = f.name.replace(".bcupack","").replace(".bcudata","")

        val shared = ac.getSharedPreferences(name, Context.MODE_PRIVATE)

        return if(shared.contains(name)) {
            val md5 = shared.getString(name, "")

            val fmd5 = StaticStore.fileToMD5(f)

            val g = File(StaticStore.getExternalRes(ac)+"data/"+f.name.replace(".bcupack",".bcudata"))

            md5 == fmd5 && g.exists()
        } else {
            false
        }
    }

    private fun removeIfDifferent() {
        val ac = a.get() ?: return

        val path = File(StaticStore.getExternalPack(ac))

        val lit = path.listFiles() ?: return

        for(f in lit) {
            if(!f.name.endsWith(".bcupack"))
                continue

            val name = f.name.replace(".bcupack", "").replace(".bcuata", "")

            val shared = ac.getSharedPreferences(name, Context.MODE_PRIVATE)

            if (shared.contains(name)) {
                val omd5 = shared.getString(name, "")

                val cmd5 = StaticStore.fileToMD5(f)

                if (omd5 != cmd5) {
                    val g = File(StaticStore.getExternalRes(ac) + "data/" + f.name.replace(".bcupack", ".bcudata"))

                    if (g.exists()) {
                        if (!g.delete()) {
                            Log.e("PackExtract", "Failed to remove file " + g.absolutePath)
                        }
                    }
                }
            }
        }
    }

    private fun findPack(f: File) : Pack? {
        val path = f.absolutePath

        for(p in Pack.map) {
            if(p.value.id == 0)
                continue

            val ppath = p.value.file?.absolutePath ?: ""

            if(ppath == path)
                return p.value
        }

        return null
    }

    /**
     * Compare bcupack files with shared preferences data, and remove other pack data automatically
     */
    private fun handlePack() {
        val ac = a.get() ?: return

        val sharedDir = StaticStore.getDataPath()+"shared_prefs/"

        val f = File(sharedDir)

        if(!f.exists())
            return

        val lit = f.listFiles() ?: return

        val handler = listOf<String>().toMutableList()

        for(fs in lit) {
            if(fs.name == "configuration.xml")
                continue

            val name = fs.name.replace(".xml",".bcupack")

            val g = File(StaticStore.getExternalPack(ac), name)

            if(!g.exists()) {
                handler.add(name.replace(".bcupack",""))
            }
        }

        for(name in handler) {
            removeRelatedPackData(name)
        }
    }

    /**
     * Remove all pack data with specified name
     *
     * @param name Name of pack file, must not contain extension
     */
    private fun removeRelatedPackData(name: String) {
        val ac = a.get() ?: return

        if(name == "configuration")
            return

        val sharedPath = StaticStore.getDataPath()+"shared_prefs/$name.xml"

        var f = File(sharedPath)

        if(f.exists()) {
            if(!f.delete()) {
                Log.e("PackExtract","Failed to remove file "+f.absolutePath)
            }
        }

        val resDataPath = StaticStore.getExternalRes(ac)+"data/$name.bcudata"

        f = File(resDataPath)

        if(f.exists()) {
            if(!f.delete()) {
                Log.e("PackExtract", "Failed to remove file "+f.absolutePath)
            }
        }

        val resImgPath = StaticStore.getExternalRes(ac)+"img/$name/"

        f = File(resImgPath)

        StaticStore.removeAllFiles(f)
    }

    /**
     * Check if there are invalid bcupack files, so it won't affect application data shared preferences
     */
    private fun checkValidPack() {
        val ac = a.get() ?: return

        val invalid = listOf("configuration")

        val packDir = StaticStore.getExternalPack(ac)

        val f = File(packDir)

        if(!f.exists())
            return

        val lit = f.listFiles() ?: return

        for(fs in lit) {
            if(invalid.contains(fs.name.replace(".bcupack","").replace(".bcudata","").toLowerCase(Locale.ROOT))) {
                paused = true
                publishProgress(errInvlaid, fs.name, fs.absolutePath)
            }
        }
    }

    private fun extractImage(c: Context, img: FakeImage, path: String, name: String, unload: Boolean) : Array<String> {
        val f = File(path)
        val result = arrayOf("", "")

        if(!f.exists()) {
            if(!f.mkdirs()) {
                Log.e("PackExtract", "Failed to create directory "+f.absolutePath)
                return result
            }
        }

        val g = File(path, name)

        if(!g.exists()) {
            if(!g.createNewFile()) {
                Log.e("PackExtract", "Failed to create file "+g.absolutePath)
                return result
            }
        }

        img.bimg() ?: return result

        (img.bimg() as Bitmap).compress(Bitmap.CompressFormat.PNG, 0, FileOutputStream(g))

        if(unload) {
            img.unload()
        }

        return try {
            arrayOf(g.absolutePath.replace(".png",".bcuimg"), StaticStore.fileToMD5(g))
        } catch (e: NoSuchAlgorithmException) {
            ErrorLogWriter.writeLog(e, StaticStore.upload, c)
            arrayOf(g.absolutePath.replace(".png", ".bcuimg"),"")
        }
    }
}