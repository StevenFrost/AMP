package com.frost.steven.amp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SongsFragment extends Fragment
{
    private static final String FRAGMENT_ID = "com.frost.steven.amp.SongsFragment";

    private LruCache<Uri, Bitmap> m_cache;

    private RecyclerViewAdapter m_recyclerViewAdapter = null;
    private PlaylistCreator     m_playlistCreatorTask = null;
    private Playlist            m_masterPlaylist;


    public static SongsFragment getInstance()
    {
        return new SongsFragment();
    }

    public SongsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_masterPlaylist = new Playlist();
        m_recyclerViewAdapter = new RecyclerViewAdapter(getActivity(), m_masterPlaylist);

        m_playlistCreatorTask = new PlaylistCreator(
            getActivity().getContentResolver(),
            m_masterPlaylist,
            null,
            MediaStore.Audio.Media.TITLE
        );
        m_playlistCreatorTask.setOnTrackInsertedListener(m_recyclerViewAdapter);
        m_playlistCreatorTask.setOnPlaylistCompleteListener(m_recyclerViewAdapter);
        m_playlistCreatorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        RecyclerView view = (RecyclerView)inflater.inflate(R.layout.fragment_songs, container, false);

        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(m_recyclerViewAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    public void setCache(LruCache<Uri, Bitmap> cache)
    {
        m_cache = cache;
    }

    class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
            implements PlaylistCreator.OnTrackInsertedListener, PlaylistCreator.OnPlaylistCompleteListener
    {
        private Context  m_context;
        private Playlist m_playlist;

        private int m_tracksInserted = 0;

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

            if (m_tracksInserted % 10 == 0)
            {
                notifyDataSetChanged();
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            public final View      m_view;
            public final ImageView m_albumArt;
            public final TextView  m_title;
            public final TextView  m_artist;
            public final TextView  m_album;

            public ViewHolder(View view)
            {
                super(view);

                m_view     = view;
                m_albumArt = (ImageView)view.findViewById(R.id.tablerow_song_albumart);
                m_title    = (TextView) view.findViewById(R.id.tablerow_song_title);
                m_artist   = (TextView) view.findViewById(R.id.tablerow_song_artist);
                m_album    = (TextView) view.findViewById(R.id.tablerow_song_album);
            }
        }

        public AudioTrack getValueAt(int position)
        {
            return m_playlist.Tracks.get(position);
        }

        public RecyclerViewAdapter(Context context, Playlist playlist)
        {
            m_context = context;
            m_playlist = playlist;
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
            AudioTrack track = getValueAt(position);

            holder.m_title.setText(track.Title);
            holder.m_artist.setText(track.Artist);
            holder.m_album.setText(track.Album);

            holder.m_view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    LibraryActivity activity = (LibraryActivity)getActivity();
                    if (!activity.isServiceBound())
                    {
                        return;
                    }

                    MediaService mediaService = activity.getMediaService();

                    // Update the playlist bound to the service
                    mediaService.setPlaylist(m_masterPlaylist);

                    // Play the selected track if it isn't the track that is already playing
                    if (mediaService.getCurrentTrack() != m_masterPlaylist.Tracks.get(position))
                    {
                        mediaService.stop();
                        m_masterPlaylist.Position = position;
                        mediaService.play();
                    }

                    Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    startActivity(intent);
                }
            });

            if (track.CoverArt != null && BitmapWorkerTask.cancelOutstandingWork(track, holder.m_albumArt))
            {
                Bitmap albumArtBitmap = m_cache.get(track.CoverArt);
                if (albumArtBitmap != null)
                {
                    holder.m_albumArt.setImageBitmap(albumArtBitmap);
                }
                else
                {
                    final BitmapWorkerTask worker = new BitmapWorkerTask(m_context.getContentResolver(), holder.m_albumArt, m_cache);
                    final AsyncDrawable asyncDrawable = new AsyncDrawable(getResources(), null, worker);
                    holder.m_albumArt.setImageDrawable(asyncDrawable);
                    worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
                }
            }
            else if (track.CoverArt == null)
            {
                holder.m_albumArt.setImageBitmap(null);
            }
        }

        @Override
        public int getItemCount()
        {
            if (m_playlist != null && m_playlist.Tracks != null)
            {
                return m_playlist.Tracks.size();
            }
            return 0;
        }
    }
}
