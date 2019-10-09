package com.mandarin.bcu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.mandarin.bcu.androidutil.Revalidater;
import com.mandarin.bcu.androidutil.StaticStore;
import com.mandarin.bcu.androidutil.animation.AnimationCView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import common.system.fake.FakeImage;
import common.system.files.VFile;
import common.util.anim.ImgCut;

public class ImageViewer extends AppCompatActivity {
    private final int BG = 0;
    private final int CASTLE = 1;
    private final int ANIMU = 2;
    private final int ANIME = 3;

    private final int [] animS = {R.string.anim_move,R.string.anim_wait,R.string.anim_atk,R.string.anim_kb,R.string.anim_burrow,R.string.anim_under,R.string.anim_burrowup};

    private String path  = null;
    private int img = -1;
    private int bgnum = -1;

    private int id = -1;
    private int form = -1;

    float preX = 0;
    float preY = 0;
    int preid = -1;

    Toast toast;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences shared = getSharedPreferences("configuration",MODE_PRIVATE);
        SharedPreferences.Editor ed;
        if(!shared.contains("initial")) {
            ed = shared.edit();
            ed.putBoolean("initial",true);
            ed.putBoolean("theme",true);
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

        setContentView(R.layout.activity_image_viewer);

        Intent result = getIntent();

        if(result.getExtras() != null) {
            Bundle extra = result.getExtras();

            path = extra.getString("Path");
            img = extra.getInt("Img");
            bgnum = extra.getInt("BGNum");

            id = extra.getInt("ID");
            form = extra.getInt("Form");
        }

        ImageButton bck = findViewById(R.id.imgviewerbck);

        TableRow row = findViewById(R.id.palyrow);
        SeekBar seekBar = findViewById(R.id.animframeseek);
        TextView frame = findViewById(R.id.animframe);
        FloatingActionButton [] buttons = {findViewById(R.id.animbackward),findViewById(R.id.animplay),findViewById(R.id.animforward)};

        bck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StaticStore.play = true;
                StaticStore.frame = 0;
                StaticStore.animposition = 0;
                StaticStore.formposition = 0;
                finish();
            }
        });

        Spinner anims = findViewById(R.id.animselect);

        switch(img) {
            case BG:
                row.setVisibility(View.GONE);
                seekBar.setVisibility(View.GONE);
                frame.setVisibility(View.GONE);
                anims.setVisibility(View.GONE);

                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                int width = size.x;
                int height = size.y;

                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                paint.setFilterBitmap(true);
                paint.setAntiAlias(true);
                paint.setDither(true);

                Bitmap b = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(b);

                if(getImgcut() == 1 || getImgcut() == 8) {
                    Bitmap b1 = getImg(b.getHeight(),2);
                    Bitmap b2 = getImg2(b.getHeight(),2);

                    int h = b1.getHeight();
                    int w = b1.getWidth();

                    for (int i = 0; i < 1 + width / w; i++) {
                        canvas.drawBitmap(b2, w * i, height - 2 * h, paint);
                        canvas.drawBitmap(b1, w * i, height - h, paint);
                    }
                } else {
                    Bitmap b1 = getImg(b.getHeight(),2);

                    int h = b1.getHeight();
                    int w = b1.getWidth();

                    for (int i = 0; i < 1 + width / w; i++) {
                        canvas.drawBitmap(b1, w * i, height - h, paint);
                    }

                    Shader shader = new LinearGradient(0,0,0,height-h,getSkyUpper(),getSkyBelow(), Shader.TileMode.CLAMP);
                    paint.setShader(shader);
                    canvas.drawRect(new RectF(0,0,width,height-h),paint);
                }

                ImageView img = findViewById(R.id.imgviewerimg);

                img.setImageBitmap(b);

                break;
            case CASTLE:
                anims.setVisibility(View.GONE);
                row.setVisibility(View.GONE);
                seekBar.setVisibility(View.GONE);
                frame.setVisibility(View.GONE);

                Bitmap b2 = (Bitmap) Objects.requireNonNull(VFile.getFile(path)).getData().getImg().bimg();

                BitmapDrawable bd = new BitmapDrawable(getResources(),b2);

                bd.setFilterBitmap(true);
                bd.setAntiAlias(true);

                img = findViewById(R.id.imgviewerimg);

                ConstraintLayout constraintLayout = findViewById(R.id.imglayout);
                Toolbar toolbar = findViewById(R.id.toolbar7);

                img.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                ConstraintSet set = new ConstraintSet();

                set.clone(constraintLayout);
                set.connect(img.getId(),ConstraintSet.TOP,toolbar.getId(),ConstraintSet.BOTTOM,4);
                set.connect(img.getId(),ConstraintSet.BOTTOM,constraintLayout.getId(),ConstraintSet.BOTTOM,4);
                set.connect(img.getId(),ConstraintSet.LEFT,constraintLayout.getId(),ConstraintSet.LEFT,4);
                set.connect(img.getId(),ConstraintSet.RIGHT,constraintLayout.getId(),ConstraintSet.RIGHT,4);
                set.applyTo(constraintLayout);

                Bitmap castle = StaticStore.getResizeb(bd.getBitmap(),this,bd.getBitmap().getWidth(),bd.getBitmap().getHeight());

                img.setImageBitmap(castle);

                break;
            case ANIMU:
                if(StaticStore.play) {
                    buttons[0].hide();
                    buttons[2].hide();
                    seekBar.setEnabled(false);
                } else {
                    buttons[1].setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));
                }
                img = findViewById(R.id.imgviewerimg);

                img.setVisibility(View.GONE);

                LinearLayout linearLayout = findViewById(R.id.imgviewerln);

                AnimationCView cView;

                List<String> name = new ArrayList<>();

                for(int i = 0; i < StaticStore.units.get(id).forms[0].anim.anims.length; i++) {
                    name.add(getString(animS[i]));
                }

                List<String> ids = new ArrayList<>();

                for(int i = 0; i < StaticStore.units.get(id).forms.length; i++) {
                    ids.add(id+"-"+i);
                }

                Spinner forms = findViewById(R.id.formselect);

                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this,R.layout.spinneradapter,ids);

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,R.layout.spinneradapter,name);

                anims.setAdapter(adapter);

                forms.setAdapter(adapter1);

                cView = new AnimationCView(this,id,StaticStore.formposition,0,!shared.getBoolean("theme",false),shared.getBoolean("Axis",true),shared.getBoolean("FPS",true),frame,seekBar);

                int px = StaticStore.dptopx(1f,this);

                cView.siz = (float)px/1.25f;

                ScaleGestureDetector detector = new ScaleGestureDetector(this,new ScaleListener(cView));

                cView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        detector.onTouchEvent(event);

                        if(preid == -1)
                            preid = event.getPointerId(0);

                        int id = event.getPointerId(0);

                        float x = event.getX();
                        float y = event.getY();

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_MOVE:
                                if(event.getPointerCount() == 1 && id == preid) {
                                    float dx = x - preX;
                                    float dy = y - preY;

                                    cView.x += dx;
                                    cView.y += dy;
                                }
                        }

                        preX = x;
                        preY = y;

                        preid = id;

                        return true;
                    }
                });

                cView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                linearLayout.addView(cView);

                anims.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(StaticStore.animposition != position) {
                            StaticStore.animposition = position;
                            cView.anim.changeAnim(position);
                            seekBar.setMax(cView.anim.len());
                            seekBar.setProgress(0);
                            StaticStore.frame = 0;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                forms.setSelection(form);

                forms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long ids) {
                        if(StaticStore.formposition != position) {
                            StaticStore.formposition = position;
                            cView.anim = StaticStore.units.get(id).forms[position].getEAnim(anims.getSelectedItemPosition());
                            seekBar.setMax(cView.anim.len());
                            seekBar.setProgress(0);
                            StaticStore.frame = 0;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                buttons[1].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        frame.setTextColor(getAttributeColor(ImageViewer.this,R.attr.TextPrimary));

                        if(StaticStore.play) {
                            buttons[1].setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));
                            buttons[0].show();
                            buttons[2].show();
                            seekBar.setEnabled(true);
                        } else {
                            buttons[1].setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_24dp));
                            buttons[0].hide();
                            buttons[2].hide();
                            seekBar.setEnabled(false);
                        }

                        StaticStore.play = !StaticStore.play;
                    }
                });

                buttons[0].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(StaticStore.frame > 0) {
                            StaticStore.frame--;
                            cView.anim.setTime(StaticStore.frame);
                        } else {
                            frame.setTextColor(Color.rgb(227, 66, 66));

                            toast = Toast.makeText(ImageViewer.this,R.string.anim_warn_frame,Toast.LENGTH_SHORT);

                            if(toast.getView().isShown())
                                toast.show();
                        }
                    }
                });

                buttons[2].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StaticStore.frame++;
                        cView.anim.setTime(StaticStore.frame);
                        frame.setTextColor(getAttributeColor(ImageViewer.this,R.attr.TextPrimary));
                    }
                });

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(fromUser) {
                            StaticStore.frame = progress;
                            cView.anim.setTime((int) StaticStore.frame);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                frame.setText(getString(R.string.anim_frame).replace("-",""+StaticStore.frame));
                seekBar.setProgress(StaticStore.frame);
                anims.setSelection(StaticStore.animposition);
                forms.setSelection(StaticStore.formposition);
                cView.anim.changeAnim(StaticStore.animposition);
                cView.anim.setTime(StaticStore.frame);
                seekBar.setMax(cView.anim.len());

                break;
            case ANIME:
                if(StaticStore.play) {
                    buttons[0].hide();
                    buttons[2].hide();
                    seekBar.setEnabled(false);
                } else {
                    buttons[1].setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));
                }

                frame.setText(getString(R.string.anim_frame).replace("-",""+StaticStore.frame));
                seekBar.setProgress(StaticStore.frame);

                img = findViewById(R.id.imgviewerimg);

                img.setVisibility(View.GONE);

                forms = findViewById(R.id.formselect);

                forms.setVisibility(View.GONE);

                linearLayout = findViewById(R.id.imgviewerln);

                cView = new AnimationCView(this,id,0,!shared.getBoolean("theme",false),shared.getBoolean("Axis",true),shared.getBoolean("FPS",true),frame,seekBar);

                px = StaticStore.dptopx(1f,this);

                cView.siz = (float)px/1.25f;

                detector = new ScaleGestureDetector(this,new ScaleListener(cView));

                cView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        detector.onTouchEvent(event);

                        if(preid == -1)
                            preid = event.getPointerId(0);

                        int id = event.getPointerId(0);

                        float x = event.getX();
                        float y = event.getY();

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_MOVE:
                                if(event.getPointerCount() == 1 && id == preid) {
                                    float dx = x - preX;
                                    float dy = y - preY;

                                    cView.x += dx;
                                    cView.y += dy;
                                }
                        }

                        preX = x;
                        preY = y;

                        preid = id;

                        return true;
                    }
                });

                name = new ArrayList<>();

                for(int i = 0; i < StaticStore.enemies.get(id).anim.anims.length; i++) {
                    name.add(getString(animS[i]));
                }

                adapter = new ArrayAdapter<>(this,R.layout.spinneradapter,name);

                anims.setAdapter(adapter);

                cView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                linearLayout.addView(cView);

                anims.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(StaticStore.animposition != position) {
                            StaticStore.animposition = position;
                            cView.anim.changeAnim(position);
                            seekBar.setMax(cView.anim.len());
                            seekBar.setProgress(0);
                            StaticStore.frame = 0;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                buttons[1].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        frame.setTextColor(getAttributeColor(ImageViewer.this,R.attr.TextPrimary));

                        if(StaticStore.play) {
                            buttons[1].setImageDrawable(getDrawable(R.drawable.ic_pause_black_24dp));
                            buttons[0].show();
                            buttons[2].show();
                            seekBar.setEnabled(true);
                        } else {
                            buttons[1].setImageDrawable(getDrawable(R.drawable.ic_play_arrow_black_24dp));
                            buttons[0].hide();
                            buttons[2].hide();
                            seekBar.setEnabled(false);
                        }

                        StaticStore.play = !StaticStore.play;
                    }
                });

                buttons[0].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(StaticStore.frame > 0) {
                            StaticStore.frame--;
                            cView.anim.setTime(StaticStore.frame);
                        } else {
                            frame.setTextColor(Color.rgb(227, 66, 66));

                            toast = Toast.makeText(ImageViewer.this,R.string.anim_warn_frame,Toast.LENGTH_SHORT);

                            if(toast.getView().isShown())
                                toast.show();
                        }
                    }
                });

                buttons[2].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StaticStore.frame++;
                        cView.anim.setTime(StaticStore.frame);
                        frame.setTextColor(getAttributeColor(ImageViewer.this,R.attr.TextPrimary));
                    }
                });

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(fromUser) {
                            StaticStore.frame = progress;
                            cView.anim.setTime((int) StaticStore.frame);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                
                frame.setText(getString(R.string.anim_frame).replace("-",""+StaticStore.frame));
                seekBar.setProgress(StaticStore.frame);
                anims.setSelection(StaticStore.animposition);
                cView.anim.changeAnim(StaticStore.animposition);
                cView.anim.setTime(StaticStore.frame);
                seekBar.setMax(cView.anim.len());

                break;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private final AnimationCView cView;

        ScaleListener(AnimationCView view) {
            this.cView = view;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            cView.siz *= detector.getScaleFactor();

            return true;
        }
    }

    private String [] getData() {
        String datapath = "./org/battle/bg/bg.csv";
        Queue<String> qs = Objects.requireNonNull(VFile.getFile(datapath)).getData().readLine();

        for(String s : qs) {
            String [] data = s.trim().split(",");

            try {
                if (Integer.parseInt(data[0]) == bgnum)
                    return data;
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @NonNull
    private Bitmap getImg(int height, float param) {
        String[] data = getData();

        if (data == null) return StaticStore.empty(this,100f,100f);

        String imgPath;

        if(Integer.parseInt(data[13]) == 8)
            imgPath = "./org/battle/bg/bg0"+1+".imgcut";
        else
            imgPath = "./org/battle/bg/bg0"+data[13]+".imgcut";

        ImgCut img = ImgCut.newIns(imgPath);

        File f = new File(path);

        try {
            FakeImage png = FakeImage.read(f);
            FakeImage[] imgs = img.cut(png);
            Bitmap b = (Bitmap)imgs[0].bimg();

            float ratio = (height/param)/b.getHeight();

            return StaticStore.getResizebp(b,this,ratio*b.getWidth(),ratio*b.getHeight());
        } catch (IOException e) {
            e.printStackTrace();

            return StaticStore.empty(this,100f,100f);
        }
    }

    @NonNull
    private Bitmap getImg2(int height, float param) {
        String[] data = getData();

        if (data == null) return StaticStore.empty(this,100f,100f);

        String imgPath;

        if(Integer.parseInt(data[13]) == 8)
            imgPath = "./org/battle/bg/bg0"+1+".imgcut";
        else
            imgPath = "./org/battle/bg/bg0"+data[13]+".imgcut";

        ImgCut img = ImgCut.newIns(imgPath);

        File f = new File(path);

        try {
            FakeImage png = FakeImage.read(f);
            FakeImage[] imgs = img.cut(png);
            Bitmap b = (Bitmap)imgs[20].bimg();

            float ratio = (height/param)/b.getHeight();

            return StaticStore.getResizebp(b,this,ratio*b.getWidth(),ratio*b.getHeight());
        } catch (IOException e) {
            e.printStackTrace();

            return StaticStore.empty(this,100f,100f);
        }
    }

    private int getImgcut() {
        String [] data = getData();

        if(data == null) return -1;

        return Integer.parseInt(data[13]);
    }

    private int getSkyUpper() {
        String [] data = getData();

        if(data == null) return 0;

        int R = Integer.parseInt(data[1]);
        int G = Integer.parseInt(data[2]);
        int B = Integer.parseInt(data[3]);

        return Color.rgb(R,G,B);
    }

    private int getSkyBelow() {
        String [] data = getData();

        if(data == null) return 0;

        int R = Integer.parseInt(data[4]);
        int G = Integer.parseInt(data[5]);
        int B = Integer.parseInt(data[6]);

        return Color.rgb(R,G,B);
    }

    private int getGroundUpper() {
        String [] data = getData();

        if(data == null) return 0;

        int R = Integer.parseInt(data[7]);
        int G = Integer.parseInt(data[8]);
        int B = Integer.parseInt(data[9]);

        return Color.rgb(R,G,B);
    }

    private int getGroundBelow() {
        String [] data = getData();

        if(data == null) return 0;

        int R = Integer.parseInt(data[10]);
        int G = Integer.parseInt(data[11]);
        int B = Integer.parseInt(data[12]);

        return Color.rgb(R,G,B);
    }

    @Override
    public void onBackPressed() {
        ImageButton bck = findViewById(R.id.imgviewerbck);

        bck.performClick();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences shared = newBase.getSharedPreferences("configuration",Context.MODE_PRIVATE);
        super.attachBaseContext(Revalidater.LangChange(newBase,shared.getInt("Language",0)));
    }

    private static int getAttributeColor(Context context, int attributeId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, true);
        int colorRes = typedValue.resourceId;
        int color = -1;
        try {
            color = ContextCompat.getColor(context,colorRes);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return color;
    }
}