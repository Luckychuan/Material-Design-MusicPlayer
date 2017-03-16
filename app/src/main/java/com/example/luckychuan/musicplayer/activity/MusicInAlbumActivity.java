package com.example.luckychuan.musicplayer.activity;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.adapter.MusicInAlbumRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.fragment.MusicBottomFragment;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.Album;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.DeleteUtil;
import com.example.luckychuan.musicplayer.util.ImageUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 点击专辑列表的item后进入每个专辑的音乐列表
 */
public class MusicInAlbumActivity extends AppCompatActivity implements RecyclerViewClickListener, View.OnClickListener {

    private List<MusicInfo> mList;
    private Album mAlbum;
    private MusicInAlbumRecyclerViewAdapter mAdapter;

    private TextView mNameTextView;

    private DeleteUtil mDeleteUtil;
    private LinearLayout mDeleteLayout;
    private TextView mDeleteTextView;

    private UIUpdateReceiver mUIUpdateReceiver;
    private DeleteFinishReceiver mDeleteFinishReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExitApplication.getInstance().addActivity(this);
        setContentView(R.layout.activity_music_in_album);

        mAlbum = (Album) getIntent().getSerializableExtra("album");

        mDeleteUtil = new DeleteUtil(this);

        initToolBar();

        //获得播放列表的音乐
        mList = new ArrayList<>();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.music_in_album_recyclerView);
        mAdapter = new MusicInAlbumRecyclerViewAdapter(mList);
        mAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mNameTextView = (TextView) findViewById(R.id.album_name_textView_header);
        mNameTextView.setText(mAlbum.getAlbumName());


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
                    Intent intent = new Intent(MusicInAlbumActivity.this, MusicActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.translate_in, R.anim.fade_out);
                }
            }
        });

        //删除音乐的控件
        ImageButton deleteButton = (ImageButton) findViewById(R.id.music_in_album_delete);
        mDeleteLayout = (LinearLayout) findViewById(R.id.music_in_album_delete_layout);
        ImageButton selectAllButton = (ImageButton) findViewById(R.id.music_in_album_select_all);
        mDeleteTextView = (TextView) findViewById(R.id.music_in_album__deleteSize_text);
        deleteButton.setOnClickListener(this);
        selectAllButton.setOnClickListener(this);

        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        registerReceiver(mUIUpdateReceiver, filter);

        mDeleteFinishReceiver = new DeleteFinishReceiver();
        IntentFilter filter2 = new IntentFilter("DELETE_FINISH");
        registerReceiver(mDeleteFinishReceiver, filter2);

    }

    private void initToolBar() {


        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle(" ");
        collapsingToolbarLayout.setScrimsShown(false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ImageView imageView = (ImageView) findViewById(R.id.album_toolbar_layout);
        ImageUtil.displayImage(this, mAlbum.getAlbumUri(), imageView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mList.clear();
        mList.addAll(MediaStoreManager.getInstance(this).queryMusicDataInAlbum(mAlbum.getId()));
        if (mList.size() == 0) {
            finish();
        }
        mAdapter.notifyDataSetChanged();
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        ExitApplication.getInstance().removeActivity(this);
        unregisterReceiver(mDeleteFinishReceiver);
        unregisterReceiver(mUIUpdateReceiver);
    }


    @Override
    public void onItemClick(int position) {
        if (!mDeleteUtil.isOnDeleteMode()) {
            final int finalPosition = position;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //将当前音乐列表发送给service，播放选中的音乐
                    Intent intent = new Intent(MusicInAlbumActivity.this, PlayService.class);
                    intent.putExtra("position", finalPosition);
                    intent.putExtra("list", (Serializable) mList);
                    startService(intent);
                }
            }, 200);
        } else {
            //删除模式，选择将要删除的音乐
            Integer clickId = mList.get(position).getId();
            mDeleteUtil.selectItem(clickId);
            mDeleteTextView.setText("已选择： " + mDeleteUtil.getDeleteListSize());
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
            mNameTextView.setVisibility(View.GONE);
            mDeleteLayout.setVisibility(View.VISIBLE);
        }
        if (mDeleteUtil.isOnDeleteMode()) {
            //选择当前点击的item进入待删除列表
            onItemClick(position);
        }
    }

    @Override
    public void onItemClick(int position, ActivityOptions options) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //全选按钮
            case R.id.music_in_album_select_all:
                if (mList.size() == mDeleteUtil.getDeleteListSize()) {
                    mDeleteUtil.clearDeleteList();
                } else {
                    List<Integer> idList = new ArrayList<>();
                    for (MusicInfo music : mList
                            ) {
                        idList.add((Integer) music.getId());
                    }
                    mDeleteUtil.selectAll(idList);
                }
                mAdapter.notifyDataSetChanged();
                mDeleteTextView.setText("已选择： " + mDeleteUtil.getDeleteListSize());
                break;

            //确认删除按钮
            case R.id.music_in_album_delete:
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
                            for (int id : mDeleteUtil.getDeleteList()) {
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
    }

    @Override
    public void onBackPressed() {
        if (mDeleteUtil.isOnDeleteMode()) {
            mDeleteUtil.cancelDeleteMode();
            mNameTextView.setVisibility(View.VISIBLE);
            mDeleteLayout.setVisibility(View.GONE);
            mAdapter.notifyDataSetChanged();
        } else {
            finishAfterTransition();
        }
    }



    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //正在播放的position高亮
            mAdapter.notifyDataSetChanged();

        }
    }

    class DeleteFinishReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //当全部删除时
            if (mList.size() == 0) {
                finish();
            } else {
                mAdapter.notifyDataSetChanged();
            }
            mNameTextView.setVisibility(View.VISIBLE);
            mDeleteLayout.setVisibility(View.GONE);
        }
    }

}
