package com.frost.steven.amp.service;

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
import android.widget.Toast;

import com.frost.steven.amp.ui.PlayerActivity;
import com.frost.steven.amp.R;
import com.frost.steven.amp.model.AudioTrack;
import com.frost.steven.amp.model.Playlist;

import java.io.IOException;

/**
 * A foreground service that acts as a front end for a media player. This
 * service can be sent a playlist and queried for state as well as instructed
 * to perform several operations on the current playlist.
 *
 * This service also displays a permanent notification while in a playing state
 * which can be dismissed while music is paused or stopped.
 */
public class MediaService extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener
{
    public static final String ACTION_PREVIOUS   = "com.frost.steven.MediaService.ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PAUSE = "com.frost.steven.MediaService.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT       = "com.frost.steven.MediaService.ACTION_NEXT";

    private static final int NOTIFICATION_ID = 1337;

    private OnTrackChangedListener     m_onTrackChanged     = null;
    private OnPlayStateChangedListener m_onPlayStateChanged = null;

    private PlayerState m_playerState = PlayerState.Stopped;
    private Playlist    m_playlist    = null;
    private AudioTrack  m_prevTrack   = null;

    private final IBinder       m_binder = new MediaBinder();
    private MediaPlayer         m_player = null;
    private NotificationManager m_notificationManager;
    private BroadcastReceiver   m_broadcastReceiver;
    private AudioManager        m_audioManager;

    public enum PlayerState
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

        m_audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        m_player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        m_player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        m_player.setOnPreparedListener(this);
        m_player.setOnCompletionListener(this);
        m_player.setOnErrorListener(this);

        // Register the broadcast receiver for the notification transport controls and headphone removal
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PREVIOUS);
        intentFilter.addAction(ACTION_PLAY_PAUSE);
        intentFilter.addAction(ACTION_NEXT);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        m_broadcastReceiver = new NotificationBroadcastReceiver();
        registerReceiver(m_broadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy()
    {
        stop();
        m_notificationManager.cancel(NOTIFICATION_ID);

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

    @Override
    public void onPrepared(MediaPlayer player)
    {
        player.start();
        setPlayerState(PlayerState.Playing);
    }

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

    @Override
    public boolean onError(MediaPlayer player, int what, int extra)
    {
        setPlayerState(PlayerState.Stopped);

        Log.e(MediaService.class.getName(), "An error occurred in the media player");
        return true;
    }

    @Override
    public void onAudioFocusChange(int focusChange)
    {
        switch (focusChange)
        {
        case AudioManager.AUDIOFOCUS_GAIN:
            if (m_playerState != PlayerState.Playing)
            {
                playAuthorized();
                m_player.setVolume(1.0f, 1.0f);
            }
            break;
        case AudioManager.AUDIOFOCUS_LOSS:
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            if (m_playerState == PlayerState.Playing)
            {
                pause();
            }
            break;
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            if (m_playerState == PlayerState.Playing)
            {
                m_player.setVolume(0.1f, 0.1f);
            }
            break;
        }
    }

    /**
     * Requests audio focus from the audio manager. If successful the track
     * will begin playing shortly after.
     *
     * Unsuccessful requests will result in a toast notifying the user that we
     * were unable to play audio. No state will be changed as a result of this.
     */
    public void play()
    {
        int result = m_audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        {
            Toast.makeText(getApplicationContext(), "Unable to play audio at this time.", Toast.LENGTH_LONG).show();
        }
        else
        {
            playAuthorized();
        }
    }

    /**
     * Plays the track pointed to by the position field in the playlist. If the
     * player is in the `Paused` state when this function is called the track
     * will be resumed.
     */
    private void playAuthorized()
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

    /**
     * Stops the current track from playing and moves to the previous track in
     * the playlist. The new track will play shortly after.
     */
    public void previousTrack()
    {
        stop();
        m_playlist.moveToPreviousTrack();
        notifyTrackChanged();
        play();
    }

    /**
     * Stops the current track from playing and moves to the next track in the
     * playlist. The new track will play shortly after.
     */
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

    public void setPlaylist(Playlist playlist)
    {
        m_playlist = playlist;
        m_prevTrack = m_playlist.getCurrentTrack();
    }

    public PlayerState getPlayerState()
    {
        return m_playerState;
    }

    /**
     * Attaches a listener to listen for track changed events.
     *
     * @param listener the listener to attach
     */
    public void setOnTrackChangedListener(@Nullable OnTrackChangedListener listener)
    {
        m_onTrackChanged = listener;
    }

    /**
     * Attaches a listener to listen for player state changes.
     *
     * @param listener the listener to attach
     */
    public void setOnPlayStateChangedListener(@Nullable OnPlayStateChangedListener listener)
    {
        m_onPlayStateChanged = listener;
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

    /**
     * Notifies any listeners that the track
     */
    private void notifyTrackChanged()
    {
        if (m_onTrackChanged != null && m_playlist != null)
        {
            AudioTrack currentTrack = m_playlist.getCurrentTrack();
            m_onTrackChanged.onTrackChanged(m_prevTrack, currentTrack);
            m_prevTrack = currentTrack;
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

    /**
     * Updates the service notification with the latest content and player
     * state. An existing instance of the notification will be replaced.
     */
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

        AudioTrack track = m_playlist.getCurrentTrack();
        builder.setTicker(getResources().getString(R.string.notification_now_playing).replace("{0}", track.Title));
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setAutoCancel(false);
        builder.setOngoing(m_playerState == PlayerState.Playing);

        Notification notification = builder.build();
        notification.priority = Notification.PRIORITY_MAX;
        if (Build.VERSION.SDK_INT >= 21)
        {
            notification.visibility = Notification.VISIBILITY_PUBLIC;
        }

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_player);
        updateRemoteViewElements(contentView);
        notification.contentView = contentView;

        if (Build.VERSION.SDK_INT >= 16)
        {
            RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_player_expanded);
            updateRemoteViewElements(expandedView);
            notification.bigContentView = expandedView;
        }

        m_notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Updates remove UI elements in the given remote view. This is for use
     * with the two types of notification available as they share the same
     * IDs.
     *
     * @param view the remote view to update.
     */
    private void updateRemoteViewElements(RemoteViews view)
    {
        AudioTrack track = m_playlist.getCurrentTrack();

        view.setOnClickPendingIntent(R.id.notification_player_prev, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_PREVIOUS), 0));
        view.setOnClickPendingIntent(R.id.notification_player_playpause, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_PLAY_PAUSE), 0));
        view.setOnClickPendingIntent(R.id.notification_player_next, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_NEXT), 0));

        view.setTextViewText(R.id.notification_player_title, track.Title);
        view.setTextViewText(R.id.notification_player_artist, track.Artist);
        view.setTextViewText(R.id.notification_player_album, track.Album);

        view.setImageViewResource(R.id.notification_player_playpause, m_playerState == PlayerState.Playing ? R.drawable.ic_player_pause_minimal : R.drawable.ic_player_play_minimal);

        if (track.CoverArt != null)
        {
            view.setViewPadding(R.id.notification_player_artwork, 0, 0, 0, 0);
            view.setImageViewUri(R.id.notification_player_artwork, track.CoverArt);
        }
        else
        {
            view.setImageViewResource(R.id.notification_player_artwork, R.drawable.ic_album_placeholder_100);
        }
    }

    /**
     * Basic binder extension for the media service
     */
    public class MediaBinder extends Binder
    {
        public MediaService getService()
        {
            return MediaService.this;
        }
    }

    public interface OnTrackChangedListener
    {
        void onTrackChanged(AudioTrack previousTrack, AudioTrack newTrack);
    }

    public interface OnPlayStateChangedListener
    {
        void onStateChanged(PlayerState oldState, PlayerState newState);
    }

    /**
     * Broadcast receiver that handles various custom transport controls and
     * headphone removal broadcasts. This class directly alters the service
     * state.
     */
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
            case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                if (m_playerState == PlayerState.Playing)
                {
                    pause();
                }
                break;
            }
        }
    }
}
