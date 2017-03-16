package com.example.luckychuan.musicplayer.fragment;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.activity.MainActivity;
import com.example.luckychuan.musicplayer.activity.PlayListActivity;
import com.example.luckychuan.musicplayer.adapter.PlaylistRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.database.MusicDatabase;
import com.example.luckychuan.musicplayer.application.ExitApplication;
import com.example.luckychuan.musicplayer.listener.OnFragmentDeleteModeEnterListener;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;
import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.util.DeleteUtil;
import com.example.luckychuan.musicplayer.util.ImageUtil;
import com.kevin.wraprecyclerview.WrapRecyclerView;

import java.util.ArrayList;
import java.util.List;


public class PlaylistFragment extends Fragment implements View.OnClickListener, RecyclerViewClickListener {

    //最常播放
    public static final int PLAY_MOST = -2;
    //最近播放
    public static final int LATEST_PLAY = -1;
    //我的最爱
    public static final int FAVORITE = -3;

    private LinearLayout mLinearLayout1;
    private LinearLayout mLinearLayout2;
    private LinearLayout mLinearLayout3;


    private ImageView mAlbum1;
    private ImageView mAlbum2;
    private ImageView mAlbum3;

    private TextView mTextView1;
    private TextView mTextView2;
    private TextView mTextView3;

    private TextView mTextViewSmall1;
    private TextView mTextViewSmall2;
    private TextView mTextViewSmall3;

    private PlaylistRecyclerViewAdapter mAdapter;
    private List<PlaylistInfo> mList;

    private UIUpdateReceiver mUIUpdateReceiver;

    private DeleteResultReceiver mDeleteResultReceiver;

    private OnFragmentDeleteModeEnterListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playinglist, container, false);

    }

    @Override
    public void onResume() {
        super.onResume();
        queryData();
        updateThreePlaylist();
    }

    public void queryData(){
        mList.clear();
        mList.addAll(MediaStoreManager.getInstance(getActivity()).queryPlaylistData());
        mAdapter.notifyDataSetChanged();
    }

    private void updateThreePlaylist() {
        //设置三个封面
        MusicDatabase db = MusicDatabase.getInstance(getContext());
        String[] albumUri = db.queryPlayListUri();
        ImageUtil.displayImageAndPalette(getContext(), albumUri[0], mAlbum1,mLinearLayout1,mTextView1,mTextViewSmall1);
        ImageUtil.displayImageAndPalette(getContext(), albumUri[1], mAlbum2, mLinearLayout2, mTextView2, mTextViewSmall2);
        ImageUtil.displayImageAndPalette(getContext(), albumUri[2], mAlbum3, mLinearLayout3, mTextView3, mTextViewSmall3);

        //设置列表大小
        int[] size = db.queryPlayListSize();
        if (size[1] > 50) {
            size[1] = 50;
        }
        if(size[0] > 50){
            size[0] = 50;
        }
        mTextViewSmall1.setText(size[0] + " 首音乐");
        mTextViewSmall2.setText(size[1] + " 首音乐");
        mTextViewSmall3.setText(size[2] + " 首音乐");

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListener = (OnFragmentDeleteModeEnterListener) getActivity();

        //获得播放列表
        mList = new ArrayList<>();
        mAdapter = new PlaylistRecyclerViewAdapter(mList);
        mAdapter.setItemClickListener(this);
        WrapRecyclerView recyclerView = (WrapRecyclerView) view.findViewById(R.id.playing_list_listView);
        //让FloatingActionButton与RecyclerView绑定
        MainActivity.getAddButton().attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        View headerView = LayoutInflater.from(getContext()).inflate(R.layout.play_list_listview_header, null);
        recyclerView.addHeaderView(headerView);

        //三个卡片的layout
        mLinearLayout1 = (LinearLayout) headerView.findViewById(R.id.latest_play_layout);
        mLinearLayout2 = (LinearLayout) headerView.findViewById(R.id.most_play_layout);
        mLinearLayout3 = (LinearLayout) headerView.findViewById(R.id.favorite_layout);

        mAlbum1 = (ImageView) headerView.findViewById(R.id.album_latest_play);
        mAlbum2 = (ImageView) headerView.findViewById(R.id.album_most_play);
        mAlbum3 = (ImageView) headerView.findViewById(R.id.album_favorite);

        mTextView1 = (TextView) headerView.findViewById(R.id.latest_play_textView);
        mTextView2 = (TextView) headerView.findViewById(R.id.most_play_textView);
        mTextView3 = (TextView) headerView.findViewById(R.id.favorite_textView);


        mTextViewSmall1 = (TextView) headerView.findViewById(R.id.textView_small1);
        mTextViewSmall2 = (TextView) headerView.findViewById(R.id.textView_small2);
        mTextViewSmall3 = (TextView) headerView.findViewById(R.id.textView_small3);

        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_THREE_PLAYLIST");
        filter.addAction("UPDATE_PLAYLIST");
        getActivity().registerReceiver(mUIUpdateReceiver, filter);

        mDeleteResultReceiver = new DeleteResultReceiver();
        IntentFilter filter1 = new IntentFilter("DELETE_CANCEL");
        filter1.addAction("ON_DELETE_PLAYLIST");
        filter1.addAction("DELETE_FINISH");
        getActivity().registerReceiver(mDeleteResultReceiver, filter1);

        //三个播放列表初始化
        //获得数据
        updateThreePlaylist();
        //控件初始化
        CardView cardView1 =(CardView)headerView.findViewById(R.id.latest_play_card);
        final CardView cardView2 = (CardView) headerView.findViewById(R.id.most_play_card);
        final CardView cardView3 = (CardView) headerView.findViewById(R.id.favorite_card);
        cardView1.setOnClickListener(this);
        cardView2.setOnClickListener(this);
        cardView3.setOnClickListener(this);

        //三张卡片进入动画
        if(ExitApplication.getInstance().size() == 1){
            cardView1.setVisibility(View.INVISIBLE);
            cardView3.setVisibility(View.INVISIBLE);
            cardView2.setVisibility(View.INVISIBLE);

            cardView1.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.card_in));
            cardView1.setVisibility(View.VISIBLE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    cardView2.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.card_in));
                    cardView2.setVisibility(View.VISIBLE);
                }
            }, 300);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    cardView3.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.card_in));
                    cardView3.setVisibility(View.VISIBLE);
                }
            }, 600);
        }

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(getContext(), PlayListActivity.class);
        switch (v.getId()) {
            case R.id.latest_play_card:
                intent.putExtra("play_list_id", LATEST_PLAY);
                intent.putExtra("play_list_name", "最近播放");
                break;
            case R.id.most_play_card:
                intent.putExtra("play_list_id", PLAY_MOST);
                intent.putExtra("play_list_name", "最常播放");
                break;
            case R.id.favorite_card:
                intent.putExtra("play_list_id", FAVORITE);
                intent.putExtra("play_list_name", "我的最爱");
                break;
        }
        startActivity(intent);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mUIUpdateReceiver);
        getActivity().unregisterReceiver(mDeleteResultReceiver);
    }

    @Override
    public void onItemClick(int position) {
        if (!MainActivity.isDeleteMode()) {
            Intent intent = new Intent(getActivity(), PlayListActivity.class);
            intent.putExtra("play_list", position);
            //hearView的position为0
            intent.putExtra("play_list_name", mList.get(position - 1).getName());
            intent.putExtra("play_list_id", mList.get(position - 1).getId());
            startActivity(intent);
        } else {
            //删除模式，选择将要删除的
            if (mListener != null) {
                if (MainActivity.isDeleteMode()) {
                    mListener.onListItemClick(mList.get(position - 1).getId());
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onItemLongClick(int position) {
        //启动删除模式
        if (!MainActivity.isDeleteMode()) {
            if (mListener != null) {
                mListener.startDeleteMode(DeleteUtil.PLAYLIST);
            }
        }
        if (MainActivity.isDeleteMode()) {
            //选择当前点击的item进入待删除列表
            onItemClick(position);
        }
    }

    @Override
    public void onItemClick(int position, ActivityOptions options) {

    }

    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_THREE_PLAYLIST")) {
               updateThreePlaylist();
            } else if (intent.getAction().equals("UPDATE_PLAYLIST")) {
                queryData();
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    class DeleteResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("DELETE_CANCEL")){
                mAdapter.notifyDataSetChanged();
            }else if(intent.getAction().equals("ON_DELETE_PLAYLIST")){
                //更新mList的数据以及刷新mAdapter
                for (int id : intent.getIntegerArrayListExtra("delete_list")) {
                    for (int position = 0; position < mList.size(); position++) {
                        if (mList.get(position).getId() == id) {
                            mList.remove(position);
                            mAdapter.notifyItemRemoved(position+1);
                            position--;
                            break;
                        }
                    }
                }
            }else if(intent.getAction().equals("DELETE_FINISH")){
                //重新载入数据
                queryData();
            }
        }
    }
}
