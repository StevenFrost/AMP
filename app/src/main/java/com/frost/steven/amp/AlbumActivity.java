package com.frost.steven.amp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
