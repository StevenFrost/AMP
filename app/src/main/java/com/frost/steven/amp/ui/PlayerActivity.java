package com.frost.steven.amp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.frost.steven.amp.helpers.BitmapResolver;
import com.frost.steven.amp.R;
import com.frost.steven.amp.model.AudioTrack;
import com.frost.steven.amp.service.MediaService;

/**
 * Activity representing the player. The player presents transport controls and
 * current song information to the user, allowing them to scrub through the
 * track, pause, play, move forward and backwards and enable/disable repeat or
 * shuffle functionality.
 */
public class PlayerActivity extends MediaServiceActivity
{
    private BitmapResolver m_bitmapResolver;

    private boolean m_shuffle = false;
    private boolean m_repeat = false;

    private Handler  m_handler = new Handler();
    private boolean  m_updateTimecodeView = true;
    private Runnable m_updateTimecodeViewRunnable = new Runnable()
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
        seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());

        // Bitmap Provider
        StaticFragment sf = StaticFragment.getInstance(getSupportFragmentManager(), getContentResolver(), getResources());
        m_bitmapResolver = sf.getBitmapProvider();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        Intent playIntent = new Intent(this, MediaService.class);
        bindService(playIntent, getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        m_updateTimecodeView = false;
        super.onStop();
    }

    @Override
    protected void onMediaServiceConnected()
    {
        final MediaService mediaService = getMediaService();

        // Track Change Listener
        mediaService.setOnTrackChangedListener(new MediaService.OnTrackChangedListener()
        {
            @Override
            public void onTrackChanged(AudioTrack previousTrack, AudioTrack newTrack)
            {
                refreshVisibleTrackData(previousTrack, newTrack);
            }
        });

        // Play State Change Listener
        mediaService.setOnPlayStateChangedListener(new MediaService.OnPlayStateChangedListener()
        {
            @Override
            public void onStateChanged(MediaService.PlayerState oldState, MediaService.PlayerState newState)
            {
                switch (newState)
                {
                case Playing:
                    m_updateTimecodeView = true;
                    m_handler.post(m_updateTimecodeViewRunnable);
                    break;
                case Paused:
                case Stopped:
                    m_updateTimecodeView = false;
                    break;
                }

                updatePlayButtonImage();
                updateTrackPositionInterface();
            }
        });

        refreshVisibleTrackData(null, mediaService.getCurrentTrack());
    }

    @Override
    protected void onMediaServiceDisconnected()
    {
        m_updateTimecodeView = false;
    }

    /**
     * Queries the media service to get the current track information. This
     * function is bound to the OnTrackChanged event and is also called upon
     * initial service connection.
     */
    private void refreshVisibleTrackData(AudioTrack previousTrack, AudioTrack newTrack)
    {
        if (newTrack == null)
        {
            return;
        }

        // Album artwork
        ImageView albumArtLargePlaceholderView = (ImageView) findViewById(R.id.player_large_albumart_placeholder);
        ImageView albumArtLargeView = (ImageView) findViewById(R.id.player_large_albumart);

        // Adjust the visibility of placeholder views
        albumArtLargePlaceholderView.setVisibility(newTrack.CoverArt != null ? View.GONE : View.VISIBLE);
        albumArtLargeView.setVisibility(newTrack.CoverArt != null ? View.VISIBLE : View.GONE);

        // Load the album bitmaps if needed
        if (previousTrack == null || previousTrack.CoverArt == null || !previousTrack.CoverArt.equals(newTrack.CoverArt))
        {
            ImageView albumArtView = (ImageView) findViewById(R.id.element_song_artwork);
            m_bitmapResolver.makeRequest(albumArtView, newTrack.CoverArt, 100);

            if (newTrack.CoverArt != null)
            {
                m_bitmapResolver.makeRequest(albumArtLargeView, newTrack.CoverArt, 500);
            }
            else
            {
                albumArtLargeView.setImageBitmap(null);
            }
        }

        // Track title, artist and album
        ((TextView)findViewById(R.id.element_song_title)).setText(newTrack.Title);
        ((TextView)findViewById(R.id.element_song_artist)).setText(newTrack.Artist);
        ((TextView)findViewById(R.id.element_song_album)).setText(newTrack.Album);
        (findViewById(R.id.element_song_duration)).setVisibility(View.GONE);
        (findViewById(R.id.element_song_menu)).setVisibility(View.GONE);

        // Static timecode views
        ((TextView)findViewById(R.id.player_track_duration)).setText(newTrack.getFormattedDuration());
        ((SeekBar)findViewById(R.id.player_seek_bar)).setMax(newTrack.Duration);

        // Play/Pause button
        updatePlayButtonImage();

        m_handler.post(m_updateTimecodeViewRunnable);
    }

    /**
     * Triggered when the previous button is clicked in the transport controls
     * section of the UI.
     *
     * @param view the view that was clicked
     */
    public void onPrevButtonClick(View view)
    {
        final int PREV_THRESHOLD = 2 * 1000; // 2 second threshold for going to the previous track

        final MediaService service = getMediaService();
        if (service.getPlayheadTimecode() > PREV_THRESHOLD)
        {
            service.setPlayheadTimecode(0);
            updateTrackPositionInterface();
        }
        else
        {
            service.previousTrack();
        }
    }

    /**
     * Triggered when the play/pause button is clicked in the transport
     * controls section of the UI.
     *
     * @param view the view that was clicked
     */
    public void onPlayButtonClick(View view)
    {
        final MediaService service = getMediaService();
        switch(service.getPlayerState())
        {
        case Playing:
            service.pause();
            break;
        case Paused:
        case Stopped:
            service.play();
            break;
        }
        updatePlayButtonImage();
    }

    /**
     * Triggered when the next button is clicked in the transport controls
     * section of the UI.
     *
     * @param view the view that was clicked
     */
    public void onNextButtonClick(View view)
    {
        final MediaService service = getMediaService();
        service.nextTrack();
    }

    /**
     * Triggered when the shuffle button is toggled.
     *
     * @param view the view that was clicked
     */
    public void onShuffleButtonClick(View view)
    {
        final MediaService service = getMediaService();
        ImageButton imageButton = (ImageButton)view;

        imageButton.setImageResource(m_shuffle ? R.drawable.player_shuffle : R.drawable.player_shuffle_selected);
        m_shuffle = !m_shuffle;

        service.setShuffle(m_shuffle);
    }

    /**
     * Triggered when the repeat button is toggled.
     *
     * @param view the view that was clicked
     */
    public void onRepeatButtonClick(View view)
    {
        final MediaService service = getMediaService();
        ImageButton imageButton = (ImageButton)view;

        imageButton.setImageResource(m_repeat ? R.drawable.player_repeat : R.drawable.player_repeat_selected);
        m_repeat = !m_repeat;

        service.setRepeat(m_repeat);
    }

    private void updatePlayButtonImage()
    {
        final MediaService service = getMediaService();
        ImageButton imageButton = ((ImageButton)findViewById(R.id.player_play_button));

        switch(service.getPlayerState())
        {
        case Playing:
            imageButton.setImageResource(R.drawable.player_pause);
            break;
        case Paused:
        case Stopped:
            imageButton.setImageResource(R.drawable.player_play);
            break;
        }
    }

    private void updateTrackPositionInterface()
    {
        final MediaService service = getMediaService();

        int timecode = service.getPlayheadTimecode();
        ((SeekBar)findViewById(R.id.player_seek_bar)).setProgress(timecode);
        ((TextView)findViewById(R.id.player_track_position)).setText(AudioTrack.formatDuration(timecode));
    }

    /**
     * Listener for the seek bar, specifically for user input so we can update
     * other related UI elements without continuously notifying the media
     * service while keeping the user informed of changes they are making.
     */
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener
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
            MediaService service = getMediaService();

            m_stateOnStartTouch = service.getPlayerState();
            service.pause();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar)
        {
            MediaService service = getMediaService();

            service.setPlayheadTimecode(seekBar.getProgress());
            if (m_stateOnStartTouch == MediaService.PlayerState.Playing)
            {
                service.play();
            }
        }
    }
}
