package com.frost.steven.amp.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.frost.steven.amp.R;
import com.frost.steven.amp.helpers.BitmapResolver;
import com.frost.steven.amp.model.Album;

import java.util.ArrayList;
import java.util.List;

public class AlbumsFragment extends Fragment
{
    public static final  String BUNDLE_PARCEL_ALBUM = "com.frost.steven.amp.BundleParcelAlbum";

    private LibraryActivity m_activity;

    private RecyclerViewAdapter m_recyclerViewAdapter = null;
    private Album.ListCreator   m_albumListCreatorTask = null;
    private List<Album>         m_albums;


    public static AlbumsFragment getInstance()
    {
        return new AlbumsFragment();
    }

    public AlbumsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_activity = (LibraryActivity)getActivity();

        m_albums = new ArrayList<>();
        m_recyclerViewAdapter = new RecyclerViewAdapter(m_albums);

        m_albumListCreatorTask = new Album.ListCreator(
            getActivity().getContentResolver(),
            m_albums
        );
        m_albumListCreatorTask.setOnListCompletedListener(m_recyclerViewAdapter);
        m_albumListCreatorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        RecyclerView view = (RecyclerView)inflater.inflate(R.layout.fragment_albums, container, false);

        view.setLayoutManager(new LinearLayoutManager(view.getContext()));
        view.setAdapter(m_recyclerViewAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>
            implements Album.ListCreator.OnListCompletedListener
    {
        private List<Album> m_albums;

        @Override
        public void onListCompleted()
        {
            m_albumListCreatorTask = null;
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            public final View      m_view;
            public final TextView  m_title;
            public final TextView  m_artist;
            public final ImageView m_artwork;

            public ViewHolder(View view)
            {
                super(view);

                m_view    = view;
                m_title   = (TextView)view.findViewById(R.id.element_album_title);
                m_artist  = (TextView)view.findViewById(R.id.element_album_artist);
                m_artwork = (ImageView)view.findViewById(R.id.element_album_artwork);
            }
        }

        public Album getValueAt(int position)
        {
            return m_albums.get(position);
        }

        public RecyclerViewAdapter(List<Album> albums)
        {
            m_albums = albums;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.element_album, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position)
        {
            Album album = getValueAt(position);

            holder.m_title.setText(album.Title);
            holder.m_artist.setText(album.Artist);

            holder.m_view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Intent intent = new Intent(getActivity(), AlbumActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(BUNDLE_PARCEL_ALBUM, m_albums.get(position));
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });

            BitmapResolver resolver = m_activity.getBitmapProvider();
            if (resolver != null)
            {
                resolver.makeRequest(holder.m_artwork, album.Artwork, 100);
            }
        }

        @Override
        public int getItemCount()
        {
            if (m_albums != null)
            {
                return m_albums.size();
            }
            return 0;
        }
    }
}
