package com.frost.steven.amp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class AudioTrackAdapter extends ArrayAdapter<AudioTrack>
{
    Context    context;
    int        layoutResourceId;
    AudioTrack data[] = null;

    public AudioTrackAdapter(Context context, int layoutResourceId, AudioTrack[] data)
    {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        AudioTrackViewGroup viewGroup;

        if (row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

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

        // Album art
        Bitmap bitmap = null;
        try
        {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), data[position].CoverArt);
            bitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true);

        }
        catch (FileNotFoundException exception)
        {
            exception.printStackTrace();
            bitmap = BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_help);
        }
        catch (IOException | NullPointerException e)
        {
            e.printStackTrace();
        }

        AudioTrack track = data[position];
        viewGroup.AlbumArt.setImageBitmap(bitmap);
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
