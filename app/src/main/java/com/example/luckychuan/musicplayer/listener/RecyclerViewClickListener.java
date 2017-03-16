package com.example.luckychuan.musicplayer.listener;

import android.app.ActivityOptions;

/**
 * Created by Luckychuan on 2016/9/15.
 */
public interface RecyclerViewClickListener {

    void onItemClick(int position);
    void onItemLongClick(int position);
    void onItemClick(int position,ActivityOptions options);

}
