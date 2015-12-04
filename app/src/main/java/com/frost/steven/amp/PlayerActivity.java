package com.frost.steven.amp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayerActivity extends AppCompatActivity
{
    private MediaService m_service;
    private boolean      m_bound = false;

    private boolean      m_shuffle = false;
    private boolean      m_repeat = false;

    private Handler      m_handler = new Handler();
    private boolean      m_updateTimecodeView = true;
    private Runnable     m_updateTimecodeViewRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (m_updateTimecodeView)
            {
                updateTrackPositionInterface();
                m_handler.postDelayed(m_updateTimecodeViewRunnable, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Seekbar listener
        SeekBar seekBar = (SeekBar) findViewById(R.id.player_seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            private MediaService.PlayerState m_stateOnStartTouch = MediaService.PlayerState.Stopped;

            @Override
            public void onProgressChanged(SeekBar seekBar, int timecode, boolean fromUser)
            {
                if (fromUser)
                {
                    ((TextView) findViewById(R.id.player_track_position)).setText(AudioTrack.formatDuration(timecode));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                m_stateOnStartTouch = m_service.getPlayerState();
                m_service.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                m_service.setPlayheadTimecode(seekBar.getProgress());
                if (m_stateOnStartTouch == MediaService.PlayerState.Playing)
                {
                    m_service.play();
                }
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        Intent playIntent = new Intent(this, MediaService.class);
        bindService(playIntent, m_connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        m_updateTimecodeView = false;
        if (m_bound)
        {
            m_bound = false;
            unbindService(m_connection);
        }

        super.onStop();
    }

    /**
     * Queries the media service to get the current track information. This
     * function is bound to the OnTrackChanged event and is also called upon
     * initial service connection.
     */
    private void refreshVisibleTrackData(AudioTrack track)
    {
        // TODO: Make album art bitmap loading async

        // Album art
        ImageView albumArtView                 = (ImageView)findViewById(R.id.element_song_artwork);
        ImageView albumArtLargePlaceholderView = (ImageView)findViewById(R.id.player_large_albumart_placeholder);
        ImageView albumArtLargeView            = (ImageView)findViewById(R.id.player_large_albumart);

        if (track.CoverArt != null)
        {
            try
            {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), track.CoverArt);
                Bitmap albumArtSmall = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
                Bitmap albumArtLarge = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                albumArtView.setImageBitmap(albumArtSmall);
                albumArtLargeView.setImageBitmap(albumArtLarge);

                albumArtLargePlaceholderView.setVisibility(View.GONE);
                albumArtLargeView.setVisibility(View.VISIBLE);
            }
            catch (Exception ex)
            {
                // TODO: Revert to a more suitable album art bitmap here
                ex.printStackTrace();
            }
        }
        else
        {
            albumArtView.setImageBitmap(null);
            albumArtView.setImageBitmap(null);

            albumArtLargePlaceholderView.setVisibility(View.VISIBLE);
            albumArtLargeView.setVisibility(View.GONE);
        }

        // Track title, artist and album
        ((TextView)findViewById(R.id.element_song_title)).setText(track.Title);
        ((TextView)findViewById(R.id.element_song_artist)).setText(track.Artist);
        ((TextView)findViewById(R.id.element_song_album)).setText(track.Album);
        (findViewById(R.id.element_song_duration)).setVisibility(View.GONE);
        (findViewById(R.id.element_song_menu)).setVisibility(View.GONE);

        // Static timecode views
        ((TextView)findViewById(R.id.player_track_duration)).setText(track.getFormattedDuration());
        ((SeekBar)findViewById(R.id.player_seek_bar)).setMax(track.Duration);

        // Play/Pause button
        updatePlayButtonImage();

        m_handler.post(m_updateTimecodeViewRunnable);
    }

    public void onPrevButtonClick(View view)
    {
        final int PREV_THRESHOLD = 2 * 1000; // 2 second threshold for going to the previous track
        if (m_service.getPlayheadTimecode() > PREV_THRESHOLD)
        {
            m_service.setPlayheadTimecode(0);
            updateTrackPositionInterface();
        }
        else
        {
            m_service.previousTrack();
        }
    }

    public void onPlayButtonClick(View view)
    {
        ImageButton ib = ((ImageButton)findViewById(R.id.player_play_button));

        switch(m_service.getPlayerState())
        {
        case Playing:
            m_service.pause();
            break;
        case Paused:
        case Stopped:
            m_service.play();
            break;
        }

        updatePlayButtonImage();
    }

    public void onNextButtonClick(View view)
    {
        m_service.nextTrack();
    }

    public void onShuffleButtonClick(View view)
    {
        if (!m_shuffle)
        {
            ((ImageButton) view).setImageResource(R.drawable.player_shuffle_selected);
        }
        else
        {
            ((ImageButton) view).setImageResource(R.drawable.player_shuffle);
        }
        m_shuffle = !m_shuffle;
        m_service.setShuffle(m_shuffle);
    }

    public void onRepeatButtonClick(View view)
    {
        if (!m_repeat)
        {
            ((ImageButton) view).setImageResource(R.drawable.player_repeat_selected);
        }
        else
        {
            ((ImageButton) view).setImageResource(R.drawable.player_repeat);
        }
        m_repeat = !m_repeat;
        m_service.setRepeat(m_repeat);
    }

    private void updatePlayButtonImage()
    {
        ImageButton ib = ((ImageButton)findViewById(R.id.player_play_button));

        switch(m_service.getPlayerState())
        {
        case Playing:
            ib.setImageResource(R.drawable.player_pause);
            break;
        case Paused:
        case Stopped:
            ib.setImageResource(R.drawable.player_play);
            break;
        }
    }

    private void updateTrackPositionInterface()
    {
        int timecode = m_service.getPlayheadTimecode();

        ((SeekBar) findViewById(R.id.player_seek_bar)).setProgress(timecode);
        ((TextView) findViewById(R.id.player_track_position)).setText(AudioTrack.formatDuration(timecode));
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
                public void onStateChanged(MediaService.PlayerState oldState, MediaService.PlayerState newState)
                {
                    switch(newState)
                    {
                    case Playing:
                        m_updateTimecodeView = true;
                        ((ImageButton)findViewById(R.id.player_play_button)).setImageResource(R.drawable.player_pause);
                        m_handler.post(m_updateTimecodeViewRunnable);
                        break;
                    case Paused:
                        m_updateTimecodeView = false;
                        ((ImageButton)findViewById(R.id.player_play_button)).setImageResource(R.drawable.player_play);
                        break;
                    case Stopped:
                        m_updateTimecodeView = false;
                        ((ImageButton)findViewById(R.id.player_play_button)).setImageResource(R.drawable.player_play);
                        break;
                    }
                    updateTrackPositionInterface();
                }
            });
            m_bound = true;

            refreshVisibleTrackData(m_service.getCurrentTrack());
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            m_bound = false;
            m_updateTimecodeView = false;
        }
    };
}
