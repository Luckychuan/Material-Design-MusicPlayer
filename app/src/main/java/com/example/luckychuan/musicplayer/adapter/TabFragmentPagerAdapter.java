package com.example.luckychuan.musicplayer.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by Luckychuan on 2016/9/3.
 */
public class TabFragmentPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> mList;
    private static final String[] titles = {"播放列表","歌曲","专辑","艺术家"};

    public TabFragmentPagerAdapter(FragmentManager fm, List<Fragment> list) {
        super(fm);
        mList = list;
    }

    @Override
    public Fragment getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }
}
