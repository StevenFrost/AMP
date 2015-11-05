package com.frost.steven.amp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AudioTrackAdapter extends ArrayAdapter<AudioTrack>
{
    private Context    m_context;
    private int        m_layoutResourceId;
    private AudioTrack m_data[] = null;

    public AudioTrackAdapter(Context context, int layoutResourceId, AudioTrack[] data)
    {
        super(context, layoutResourceId, data);
        m_context = context;
        m_layoutResourceId = layoutResourceId;
        m_data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        AudioTrackViewGroup viewGroup;

        if (row == null)
        {
            LayoutInflater inflater = ((Activity)m_context).getLayoutInflater();
            row = inflater.inflate(m_layoutResourceId, parent, false);

            viewGroup = new AudioTrackViewGroup();
            viewGroup.AlbumArt = (ImageView)row.findViewById(R.id.tablerow_song_albumart);
            viewGroup.Title = (TextView)row.findViewById(R.id.tablerow_song_title);
            viewGroup.Artist = (TextView)row.findViewById(R.id.tablerow_song_artist);
            viewGroup.Album = (TextView)row.findViewById(R.id.tablerow_song_album);

            row.setTag(viewGroup);
        }
        else
        {
            viewGroup = ((AudioTrackViewGroup)row.getTag());
        }

        if (BitmapWorkerTask.cancelOutstandingWork(m_data[position], viewGroup.AlbumArt))
        {
            final BitmapWorkerTask task = new BitmapWorkerTask(m_context.getContentResolver(), viewGroup.AlbumArt);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(row.getResources(), null, task);
            viewGroup.AlbumArt.setImageDrawable(asyncDrawable);
            task.execute(m_data[position]);
        }

        AudioTrack track = m_data[position];
        viewGroup.Title.setText(track.Title);
        viewGroup.Artist.setText(track.Artist);
        viewGroup.Album.setText(track.Album);

        return row;
    }

    static class AudioTrackViewGroup
    {
        ImageView AlbumArt;
        TextView  Title;
        TextView  Artist;
        TextView  Album;
    }
}
