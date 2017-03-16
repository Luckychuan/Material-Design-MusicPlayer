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

import com.example.luckychuan.musicplayer.activity.MainActivity;
import com.example.luckychuan.musicplayer.adapter.MusicInfoRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.OnFragmentDeleteModeEnterListener;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.DeleteUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class MusicFragment extends Fragment implements RecyclerViewClickListener{

    private static final String TAG = "MusicFragment";

    private TextView mEmptyText;
    private List<MusicInfo> mList;
    private MusicInfoRecyclerViewAdapter mAdapter;

    private UIUpdateReceiver mUIUpdateReceiver;
    private DeleteResultReceiver mDeleteResultReceiver;

    private OnFragmentDeleteModeEnterListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        getActivity().registerReceiver(mUIUpdateReceiver, filter);

        mDeleteResultReceiver = new DeleteResultReceiver();
        IntentFilter filter1 = new IntentFilter("DELETE_CANCEL");
        filter1.addAction("ON_DELETE_MUSIC_INFO");
        filter1.addAction("DELETE_FINISH");
        getActivity().registerReceiver(mDeleteResultReceiver,filter1);

        mListener = (OnFragmentDeleteModeEnterListener) getActivity();

        //获得播放列表的音乐
        mList = new ArrayList<>();
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.music_recyclerView);
        mEmptyText = (TextView) view.findViewById(R.id.list_empty);
        mAdapter = new MusicInfoRecyclerViewAdapter(mList);
        mAdapter.setItemClickListener(MusicFragment.this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }



    @Override
    public void onResume() {
        super.onResume();
        queryData();
    }

    private void queryData() {
        mList.clear();
        //更新列表
        mList.addAll(MediaStoreManager.getInstance(getActivity()).queryMusicData());
        if (mList.size() == 0) {
            mEmptyText.setVisibility(View.VISIBLE);
        } else {
            mEmptyText.setVisibility(View.GONE);
        }
        mAdapter.notifyDataSetChanged();
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
            final int finalPosition = position;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //将当前音乐列表发送给service，播放选中的音乐
                    Intent intent = new Intent(getActivity(), PlayService.class);
                    intent.putExtra("position", finalPosition);
                    intent.putExtra("list", (Serializable) mList);
                    getActivity().startService(intent);
                }
            },100);
        } else {
            //删除模式，选择将要删除的音乐
            if (mListener != null) {
                if(MainActivity.isDeleteMode()){
                    mListener.onListItemClick(mList.get(position).getId());
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public void onItemLongClick(int position) {
        //启动删除模式
        if (!MainActivity.isDeleteMode()) {
            if(mListener !=null){
                mListener.startDeleteMode(DeleteUtil.MUSIC_INFO);
            }
        }
        if(MainActivity.isDeleteMode()){
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
                    //正在播放的position高亮
                    mAdapter.notifyDataSetChanged();
        }
    }

    class DeleteResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("DELETE_CANCEL")){
                    mAdapter.notifyDataSetChanged();
                }else if(intent.getAction().equals("ON_DELETE_MUSIC_INFO")){
                    //更新mList的数据以及刷新mAdapter
                    for (int id : intent.getIntegerArrayListExtra("delete_list")) {
                        for (int position = 0; position < mList.size(); position++) {
                            if (mList.get(position).getId() == id) {
                                mList.remove(position);
                                mAdapter.notifyItemRemoved(position);
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


