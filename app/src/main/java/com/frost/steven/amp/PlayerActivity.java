package com.frost.steven.amp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity
{
    MediaService m_service;
    boolean      m_bound = false;

    private Handler  m_handler = new Handler();
    private boolean  m_updateTimecodeView = true;
    private Runnable m_updateTimecodeViewRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (m_updateTimecodeView)
            {
                int timecode = m_service.getPlayheadTimecode();

                ((SeekBar) findViewById(R.id.player_seek_bar)).setProgress(timecode);
                ((TextView)findViewById(R.id.player_track_position)).setText(formatTimecode(timecode));

                m_handler.postDelayed(m_updateTimecodeViewRunnable, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Bind the media service
        Intent playIntent = new Intent(this, MediaService.class);
        bindService(playIntent, m_connection, Context.BIND_AUTO_CREATE);
        startService(playIntent);

        // Seekbar listener
        SeekBar seekBar = (SeekBar) findViewById(R.id.player_seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int timecode, boolean fromUser)
            {
                if (fromUser)
                {
                    ((TextView)findViewById(R.id.player_track_position)).setText(formatTimecode(timecode));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                m_service.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                m_service.setPlayheadTimecode(seekBar.getProgress());
                m_service.play();
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    /**
     * Queries the media service to get the current track information. This
     * function is bound to the OnTrackChanged event and is also called upon
     * initial service connection.
     */
    private void refreshVisibleTrackData(AudioTrack track)
    {
        // TODO: Make album art bitmap loading async via BitmapWorkerTask and AsyncDrawable.

        // Album art
        ImageView albumArtView = (ImageView)findViewById(R.id.tablerow_song_albumart);
        ImageView albumArtLargeView = (ImageView)findViewById(R.id.player_large_album_art);
        try
        {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), track.CoverArt);
            Bitmap albumArtSmall = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
            Bitmap albumArtLarge = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
            albumArtView.setImageBitmap(albumArtSmall);
            albumArtLargeView.setImageBitmap(albumArtLarge);
        }
        catch (IOException ex)
        {
            // TODO: Revert to a more suitable album art bitmap here
            ex.printStackTrace();
        }

        // Track title, artist and album
        ((TextView)findViewById(R.id.tablerow_song_title)).setText(track.Title);
        ((TextView)findViewById(R.id.tablerow_song_artist)).setText(track.Artist);
        ((TextView)findViewById(R.id.tablerow_song_album)).setText(track.Album);

        // Static timecode views
        ((TextView)findViewById(R.id.player_track_duration)).setText(formatTimecode(track.Duration));
        ((SeekBar)findViewById(R.id.player_seek_bar)).setMax(track.Duration);

        m_handler.post(m_updateTimecodeViewRunnable);
    }

    private String formatTimecode(long milliseconds)
    {
        return String.format("%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1)
        );
    }

    public void onPrevButtonClick(View view)
    {
        final int PREV_THRESHOLD = 2 * 1000; // 2 second threshold for going to the previous track
        if (m_service.getPlayheadTimecode() > PREV_THRESHOLD)
        {
            m_service.setPlayheadTimecode(0);
        }
        else
        {
            m_service.previousTrack();
        }
    }

    public void onPlayButtonClick(View view)
    {
        Drawable drawable = null;

        switch(m_service.getPlayerState())
        {
        case Playing:
            m_service.pause();
            drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.player_play);
            break;
        case Paused:
        case Stopped:
            m_service.play();
            drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.player_pause);
            break;
        }

        findViewById(R.id.player_play_button).setBackground(drawable);
    }

    public void onNextButtonClick(View view)
    {
        m_service.nextTrack();
    }

    private ServiceConnection m_connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MediaService.MediaBinder binder = (MediaService.MediaBinder)service;

            m_service = binder.getService();
            m_service.setOnTrackChangedListener(new MediaService.OnTrackChangedListener()
            {
                @Override
                public void onTrackChanged(AudioTrack track)
                {
                    refreshVisibleTrackData(track);
                }
            });
            m_service.setOnPlayStateChangedListener(new MediaService.OnPlayStateChangedListener()
            {
                @Override
                public void onStateChanged(MediaService.PlayState oldState, MediaService.PlayState newState)
                {
                    switch(newState)
                    {
                    case Playing:
                        m_updateTimecodeView = true;
                        m_handler.post(m_updateTimecodeViewRunnable);
                        break;
                    case Paused:
                        m_updateTimecodeView = false;
                        break;
                    case Stopped:
                        m_updateTimecodeView = false;
                        break;
                    }
                }
            });
            m_bound = true;

            refreshVisibleTrackData(m_service.getCurrentTrack());
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            m_bound = false;
        }
    };
}
