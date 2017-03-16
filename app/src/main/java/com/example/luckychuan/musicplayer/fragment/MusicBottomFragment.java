package com.example.luckychuan.musicplayer.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.ImageUtil;


public class MusicBottomFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "MusicBottomFragment";
    private Button mNextButton;
    private Button mPreviousButton;
    private Button mPlayButton;
    private ImageView mAlbum;
    private TextView mMusicName;
    private TextView mArtist;

    private UIUpdateReceiver mUIUpdateReceiver;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_bottom, container, false);
        mNextButton = (Button) view.findViewById(R.id.next_tab);
        mPreviousButton = (Button) view.findViewById(R.id.previous_tab);
        mPlayButton = (Button) view.findViewById(R.id.play_tab);
        mAlbum = (ImageView) view.findViewById(R.id.album_tab);
        mArtist = (TextView) view.findViewById(R.id.artist_tab);
        mMusicName = (TextView) view.findViewById(R.id.musicName_tab);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNextButton.setOnClickListener(this);
        mPreviousButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mPlayButton.setActivated(true);

        if (PlayService.isPlaying()) {
            mPlayButton.setActivated(false);
        } else {
            mPlayButton.setActivated(true);
        }
        changeNewMusicUI(PlayService.getMusicInfo());
        mMusicName.setFocusable(true);
        mMusicName.requestFocus();

        mUIUpdateReceiver = new UIUpdateReceiver();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        filter.addAction("UPDATE_PLAY_BUTTON");
        getActivity().registerReceiver(mUIUpdateReceiver, filter);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mUIUpdateReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next_tab:
                getActivity().sendBroadcast(new Intent("NEXT"));
                break;
            case R.id.play_tab:
                if (PlayService.isPlaying()) {
                    getActivity().sendBroadcast(new Intent("PAUSE"));
                } else {
                    getActivity().sendBroadcast(new Intent("PLAY"));
                }
                break;
            case R.id.previous_tab:
                getActivity().sendBroadcast(new Intent("PREVIOUS"));
                break;
        }
    }

    private void changeNewMusicUI(MusicInfo musicInfo) {
        if (musicInfo != null) {
            ImageUtil.displayImage(getContext(), musicInfo.getAlbumUri(), mAlbum);
            mArtist.setText(musicInfo.getArtist());
            mMusicName.setText(musicInfo.getTitle());
        } else {
            mAlbum.setImageResource(R.drawable.play_page_default_cover);
            mArtist.setText("没有正在播放的音乐");
            mMusicName.setText("没有正在播放的音乐");
            mPlayButton.setActivated(true);
        }
        if (PlayService.isPlaying()) {
            mPlayButton.setActivated(false);
        } else {
            mPlayButton.setActivated(true);
        }
    }

    class UIUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("UPDATE_UI")) {
                changeNewMusicUI(PlayService.getMusicInfo());
            } else if (intent.getAction().equals("UPDATE_PLAY_BUTTON")) {
                if (PlayService.isPlaying()) {
                    mPlayButton.setActivated(false);
                } else {
                    mPlayButton.setActivated(true);
                }
            }
        }
    }
}
