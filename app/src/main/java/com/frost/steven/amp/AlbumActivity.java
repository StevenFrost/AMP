package com.frost.steven.amp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumActivity extends AppCompatActivity
{
    private MediaService m_mediaService;
    private boolean      m_serviceBound = false;

    private Album m_album;

    private RecyclerViewAdapter m_recyclerViewAdapter = null;
    private PlaylistCreator     m_playlistCreatorTask = null;
    private Playlist            m_playlist;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle bundle = getIntent().getExtras();
        m_album = (Album)bundle.get(AlbumsFragment.BUNDLE_PARCEL_ALBUM);

        setTitle(m_album.Title);

        m_playlist = new Playlist();
        m_recyclerViewAdapter = new RecyclerViewAdapter(this, m_playlist);

        RecyclerView view = (RecyclerView)findViewById(R.id.content_album_recyclerview);
        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(m_recyclerViewAdapter);

        m_playlistCreatorTask = new PlaylistCreator(
            getContentResolver(),
            m_playlist,
            new String[] { MediaStore.Audio.Media.ALBUM_ID + " == " + m_album.AlbumID.toString() },
            MediaStore.Audio.Media.TRACK + " ASC"
        );
        m_playlistCreatorTask.setOnTrackInsertedListener(m_recyclerViewAdapter);
        m_playlistCreatorTask.setOnPlaylistCompleteListener(m_recyclerViewAdapter);
        m_playlistCreatorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (m_album.Artwork != null)
        {
            try
            {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), m_album.Artwork);
                Bitmap albumArt = Bitmap.createScaledBitmap(bitmap, 512, 512, true);

                ((ImageView)findViewById(R.id.activity_album_artwork)).setImageBitmap(albumArt);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();

        Intent playIntent = new Intent(this, MediaService.class);
        bindService(playIntent, m_connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        if (m_serviceBound)
        {
            m_serviceBound = false;
            unbindService(m_connection);
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home)
        {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
            implements PlaylistCreator.OnTrackInsertedListener, PlaylistCreator.OnPlaylistCompleteListener
    {
        private Context m_context;
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

            // TODO: Adjust this for the current number of visible elements on screen
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
            public final TextView  m_duration;

            public ViewHolder(View view)
            {
                super(view);

                m_view     = view;
                m_albumArt = (ImageView)view.findViewById(R.id.element_song_artwork);
                m_title    = (TextView)view.findViewById(R.id.element_song_title);
                m_artist   = (TextView)view.findViewById(R.id.element_song_artist);
                m_album    = (TextView)view.findViewById(R.id.element_song_album);
                m_duration = (TextView)view.findViewById(R.id.element_song_duration);
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
            holder.m_album.setVisibility(View.GONE);
            holder.m_albumArt.setVisibility(View.GONE);
            holder.m_duration.setText(track.getFormattedDuration());

            holder.m_view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (!m_serviceBound)
                    {
                        return;
                    }

                    AudioTrack currentTrack = m_mediaService.getCurrentTrack();

                    // Update the playlist bound to the service
                    m_mediaService.setPlaylist(m_playlist);
                    m_playlist.Position = position;

                    // Play the selected track if it isn't the track that is already playing
                    if (currentTrack != m_playlist.Tracks.get(position))
                    {
                        m_mediaService.stop();
                        m_mediaService.play();
                    }

                    Intent intent = new Intent(holder.m_view.getContext(), PlayerActivity.class);
                    startActivity(intent);
                }
            });

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

    private ServiceConnection m_connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MediaService.MediaBinder binder = (MediaService.MediaBinder)service;

            m_mediaService = binder.getService();
            m_serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            m_serviceBound = false;
        }
    };
}
