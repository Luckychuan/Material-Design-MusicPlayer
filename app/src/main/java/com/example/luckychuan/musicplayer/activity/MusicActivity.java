package com.example.luckychuan.musicplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.adapter.ImageFragmentPagerAdapter;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.database.MusicDatabase;
import com.example.luckychuan.musicplayer.fragment.CurrentMusicListFragment;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.ImageUtil;
import com.example.luckychuan.musicplayer.util.MenuUtil;

import java.util.List;

/**
 * 正在播放的音乐页面
 * 通过点击通知，MusicBottomFragment点击启动
 */
public class MusicActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final int VIBRANT = 0;
    private static final int MUTED = 1;
    private static final int LIGHT_MUTED = 2;

    private static final String TAG = "MusicActivity";
    private ViewPager mViewPager;
    private ImageFragmentPagerAdapter mPagerAdapter;

    private Button mPlayButton;
    private ImageButton mRandomButton;
    private ImageButton mLoopButton;

    private TextView mMusicName;
    private TextView mArtist;
    private TextView mCurrentTime;
    private TextView mMaxTime;

    private CurrentMusicListFragment mCurrentListFragment;

    private ImageButton mFavoriteButton;

    private SeekBar mSeekBar;
    //更新音乐进度的定时器
    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    private UIUpdateReceiver mUIUpdateReceiver;
    private OnCurrentListChangeListener mOnCurrentListChangeListener;

    //动态添加item
    private SubMenu mSubMenu;
    //加入到播放列表
    private List<PlaylistInfo> mPlaylistInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExitApplication.getInstance().addActivity(this);
        setContentView(R.layout.activity_music);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar1);
        toolbar.setTitle("");
        invalidateOptionsMenu();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        initViewPager();

        //       mAlbum = (ImageView) findViewById(R.id.album_main);
        mMusicName = (TextView) findViewById(R.id.musicName_main);
        mArtist = (TextView) findViewById(R.id.artist_main);
        mCurrentTime = (TextView) findViewById(R.id.current_time);
        mMaxTime = (TextView) findViewById(R.id.max_time);

//        Button addButton = (Button) findViewById(R.id.add);
        mFavoriteButton = (ImageButton) findViewById(R.id.favorite);
//        Button deleteButton = (Button) findViewById(R.id.delete);
        //若当前播放的音乐存在我的最爱列表中
        if (MusicDatabase.getInstance(MusicActivity.this).querySingleMusic("favorite", "musicId=?", new String[]{PlayService.getMusicInfo().getId() + ""}, null) == null) {
            mFavoriteButton.setActivated(false);
        } else {
            mFavoriteButton.setActivated(true);
        }
//        addButton.setOnClickListener(this);
        mFavoriteButton.setOnClickListener(this);
//        deleteButton.setOnClickListener(this);

        mPlayButton = (Button) findViewById(R.id.play_main);
        ((ImageButton) findViewById(R.id.next_main)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.previous_main)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.current_list_button)).setOnClickListener(this);
        mRandomButton = (ImageButton) findViewById(R.id.randem_main);
        mLoopButton = (ImageButton) findViewById(R.id.loop_main);
        mPlayButton.setOnClickListener(this);
        mRandomButton.setOnClickListener(this);
        mLoopButton.setOnClickListener(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mPlayButton.setVisibility(View.VISIBLE);
                mPlayButton.startAnimation(AnimationUtils.loadAnimation(MusicActivity.this, R.anim.fab_in));
            }
        }, 300);


        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        //使SeekBar宽度全屏
        mSeekBar.setPadding(0, 0, 0, 0);
        mSeekBar.setOnSeekBarChangeListener(this);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        mCurrentListFragment = new CurrentMusicListFragment();
        transaction.add(R.id.fragment_layout_main, mCurrentListFragment);
        transaction.hide(mCurrentListFragment);
        transaction.commit();

        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        filter.addAction("UPDATE_PLAY_BUTTON");
        registerReceiver(mUIUpdateReceiver, filter);


        mOnCurrentListChangeListener = new OnCurrentListChangeListener();
        IntentFilter filter1 = new IntentFilter("ON_SINGLE_MUSIC_DELETE");
        filter1.addAction("CURRENT_LIST_SEQUENCE_UPDATE_FINISH");
        filter1.addAction("MUSIC_LIST_CHANGE");
        registerReceiver(mOnCurrentListChangeListener, filter1);

        //初始化UI
        changeNewMusicUI(PlayService.getMusicInfo());

        //从存储中获得随机和循环变量
        SharedPreferences randomAndLoopSP = getSharedPreferences("random_and_loop", MODE_PRIVATE);
        boolean isRandom = randomAndLoopSP.getBoolean("random", false);
        boolean isLoop = randomAndLoopSP.getBoolean("loop", false);
        if (isRandom) {
            mRandomButton.setActivated(true);
        } else {
            mRandomButton.setActivated(false);
        }
        if (isLoop) {
            mLoopButton.setActivated(true);
        } else {
            mLoopButton.setActivated(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mSubMenu = menu.addSubMenu(Menu.NONE, R.id.add_to_playlist, 1, "添加到播放列表");
        getMenuInflater().inflate(R.menu.menu_music, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            case R.id.delete:
//                MenuUtil.deleteMusic(this, PlayService.getMusicInfo(), null);
                break;

            case R.id.music_in_album_activity:
                Intent intent1 = new Intent(this, MusicInAlbumActivity.class);
                intent1.putExtra("album", MediaStoreManager.getInstance(this).queryAlbumData(PlayService.getMusicInfo().getAlbumId()));
                startActivity(intent1);
                break;

            case R.id.music_in_artist_activity:
                Intent intent2 = new Intent(this, MusicInArtistActivity.class);
                intent2.putExtra("artist", MediaStoreManager.getInstance(this).queryArtistData(PlayService.getMusicInfo().getArtist()));
                this.startActivity(intent2);
                break;

            case R.id.edit_music_info:
                Intent intent3 = new Intent(this, EditInfoActivity.class);
                intent3.putExtra("music", PlayService.getMusicInfo());
                startActivity(intent3);
                break;

            case R.id.add_to_playlist:
                mSubMenu.removeGroup(R.id.add_to_playlist);
                mPlaylistInfoList = MediaStoreManager.getInstance(this).queryPlaylistData();
                mSubMenu.add(R.id.add_to_playlist, 0, 0, "创建新的播放列表");
//                            Log.d(TAG, "onMenuItemClick: add");
                for (int i = 0; i < mPlaylistInfoList.size(); i++) {
                    mSubMenu.add(R.id.add_to_playlist, i + 1, i + 1, mPlaylistInfoList.get(i).getName());

                }
                break;
        }

        int groupId = item.getGroupId();
        if (groupId == R.id.add_to_playlist) {
            if (id == 0) {
                //创建新的播放列表
                MenuUtil.editPlaylist(this, new MenuUtil.OnPlaylistDialogConfirmFinishListener() {
                    @Override
                    public void OnPlaylistDialogConfirmFinish(String editName) {
                        MediaStoreManager.getInstance(MusicActivity.this).addPlaylistData(editName);
                        //刚创建的播放列表的id
                        int playlistId = MediaStoreManager.getInstance(MusicActivity.this).queryNewPlaylistId();
                        MediaStoreManager.getInstance(MusicActivity.this).insertMusicToPlaylist(MusicActivity.this, playlistId, PlayService.getMusicInfo().getId());
                        sendBroadcast(new Intent("UPDATE_PLAYLIST"));
                    }
                });
            } else {
//                            Log.d(TAG, "onMenuItemClick: " + item.getItemId());
                //添加到现有的播放列表
                MediaStoreManager.getInstance(this).insertMusicToPlaylist(this, mPlaylistInfoList.get(id - 1).getId(), PlayService.getMusicInfo().getId());
                sendBroadcast(new Intent("UPDATE_PLAYLIST"));
            }
        }

        return super.onOptionsItemSelected(item);
    }


    private void initViewPager() {
        //由于ViewPager不能自适应高度，因此这里要获取设备屏幕的宽度，然后设置ViewPager的高度为设备屏幕的宽度
        int screenWidth = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
        mViewPager = (ViewPager) findViewById(R.id.main_viewPager);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mViewPager.getLayoutParams();
        params.height = screenWidth;
        mViewPager.setLayoutParams(params);

        mPagerAdapter = new ImageFragmentPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            private int originItem;
            //判断是否是用户人为滑动
            private boolean isUser;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    isUser = true;
                    originItem = mViewPager.getCurrentItem();
                }
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    if (mViewPager.getCurrentItem() != originItem && isUser) {
                        //切换歌曲
                        Intent intent = new Intent("VIEW_PAGER_CHANGE_MUSIC");
                        intent.putExtra("position", mViewPager.getCurrentItem());
                        sendBroadcast(intent);
                        //还原默认值
                        isUser = false;
                    }

                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mCurrentListFragment.isVisible()) {
            //将音乐列表隐藏
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fragment_out, R.anim.fragment_out);
            transaction.hide(mCurrentListFragment);
            transaction.commit();
        } else {
            //当前Activity从通知启动，点击返回键时启动MainActivity
            if (ExitApplication.getInstance().size() == 1) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //开始更新音乐进度的定时器
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (PlayService.getMusicInfo() != null) {
                    int currentPosition = PlayService.getCurrentPosition();
                    mCurrentTime.setText(formatDuration(currentPosition));
                    mSeekBar.setProgress(currentPosition);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.post(mRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //将定时器取消
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.translate_out);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUIUpdateReceiver);
        unregisterReceiver(mOnCurrentListChangeListener);
        ExitApplication.getInstance().removeActivity(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_main:

                Intent intent;
                if (PlayService.isPlaying()) {
                    intent = new Intent("PAUSE");
                } else {
                    intent = new Intent("PLAY");
                }
                mPlayButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fab_click));
                sendBroadcast(intent);

                break;
            case R.id.next_main:

//                int position = mViewPager.getCurrentItem()+1;
//                if(position >= mCurrentList.size()){
//                    position = 0;
//                }
//                mViewPager.setCurrentItem(position);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadcast(new Intent("NEXT"));
                    }
                }, 200);

                break;
            case R.id.previous_main:

//                //当播放进度小于等于3秒时上一首，大于3秒上一首
//                if(PlayService.getCurrentPosition() <= 3000){
//                    int position1 = mViewPager.getCurrentItem() -1;
//                    if(position1 < 0){
//                        position1 = mCurrentList.size() -1;
//                    }
//                    mViewPager.setCurrentItem(position1);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadcast(new Intent("PREVIOUS"));
                    }
                }, 200);

//                }else{
//                    Intent intent1 = new Intent("SEEK_TO");
//                    intent1.putExtra("progress",0);
//                    sendBroadcast(intent1);
//                }

                break;
            case R.id.randem_main:
                onRandomButtonClick();
                break;
            case R.id.loop_main:
                onLoopButtonClick();
                break;
            case R.id.current_list_button:
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.fragment_in, R.anim.fragment_out);
                transaction.show(mCurrentListFragment);
                transaction.commit();
                break;

            case R.id.favorite:
                onFavoriteButtonClick();
                break;
        }
    }

    private void onFavoriteButtonClick() {
        if (mFavoriteButton.isActivated()) {
            //移除出我的最爱列表
            MusicDatabase.getInstance(MusicActivity.this).deleteSingle("favorite", PlayService.getMusicInfo().getId());
            mFavoriteButton.setActivated(false);
            Toast toast = Toast.makeText(MusicActivity.this, "已从 我的最爱 移除", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 400);
            toast.show();
        } else {
            //添加出我的最爱列表
            MusicDatabase.getInstance(MusicActivity.this).insertFavorite(PlayService.getMusicInfo());
            mFavoriteButton.setActivated(true);
            Toast toast = Toast.makeText(MusicActivity.this, "已添加到 我的最爱", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 400);
            toast.show();
        }
        sendBroadcast(new Intent("UPDATE_THREE_PLAYLIST"));
    }

    /**
     * 设置背景颜色
     *
     * @param bitmap 专辑封面
     */
    private void displayPalette(Bitmap bitmap) {
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.play_page_default_cover);
        }
        Palette.Builder builder = Palette.from(bitmap);
        builder.generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                int intSwatch = 0;
                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                Palette.Swatch mutedSwatch = palette.getMutedSwatch();
                Palette.Swatch lightMutedSwatch = palette.getLightMutedSwatch();
                int[] population = new int[3];
                if (vibrantSwatch != null) {
                    population[VIBRANT] = vibrantSwatch.getPopulation();
//                    Log.d("palette", "onGenerated: vibrant" + vibrantSwatch.getPopulation());
                }
                if (mutedSwatch != null) {
                    population[MUTED] = mutedSwatch.getPopulation();
//                    Log.d("palette", "onGenerated: " + "mutedSwatch:" + mutedSwatch.getPopulation());
                }
                if (lightMutedSwatch != null) {
                    population[LIGHT_MUTED] = lightMutedSwatch.getPopulation();
//                    Log.d("palette", "onGenerated: lightMuted" + lightMutedSwatch.getPopulation());

                    int red = lightMutedSwatch.getRgb() >> 16 & 0xFF;
                    int green = lightMutedSwatch.getRgb() >> 8 & 0xFF;
                    int blue = lightMutedSwatch.getRgb() & 0xFF;
//                    Log.d("palette", "red:" + red + "  green:" + green + "  blue:" + blue);
                }
                //找出population最大的下标
                int select = VIBRANT;
                if (population[MUTED] * 0.5 > population[select]) {
                    select = MUTED;
                }
                if (population[select] < 50 && population[LIGHT_MUTED] * 0.3 > population[select]) {
                    int red = lightMutedSwatch.getRgb() >> 16 & 0xFF;
                    int green = lightMutedSwatch.getRgb() >> 8 & 0xFF;
                    int blue = lightMutedSwatch.getRgb() & 0xFF;
                    int max = red>green?(red>blue?red:blue):(green>blue?green:blue);
                    int min =  red<green?(red<blue?red:blue):(green<blue?green:blue);
                    if (max-min>40) {
                        select = LIGHT_MUTED;
                    }else if(population[select]<10){
                        select = LIGHT_MUTED;
                    }
                }
//                                 for (int i = 1; i < 3; i++) {
//                                     if(i == LIGHT_MUTED){
//
//                                     }else{
//                                         if (population[maxIndex] < population[i] * 0.3) {
//                                             maxIndex = i;
//                                         }
//                                     }
//                                 }
                    Palette.Swatch swatch = null;
                    switch (select) {
                        case VIBRANT:
                            swatch = vibrantSwatch;
                            intSwatch = VIBRANT;
                            Log.d("palette", "使用VIBRANT");
                            break;
                        case MUTED:
                            swatch = mutedSwatch;
                            intSwatch = MUTED;
                            Log.d("palette", "使用MUTED");
                            break;
                        case LIGHT_MUTED:
                            swatch = lightMutedSwatch;
                            intSwatch = LIGHT_MUTED;
                            Log.d("palette", "使用LIGHT_MUTED");

                            break;
                    }
                    if (swatch != null) {
                        //SeekBar的样式
                        ClipDrawable clipDrawable = new ClipDrawable(new ColorDrawable(colorSeekBarProgress(swatch.getRgb(), intSwatch)), Gravity.START, ClipDrawable.HORIZONTAL);
                        GradientDrawable gradientDrawable = (GradientDrawable) getDrawable(R.drawable.gradient_seek_bar);
                        Drawable[] drawable = {new ColorDrawable(colorSeekBarBackground(swatch.getRgb(), intSwatch)), clipDrawable, gradientDrawable};
                        LayerDrawable ld = new LayerDrawable(drawable);
                        ld.setDrawableByLayerId(android.R.id.progress, ld.getDrawable(2));
                        ld.setDrawableByLayerId(android.R.id.progress, ld.getDrawable(1));
                        ld.setDrawableByLayerId(android.R.id.background, ld.getDrawable(0));
                        mSeekBar.setProgressDrawable(ld);
                        mSeekBar.setMax(PlayService.getDuration());
                        mSeekBar.setProgress(PlayService.getCurrentPosition());

                        ((RelativeLayout) findViewById(R.id.music_view_main_layout)).setBackgroundColor(colorMain(swatch.getRgb(), intSwatch));
                        mCurrentTime.setTextColor(swatch.getTitleTextColor());
                        mMaxTime.setTextColor(swatch.getTitleTextColor());
                        mMusicName.setTextColor(swatch.getBodyTextColor());
                        mArtist.setTextColor(swatch.getTitleTextColor());
                    }
                }
        });
    }


    private int colorMain(int RGBValues, int swatch) {
        int red = RGBValues >> 16 & 0xFF;
        int green = RGBValues >> 8 & 0xFF;
        int blue = RGBValues & 0xFF;

        Log.d(TAG, "colorMain: " + swatch);
        if (swatch == LIGHT_MUTED) {
            red = (int) Math.floor(red * (1 - 0.2));
            green = (int) Math.floor(green * (1 - 0.2));
            blue = (int) Math.floor(blue * (1 - 0.2));
        } else {
            red = (int) Math.floor(red * (1 - 0.1));
            green = (int) Math.floor(green * (1 - 0.1));
            blue = (int) Math.floor(blue * (1 - 0.1));
        }
        return Color.rgb(red, green, blue);
    }

    private int colorSeekBarBackground(int RGBValues, int swatch) {
        int red = RGBValues >> 16 & 0xFF;
        int green = RGBValues >> 8 & 0xFF;
        int blue = RGBValues & 0xFF;
        if (swatch == LIGHT_MUTED) {
            red = (int) Math.floor(red * (1 - 0.25));
            green = (int) Math.floor(green * (1 - 0.25));
            blue = (int) Math.floor(blue * (1 - 0.25));
        } else {
            red = (int) Math.floor(red * (1 - 0.15));
            green = (int) Math.floor(green * (1 - 0.15));
            blue = (int) Math.floor(blue * (1 - 0.15));
        }

        return Color.rgb(red, green, blue);
    }

    private int colorSeekBarProgress(int RGBValues, int swatch) {
        int red = RGBValues >> 16 & 0xFF;
        int green = RGBValues >> 8 & 0xFF;
        int blue = RGBValues & 0xFF;
        int redPlus;
        int greenPlus;
        int bluePlus;
        if (swatch == LIGHT_MUTED) {
            redPlus = (int) Math.floor(red * (1 + 0.01));
            greenPlus = (int) Math.floor(green * (1 + 0.01));
            bluePlus = (int) Math.floor(blue * (1 + 0.01));
        } else if (swatch == MUTED) {
            redPlus = (int) Math.floor(red * (1 + 0.3));
            greenPlus = (int) Math.floor(green * (1 + 0.3));
            bluePlus = (int) Math.floor(blue * (1 + 0.3));
        } else {
            redPlus = (int) Math.floor(red * (1 + 0.2));
            greenPlus = (int) Math.floor(green * (1 + 0.2));
            bluePlus = (int) Math.floor(blue * (1 + 0.2));
        }
//        Log.d(TAG, "colorLight: red" + redPlus);
//        Log.d(TAG, "colorLight: green" + greenPlus);
//        Log.d(TAG, "colorLight: blue" + bluePlus);

        //防止颜色超界
        if (redPlus > 255) {
            redPlus = 255;
        }
        if (greenPlus > 255) {
            greenPlus = 255;
        }
        if (bluePlus > 255) {
            bluePlus = 255;
        }

        //防止进度条亮度太低
        if (redPlus < 20) {
            redPlus = (int) Math.floor(red * 5);
            //           Log.d(TAG, "a red" + redPlus);
        }
        if (redPlus < 40) {
            redPlus = (int) Math.floor(red * 2);
            //           Log.d(TAG, "a red" + redPlus);
        }


        return Color.rgb(redPlus, greenPlus, bluePlus);
    }

    /**
     * 修改新音乐的UI
     */
    private void changeNewMusicUI(MusicInfo music) {
        mViewPager.setCurrentItem(PlayService.getPosition());
        ImageUtil.decodeBitmap(this, PlayService.getMusicInfo().getAlbumUri(), new ImageUtil.OnDecodeFinishListener() {
            @Override
            public void onDecodeFinish(Bitmap bitmap) {
                displayPalette(bitmap);
            }
        });
        if (PlayService.isPlaying()) {
            if (mPlayButton.isActivated()) {
                mPlayButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fab_click));
            }
            mPlayButton.setActivated(false);
        } else {
            mPlayButton.setActivated(true);
        }
//        ImageUtil.displayImage(MusicActivity.this, music.getAlbumUri(), mAlbum);
//        displayPalette(((BitmapDrawable) mAlbum.getDrawable()).getBitmap());
        mArtist.setText(music.getArtist());
        mMusicName.setText(music.getTitle());
        mMaxTime.setText(formatDuration(PlayService.getDuration()));
        mCurrentTime.setText(formatDuration(PlayService.getCurrentPosition()));
//        Log.d(TAG, "changeNewMusicUI: current_time" + mCurrentTime.getText());
//        Log.d(TAG, "changeNewMusicUI: progress" + mSeekBar.getProgress());
        //若当前播放的音乐存在我的最爱列表中
        if (MusicDatabase.getInstance(MusicActivity.this).querySingleMusic("favorite", "musicId=?", new String[]{PlayService.getMusicInfo().getId() + ""}, null) == null) {
            mFavoriteButton.setActivated(false);
        } else {
            mFavoriteButton.setActivated(true);
        }

    }

    private void onLoopButtonClick() {
        boolean isLoop = getSharedPreferences("random_and_loop", MODE_PRIVATE).getBoolean("loop", false);
        if (isLoop) {
            mLoopButton.setActivated(false);
            isLoop = false;
            Toast toast = Toast.makeText(MusicActivity.this, "全部循环", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 400);
            toast.show();
        } else {
            mLoopButton.setActivated(true);
            isLoop = true;
            Toast toast = Toast.makeText(MusicActivity.this, "单曲循环", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 400);
            toast.show();
        }
        SharedPreferences.Editor editor = getSharedPreferences("random_and_loop", MODE_PRIVATE).edit();
        editor.putBoolean("loop", isLoop);
        editor.commit();
    }

    private void onRandomButtonClick() {
        boolean isRandom = getSharedPreferences("random_and_loop", MODE_PRIVATE).getBoolean("random", false);
        if (isRandom) {
            mRandomButton.setActivated(false);
            Toast toast = Toast.makeText(MusicActivity.this, "顺序播放", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 400);
            toast.show();
            isRandom = false;
        } else {
            mRandomButton.setActivated(true);
            Toast toast = Toast.makeText(MusicActivity.this, "随机播放", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 400);
            toast.show();
            isRandom = true;
        }

//        Log.d("random","origin_item p:"+mViewPager.getCurrentItem());
//        Log.d("random", "origin+++++++++++++++++++++++++");
//        for (MusicInfo music : mCurrentList) {
//            Log.d("random", "onReceive: " + music.getTitle());
//        }

        //发送广播到Service，切换列表
        Intent intent = new Intent("RANDOM_CHANGE");
        intent.putExtra("is_random", isRandom);
        sendBroadcast(intent);

        SharedPreferences.Editor editor = getSharedPreferences("random_and_loop", MODE_PRIVATE).edit();
        editor.putBoolean("random", isRandom);
        editor.commit();
    }


    //实现OnSeekBarChangeListener接口的方法
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mCurrentTime.setText(formatDuration(progress));
        mSeekBar.setProgress(progress);
        if (fromUser) {
            //发送广播调整音乐进度
            Intent intent = new Intent("SEEK_TO");
            intent.putExtra("progress", progress);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    /**
     * 将duration转换成分秒
     */

    private String formatDuration(int duration) {
        //将毫秒转换成秒
        int time = duration / 1000;
        int minute = time / 60;
        int second = time % 60;
        return String.format("%2d:%02d", minute, second);
    }


    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_UI")) {
                MusicInfo music = PlayService.getMusicInfo();
                //仅当音乐变化时更新UI
                if (music != null) {
                    //当音乐不变时不需要修改UI
                    if (!mMusicName.getText().equals(music.getTitle()) || !mArtist.getText().equals(music.getArtist()) || mMaxTime.getText().equals(formatDuration(music.getDuration()))) {
                        changeNewMusicUI(music);
                    }
                }
            } else if (intent.getAction().equals("UPDATE_PLAY_BUTTON")) {
                if (PlayService.isPlaying()) {
                    mPlayButton.setActivated(false);
                } else {
                    mPlayButton.setActivated(true);
                }
            }
        }
    }


    class OnCurrentListChangeListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("ON_SINGLE_MUSIC_DELETE")) {
                if (PlayService.getMusicInfo() == null) {
                    finish();
                }
                mPagerAdapter.notifyDataSetChanged();
                if (intent.getBooleanExtra("is_mMusicInfo", false)) {
                    mViewPager.setCurrentItem(PlayService.getPosition());
                } else {
                    mViewPager.setCurrentItem(PlayService.getPosition(), false);
                }
                mCurrentListFragment.notifyDataSetChanged();
            } else if (action.equals("CURRENT_LIST_SEQUENCE_UPDATE_FINISH")) {
                mPagerAdapter.notifyDataSetChanged();
                mViewPager.setCurrentItem(PlayService.getPosition(), false);
                mCurrentListFragment.notifyDataSetChanged();
            } else if (action.equals("MUSIC_LIST_CHANGE")) {
                mPagerAdapter.notifyDataSetChanged();
                mViewPager.setCurrentItem(PlayService.getPosition(), false);
                int removePosition = intent.getIntExtra("remove_position", -1);
                if (removePosition != -1) {
                    mCurrentListFragment.notifyItemRemoved(removePosition);
                }
                int insertPosition = intent.getIntExtra("insert_position", -1);
                if (insertPosition != -1) {
                    mCurrentListFragment.notifyItemInserted(insertPosition);
                }
            }

        }
    }

}


