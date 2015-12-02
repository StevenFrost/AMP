package com.frost.steven.amp;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class EditPlaylistFragment extends DialogFragment
{
    private int    m_playlistPosition;
    private String m_playlistName;

    private DBPlaylistManager m_playlistManager;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        m_playlistPosition = bundle.getInt("playlistPosition");
        m_playlistName = bundle.getString("playlistName");

        final DBPlaylistManager.Container container = (DBPlaylistManager.Container)getActivity();
        m_playlistManager = container.getDBPlaylistManager();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_playlist_details, null);
        final TextView textView = (TextView)(view.findViewById(R.id.playlist_details_name));

        textView.setText(m_playlistName);
        textView.setFilters(new InputFilter[] {
            new InputFilter()
            {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
                {
                    if (source.equals("") || source.toString().matches("[a-zA-Z 0-9 _-]+"))
                    {
                        return source;
                    }
                    return "";
                }
            }
        });

        builder.setTitle("Edit Playlist");
        builder.setView(view);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener()
        {
            @Override public void onClick(DialogInterface dialog, int id)
            {
                final String playlistName = textView.getText().toString();
                m_playlistManager.edit(m_playlistPosition, playlistName);
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
