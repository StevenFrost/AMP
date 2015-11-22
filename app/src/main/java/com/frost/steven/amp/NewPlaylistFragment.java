package com.frost.steven.amp;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Toast;

public class NewPlaylistFragment extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setTitle("New Playlist")
            .setView(inflater.inflate(R.layout.fragment_new_playlist, null))
            .setPositiveButton("Create", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    LibraryActivity activity = (LibraryActivity)getActivity();
                    activity.getPlaylists().add(new DBPlaylist(new Long(1337), "Testing!", new Long(0)));

                    Toast.makeText(getActivity(), "New playlist created!", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    NewPlaylistFragment.this.getDialog().cancel();
                }
            });
        return builder.create();
    }
}
