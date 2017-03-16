package com.example.luckychuan.musicplayer.model;

import java.io.Serializable;


public class Album  implements Serializable{

    private int id;
    private String artist;
    private String albumName;
    private String albumUri;
    private int numberOfSongs;

    public Album(int id, String albumName, int numberOfSongs,String artist,String albumUri) {
        this.id = id;
        this.albumName = albumName;
        this.numberOfSongs = numberOfSongs;
        this.artist = artist;
        this.albumUri =albumUri;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAlbumUri() {
        return albumUri;
    }

    public String getAlbumName() {
        return albumName;
    }

    public int getNumberOfSongs() {
        return numberOfSongs;
    }


    public String getArtist() {
        return artist;
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", albumName='" + albumName + '\'' +
                ", numberOfSongs=" + numberOfSongs +
                '}';
    }
}
