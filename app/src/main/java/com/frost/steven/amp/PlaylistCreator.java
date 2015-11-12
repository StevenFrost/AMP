package com.frost.steven.amp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class PlaylistCreator extends AsyncTask<Void, AudioTrack, Void>
{
    private static final String   s_selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
    private static final String[] s_projection = {
        MediaStore.Audio.Media.ALBUM_ID,    /** Cover art ID             */
        MediaStore.Audio.Media.TITLE,       /** Song title               */
        MediaStore.Audio.Media.ARTIST,      /** Primary artist           */
        MediaStore.Audio.Media.ALBUM,       /** Album title              */
        MediaStore.Audio.Media.DATA,        /** Path to the audio file   */
        MediaStore.Audio.Media.DURATION     /** Duration in milliseconds */
    };

    private ContentResolver            m_contentResolver;
    private OnTrackInsertedListener    m_onTrackInserted;
    private OnPlaylistCompleteListener m_onPlaylistComplete;

    private Playlist m_playlist;    /** The playlist to write to      */
    private String   m_selection;   /** Additional selection criteria */
    private String   m_orderBy;     /** The field to order by         */

    public PlaylistCreator(ContentResolver contentResolver, Playlist playlist, @Nullable String[] selection, @Nullable String orderBy)
    {
        m_contentResolver = contentResolver;
        m_playlist = playlist;
        m_orderBy = orderBy;

        m_selection = s_selection;
        if (selection != null)
        {
            for (String cond : selection)
            {
                m_selection += " AND " + cond;
            }
        }
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        m_playlist.Tracks = new ArrayList<>();
        m_playlist.Position = 0;
        m_playlist.Shuffle = false;

        Cursor cursor = m_contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            s_projection,
            m_selection,
            null,
            m_orderBy
        );

        while (cursor != null && cursor.moveToNext())
        {
            if (isCancelled()) { break; }

            long albumId  = cursor.getLong(0);
            String title  = cursor.getString(1);
            String artist = cursor.getString(2);
            String album  = cursor.getString(3);
            String data   = cursor.getString(4);
            int duration  = cursor.getInt(5);

            Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri albumArtworkUri = ContentUris.withAppendedId(artworkUri, albumId);

            try
            {
                // Attempt to read the album art. We don't care about accessing
                // the contents of the file yet, it's just useful to know if it
                // exists in advance
                m_contentResolver.openFileDescriptor(albumArtworkUri, "r");
            }
            catch (FileNotFoundException ex)
            {
                albumArtworkUri = null;
            }

            AudioTrack track = new AudioTrack(title, artist, album, data, albumArtworkUri, duration);
            m_playlist.Tracks.add(track);
            publishProgress(track);
        }

        if (cursor != null)
        {
            cursor.close();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(AudioTrack... progress)
    {
        m_onTrackInserted.onTrackInserted(progress[0]);
    }

    @Override
    protected void onPostExecute(Void result)
    {
        m_onPlaylistComplete.onPlaylistComplete();
    }

    public void setOnTrackInsertedListener(OnTrackInsertedListener listener)
    {
        m_onTrackInserted = listener;
    }

    public void setOnPlaylistCompleteListener(OnPlaylistCompleteListener listener)
    {
        m_onPlaylistComplete = listener;
    }

    public interface OnTrackInsertedListener
    {
        void onTrackInserted(AudioTrack track);
    }

    public interface OnPlaylistCompleteListener
    {
        void onPlaylistComplete();
    }
}
