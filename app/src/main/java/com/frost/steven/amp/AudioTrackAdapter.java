package com.frost.steven.amp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AudioTrackAdapter extends ArrayAdapter<AudioTrack>
{
    private Context               m_context;
    private int                   m_layoutResourceId;
    private AudioTrack            m_data[] = null;
    private LruCache<Uri, Bitmap> m_cache = null;

    public AudioTrackAdapter(Context context, int layoutResourceId, AudioTrack[] data, LruCache<Uri, Bitmap> cache)
    {
        super(context, layoutResourceId, data);
        m_context = context;
        m_layoutResourceId = layoutResourceId;
        m_data = data;
        m_cache = cache;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        AudioTrackViewGroup viewGroup;
        AudioTrack track = m_data[position];

        if (row == null)
        {
            LayoutInflater inflater = ((Activity)m_context).getLayoutInflater();
            row = inflater.inflate(m_layoutResourceId, parent, false);

            viewGroup = new AudioTrackViewGroup();
            viewGroup.AlbumArtPlaceholder = (ImageView)row.findViewById(R.id.tablerow_song_albumart_placeholder);
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

        if (track.CoverArt != null && BitmapWorkerTask.cancelOutstandingWork(track, viewGroup.AlbumArt))
        {
            Bitmap albumArtBitmap = m_cache.get(track.CoverArt);
            if (albumArtBitmap != null)
            {
                viewGroup.AlbumArt.setImageBitmap(albumArtBitmap);
            }
            else
            {
                final BitmapWorkerTask worker = new BitmapWorkerTask(m_context.getContentResolver(), viewGroup.AlbumArt, m_cache);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(row.getResources(), null, worker);
                viewGroup.AlbumArt.setImageDrawable(asyncDrawable);
                worker.execute(track);
            }
            viewGroup.AlbumArtPlaceholder.setVisibility(View.GONE);
            viewGroup.AlbumArt.setVisibility(View.VISIBLE);
        }
        else if (track.CoverArt == null)
        {
            viewGroup.AlbumArt.setImageBitmap(null);
            viewGroup.AlbumArtPlaceholder.setVisibility(View.VISIBLE);
            viewGroup.AlbumArt.setVisibility(View.GONE);
        }

        viewGroup.Title.setText(track.Title);
        viewGroup.Artist.setText(track.Artist);
        viewGroup.Album.setText(track.Album);

        return row;
    }

    static class AudioTrackViewGroup
    {
        ImageView AlbumArtPlaceholder;
        ImageView AlbumArt;
        TextView  Title;
        TextView  Artist;
        TextView  Album;
    }
}
