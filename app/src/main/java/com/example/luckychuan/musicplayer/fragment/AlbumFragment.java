package com.example.luckychuan.musicplayer.fragment;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.activity.MainActivity;
import com.example.luckychuan.musicplayer.activity.MusicInAlbumActivity;
import com.example.luckychuan.musicplayer.adapter.AlbumRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.OnFragmentDeleteModeEnterListener;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.Album;
import com.example.luckychuan.musicplayer.util.DeleteUtil;

import java.util.ArrayList;
import java.util.List;


public class AlbumFragment extends Fragment implements RecyclerViewClickListener {

    private static final String TAG = "AlbumFragment";

    private List<Album> mList;
    private AlbumRecyclerViewAdapter mAdapter;

    private DeleteResultReceiver mDeleteResultReceiver;
    private OnFragmentDeleteModeEnterListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_album, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListener = (OnFragmentDeleteModeEnterListener) getActivity();
        mList = new ArrayList<>();
        mAdapter = new AlbumRecyclerViewAdapter(mList);
        mAdapter.setOnItemClickListener(this);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.album_recyclerView);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mDeleteResultReceiver = new DeleteResultReceiver();
        IntentFilter filter1 = new IntentFilter("DELETE_CANCEL");
        filter1.addAction("DELETE_FINISH");
        filter1.addAction("ON_DELETE_ALBUM");
        getActivity().registerReceiver(mDeleteResultReceiver, filter1);
    }

    @Override
    public void onResume() {
        super.onResume();
        queryData();
    }

    public void queryData() {
        mList.clear();
        mList.addAll(MediaStoreManager.getInstance(getContext()).queryAlbumData());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mDeleteResultReceiver);
    }

    @Override
    public void onItemClick(int position) {

    }

    @Override
    public void onItemLongClick(int position) {
        //启动删除模式
        if (!MainActivity.isDeleteMode()) {
            if (mListener != null) {
                mListener.startDeleteMode(DeleteUtil.ALBUM);
            }
        }
        if (MainActivity.isDeleteMode()) {
            //选择当前点击的item进入待删除列表
            onItemClick(position);
        }
    }

    @Override
    public void onItemClick(int position, ActivityOptions options) {
        if (!MainActivity.isDeleteMode()) {
            Intent intent = new Intent(getActivity(), MusicInAlbumActivity.class);
            intent.putExtra("album", mList.get(position));
            startActivity(intent,options.toBundle());
        } else {
            //删除模式，选择将要删除的
            if (mListener != null) {
                if (MainActivity.isDeleteMode()) {
                    mListener.onListItemClick(mList.get(position).getId());
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    class DeleteResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("DELETE_CANCEL")) {
                mAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals("ON_DELETE_ALBUM")) {
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
            } else if (intent.getAction().equals("DELETE_FINISH")) {
                //重新载入数据
                queryData();
            }
        }
    }
}
