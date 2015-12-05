package com.frost.steven.amp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

public class PlaylistActivity extends MediaServiceActivity
        implements Playlist.ListCreator.CompletionListener, ListenableArrayList.OnCollectionChangedListener
{
    private Playlist.ListCreator    m_playlistCreator;
    private SongRecyclerViewAdapter m_songViewAdapter;
    private BitmapProvider          m_bitmapProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Action bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bundled DBPlaylist
        Bundle bundle = getIntent().getExtras();
        DBPlaylist playlist = (DBPlaylist)bundle.get(PlaylistsFragment.BUNDLE_PARCEL_PLAYLIST);

        // If we don't have a playlist we need to return
        if (playlist == null)
        {
            finish();
            return;
        }

        // Activity title
        setTitle(playlist.Name);

        // Playlist members, ordered by play order ascending
        Playlist internalPlaylist = new Playlist();
        m_playlistCreator = new Playlist.ListCreator(
            getContentResolver(),
            internalPlaylist,
            null,
            MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC",
            playlist.Id
        );
        m_playlistCreator.addCompletionListener(this);
        m_playlistCreator.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Bitmap Provider
        StaticFragment sf = StaticFragment.getInstance(getSupportFragmentManager(), getContentResolver(), getResources());
        m_bitmapProvider = sf.getBitmapProvider();

        m_songViewAdapter = new SongRecyclerViewAdapter(
            m_playlistCreator,
            this,
            new MenuOnClickListener.PlaylistSongListener.Factory(internalPlaylist, playlist),
            m_bitmapProvider
        );

        RecyclerView view = (RecyclerView)findViewById(R.id.content_playlist_recyclerview);
        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(m_songViewAdapter);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onPlaylistCompleted()
    {
        m_playlistCreator.getPlaylist().attachPlaylistChangedListener(this);
    }

    @Override
    public void onPlaylistCollectionChanged(ListenableArrayList collection)
    {
        m_songViewAdapter.notifyDataSetChanged();
    }
}
