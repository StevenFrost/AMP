package com.frost.steven.amp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class AlbumActivity extends AppCompatActivity
{
    private Album m_album;

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
        return super.onOptionsItemSelected(item);
    }
}
