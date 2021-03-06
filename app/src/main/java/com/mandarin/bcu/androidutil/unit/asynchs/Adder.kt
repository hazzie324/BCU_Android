package com.mandarin.bcu.androidutil.unit.asynchs

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mandarin.bcu.R
import com.mandarin.bcu.androidutil.StaticStore
import com.mandarin.bcu.androidutil.adapters.MeasureViewPager
import com.mandarin.bcu.androidutil.fakeandroid.FIBM
import com.mandarin.bcu.androidutil.pack.AImageWriter
import com.mandarin.bcu.androidutil.io.DefferedLoader
import com.mandarin.bcu.androidutil.io.DefineItf
import com.mandarin.bcu.androidutil.io.ErrorLogWriter
import com.mandarin.bcu.androidutil.unit.Definer
import com.mandarin.bcu.androidutil.unit.adapters.UnitListPager
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

class Adder(context: Activity, private val fm : FragmentManager?) : AsyncTask<Void?, String?, Void?>() {
    private val weakReference: WeakReference<Activity> = WeakReference(context)

    private val image = "3"
    private val castle = "4"
    private val bg = "5"
    private val packext = "6"

    override fun onPreExecute() {
        val activity = weakReference.get() ?: return
        val tab = activity.findViewById<TabLayout>(R.id.unittab)
        val pager = activity.findViewById<MeasureViewPager>(R.id.unitpager)
        val search: FloatingActionButton = activity.findViewById(R.id.animsch)
        val schname: TextInputEditText = activity.findViewById(R.id.animschname)
        val layout: TextInputLayout = activity.findViewById(R.id.animschnamel)
        tab.visibility = View.GONE
        pager.visibility = View.GONE
        search.hide()
        schname.visibility = View.GONE
        layout.visibility = View.GONE
    }

    override fun doInBackground(vararg voids: Void?): Void? {
        val activity = weakReference.get() ?: return null

        Definer().define(activity)
        
        publishProgress("1")

        if(!StaticStore.packread && Pack.map.size == 1) {

            checkValidPack()
            handlePack()
            removeIfDifferent()

            try {
                Pack.read()
                StaticStore.packread = true
                DefferedLoader.clearPending("Context", activity)
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorLogWriter.writeLog(e, StaticStore.upload, activity)
            }

            for (path in DefineItf.packPath) {
                val f = File(path)

                val fname = f.name

                if (fname.endsWith(".bcupack")) {
                    if (!checkPack(f)) {
                        val p = findPack(f)

                        val name = f.name.replace(".bcupack", "")

                        val shared = activity.getSharedPreferences(name, Context.MODE_PRIVATE)
                        val ed = shared.edit()

                        ed.putString(name, StaticStore.fileToMD5(f))
                        ed.apply()

                        val resimg = StaticStore.getExternalRes(activity) + "img/$name/"

                        val g = File(resimg)

                        val glit = g.listFiles() ?: continue

                        for (gs in glit) {
                            val pngname = gs.name

                            publishProgress(image, pngname.replace(".png", ""))

                            if (pngname.endsWith(".png")) {
                                val md5 = StaticStore.fileToMD5(gs)

                                StaticStore.encryptPNG(gs.absolutePath, md5, StaticStore.IV, true)

                                ed.putString(gs.absolutePath.replace(".png", ".bcuimg"), md5)
                                ed.apply()
                            }
                        }

                        p ?: continue

                        val bpathList = ArrayList<String>()

                        for (i in p.bg.list) {
                            val img = i.img?.bimg ?: continue

                            val bpath = StaticStore.getExternalRes(activity) + "img/$name/"
                            val bname = findBgName(bpath)

                            val info = extractImage(activity, img, bpath, bname, false)

                            if (info.size != 2)
                                continue

                            val result = info[0] + "\\" + info[1]

                            (i.img.bimg as FIBM).reference = result

                            bpathList.add(result)
                        }

                        for (i in bpathList) {
                            val info = i.split("\\")

                            val bf = File(info[0].replace(".bcuimg", ".png"))

                            if (!bf.exists())
                                continue

                            publishProgress(bg, bf.name.replace(".png", ""))

                            if (info.size != 2)
                                continue

                            StaticStore.encryptPNG(info[0].replace(".bcuimg", ".png"), info[1], StaticStore.IV, true)
                        }

                        val cpathList = ArrayList<String>()

                        for (i in p.cs.list) {
                            val img = i.img ?: continue

                            val cpath = StaticStore.getExternalRes(activity) + "img/$name/"
                            val cname = findCsName(cpath)

                            val info = extractImage(activity, img, cpath, cname, false)

                            if (info.size != 2)
                                continue

                            val result = info[0] + "\\" + info[1]

                            (i.img as FIBM).reference = result

                            cpathList.add(result)
                        }

                        for (i in cpathList) {
                            val info = i.split("\\")

                            val cf = File(info[0].replace(".bcuimg", ".png"))

                            if (!cf.exists())
                                continue

                            publishProgress(castle, cf.name.replace(".png", ""))

                            if (info.size != 2)
                                continue

                            StaticStore.encryptPNG(info[0].replace(".bcuimg", ".png"), info[1], StaticStore.IV, true)
                        }

                        publishProgress(packext, f.name.replace(".bcupack", ""))

                        p.packData(AImageWriter())
                    }
                }
            }

            DefineItf.packPath.clear()

            StaticStore.filterEntityList = BooleanArray(Pack.map.size)
        }

        StaticStore.filterEntityList = BooleanArray(Pack.map.size)

        publishProgress("2")
        return null
    }

    override fun onProgressUpdate(vararg values: String?) {
        val activity = weakReference.get() ?: return
        val ulistst = activity.findViewById<TextView>(R.id.unitinfst)
        when (values[0]) {
            "1" -> ulistst.setText(R.string.main_pack)
            "2" -> {
                val schname: TextInputEditText = activity.findViewById(R.id.animschname)
                val tab = activity.findViewById<TabLayout>(R.id.unittab)
                val pager = activity.findViewById<MeasureViewPager>(R.id.unitpager)

                if(StaticStore.entityname != "") {
                    schname.setText(StaticStore.entityname)
                }

                schname.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        StaticStore.entityname = s.toString()

                        for(i in StaticStore.filterEntityList.indices) {
                            StaticStore.filterEntityList[i] = true
                        }
                    }
                })

                fm ?: return

                pager.removeAllViewsInLayout()
                pager.adapter = UnitListTab(fm)
                pager.offscreenPageLimit = Pack.map.keys.size
                pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tab))

                tab.setupWithViewPager(pager)
            }

            image -> {
                val name = activity.getString(R.string.main_pack_img)+ (values[1] ?: "")

                ulistst.text = name
            }

            bg -> {
                val name = activity.getString(R.string.main_pack_bg) + (values[1] ?: "")

                ulistst.text = name
            }

            castle -> {
                val name = activity.getString(R.string.main_pack_castle) + (values[1] ?: "")

                ulistst.text = name
            }

            packext -> {
                val name = activity.getString(R.string.main_pack_ext)+ (values[1] ?: "")

                ulistst.text = name
            }
        }
    }

    override fun onPostExecute(result: Void?) {
        val activity = weakReference.get()
        super.onPostExecute(result)
        if (activity == null) return
        val tab = activity.findViewById<TabLayout>(R.id.unittab)
        val pager = activity.findViewById<MeasureViewPager>(R.id.unitpager)
        val prog = activity.findViewById<ProgressBar>(R.id.unitinfprog)
        val search: FloatingActionButton = activity.findViewById(R.id.animsch)
        val schname: TextInputEditText = activity.findViewById(R.id.animschname)
        val layout: TextInputLayout = activity.findViewById(R.id.animschnamel)
        val loadt = activity.findViewById<TextView>(R.id.unitinfst)
        loadt.visibility = View.GONE
        if(Pack.map.size != 1) {
            tab.visibility = View.VISIBLE
        }
        pager.visibility = View.VISIBLE
        prog.visibility = View.GONE
        search.show()
        schname.visibility = View.VISIBLE
        layout.visibility = View.VISIBLE
    }

    inner class UnitListTab internal constructor(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val keys: ArrayList<Int>

        init {
            val lit = fm.fragments
            val trans = fm.beginTransaction()

            for(f in lit) {
                trans.remove(f)
            }

            trans.commitAllowingStateLoss()

            keys = getExistingPack()
        }

        override fun getItem(position: Int): Fragment {
            return UnitListPager.newInstance(keys[position], position)
        }

        override fun getCount(): Int {
            return keys.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return if(position == 0) {
                "Default"
            } else {
                val pack = Pack.map[keys[position]]

                if(pack == null) {
                    keys[position].toString()
                }

                val name = pack?.name ?: ""

                if(name.isEmpty()) {
                    keys[position].toString()
                } else {
                    name
                }
            }
        }

        override fun saveState(): Parcelable? {
            return null
        }

        private fun getExistingPack() : ArrayList<Int> {
            val key = Pack.map.keys.toMutableList()
            val res = ArrayList<Int>()

            for(k in key) {
                val p = Pack.map[k] ?: continue

                if(p.us.ulist.list.isNotEmpty()) {
                    res.add(k)
                }
            }

            return res
        }
    }

    private fun checkPack(f: File) : Boolean {
        val ac = weakReference.get() ?: return false

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

    private fun removeIfDifferent() {
        val ac = weakReference.get() ?: return

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

    /**
     * Compare bcupack files with shared preferences data, and remove other pack data automatically
     */
    private fun handlePack() {
        val ac = weakReference.get() ?: return

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
        val ac = weakReference.get() ?: return

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
        val ac = weakReference.get() ?: return

        val invalid = listOf("configuration")

        val packDir = StaticStore.getExternalPack(ac)

        val f = File(packDir)

        if(!f.exists())
            return

        val lit = f.listFiles() ?: return

        for(fs in lit) {
            if(invalid.contains(fs.name.replace(".bcupack","").replace(".bcudata","").toLowerCase(Locale.ROOT))) {
                if(!fs.delete()) {
                    Log.e("Adder", "Failed to delete file "+ f.absolutePath)
                }
            }
        }
    }
}