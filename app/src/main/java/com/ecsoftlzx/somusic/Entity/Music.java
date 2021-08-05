package com.ecsoftlzx.somusic.Entity;

import java.io.Serializable;
import java.util.List;

/**
 * 音乐实体类
 */
public class Music implements Serializable {
    private String musicId;
    private String musicName;
    private String musicAlum;
    private List<String> authors;
    private String lyricId;
    private String picId;
    private String urlId;


    @Override
    public String toString() {
        return "Music{" +
                "musicId='" + musicId + '\'' +
                ", musicName='" + musicName + '\'' +
                ", musicAlum='" + musicAlum + '\'' +
                ", authors=" + authors +
                ", lyricId='" + lyricId + '\'' +
                ", picId='" + picId + '\'' +
                ", urlId='" + urlId + '\'' +
                '}';
    }

    public Music() {
    }
    public String getUrlId() {
        return urlId;
    }

    public void setUrlId(String urlId) {
        this.urlId = urlId;
    }
    public Music(String musicId, String musicName, String musicAlum, List<String> authors, String lyricId, String picId,String urlId) {
        this.musicId = musicId;
        this.musicName = musicName;
        this.musicAlum = musicAlum;
        this.authors = authors;
        this.lyricId = lyricId;
        this.picId = picId;
        this.urlId = urlId;
    }

    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(String musicId) {
        this.musicId = musicId;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getMusicAlum() {
        return musicAlum;
    }

    public void setMusicAlum(String musicAlum) {
        this.musicAlum = musicAlum;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getLyricId() {
        return lyricId;
    }

    public void setLyricId(String lyricId) {
        this.lyricId = lyricId;
    }

    public String getPicId() {
        return picId;
    }

    public void setPicId(String picId) {
        this.picId = picId;
    }
}
