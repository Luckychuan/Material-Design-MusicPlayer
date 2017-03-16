package com.example.luckychuan.musicplayer.model;


import java.io.Serializable;

public class Artist implements Serializable {

    private int id;
    private int numberOfAlbums;
    private int numberOfTracks;

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", numberOfAlbums=" + numberOfAlbums +
                ", numberOfTracks=" + numberOfTracks +
                ", name='" + name + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public int getNumberOfAlbums() {
        return numberOfAlbums;
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public String getName() {
        return name;
    }

    public Artist(int id, int numberOfAlbums, int numberOfTracks, String name) {

        this.id = id;
        this.numberOfAlbums = numberOfAlbums;
        this.numberOfTracks = numberOfTracks;
        this.name = name;
    }

    private String  name;

}
