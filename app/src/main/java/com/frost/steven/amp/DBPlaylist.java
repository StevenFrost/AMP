package com.frost.steven.amp;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    public Long   Id;
    public String Name;
    public Long   DateAdded;

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

    public String getFormattedDateAdded()
    {
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        return date.format(new Date(DateAdded));
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

    public static class ListCreator extends AsyncTask<Void, DBPlaylist, Void>
    {
        private static final String[] s_projection = {
            MediaStore.Audio.Playlists._ID,         /** Playlist ID   */
            MediaStore.Audio.Playlists.NAME,        /** Playlist Name */
            MediaStore.Audio.Playlists.DATE_ADDED   /** Creation date */
        };

        private ContentResolver          m_contentResolver;
        private List<DBPlaylist> m_playlists;
        private boolean                  m_complete;

        private List<OnUnresolvedPlaylistsCompletedListener> m_onPlaylistsCompletedListeners;

        public ListCreator(ContentResolver contentResolver, List<DBPlaylist> playlists)
        {
            m_complete        = false;
            m_contentResolver = contentResolver;
            m_playlists       = playlists;

            m_onPlaylistsCompletedListeners = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            m_playlists.clear();

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
                long date = cursor.getLong(dateIdx);

                DBPlaylist playlist = new DBPlaylist(id, name, date);
                m_playlists.add(playlist);
            }

            cursor.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            m_complete = true;
            for (OnUnresolvedPlaylistsCompletedListener listener : m_onPlaylistsCompletedListeners)
            {
                listener.onUnresolvedPlaylistsCompleted();
            }
        }

        public void addOnUnresolvedPlaylistsCompletedListener(OnUnresolvedPlaylistsCompletedListener listener)
        {
            if (m_complete)
            {
                listener.onUnresolvedPlaylistsCompleted();
                return;
            }
            m_onPlaylistsCompletedListeners.add(listener);
        }

        public interface OnUnresolvedPlaylistsCompletedListener
        {
            void onUnresolvedPlaylistsCompleted();
        }
    }
}
