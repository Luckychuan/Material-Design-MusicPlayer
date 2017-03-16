package com.example.luckychuan.musicplayer.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 点击menu的item后的功能
 */
public class MenuUtil {

    private MenuUtil(){}

    /**
     *    创建新的播放列表或者重命名
     * @param context
     */
    public static  void editPlaylist(final Context context, final OnPlaylistDialogConfirmFinishListener listener){
        final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
        //对话框里的控件
        View view = LayoutInflater.from(context).inflate(R.layout.add_play_list_dialog, null);
        final EditText editText = (EditText) view.findViewById(R.id.new_play_list_edit);
        Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
        Button confirmButton = (Button) view.findViewById(R.id.confirm_button);

        builder.setCancelable(true).setTitle("新建播放列表：").setView(view);
        final android.support.v7.app.AlertDialog dialog = builder.create();
        //弹出对话框时显示键盘
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        dialog.show();
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.hide();
            }
        });
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editText.getText().toString();
                if (name.length() == 0) {
                    editText.setError("名称不能为空");
                } else if (name.length() > 25) {
                    editText.setError("字数超过限制");
                } else {
                    //判断当前名字是否重复
                    boolean isExist = false;
                    //播放列表数据
                    List<PlaylistInfo> playlistInfoList = MediaStoreManager.getInstance(context).queryPlaylistData();
                    List<String> playlistNameList = new ArrayList<>();
                    for (PlaylistInfo playlistInfo : playlistInfoList) {
                        playlistNameList.add(playlistInfo.getName());
                    }
                    for (String existName : playlistNameList) {
                        //                   Log.d(TAG, "name" + existName);
                        if (existName.equals(name)) {
                            isExist = true;
                            break;
                        }
                    }
                    if (isExist) {
                        editText.setError("已存在此列表，请重新输入");
                    } else {
                        dialog.hide();
                        listener.OnPlaylistDialogConfirmFinish(name);
                    }
                }
            }
        });
    }

    public static void deleteMusic(final Context context, final MusicInfo musicInfo, final OnDeleteDialogConfirmFinishListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("删除：");
        builder.setMessage("是否删除 " + musicInfo.getTitle());
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent("DELETE_SINGLE");
                intent.putExtra("music", musicInfo);
                context.sendBroadcast(intent);
                DeleteUtil.deleteMusicFile(context, musicInfo);
                Toast.makeText(context, "删除成功！", Toast.LENGTH_SHORT).show();
                if(listener != null){
                    listener.OnDeleteDialogConfirmFinish();
                }
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setCancelable(true);
        builder.create().show();
    }



    /**
     * 当编辑Playlist对话框的确认按钮点击时的回调
     */
    public interface OnPlaylistDialogConfirmFinishListener{
         void OnPlaylistDialogConfirmFinish(String editName);
    }

    public interface OnDeleteDialogConfirmFinishListener{
        void OnDeleteDialogConfirmFinish();
    }


}
