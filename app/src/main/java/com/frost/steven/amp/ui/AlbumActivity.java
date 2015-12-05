package com.frost.steven.amp.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.frost.steven.amp.helpers.BitmapResolver;
import com.frost.steven.amp.model.DBPlaylist;
import com.frost.steven.amp.helpers.DBPlaylistManager;
import com.frost.steven.amp.ui.listeners.MenuOnClickListener;
import com.frost.steven.amp.R;
import com.frost.steven.amp.ui.adapters.SongRecyclerViewAdapter;
import com.frost.steven.amp.model.Album;
import com.frost.steven.amp.model.Playlist;
import com.frost.steven.amp.service.MediaService;

public class AlbumActivity extends MediaServiceActivity implements DBPlaylistManager.Container
{
    private Album m_album;

    private DBPlaylistManager    m_playlistManager;
    private Playlist.ListCreator m_playlistTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bundle album
        Bundle bundle = getIntent().getExtras();
        m_album = (Album)bundle.get(AlbumsFragment.BUNDLE_PARCEL_ALBUM);

        if (m_album == null)
        {
            finish();
            return;
        }
        setTitle(m_album.Title);

        initActivityState();

        // Inflate the recycler view
        SongViewAdapter songRecyclerViewAdapter = new SongViewAdapter(
            m_playlistTask,
            this,
            new MenuOnClickListener.SongListener.Factory(m_playlistManager)
        );
        RecyclerView view = (RecyclerView)findViewById(R.id.content_album_recyclerview);
        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(songRecyclerViewAdapter);

        // Bitmap Provider
        StaticFragment sf = StaticFragment.getInstance(getSupportFragmentManager(), getContentResolver(), getResources());
        BitmapResolver bitmapResolver = sf.getBitmapProvider();

        bitmapResolver.makeRequest((ImageView)findViewById(R.id.activity_album_artwork), m_album.Artwork, 500);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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
        else if (itemID == R.id.menu_library_player)
        {
            MediaService mediaService = getMediaService();
            if (mediaService.getPlayerState() == MediaService.PlayerState.Stopped)
            {
                mediaService.setPlaylist(m_playlistTask.getPlaylist());
                getMediaService().play();
            }
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public DBPlaylistManager getDBPlaylistManager()
    {
        return m_playlistManager;
    }

    private void initActivityState()
    {
        // Playlist Manager
        m_playlistManager = new DBPlaylistManager(getContentResolver());

        // MediaStore Playlists
        DBPlaylist.ListCreator playlistsTask = new DBPlaylist.ListCreator(getContentResolver(), m_playlistManager.getPlaylists());
        playlistsTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        // Album playlist
        Playlist playlist = new Playlist();
        m_playlistTask = new Playlist.ListCreator(
            getContentResolver(),
            playlist,
            new String[] { MediaStore.Audio.Media.ALBUM_ID + " == " + m_album.AlbumID },
            MediaStore.Audio.Media.TRACK + " ASC"
        );
        m_playlistTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * This class derives from the song recycler view adapter to adjust the
     * visibility of the album art for this specific use case. All other
     * behaviour remains the same.
     */
    private class SongViewAdapter extends SongRecyclerViewAdapter
    {
        public SongViewAdapter(Playlist.ListCreator playlistCreatorTask,
                               MediaServiceActivity activity,
                               @Nullable MenuOnClickListener.Factory menuFactory)
        {
            super(playlistCreatorTask, activity, menuFactory, null);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position)
        {
            holder.m_albumArt.setVisibility(View.GONE);
            super.onBindViewHolder(holder, position);
        }
    }
}
