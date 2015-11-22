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
import java.util.List;
import java.util.Random;

/**
 * A playlist is a collection of `AudioTrack` objects. This is the only object
 * that should be sent to the player. If the user selects a single song in the
 * global songs list then a playlist containing every song will be submitted
 * with a start index of the item pressed in the list.
 */
public class Playlist
{
    private List<AudioTrack> m_originalTracks;
    private List<AudioTrack> m_shuffledTracks;
    private Integer          m_cursor;

    private Random           m_random;
    private Boolean          m_repeat;
    private Boolean          m_shuffle;

    public Playlist()
    {
        m_originalTracks = new ArrayList<>();
        m_shuffledTracks = new ArrayList<>();
        m_cursor = 0;
        m_random = new Random();
        m_repeat = false;
        m_shuffle = false;
    }

    public void addTrack(AudioTrack track)
    {
        m_originalTracks.add(track);
    }

    public void setShuffle(boolean shuffle)
    {
        if (!m_shuffle && shuffle)
        {
            m_shuffle = true;

            m_shuffledTracks.clear();
            for (AudioTrack track : m_originalTracks)
            {
                m_shuffledTracks.add(track);
            }

            // Swap the current song and the first song
            AudioTrack temp = m_shuffledTracks.get(0);
            m_shuffledTracks.set(0, m_shuffledTracks.get(m_cursor));
            m_shuffledTracks.set(m_cursor, temp);

            // Shuffle the rest of the list
            for (int i = m_shuffledTracks.size() - 1; i > 0; --i)
            {
                int idx = m_random.nextInt(i == 1 ? 1 : i - 1) + 1;
                AudioTrack track = m_shuffledTracks.get(i);
                m_shuffledTracks.set(i, m_shuffledTracks.get(idx));
                m_shuffledTracks.set(idx, track);
            }

            m_cursor = 0;
        }
        else if (m_shuffle && !shuffle)
        {
            m_shuffle = false;

            // Find the position of the current song in the original list
            int cursor = m_cursor;
            m_cursor = 0;
            for (int i = 0; i < m_originalTracks.size(); ++i)
            {
                if (m_shuffledTracks.get(cursor) == m_originalTracks.get(i))
                {
                    m_cursor = i;
                    break;
                }
            }

            m_shuffledTracks.clear();
        }
    }

    public void setRepeat(boolean repeat)
    {
        m_repeat = repeat;
    }

    public AudioTrack moveToPreviousTrack()
    {
        if (m_cursor == 0 && m_repeat)
        {
            m_cursor = getNumTracks() - 1;
        }
        else if (m_cursor != 0)
        {
            --m_cursor;
        }
        return getCurrentTrack();
    }

    public void setCursor(int cursor)
    {
        m_cursor = cursor;
    }

    public AudioTrack getCurrentTrack()
    {
        if (m_shuffle)
        {
            return m_shuffledTracks.get(m_cursor);
        }
        return m_originalTracks.get(m_cursor);
    }

    public AudioTrack moveToNextTrack()
    {
        int end = getNumTracks() - 1;
        if (m_cursor == end && m_repeat)
        {
            m_cursor = 0;
        }
        else if (m_cursor != end)
        {
            ++m_cursor;
        }
        return getCurrentTrack();
    }

    public int getNumTracks()
    {
        return (m_shuffle ? m_shuffledTracks.size() : m_originalTracks.size());
    }

    public AudioTrack getUnshuffledTrack(int position)
    {
        return m_originalTracks.get(position);
    }

    public boolean hasNextTrack()
    {
        return m_repeat || m_cursor < (getNumTracks() - 1);
    }

    public boolean hasPreviousTrack()
    {
        return m_repeat || m_cursor > 0;
    }

    public static class ListCreator extends AsyncTask<Void, AudioTrack, Void>
    {
        private static final String   s_extVolume = "external";
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

        private Long     m_playlistId;  /** The ID of the MediaStore playlist to create this one from */

        /**
         * Fills the given playlist with a selection of audio tracks based on
         * the given selection and ordering criteria. Common use cases are
         * creating a playlist containing all audio tracks or all tracks in a
         * specific album.
         *
         * @param contentResolver   content resolver object
         * @param playlist          the playlist to populate
         * @param selection         selection criteria such as ''MediaStore.Audio.Media.ARTIST' == 'Coldplay''
         * @param orderBy           ordering criteria such as ''MediaStore.Audio.Media.ARTIST' ASC'
         */
        public ListCreator(ContentResolver contentResolver, Playlist playlist, @Nullable String[] selection, @Nullable String orderBy)
        {
            this(contentResolver, playlist, selection, orderBy, null);
        }

        /**
         * Fills the given playlist with a selection of audio tracks based on
         * the given selection and ordering criteria. This constructor can also
         * take a playlist ID which will only fill the playlist with songs in
         * that given MediaStore playlist. Selection and ordering criteria will
         * then be applied after the initial cull.
         *
         * @param contentResolver   content resolver object
         * @param playlist          the playlist to populate
         * @param selection         selection criteria such as ''MediaStore.Audio.Media.ARTIST' == 'Coldplay''
         * @param orderBy           ordering criteria such as ''MediaStore.Audio.Media.ARTIST' ASC'
         * @param playlistId        the ID of the MediaStore playlist to populate this playlist with
         */
        public ListCreator(ContentResolver contentResolver, Playlist playlist, @Nullable String[] selection, @Nullable String orderBy, @Nullable Long playlistId)
        {
            m_contentResolver = contentResolver;
            m_playlist = playlist;
            m_orderBy = orderBy;
            m_playlistId = playlistId;

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
            // The content URI is different depending on whether we're dealing
            // with MediaStore playlists or a playlist created from the whole
            // list of songs in the media store. The projection remains the
            // same however.
            Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            if (m_playlistId != null)
            {
                contentUri = MediaStore.Audio.Playlists.Members.getContentUri(s_extVolume, m_playlistId);
            }

            Cursor cursor = m_contentResolver.query(
                contentUri,
                s_projection,
                m_selection,
                null,
                m_orderBy
            );
            if (cursor == null) { return null; }

            // Common column indices
            final int albumIdIdx  = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            final int titleIdx    = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            final int artistIdx   = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int albumIdx    = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            final int dataIdx     = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            final int durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            while (cursor.moveToNext())
            {
                if (isCancelled()) { break; }

                long albumId  = cursor.getLong(albumIdIdx);
                String title  = cursor.getString(titleIdx);
                String artist = cursor.getString(artistIdx);
                String album  = cursor.getString(albumIdx);
                String data   = cursor.getString(dataIdx);
                int duration  = cursor.getInt(durationIdx);

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
                m_playlist.addTrack(track);
                publishProgress(track);
            }

            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(AudioTrack... progress)
        {
            if (m_onTrackInserted != null)
            {
                m_onTrackInserted.onTrackInserted(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if (m_onPlaylistComplete != null)
            {
                m_onPlaylistComplete.onPlaylistComplete();
            }
        }

        public Playlist getPlaylist()
        {
            return m_playlist;
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
}
