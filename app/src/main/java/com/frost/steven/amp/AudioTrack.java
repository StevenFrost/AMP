package com.frost.steven.amp;

import android.net.Uri;

import java.util.concurrent.TimeUnit;

/**
 * Basic representation of an audio track that holds the title, artist, album
 * and any available cover art in a single structure that can be bound to a
 * list or detailed layout.
 */
public class AudioTrack
{
    public String Title;    /** Track title                    */
    public String Artist;   /** Primary artist                 */
    public String Album;    /** Album name                     */
    public String Data;     /** Track location in storage      */
    public Uri    CoverArt; /** Cover art graphic              */
    public int    Duration; /** Track duration in milliseconds */

    public AudioTrack()
    {
        super();
    }

    public AudioTrack(String title, String artist, String album)
    {
        Title = title;
        Artist = artist;
        Album = album;
    }

    public AudioTrack(String title, String artist, String album, String data, Uri coverArt, int duration)
    {
        Title = title;
        Artist = artist;
        Album = album;
        Data = data;
        CoverArt = coverArt;
        Duration = duration;
    }

    public String getFormattedDuration()
    {
        return formatDuration(Duration);
    }

    public static String formatDuration(int milliseconds)
    {
        return String.format("%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1)
        );
    }
}
