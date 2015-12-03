package com.frost.steven.amp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.List;

public class PlaylistActivity extends MediaServiceActivity
{
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
        Playlist.ListCreator playlistCreator = new Playlist.ListCreator(
            getContentResolver(),
            internalPlaylist,
            null,
            MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC",
            playlist.Id
        );
        playlistCreator.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        SongViewAdapter songRecyclerViewAdapter = new SongViewAdapter(this, null, null, playlistCreator);
        RecyclerView view = (RecyclerView)findViewById(R.id.content_playlist_recyclerview);
        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(songRecyclerViewAdapter);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private class SongViewAdapter extends SongRecyclerViewAdapter
    {
        public SongViewAdapter(MediaServiceActivity activity, @Nullable BitmapProvider bitmapProvider, @Nullable List<DBPlaylist> playlists, Playlist.ListCreator playlistCreatorTask)
        {
            super(activity, bitmapProvider, playlists, playlistCreatorTask);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position)
        {
            holder.m_albumArt.setVisibility(View.GONE);
            holder.m_menu.setVisibility(View.GONE);
            super.onBindViewHolder(holder, position);
        }
    }
}
