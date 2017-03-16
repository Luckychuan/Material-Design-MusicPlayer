package com.example.luckychuan.musicplayer.adapter;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.Album;
import com.example.luckychuan.musicplayer.util.DeleteUtil;
import com.example.luckychuan.musicplayer.util.ImageUtil;

import java.util.List;

/**
 * AlbumFragment的RecyclerView的适配器
 */
public class AlbumRecyclerViewAdapter extends RecyclerView.Adapter<AlbumRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "AlbumRecyclerViewAdapter";

    private List<Album> mList;
    private RecyclerViewClickListener mListener;
    private Context mContext;

    public AlbumRecyclerViewAdapter(List<Album> albums){
        mList = albums;
    }

    @Override
    public AlbumRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.album_recycler_view_item, null));
    }

    @Override
    public void onBindViewHolder(final AlbumRecyclerViewAdapter.ViewHolder holder, int position) {
        Album album = mList.get(position);
        holder.textView.setText(album.getAlbumName());
        holder.textViewLight.setText(album.getArtist()+"  (" + album.getNumberOfSongs() + ")");
        ImageUtil.displayAlbumListImageByUniversalImageLoader(album.getAlbumUri().toString(), holder.imageView,holder.itemLayout,holder.textView,holder.textViewLight);

        //选择时让选择的item变黑
        if (DeleteUtil.isContain(album.getId()) ) {
            holder.deleteView.setVisibility(View.VISIBLE);
        } else {
            holder.deleteView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setOnItemClickListener(RecyclerViewClickListener listener) {
        mListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView textView;
        private TextView textViewLight;
        private ImageView deleteView;
        private LinearLayout itemLayout;
        private ImageButton menuButton;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.album_item_imageView);
            textView = (TextView) itemView.findViewById(R.id.album_item_textView);
            textViewLight = (TextView) itemView.findViewById(R.id.album_item_textView_light);
            deleteView = (ImageView) itemView.findViewById(R.id.delete_view);
            itemLayout = (LinearLayout) itemView.findViewById(R.id.album_item_layout);
            menuButton = (ImageButton) itemView.findViewById(R.id.popup_menu);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation((Activity) mContext,imageView,"image");
                        mListener.onItemClick(getLayoutPosition(),options);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mListener != null) {
                        mListener.onItemLongClick(getLayoutPosition());
                    }
                    return false;
                }
            });

            final PopupMenu popup = new PopupMenu(mContext,menuButton);
            popup.getMenuInflater().inflate(R.menu.menu_album_and_artist,popup.getMenu());
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popup.show();
                }
            });

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return false;
                }
            });



        }
    }

}
