package com.frost.steven.amp;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class LibraryActivity extends MediaServiceActivity
        implements DBPlaylist.ListCreator.OnUnresolvedPlaylistsCompletedListener
{
    private BitmapProvider                  m_bitmapProvider;
    private ListenableArrayList<DBPlaylist> m_playlists;
    private DBPlaylist.ListCreator          m_playlistsTask;

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

        // Playlists
        m_playlists = new ListenableArrayList<>();
        m_playlistsTask = new DBPlaylist.ListCreator(getContentResolver(), m_playlists);
        m_playlistsTask.addOnUnresolvedPlaylistsCompletedListener(this);
        m_playlistsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Bitmap Provider
        m_bitmapProvider = new BitmapProvider(getResources(), getContentResolver());

        // Media service
        Intent intent = new Intent(this, MediaService.class);
        startService(intent);

        // Tabs
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    protected void onDestroy()
    {
        Intent intent = new Intent(this, MediaService.class);
        stopService(intent);

        if (m_playlistsTask != null)
        {
            m_playlistsTask.cancel(true);
        }
        super.onDestroy();
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
    public void onUnresolvedPlaylistsCompleted()
    {
        m_playlistsTask = null;
    }

    public BitmapProvider getBitmapProvider()
    {
        return m_bitmapProvider;
    }

    public ListenableArrayList<DBPlaylist> getPlaylists()
    {
        return m_playlists;
    }

    /**
     * A FragmentPagerAdapter that returns a fragment corresponding to one of
     * the sections/tabs/pages. FragmentStatePagerAdapter may be better if the
     * memory footprint of this is too large.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        private static final int NUM_PAGES = 4;

        public SectionsPagerAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
        }

        /**
         * Called to instantiate the fragment for the given page.
         *
         * @param position section/tab/page index
         * @return a new page fragment
         */
        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
            case 0:
                return AlbumsFragment.getInstance();
            case 1:
                return ArtistsFragment.getInstance(getSupportFragmentManager());
            case 2:
                return PlaylistsFragment.getInstance();
            case 3:
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
            // TODO: Use string resources instead
            switch (position)
            {
            case 0:
                return "Albums";
            case 1:
                return "Artists";
            case 2:
                return "Playlists";
            case 3:
                return "Songs";
            }
            return null;
        }
    }
}
