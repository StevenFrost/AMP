package com.frost.steven.amp.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.frost.steven.amp.service.MediaService;

public class MediaServiceActivity extends AppCompatActivity
{
    private MediaService m_mediaService;
    private boolean      m_serviceBound;

    public MediaServiceActivity()
    {
        m_serviceBound = false;
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        Intent intent = new Intent(this, MediaService.class);
        bindService(intent, m_connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        if (m_serviceBound)
        {
            unbindService(m_connection);
            m_serviceBound = false;
        }

        super.onStop();
    }

    protected void onMediaServiceConnected()
    {
        // Intentionally left (almost) blank.
    }

    protected void onMediaServiceDisconnected()
    {
        // Intentionally left (almost) blank.
    }

    public ServiceConnection getServiceConnection()
    {
        return m_connection;
    }

    public MediaService getMediaService()
    {
        return m_mediaService;
    }

    public boolean isMediaServiceBound()
    {
        return m_serviceBound;
    }

    private ServiceConnection m_connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MediaService.MediaBinder binder = (MediaService.MediaBinder)service;

            m_mediaService = binder.getService();
            m_serviceBound = true;

            onMediaServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            m_serviceBound = false;
            onMediaServiceDisconnected();
        }
    };
}
