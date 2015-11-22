package com.frost.steven.amp;

import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class SongsFragment extends Fragment
{
    private static final String FRAGMENT_ID = "com.frost.steven.amp.SongsFragment";

    private RecyclerView            m_recyclerView;
    private SongRecyclerViewAdapter m_songRecyclerViewAdapter = null;

    public static SongsFragment getInstance()
    {
        return new SongsFragment();
    }

    public SongsFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        BitmapProvider bitmapProvider = ((LibraryActivity)getActivity()).getBitmapProvider();
        List<DBPlaylist> playlists = ((LibraryActivity)getActivity()).getPlaylists();

        Playlist playlist = new Playlist();
        Playlist.ListCreator playlistCreator = new Playlist.ListCreator(
            getActivity().getContentResolver(),
            playlist,
            null,
            MediaStore.Audio.Media.TITLE
        );
        m_songRecyclerViewAdapter = new SongRecyclerViewAdapter((MediaServiceActivity)getActivity(), bitmapProvider, playlists, playlistCreator);
        m_recyclerView.setAdapter(m_songRecyclerViewAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        m_recyclerView = (RecyclerView)inflater.inflate(R.layout.fragment_songs, container, false);
        m_recyclerView.setLayoutManager(new LinearLayoutManager(m_recyclerView.getContext()));

        return m_recyclerView;
    }
}
