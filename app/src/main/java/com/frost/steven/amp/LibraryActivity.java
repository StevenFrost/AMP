package com.frost.steven.amp;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class LibraryActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager)findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_library, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                return AlbumsFragment.getInstance(getSupportFragmentManager());
            case 1:
                return ArtistsFragment.getInstance(getSupportFragmentManager());
            case 2:
                return PlaylistsFragment.getInstance(getSupportFragmentManager());
            case 3:
                return SongsFragment.getInstance(getSupportFragmentManager());
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
