package com.example.luckychuan.musicplayer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class MusicOpenHelper extends SQLiteOpenHelper {

    //service中保存的播放列表
    private static final String SERVICE_LIST = "create table service_list(" + "musicId integer," + "display_name text," +
            "artist text," + "title text," + "duration integer," + "albumUri text," + "data text," + "album_name text,"+"size integer,"+"album_id integer)";

    private static final String FAVORITE = "create table favorite(" +"_id integer primary key autoincrement," + "musicId integer," + "display_name text," +
            "artist text," + "title text," + "duration integer," + "albumUri text," + "data text," + "album_name text,"+"size integer,"+"album_id integer)";

    private static final String LATEST_PLAY = "create table latest_play("+"_id integer primary key autoincrement,"  + "musicId integer," + "display_name text," +
            "artist text," + "title text," + "duration integer," + "albumUri text," + "data text," + "album_name text,"+"size integer,"+"album_id integer)";

    private static final String PLAY_MOST = "create table play_most("+"_id integer primary key autoincrement," + "musicId integer," + "display_name text," +
            "artist text," + "title text," + "duration integer," + "albumUri text," + "data text," +"count integer,"+ "album_name text,"+"size integer,"+"album_id integer)";


    public MusicOpenHelper(Context context, String name) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SERVICE_LIST);
        db.execSQL(LATEST_PLAY);
        db.execSQL(FAVORITE);
        db.execSQL(PLAY_MOST);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
