package com.frost.steven.amp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.Random;

public class MediaService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener
{
    // Event listeners
    private OnTrackChangedListener m_onTrackChanged = null;
    private OnPlayStateChangedListener m_onPlayStateChanged = null;

    // State
    private PlayerState m_playerState = PlayerState.Stopped;
    private Playlist    m_playlist = null;

    // Assorted private members
    private final IBinder m_binder = new MediaBinder();
    private final Random  m_random = new Random();
    private MediaPlayer   m_player = null;

    public MediaService() {}

    @Override
    public void onCreate()
    {
        super.onCreate();
        m_player = new MediaPlayer();

        m_player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        m_player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        m_player.setOnPreparedListener(this);
        m_player.setOnCompletionListener(this);
        m_player.setOnErrorListener(this);
    }

    @Override
    public void onDestroy()
    {
        stopForeground(true);
        m_player.reset();
        m_player.release();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return m_binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        stop();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer player)
    {
        player.start();
        setPlayerState(PlayerState.Playing);

        Intent notIntent = new Intent(this, LibraryActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.notes)
                .setTicker("My Song")
                .setOngoing(true)
                .setContentTitle("Playing")
        .setContentText("The Song Title");
        Notification not = builder.build();

        startForeground(1337, not);
    }

    @Override
    public void onCompletion(MediaPlayer player)
    {
        setPlayerState(PlayerState.Stopped);

        if (m_playlist.Shuffle)
        {
            m_playlist.Position = nextRandomTrackIndex();
        }
        else
        {
            m_playlist.Position++;
        }

        if (m_playlist.Position != m_playlist.Tracks.size())
        {
            if (m_onTrackChanged != null)
            {
                m_onTrackChanged.onTrackChanged(m_playlist.Tracks.get(m_playlist.Position));
            }
            play();
        }
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra)
    {
        return true;
    }

    public void setOnTrackChangedListener(@Nullable OnTrackChangedListener listener)
    {
        m_onTrackChanged = listener;
    }

    @Nullable
    public OnTrackChangedListener getOnTrackChangedListener()
    {
        return m_onTrackChanged;
    }

    public void setOnPlayStateChangedListener(@Nullable OnPlayStateChangedListener listener)
    {
        m_onPlayStateChanged = listener;
    }

    @Nullable
    public OnPlayStateChangedListener getOnPlayStateChangedListener()
    {
        return m_onPlayStateChanged;
    }

    public void setPlaylist(Playlist playlist)
    {
        m_playlist = playlist;
    }

    public void play()
    {
        // TODO: Handle states correctly here, it's not great currently
        if (m_playerState == PlayerState.Paused)
        {
            m_player.start();
            setPlayerState(PlayerState.Playing);
            return;
        }

        m_player.reset();
        AudioTrack track = m_playlist.Tracks.get(m_playlist.Position);
        Uri trackUri = Uri.parse(track.Data);

        try
        {
            m_player.setDataSource(this, trackUri);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        m_player.prepareAsync();
    }

    public void pause()
    {
        if (m_playerState == PlayerState.Playing)
        {
            m_player.pause();
            setPlayerState(PlayerState.Paused);
        }
        else
        {
            System.out.println("Media service paused while already in a paused or stopped state.");
        }
    }

    public void stop()
    {
        if (m_playerState != PlayerState.Stopped)
        {
            m_player.stop();
            setPlayerState(PlayerState.Stopped);
        }
    }

    /**
     * Gets the track pointed to by the playlist cursor. There is no guarantee
     * that the audio track is currently playing.
     *
     * @return The current playing or pending audio track
     */
    public AudioTrack getCurrentTrack()
    {
        if (m_playlist != null && m_playlist.Tracks != null)
        {
            return m_playlist.Tracks.get(m_playlist.Position);
        }
        return null;
    }

    /**
     * Gets the current playhead timecode in milliseconds
     *
     * @return the playhead timecode
     */
    public int getPlayheadTimecode()
    {
        return m_player.getCurrentPosition();
    }

    /**
     * Seeks to the given timecode (in milliseconds)
     *
     * @param timecode the timecode to seek to
     */
    public void setPlayheadTimecode(int timecode)
    {
        m_player.seekTo(timecode);
    }

    public void nextTrack()
    {
        m_player.stop();
        setPlayerState(PlayerState.Stopped);

        // TODO: Alter to work with shuffle
        m_playlist.Position++;
        if (m_onTrackChanged != null)
        {
            m_onTrackChanged.onTrackChanged(m_playlist.Tracks.get(m_playlist.Position));
        }

        play();
        setPlayerState(PlayerState.Playing);
    }

    public void previousTrack()
    {
        // TODO: Work out how to do this with shuffle
        m_player.stop();
        setPlayerState(PlayerState.Stopped);

        m_playlist.Position--;
        if (m_onTrackChanged != null)
        {
            m_onTrackChanged.onTrackChanged(m_playlist.Tracks.get(m_playlist.Position));
        }

        play();
        setPlayerState(PlayerState.Playing);
    }

    public PlayerState getPlayerState()
    {
        return m_playerState;
    }

    /**
     * Generates a new random index in the bounds of the tracks list for the
     * current playlist with the guarantee that the index will be different
     * from the current audio track index.
     *
     * @return A new random index into the playlist audio track array
     */
    private Integer nextRandomTrackIndex()
    {
        Integer position = m_random.nextInt(m_playlist.Tracks.size());
        if (position.equals(m_playlist.Position) && m_playlist.Tracks.size() != 1)
        {
            return nextRandomTrackIndex();
        }
        return position;
    }

    /**
     * Updates the player state and notifies any player state listeners that
     * something has changed.
     *
     * @param newState the new player state
     */
    private void setPlayerState(PlayerState newState)
    {
        PlayerState prevState = m_playerState;
        m_playerState = newState;

        if (m_onPlayStateChanged != null)
        {
            m_onPlayStateChanged.onStateChanged(prevState, newState);
        }
    }

    enum PlayerState
    {
        Paused,
        Playing,
        Stopped
    }

    /**
     * Basic binder extension for the media service
     */
    public class MediaBinder extends Binder
    {
        MediaService getService()
        {
            return MediaService.this;
        }
    }

    public interface OnTrackChangedListener
    {
        void onTrackChanged(AudioTrack track);
    }

    public interface OnPlayStateChangedListener
    {
        void onStateChanged(PlayerState oldState, PlayerState newState);
    }
}
