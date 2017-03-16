package com.example.luckychuan.musicplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 将albumUri解析成bitmap,并加载在imageView
 */
public class ImageUtil {

    //开源库Universal-Image-Loader，设置显示图片的配置
    private static DisplayImageOptions mMusicOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.play_page_default_cover)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .showImageOnFail(R.drawable.play_page_default_cover)
            .build();

    private static DisplayImageOptions mAlbumOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.color.colorWhile)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .showImageOnFail(R.drawable.play_page_default_cover)
            .build();

    private ImageUtil() {
    }

    /**
     * 使用开源库Universal-Image-Loader加载图片
     *
     * @param albumUri
     * @param imageView
     */
    public static void displayMusicListImageByUniversalImageLoader(String albumUri, ImageView imageView) {
        ImageLoader.getInstance().displayImage(albumUri, imageView, mMusicOptions);
    }

    public static void displayAlbumListImageByUniversalImageLoader(String albumUri, ImageView imageView, final LinearLayout linearLayout, final TextView textView, final TextView textViewSmall) {
        ImageLoader.getInstance().displayImage(albumUri, imageView, mAlbumOptions, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                displayPalette(loadedImage, linearLayout, textView, textViewSmall);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        });
    }

    public static void displayImage(Context context, String albumUri, ImageView image) {
        Uri uri = Uri.parse(albumUri);
        Bitmap bitmap = null;
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in != null) {
            bitmap = BitmapFactory.decodeStream(in);
            image.setImageBitmap(bitmap);
        } else {
            image.setImageResource(R.drawable.play_page_default_cover);
        }
    }

    public static void displayImageAndPalette(Context context, String albumUri, ImageView image, LinearLayout layout, TextView textView, TextView textViewSmall) {
        Uri uri = Uri.parse(albumUri);
        Bitmap bitmap = null;
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in != null) {
            bitmap = BitmapFactory.decodeStream(in);
            image.setImageBitmap(bitmap);
        } else {
            image.setImageResource(R.drawable.play_page_default_cover);
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.play_page_default_cover);
        }
        //使用Palette获得配色
        displayPalette(bitmap, layout, textView, textViewSmall);
    }

    public static void decodeBitmap(final Context context, final String albumUri, final OnDecodeFinishListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri = Uri.parse(albumUri);
                Bitmap bitmap = null;
                InputStream in = null;
                try {
                    in = context.getContentResolver().openInputStream(uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (in != null) {
                    bitmap = BitmapFactory.decodeStream(in);
                } else {
                    bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.play_page_default_cover);
                }
                if (listener != null) {
                    listener.onDecodeFinish(bitmap);
                }
            }
        }).start();
    }


    /**
     * 使用Palette获得配色,给控件设置颜色
     *
     * @param bitmap
     * @param layout        需要设置配色的layout
     * @param textView      layout中的大的textView
     * @param textViewSmall layout中的小的textView
     */
    private static void displayPalette(Bitmap bitmap, final LinearLayout layout, final TextView textView, final TextView textViewSmall) {

        Palette.Builder builder = Palette.from(bitmap);
        builder.generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                Palette.Swatch swatch = palette.getLightVibrantSwatch();
                if (swatch == null) {
                    swatch = palette.getLightMutedSwatch();
                }
                if (swatch == null) {
                    swatch = palette.getVibrantSwatch();
                }
                if (swatch == null) {
                    swatch = palette.getMutedSwatch();
                }
                if (swatch == null) {
                    swatch = palette.getDarkVibrantSwatch();
                }
                if (swatch == null) {
                    swatch = palette.getDarkMutedSwatch();
                }
                layout.setBackgroundColor(swatch.getRgb());
                textView.setTextColor(swatch.getBodyTextColor());
                textViewSmall.setTextColor(swatch.getTitleTextColor());
            }


        });
    }

    public interface OnDecodeFinishListener {
        void onDecodeFinish(Bitmap bitmap);
    }

}