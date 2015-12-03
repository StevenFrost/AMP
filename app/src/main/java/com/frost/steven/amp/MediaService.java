package com.frost.steven.amp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;

/**
 * A foreground service that acts as a front end for a media player. This
 * service can be sent a playlist and queried for state as well as instructed
 * to perform several operations on the current playlist.
 *
 * This service also displays a permanent notification while in a playing state
 * which can be dismissed while music is paused or stopped.
 */
public class MediaService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener
{
    public static final String ACTION_PREVIOUS   = "com.frost.steven.MediaService.ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PAUSE = "com.frost.steven.MediaService.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT       = "com.frost.steven.MediaService.ACTION_NEXT";

    // Event listeners
    private OnTrackChangedListener     m_onTrackChanged     = null;
    private OnPlayStateChangedListener m_onPlayStateChanged = null;

    // State
    private PlayerState m_playerState = PlayerState.Stopped;
    private Playlist    m_playlist    = null;

    // Assorted private members
    private final IBinder       m_binder = new MediaBinder();
    private MediaPlayer         m_player = null;
    private NotificationManager m_notificationManager;
    private BroadcastReceiver   m_broadcastReceiver;

    enum PlayerState
    {
        Paused,
        Playing,
        Stopped
    }

    public MediaService() {}

    @Override
    public void onCreate()
    {
        super.onCreate();
        m_notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        m_player = new MediaPlayer();

        m_player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        m_player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        m_player.setOnPreparedListener(this);
        m_player.setOnCompletionListener(this);
        m_player.setOnErrorListener(this);

        // Register the broadcast receiver for the notification transport controls
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PREVIOUS);
        intentFilter.addAction(ACTION_PLAY_PAUSE);
        intentFilter.addAction(ACTION_NEXT);

        m_broadcastReceiver = new NotificationBroadcastReceiver();
        registerReceiver(m_broadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy()
    {
        stop();
        m_notificationManager.cancel(1337);

        m_player.reset();
        m_player.release();

        unregisterReceiver(m_broadcastReceiver);

        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return m_binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    /**
     * Called when the media player is ready to play the current track. This
     * results in the player state changing to `Playing` and the notification
     * is updated with the new track information.
     *
     * @param player the media player instance
     */
    @Override
    public void onPrepared(MediaPlayer player)
    {
        player.start();
        setPlayerState(PlayerState.Playing);
    }

    /**
     * Called when the current track has finished playing. Results in a stopped
     * player state followed by a play if there is another track available to
     * play.
     *
     * @param player the media player instance
     */
    @Override
    public void onCompletion(MediaPlayer player)
    {
        setPlayerState(PlayerState.Stopped);

        if (m_playlist.hasNextTrack())
        {
            m_playlist.moveToNextTrack();
            notifyTrackChanged();
            play();
        }
    }

    /**
     * Called when an error occurred in the media player. The player state
     * will be set to `Stopped` and no further items will be played.
     *
     * @return true since the error has been handled
     */
    @Override
    public boolean onError(MediaPlayer player, int what, int extra)
    {
        setPlayerState(PlayerState.Stopped);

        Log.e(MediaService.class.getName(), "An error occurred in the media player");
        return true;
    }

    /**
     * Plays the track pointed to by the position field in the playlist. If the
     * player is in the `Paused` state when this function is called the track
     * will be resumed.
     */
    public void play()
    {
        if (m_playerState == PlayerState.Paused)
        {
            m_player.start();
            setPlayerState(PlayerState.Playing);
            return;
        }

        m_player.reset();
        AudioTrack track = m_playlist.getCurrentTrack();
        Uri trackUri = Uri.parse(track.Data);

        try
        {
            m_player.setDataSource(this, trackUri);
            m_player.prepareAsync();
        }
        catch (IOException ex)
        {
            Log.e(MediaService.class.getName(), "Error setting data source to '" + trackUri.toString() + "'.");
        }
    }

    /**
     * Pauses the media player only if it is currently in the `Playing` state,
     * has no effect otherwise.
     */
    public void pause()
    {
        if (m_playerState == PlayerState.Playing)
        {
            m_player.pause();
            setPlayerState(PlayerState.Paused);
        }
    }

    /**
     * Stops the media player only if it is currently in the `Playing` or
     * `Paused` state, has no effect otherwise.
     */
    public void stop()
    {
        if (m_playerState != PlayerState.Stopped)
        {
            m_player.stop();
            setPlayerState(PlayerState.Stopped);
        }
    }

    public void previousTrack()
    {
        stop();
        m_playlist.moveToPreviousTrack();
        notifyTrackChanged();
        play();
    }

    public void nextTrack()
    {
        stop();
        m_playlist.moveToNextTrack();
        notifyTrackChanged();
        play();
    }

    public void setRepeat(boolean repeat)
    {
        m_playlist.setRepeat(repeat);
    }

    public void setShuffle(boolean shuffle)
    {
        m_playlist.setShuffle(shuffle);
    }

    public void setOnTrackChangedListener(@Nullable OnTrackChangedListener listener)
    {
        m_onTrackChanged = listener;
    }

    public void setOnPlayStateChangedListener(@Nullable OnPlayStateChangedListener listener)
    {
        m_onPlayStateChanged = listener;
    }

    public void setPlaylist(Playlist playlist)
    {
        m_playlist = playlist;
    }

    /**
     * Gets the track pointed to by the playlist cursor. There is no guarantee
     * that the audio track is currently playing.
     *
     * @return The current playing or pending audio track
     */
    public AudioTrack getCurrentTrack()
    {
        if (m_playlist != null)
        {
            return m_playlist.getCurrentTrack();
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

    public PlayerState getPlayerState()
    {
        return m_playerState;
    }

    private void notifyTrackChanged()
    {
        if (m_onTrackChanged != null && m_playlist != null)
        {
            m_onTrackChanged.onTrackChanged(m_playlist.getCurrentTrack());
        }
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
        updateNotification();
    }

    private void updateNotification()
    {
        if (m_playlist == null)
        {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        builder.setTicker("Custom Notification");
        builder.setSmallIcon(R.drawable.notes);
        builder.setAutoCancel(false);
        builder.setOngoing(m_playerState == PlayerState.Playing);

        Notification notification = builder.build();
        notification.priority = Notification.PRIORITY_MAX;
        notification.visibility = Notification.VISIBILITY_PUBLIC;

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_player);
        updateRemoteViewElements(contentView);
        notification.contentView = contentView;

        if (Build.VERSION.SDK_INT >= 16)
        {
            RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_player_expanded);
            updateRemoteViewElements(expandedView);
            notification.bigContentView = expandedView;
        }

        m_notificationManager.notify(1337, notification);
    }

    private void updateRemoteViewElements(RemoteViews view)
    {
        AudioTrack track = m_playlist.getCurrentTrack();

        view.setOnClickPendingIntent(R.id.notification_player_prev, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_PREVIOUS), 0));
        view.setOnClickPendingIntent(R.id.notification_player_playpause, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_PLAY_PAUSE), 0));
        view.setOnClickPendingIntent(R.id.notification_player_next, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_NEXT), 0));

        view.setTextViewText(R.id.notification_player_title, track.Title);
        view.setTextViewText(R.id.notification_player_artist, track.Artist);
        view.setTextViewText(R.id.notification_player_album, track.Album);

        view.setImageViewResource(R.id.notification_player_playpause, m_playerState == PlayerState.Playing ? R.drawable.player_pause_minimal : R.drawable.player_play_minimal);
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

    private class NotificationBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            switch(action)
            {
            case ACTION_PREVIOUS:
                previousTrack();
                break;
            case ACTION_PLAY_PAUSE:
                if (m_playerState == PlayerState.Playing)
                {
                    pause();
                }
                else
                {
                    play();
                }
                break;
            case ACTION_NEXT:
                nextTrack();
                break;
            }
        }
    }
}
