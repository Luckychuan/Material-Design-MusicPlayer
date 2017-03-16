package com.example.luckychuan.musicplayer.adapter;


import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.activity.EditInfoActivity;
import com.example.luckychuan.musicplayer.activity.MusicInAlbumActivity;
import com.example.luckychuan.musicplayer.activity.MusicInArtistActivity;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.MenuUtil;

import java.util.List;

public class CurrentMusicListRecyclerViewAdapter extends RecyclerView.Adapter<CurrentMusicListRecyclerViewAdapter.ViewHolder> {

    private Context mContext;
    private List<MusicInfo> mList;
    private RecyclerViewClickListener mListener;

    public CurrentMusicListRecyclerViewAdapter(List<MusicInfo> list) {
        mList = list;
    }

    public void setItemClickListener(RecyclerViewClickListener listener) {
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.current_music_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MusicInfo musicInfo = mList.get(position);
        holder.artist.setText(musicInfo.getArtist());
        holder.title.setText(musicInfo.getTitle());
        //将选中的音乐高亮
        if (PlayService.getPosition() == position) {
            holder.artist.setTextColor(mContext.getColor(R.color.colorPrimary));
            holder.title.setTextColor(mContext.getColor(R.color.colorPrimary));
        } else {
            holder.artist.setTextColor(0xff757575);
            holder.title.setTextColor(0xff000000);
        }

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView artist;
        private ImageButton menuButton;

        public ViewHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.current_music_list_item_textView);
            this.artist = (TextView) itemView.findViewById(R.id.current_music_list_item_textViewSmall);
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

            final PopupMenu popup = new PopupMenu(mContext, menuButton);
            Menu menu = popup.getMenu();
            menu.add(Menu.NONE, R.id.remove, 0, "从列表中移除");
            popup.getMenuInflater().inflate(R.menu.menu_music_info, menu);
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
                            if (!PlayService.getMusicInfo().toString().equals(listItem.toString())) {
                                Intent intent = new Intent("INSERT_MUSIC");
                                intent.putExtra("music", listItem);
                                mContext.sendBroadcast(intent);
                            } else {
                                Toast.makeText(mContext, listItem.getTitle() + " 正在播放中", Toast.LENGTH_SHORT).show();
                            }
                            break;

                        case R.id.music_in_album_activity:
                            Intent intent1 = new Intent(mContext, MusicInAlbumActivity.class);
                            intent1.putExtra("album", MediaStoreManager.getInstance(mContext).queryAlbumData(listItem.getAlbumId()));
                            mContext.startActivity(intent1);
                            break;

                        case R.id.music_in_artist_activity:
                            Intent intent2 = new Intent(mContext, MusicInArtistActivity.class);
                            intent2.putExtra("artist", MediaStoreManager.getInstance(mContext).queryArtistData(listItem.getArtist()));
                            mContext.startActivity(intent2);
                            break;

//                        case R.id.delete:
//                            MenuUtil.deleteMusic(mContext, listItem, new MenuUtil.OnDeleteDialogConfirmFinishListener() {
//                                @Override
//                                public void OnDeleteDialogConfirmFinish() {
//                                    mList.remove(getLayoutPosition());
//                                    notifyItemRemoved(getLayoutPosition());
//                                    //                                  Log.d(TAG, "OnPlaylistDeleteConfirmFinish: ");
//                                }
//                            });
//                            break;

//

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
