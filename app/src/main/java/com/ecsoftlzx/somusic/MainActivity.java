package com.ecsoftlzx.somusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.ecsoftlzx.somusic.Adapter.MusicSearchListViewAdapter;
import com.ecsoftlzx.somusic.Entity.Music;
import com.ecsoftlzx.somusic.Service.SearchMusicItemBundleObject;
import com.ecsoftlzx.somusic.Service.SearchMusicListBundleObject;
import com.ecsoftlzx.somusic.Util.FastBlur;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ListView lvMusicList;
    private EditText etMusicName;
    private Button btnMusicSearch;
    private ImageView ivUserInfo;
    private ProgressDialog pgSearchLoading;
    private boolean firstRun = true;
    private List<Music> musicList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initView();
        setEvent();

        //背景模糊
        View rootView = findViewById(R.id.main_activity_root);
        rootView.setPadding(0,getStatusBarHeight(MainActivity.this)+5,0,0);
        rootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                if (firstRun){
                    applyBlur();
                }
                firstRun = false;

                return true;
            }
        });


    }


    private void initView() {
        initFirst();

        lvMusicList = findViewById(R.id.lv_music_list);
        etMusicName = findViewById(R.id.et_music_input);
        btnMusicSearch = findViewById(R.id.btn_music_search);
        ivUserInfo = findViewById(R.id.iv_user_info);

    }
    private void setEvent() {
        btnMusicSearch.setOnClickListener(v -> {
            final View vee = getWindow().peekDecorView();
            if (vee != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(vee.getWindowToken(), 0);
            }


            String searchMusicName = etMusicName.getText().toString();
            if (!searchMusicName.equals("")){

                pgSearchLoading = new ProgressDialog(MainActivity.this);
                pgSearchLoading.setMessage("正在音乐星球搜索中...");
                pgSearchLoading.setCancelable(false);
                pgSearchLoading.show();

                musicList = new ArrayList<>();
                String resUrl = "http://music.imwzh.com/api.php?callback=appRes";



                OkHttpClient client = new OkHttpClient();

                FormBody requestBody = new FormBody.Builder()
                        .add("types","search")
                        .add("count","50")
                        .add("source","netease")
                        .add("pages","1")
                        .add("name",searchMusicName)
                        .build();

                Request request = new Request.Builder()
                        .url(resUrl)
                        .post(requestBody)
                        .build();
                Call call = client.newCall(request);

                Handler handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(@NonNull Message msg) {
                        /*

                            0x1001 访问失败What
                         */
                        if (msg.what == 0x0001){
                            MusicSearchListViewAdapter adapter = new MusicSearchListViewAdapter(musicList,MainActivity.this);
                            lvMusicList.setAdapter(adapter);
                            pgSearchLoading.cancel();
                        } else if (msg.what == 0x1001) {
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                    .setIcon(R.drawable.icon_return)
                                    .setTitle("访问失败")
                                    .setMessage("对不起，服务器开小差了，请稍后重试...")
                                    .setPositiveButton("知道了",null)
                                    .show();
                        }
                        return true;
                    }
                });

                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        pgSearchLoading.cancel();

                        Message msg = new Message();
                        msg.what = 0x1001;
                        handler.sendMessage(msg);
                        Log.e("测试","访问失败！！！！");

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String strBody = Objects.requireNonNull(response.body()).string();
                        StringBuilder stringBuilder = new StringBuilder(strBody);
                        strBody = stringBuilder.substring(7,stringBuilder.length()-1);
                        if (!strBody.equals("")){
                            JSONTokener token =  new JSONTokener(strBody);
                            try {
                                JSONArray jsonArray = (JSONArray) token.nextValue();
                                for (int i = 0;i < jsonArray.length();i++){
                                    Music music = new Music();
                                    JSONObject object = (JSONObject) jsonArray.get(i);
                                    music.setMusicId(object.getString("id"));
                                    music.setMusicName(object.getString("name"));
                                    music.setMusicAlum(object.getString("album"));

                                    List<String> authors = new ArrayList<>();
                                    JSONArray artist = object.getJSONArray("artist");
                                    for (int j = 0;j < artist.length();j++){
                                        authors.add(artist.get(j).toString());
                                    }
                                    music.setAuthors(authors);
                                    music.setLyricId(object.getString("lyric_id"));
                                    music.setPicId(object.getString("pic_id"));
                                    music.setUrlId(object.getString("url_id"));
                                    musicList.add(music);
                                    Log.e("测试",music.toString());
                                }

                                Bundle data = new Bundle();
                                data.putSerializable("data",new SearchMusicListBundleObject(musicList));
                                Message msg = new Message();
                                msg.setData(data);
                                msg.what = 0x0001;
                                handler.sendMessage(msg);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                });
            } else {
                AlertDialog builder = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("提示")
                        .setMessage("请输入想搜索的音乐名称！\n比如：打上花火...")
                        .setPositiveButton("确定",null)
                        .setCancelable(false)
                        .show();
            }
        });

        lvMusicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchMusicItemBundleObject itemBundleObject = new SearchMusicItemBundleObject(musicList.get(position));


                Intent intent = new Intent(MainActivity.this,MusicDetailActivity.class);
                intent.putExtra("data",itemBundleObject);
                startActivity(intent);
            }
        });

        ivUserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,UserInfoActivity.class);
                startActivity(intent);
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


        //5.0 全透明实现
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
    }


    private void applyBlur() {
        LinearLayout view = findViewById(R.id.main_activity_root);
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        /*
         * 获取当前窗口快照，相当于截屏
         */

        Resources res      = getResources();
        Drawable drawable = res.getDrawable(R.drawable.bg_pic);//获取drawable
        BitmapDrawable bd = (BitmapDrawable) drawable;
        Bitmap bmp1         = bd.getBitmap();

        BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getBackground();
//        Bitmap bmp1 = bitmapDrawable.getBitmap();

//        final Bitmap bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        int height = getOtherHeight();
        /*
         * 除去状态栏和标题栏
         */
        Bitmap bmp2 = Bitmap.createBitmap(bmp1, 0, height,bmp1.getWidth(), bmp1.getHeight() - height);
        blur(bmp2, findViewById(R.id.main_activity_root));
    }

    @SuppressLint("NewApi")
    private void blur(Bitmap bkg, View view) {
        long startMs = System.currentTimeMillis();
        float scaleFactor = 8;//图片缩放比例；
        float radius = 20;//模糊程度

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
         * 打印高斯模糊处理时间，如果时间大约16ms，用户就能感到到卡顿，时间越长卡顿越明显，如果对模糊完图片要求不高，可是将scaleFactor设置大一些。
         */
//        Log.i("jerome", "blur time:" + (System.currentTimeMillis() - startMs));
    }

    /**
     * 获取系统状态栏和软件标题栏，部分软件没有标题栏，看自己软件的配置；
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

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}