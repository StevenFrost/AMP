package com.frost.steven.amp;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class EditPlaylistFragment extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle bundle = getArguments();
        final int playlistPosition = bundle.getInt("playlistPosition");
        final String playlistName = bundle.getString("playlistName");

        final DBPlaylistManager.Container container = (DBPlaylistManager.Container) getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.fragment_playlist_details, null);
        final TextView textView = (TextView) (view.findViewById(R.id.playlist_details_name));
        textView.setText(playlistName);

        builder.setTitle("Edit Playlist");
        builder.setView(view);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                DBPlaylistManager playlistManager = container.getDBPlaylistManager();

                // TODO: Validate & sanitize the input
                playlistManager.edit(playlistPosition, textView.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                EditPlaylistFragment.this.getDialog().cancel();
            }
        });
        return builder.create();
    }
}
