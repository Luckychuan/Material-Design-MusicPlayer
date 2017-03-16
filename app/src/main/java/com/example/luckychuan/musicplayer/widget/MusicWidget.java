package com.example.luckychuan.musicplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.RemoteViews;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.service.PlayService;

import java.io.FileNotFoundException;
import java.io.InputStream;


public class MusicWidget extends AppWidgetProvider {

    private static final String TAG = "MusicWidget";
    public static final int START_ACTIVITY =1;
    public static final int PREVIOUS = 2;
    public static final int NEXT = 3;
    public static final int PLAY = 4;
    public static final int PAUSE = 5;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int i = 0; i <appWidgetIds.length ; i++) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.widget_layout);

            Intent intent1 = new Intent(context,PlayService.class);
            intent1.putExtra("action", START_ACTIVITY);
            PendingIntent pi = PendingIntent.getService(context, 0, intent1, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget_layout, pi);

            Intent intent2 = new Intent(context,PlayService.class);
            intent2.putExtra("action", PREVIOUS);
            PendingIntent pi1 = PendingIntent.getService(context, 1, intent2, 0);
            remoteViews.setOnClickPendingIntent(R.id.previous_widget, pi1);

            Intent intent3 = new Intent(context,PlayService.class);
            intent3.putExtra("action",NEXT);
            PendingIntent pi4 = PendingIntent.getService(context, 4, intent3, 0);
            remoteViews.setOnClickPendingIntent(R.id.next_widget, pi4);

            //播放按钮
            if (!PlayService.isPlaying()) {
                Intent intent4 = new Intent(context,PlayService.class);
                intent4.putExtra("action",PLAY);
                PendingIntent pi2 = PendingIntent.getService(context, 2, intent4, 0);
                remoteViews.setOnClickPendingIntent(R.id.play_widget, pi2);
            } else {
                Intent intent5 = new Intent(context,PlayService.class);
                intent5.putExtra("action", PAUSE);
                PendingIntent pi3 = PendingIntent.getService(context, 3, intent5, 0);
                remoteViews.setOnClickPendingIntent(R.id.play_widget, pi3);
            }

            appWidgetManager.updateAppWidget(appWidgetIds[i],remoteViews);
        }
    }

    public static void updateWidget(Context context, MusicInfo musicInfo) {
        //设置UI
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        remoteViews.setTextViewText(R.id.textView, musicInfo.getTitle());
        remoteViews.setTextViewText(R.id.textViewSmall, musicInfo.getArtist());
        if (PlayService.isPlaying()) {
            remoteViews.setImageViewResource(R.id.play_widget, R.drawable.pause_widget_ripple);
        } else {
            remoteViews.setImageViewResource(R.id.play_widget, R.drawable.play_widget_ripple);
        }
        Bitmap bitmap;
        Uri uri = Uri.parse(musicInfo.getAlbumUri());
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in != null) {
            bitmap = BitmapFactory.decodeStream(in);
            remoteViews.setImageViewBitmap(R.id.image_widget, bitmap);
        } else {
            remoteViews.setImageViewResource(R.id.image_widget, R.drawable.play_page_default_cover);
        }
        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, MusicWidget.class), remoteViews);
    }

    public static void updateWidgetPlayButton(Context context, boolean isPlaying){
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        if(isPlaying){
            remoteViews.setImageViewResource(R.id.play_widget, R.drawable.pause_widget_ripple);
            PendingIntent pi3 = PendingIntent.getBroadcast(context, 3, new Intent("PAUSE"), 0);
            remoteViews.setOnClickPendingIntent(R.id.play_widget, pi3);
        }else{
            remoteViews.setImageViewResource(R.id.play_widget, R.drawable.play_widget_ripple);
            PendingIntent pi2 = PendingIntent.getBroadcast(context, 2, new Intent("PLAY"), 0);
            remoteViews.setOnClickPendingIntent(R.id.play_widget, pi2);
        }
        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, MusicWidget.class), remoteViews);
    }

    @Override
    public void onEnabled(Context context) {
        MusicInfo musicInfo = PlayService.getMusicInfo();
        if (musicInfo != null) {
            updateWidget(context, musicInfo);
        }
        super.onEnabled(context);
    }
}
