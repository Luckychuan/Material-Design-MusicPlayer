package com.example.luckychuan.musicplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.database.MediaStoreManager;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.MusicInfo;
import com.example.luckychuan.musicplayer.model.PlaylistInfo;
import com.example.luckychuan.musicplayer.util.DeleteUtil;
import com.example.luckychuan.musicplayer.util.ImageUtil;
import com.example.luckychuan.musicplayer.util.MenuUtil;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.Serializable;
import java.util.List;


public class PlaylistRecyclerViewAdapter extends RecyclerView.Adapter<PlaylistRecyclerViewAdapter.ViewHolder> {

    private List<PlaylistInfo> mList;
    private Context mContext;
    private RecyclerViewClickListener mListener;

    public PlaylistRecyclerViewAdapter(List<PlaylistInfo> playlistInfoList) {
        mList = playlistInfoList;
    }


    @Override
    public PlaylistRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.playinglist_listview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(PlaylistRecyclerViewAdapter.ViewHolder holder, int position) {
        PlaylistInfo playlistInfo = mList.get(position);
        holder.name.setText(playlistInfo.getName());
        holder.size.setText(playlistInfo.getSize() + " 首音乐");
        if (playlistInfo.getFirstMusicAlbumUrl() == null) {
            holder.imageView.setImageResource(R.drawable.play_page_default_cover);
        } else {
            ImageUtil.displayMusicListImageByUniversalImageLoader(playlistInfo.getFirstMusicAlbumUrl(), holder.imageView);
        }
        //选择时让选择的item变黑
        if (DeleteUtil.isContain(playlistInfo.getId())) {
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

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public RoundedImageView imageView;
        public TextView name;
        public TextView size;
        public RelativeLayout itemLayout;
        private ImageButton menuButton;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (RoundedImageView) itemView.findViewById(R.id.play_list_image);
            this.name = (TextView) itemView.findViewById(R.id.playlist_item_textView);
            this.size = (TextView) itemView.findViewById(R.id.playlist_item_textViewSmall);
            this.itemLayout = (RelativeLayout) itemView.findViewById(R.id.playlist_item);
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
            popup.getMenuInflater().inflate(R.menu.menu_playlist, popup.getMenu());
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popup.show();
                }
            });

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                private PlaylistInfo listItem;

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    //headerView为0，占了一个位置
                    listItem = mList.get(getLayoutPosition()-1);
                    int id = item.getItemId();

                    switch (id) {
                        case R.id.rename:
                           menuRename();
                            break;
                        case R.id.add_to_current_list:
                            addToCurrentList();
                            break;
                        case R.id.delete:
                            delete();
                            break;
                    }


                    return false;
                }

                private void delete() {
                    MediaStoreManager.getInstance(mContext).deletePlaylistData(listItem.getId());
                    mList.remove(getLayoutPosition()-1);
                    notifyItemRemoved(getLayoutPosition());
                }

                private void addToCurrentList() {
                    Intent intent = new Intent("ADD_TO_CURRENT_LIST");
                    List<MusicInfo> musicInfoList= MediaStoreManager.getInstance(mContext).queryMusicDataInPlaylist(listItem.getId());
                    intent.putExtra("list", (Serializable) musicInfoList);
                    mContext.sendBroadcast(intent);
                }

                private void menuRename() {
                    MenuUtil.editPlaylist(mContext, new MenuUtil.OnPlaylistDialogConfirmFinishListener() {
                        @Override
                        public void OnPlaylistDialogConfirmFinish(String editName) {
                            MediaStoreManager.getInstance(mContext).updatePlaylistName(listItem.getId(),editName);
                            listItem.setName(editName);
                            notifyItemChanged(getLayoutPosition());
                        }
                    });
                }

            });
        }

    }
}
