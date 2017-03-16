package com.example.luckychuan.musicplayer.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.model.Album;
import com.example.luckychuan.musicplayer.model.Artist;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 系统数据库
 */

public class MediaStoreManager {

    private static final String TAG = "MediaStoreManager";

    //查询条件常量
    private static final int _ID = 0;
    private static final int DISPLAY_NAME = 1;
    private static final int DATA = 2;
    private static final int ALBUM_ID = 3;
    private static final int ARTIST = 4;
    private static final int DURATION = 5;
    private static final int TITLE = 6;
    private static final int ALBUM = 7;
    private static final int SIZE = 8;

    //MEDIA的常量
    private static final Uri MEDIA_URI = Media.EXTERNAL_CONTENT_URI;
    //查询条件：0.id,1.文件名,2.文件路径,3.专辑封面id,4.艺术家,5.时长,6.音乐名,7.专辑封面名字
    private static final String[] MEDIA_PROJECTION = {
            Media._ID,
            Media.DISPLAY_NAME,
            Media.DATA,
            Media.ALBUM_ID,
            Media.ARTIST,
            Media.DURATION,
            Media.TITLE,
            Media.ALBUM,
            Media.SIZE,
    };
    //查询专辑封面的String，用于组成Uri
    private static final String ALBUM_URI_STRING = "content://media/external/audio/albumart";

    //PLAYLIST的常量
    private static final Uri PLAYLIST_URI = Playlists.EXTERNAL_CONTENT_URI;
    private static final String[] PLAYLIST_PROJECTION = {
            Playlists._ID,
            Playlists.NAME,
    };
    private static final String[] PLAYLIST_MEMBER_PROJECTION = {
            Playlists.Members.AUDIO_ID,
            Media.DISPLAY_NAME,
            Media.DATA,
            Media.ALBUM_ID,
            Media.ARTIST,
            Media.DURATION,
            Media.TITLE,
            Media.ALBUM,
            Media.SIZE,
    };

    private final static Uri ALBUM_URI = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    private final static String[] ALBUM_PROJECTION = {
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.ARTIST,
    };

    private final static Uri ARTIST_URI = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    private final static String[] ARTIST_PROJECTION = {
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
    };

    //单例模式
    private static MediaStoreManager mMediaStoreManager;

    private ContentResolver mResolver;


    private MediaStoreManager(Context context) {
        mResolver = context.getContentResolver();
    }

    public synchronized static MediaStoreManager getInstance(Context context) {
        if (mMediaStoreManager == null) {
            mMediaStoreManager = new MediaStoreManager(context);
        }
        return mMediaStoreManager;
    }

    /**
     * 从系统中获得音乐数据
     */
    public List<MusicInfo> queryMusicData(Uri uri, String[] projection, String selection, String[] selectionArgs, String oderBy) {
        List<MusicInfo> list = new ArrayList<>();
        Cursor cursor = mResolver.query(uri, projection, selection, selectionArgs, oderBy);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(projection[_ID]));
                String title = cursor.getString(cursor.getColumnIndex(projection[TITLE]));
                String data = cursor.getString(cursor.getColumnIndex(projection[DATA]));
                String artist = cursor.getString(cursor.getColumnIndex(projection[ARTIST]));
                int duration = cursor.getInt(cursor.getColumnIndex(projection[DURATION]));
                String displayName = cursor.getString(cursor.getColumnIndex(projection[DISPLAY_NAME]));
                String albumName = cursor.getString(cursor.getColumnIndex(projection[ALBUM]));
                int albumId = cursor.getInt(cursor.getColumnIndex(projection[ALBUM_ID]));
                long size = cursor.getLong(cursor.getColumnIndex(projection[SIZE]));
                String album = ContentUris.withAppendedId(Uri.parse(ALBUM_URI_STRING), albumId).toString();
                MusicInfo musicInfo = new MusicInfo(id, artist, title, duration, album, data, displayName, albumName, size, albumId);
//                Log.d(TAG, "queryMusicData: "+musicInfo.toString());
                list.add(musicInfo);
            }
            cursor.close();
        }
        return list;
    }

    /**
     * 从import android.provider.MediaStore.Audio.Media中获取音乐
     *
     * @return
     */
    public List<MusicInfo> queryMusicData() {
        return queryMusicData(MEDIA_URI, MEDIA_PROJECTION, null, null, Media._ID + " desc");
    }

    public List<MusicInfo> queryMusicData(String selection){
        return queryMusicData(MEDIA_URI, MEDIA_PROJECTION, Media.TITLE+" like '%"+selection+"%'", new String[]{}, Media.DEFAULT_SORT_ORDER);
    }

    public boolean deleteMusic(int id, String url) {
        boolean flag = new File(url).getAbsoluteFile().delete();
        mResolver.delete(MEDIA_URI, "_id=?", new String[]{id + ""});
        return flag;
    }

    public void deleteMusic(List<Integer> musicIdList) {
        for (Integer id : musicIdList) {
            //通过id查找要删除音乐的地址
            Cursor cursor = mResolver.query(MEDIA_URI, new String[]{Media.DATA}, "_id=?", new String[]{id + ""}, null);
            while (cursor.moveToNext()) {
                deleteMusic(id, cursor.getString(cursor.getColumnIndex(Media.DATA)));
            }
            cursor.close();
        }

    }

    /**
     * 获得播放列表
     *
     * @return
     */
    public List<PlaylistInfo> queryPlaylistData() {
        List<PlaylistInfo> list = new ArrayList<>();
        Cursor cursor = mResolver.query(PLAYLIST_URI, PLAYLIST_PROJECTION, Playlists.NAME + "!=?", new String[]{"youku"}, Playlists.DATE_ADDED + " asc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(Playlists._ID));
                String name = cursor.getString(cursor.getColumnIndex(Playlists.NAME));
                Cursor cursor1 = mResolver.query(Playlists.Members.getContentUri("external", id), new String[]{Playlists.Members.ALBUM_ID}, null, null, Playlists.Members.AUDIO_ID + " asc");
                int size = cursor1.getCount();
                String album = null;
                if (cursor1.moveToNext()) {
                    //获得第一首音乐的封面
                    //获得专辑封面的id
                    int albumId = cursor1.getInt(cursor1.getColumnIndex(Playlists.Members.ALBUM_ID));
                    album = ContentUris.withAppendedId(Uri.parse(ALBUM_URI_STRING), albumId).toString();
                }
                cursor1.close();
                PlaylistInfo playlistInfo = new PlaylistInfo(id, name, size, album);
                list.add(playlistInfo);
            }
            cursor.close();
        }
        return list;
    }

    public int queryNewPlaylistId() {
        Cursor cursor = mResolver.query(PLAYLIST_URI, new String[]{Playlists._ID}, null, null, Playlists.DATE_ADDED + " desc");
        int id = 0;
        if (cursor.moveToNext()) {
            id = cursor.getInt(cursor.getColumnIndex(Playlists._ID));
        }
        return id;
    }


    /**
     * 获得选定播放列表里的音乐信息
     *
     * @param id 列表的id
     * @return
     */
    public List<MusicInfo> queryMusicDataInPlaylist(int id) {
        return queryMusicData(Playlists.Members.getContentUri("external", id), PLAYLIST_MEMBER_PROJECTION, null, null, Playlists.Members.DEFAULT_SORT_ORDER+ " asc");
    }

    public PlaylistInfo querySinglePlaylistInfo() {
        Cursor cursor = mResolver.query(PLAYLIST_URI, PLAYLIST_PROJECTION, null, null, Playlists._ID + " asc");
        PlaylistInfo playlistInfo = null;
        if (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(Playlists._ID));
            String name = cursor.getString(cursor.getColumnIndex(Playlists.NAME));
            Cursor cursor1 = mResolver.query(Playlists.Members.getContentUri("external", id), new String[]{Playlists.Members.ALBUM_ID}, null, null, Playlists.Members.AUDIO_ID + " desc");
            int size = cursor1.getCount();
            String album = null;
            if (cursor1.moveToNext()) {
                //获得第一首音乐的封面
                //获得专辑封面的id
                int albumId = cursor1.getInt(cursor1.getColumnIndex(Playlists.Members.ALBUM_ID));
                album = ContentUris.withAppendedId(Uri.parse(ALBUM_URI_STRING), albumId).toString();
            }
            cursor1.close();
            playlistInfo = new PlaylistInfo(id, name, size, album);

        }
        cursor.close();
        return playlistInfo;
    }

    /**
     * 添加播放列表
     *
     * @param name
     */
    public void addPlaylistData(String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Playlists.NAME, name);
        mResolver.insert(PLAYLIST_URI, contentValues);
    }

    public void updatePlaylistName(int id, String newName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Playlists.NAME, newName);
        mResolver.update(PLAYLIST_URI, contentValues, Playlists._ID + "=?", new String[]{id + ""});
    }

    public void deletePlaylistData(int playlistId) {
        mResolver.delete(PLAYLIST_URI, Playlists._ID + "=?", new String[]{playlistId + ""});
    }

    public void deleteMusicFromPlaylist(int playlistId, List<Integer> list) {
        Uri uri = Playlists.Members.getContentUri("external", playlistId);
        for (Integer i : list
                ) {
            mResolver.delete(uri, "audio_id=?", new String[]{i.toString()});
        }
    }

    public void insertListMusicToPlaylist(Context context, int playlistId, List<MusicInfo> list) {
        Uri uri = Playlists.Members.getContentUri("external", playlistId);
        Log.d(TAG, "insertListMusicToPlaylist: " + uri.toString());
        for (MusicInfo musicInfo : list) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Playlists.Members.AUDIO_ID, musicInfo.getId());
            contentValues.put(Playlists.Members.PLAY_ORDER, Playlists.Members.DEFAULT_SORT_ORDER);
            mResolver.insert(uri, contentValues);
        }
        Toast.makeText(context, "成功添加 " + list.size() + " 首歌曲到播放列表", Toast.LENGTH_SHORT).show();
    }

    public void insertMusicToPlaylist(Context context, int playlistId, int audioId) {
        Uri uri = Playlists.Members.getContentUri("external", playlistId);
        List<MusicInfo> existMusicList = queryMusicDataInPlaylist(playlistId);
        boolean isExist = false;
        for (MusicInfo music : existMusicList) {
            if (music.getId() == audioId) {
                isExist = true;
                break;
            }
        }
        if (isExist) {
            Toast.makeText(context, "音乐已存在", Toast.LENGTH_SHORT).show();
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Playlists.Members.AUDIO_ID, audioId);
            contentValues.put(Playlists.Members.PLAY_ORDER, Playlists.Members.DEFAULT_SORT_ORDER);
            mResolver.insert(uri, contentValues);
            Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show();
        }
    }

    public List<Album> queryAlbumData() {
        List<Album> albumList = new ArrayList<>();
        Cursor cursor = mResolver.query(ALBUM_URI, ALBUM_PROJECTION, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                int numberOfSong = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                Uri albumUri = ContentUris.withAppendedId(Uri.parse(ALBUM_URI_STRING), id);
                String albumUrl = albumUri.toString();
                Album album = new Album(id, name, numberOfSong, artist, albumUrl);
                albumList.add(album);
            }

            cursor.close();
        }
        return albumList;
    }

    public Album queryAlbumData(int albumId) {
        Album album = null;
        Cursor cursor = mResolver.query(ALBUM_URI, ALBUM_PROJECTION, MediaStore.Audio.Albums._ID + "=?", new String[]{albumId + ""}, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                int numberOfSong = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
                Uri albumUri = ContentUris.withAppendedId(Uri.parse(ALBUM_URI_STRING), id);
                String albumUrl = albumUri.toString();
                album = new Album(id, name, numberOfSong, artist, albumUrl);
            }
            cursor.close();
        }
        return album;
    }


    /**
     * 获得选定专辑里的音乐信息
     *
     * @param albumId 专辑的id
     * @return
     */
    public List<MusicInfo> queryMusicDataInAlbum(int albumId) {
        return queryMusicData(MEDIA_URI, MEDIA_PROJECTION, Media.ALBUM_ID + "=?", new String[]{albumId + ""}, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }

    public List<Integer> queryMusicIdInAlbum(int albumId) {
        List<Integer> list = new ArrayList<>();
        Cursor cursor = mResolver.query(MEDIA_URI, new String[]{Media._ID}, Media.ALBUM_ID + "=?", new String[]{albumId + ""}, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
        while (cursor.moveToNext()) {
            list.add((Integer) cursor.getInt(cursor.getColumnIndex(Media._ID)));
        }
        cursor.close();
        return list;
    }


    public List<Artist> queryArtistData() {
        List<Artist> list = new ArrayList<>();
        Cursor cursor = mResolver.query(ARTIST_URI, ARTIST_PROJECTION, null, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
            int numberOfAlbums = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
            int numberOfTracks = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
            Artist artist = new Artist(id, numberOfAlbums, numberOfTracks, name);
//            Log.d(TAG, "queryArtistData: " + artist.toString());
            list.add(artist);
        }
        cursor.close();
        return list;
    }

    public Artist queryArtistData(String artistName) {
        Artist artist = null;
        Cursor cursor = mResolver.query(ARTIST_URI, ARTIST_PROJECTION, MediaStore.Audio.Artists.ARTIST + "=?", new String[]{artistName}, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                int numberOfAlbums = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
                int numberOfTracks = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
                artist = new Artist(id, numberOfAlbums, numberOfTracks, name);
            }
            cursor.close();
        }

        return artist;
    }


    /**
     * 从艺术家里查询音乐，先获得艺术家所有专辑，再查询每张专辑里的音乐
     *
     * @param artistId
     * @return
     */
    public List<MusicInfo> queryMusicDataInArtist(int artistId) {
        List<MusicInfo> musicInfoList = queryMusicData(MEDIA_URI, MEDIA_PROJECTION, Media.ARTIST_ID + "=?", new String[]{artistId + ""}, ALBUM_ID + "");
//        List<Album> albumList = queryAlbumInArtist(artistId);
//        for (Album album : albumList) {
//            musicInfoList.addAll(queryMusicDataInAlbum(album.getId()));
//        }
        return musicInfoList;
    }

    public List<Integer> queryMusicIdInArtist(int artistId) {
        List<Integer> list = new ArrayList<>();
        Cursor cursor = mResolver.query(MEDIA_URI, new String[]{Media._ID}, Media.ARTIST_ID + "=?", new String[]{artistId + ""}, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
        while (cursor.moveToNext()) {
            list.add((Integer) cursor.getInt(cursor.getColumnIndex(Media._ID)));
        }
        cursor.close();
        return list;
    }


}
