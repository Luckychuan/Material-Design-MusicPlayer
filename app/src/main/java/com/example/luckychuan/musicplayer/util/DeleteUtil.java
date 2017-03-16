package com.example.luckychuan.musicplayer.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.database.MusicDatabase;
import com.example.luckychuan.musicplayer.model.MusicInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 删除操作
 * 1.删除单首音乐
 * 2.删除多首音乐
 * 3.删除多个专辑
 * 4.删除多个艺术家
 * 5.删除多个音乐聊表
 */
public class DeleteUtil {

    private static final String TAG = "DeleteUtil";

    private final static int HIDE_PROGRESS_DIALOG = 1;

    //item被删除的类型
    private int mDeleteType;
    public final static int MUSIC_INFO = 1;
    public final static int ALBUM = 2;
    public final static int ARTIST = 3;
    public final static int PLAYLIST = 4;


    private Context mContext;

    //长按item进入删除模式
    private boolean isOnDeleteMode;
    //被选中将要进行删除的音乐的id
    private static List<Integer> mDeleteIdList = new ArrayList<>();


    public DeleteUtil(Context context) {
        mContext = context;
        isOnDeleteMode = false;
    }

    public static boolean deleteMusicFile(Context context, MusicInfo music) {
        //从设备中删除
        boolean flag = MediaStoreManager.getInstance(context).deleteMusic(music.getId(), music.getData());
        //将三个表的数据删除
        deleteMusicInPlaylist(context, music.getId());
        return flag;
    }

    public void confirmDelete() {
        final ProgressDialog progressDialog = ProgressDialog.show(mContext, "", "正在删除...", true, false);
        //删除完成后关闭progressDialog
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == HIDE_PROGRESS_DIALOG) {
                    progressDialog.hide();
                    //删除完成，退出删除模式
                    cancelDeleteMode();
                }
            }
        };

        //防止线程阻塞使progressDialog无法显示
        new Thread(new Runnable() {
            @Override
            public void run() {

                //发送广播刷新删除的数据
                Intent intent1 = new Intent();
                intent1.putIntegerArrayListExtra("delete_list", (ArrayList<Integer>) mDeleteIdList);

                //所有要删除的音乐id数据
                ArrayList<Integer> deleteMusicIdList = new ArrayList<>();


                //删除的类型
                if (mDeleteType == MUSIC_INFO) {
                    intent1.setAction("ON_DELETE_MUSIC_INFO");
                    mContext.sendBroadcast(intent1);

                    //音乐类型： 直接删除
                    MediaStoreManager.getInstance(mContext).deleteMusic(mDeleteIdList);
                    deleteMusicIdList.addAll(mDeleteIdList);

                } else if (mDeleteType == ALBUM) {
                    intent1.setAction("ON_DELETE_ALBUM");
                    mContext.sendBroadcast(intent1);

                    //专辑类型： 先通过id查询专辑里的所有音乐的id加入删除列表中，然后再通过id删除音乐
                    for (Integer id : mDeleteIdList) {
                        deleteMusicIdList.addAll(MediaStoreManager.getInstance(mContext).queryMusicIdInAlbum(id));
                    }
                    MediaStoreManager.getInstance(mContext).deleteMusic(deleteMusicIdList);

                } else if (mDeleteType == ARTIST) {
                    intent1.setAction("ON_DELETE_ARTIST");
                    mContext.sendBroadcast(intent1);

                    //艺术家类型： 先通过id查询艺术家里的所有音乐的id加入删除列表中，然后再通过id删除音乐
                    for (Integer id : mDeleteIdList) {
                        deleteMusicIdList.addAll(MediaStoreManager.getInstance(mContext).queryMusicIdInArtist(id));
                    }
                    MediaStoreManager.getInstance(mContext).deleteMusic(deleteMusicIdList);

                } else {
                    intent1.setAction("ON_DELETE_PLAYLIST");

                    mContext.sendBroadcast(intent1);

                    //音乐列表类型：仅删除列表，不删除音乐
                    for (Integer id : mDeleteIdList) {
                        MediaStoreManager.getInstance(mContext).deletePlaylistData(id);
                    }
                }

                //等待动画完成
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //将三个表的数据删除
                if (deleteMusicIdList.size() > 0) {
                    for (Integer id : deleteMusicIdList) {
                        deleteMusicInPlaylist(mContext, id);
                    }
                    mContext.sendBroadcast(new Intent("UPDATE_THREE_PLAYLIST"));
                }

                //删除完成，重新载入
                mContext.sendBroadcast(new Intent("DELETE_FINISH"));


                //发送广播到service更新正在播放的列表
                Intent serviceIntent = new Intent("REMOVE_MUSIC_LIST");
                serviceIntent.putIntegerArrayListExtra("remove_list", deleteMusicIdList);
                mContext.sendBroadcast(serviceIntent);

                //将progressDialog关闭
                Message msg = new Message();
                msg.what = HIDE_PROGRESS_DIALOG;
                handler.sendMessage(msg);

                cancelDeleteMode();
            }
        }).start();

    }


    /**
     * 从三个表中删除单个数据
     *
     * @param id
     */
    public static void deleteMusicInPlaylist(Context context, int id) {
        MusicDatabase db = MusicDatabase.getInstance(context);
        Log.d("PlayService", "deleteMusicInPlaylist: ");
        db.deleteSingle("latest_play", id);
        db.deleteSingle("play_most", id);
        db.deleteSingle("favorite", id);

    }

    public List<Integer> getDeleteList() {
        return mDeleteIdList;
    }

    public int getDeleteListSize() {
        return mDeleteIdList.size();
    }

    public void clearDeleteList() {
        mDeleteIdList.clear();
    }

    public void startDeleteMode(int type) {
        mDeleteType = type;
        if (isOnDeleteMode) {
            cancelDeleteMode();
        }
        isOnDeleteMode = true;
    }

    public void cancelDeleteMode() {
        isOnDeleteMode = false;
        mDeleteIdList.clear();
    }

    public boolean isOnDeleteMode() {
        return isOnDeleteMode;
    }

    public void selectItem(Integer id) {
        if (mDeleteIdList.contains(id)) {
            mDeleteIdList.remove(id);
        } else {
            mDeleteIdList.add(id);
        }
    }

    public static boolean isContain(Integer id) {
        return mDeleteIdList.contains(id);
    }


    public void selectAll(List<Integer> idList) {
        mDeleteIdList.clear();
        mDeleteIdList.addAll(idList);
    }

}
