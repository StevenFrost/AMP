package com.frost.steven.amp.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.frost.steven.amp.R;
import com.frost.steven.amp.helpers.BitmapResolver;
import com.frost.steven.amp.helpers.DBPlaylistManager;
import com.frost.steven.amp.ui.adapters.SongRecyclerViewAdapter;
import com.frost.steven.amp.ui.listeners.MenuOnClickListener;

/**
 * Fragment representing the songs tab in the Library activity. This fragment
 * will only ever be loaded via the LibraryActivity class so we can safely
 * assume that we can access state by casting the generic Activity to this
 * derived version.
 */
public class SongsFragment extends Fragment
{
    private RecyclerView m_recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        m_recyclerView = (RecyclerView)inflater.inflate(R.layout.fragment_songs, container, false);
        m_recyclerView.setLayoutManager(new LinearLayoutManager(m_recyclerView.getContext()));

        return m_recyclerView;
    }

    @Override
    public void onResume()
    {
        LibraryActivity activity = (LibraryActivity)getActivity();

        BitmapResolver bitmapResolver = activity.getBitmapProvider();
        DBPlaylistManager playlistManager = activity.getDBPlaylistManager();

        // Attach the song recycler view adapter
        SongRecyclerViewAdapter songRecyclerViewAdapter = new SongRecyclerViewAdapter(
                activity.getMasterPlaylistTask(),
                activity,
                new MenuOnClickListener.SongListener.Factory(playlistManager), bitmapResolver
        );
        m_recyclerView.setAdapter(songRecyclerViewAdapter);

        super.onResume();
    }

    public static SongsFragment getInstance()
    {
        return new SongsFragment();
    }
}
