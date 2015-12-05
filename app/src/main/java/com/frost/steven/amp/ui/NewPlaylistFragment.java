package com.frost.steven.amp.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.frost.steven.amp.R;
import com.frost.steven.amp.helpers.DBPlaylistManager;

public class NewPlaylistFragment extends DialogFragment
{
    long m_trackId;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle bundle = getArguments();
        m_trackId = bundle.getLong("trackID");

        final DBPlaylistManager.Container container = (DBPlaylistManager.Container)getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.fragment_playlist_details, null);

        builder.setTitle("New Playlist");
        builder.setView(view);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                DBPlaylistManager playlistManager = container.getDBPlaylistManager();
                TextView textView = (TextView)(view.findViewById(R.id.playlist_details_name));

                // TODO: Validate & sanitize the input
                playlistManager.create(textView.getText().toString(), m_trackId);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                NewPlaylistFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }
}
