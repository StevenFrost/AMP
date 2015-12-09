package com.frost.steven.amp.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class represents a playlist in the media store and only holds basic
 * information for resolving the playlist further and efficiently
 * displaying it in places where more detailed playlist information isn't
 * required, such as in the 'Add Playlist' menu.
 */
public class DBPlaylist implements Parcelable
{
    public Long   Id;           /** Playlist database ID                  */
    public String Name;         /** Playlist name                         */
    public Long   DateAdded;    /** Dat on which the playlist was created */

    public DBPlaylist(Long id, String name, Long dateAdded)
    {
        Id        = id;
        Name      = name;
        DateAdded = dateAdded;
    }

    private DBPlaylist(Parcel parcel)
    {
        Id        = parcel.readLong();
        Name      = parcel.readString();
        DateAdded = parcel.readLong();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(Id);
        dest.writeString(Name);
        dest.writeLong(DateAdded);
    }

    /**
     * Adds a new track to the playlist table
     *
     * @param contentResolver Application content resolver
     * @param trackIdx        Database index of the track to insert
     *
     * @return An async task representing the operation
     */
    public AddTrackTask addTrack(ContentResolver contentResolver, long trackIdx)
    {
        AddTrackTask task = new AddTrackTask(contentResolver, trackIdx);
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return task;
    }

    /**
     * Removes an existing track from the playlist table
     *
     * @param contentResolver Application content resolver
     * @param trackIdx        Database index of the track to remove
     *
     * @return An async task representing the operation
     */
    public RemoveTrackTask removeTrack(ContentResolver contentResolver, long trackIdx)
    {
        RemoveTrackTask task = new RemoveTrackTask(contentResolver, trackIdx);
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return task;
    }

    /**
     * Gets the date the playlist was created as a string in the format dd/MM/yyy.
     *
     * @return Formatted string
     */
    public String getFormattedDateAdded()
    {
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        Date d = new Date(DateAdded.longValue());
        String s = date.format(d);
        return s;
    }

    /**
     * Async task representing a track being inserted into a playlist.
     */
    public class AddTrackTask extends AsyncTask<Void, Void, Void>
    {
        private ContentResolver m_contentResolver;
        private long            m_trackIdx;

        public AddTrackTask(ContentResolver contentResolver, long trackIdx)
        {
            m_contentResolver = contentResolver;
            m_trackIdx = trackIdx;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", Id);
            Cursor cursor = m_contentResolver.query(
                uri,
                new String[] { MediaStore.Audio.Playlists.Members._ID, MediaStore.Audio.Playlists.Members.PLAY_ORDER },
                null,
                null,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC"
            );

            int nextPlayOrder = 1;
            if (cursor != null)
            {
                if (cursor.moveToLast())
                {
                    final int playOrderIdx = cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
                    nextPlayOrder = (cursor.getInt(playOrderIdx)) + 1;
                }
                cursor.close();
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, m_trackIdx);
            contentValues.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, nextPlayOrder);

            m_contentResolver.insert(uri, contentValues);
            return null;
        }
    }

    /**
     * Async task representing a track being removed from a playlist
     */
    public class RemoveTrackTask extends AsyncTask<Void, Void, Void>
    {
        private ContentResolver m_contentResolver;
        private long            m_trackIdx;

        public RemoveTrackTask(ContentResolver contentResolver, long trackIdx)
        {
            m_contentResolver = contentResolver;
            m_trackIdx = trackIdx;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", Id);
            m_contentResolver.delete(uri, "_id=" + m_trackIdx, null);

            return null;
        }
    }

    /**
     * An async task that populates a list with in-memory representations of
     * playlists currently in the media store. These playlists can then be
     * added to or removed from.
     */
    public static class ListCreator extends AsyncTask<Void, DBPlaylist, Void>
    {
        private static final String[] s_projection = {
            MediaStore.Audio.Playlists._ID,         /** Playlist ID   */
            MediaStore.Audio.Playlists.NAME,        /** Playlist Name */
            MediaStore.Audio.Playlists.DATE_ADDED   /** Creation date */
        };

        private ContentResolver  m_contentResolver;
        private List<DBPlaylist> m_playlists;

        public ListCreator(ContentResolver contentResolver, List<DBPlaylist> playlists)
        {
            m_contentResolver = contentResolver;
            m_playlists       = playlists;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            Cursor cursor = m_contentResolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                s_projection,
                null,
                null,
                null
            );

            if (cursor == null) { return null; }

            final int idIdx   = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID);
            final int nameIdx = cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            final int dateIdx = cursor.getColumnIndex(MediaStore.Audio.Playlists.DATE_ADDED);

            while (cursor.moveToNext())
            {
                if (isCancelled()) { break; }

                long id = cursor.getLong(idIdx);
                String name = cursor.getString(nameIdx);
                long date = cursor.getLong(dateIdx) * 1000;

                publishProgress(new DBPlaylist(id, name, date));
            }

            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(DBPlaylist ... progress)
        {
            m_playlists.add(progress[0]);
        }
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        public DBPlaylist createFromParcel(Parcel parcel)
        {
            return new DBPlaylist(parcel);
        }

        public DBPlaylist[] newArray(int size)
        {
            return new DBPlaylist[size];
        }
    };
}
