package com.ecsoftlzx.somusic.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.IOException;

public class MusicPlayService extends Service {
    private MyBinder mBinder = new MyBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public MediaPlayer mMediaPlayer = new MediaPlayer();

    public class MyBinder extends Binder {

        private String musicUrl;
        private Context context;
        public void init(String musicUr, Context context){
            this.musicUrl = musicUr;
            this.context = context;
            initMediaPlayerFile();
        }
        public void playMusic(){

            if (!mMediaPlayer.isPlaying()){
                mMediaPlayer.start();
            }
        }
        public void pauseMusic() {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        }

        public void closeMedia(){
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }
        }

        /**
         * 获取歌曲长度
         **/
        public int getProgress() {

            return mMediaPlayer.getDuration();
        }

        /**
         * 获取播放位置
         */
        public int getPlayPosition() {

            return mMediaPlayer.getCurrentPosition();
        }
        /**
         * 播放指定位置
         */
        public void seekToPosition(int msec) {
            mMediaPlayer.seekTo(msec);
        }

        /**
         * 初始化文件
         */
        private void initMediaPlayerFile(){
            try {
                Uri uri = Uri.parse(musicUrl);

                mMediaPlayer.setDataSource(context,uri);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
