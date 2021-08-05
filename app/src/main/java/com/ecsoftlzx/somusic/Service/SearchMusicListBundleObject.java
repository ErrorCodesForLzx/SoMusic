package com.ecsoftlzx.somusic.Service;

import com.ecsoftlzx.somusic.Entity.Music;

import java.io.Serializable;
import java.util.List;

public class SearchMusicListBundleObject implements Serializable {
    List<Music> musicList;

    public SearchMusicListBundleObject(List<Music> musicList) {
        this.musicList = musicList;
    }

    public List<Music> getMusicList() {
        return musicList;
    }

    public void setMusicList(List<Music> musicList) {
        this.musicList = musicList;
    }
}
