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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.activity.EditInfoActivity;
import com.example.luckychuan.musicplayer.activity.MusicInAlbumActivity;
import com.example.luckychuan.musicplayer.activity.MusicInArtistActivity;
import com.example.luckychuan.musicplayer.activity.SelectMusicActivity;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;
import com.example.luckychuan.musicplayer.service.PlayService;
import com.example.luckychuan.musicplayer.util.DeleteUtil;
import com.example.luckychuan.musicplayer.util.ImageUtil;
import com.example.luckychuan.musicplayer.util.MenuUtil;

import java.util.List;

/**
 * Created by Luckychuan on 2016/9/15.
 */
public class MusicInfoRecyclerViewAdapter extends RecyclerView.Adapter<MusicInfoRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "MusicInfoRecyclerViewAdapter";
    private List<MusicInfo> mList;
    private Context mContext;
    private RecyclerViewClickListener mListener;


    public MusicInfoRecyclerViewAdapter(List<MusicInfo> list) {
        mList = list;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.music_info_list_view_item, parent, false));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MusicInfo musicInfo = mList.get(position);
        holder.artist.setText(musicInfo.getArtist());
        holder.title.setText(musicInfo.getTitle());
        ImageUtil.displayMusicListImageByUniversalImageLoader(musicInfo.getAlbumUri().toString(), holder.album);
        //将选中的音乐高亮
        if (PlayService.getMusicInfo() != null) {
            if (musicInfo.toString().equals(PlayService.getMusicInfo().toString())) {
                holder.artist.setTextColor(mContext.getColor(R.color.colorPrimary));
                holder.title.setTextColor(mContext.getColor(R.color.colorPrimary));
            } else {
                holder.artist.setTextColor(0xff757575);
                holder.title.setTextColor(0xff000000);
            }
        }
        //选择音乐时让选择的item变黑
        if (DeleteUtil.isContain(musicInfo.getId()) || SelectMusicActivity.isContain(musicInfo.getId())) {
            holder.itemLayout.setBackgroundColor(0x66C0C0C0);
        } else {
            //设置水波纹效果
            TypedValue outValue = new TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            holder.itemLayout.setBackgroundResource(outValue.resourceId);
        }
    }

    public void setItemClickListener(RecyclerViewClickListener listener) {
        mListener = listener;
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView artist;
        public ImageView album;
        public LinearLayout itemLayout;
        private ImageButton menuButton;

        public ViewHolder(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.musicName_list);
            this.artist = (TextView) itemView.findViewById(R.id.artist_list);
            this.album = (ImageView) itemView.findViewById(R.id.album_list);
            this.itemLayout = (LinearLayout) itemView.findViewById(R.id.list_view_item);
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
                            if(!PlayService.getMusicInfo().toString().equals(listItem.toString())){
                                Intent intent = new Intent("INSERT_MUSIC");
                                intent.putExtra("music", listItem);
                                mContext.sendBroadcast(intent);
                            }else{
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

                        case R.id.delete:
//                            MenuUtil.deleteMusic(mContext, listItem, new MenuUtil.OnDeleteDialogConfirmFinishListener() {
//                                @Override
//                                public void OnDeleteDialogConfirmFinish() {
//                                    mList.remove(getLayoutPosition());
//                                    notifyItemRemoved(getLayoutPosition());
//  //                                  Log.d(TAG, "OnPlaylistDeleteConfirmFinish: ");
//                                }
//                            });
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