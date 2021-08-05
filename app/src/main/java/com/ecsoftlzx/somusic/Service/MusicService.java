package com.ecsoftlzx.somusic.Service;

import android.content.Context;
import android.widget.ListView;

import com.ecsoftlzx.somusic.Adapter.MusicSearchListViewAdapter;
import com.ecsoftlzx.somusic.Entity.Music;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MusicService {

    public void searchMusic(String keyWord, Integer pageNum, ListView showResultLv, Context context){
        List<Music> musicList = new ArrayList<>();
        String resUrl = "http://music.imwzh.com/api.php?callback=appRes";
        OkHttpClient client = new OkHttpClient();

        FormBody requestBody = new FormBody.Builder()
                .add("types","types")
                .add("count","50")
                .add("source","kugou")
                .add("pages",pageNum.toString())
                .add("name",keyWord)
                .build();

        Request request = new Request.Builder()
                .url(resUrl)
                .post(requestBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String strBody = response.body().string();
                if (!strBody.equals("")){
                    JSONTokener token =  new JSONTokener(strBody);
                    try {
                        JSONArray jsonArray = (JSONArray) token.nextValue();
                        for (int i = 0;i <= jsonArray.length();i++){
                            Music music = new Music();
                            JSONObject object = (JSONObject) jsonArray.get(i);
                            music.setMusicId(object.getString("id"));
                            music.setMusicAlum(object.getString("album"));

                            List<String> authors = new ArrayList<>();
                            JSONArray artist = object.getJSONArray("artist");
                            for (int j = 0;j <= artist.length();j++){
                                authors.add(artist.get(j).toString());
                            }
                            music.setAuthors(authors);
                            music.setLyricId(object.getString("lyric_id"));
                            music.setPicId(object.getString("pic_id"));
                            music.setUrlId(object.getString("url_id"));
                            musicList.add(music);
                        }

                        MusicSearchListViewAdapter adapter = new MusicSearchListViewAdapter(musicList,context);
                        showResultLv.setAdapter(adapter);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
