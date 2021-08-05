package com.ecsoftlzx.somusic.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ecsoftlzx.somusic.Entity.Music;
import com.ecsoftlzx.somusic.R;

import java.util.List;

public class MusicSearchListViewAdapter extends BaseAdapter {

    private List<Music> musicList;
    private Context context;

    public MusicSearchListViewAdapter(List<Music> musicList, Context context) {
        this.musicList = musicList;
        this.context = context;
    }

    @Override
    public int getCount() {
        // musicList.size()
        return musicList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class ViewHolder{
        TextView lvMusicTitle;
        TextView lvMusicAuthor;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView;
        ViewHolder viewHolder = new ViewHolder();
        if (convertView != null){
            itemView = convertView;
        } else {
            itemView = LayoutInflater.from(context).inflate(R.layout.list_item_music_item,parent,false);
        }

        viewHolder.lvMusicTitle = itemView.findViewById(R.id.item_tv_title);
        viewHolder.lvMusicAuthor = itemView.findViewById(R.id.item_tv_author);
        //Get entity
        Music musicItem = musicList.get(position);
        viewHolder.lvMusicTitle.setText(musicItem.getMusicName());
        List<String> authors = musicItem.getAuthors();
        StringBuilder sb = new StringBuilder();
        for (String atu:authors){
            sb.append(atu).append(",");
        }
        viewHolder.lvMusicAuthor.setText(sb.substring(0,sb.length()-1));

        return itemView;
    }
}
