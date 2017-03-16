package com.example.luckychuan.musicplayer.listener;

/**
 * 当MainActivity的4个fragment的item长按时进入删除模式，回调Activity切换菜单
 */
public interface OnFragmentDeleteModeEnterListener {

/**
 * 长按时进入删除模式
 */
    void startDeleteMode(int deleteType);

    void onListItemClick(Integer id);

    void cancelDeleteMode();

}
