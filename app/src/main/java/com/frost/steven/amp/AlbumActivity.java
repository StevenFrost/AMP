package com.frost.steven.amp;

import android.content.Intent;
import android.graphics.Bitmap;
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

public class AlbumActivity extends MediaServiceActivity
{
    private Album m_album;

    private SongViewAdapter m_songRecyclerViewAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle bundle = getIntent().getExtras();
        m_album = (Album)bundle.get(AlbumsFragment.BUNDLE_PARCEL_ALBUM);

        if (m_album != null)
        {
            setTitle(m_album.Title);
        }

        Playlist paylist = new Playlist();
        PlaylistCreator playlistCreator = new PlaylistCreator(
            getContentResolver(),
            paylist,
            new String[] { MediaStore.Audio.Media.ALBUM_ID + " == " + m_album.AlbumID.toString() },
            MediaStore.Audio.Media.TRACK + " ASC"
        );
        m_songRecyclerViewAdapter = new SongViewAdapter(this, null, playlistCreator);

        RecyclerView view = (RecyclerView)findViewById(R.id.content_album_recyclerview);
        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(m_songRecyclerViewAdapter);

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
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private class SongViewAdapter extends SongRecyclerViewAdapter
    {
        public SongViewAdapter(MediaServiceActivity activity, @Nullable BitmapProvider bitmapProvider, PlaylistCreator playlistCreatorTask)
        {
            super(activity, bitmapProvider, playlistCreatorTask);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position)
        {
            holder.m_albumArt.setVisibility(View.GONE);
            super.onBindViewHolder(holder, position);
        }
    }
}
