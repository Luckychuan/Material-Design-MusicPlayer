package com.example.luckychuan.musicplayer.adapter;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;

import com.example.luckychuan.musicplayer.fragment.ImageViewFragment;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.service.PlayService;

import java.util.ArrayList;
import java.util.List;

public class ImageFragmentPagerAdapter extends FragmentPagerAdapter{

    private List<MusicInfo> mList;
    private ArrayList<Fragment> mFragmentList;
    private FragmentManager mFragmentManager;

    public ImageFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragmentManager = fm;
        mList = PlayService.getCurrentMusicList();
        mFragmentList = new ArrayList<>();

    }

    @Override
    public Fragment getItem(int position) {
        ImageViewFragment fragment = new ImageViewFragment();
        Bundle bundle = new Bundle();
//        Log.d("random", "getItem :" + mList.get(position).getTitle());
        bundle.putString("album_uri", mList.get(position).getAlbumUri());
        fragment.setArguments(bundle);
        mFragmentList.add(fragment);
        return fragment;
    }

    @Override
    public void notifyDataSetChanged() {
        //先清除fragment缓存再刷新数据
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for(Fragment fragment: mFragmentList){
            transaction.remove(fragment);
        }
        mFragmentList.clear();
        transaction.commitAllowingStateLoss();
        mFragmentManager.executePendingTransactions();
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }


    @Override
    public int getCount() {
        return mList.size();
    }




}
