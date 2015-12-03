package com.frost.steven.amp;

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

/**
 * This is the main activity that the user lands on when the launch the
 * application. It holds three tabs; Albums, Playlists and Songs. Each tab
 * is a fragment that accesses shared state such as the bitmap cache and
 * playlist manager.
 */
public class LibraryActivity extends MediaServiceActivity implements DBPlaylistManager.Container
{
    private BitmapProvider       m_bitmapProvider;
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

    public BitmapProvider getBitmapProvider()
    {
        return m_bitmapProvider;
    }

    public Playlist.ListCreator getMasterPlaylistTask()
    {
        return m_masterPlaylistTask;
    }

    private void initActivityState()
    {
        // Playlist Manager
        m_playlistManager = new DBPlaylistManager(getContentResolver());

        // MediaStore Playlists
        DBPlaylist.ListCreator playlistsTask = new DBPlaylist.ListCreator(getContentResolver(), m_playlistManager.getPlaylists());
        playlistsTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

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
        m_bitmapProvider = new BitmapProvider(getResources(), getContentResolver());
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
