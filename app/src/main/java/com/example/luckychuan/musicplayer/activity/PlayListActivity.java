package com.example.luckychuan.musicplayer.activity;


import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.adapter.MusicInfoRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.database.MusicDatabase;
import com.example.luckychuan.musicplayer.fragment.MusicBottomFragment;
import com.example.luckychuan.musicplayer.fragment.PlaylistFragment;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.DeleteUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 最近播放，最常播放，我的最爱，自定义列表的音乐
 */
public class PlayListActivity extends AppCompatActivity implements RecyclerViewClickListener {

    private static final String TAG = "PlayListActivity";

    //点击添加音乐按钮启动SelectActivity,并且返回选中的要添加的音乐
    public static final int SELECT_ACTIVITY_RESULT = 1;

    private Toolbar mToolbar;
    //当列表为空时的提示text
    private TextView mEmptyText;
    private MusicInfoRecyclerViewAdapter mAdapter;
    private List<MusicInfo> mList;

    //当前播放列表
    private int mCurrentPlayList;

    private UIUpdateReceiver mUIUpdateReceiver;

    private DeleteUtil mDeleteUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExitApplication.getInstance().addActivity(this);
        setContentView(R.layout.activity_playlist);

        mDeleteUtil = new DeleteUtil(this);

        //设置Toolbar
        Intent intent = getIntent();
        mCurrentPlayList = intent.getIntExtra("play_list_id", 0);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(intent.getStringExtra("play_list_name"));
        invalidateOptionsMenu();
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //音乐列表
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.music_recyclerView);

        mEmptyText = (TextView) findViewById(R.id.list_empty);
        //初始化RecyclerView和Adapter
        mList = new ArrayList<>();
        mAdapter = new MusicInfoRecyclerViewAdapter(mList);
        mAdapter.setItemClickListener(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        //注册更新列表数据广播
        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        registerReceiver(mUIUpdateReceiver, filter);


        //底部音乐栏fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment1 = new MusicBottomFragment();
        transaction.add(R.id.bottom_fragment_play_list, fragment1);
        transaction.commit();

        //底部音乐栏点击事件
        LinearLayout mMusicBottom = (LinearLayout) findViewById(R.id.bottom_fragment_play_list);
        mMusicBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayService.getMusicInfo() != null && !mDeleteUtil.isOnDeleteMode()) {
                    Intent intent = new Intent(PlayListActivity.this, MusicActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.translate_in, R.anim.fade_out);
                }
            }
        });

    }

    private void updateList() {
        mList.clear();
        if (mCurrentPlayList == PlaylistFragment.PLAY_MOST) {
            mList.addAll(MusicDatabase.getInstance(this).queryAllMusic("play_most", "count desc", "50"));
        } else if (mCurrentPlayList == PlaylistFragment.LATEST_PLAY) {
            mList.addAll(MusicDatabase.getInstance(this).queryAllMusic("latest_play", "_id desc", "50"));
        } else if (mCurrentPlayList == PlaylistFragment.FAVORITE) {
            mList.addAll(MusicDatabase.getInstance(this).queryAllMusic("favorite", "_id asc", null));
        } else {
            //用户创建的播放列表
            mList.addAll(MediaStoreManager.getInstance(this).queryMusicDataInPlaylist(mCurrentPlayList));
        }
        if (mList.size() == 0) {
            mEmptyText.setVisibility(View.VISIBLE);
        } else {
            mEmptyText.setVisibility(View.GONE);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        //更新列表
        updateList();
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        ExitApplication.getInstance().removeActivity(this);
        unregisterReceiver(mUIUpdateReceiver);
        Log.d("onDestroy", TAG + "onDestroy: ");
    }

    @Override
    public void onItemClick(int position) {
        if (!mDeleteUtil.isOnDeleteMode()) {
            final int finalPosition = position;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //将当前音乐列表发送给service，播放选中的音乐
                    Intent intent = new Intent(PlayListActivity.this, PlayService.class);
                    intent.putExtra("position", finalPosition);
                    intent.putExtra("list", (Serializable) mList);
                    startService(intent);
                }
            }, 200);
        } else {
            //删除模式，选择将要删除的音乐
            Integer clickId = mList.get(position).getId();
            mDeleteUtil.selectItem(clickId);
            mToolbar.setTitle("已选择： " + mDeleteUtil.getDeleteListSize());
            mAdapter.notifyDataSetChanged();
            //当全部item都放弃选择时退出删除模式
            if (mDeleteUtil.getDeleteListSize() == 0) {
                onBackPressed();
            }
        }
    }

    @Override
    public void onItemLongClick(int position) {
        //启动删除模式
        if (!mDeleteUtil.isOnDeleteMode()) {
            mDeleteUtil.startDeleteMode(DeleteUtil.MUSIC_INFO);
            invalidateOptionsMenu();
        }
        if (mDeleteUtil.isOnDeleteMode()) {
            //选择当前点击的item进入待删除列表
            onItemClick(position);
        }
    }

    @Override
    public void onItemClick(int position, ActivityOptions options) {

    }

    /**
     * 进入和退出删除模式时改变Toolbar上的菜单
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mDeleteUtil.isOnDeleteMode()) {
            getMenuInflater().inflate(R.menu.menu_remove, menu);
        } else {
            if (mCurrentPlayList != PlaylistFragment.LATEST_PLAY && mCurrentPlayList != PlaylistFragment.PLAY_MOST) {
                getMenuInflater().inflate(R.menu.menu_add_music, menu);
            }
            menu.add(Menu.NONE,R.id.random_play,0,"随机播放所有歌曲");
            menu.add(Menu.NONE, R.id.add_to_current_list, 1, "添加到当前播放队列");
        }
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            //全选按钮
            case R.id.select_all:

                if(mList.size() == mDeleteUtil.getDeleteListSize()){
                    mDeleteUtil.clearDeleteList();
                }else{
                    List<Integer> idList = new ArrayList<>();
                    for (MusicInfo music : mList
                            ) {
                        idList.add((Integer)music.getId());
                    }
                    mDeleteUtil.selectAll(idList);
                }
                mAdapter.notifyDataSetChanged();
                mToolbar.setTitle("已选择： " + mDeleteUtil.getDeleteListSize());
                break;

            //确认删除按钮
            case R.id.remove:

                menuRemove();
                break;
            case R.id.add_menu:

                Intent intent = new Intent(this, SelectMusicActivity.class);
                intent.putExtra("list", (Serializable) mList);
                startActivityForResult(intent, SELECT_ACTIVITY_RESULT);
                break;

  //          case R.id.
        }
        return true;
    }

    private void menuRemove() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PlayListActivity.this);
        builder.setTitle("移除：");
        builder.setMessage("是否从列表中移除 " + mDeleteUtil.getDeleteListSize() + " 首曲目");
        builder.setCancelable(false);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mCurrentPlayList == PlaylistFragment.LATEST_PLAY) {
                    MusicDatabase.getInstance(PlayListActivity.this).deleteListData("latest_play", mDeleteUtil.getDeleteList());
                } else if (mCurrentPlayList == PlaylistFragment.PLAY_MOST) {
                    MusicDatabase.getInstance(PlayListActivity.this).deleteListData("play_most", mDeleteUtil.getDeleteList());
                } else if (mCurrentPlayList == PlaylistFragment.FAVORITE) {
                    MusicDatabase.getInstance(PlayListActivity.this).deleteListData("favorite", mDeleteUtil.getDeleteList());
                } else {
                    MediaStoreManager.getInstance(PlayListActivity.this).deleteMusicFromPlaylist(mCurrentPlayList, mDeleteUtil.getDeleteList());
                }
                //更新mList的数据以及刷新mAdapter
                for (int id: mDeleteUtil.getDeleteList()) {
                    for (int position = 0; position < mList.size(); position++) {
                        if (mList.get(position).getId() == id) {
                            mList.remove(position);
                            mAdapter.notifyItemRemoved(position);
                            position--;
                            break;
                        }
                    }
                }
                sendBroadcast(new Intent("UPDATE_THREE_PLAYLIST"));
                //删除完成，退出删除模式
                mDeleteUtil.cancelDeleteMode();
                invalidateOptionsMenu();
                mToolbar.setTitle(getIntent().getStringExtra("play_list_name"));
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onBackPressed();
            }
        });
        builder.create().show();
    }

    @Override
    public void onBackPressed() {
        if (mDeleteUtil.isOnDeleteMode()) {
            mDeleteUtil.cancelDeleteMode();
            mToolbar.setTitle(getIntent().getStringExtra("play_list_name"));
            invalidateOptionsMenu();
            mAdapter.notifyDataSetChanged();
        } else {
            super.onBackPressed();
        }
    }


    /**
     * 得到选中的音乐数据
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        HashMap<Integer, MusicInfo> map = (HashMap<Integer, MusicInfo>) data.getSerializableExtra("result");
        if (map.size() != 0) {
            Set<Map.Entry<Integer, MusicInfo>> sets = map.entrySet();
            List<MusicInfo> list = new ArrayList<>();
            for (Map.Entry entry : sets) {
                list.add((MusicInfo) entry.getValue());
            }
            //加入到数据库中
            if (mCurrentPlayList == PlaylistFragment.FAVORITE) {
                MusicDatabase.getInstance(PlayListActivity.this).insertListMusic("favorite", list);
            } else {
                MediaStoreManager.getInstance(PlayListActivity.this).insertListMusicToPlaylist(PlayListActivity.this, mCurrentPlayList, list);
            }
            Toast.makeText(PlayListActivity.this, "成功添加" + list.size() + "首音乐", Toast.LENGTH_SHORT).show();
        }
    }

    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
                    //正在播放的position高亮
                    mAdapter.notifyDataSetChanged();
        }
    }

}
