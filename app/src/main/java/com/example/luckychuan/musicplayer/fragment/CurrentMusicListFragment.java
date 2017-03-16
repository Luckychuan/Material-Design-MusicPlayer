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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.adapter.CurrentMusicListRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.service.PlayService;

import java.util.List;

/**
 * MusicActivity里的当前播放的音乐列表
 */
public class CurrentMusicListFragment extends Fragment implements RecyclerViewClickListener {

    private static final String TAG = "CurrentMusicListFragment";

    private List<MusicInfo> mList;
    private UIUpdateReceiver mUIUpdateReceiver;
    private CurrentMusicListRecyclerViewAdapter mAdapter;
    private RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_music_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        getActivity().registerReceiver(mUIUpdateReceiver, filter);

        //获得播放列表的音乐
        mList = PlayService.getCurrentMusicList();
        mRecyclerView = (RecyclerView) view.findViewById(R.id.current_music_list_recyclerView);
        ((TextView) view.findViewById(R.id.current_music_list_size_text)).setText("当前音乐列表（" + mList.size() + "）");

        mAdapter = new CurrentMusicListRecyclerViewAdapter(mList);
        mAdapter.setItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

    }

    @Override
    public void onItemClick(int position) {
        final int finalPosition = position;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //将选中的音乐发送给service，播放选中的音乐
                Intent intent = new Intent("PLAY");
                intent.putExtra("position", finalPosition);
                getActivity().sendBroadcast(intent);

            }
        }, 100);
    }

    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    public void notifyItemInserted(int position) {
        mAdapter.notifyItemInserted(position);
        //      Log.d("play_this_next",mList.get(position).getTitle());
    }

    public void notifyItemRemoved(int position) {
        mAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            mRecyclerView.scrollToPosition(PlayService.getPosition());
        }
    }

    @Override
    public void onItemLongClick(int position) {

    }

    @Override
    public void onItemClick(int position, ActivityOptions options) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mUIUpdateReceiver);
    }

    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //正在播放的position高亮
            mAdapter.notifyDataSetChanged();
        }
    }

}
