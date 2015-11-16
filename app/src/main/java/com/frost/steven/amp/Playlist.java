package com.frost.steven.amp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A playlist is a collection of `AudioTrack` objects. This is the only object
 * that should be sent to the player. If the user selects a single song in the
 * global songs list then a playlist containing every song will be submitted
 * with a start index of the item pressed in the list.
 */
public class Playlist
{
    private List<AudioTrack> m_originalTracks;
    private List<AudioTrack> m_shuffledTracks;
    private Integer          m_cursor;

    private Random           m_random;
    private Boolean          m_repeat;
    private Boolean          m_shuffle;

    public Playlist()
    {
        m_originalTracks = new ArrayList<>();
        m_shuffledTracks = new ArrayList<>();
        m_cursor = 0;
        m_random = new Random();
        m_repeat = false;
        m_shuffle = false;
    }

    public void addTrack(AudioTrack track)
    {
        m_originalTracks.add(track);
    }

    public void setShuffle(boolean shuffle)
    {
        if (!m_shuffle && shuffle)
        {
            m_shuffle = true;

            m_shuffledTracks.clear();
            for (AudioTrack track : m_originalTracks)
            {
                m_shuffledTracks.add(track);
            }

            // Swap the current song and the first song
            AudioTrack temp = m_shuffledTracks.get(0);
            m_shuffledTracks.set(0, m_shuffledTracks.get(m_cursor));
            m_shuffledTracks.set(m_cursor, temp);

            // Shuffle the rest of the list
            for (int i = m_shuffledTracks.size() - 1; i > 0; --i)
            {
                int idx = m_random.nextInt(i == 1 ? 1 : i - 1) + 1;
                AudioTrack track = m_shuffledTracks.get(i);
                m_shuffledTracks.set(i, m_shuffledTracks.get(idx));
                m_shuffledTracks.set(idx, track);
            }

            m_cursor = 0;
        }
        else if (m_shuffle && !shuffle)
        {
            m_shuffle = false;

            // Find the position of the current song in the original list
            int cursor = m_cursor;
            m_cursor = 0;
            for (int i = 0; i < m_originalTracks.size(); ++i)
            {
                if (m_shuffledTracks.get(cursor) == m_originalTracks.get(i))
                {
                    m_cursor = i;
                    break;
                }
            }

            m_shuffledTracks.clear();
        }
    }

    public void setRepeat(boolean repeat)
    {
        m_repeat = repeat;
    }

    public AudioTrack moveToPreviousTrack()
    {
        if (m_cursor == 0 && m_repeat)
        {
            m_cursor = getNumTracks() - 1;
        }
        else if (m_cursor != 0)
        {
            --m_cursor;
        }
        return getCurrentTrack();
    }

    public void setCursor(int cursor)
    {
        m_cursor = cursor;
    }

    public AudioTrack getCurrentTrack()
    {
        if (m_shuffle)
        {
            return m_shuffledTracks.get(m_cursor);
        }
        return m_originalTracks.get(m_cursor);
    }

    public AudioTrack moveToNextTrack()
    {
        int end = getNumTracks() - 1;
        if (m_cursor == end && m_repeat)
        {
            m_cursor = 0;
        }
        else if (m_cursor != end)
        {
            ++m_cursor;
        }
        return getCurrentTrack();
    }

    public int getNumTracks()
    {
        return (m_shuffle ? m_shuffledTracks.size() : m_originalTracks.size());
    }

    public AudioTrack getUnshuffledTrack(int position)
    {
        return m_originalTracks.get(position);
    }

    public boolean hasNextTrack()
    {
        return m_repeat || m_cursor < (getNumTracks() - 1);
    }

    public boolean hasPreviousTrack()
    {
        return m_repeat || m_cursor > 0;
    }
}
