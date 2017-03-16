package com.example.luckychuan.musicplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.activity.MusicActivity;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.database.MusicDatabase;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.widget.MusicWidget;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 播放音乐的服务
 * 1.对音乐播放，暂停，切歌，切换进度的操作，
 * 2.保存和修改正在播放的音乐信息和音乐列表
 * 3.设置和取消通知
 */
public class PlayService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "PlayService";
    //通知的requestCode
    private static final int MINI_PLAYER = 100;
    private static final int NEXT_MUSIC = 1;
    private static final int PREVIOUS_MUSIC = 2;

    private static MediaPlayer mMediaPlayer;

    //用户选择的播放列表
    private static List<MusicInfo> mList;
    //当前播放列表，正常顺序或随机顺序的播放列表
    private static List<MusicInfo> mCurrentList;

    private static int mPosition;
    private static MusicInfo mMusicInfo;

    private UserActionReceiver mUserActionReceiver;
    private ListReceiver mListReceiver;
    private SystemExitReceiver mSystemExitReceiver;
    private HeadsetPlugReceiver mHeadsetPlugReceiver;

    //通知
    private NotificationManager mNotificationManager;
    private AudioManager mAudioManager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");

        mMediaPlayer = new MediaPlayer();
        //当音乐播放完毕时播放下一首歌曲
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //从存储中获得随机和循环变量
                boolean isLoop = getSharedPreferences("random_and_loop", MODE_PRIVATE).getBoolean("loop", false);
//                Log.d(TAG, "onCompletion: isLoop"+isLoop);
                if (isLoop) {
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.start();
                    //当MediaPlayer.isLoop()为true时不会再调用onCompletion方法，这里在重新播放之后强行让loop为false
                    mMediaPlayer.setLooping(false);
                    MusicDatabase.getInstance(getApplicationContext()).updatePlayMostList(mMusicInfo);
                } else {
//                    Log.d(TAG, "onCompletion: change");
                    changeMusic(NEXT_MUSIC);
                }
            }
        });

        //打开app时初始化音乐列表
        initMusicList();


        mUserActionReceiver = new UserActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("SEEK_TO");
        filter.addAction("PAUSE");
        filter.addAction("PLAY");
        filter.addAction("NEXT");
        filter.addAction("PREVIOUS");
        filter.addAction("RANDOM_CHANGE");
        filter.addAction("DELETE_SINGLE");
        filter.addAction("VIEW_PAGER_CHANGE_MUSIC");
        filter.addAction("RANDOM_PLAY");
        registerReceiver(mUserActionReceiver, filter);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        mListReceiver = new ListReceiver();
        IntentFilter filter1 = new IntentFilter("REMOVE_MUSIC_LIST");
        filter1.addAction("ADD_TO_CURRENT_LIST");
        filter1.addAction("INSERT_MUSIC");
        filter1.addAction("REMOVE_MUSIC");
        registerReceiver(mListReceiver, filter1);

        mSystemExitReceiver = new SystemExitReceiver();
        IntentFilter filter2 = new IntentFilter("SYSTEM_EXIT");
        registerReceiver(mSystemExitReceiver, filter2);

        mHeadsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter filter3 = new IntentFilter("android.intent.action.HEADSET_PLUG");
        registerReceiver(mHeadsetPlugReceiver, filter3);

    }

    private void initMusicList() {
        SharedPreferences sp = getSharedPreferences("position", MODE_PRIVATE);
        mPosition = sp.getInt("position", -1);

        //从数据库中获得当前播放列表
//        Log.d(TAG, "initMusicList: position:" + mPosition);
        if (mPosition != -1) {
            //若保存的播放列表不为空
            mList = MusicDatabase.getInstance(getApplicationContext()).queryAllMusic("service_list", null, null);
            mCurrentList = new ArrayList<>();
            if (mList.size() != 0) {
                mCurrentList.addAll(mList);
                //获得当前播放模式
                boolean isRandom = getSharedPreferences("random_and_loop", MODE_PRIVATE).getBoolean("random", false);
                if (isRandom) {
                    Collections.shuffle(mCurrentList);
                    MusicInfo music = mList.get(mPosition);
                    for (int i = 0; i < mCurrentList.size(); i++) {
                        if (music.toString().equals(mCurrentList.get(i).toString())) {
                            mPosition = i;
                            break;
                        }
                    }
                }
                //初始化第第一首音乐
                setDataSource();
            }
        } else {
            mList = new ArrayList<>();
            mCurrentList = new ArrayList<>();
        }
    }

    /**
     * 当点击音乐列表的音乐时会被调用
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        if (intent != null) {
            List<MusicInfo> list = (List<MusicInfo>) intent.getSerializableExtra("list");
            //获得新的音乐列表
            if (list != null) {
                mList.clear();
                mList.addAll(list);
                mCurrentList.clear();
                mCurrentList.addAll(mList);

                int position = intent.getIntExtra("position", -1);
                if (position != -1) {
                    mPosition = position;
                    mMusicInfo = mList.get(mPosition);
//                    Log.d("random","onStartCommand position:"+position);
//                    Log.d("random", "onStartCommand: "+mCurrentList.get(mPosition).getTitle());
                }


//                for (MusicInfo music : mList) {
//                    Log.d("random", "点击 mList " + music.getTitle());
//                }

                //获得当前播放模式
                boolean isRandom = getSharedPreferences("random_and_loop", MODE_PRIVATE).getBoolean("random", false);
//                Log.d("random", "点击 isRandom" + isRandom);
                if (isRandom) {
                    Collections.shuffle(mCurrentList);
                    //重置position
                    for (int position1 = 0; position1 < mCurrentList.size(); position1++) {
                        if (mMusicInfo.toString().equals(mCurrentList.get(position1).toString())) {
                            mPosition = position1;
                            break;
                        }
                    }
                }

//                for (MusicInfo music : mCurrentList) {
//                    Log.d("random", "点击 mCurrentList " + music.getTitle());
//                }
                //更新MusicActivity的当前播放列表界面
                sendBroadcast(new Intent("MUSIC_LIST_CHANGE"));
                start(true);
            }

            //从桌面小组件发来的操作
            int action = intent.getIntExtra("action", 0);
            switch (action) {
                case 0:
                    break;
                case MusicWidget.START_ACTIVITY:
                    Intent startActivity = new Intent(getApplicationContext(), MusicActivity.class);
                    startActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startActivity);
                    break;
                case MusicWidget.NEXT:
                    changeMusic(NEXT_MUSIC);
                    break;
                case MusicWidget.PREVIOUS:
                    if (mMediaPlayer.getCurrentPosition() < 3000) {
                        changeMusic(PREVIOUS_MUSIC);
                    } else {
                        mMediaPlayer.seekTo(0);
                    }
                    break;
                case MusicWidget.PAUSE:
                    pause();
                    break;
                case MusicWidget.PLAY:
                    start(false);
                    break;
            }
        }

        return super.

                onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        mMediaPlayer.release();
        mMediaPlayer = null;

        unregisterReceiver(mUserActionReceiver);
        unregisterReceiver(mListReceiver);
        unregisterReceiver(mSystemExitReceiver);
        unregisterReceiver(mHeadsetPlugReceiver);

        cancelNotification();

        mAudioManager.abandonAudioFocus(this);

        boolean isRandom = getSharedPreferences("random_and_loop", MODE_PRIVATE).getBoolean("random", false);
        if (isRandom) {
            for (int i = 0; i < mList.size(); i++) {
                if (mMusicInfo.toString().equals(mList.get(i).toString())) {
                    //                  Log.d(TAG, "onDestroy: mMusicInfo:"+mMusicInfo.getTitle()+"mlist.get:"+mList.get(i).getTitle());
                    mPosition = i;
                    break;
                }
            }
        }

        //保存当前list表名和position
        SharedPreferences.Editor editor = getSharedPreferences("position", MODE_PRIVATE).edit();
        editor.putInt("position", mPosition);
        editor.commit();
        MusicDatabase musicDatabase = MusicDatabase.getInstance(getApplicationContext());
        musicDatabase.deleteAllData("service_list");
        musicDatabase.insertListMusic("service_list", mList);

        super.onDestroy();
    }

    private void cancelNotification() {
        //音乐后台关闭
        stopForeground(true);
        mNotificationManager.cancel(MINI_PLAYER);
    }


    private void setNotification() {
        //设置通知的UI
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
        remoteViews.setTextViewText(R.id.musicName_notification, mMusicInfo.getTitle());
        Bitmap bitmap;
        Uri uri = Uri.parse(mMusicInfo.getAlbumUri());
        InputStream in = null;
        try {
            in = getApplicationContext().getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in != null) {
            bitmap = BitmapFactory.decodeStream(in);
            remoteViews.setImageViewBitmap(R.id.album_notification, bitmap);
        } else {
            remoteViews.setImageViewResource(R.id.album_notification, R.drawable.play_page_default_cover);
        }
        remoteViews.setTextViewText(R.id.artist_notification, mMusicInfo.getArtist());

        if (mMediaPlayer.isPlaying()) {
            remoteViews.setImageViewResource(R.id.play_notification, R.drawable.pause_ripple);
        } else {
            remoteViews.setImageViewResource(R.id.play_notification, R.drawable.play_ripple);
        }


        //点击事件
        //点击notification进入到Activity
        Intent toActivity = new Intent(this, MusicActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, toActivity, 0);
        remoteViews.setOnClickPendingIntent(R.id.notification_layout, pi);

        //上一首按钮
        PendingIntent pi1 = PendingIntent.getBroadcast(this, 1, new Intent("PREVIOUS"), 0);
        remoteViews.setOnClickPendingIntent(R.id.previous_notification, pi1);


        //播放按钮
        if (!mMediaPlayer.isPlaying()) {
            PendingIntent pi2 = PendingIntent.getBroadcast(this, 2, new Intent("PLAY"), 0);
            remoteViews.setOnClickPendingIntent(R.id.play_notification, pi2);
        } else {
            PendingIntent pi3 = PendingIntent.getBroadcast(this, 3, new Intent("PAUSE"), 0);
            remoteViews.setOnClickPendingIntent(R.id.play_notification, pi3);
        }

        //下一首按钮
        PendingIntent pi4 = PendingIntent.getBroadcast(this, 4, new Intent("NEXT"), 0);
        remoteViews.setOnClickPendingIntent(R.id.next_notification, pi4);

        //关闭通知
        Intent close = new Intent("SYSTEM_EXIT");
        PendingIntent pi5 = PendingIntent.getBroadcast(this, 5, close, 0);
        remoteViews.setOnClickPendingIntent(R.id.close_notification, pi5);


        //创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.music_write);
        Notification notification = builder.build();
        notification.contentView = remoteViews;
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        //保证service不会被杀死
        startForeground(MINI_PLAYER, notification);
        mNotificationManager.notify(MINI_PLAYER, notification);
    }

    private void setDataSource() {
        mMusicInfo = mCurrentList.get(mPosition);
//        Log.d("random","setDataSource:"+ mMusicInfo.getTitle());
        //发送广播更新UI
        sendBroadcast(new Intent("UPDATE_UI"));
        Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                "" + mMusicInfo.getId());
        mMediaPlayer.reset();

        try {
            mMediaPlayer.setDataSource(this, uri);
            mMediaPlayer.prepare();
        } catch (IOException e) {
            //载入文件发生异常，无法播放
//            //播放下一首歌曲
//            changeMusic(NEXT_MUSIC);
//            //将数据库的数据删除
//            DeleteUtil.deleteMusicInPlaylist(getApplicationContext(), mMusicInfo.getId());
            e.printStackTrace();
        }
    }

    private void changeMusic(int control) {
        if (control == NEXT_MUSIC) {
            if (mPosition == mList.size() - 1) {
                mPosition = 0;
            } else {
                mPosition += 1;
            }

        } else if (control == PREVIOUS_MUSIC) {
            if (mPosition == 0) {
                mPosition = mList.size() - 1;
            } else {
                mPosition -= 1;
            }
        }

        mMusicInfo = mList.get(mPosition);
        start(true);
    }


    private void pause() {
        mMediaPlayer.pause();
        //更新ui
        setNotification();
        sendBroadcast(new Intent("UPDATE_PLAY_BUTTON"));
        MusicWidget.updateWidgetPlayButton(getApplicationContext(), false);
    }

    public void removeListItem(int position) {
        MusicInfo musicInfo = mCurrentList.get(position);
        mCurrentList.remove(musicInfo);
        mList.remove(musicInfo);
        if (mList.size() != 0) {
            if (mPosition > position) {
                mPosition--;
                //发送广播通知MusicActivity更新UI
                sendBroadcast(new Intent("ON_SINGLE_MUSIC_DELETE"));
            } else if (mPosition == position) {
                mMediaPlayer.reset();
                if (mPosition >= mList.size()) {
                    mPosition = 0;
                }
                //发送广播通知MusicActivity更新UI
                Intent intent2 = new Intent("ON_SINGLE_MUSIC_DELETE");
                intent2.putExtra("is_mMusicInfo", true);
                sendBroadcast(intent2);
                start(true);
            } else {
                sendBroadcast(new Intent("ON_SINGLE_MUSIC_DELETE"));
            }
        } else {
            mMusicInfo = null;
            mMediaPlayer.reset();
            //发送广播通知MusicActivity更新UI
            sendBroadcast(new Intent("ON_SINGLE_MUSIC_DELETE"));
            sendBroadcast(new Intent("UPDATE_UI"));
        }
    }


    public static boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        } else {
            return false;
        }
    }

    public static MusicInfo getMusicInfo() {
        return mMusicInfo;
    }

    public static List<MusicInfo> getCurrentMusicList() {
        return mCurrentList;
    }


    public static int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public static int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public static int getPosition() {
        return mPosition;
    }


    private void start(boolean isNewMusic) {
        Log.d(TAG, "start: ");
        //获得音乐焦点
        mAudioManager.abandonAudioFocus(this);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (isNewMusic) {
            setDataSource();
            mMediaPlayer.start();

            sendBroadcast(new Intent("UPDATE_THREE_PLAYLIST"));

            //更新最近播放和最常播放
            MusicDatabase.getInstance(getApplicationContext()).updateNewMusicToDatabase(mMusicInfo);

        } else {
            mMediaPlayer.start();
            sendBroadcast(new Intent("UPDATE_PLAY_BUTTON"));
            MusicWidget.updateWidgetPlayButton(getApplicationContext(), true);
        }
        //设置通知
        setNotification();
        //更新桌面小部件
        MusicWidget.updateWidget(getApplicationContext(), mMusicInfo);
    }


    /**
     * 实现AudioManager.OnAudioFocusChangeListener的方法
     *
     * @param focusChange
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pause();
        }
    }

    //正在播放的的音乐列表的增加修改和删除
    class ListReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("REMOVE_MUSIC_LIST")) {
                //将要删除的音乐移出列表
                List<Integer> removeList = intent.getIntegerArrayListExtra("remove_list");
                boolean isMediaReset = false;
                for (int i = 0; i < removeList.size(); i++) {
                    //mCurrentList当前的位置
                    int position = -1;
                    for (MusicInfo music : mCurrentList) {
                        position++;
                        if (removeList.get(i) == music.getId()) {
                            mCurrentList.remove(music);
                            position--;
                            //更新正在播放的音乐信息
                            if (mMusicInfo.getId() == removeList.get(i)) {

                                //将当前的音乐信息重新赋值
                                if (mCurrentList.size() > 0) {
                                    if (position + 1 < mCurrentList.size()) {
                                        mMusicInfo = mCurrentList.get(position + 1);
                                    } else {
                                        mMusicInfo = mCurrentList.get(0);
                                    }
                                } else {
                                    mMusicInfo = null;
                                }
                                //停止音乐的播放
                                mMediaPlayer.reset();
                                isMediaReset = true;
                            }
                            //找到符合条件的音乐，跳出内循环，寻找下一首需要删除的音乐
                            break;
                        }
                    }
                }
                //过滤之后,如果列表还有音乐
                if (mMusicInfo != null) {
                    if (isMediaReset) {
                        //开始新的音乐
                        start(true);
                    }
                    //重置mMusicInfo的位置
                    if (mPosition != 0) {
                        for (int i = 0; i < mCurrentList.size(); i++) {
                            if (mCurrentList.get(i).toString().equals(mMusicInfo.toString())) {
                                mPosition = i;
                                break;
                            }
                        }
                    }
                    //更新mList
                    mList.retainAll(mCurrentList);

                } else {
                    //列表中无音乐，将通知关闭
                    cancelNotification();
                    //更新UI
                    mPosition = -1;
                    sendBroadcast(new Intent("UPDATE_UI"));
                }
            } else if (action.equals("ADD_TO_CURRENT_LIST")) {
                List<MusicInfo> list = (List<MusicInfo>) intent.getSerializableExtra("list");
                int count = 0;
                for (MusicInfo music : list) {
                    if (!mCurrentList.contains(music)) {
                        mCurrentList.add(music);
                        mList.add(music);
                        count++;
                    }
                }
                Toast.makeText(getApplicationContext(), "成功添加 " + count + " 首歌曲到当前播放列表", Toast.LENGTH_SHORT).show();
            } else if (action.equals("INSERT_MUSIC")) {
                MusicInfo musicInfo = (MusicInfo) intent.getSerializableExtra("music");
                Intent intent1 = new Intent("MUSIC_LIST_CHANGE");
                //如果加入的音乐已在列表中
                for (int i = 0; i < mCurrentList.size(); i++) {
                    if (mCurrentList.get(i).equals(musicInfo)) {
//                        Log.d(TAG, "onReceive: contain");
//                        Log.d("play_this_next", "remove" + mCurrentList.get(i).getTitle());
                        mCurrentList.remove(i);
                        mList.remove(i);
                        intent1.putExtra("remove_position", i);
                        //如果加入的音乐在mPosition之前
                        if (i < mPosition) {
//                            Log.d(TAG, "onReceive: 之前");
                            mPosition--;
//                            Log.d("play_this_next", "mPosition"+mCurrentList.get(mPosition).getTitle());
                        }
                        break;
                    }
                }
                mCurrentList.add(mPosition + 1, musicInfo);
                intent1.putExtra("insert_position", mPosition + 1);
//                Log.d("insert", "add position:"+mCurrentList.get(mPosition+1));
                mList.add(musicInfo);
                //更新MusicActivity的当前播放列表界面
                sendBroadcast(intent1);
                Toast.makeText(getApplicationContext(), musicInfo.getTitle() + " 作为下一首歌曲播放", Toast.LENGTH_SHORT).show();
            } else if (action.equals("REMOVE_MUSIC")) {
                int position = intent.getIntExtra("position", -1);
//                Log.d(TAG, "onReceive: position"+position);
                removeListItem(position);
            }
        }
    }


    /**
     * 接收用户对每个控件的操作
     */
    class UserActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (mMusicInfo != null) {
                if (action.equals("SEEK_TO")) {
                    //改变音乐进度
                    int to = intent.getIntExtra("progress", 0);
                    mMediaPlayer.seekTo(to);
                } else if (action.equals("PAUSE")) {
                    pause();
                } else if (action.equals("PLAY")) {
                    int position = intent.getIntExtra("position", -1);
                    if (position != -1) {
                        mPosition = position;
                        start(true);
                    } else {
                        start(false);
                    }
                } else if (action.equals("NEXT")) {
                    changeMusic(NEXT_MUSIC);
                } else if (action.equals("PREVIOUS")) {
                    if (mMediaPlayer.getCurrentPosition() < 3000) {
                        changeMusic(PREVIOUS_MUSIC);
                    } else {
                        mMediaPlayer.seekTo(0);
                    }
                } else if (action.equals("VIEW_PAGER_CHANGE_MUSIC")) {
                    mPosition = intent.getIntExtra("position", mPosition);
                    start(true);
                } else if (action.equals("RANDOM_CHANGE")) {
                    //判断是正常播放还是随机播放
                    if (intent.getBooleanExtra("is_random", false)) {
                        //随机播放
                        Collections.shuffle(mCurrentList);

//                        Log.d("random", "随机+++++++++++++++++++++++++");
//                        for (MusicInfo music : mCurrentList) {
//                            Log.d("random", "onReceive: " + music.getTitle());
//                        }

                    } else {
                        //正常播放
                        mCurrentList.clear();
                        mCurrentList.addAll(mList);

//                        Log.d("random", "正常+++++++++++++++++++++++++");
//                        for (MusicInfo music : mCurrentList) {
//                            Log.d("random", "mCurrentList: " + music.getTitle());
//                        }
//                        for (MusicInfo music: mList) {
//                            Log.d("random", "mList: " + music.getTitle());
//                        }
                    }
                    //重置position
                    for (int position = 0; position < mCurrentList.size(); position++) {
                        if (mMusicInfo.toString().equals(mCurrentList.get(position).toString())) {
                            mPosition = position;
                            break;
                        }
                    }
                    //通知MusicActivity更新ViewPager的数据
                    sendBroadcast(new Intent("CURRENT_LIST_SEQUENCE_UPDATE_FINISH"));

                } else if (action.equals("RANDOM_PLAY")) {
                    mPosition = new Random().nextInt(mCurrentList.size()) % (mCurrentList.size() - 0 + 1) + 0;
                    start(true);
                } else if (action.equals("DELETE_SINGLE")) {
                    MusicInfo musicInfo = (MusicInfo) intent.getSerializableExtra("music");
                    int position = 0;
                    boolean isContain = false;
                    for (MusicInfo music : mCurrentList) {
                        position++;
                        if (music.equals(musicInfo)) {
                            isContain = true;
                            break;
                        }
                    }
                    if (isContain) {
                        removeListItem(position);
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "请先选择一首音乐", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 监听app退出事件
     */
    class SystemExitReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //finish掉所有activity
            ExitApplication.getInstance().systemExit();
            stopService(new Intent(getApplicationContext(), PlayService.class));
        }
    }

    /**
     * 监听耳机插拔事件
     */
    class HeadsetPlugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                if (intent.getIntExtra("state", 0) == 0) {
                    //当耳机拔出
                    if (mMediaPlayer.isPlaying()) {
                        pause();
                    }
                }
            }
        }
    }


}
