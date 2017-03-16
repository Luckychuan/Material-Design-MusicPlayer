package com.example.luckychuan.musicplayer.model;

/**
 * 播放列表
 */
public class PlaylistInfo {

    private int id;
    private String name;
    private int size;
    private String firstMusicAlbumUrl;

    public PlaylistInfo(int id,String name,int size,String firstMusicAlbumUrl ){
        this.id = id;
        this.name = name;
        this.size = size;
        this.firstMusicAlbumUrl = firstMusicAlbumUrl;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String getFirstMusicAlbumUrl() {
        return firstMusicAlbumUrl;
    }

    public void setName(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return "PlaylistInfo{" +

                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
