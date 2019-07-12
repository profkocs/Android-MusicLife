package com.xva.musiclife.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.xva.musiclife.R;
import com.xva.musiclife.data.SharedPrefencesHelper;
import com.xva.musiclife.models.Song;
import com.xva.musiclife.utils.EventBusHelper;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.MyViewHolder> {

    private ArrayList<Song> data;
    private onItemClick listener;
    private SharedPrefencesHelper sharedPrefencesHelper;
    private Song lastSong;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mName;
        private TextView mArtist;
        private ImageView mSettings;

        public MyViewHolder(final View itemView, final onItemClick listener) {
            super(itemView);
            mName = itemView.findViewById(R.id.textViewSongName);
            mArtist = itemView.findViewById(R.id.textViewArtist);
            mSettings = itemView.findViewById(R.id.imageViewSongSettings);


        }
    }

    public SongAdapter(ArrayList<Song> data, onItemClick listener, Context context) {
        this.data = data;
        this.listener = listener;
        this.mContext = context;
        sharedPrefencesHelper = new SharedPrefencesHelper(context);
        lastSong = sharedPrefencesHelper.getLastSong();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_songs_list, parent, false);
        return new MyViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.mName.setText(data.get(position).getName());
        holder.mArtist.setText(data.get(position).getArtist());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(holder.itemView, position, "mainView");
                EventBus.getDefault().postSticky(new EventBusHelper.playingSong(data.get(position)));
                sharedPrefencesHelper.saveLastSong(data.get(position));
                lastSong = data.get(position);
                Log.e("position","" + data.get(position).getName());
                EventBus.getDefault().postSticky(new EventBusHelper.isShowMiniPl("show"));
            }
        });

        holder.mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(holder.itemView, position, "settings");
                holder.mSettings.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.scale));
                EventBus.getDefault().postSticky(new EventBusHelper.songInformations(data.get(position)));
            }
        });


        if (lastSong.getPath().equals(data.get(position).getPath())) {
            Log.e("position",lastSong.getName());
            holder.mName.setTextColor(ContextCompat.getColorStateList(mContext, R.color.colorGreen));
            EventBus.getDefault().postSticky(new EventBusHelper.lastSongView(holder.itemView));
        } else {
            holder.mName.setTextColor(ContextCompat.getColorStateList(mContext, R.color.colorWhite));
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    //Item'i silme işlemi
    public void removeItem(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    // Snackbar işlemi tıklandığında, restoreItem metodunu kullanarak öğeyi RecyclerView'da geri yükleriz.
    public void restoreItem(Song item, int position) {
        data.add(position, item);
        notifyItemInserted(position);
    }

    public ArrayList<Song> getData() {
        return data;
    }


    public interface onItemClick {
        void onItemClick(View view, Integer position, String object);
    }


}