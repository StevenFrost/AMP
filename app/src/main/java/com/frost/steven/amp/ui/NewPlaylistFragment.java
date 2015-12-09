package com.frost.steven.amp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.frost.steven.amp.R;
import com.frost.steven.amp.helpers.DBPlaylistManager;
import com.frost.steven.amp.ui.listeners.PlaylistDetailsPositiveOnClickListener;

public class NewPlaylistFragment extends DialogFragment
{
    private long m_trackId;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Bundle bundle = getArguments();
        m_trackId = bundle.getLong("trackID");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final Activity activity = getActivity();
        final DBPlaylistManager.Container container = (DBPlaylistManager.Container)activity;
        final DBPlaylistManager m_playlistManager = container.getDBPlaylistManager();

        final Resources resources = getResources();
        final View view = inflater.inflate(R.layout.fragment_playlist_details, null);

        builder.setTitle(resources.getString(R.string.playlist_new));
        builder.setView(view);
        builder.setPositiveButton(resources.getString(R.string.playlist_details_create), new PositiveDialogClickListener());
        builder.setNegativeButton(resources.getString(R.string.playlist_details_cancel), new NegativeDialogClickListener());

        final AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new PositiveClickListener(activity, view, dialog, m_playlistManager));

        return dialog;
    }

    private class PositiveDialogClickListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialog, int id)
        {
            // Intentionally left (almost) blank.
        }
    }

    private class NegativeDialogClickListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick(DialogInterface dialogInterface, int id)
        {
            Dialog dialog = NewPlaylistFragment.this.getDialog();
            dialog.cancel();
        }
    }

    private class PositiveClickListener extends PlaylistDetailsPositiveOnClickListener
    {
        public PositiveClickListener(Context context, View view, Dialog dialog, DBPlaylistManager playlistManager)
        {
            super(context, view, getResources(), dialog, playlistManager, m_trackId);
        }

        @Override
        protected void onSuccessfulValidation(long trackID, String playlistName, DBPlaylistManager playlistManager)
        {
            playlistManager.create(playlistName, trackID);
        }
    }
}
