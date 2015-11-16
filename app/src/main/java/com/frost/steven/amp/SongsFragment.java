package com.frost.steven.amp;

import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SongsFragment extends Fragment
{
    private static final String FRAGMENT_ID = "com.frost.steven.amp.SongsFragment";

    private BitmapProvider m_bitmapProvider;

    private SongRecyclerViewAdapter m_songRecyclerViewAdapter = null;

    public static SongsFragment getInstance()
    {
        return new SongsFragment();
    }

    public SongsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Playlist playlist = new Playlist();
        PlaylistCreator playlistCreator = new PlaylistCreator(
            getActivity().getContentResolver(),
            playlist,
            null,
            MediaStore.Audio.Media.TITLE
        );
        m_songRecyclerViewAdapter = new SongRecyclerViewAdapter((MediaServiceActivity)getActivity(), m_bitmapProvider, playlistCreator);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        RecyclerView view = (RecyclerView)inflater.inflate(R.layout.fragment_songs, container, false);

        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(m_songRecyclerViewAdapter);

        return view;
    }

    public void setBitmapProvider(BitmapProvider bitmapProvider)
    {
        m_bitmapProvider = bitmapProvider;
    }
}
