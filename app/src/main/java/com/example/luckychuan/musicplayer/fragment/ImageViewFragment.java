package com.example.luckychuan.musicplayer.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.util.ImageUtil;

/**
 * MusicActivity里的ViewPager控件中需要的Fragment
 */
public class ImageViewFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_view_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView imageView = (ImageView) view.findViewById(R.id.album_main);
        ImageUtil.displayImage(getContext(), getArguments().getString("album_uri"), imageView);
    }



}
