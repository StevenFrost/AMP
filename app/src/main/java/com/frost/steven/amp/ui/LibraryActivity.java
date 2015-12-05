package com.frost.steven.amp.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.frost.steven.amp.helpers.BitmapResolver;
import com.frost.steven.amp.model.DBPlaylist;
import com.frost.steven.amp.helpers.DBPlaylistManager;
import com.frost.steven.amp.R;
import com.frost.steven.amp.model.Playlist;
import com.frost.steven.amp.service.MediaService;

/**
 * This is the main activity that the user lands on when the launch the
 * application. It holds three tabs; Albums, Playlists and Songs. Each tab
 * is a fragment that accesses shared state such as the bitmap cache and
 * playlist manager.
 */
public class LibraryActivity extends MediaServiceActivity implements DBPlaylistManager.Container
{
    private BitmapResolver m_bitmapResolver;
    private DBPlaylistManager    m_playlistManager;
    private Playlist.ListCreator m_masterPlaylistTask;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        setTitle(R.string.title_activity_library);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Page adapter
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager)findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);

        initActivityState();

        // Media service
        Intent intent = new Intent(this, MediaService.class);
        startService(intent);

        // Tabs
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // TODO: Attach a content observer to listen for MediaStore changes.
        //       This will require changes to many components to deal with all the edge cases.
        //       There may be some noise around this as it appears the observer is notified when
        //       tracks are simply accessed.
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Playlist Manager
        m_playlistManager = new DBPlaylistManager(getContentResolver());

        // MediaStore Playlists
        DBPlaylist.ListCreator playlistsTask = new DBPlaylist.ListCreator(getContentResolver(), m_playlistManager.getPlaylists());
        playlistsTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
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
        int itemId = item.getItemId();
        if (itemId == R.id.menu_library_player)
        {
            MediaService mediaService = getMediaService();
            if (mediaService.getPlayerState() == MediaService.PlayerState.Stopped)
            {
                mediaService.setPlaylist(m_masterPlaylistTask.getPlaylist());
                getMediaService().play();
            }
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true);
    }

    @Override
    public DBPlaylistManager getDBPlaylistManager()
    {
        return m_playlistManager;
    }

    public BitmapResolver getBitmapProvider()
    {
        return m_bitmapResolver;
    }

    public Playlist.ListCreator getMasterPlaylistTask()
    {
        return m_masterPlaylistTask;
    }

    private void initActivityState()
    {
        // Master playlist
        Playlist masterPlaylist = new Playlist();
        m_masterPlaylistTask = new Playlist.ListCreator(
            getContentResolver(),
            masterPlaylist,
            null,
            MediaStore.Audio.Media.TITLE
        );
        m_masterPlaylistTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Bitmap Provider
        StaticFragment sf = StaticFragment.getInstance(getSupportFragmentManager(), getContentResolver(), getResources());
        m_bitmapResolver = sf.getBitmapProvider();
    }

    /**
     * A FragmentPagerAdapter that returns a fragment corresponding to one of
     * the sections/tabs/pages. FragmentStatePagerAdapter may be better if the
     * memory footprint of this is too large.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private static final int NUM_PAGES = 3;

        public SectionsPagerAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
            case 0:
                return AlbumsFragment.getInstance();
            case 1:
                return PlaylistsFragment.getInstance();
            case 2:
                return SongsFragment.getInstance();
            }
            return null;
        }

        @Override
        public int getCount()
        {
            return NUM_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            Resources resources = getResources();
            switch (position)
            {
            case 0:
                return resources.getString(R.string.tab_albums);
            case 1:
                return resources.getString(R.string.tab_playlists);
            case 2:
                return resources.getString(R.string.tab_songs);
            }
            return null;
        }
    }
}
