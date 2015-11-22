package com.frost.steven.amp;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SongRecyclerViewAdapter
    extends RecyclerView.Adapter<SongRecyclerViewAdapter.ViewHolder>
    implements Playlist.ListCreator.OnTrackInsertedListener, Playlist.ListCreator.OnPlaylistCompleteListener
{
    private MediaServiceActivity     m_activity;
    private BitmapProvider           m_bitmapProvider;
    private List<DBPlaylist> m_playlists;
    private Playlist                 m_playlist;
    private Playlist.ListCreator     m_playlistCreatorTask;

    private int m_tracksInserted = 0;

    public SongRecyclerViewAdapter(
            MediaServiceActivity activity,
            @Nullable BitmapProvider bitmapProvider,
            List<DBPlaylist> playlists,
            Playlist.ListCreator playlistCreatorTask)
    {
        m_activity = activity;
        m_bitmapProvider = bitmapProvider;
        m_playlists = playlists;

        m_playlistCreatorTask = playlistCreatorTask;
        m_playlist = m_playlistCreatorTask.getPlaylist();
        m_playlistCreatorTask.setOnTrackInsertedListener(this);
        m_playlistCreatorTask.setOnPlaylistCompleteListener(this);
        m_playlistCreatorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.element_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onPlaylistComplete()
    {
        m_playlistCreatorTask = null;
        notifyDataSetChanged();
    }

    @Override
    public void onTrackInserted(AudioTrack track)
    {
        ++m_tracksInserted;

        // TODO: Adjust this for the current number of visible elements on screen
        if (m_tracksInserted % 50 == 0)
        {
            notifyDataSetChanged();
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        AudioTrack track = m_playlist.getUnshuffledTrack(position);

        // Title, artist, album and duration text fields
        holder.m_title.setText(track.Title);
        holder.m_artist.setText(track.Artist);
        holder.m_album.setText(track.Album);
        holder.m_duration.setText(track.getFormattedDuration());

        // Album art
        if (track.CoverArt == null || m_bitmapProvider == null || holder.m_albumArt.getVisibility() == View.GONE)
        {
            holder.m_albumArt.setImageBitmap(null);
        }
        else
        {
            m_bitmapProvider.makeRequest(holder.m_albumArt, track.CoverArt);
        }

        // Song click listener
        holder.m_view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!m_activity.isMediaServiceBound())
                {
                    return;
                }

                // Get the current track
                MediaService mediaService = m_activity.getMediaService();
                AudioTrack currentTrack = mediaService.getCurrentTrack();

                // Update the playlist bound to the service
                mediaService.setPlaylist(m_playlist);
                m_playlist.setCursor(position);

                // Play the selected track if it isn't the track that is already playing
                if (currentTrack != m_playlist.getUnshuffledTrack(position))
                {
                    mediaService.stop();
                    mediaService.play();
                }

                // Start the player activity
                Intent intent = new Intent(m_activity, PlayerActivity.class);
                m_activity.startActivity(intent);
            }
        });

        // Song menu click listener
        holder.m_menu.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    @Override
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        if (item.getItemId() == R.id.menu_song_add_to_playlist)
                        {
                            return true;
                        }

                        if (item.getItemId() == R.id.menu_song_new_playlist)
                        {
                            DialogFragment df = new NewPlaylistFragment();
                            df.show(m_activity.getFragmentManager(), "dialog-new-playlist");
                        }
                        return true;
                    }
                });
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_song, popup.getMenu());

                MenuItem playlistsMenuItem = popup.getMenu().getItem(0);
                Menu playlistsSubMenu = playlistsMenuItem.getSubMenu();

                for (int i = 0; i < m_playlists.size(); ++i)
                {
                    DBPlaylist playlist = m_playlists.get(i);
                    playlistsSubMenu.add(Menu.NONE, i, Menu.NONE, playlist.Name);
                }

                popup.show();
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return m_tracksInserted;
    }

    protected class ViewHolder extends RecyclerView.ViewHolder
    {
        public final View        m_view;
        public final ImageView   m_albumArt;
        public final TextView    m_title;
        public final TextView    m_artist;
        public final TextView    m_album;
        public final TextView    m_duration;
        public final ImageButton m_menu;

        public ViewHolder(View view)
        {
            super(view);

            m_view     = view;
            m_albumArt = (ImageView)view.findViewById(R.id.element_song_artwork);
            m_title    = (TextView)view.findViewById(R.id.element_song_title);
            m_artist   = (TextView)view.findViewById(R.id.element_song_artist);
            m_album    = (TextView)view.findViewById(R.id.element_song_album);
            m_duration = (TextView)view.findViewById(R.id.element_song_duration);
            m_menu     = (ImageButton)view.findViewById(R.id.element_song_menu);
        }
    }
}
