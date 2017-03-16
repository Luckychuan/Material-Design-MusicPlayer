package com.example.luckychuan.musicplayer.model;

import java.io.Serializable;

/**
 * 从android.provider.MediaStore.Audio.Media查询到的音乐
 */
public class MusicInfo implements Serializable {

    private int id;
    private String displayName;
    private String artist;
    private String title;
    private int duration;
    //专辑图片的url
    private String albumUri;
    //文件路径
    private String data;
    private String albumName;
    private long size;
    private int albumId;



    public MusicInfo(int id, String artist, String title, int duration, String albumUri, String data, String displayName,String albumName,long size,int albumId) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.duration = duration;
        this.albumUri = albumUri;
        this.data = data;
        this.displayName = displayName;
        this.albumName = albumName;
        this.albumId =albumId;
        this.size = size;
    }

    public int getAlbumId() {
        return albumId;
    }

    public long getSize() {
        return size;
    }

    public String getAlbumUri() {
        return albumUri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getData() {
        return data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDuration() {
        return duration;
    }



    public int getId() {
        return id;
    }


    public String getArtist() {

        return artist;
    }

    public String getAlbumName(){
        return albumName;
    }

    @Override
    public String toString() {
        return "MusicInfo{" +
                "id=" + id +
                ", displayName='" + displayName + '\'' +
                ", artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", duration=" + duration +
                ", albumUri='" + albumUri + '\'' +
                ", data='" + data + '\'' +
                ", albumName='" + albumName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MusicInfo musicInfo = (MusicInfo) o;

        if (id != musicInfo.id) return false;
        if (duration != musicInfo.duration) return false;
        if (displayName != null ? !displayName.equals(musicInfo.displayName) : musicInfo.displayName != null)
            return false;
        if (artist != null ? !artist.equals(musicInfo.artist) : musicInfo.artist != null)
            return false;
        if (title != null ? !title.equals(musicInfo.title) : musicInfo.title != null) return false;
        if (albumUri != null ? !albumUri.equals(musicInfo.albumUri) : musicInfo.albumUri != null)
            return false;
        if (data != null ? !data.equals(musicInfo.data) : musicInfo.data != null) return false;
        return !(albumName != null ? !albumName.equals(musicInfo.albumName) : musicInfo.albumName != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (artist != null ? artist.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + (albumUri != null ? albumUri.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (albumName != null ? albumName.hashCode() : 0);
        return result;
    }


}
