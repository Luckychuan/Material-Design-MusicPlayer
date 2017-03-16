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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.example.luckychuan.musicplayer.adapter.MusicInfoRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.fragment.MusicBottomFragment;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.Artist;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.DeleteUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * 点击歌手列表的item后进入每个专辑的音乐列表
 */
public class MusicInArtistActivity extends AppCompatActivity implements RecyclerViewClickListener {

    private Toolbar mToolbar;
    private MusicInfoRecyclerViewAdapter mAdapter;
    private List<MusicInfo> mList;
    private Artist mArtist;

    //删除item的工具类
    private  DeleteUtil mDeleteUtil;

    private UIUpdateReceiver mUIUpdateReceiver;
    private DeleteFinishReceiver mDeleteFinishReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExitApplication.getInstance().addActivity(this);
        setContentView(R.layout.activity_playlist);

        mDeleteUtil = new DeleteUtil(this);

        mArtist = (Artist) getIntent().getSerializableExtra("artist");

        //设置Toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(mArtist.getName());
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

        mDeleteFinishReceiver = new DeleteFinishReceiver();
        IntentFilter filter2 = new IntentFilter("DELETE_FINISH");
        registerReceiver(mDeleteFinishReceiver, filter2);

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
                    Intent intent = new Intent(MusicInArtistActivity.this, MusicActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.translate_in, R.anim.fade_out);

                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mList.clear();
        mList.addAll(MediaStoreManager.getInstance(this).queryMusicDataInArtist(mArtist.getId()));
        if (mList.size() == 0) {
            finish();
        }
        mAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUIUpdateReceiver);
        unregisterReceiver(mDeleteFinishReceiver);
        ExitApplication.getInstance().removeActivity(this);
    }

    @Override
    public void onItemClick(int position) {
        if (!mDeleteUtil.isOnDeleteMode()) {
            final int finalPosition = position;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //将当前音乐列表发送给service，播放选中的音乐
                    Intent intent = new Intent(MusicInArtistActivity.this, PlayService.class);
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
            getMenuInflater().inflate(R.menu.menu_delete, menu);
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
            //删除按钮
            case R.id.delete_menu:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("删除：");
                builder.setMessage("是否从设备中删除 " + mDeleteUtil.getDeleteListSize() + " 首曲目");
                builder.setCancelable(false);
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //更新mList
                        //当全部删除时
                        if (mDeleteUtil.getDeleteListSize() == mList.size()) {
                            mAdapter.notifyItemRangeRemoved(0, mList.size());
                            mList.clear();
                        } else {
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
                        }
                        //删除文件
                        mDeleteUtil.confirmDelete();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                    }
                });
                builder.create().show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDeleteUtil.isOnDeleteMode()) {
            mDeleteUtil.cancelDeleteMode();
            mToolbar.setTitle(mArtist.getName());
            invalidateOptionsMenu();
            mAdapter.notifyDataSetChanged();
        } else {
            super.onBackPressed();
        }
    }

    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
                    //正在播放的position高亮
                    mAdapter.notifyDataSetChanged();
        }
    }

    class DeleteFinishReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            //当全部删除时
            if (mList.size() == 0) {
                finish();
            }else{
                mToolbar.setTitle(mArtist.getName());
                mAdapter.notifyDataSetChanged();
            }
            invalidateOptionsMenu();
        }
    }

}
