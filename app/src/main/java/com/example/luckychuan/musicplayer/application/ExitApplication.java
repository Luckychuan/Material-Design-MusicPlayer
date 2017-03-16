package com.example.luckychuan.musicplayer.application;

import android.app.Activity;

import java.util.LinkedList;
import java.util.List;

/**
 * Activity的管理类，用于
 * 1.判断当前有多少个Activity启动，
 * 2.程序退出时销毁所有Activity
 */
public class ExitApplication extends MyApplication {

    private List<Activity> list = new LinkedList();
    private static ExitApplication instance;

    private ExitApplication() {
    }

    public synchronized static ExitApplication getInstance() {
        if (instance == null) {
            instance = new ExitApplication();
        }
        return instance;
    }

    public int size() {
        return list.size();
    }

    public void addActivity(Activity activity) {
        list.add(activity);
    }

    public void systemExit() {
        for (Activity activity : list) {
            activity.finish();
        }
    }

    public void removeActivity(Activity activity) {
        list.remove(activity);
    }


}
