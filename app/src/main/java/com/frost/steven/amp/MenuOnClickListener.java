package com.frost.steven.amp;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

public abstract class MenuOnClickListener implements View.OnClickListener
{
    protected int      m_position;
    protected Playlist m_playlist;
    protected Activity m_activity;

    /**
     * Constructor
     *
     * @param activity The parent activity
     */
    public MenuOnClickListener(Activity activity)
    {
        m_activity = activity;
    }

    public void setPosition(int position)
    {
        m_position = position;
    }

    public void setPlaylist(Playlist playlist)
    {
        m_playlist = playlist;
    }

    /**
     * Factory interface that allows the creation of specific listeners through
     * a common interface. This allows a more streamlined song recycler view
     * adapter implementation.
     */
    public interface Factory
    {
        MenuOnClickListener create(Activity activity);
    }

    /**
     * Concrete implementation of a menu click listener for a song element.
     * The menu features the ability to add a song to an existing playlist or
     * create a new playlist, showing a popup dialog box for the user to enter
     * the name of the new playlist.
     */
    public static class SongListener extends MenuOnClickListener
    {
        DBPlaylistManager m_playlistManager;

        /**
         * Constructor
         *
         * @param activity        The parent activity
         * @param playlistManager The database playlist manager
         */
        SongListener(Activity activity, DBPlaylistManager playlistManager)
        {
            super(activity);

            m_playlistManager = playlistManager;
        }

        @Override
        public void onClick(View view)
        {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    final AudioTrack track = m_playlist.getUnshuffledTrack(m_position);

                    if (item.getItemId() == R.id.menu_song_add_to_playlist)
                    {
                        return true;
                    }
                    else if (item.getItemId() == R.id.menu_song_new_playlist)
                    {
                        Bundle bundle = new Bundle();
                        bundle.putLong("trackID", track.ID);

                        DialogFragment df = new NewPlaylistFragment();
                        df.setArguments(bundle);

                        df.show(m_activity.getFragmentManager(), "dialog-new-playlist");
                    }
                    else
                    {
                        DBPlaylist playlist = m_playlistManager.getPlaylistAt(item.getItemId());
                        playlist.addTrack(m_activity.getContentResolver(), track.ID);
                    }
                    return true;
                }
            });
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_song, popup.getMenu());

            MenuItem playlistsMenuItem = popup.getMenu().getItem(0);
            Menu playlistsSubMenu = playlistsMenuItem.getSubMenu();

            List<DBPlaylist> playlists = m_playlistManager.getPlaylists();
            for (int i = 0; i < playlists.size(); ++i)
            {
                DBPlaylist playlist = playlists.get(i);
                playlistsSubMenu.add(Menu.NONE, i, Menu.NONE, playlist.Name);
            }

            popup.show();
        }

        /**
         * Concrete factory implementation for the SongListener. A new instance
         * of the SongListener class is returned via the MenuOnClickListener
         * abstract class object.
         */
        public static class Factory implements MenuOnClickListener.Factory
        {
            private DBPlaylistManager m_playlistManager;

            public Factory(DBPlaylistManager playlistManager)
            {
                m_playlistManager = playlistManager;
            }

            @Override
            public MenuOnClickListener create(Activity activity)
            {
                return new SongListener(activity, m_playlistManager);
            }
        }
    }

    /**
     * Concrete implementation of a menu click listener for a playlist song
     * element. The menu features the ability to remove a song from the current
     * playlist.
     */
    public static class PlaylistSongListener extends MenuOnClickListener
    {
        Playlist   m_playlist;
        DBPlaylist m_databasePlaylist;

        /**
         * Constructor
         *
         * @param activity The parent activity
         * @param playlist The database playlist to use as context
         */
        PlaylistSongListener(Activity activity, Playlist playlist, DBPlaylist databasePlaylist)
        {
            super(activity);

            m_playlist = playlist;
            m_databasePlaylist = databasePlaylist;
        }

        @Override
        public void onClick(View view)
        {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    if (item.getItemId() == R.id.menu_playlist_song_remove)
                    {
                        AudioTrack track = m_playlist.getUnshuffledTrack(m_position);

                        m_databasePlaylist.removeTrack(m_activity.getContentResolver(), track.ID);
                        m_playlist.removeTrack(m_position);
                        return true;
                    }
                    return false;
                }
            });

            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_playlist_song, popup.getMenu());

            popup.show();
        }

        /**
         * Concrete factory implementation for the PlaylistSongListener. A new
         * instance of the PlaylistSongListener class is returned via the
         * MenuOnClickListener abstract class object.
         */
        public static class Factory implements MenuOnClickListener.Factory
        {
            private Playlist   m_playlist;
            private DBPlaylist m_databasePlaylist;

            public Factory(Playlist playlist, DBPlaylist databasePlaylist)
            {
                m_playlist         = playlist;
                m_databasePlaylist = databasePlaylist;
            }

            @Override
            public MenuOnClickListener create(Activity activity)
            {
                return new PlaylistSongListener(activity, m_playlist, m_databasePlaylist);
            }
        }
    }
}
