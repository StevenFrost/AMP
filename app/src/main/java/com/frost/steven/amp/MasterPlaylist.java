package com.frost.steven.amp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

public class MasterPlaylist extends Playlist
{
    private static String s_selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
    private static String[] s_projection = {
        MediaStore.Audio.Media.ALBUM_ID,    /** Cover art ID             */
        MediaStore.Audio.Media.TITLE,       /** Song title               */
        MediaStore.Audio.Media.ARTIST,      /** Primary artist           */
        MediaStore.Audio.Media.ALBUM,       /** Album title              */
        MediaStore.Audio.Media.DATA,        /** Path to the audio file   */
        MediaStore.Audio.Media.DURATION     /** Duration in milliseconds */
    };

    /**
     * Constructs the master playlist. All music stored in external storage will
     * be scanned and placed in a new playlist.
     *
     * @param resolver the content resolover
     */
    MasterPlaylist(ContentResolver resolver)
    {
        Tracks   = new ArrayList<>();
        Position = 0;
        Shuffle  = false;

        try
        {
            Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, s_projection, s_selection, null, null);

            while (cursor.moveToNext())
            {
                long albumId  = cursor.getLong(0);
                String title  = cursor.getString(1);
                String artist = cursor.getString(2);
                String album  = cursor.getString(3);
                String data   = cursor.getString(4);
                long duration = cursor.getLong(5);

                Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
                Uri albumArtworkUri = ContentUris.withAppendedId(artworkUri, albumId);

                Tracks.add(new AudioTrack(title, artist, album, data, albumArtworkUri, duration));
            }
            cursor.close();
        }
        catch (NullPointerException ex)
        {
            System.err.println("Some bad shit happened");
        }
    }
}
