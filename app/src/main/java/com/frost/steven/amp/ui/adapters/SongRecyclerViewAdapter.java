package com.frost.steven.amp.ui.adapters;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.frost.steven.amp.helpers.BitmapResolver;
import com.frost.steven.amp.ui.listeners.MenuOnClickListener;
import com.frost.steven.amp.R;
import com.frost.steven.amp.model.AudioTrack;
import com.frost.steven.amp.model.Playlist;
import com.frost.steven.amp.service.MediaService;
import com.frost.steven.amp.ui.MediaServiceActivity;
import com.frost.steven.amp.ui.PlayerActivity;

/**
 * Recycler view adapter for lists of audio tracks. Handles the presentation of
 * a playlist with some customisation options such as being able to disable
 * album art.
 */
public class SongRecyclerViewAdapter
    extends RecyclerView.Adapter<SongRecyclerViewAdapter.ViewHolder>
    implements Playlist.ListCreator.CompletionListener
{
    private MediaServiceActivity        m_activity;
    private BitmapResolver              m_bitmapResolver;
    private MenuOnClickListener.Factory m_menuFactory;

    private Playlist             m_playlist;
    private Playlist.ListCreator m_playlistCreatorTask;

    /**
     * Constructor
     *
     * @param playlistCreatorTask   Async task representing the playlist builder. The view adapter
     *                              will attach to this to listen for completion in order to update
     *                              the view appropriately.
     * @param activity              The parent activity
     * @param bitmapResolver        The async bitmap provider
     */
    public SongRecyclerViewAdapter(Playlist.ListCreator playlistCreatorTask,
                                   MediaServiceActivity activity,
                                   @Nullable MenuOnClickListener.Factory menuFactory,
                                   @Nullable BitmapResolver bitmapResolver)
    {
        m_activity       = activity;
        m_bitmapResolver = bitmapResolver;
        m_menuFactory    = menuFactory;

        m_playlistCreatorTask = playlistCreatorTask;
        m_playlist = m_playlistCreatorTask.getPlaylist();
        m_playlistCreatorTask.addCompletionListener(this);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.element_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position)
    {
        final AudioTrack track = m_playlist.getUnshuffledTrack(position);

        // Title, artist, album and duration text fields
        holder.m_title.setText(track.Title);
        holder.m_artist.setText(track.Artist);
        holder.m_album.setText(track.Album);
        holder.m_duration.setText(track.getFormattedDuration());

        // Album art
        if (m_bitmapResolver == null ||
            holder.m_albumArt.getVisibility() == View.GONE)
        {
            holder.m_albumArt.setImageBitmap(null);
        }
        else
        {
            m_bitmapResolver.makeRequest(holder.m_albumArt, track.CoverArt, 100);
        }

        holder.m_view.setOnClickListener(new SongClickListener(position));

        // Optional popup menu click listener
        if (m_menuFactory != null)
        {
            MenuOnClickListener listener = m_menuFactory.create(m_activity);
            listener.setPlaylist(m_playlist);
            listener.setPosition(position);

            holder.m_menu.setOnClickListener(listener);
        }
    }

    @Override
    public int getItemCount()
    {
        return m_playlist.getNumTracks();
    }

    @Override
    public void onPlaylistCompleted()
    {
        m_playlistCreatorTask = null;
        notifyDataSetChanged();
    }

    /**
     * POD structure holding view objects contained in a single audio
     * track row.
     */
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

    /**
     * On click listener implementation for a single audio track list item. The
     * position of the element is bound in an instance of this class.
     */
    private class SongClickListener implements View.OnClickListener
    {
        private final int m_position;

        public SongClickListener(int position)
        {
            m_position = position;
        }

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
            m_playlist.setCursor(m_position);
            mediaService.setPlaylist(m_playlist);

            // Play the selected track if it isn't the track that is already playing
            if (currentTrack != m_playlist.getUnshuffledTrack(m_position))
            {
                mediaService.stop();
                mediaService.play();
            }

            // Start the player activity
            Intent intent = new Intent(m_activity, PlayerActivity.class);
            m_activity.startActivity(intent);
        }
    }
}
