package com.frost.steven.amp.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Simple POD type containing the title of an album and a Uri to its artwork
 * if available.
 */
public class Album implements Parcelable
{
    public Long   AlbumID;
    public String Title;
    public String Artist;
    public Uri    Artwork;

    public Album(Long albumID, String title, String artist, @Nullable Uri artwork)
    {
        AlbumID = albumID;
        Title = title;
        Artist = artist;
        Artwork = artwork;
    }

    public Album(Parcel parcel)
    {
        AlbumID = parcel.readLong();
        Title = parcel.readString();
        Artist = parcel.readString();

        String artworkPath = parcel.readString();
        Artwork = artworkPath == null ? null : Uri.parse(artworkPath);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(AlbumID);
        dest.writeString(Title);
        dest.writeString(Artist);
        dest.writeString(Artwork == null ? null : Artwork.toString());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
    {
        public Album createFromParcel(Parcel parcel)
        {
            return new Album(parcel);
        }

        public Album[] newArray(int size)
        {
            return new Album[size];
        }
    };

    /**
     * The list creator is an asynchronous task that generates a list of albums
     * in the media store. Progress will be reported for every album discovered
     * and completion will be triggered when all albums have been inserted into
     * the list.
     *
     * If an album has no artwork, the Uri will be set to `null` to indicate
     * this.
     */
    public static class ListCreator extends AsyncTask<Void, Album, Void>
    {
        private static final String   s_selection  = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        private static final String   s_orderBy    = MediaStore.Audio.Media.ALBUM + " ASC";
        private static final String[] s_projection = {
            MediaStore.Audio.Media.ALBUM,       /** Album title  */
            MediaStore.Audio.Media.ARTIST,      /** Album artist */
            MediaStore.Audio.Media.ALBUM_ID,    /** Cover art ID */
        };

        private ContentResolver         m_contentResolver;
        private OnAlbumInsertedListener m_onAlbumInsertedListener;
        private OnListCompletedListener m_onListCompletedListener;

        private List<Album> m_albums;

        public ListCreator(ContentResolver contentResolver, List<Album> albums)
        {
            m_contentResolver = contentResolver;
            m_albums = albums;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            m_albums.clear();

            Cursor cursor = m_contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                s_projection,
                s_selection,
                null,
                s_orderBy
            );

            if (cursor == null) { return null; }

            // Column indices
            final int titleIdx   = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            final int artistIdx  = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            String curTitle = null;
            while (cursor.moveToNext())
            {
                if (isCancelled()) { break; }

                if (curTitle == null || !curTitle.equals(cursor.getString(0)))
                {
                    curTitle = cursor.getString(0);
                }
                else if (curTitle.equals(cursor.getString(0)))
                {
                    continue;
                }

                String title  = cursor.getString(titleIdx);
                String artist = cursor.getString(artistIdx);
                long albumID  = cursor.getLong(albumIdIdx);

                Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
                Uri albumArtworkUri = ContentUris.withAppendedId(artworkUri, albumID);

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

                Album album = new Album(albumID, title, artist, albumArtworkUri);
                m_albums.add(album);
                publishProgress(album);
            }

            cursor.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(Album... progress)
        {
            if (m_onAlbumInsertedListener != null)
            {
                m_onAlbumInsertedListener.onAlbumInserted(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if (m_onListCompletedListener != null)
            {
                m_onListCompletedListener.onListCompleted();
            }
        }

        public void setOnAlbumInsertedListener(OnAlbumInsertedListener listener)
        {
            m_onAlbumInsertedListener = listener;
        }

        public void setOnListCompletedListener(OnListCompletedListener listener)
        {
            m_onListCompletedListener = listener;
        }

        public interface OnAlbumInsertedListener
        {
            void onAlbumInserted(Album album);
        }

        public interface OnListCompletedListener
        {
            void onListCompleted();
        }
    }
}
