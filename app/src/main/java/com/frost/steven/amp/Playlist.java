package com.frost.steven.amp;

import java.util.List;

/**
 * A playlist is a collection of `AudioTrack` objects. This is the only object
 * that should be sent to the player. If the user selects a single song in the
 * global songs list then a playlist containing every song will be submitted
 * with a start index of the item pressed in the list. Shuffle is easy to
 * achieve  since we can generate a random number for the song index.
 */
public class Playlist
{
    public List<AudioTrack> Tracks;
    public Integer          Position;
    public Boolean          Shuffle;
};
