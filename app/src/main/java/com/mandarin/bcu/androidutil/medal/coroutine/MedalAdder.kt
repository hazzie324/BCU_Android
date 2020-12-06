package com.mandarin.bcu.androidutil.medal.coroutine

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import com.mandarin.bcu.R
import com.mandarin.bcu.androidutil.Definer
import com.mandarin.bcu.androidutil.StaticStore
import com.mandarin.bcu.androidutil.medal.adapters.MedalListAdapter
import com.mandarin.bcu.androidutil.supports.CoroutineTask
import common.system.files.VFile
import java.lang.ref.WeakReference

class MedalAdder(activity: Activity) : CoroutineTask<String>() {
    private val weakReference: WeakReference<Activity> = WeakReference(activity)

    private val done = "done"

    override fun prepare() {
        val activity = weakReference.get() ?: return
        val listView = activity.findViewById<ListView>(R.id.medallist)
        setDisappear(listView)
    }

    override fun doSomething() {
        val activity = weakReference.get() ?: return

        Definer.define(activity, this::updateProg, this::updateText)

        publishProgress(StaticStore.TEXT, activity.getString(R.string.medal_reading_icon))

        val path = "./org/page/medal/"

        if (StaticStore.medals.isEmpty()) {
            for (i in 0 until StaticStore.medalnumber) {
                val name = "medal_" + number(i) + ".png"

                val medal = VFile.get("$path$name").data.img.bimg()

                if (medal == null) {
                    StaticStore.medals.add(StaticStore.empty(1, 1))
                } else {
                    StaticStore.medals.add(medal as Bitmap)
                }
            }
        }

        publishProgress(done)
    }

    override fun progressUpdate(vararg data: String) {
        val activity = weakReference.get() ?: return
        val mlistst = activity.findViewById<TextView>(R.id.status)
        when (data[0]) {
            StaticStore.TEXT -> mlistst.text = data[1]
            StaticStore.PROG -> {
                val prog = activity.findViewById<ProgressBar>(R.id.prog)

                prog.isIndeterminate = false
                prog.max = 10000
                prog.progress = data[1].toInt()
            }
            done -> {
                val medallist = activity.findViewById<ListView>(R.id.medallist)
                val prog = activity.findViewById<ProgressBar>(R.id.prog)

                prog.isIndeterminate = true

                val width = StaticStore.getScreenWidth(activity)
                val wh: Float = if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 72f else 90f
                val num = (width - StaticStore.dptopx(16f, activity)) / StaticStore.dptopx(wh, activity)
                var line = StaticStore.medalnumber / num
                if (StaticStore.medalnumber % num != 0) line++
                val lines = arrayOfNulls<String>(line)
                val adapter = MedalListAdapter(activity, num, width, wh, lines)
                medallist.adapter = adapter
                medallist.isClickable = false
            }
        }
    }

    override fun finish() {
        val activity = weakReference.get() ?: return
        val listView = activity.findViewById<ListView>(R.id.medallist)
        val prog = activity.findViewById<ProgressBar>(R.id.prog)
        val medalt = activity.findViewById<TextView>(R.id.status)
        setAppear(listView)
        setDisappear(prog, medalt)
    }

    private fun updateText(info: String) {
        val ac = weakReference.get() ?: return

        publishProgress(StaticStore.TEXT, StaticStore.getLoadingText(ac, info))
    }

    private fun updateProg(p: Double) {
        publishProgress(StaticStore.PROG, (p * 10000.0).toInt().toString())
    }

    private fun setDisappear(vararg views: View) {
        for (v in views) {
            v.visibility = View.GONE
        }
    }

    private fun setAppear(vararg views: View) {
        for (v in views) {
            v.visibility = View.VISIBLE
        }
    }

    private fun number(num: Int): String {
        return when (num) {
            in 0..9 -> {
                "00$num"
            }
            in 10..99 -> {
                "0$num"
            }
            else -> {
                num.toString()
            }
        }
    }

}