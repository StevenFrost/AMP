package com.frost.steven.amp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.DropBoxManager;
import android.provider.MediaStore;

/**
 * This class manages the interaction between the application and the internal
 * playlists database provided by the MediaStore. The functionality in this
 * manager allows easy binding to the application UI and is intended to be
 * created for each activity rather than being preserved in a bundle.
 *
 * All creation, edit and removal actions in this class are asynchronous and
 * use the serial executor to guarantee ordering.
 */
public class DBPlaylistManager
{
    private ListenableArrayList<DBPlaylist> m_playlists;

    private ContentResolver m_contentResolver;

    public DBPlaylistManager(ContentResolver contentResolver)
    {
        m_playlists = new ListenableArrayList<>();
        m_contentResolver = contentResolver;
    }

    /**
     * Creates a new playlists in the MediaStore database
     *
     * @param name      name of the new playlist
     * @param trackIdx  index of the track to add as the first in the playlist
     *
     * @return AsyncTask representing the creation operation
     */
    public CreateTask create(String name, long trackIdx)
    {
        CreateTask task = new CreateTask(name, trackIdx);
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return task;
    }

    public EditTask edit(int idx, String name)
    {
        EditTask task = new EditTask(idx, name);
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return task;
    }

    public RemoveTask remove(int idx)
    {
        RemoveTask task = new RemoveTask(idx);
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return task;
    }

    public ListenableArrayList<DBPlaylist> getPlaylists()
    {
        return m_playlists;
    }

    public DBPlaylist getPlaylistAt(int idx)
    {
        return m_playlists.get(idx);
    }

    /**
     *
     */
    public class CreateTask extends AsyncTask<Void, Void, DBPlaylist>
    {
        String m_name;
        long   m_trackIdx;

        public CreateTask(String name, long trackIdx)
        {
            m_name = name;
            m_trackIdx = trackIdx;
        }

        @Override
        protected DBPlaylist doInBackground(Void... params)
        {
            long dateAdded = System.currentTimeMillis();

            ContentValues contentValue = new ContentValues();
            contentValue.put(MediaStore.Audio.Playlists.NAME, m_name);
            contentValue.put(MediaStore.Audio.Playlists.DATE_ADDED, dateAdded);
            contentValue.put(MediaStore.Audio.Playlists.DATE_MODIFIED, dateAdded);

            Uri uri = m_contentResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValue);
            if (uri != null)
            {
                Cursor cursor = m_contentResolver.query(uri, new String[] { MediaStore.Audio.Playlists._ID }, null, null, null);
                if (cursor != null)
                {
                    cursor.moveToLast();
                    long playlistId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
                    cursor.close();

                    return new DBPlaylist(playlistId, m_name, dateAdded);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(DBPlaylist playlist)
        {
            if (playlist != null)
            {
                m_playlists.add(playlist);
                playlist.addTrack(m_contentResolver, m_trackIdx);
            }
            else
            {
                // The playlist already exists, just add the song to that one
                for (DBPlaylist plist : m_playlists)
                {
                    if (plist.Name.equals(m_name))
                    {
                        plist.addTrack(m_contentResolver, m_trackIdx);
                        break;
                    }
                }
            }
        }
    }

    /**
     *
     */
    public class EditTask extends AsyncTask<Void, Void, Long>
    {
        int    m_idx;
        String m_name;

        /**
         * Constructor
         *
         * @param idx   Index of the playlist to edit
         * @param name  New playlist name
         */
        EditTask(int idx, String name)
        {
            m_idx = idx;
            m_name = name;
        }

        @Override
        protected void onPreExecute()
        {
            DBPlaylist playlist = m_playlists.get(m_idx);
            playlist.Name = m_name;

            m_playlists.set(m_idx, playlist);
        }

        @Override
        protected Long doInBackground(Void... params)
        {
            long playlistId = m_playlists.get(m_idx).Id;
            long dateModified = System.currentTimeMillis();

            ContentValues contentValue = new ContentValues();
            contentValue.put(MediaStore.Audio.Playlists.NAME, m_name);
            contentValue.put(MediaStore.Audio.Playlists.DATE_MODIFIED, dateModified);

            m_contentResolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValue, "_id=" + playlistId, null);
            return dateModified;
        }

        @Override
        protected void onPostExecute(Long dateModified)
        {
            DBPlaylist playlist = m_playlists.get(m_idx);
            playlist.Name = m_name;
        }
    }

    /**
     *
     */
    public class RemoveTask extends AsyncTask<Void, Void, Void>
    {
        int        m_idx;
        DBPlaylist m_tempPlaylist;

        public RemoveTask(int idx)
        {
            m_idx = idx;
        }

        @Override
        protected void onPreExecute()
        {
            m_tempPlaylist = m_playlists.get(m_idx);
            m_playlists.remove(m_idx);
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            long playlistId = m_tempPlaylist.Id;
            m_contentResolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, "_id=" + playlistId, null);

            return null;
        }
    }

    public interface Container
    {
        DBPlaylistManager getDBPlaylistManager();
    }
}
