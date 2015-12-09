package com.frost.steven.amp.ui.listeners;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.frost.steven.amp.R;
import com.frost.steven.amp.helpers.DBPlaylistManager;
import com.frost.steven.amp.model.DBPlaylist;

import java.util.List;

/**
 * On click listener for use in the edit playlist and create playlist
 * fragments. This class provides common validation for both entry points
 * and allows an action to be performed upon successful validation through
 * a clean interface.
 */
public abstract class PlaylistDetailsPositiveOnClickListener implements View.OnClickListener
{
    private View              m_view;
    private Context           m_context;
    private Resources         m_resources;
    private Dialog            m_dialog;
    private DBPlaylistManager m_playlistManager;

    private long m_trackID;

    public PlaylistDetailsPositiveOnClickListener(Context context, View view, Resources resources, Dialog dialog, DBPlaylistManager playlistManager, long trackID)
    {
        m_view = view;
        m_context = context;
        m_resources = resources;
        m_dialog = dialog;
        m_playlistManager = playlistManager;

        m_trackID = trackID;
    }

    @Override
    public void onClick(View v)
    {
        TextView textView = (TextView)(m_view.findViewById(R.id.playlist_details_name));
        String playlistName = textView.getText().toString();

        // Empty string validation
        if (playlistName.isEmpty())
        {
            String message = m_resources.getString(R.string.playlist_validation_empty);
            Toast.makeText(m_context, message, Toast.LENGTH_LONG).show();
            return;
        }

        // Character usage validation
        if (!playlistName.matches("[a-zA-Z0-9 _-]+"))
        {
            Toast.makeText(m_context, "Playlist name must be alphanumeric, spaces, underscores and dashes only.", Toast.LENGTH_LONG).show();
            return;
        }

        // Playlist already exists validation
        List<DBPlaylist> playlists = m_playlistManager.getPlaylists();
        for (DBPlaylist playlist : playlists)
        {
            if (playlistName.equals(playlist.Name))
            {
                String message = m_resources.getString(R.string.playlist_validation_exists);
                Toast.makeText(m_context, message, Toast.LENGTH_LONG).show();
                return;
            }
        }

        onSuccessfulValidation(m_trackID, playlistName, m_playlistManager);
        m_dialog.dismiss();
    }

    protected abstract void onSuccessfulValidation(long trackID, String playlistName, DBPlaylistManager playlistManager);
}
