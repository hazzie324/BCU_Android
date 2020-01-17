package com.mandarin.bcu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import android.view.View;
import android.widget.Button;

import com.mandarin.bcu.androidutil.Revalidater;
import com.mandarin.bcu.androidutil.StaticStore;
import com.mandarin.bcu.androidutil.adapters.SingleClick;
import com.mandarin.bcu.androidutil.io.ErrorLogWriter;

public class MainActivity extends AppCompatActivity {
    public static boolean isRunning = false;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences shared = getSharedPreferences("configuration",MODE_PRIVATE);
        SharedPreferences.Editor ed = shared.edit();
        if(!shared.contains("initial")) {
            ed.putBoolean("initial",true);
            ed.putBoolean("theme",true);
            ed.putBoolean("frame",true);
            ed.putBoolean("apktest",false);
            ed.putInt("default_level",50);
            ed.putInt("Language",0);
            ed.apply();
        } else {
            if(!shared.getBoolean("theme",false)) {
                setTheme(R.style.AppTheme_night);
            } else {
                setTheme(R.style.AppTheme_day);
            }
        }

        if(shared.getInt("Orientation",0) == 1)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        else if(shared.getInt("Orientation",0) == 2)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        else if(shared.getInt("Orientation",0) == 0)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        Thread.setDefaultUncaughtExceptionHandler(new ErrorLogWriter(StaticStore.LOGPATH));

        setContentView(R.layout.activity_main);

        isRunning = true;

        Button animbtn = findViewById(R.id.anvibtn);
        Button stagebtn = findViewById(R.id.stgbtn);
        Button emlistbtn = findViewById(R.id.eninfbtn);
        Button basisbtn = findViewById(R.id.basisbtn);
        Button medalbtn = findViewById(R.id.medalbtn);
        Button bgbtn = findViewById(R.id.bgbtn);

        FloatingActionButton config = findViewById(R.id.mainconfig);

        animbtn.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(this,R.drawable.ic_kasa_jizo), null, null, null);
        stagebtn.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(this,R.drawable.ic_castle),null,null,null);
        emlistbtn.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(this,R.drawable.ic_enemy),null,null,null);
        emlistbtn.setCompoundDrawablePadding(StaticStore.dptopx(16f,this));
        basisbtn.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(this,R.drawable.ic_basis),null,null,null);
        medalbtn.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(this,R.drawable.ic_medal),null,null,null);
        bgbtn.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(this,R.drawable.ic_bg),null,null,null);
        bgbtn.setCompoundDrawablePadding(StaticStore.dptopx(16f,this));

        animbtn.setOnClickListener(new SingleClick() {
            @Override
            public void onSingleClick(View v) {
                animationview();
            }
        });

        stagebtn.setOnClickListener(new SingleClick() {
            @Override
            public void onSingleClick(View v) {
                stageinfoview();
            }
        });

        config.setOnClickListener(new SingleClick() {
            @Override
            public void onSingleClick(View v) {
                gotoconfig();
            }
        });

        emlistbtn.setOnClickListener(new SingleClick() {
            @Override
            public void onSingleClick(View v) {
                gotoenemyinf();
            }
        });

        basisbtn.setOnClickListener(new SingleClick() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(MainActivity.this,LineUpScreen.class);
                startActivity(intent);
            }
        });

        medalbtn.setOnClickListener(new SingleClick() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(MainActivity.this, MedalList.class);
                startActivity(intent);
            }
        });

        bgbtn.setOnClickListener(new SingleClick() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(MainActivity.this,BackgroundList.class);
                startActivity(intent);
            }
        });
    }

    protected void animationview() {
        Intent intent = new Intent(this, AnimationViewer.class);
        startActivity(intent);
    }

    protected void stageinfoview()
    {
        Intent intent = new Intent(this, MapList.class);
        startActivity(intent);
    }

    protected void gotoconfig() {
        Intent intent = new Intent(this,ConfigScreen.class);
        startActivity(intent);
        finish();
    }

    protected void gotoenemyinf() {
        Intent intent = new Intent(this,EnemyList.class);
        startActivity(intent);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences shared = newBase.getSharedPreferences("configuration",Context.MODE_PRIVATE);
        super.attachBaseContext(Revalidater.LangChange(newBase,shared.getInt("Language",0)));
    }

    @Override
    public void onBackPressed() {
        isRunning = false;
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }
}
