package com.frost.steven.amp.model;

import android.net.Uri;

import java.util.concurrent.TimeUnit;

/**
 * Basic representation of an audio track that holds the title, artist, album
 * and any available cover art in a single structure that can be bound to a
 * list or detailed layout.
 */
public class AudioTrack
{
    public long   ID;       /** Track ID                       */
    public String Title;    /** Track title                    */
    public String Artist;   /** Primary artist                 */
    public String Album;    /** Album name                     */
    public String Data;     /** Track location in storage      */
    public Uri    CoverArt; /** Cover art URI                  */
    public int    Duration; /** Track duration in milliseconds */

    public AudioTrack(long id, String title, String artist, String album, String data, Uri coverArt, int duration)
    {
        ID       = id;
        Title    = title;
        Artist   = artist;
        Album    = album;
        Data     = data;
        CoverArt = coverArt;
        Duration = duration;
    }

    /**
     * Gets the audio track duration as a formatted string
     *
     * @return Formatted String
     */
    public String getFormattedDuration()
    {
        return formatDuration(Duration);
    }

    /**
     * Formats a number of milliseconds into a `mm:ss` string
     *
     * @param milliseconds number of milliseconds
     *
     * @return Formatted string
     */
    public static String formatDuration(int milliseconds)
    {
        return String.format("%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1)
        );
    }
}
