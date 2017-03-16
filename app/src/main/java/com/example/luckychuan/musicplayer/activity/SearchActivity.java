package com.example.luckychuan.musicplayer.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.adapter.MusicInfoRecyclerViewAdapter;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.service.PlayService;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class SearchActivity extends Activity implements RecyclerViewClickListener, SearchView.OnQueryTextListener {


    private static final String TAG = "SearchActivity";
    private RecyclerView mRecyclerView;
    private List<MusicInfo> mList;
    private MusicInfoRecyclerViewAdapter mAdapter;
    private TextView mEmptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_music);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimaryDark));

        SearchView searchView = (SearchView) findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);

        //修改文字颜色
        int id = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) searchView.findViewById(id);
        textView.setTextColor(Color.WHITE);
        textView.setHintTextColor(getColor(R.color.textView_light));

        //去掉下划线
        try {
            Field field = searchView.getClass().getDeclaredField("mSearchPlate");
            field.setAccessible(true);
            View view = (View) field.get(searchView);
            view.setBackgroundColor(Color.TRANSPARENT);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        ((Button) findViewById(R.id.back_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        mList = new ArrayList<>();
        mRecyclerView = (RecyclerView) findViewById(R.id.search_recycler);
        mEmptyText = (TextView) findViewById(R.id.empty_text);
        mAdapter = new MusicInfoRecyclerViewAdapter(mList);
        mAdapter.setItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }


    @Override
    public void onItemClick(int position) {
        final int finalPosition = position;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //将当前音乐列表发送给service，播放选中的音乐
                Intent intent = new Intent(SearchActivity.this, PlayService.class);
                intent.putExtra("position", finalPosition);
                intent.putExtra("list", (Serializable) mList);
                startService(intent);
            }
        },100);
    }

    @Override
    public void onItemLongClick(int position) {

    }

    @Override
    public void onItemClick(int position, ActivityOptions options) {

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mList.clear();
        if(!TextUtils.isEmpty(newText)){
            mList.addAll(MediaStoreManager.getInstance(this).queryMusicData(newText));
            if(mList.size() != 0){
                mEmptyText.setVisibility(View.INVISIBLE);
                mRecyclerView.setVisibility(View.VISIBLE);
                mAdapter.notifyDataSetChanged();
            }else{
                mRecyclerView.setVisibility(View.INVISIBLE);
                mEmptyText.setVisibility(View.VISIBLE);
            }
        }
        return false;
    }
}
