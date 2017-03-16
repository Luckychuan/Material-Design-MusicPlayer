package com.example.luckychuan.musicplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.activity.EditInfoActivity;
import com.example.luckychuan.musicplayer.activity.MusicInArtistActivity;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.DeleteUtil;
import com.example.luckychuan.musicplayer.util.MenuUtil;

import java.util.List;

/**
 * Created by Luckychuan on 2016/10/12.
 */
public class MusicInAlbumRecyclerViewAdapter extends RecyclerView.Adapter<MusicInAlbumRecyclerViewAdapter.ViewHolder> {

    private RecyclerViewClickListener mListener;
    private List<MusicInfo> mList;
    private Context mContext;


    public MusicInAlbumRecyclerViewAdapter(List<MusicInfo> musicInfoList) {
        mList = musicInfoList;
    }

    public void setOnItemClickListener(RecyclerViewClickListener listener) {
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.music_in_album_recycler_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MusicInfo musicInfo = mList.get(position);
        holder.textView.setText(musicInfo.getTitle());
        holder.positionTextView.setText(position + 1 + "");
        //将选中的音乐高亮
        if (PlayService.getMusicInfo() != null) {
            if (musicInfo.toString().equals(PlayService.getMusicInfo().toString())) {
                holder.textView.setTextColor(mContext.getColor(R.color.colorPrimary));
                holder.positionTextView.setTextColor(mContext.getColor(R.color.colorPrimary));
            } else {
                holder.textView.setTextColor(mContext.getColor(R.color.textView));
                holder.positionTextView.setTextColor(mContext.getColor(R.color.textView));
            }
        }
        //选择音乐时让选择的item变黑
        if (DeleteUtil.isContain(musicInfo.getId())) {
            holder.itemLayout.setBackgroundColor(0x66C0C0C0);
        } else {
            //设置水波纹效果
            TypedValue outValue = new TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            holder.itemLayout.setBackgroundResource(outValue.resourceId);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView positionTextView;
        public TextView textView;
        public RelativeLayout itemLayout;
        private ImageButton menuButton;

        public ViewHolder(View itemView) {
            super(itemView);

            positionTextView = (TextView) itemView.findViewById(R.id.music_in_album_item_position);
            textView = (TextView) itemView.findViewById(R.id.music_in_album_item_textView);
            itemLayout = (RelativeLayout) itemView.findViewById(R.id.music_in_album_item_layout);
            menuButton = (ImageButton) itemView.findViewById(R.id.popup_menu);


            //为item添加回调
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(getLayoutPosition());
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mListener != null) {
                        mListener.onItemLongClick(getLayoutPosition());
                    }
                    return true;
                }
            });

            //设置菜单
            final PopupMenu popup = new PopupMenu(mContext, menuButton);
            final Menu menu = popup.getMenu();
            popup.getMenuInflater().inflate(R.menu.menu_music_in_album, menu);
            //动态添加item
            final SubMenu subMenu = menu.addSubMenu(Menu.NONE, R.id.add_to_playlist, 1, "添加到播放列表");

            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popup.show();
                }
            });

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                private MusicInfo listItem;
                private List<PlaylistInfo> playlistInfoList;

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    listItem = mList.get(getLayoutPosition());


                    int id = item.getItemId();
                    switch (id) {
                        case R.id.play_this_next:
                            Intent intent = new Intent("INSERT_MUSIC");
                            intent.putExtra("music", listItem);
                            mContext.sendBroadcast(intent);
                            break;

                        case R.id.music_in_artist_activity:
                            Intent intent2 = new Intent(mContext, MusicInArtistActivity.class);
                            intent2.putExtra("artist", MediaStoreManager.getInstance(mContext).queryArtistData(listItem.getArtist()));
                            mContext.startActivity(intent2);
                            break;

                        case R.id.delete:
                            MenuUtil.deleteMusic(mContext, listItem, new MenuUtil.OnDeleteDialogConfirmFinishListener() {
                                @Override
                                public void OnDeleteDialogConfirmFinish() {
                                    mList.remove(getLayoutPosition());
                                    notifyItemRemoved(getLayoutPosition());
                                    //                                  Log.d(TAG, "OnPlaylistDeleteConfirmFinish: ");
                                }
                            });
                            break;

                        case R.id.edit_music_info:
                            Intent intent3 = new Intent(mContext, EditInfoActivity.class);
                            intent3.putExtra("music", listItem);
                            mContext.startActivity(intent3);
                            break;

                        case R.id.add_to_playlist:
                            subMenu.removeGroup(R.id.add_to_playlist);
                            playlistInfoList = MediaStoreManager.getInstance(mContext).queryPlaylistData();
                            subMenu.add(R.id.add_to_playlist, 0, 0, "创建新的播放列表");
//                            Log.d(TAG, "onMenuItemClick: add");
                            for (int i = 0; i < playlistInfoList.size(); i++) {
                                subMenu.add(R.id.add_to_playlist, i + 1, i + 1, playlistInfoList.get(i).getName());

                            }
                            break;
                    }

                    int groupId = item.getGroupId();
                    if (groupId == R.id.add_to_playlist) {
                        if (id == 0) {
                            //创建新的播放列表
                            MenuUtil.editPlaylist(mContext, new MenuUtil.OnPlaylistDialogConfirmFinishListener() {
                                @Override
                                public void OnPlaylistDialogConfirmFinish(String editName) {
                                    MediaStoreManager.getInstance(mContext).addPlaylistData(editName);
                                    //刚创建的播放列表的id
                                    int playlistId = MediaStoreManager.getInstance(mContext).queryNewPlaylistId();
                                    MediaStoreManager.getInstance(mContext).insertMusicToPlaylist(mContext, playlistId, listItem.getId());
                                    mContext.sendBroadcast(new Intent("UPDATE_PLAYLIST"));
                                }
                            });
                        } else {
//                            Log.d(TAG, "onMenuItemClick: " + item.getItemId());
                            //添加到现有的播放列表
                            MediaStoreManager.getInstance(mContext).insertMusicToPlaylist(mContext, playlistInfoList.get(id - 1).getId(), listItem.getId());
                            mContext.sendBroadcast(new Intent("UPDATE_PLAYLIST"));
                        }
                    }

                    return false;
                }
            });


        }
    }

}
