package com.ecsoftlzx.somusic.Service;

import com.ecsoftlzx.somusic.Entity.Music;

import java.io.Serializable;

public class SearchMusicItemBundleObject implements Serializable {
    private Music music;


    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public SearchMusicItemBundleObject(Music music) {
        this.music = music;
    }

    public Music getMusic() {
        return music;
    }

    public void setMusic(Music music) {
        this.music = music;
    }
}
