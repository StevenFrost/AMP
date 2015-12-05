package com.frost.steven.amp.ui;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.frost.steven.amp.helpers.BitmapResolver;

/**
 * Persistent fragment that holds application state that should ideally
 * remain through configuration changes (resized screen, orientation change,
 * etc.)
 */
public class StaticFragment extends Fragment
{
    private static final String FRAGMENT_ID = "com.frost.steven.amp.ui.StaticFragment";

    private ContentResolver m_contentResolver;
    private Resources       m_resources;

    private BitmapResolver m_bitmapResolver;

    public static StaticFragment getInstance(FragmentManager fragmentManager, ContentResolver contentResolver, Resources resources)
    {
        StaticFragment fragment = (StaticFragment)fragmentManager.findFragmentByTag(FRAGMENT_ID);
        if (fragment == null)
        {
            fragment = new StaticFragment();
            fragment.setContentResolver(contentResolver);
            fragment.setResources(resources);
            fragment.initialise();

            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(fragment, FRAGMENT_ID);
            transaction.commit();
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    private void initialise()
    {
        m_bitmapResolver = new BitmapResolver(m_resources, m_contentResolver);
    }

    public BitmapResolver getBitmapProvider()
    {
        return m_bitmapResolver;
    }

    private void setContentResolver(ContentResolver contentResolver)
    {
        m_contentResolver = contentResolver;
    }

    private void setResources(Resources resources)
    {
        m_resources = resources;
    }
}
