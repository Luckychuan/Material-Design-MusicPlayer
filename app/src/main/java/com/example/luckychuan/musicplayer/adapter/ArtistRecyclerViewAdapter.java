package com.example.luckychuan.musicplayer.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.luckychuan.musicplayer.listener.RecyclerViewClickListener;
import com.example.luckychuan.musicplayer.model.Artist;
import com.example.luckychuan.musicplayer.R;
import com.example.luckychuan.musicplayer.util.DeleteUtil;

import java.util.List;

/**
 * ArtistFragment的RecyclerView的适配器
 */
public class ArtistRecyclerViewAdapter extends RecyclerView.Adapter<ArtistRecyclerViewAdapter.ViewHolder> {

    private RecyclerViewClickListener mListener;
    private Context mContext;
    private List<Artist> mList;

    public ArtistRecyclerViewAdapter(List<Artist> list) {
        mList = list;
    }


    public void setOnItemClickListener(RecyclerViewClickListener listener) {
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.artist_recycler_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Artist artist = mList.get(position);
        holder.textView.setText(artist.getName());
        holder.textViewSmall1.setText(artist.getNumberOfAlbums()+" 张专辑");
        holder.textViewSmall2.setText(artist.getNumberOfTracks()+" 首歌曲");
        //选择时让选择的item变黑
        if (DeleteUtil.isContain(artist.getId()) ) {
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

        private TextView textView;
        private TextView textViewSmall1;
        private TextView textViewSmall2;
        private LinearLayout itemLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            textView = (TextView) itemView.findViewById(R.id.artist_recycler_item_textView);
            textViewSmall1 = (TextView) itemView.findViewById(R.id.artist_recycler_item_textView_small1);
            itemLayout = (LinearLayout) itemView.findViewById(R.id.artist_recycler_item_layout);
            textViewSmall2 = (TextView) itemView.findViewById(R.id.artist_recycler_item_textView_small2);

            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(getLayoutPosition());
                    }
                }
            });

            itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mListener != null) {
                        mListener.onItemLongClick(getLayoutPosition());
                    }
                    return false;
                }
            });
        }
    }


}
