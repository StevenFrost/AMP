package com.frost.steven.amp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class PlaylistsFragment extends Fragment
{
    private static final String FRAGMENT_ID = "com.frost.steven.amp.PlaylistsFragment";
    public static final  String BUNDLE_PARCEL_PLAYLIST = "com.frost.steven.amp.BundleParcelPlaylist";

    private RecyclerView        m_recyclerView;
    private RecyclerViewAdapter m_recyclerViewAdapter = null;

    public static PlaylistsFragment getInstance()
    {
        return new PlaylistsFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        LibraryActivity activity = (LibraryActivity)getActivity();
        ListenableArrayList<DBPlaylist> playlists = activity.getPlaylists();

        m_recyclerViewAdapter = new RecyclerViewAdapter(playlists);
        playlists.attachListener(m_recyclerViewAdapter);
        m_recyclerView.setAdapter(m_recyclerViewAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        m_recyclerView = (RecyclerView)inflater.inflate(R.layout.fragment_playlists, container, false);
        m_recyclerView.setLayoutManager(new LinearLayoutManager(m_recyclerView.getContext()));

        return m_recyclerView;
    }

    class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
            implements ListenableArrayList.OnPlaylistCollectionChangedListener
    {
        private List<DBPlaylist> m_playlists;

        public RecyclerViewAdapter(List<DBPlaylist> playlists)
        {
            m_playlists = playlists;
        }

        @Override
        public void onPlaylistCollectionChanged(ListenableArrayList collection)
        {
            notifyDataSetChanged();
        }

        public DBPlaylist getValueAt(int position)
        {
            return m_playlists.get(position);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.element_playlist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position)
        {
            DBPlaylist playlist = getValueAt(position);

            holder.m_name.setText(playlist.Name);
            holder.m_date.setText(playlist.getFormattedDateAdded());

            holder.m_view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Toast.makeText(getActivity(), "Time to party!", Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public int getItemCount()
        {
            if (m_playlists != null)
            {
                return m_playlists.size();
            }
            return 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            public final View      m_view;
            public final TextView  m_name;
            public final TextView  m_date;
            public final ImageView m_menu;

            public ViewHolder(View view)
            {
                super(view);

                m_view = view;
                m_name = (TextView)view.findViewById(R.id.element_playlist_name);
                m_date = (TextView)view.findViewById(R.id.element_playlist_date);
                m_menu = (ImageView)view.findViewById(R.id.element_playlist_menu);
            }
        }
    }
}
