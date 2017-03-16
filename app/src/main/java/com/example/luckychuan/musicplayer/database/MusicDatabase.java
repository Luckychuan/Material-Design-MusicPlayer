package com.example.luckychuan.musicplayer.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.example.luckychuan.musicplayer.model.MusicInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite数据库
 * 最近播放，最常播放，我的最爱，service的音乐音乐
 */

public class MusicDatabase {

    private static final String TAG = "MusicDatabase";
    private static MusicDatabase mDatabase;
    private MusicOpenHelper mHelper;

    MusicDatabase(Context context) {
        mHelper = new MusicOpenHelper(context, "music");
    }

    public synchronized static MusicDatabase getInstance(Context context) {
        if (mDatabase == null) {
            mDatabase = new MusicDatabase(context);
        }
        return mDatabase;
    }

    /**
     * 更新我的最爱表
     *
     * @param music
     */
    public void insertFavorite(MusicInfo music) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.execSQL("insert into favorite(musicId,display_name,artist,title,duration,albumUri,data,album_name,size,album_id) values(?,?,?,?,?,?,?,?,?,?)", new Object[]{music.getId(),
                music.getDisplayName(), music.getArtist(), music.getTitle(), music.getDuration(), music.getAlbumUri().toString(), music.getData(), music.getAlbumName(),music.getSize(),music.getAlbumId()});
        db.close();
    }

    /**
     * 批量添加
     *
     * @param table
     * @param list
     */
    public void insertListMusic(String table, List<MusicInfo> list) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();
        SQLiteStatement statement = db.compileStatement("insert into " + table + "(musicId,display_name,artist,title,duration,albumUri,data,album_name,size,album_id) values(?,?,?,?,?,?,?,?,?,?)");
        for (MusicInfo music : list
                ) {
            statement.bindString(1, music.getId() + "");
            statement.bindString(2, music.getDisplayName());
            statement.bindString(3, music.getArtist());
            statement.bindString(4, music.getTitle());
            statement.bindString(5, music.getDuration() + "");
            statement.bindString(6, music.getAlbumUri());
            statement.bindString(7, music.getData());
            statement.bindString(8, music.getAlbumName());
            statement.bindString(9, music.getSize()+"");
            statement.bindString(10, music.getAlbumId()+"");
            statement.execute();
            statement.clearBindings();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public List<MusicInfo> queryAllMusic(String table, String orderBy, String limit) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        List<MusicInfo> list = new ArrayList<>();
        Cursor cursor = db.query(table, null, null, null, null, null, orderBy, limit);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex("musicId"));
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String data = cursor.getString(cursor.getColumnIndex("data"));
            String artist = cursor.getString(cursor.getColumnIndex("artist"));
            int duration = cursor.getInt(cursor.getColumnIndex("duration"));
            String displayName = cursor.getString(cursor.getColumnIndex("display_name"));
            String albumName = cursor.getString(cursor.getColumnIndex("album_name"));
            String albumUri = cursor.getString(cursor.getColumnIndex("albumUri"));
            int albumId = cursor.getInt(cursor.getColumnIndex("album_id"));
            long size = cursor.getLong(cursor.getColumnIndex("size"));
            MusicInfo musicInfo = new MusicInfo(id, artist, title, duration, albumUri, data, displayName, albumName,size,albumId);
            list.add(musicInfo);
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return list;
    }

    public MusicInfo querySingleMusic(String table, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        MusicInfo musicInfo = null;
        Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, orderBy, "1");
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex("musicId"));
            String title = cursor.getString(cursor.getColumnIndex("title"));
            String data = cursor.getString(cursor.getColumnIndex("data"));
            String artist = cursor.getString(cursor.getColumnIndex("artist"));
            int duration = cursor.getInt(cursor.getColumnIndex("duration"));
            String displayName = cursor.getString(cursor.getColumnIndex("display_name"));
            String albumName = cursor.getString(cursor.getColumnIndex("album_name"));
            String albumUri = cursor.getString(cursor.getColumnIndex("albumUri"));
            int albumId = cursor.getInt(cursor.getColumnIndex("album_id"));
            long size = cursor.getLong(cursor.getColumnIndex("size"));
            musicInfo = new MusicInfo(id, artist, title, duration, albumUri, data, displayName, albumName,size,albumId);
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return musicInfo;
    }

    /**
     * 查询三个表大小
     *
     * @return
     */
    public int[] queryPlayListSize() {
        int size[] = new int[3];
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from latest_play", new String[]{});
        size[0] = cursor.getCount();
        cursor.close();
        Cursor cursor2 = db.rawQuery("select * from play_most", new String[]{});
        size[1] = cursor2.getCount();
        cursor2.close();
        Cursor cursor3 = db.rawQuery("select * from favorite", new String[]{});
        size[2] = cursor3.getCount();
        cursor3.close();
        db.close();
        return size;
    }

    /**
     * 查询三个表的albumUri
     */
    public String[] queryPlayListUri() {
        String albumUri[] = {"","",""};
        SQLiteDatabase db = mHelper.getReadableDatabase();

        Cursor cursor1 = db.query("latest_play", null, null, null, null, null, "_id desc", "1");
        if(cursor1.moveToNext()){
            albumUri[0] = cursor1.getString(cursor1.getColumnIndex("albumUri"));
        }
        cursor1.close();

        Cursor cursor2 = db.query("play_most", null, null, null, null, null, "count desc", "1");
        if(cursor2.moveToNext()){
            albumUri[1] = cursor2.getString(cursor2.getColumnIndex("albumUri"));
        }
        cursor2.close();

        Cursor cursor3 = db.query("favorite", null, null, null, null, null, null, "1");
        if(cursor3.moveToNext()){
            albumUri[2] = cursor3.getString(cursor3.getColumnIndex("albumUri"));
        }
        cursor3.close();



        return albumUri;
    }


    public void deleteAllData(String table) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.execSQL("delete from " + table, new Object[]{});
        db.close();
    }

    public void deleteSingle(String table, int id) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        if (db.query(table, new String[]{"musicId"}, "musicId=?", new String[]{id + ""}, null, null, null).getCount() != 0) {
            db.execSQL("delete from " + table + " where musicId=?", new Object[]{id});
        }
        db.close();
    }

    public void deleteListData(String table, List<Integer> list) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();
        SQLiteStatement statement = db.compileStatement("delete from " + table + " where musicId = ?");
        for (Integer integer : list
                ) {
            statement.bindString(1, integer + "");
            statement.execute();
            statement.clearBindings();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * 当新的音乐播放时调用此函数
     * 1.将最近播放表更新
     * 2.将最常播放表更新
     *
     * @param musicInfo
     */
    public void updateNewMusicToDatabase(MusicInfo musicInfo) {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        //更新 最近播放表，先删除再插入，防止重复
        //若表中存在此数据
        if (db.query("latest_play", new String[]{"musicId"}, "musicId=?", new String[]{musicInfo.getId() + ""}, null, null, null).getCount() != 0) {
            //删除
            db.execSQL("delete from latest_play where musicId=?", new Object[]{musicInfo.getId()});
        }
        //插入
        db.execSQL("insert into latest_play(musicId,display_name,artist,title,duration,albumUri,data,album_name,size,album_id) values(?,?,?,?,?,?,?,?,?,?)", new Object[]{musicInfo.getId(),
                musicInfo.getDisplayName(), musicInfo.getArtist(), musicInfo.getTitle(), musicInfo.getDuration(), musicInfo.getAlbumUri(), musicInfo.getData(), musicInfo.getAlbumName(),musicInfo.getSize(),musicInfo.getAlbumId()});


        //将最常播放表的播放次数更新
        int count = 0;
        Cursor cursor = db.query("play_most", new String[]{"count"}, "musicId=?", new String[]{musicInfo.getId() + ""}, null, null, null, null);
        if (cursor.moveToNext()) {
            count = cursor.getInt(cursor.getColumnIndex("count"));
        }
        cursor.close();
        if (count == 0) {
            //若表中没有此数据，插入新数据
            db.execSQL("insert into play_most(musicId,display_name,artist,title,duration,albumUri,data,album_name,count,size,album_id) values(?,?,?,?,?,?,?,?,?,?,?)", new Object[]{musicInfo.getId(),
                    musicInfo.getDisplayName(), musicInfo.getArtist(), musicInfo.getTitle(), musicInfo.getDuration(), musicInfo.getAlbumUri(), musicInfo.getData(), musicInfo.getAlbumName(), 1,musicInfo.getSize(),musicInfo.getAlbumId()});
        } else {
            //若表中已有此数据，更新count
            db.execSQL("update play_most set count=? where musicId=?", new Object[]{count + 1, musicInfo.getId()});
        }
        db.close();
    }

    public void updatePlayMostList(MusicInfo musicInfo){
        SQLiteDatabase db = mHelper.getWritableDatabase();
        //将最常播放表的播放次数更新
        int count = 0;
        Cursor cursor = db.query("play_most", new String[]{"count"}, "musicId=?", new String[]{musicInfo.getId() + ""}, null, null, null, null);
        if (cursor.moveToNext()) {
            count = cursor.getInt(cursor.getColumnIndex("count"));
        }
        db.execSQL("update play_most set count=? where musicId=?", new Object[]{count + 1, musicInfo.getId()});
    }
}
