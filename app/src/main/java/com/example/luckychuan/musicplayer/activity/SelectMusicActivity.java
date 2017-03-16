package com.example.luckychuan.musicplayer.activity;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.adapter.MusicInfoRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 点击添加音乐菜单按钮时，选择添加的音乐
 */
public class SelectMusicActivity extends AppCompatActivity implements RecyclerViewClickListener {

    private static final String TAG = "SelectMusicActivity";

    //已选择的音乐, KEY:id
    private static Map<Integer, MusicInfo> mSelectMap;
    //当前列表的音乐
    private List<MusicInfo> mList;
    private Toolbar mToolbar;
    private MusicInfoRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExitApplication.getInstance().addActivity(this);
        setContentView(R.layout.activity_playlist);

        //初始化数据
        //从系统中获得数据
        mList = MediaStoreManager.getInstance(this).queryMusicData();
        //原有的音乐
        List<MusicInfo> musicExistList = (List<MusicInfo>) getIntent().getSerializableExtra("list");
        if (musicExistList != null) {
            //将已经存在的音乐过滤
            for (int i = 0; i < musicExistList.size(); i++) {
                for (MusicInfo music : mList) {
                    if (musicExistList.get(i).toString().equals(music.toString())) {
                        mList.remove(music);
                        break;
                    }
                }
            }
        }

        mSelectMap = new HashMap<>();

        //初始化Toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("已选择：" + mSelectMap.size());
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //初始化RecyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.music_recyclerView);
        mAdapter = new MusicInfoRecyclerViewAdapter(mList);
        mAdapter.setItemClickListener(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ExitApplication.getInstance().removeActivity(this);
    }

    @Override
    public void onItemClick(int position) {
        MusicInfo selectMusic = mList.get(position);
        Integer id = selectMusic.getId();
        if (mSelectMap.containsKey(id)) {
            mSelectMap.remove(id);
        } else {
            mSelectMap.put(id, selectMusic);
        }
        mToolbar.setTitle("已选择： " + mSelectMap.size());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemLongClick(int position) {
        onItemClick(position);
    }

    @Override
    public void onItemClick(int position, ActivityOptions options) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_select_all, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.select_all:
                //若当前已经全选
                if (mSelectMap.size() == mList.size()) {
                    //清空全选
                    mSelectMap.clear();
                } else {
                    mSelectMap.clear();
                    for (MusicInfo music:mList){
                        mSelectMap.put(music.getId(),music);
                    }
                }
                mAdapter.notifyDataSetChanged();
                mToolbar.setTitle("已选择： " + mSelectMap.size());
                break;
            case R.id.done:
                //将当前选中的音乐返回给上一个Activity
                Intent intent = new Intent();
                intent.putExtra("result", (Serializable) mSelectMap);
                if (getIntent().getStringExtra("playlist_name") != null) {
                    intent.putExtra("playlist_name", getIntent().getStringExtra("playlist_name"));
                }
                setResult(PlayListActivity.SELECT_ACTIVITY_RESULT, intent);
                mSelectMap = null;
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mSelectMap.size() != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("是否添加当前选中的音乐到列表？");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //将当前选中的音乐返回给上一个Activity
                    Intent intent = new Intent();
                    intent.putExtra("result", (Serializable) mSelectMap);
                    if (getIntent().getStringExtra("playlist_name") != null) {
                        intent.putExtra("playlist_name", getIntent().getStringExtra("playlist_name"));
                    }
                    setResult(PlayListActivity.SELECT_ACTIVITY_RESULT, intent);
                    mSelectMap = null;
                    finish();
                }
            });
            builder.setNegativeButton("放弃", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSelectMap.clear();
                    Intent intent = new Intent();
                    intent.putExtra("result", (Serializable) mSelectMap);
                    setResult(PlayListActivity.SELECT_ACTIVITY_RESULT, intent);
                    mSelectMap = null;
                    finish();
                }
            });
            builder.create().show();
        } else {
            Intent intent = new Intent();
            intent.putExtra("result", (Serializable) mSelectMap);
            setResult(PlayListActivity.SELECT_ACTIVITY_RESULT, intent);
            super.onBackPressed();
        }
    }

    public static boolean isContain(int id) {
        if (mSelectMap == null) {
            return false;
        } else {
            return mSelectMap.containsKey(id);
        }
    }
}
