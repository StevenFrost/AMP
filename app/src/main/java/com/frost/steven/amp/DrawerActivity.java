package com.frost.steven.amp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    private LruCache<Uri, Bitmap> m_cache;
    private Playlist              m_masterPlaylist;

    MediaService m_service;
    boolean      m_bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // LRU Cache
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 4;
        m_cache = new LruCache<Uri, Bitmap>(cacheSize)
        {
            @Override
            protected int sizeOf(Uri key, Bitmap bitmap)
            {
                return bitmap.getByteCount() / 1024;
            }
        };

        // Master playlist

        // Bind the media service
        Intent playIntent = new Intent(this, MediaService.class);
        bindService(playIntent, m_connection, Context.BIND_AUTO_CREATE);
        startService(playIntent);

        // List of songs
        AudioTrackAdapter adapter = new AudioTrackAdapter(
            this,
            R.layout.tablerow_song,
            m_masterPlaylist.Tracks.toArray(new AudioTrack[m_masterPlaylist.Tracks.size()]),
            m_cache
        );
        ListView lv = (ListView)findViewById(R.id.listView_songs);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View container, int position, long id)
            {
            if (!m_bound)
            {
                return;
            }

            // Update the playlist bound to the service
            m_service.setPlaylist(m_masterPlaylist);

            // Play the selected track if it isn't the track that is already playing
            if (m_service.getCurrentTrack() != m_masterPlaylist.Tracks.get(position))
            {
                m_service.stop();
                m_masterPlaylist.Position = position;
                m_service.play();
            }

            Intent intent = new Intent(DrawerActivity.this, PlayerActivity.class);
            startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private ServiceConnection m_connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MediaService.MediaBinder binder = (MediaService.MediaBinder)service;

            m_service = binder.getService();
            m_bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            m_bound = false;
        }
    };
}
