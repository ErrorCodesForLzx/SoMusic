package com.ecsoftlzx.somusic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.transition.Visibility;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ecsoftlzx.somusic.Entity.Music;
import com.ecsoftlzx.somusic.Service.InternetFileGetService;
import com.ecsoftlzx.somusic.Service.MusicPlayService;
import com.ecsoftlzx.somusic.Service.SearchMusicItemBundleObject;
import com.ecsoftlzx.somusic.Util.FastBlur;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MusicDetailActivity extends AppCompatActivity {

    private Music music;
    private ServiceConnection serviceConnection;
    private MusicPlayService.MyBinder mBinder;
    private Handler mHandlerTime = new Handler();
    private Runnable mRunnableTime;
    SimpleDateFormat timeFormatter = new SimpleDateFormat("mm:ss");
    private String musicUrl;
    private String musicPicUrl;
    private String musicLyric;
    private int thisPlayStatus = 2;
    private static final int PLAY_STATUS_PLAY = 1;
    private static final int PLAY_STATUS_PAUSE = 2;

    private ImageView ivMusicDetailBack;
    private TextView tvMusicTitle;
    private TextView tvMusicAuthor;
    private ImageView ivMusicDetailDownLoad;
    private TextView tvMusicDetailLyric;
    private ImageView ivMusicPic;
    private SeekBar sbMusicPlayProgress;
    private ImageView ivMusicPlayControl;
    private TextView tvMusicPlayThisProgress;
    private TextView tvMusicPlayAllProgress;
    private Intent MediaServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_detail);

        Intent intent = getIntent();
        SearchMusicItemBundleObject bundleObject = (SearchMusicItemBundleObject) intent.getSerializableExtra("data");
        music = bundleObject.getMusic();

        initView(savedInstanceState);
        MediaServiceIntent = new Intent(this, MusicPlayService.class);

        if (ContextCompat.checkSelfPermission(MusicDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MusicDetailActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        } else {
            //?????????????????????????????????
            bindService(MediaServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        }


        setEvent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bindService(MediaServiceIntent, serviceConnection, BIND_AUTO_CREATE);
                } else {
                    Toast.makeText(this, "????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private void initView(Bundle bundle) {
        initFirst();

        ivMusicDetailBack = findViewById(R.id.iv_music_detail_back);
        tvMusicDetailLyric = findViewById(R.id.tv_music_detail_lyric);
        ivMusicPic = findViewById(R.id.iv_music_detail_icon);
        ivMusicDetailDownLoad = findViewById(R.id.iv_music_detail_download);
        tvMusicTitle = findViewById(R.id.iv_music_detail_title);
        tvMusicAuthor = findViewById(R.id.iv_music_detail_author);
        sbMusicPlayProgress = findViewById(R.id.sb_music_play_progress);
        tvMusicPlayAllProgress = findViewById(R.id.tv_music_play_max_progress);
        tvMusicPlayThisProgress = findViewById(R.id.tv_music_play_thisProgress);
        ivMusicPlayControl = findViewById(R.id.iv_music_play_control);
        tvMusicTitle.setText(music.getMusicName());


        StringBuilder authorStr = new StringBuilder();
        for (String authorItem:music.getAuthors()) {
            authorStr.append(authorItem).append(",");
        }
        tvMusicAuthor.setText(authorStr.substring(0,authorStr.length()-1));

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBinder = (MusicPlayService.MyBinder) service;
                mRunnableTime = new Runnable() {
                    @Override
                    public void run() {
                        sbMusicPlayProgress.setProgress(mBinder.getPlayPosition());
                        tvMusicPlayThisProgress.setText(timeFormatter.format(new Date(mBinder.getPlayPosition())));

                        mHandlerTime.postDelayed(mRunnableTime,500);
                    }
                };

                mHandlerTime.post(mRunnableTime);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };


        // last to execute
        initMusicInfo(bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandlerTime.removeCallbacks(mRunnableTime);
        mBinder.closeMedia();
        unbindService(serviceConnection);
    }

    private void initMusicInfo(Bundle bundle) {
        String requestUrl = "http://music.imwzh.com/api.php?callback=appRes";

        OkHttpClient client = new OkHttpClient();

        FormBody lyricForm = new FormBody.Builder()
                .add("types","lyric")
                .add("id",music.getLyricId())
                .add("source","netease")
                .build();
        FormBody picForm = new FormBody.Builder()
                .add("types","pic")
                .add("id",music.getPicId())
                .add("source","netease")
                .build();
        FormBody urlForm = new FormBody.Builder()
                .add("types","url")
                .add("id",music.getUrlId())
                .add("source","netease")
                .build();

        Request getLyricRequest = new Request.Builder()
                .url(requestUrl)
                .post(lyricForm)
                .build();
        Request getPicRequest = new Request.Builder()
                .url(requestUrl)
                .post(picForm)
                .build();
        Request getUrlRequest = new Request.Builder()
                .url(requestUrl)
                .post(urlForm)
                .build();

        Call lyricCall = client.newCall(getLyricRequest);
        Call picCall = client.newCall(getPicRequest);
        Call urlCall = client.newCall(getUrlRequest);

        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == 0x0001) {
                    tvMusicDetailLyric.setText(musicLyric);
                } else if (msg.what == 0x0002) {
                    if (!musicPicUrl.equals("")){
                        Log.e("??????3333",musicPicUrl);
                        Glide.with(MusicDetailActivity.this).load(musicPicUrl).into(ivMusicPic);
                        RelativeLayout rootView = findViewById(R.id.rl_music_detail_root);

//                        Glide.with(MusicDetailActivity.this)
//                                .load(musicPicUrl)
//                                .into(new CustomTarget<Drawable>() {
//                                    @Override
//                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
//                                        // Do something with the Drawable here
//                                        rootView.setBackground(resource);
//                                    }
//
//                                    @Override
//                                    public void onLoadCleared(@Nullable Drawable placeholder) {
//                                        // ????????????????????????onResourceReady????????????Drawable???????????????????????????????????????
//                                    }
//                                });

                        Resources res = getResources();
                        Drawable dr = res.getDrawable(R.drawable.pic_music_detail_default);
                        dr.setTintMode(PorterDuff.Mode.OVERLAY);
                        findViewById(R.id.rl_music_detail_root).setBackground(dr);

                        applyBlur();

                    } else {
                        ivMusicPic.setImageResource(R.drawable.pic_no_pic);
                        Resources res = getResources();

                        Drawable dr = res.getDrawable(R.drawable.pic_music_detail_default);
                        dr.setTintMode(PorterDuff.Mode.OVERLAY);
                        findViewById(R.id.rl_music_detail_root).setBackground(dr);
                        applyBlur();
                    }

                } else if (msg.what == 0x1002) {
                    AlertDialog dialog = new AlertDialog.Builder(MusicDetailActivity.this)
                            .setTitle("?????????")
                            .setMessage("????????????????????????????????????????????????????????????\n?????????????????????????????????")
                            .setCancelable(false)
                            .setNegativeButton("??????????????????", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .show();
                } else if (msg.what == 0x0003) {
                    sbMusicPlayProgress.setMax(mBinder.getProgress());
                    tvMusicPlayAllProgress.setText(timeFormatter.format(new Date(mBinder.getProgress())));
                }
                return true;
            }
        });

        lyricCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                synchronized (handler) {
                    String lyricStr = Objects.requireNonNull(response.body()).string();
                    StringBuilder stringBuilder = new StringBuilder(lyricStr);
                    lyricStr = stringBuilder.substring(7,stringBuilder.length()-1);

                    if (!lyricStr.equals("")){
                        JSONTokener token = new JSONTokener(lyricStr);
                        musicLyric = "";
                        try {
                            JSONObject json = (JSONObject) token.nextValue();
                            Log.e("??????2222",lyricStr);
                            musicLyric = json.getString("lyric");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Message msg = new Message();
                        msg.what = 0x0001;
                        Bundle data = new Bundle();
                        data.putString("musicLyric",musicLyric);
                        msg.setData(data);
                        handler.sendMessage(msg);
                    } else {
                        Message msg = new Message();
                        msg.what = 0x0001;
                        Bundle data = new Bundle();
                        data.putString("musicLyric","[00:00:00]?????????????????????...");
                        msg.setData(data);
                        handler.sendMessage(msg);
                    }
                }

            }
        });

        picCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                synchronized (handler) {
                    String jsonStr = Objects.requireNonNull(response.body()).string();
                    StringBuilder sb = new StringBuilder(jsonStr);
                    jsonStr = sb.substring(7,sb.length()-1);


                    JSONTokener token = new JSONTokener(jsonStr);
                    try {
                        Log.e("??????2222",jsonStr);
                        JSONObject json = (JSONObject) token.nextValue();
                        musicPicUrl = json.getString("url");
                        Message msg = new Message();
                        msg.what = 0x0002;
                        handler.sendMessage(msg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        urlCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                synchronized (handler) {
                    String jsonStr = Objects.requireNonNull(response.body()).string();
                    StringBuilder sb = new StringBuilder(jsonStr);
                    jsonStr = sb.substring(7,sb.length()-1);
                    JSONTokener token = new JSONTokener(jsonStr);
                    try {
                        JSONObject json = (JSONObject) token.nextValue();
                        musicUrl = json.getString("url");

                        if (json.getInt("br") == -1){
                            Message msg = new Message();
                            msg.what = 0x1002;
                            handler.sendMessage(msg);
                            return;
                        }



                        while (mBinder == null){

                        }
                        //?????????????????????
                        mBinder.init(musicUrl,MusicDetailActivity.this);

                        Message msg = new Message();
                        msg.what = 0x0003;
                        handler.sendMessage(msg);

                        // ??????????????????
                        FileOutputStream fosMusicWrite = openFileOutput("musicTmpData", MODE_PRIVATE);
                        // ??????????????????
                        InputStream gotFileData = InternetFileGetService.getInternetFile(musicUrl, 10 * 1000);
                        // ???????????????
                        byte[] dataBuffer = new byte[1024];
                        int thisReadiedDataLen = 0;
                        // ??????????????????
                        while ((thisReadiedDataLen = gotFileData.read(dataBuffer)) != -1) {
                            fosMusicWrite.write(dataBuffer,0,thisReadiedDataLen);
                        }
                        // ?????????
                        fosMusicWrite.close();
                        gotFileData.close();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    private void setEvent() {
        ivMusicDetailBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ivMusicPlayControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thisPlayStatus == PLAY_STATUS_PAUSE) {
                    //????????????
                    ivMusicPlayControl.setImageResource(R.drawable.icon_suspend);
                    thisPlayStatus = PLAY_STATUS_PLAY;
                    mBinder.playMusic();
                } else if (thisPlayStatus == PLAY_STATUS_PLAY) {
                    // ????????????
                    ivMusicPlayControl.setImageResource(R.drawable.icon_play);
                    thisPlayStatus = PLAY_STATUS_PAUSE;
                    mBinder.pauseMusic();

                }
            }
        });

        sbMusicPlayProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    mBinder.seekToPosition(seekBar.getProgress());
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ivMusicDetailDownLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(MusicDetailActivity.this)
                        .setTitle("????????????")
                        .setMessage("??????????????????????????????????????????")
                        .setPositiveButton("????????????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent startWebBrowser = new Intent();
                                startWebBrowser.setAction(Intent.ACTION_VIEW);
                                startWebBrowser.setData(Uri.parse(musicUrl));
                                startActivity(startWebBrowser);
                                Toast.makeText(MusicDetailActivity.this, "????????????????????????????????????...", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setCancelable(false)
                        .show();

            }
        });
    }

    private void initFirst(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);


        //5.0 ???????????????
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        RelativeLayout rootView = findViewById(R.id.rl_music_detail_root);
        rootView.setPadding(0,getStatusBarHeight(MusicDetailActivity.this)+5,0,0);

        // ???????????????

    }

    private void applyBlur() {
        RelativeLayout view = findViewById(R.id.rl_music_detail_root);
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        /*
         * ??????????????????????????????????????????
         */

        Resources res      = getResources();
        Drawable drawable = view.getBackground();//??????drawable
        BitmapDrawable bd = (BitmapDrawable) drawable;
        Bitmap bmp1         = bd.getBitmap();

        BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getBackground();
//        Bitmap bmp1 = bitmapDrawable.getBitmap();

//        final Bitmap bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        int height = getOtherHeight();
        /*
         * ???????????????????????????
         */
        Bitmap bmp2 = Bitmap.createBitmap(bmp1, 0, height,bmp1.getWidth(), bmp1.getHeight() - height);
        blur(bmp2, findViewById(R.id.rl_music_detail_root));
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????
     * @return
     */
    private int getOtherHeight() {
        Rect frame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        int contentTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight = contentTop - statusBarHeight;
        return statusBarHeight + titleBarHeight;
    }

    @SuppressLint("NewApi")
    private void blur(Bitmap bkg, View view) {
        long startMs = System.currentTimeMillis();
        float scaleFactor = 8;//?????????????????????
        float radius = 20;//????????????

        Bitmap overlay = Bitmap.createBitmap(
                (int) (view.getMeasuredWidth() / scaleFactor),
                (int) (view.getMeasuredHeight() / scaleFactor),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.translate(-view.getLeft() / scaleFactor, -view.getTop()/ scaleFactor);
        canvas.scale(1 / scaleFactor, 1 / scaleFactor);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bkg, 0, 0, paint);


        overlay = FastBlur.doBlur(overlay, (int) radius, true);
        view.setBackground(new BitmapDrawable(getResources(), overlay));
        /**
         * ???????????????????????????????????????????????????16ms???????????????????????????????????????????????????????????????????????????????????????????????????????????????scaleFactor??????????????????
         */
//        Log.i("jerome", "blur time:" + (System.currentTimeMillis() - startMs));
    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}