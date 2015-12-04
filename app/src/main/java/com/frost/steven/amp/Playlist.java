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
 *
 * This class is different from DBPlaylist as it represents a 'resolved' set
 * of audio tracks that can be played by the media service rather than a
 * weak reference to a database row which still needs to be resolved further.
 */
public class Playlist
{
    private ListenableArrayList<AudioTrack> m_originalTracks;
    private ArrayList<AudioTrack>           m_shuffledTracks;

    private Integer m_cursor;
    private Random  m_random;
    private Boolean m_repeat;
    private Boolean m_shuffle;

    public Playlist()
    {
        m_originalTracks = new ListenableArrayList<>();
        m_shuffledTracks = new ArrayList<>();

        m_cursor  = 0;
        m_random  = new Random();
        m_repeat  = false;
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

    public void removeTrack(int position)
    {
        AudioTrack track = m_originalTracks.get(position);
        if (m_shuffle)
        {
            int removalPosition = 0;
            for (; removalPosition < m_shuffledTracks.size(); ++removalPosition)
            {
                AudioTrack at = m_shuffledTracks.get(removalPosition);
                if (at.ID == track.ID)
                {
                    break;
                }
            }
            m_shuffledTracks.remove(removalPosition);
        }
        m_originalTracks.remove(position);
    }

    public void attachPlaylistChangedListener(ListenableArrayList.OnCollectionChangedListener listener)
    {
        m_originalTracks.attachListener(listener);
    }

    /**
     * Asynchronous task that builds a playlist given some basic criteria. The
     * resulting playlist can be sent to the media service for playback.
     */
    public static class ListCreator extends AsyncTask<Void, AudioTrack, Void>
    {
        private static final String   s_extVolume = "external";
        private static final String   s_selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        private static final String[] s_projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        };

        private ContentResolver m_contentResolver;

        private List<ProgressListener>   m_progressListeners;
        private List<CompletionListener> m_completionListeners;

        private Playlist m_playlist;
        private String   m_selection;
        private String   m_orderBy;

        private Long     m_playlistId;
        private boolean  m_complete;

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

            m_progressListeners   = new ArrayList<>();
            m_completionListeners = new ArrayList<>();

            m_playlist   = playlist;
            m_orderBy    = orderBy;
            m_playlistId = playlistId;
            m_complete   = false;

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
            final int idIdx       = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            final int albumIdIdx  = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            final int titleIdx    = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            final int artistIdx   = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int albumIdx    = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            final int dataIdx     = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            final int durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            while (cursor.moveToNext())
            {
                if (isCancelled()) { break; }

                long id       = cursor.getLong(idIdx);
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

                AudioTrack track = new AudioTrack(id, title, artist, album, data, albumArtworkUri, duration);
                m_playlist.addTrack(track);
                publishProgress(track);
            }

            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(AudioTrack... progress)
        {
            for (ProgressListener listener : m_progressListeners)
            {
                listener.onPlaylistProgress(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(Void result)
        {
            m_complete = true;
            for (CompletionListener listener : m_completionListeners)
            {
                listener.onPlaylistCompleted();
            }
        }

        public Playlist getPlaylist()
        {
            return m_playlist;
        }

        public void addProgressListener(ProgressListener listener)
        {
            if (!m_complete)
            {
                m_progressListeners.add(listener);
            }
        }

        public void addCompletionListener(CompletionListener listener)
        {
            if (m_complete)
            {
                listener.onPlaylistCompleted();
                return;
            }
            m_completionListeners.add(listener);
        }

        public interface ProgressListener
        {
            void onPlaylistProgress(AudioTrack track);
        }

        public interface CompletionListener
        {
            void onPlaylistCompleted();
        }
    }
}
