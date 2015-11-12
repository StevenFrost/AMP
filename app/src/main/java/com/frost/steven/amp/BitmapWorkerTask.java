package com.frost.steven.amp;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class BitmapWorkerTask extends AsyncTask<AudioTrack, Void, Bitmap>
{
    private final ContentResolver          m_resolver;
    private final WeakReference<ImageView> m_weakRef;
    private AudioTrack                     m_data;
    private LruCache<Uri, Bitmap>          m_cache;

    public BitmapWorkerTask(ContentResolver resolver, ImageView imageView, LruCache<Uri, Bitmap> cache)
    {
        m_resolver = resolver;
        m_weakRef = new WeakReference<>(imageView);
        m_cache = cache;
    }

    /**
     * Loads a bitmap given one or more AudioTrack objects. See the `params`
     * notes for details on what AudioTrack objects are processed.
     *
     * @param params a number of AudioTrack objects. Only the first in the
     *               sequence will be loaded by this function, any further are
     *               ignored.
     * @return       the loaded bitmap in memory
     */
    @Override
    protected Bitmap doInBackground(AudioTrack... params)
    {
        m_data = params[0];
        return loadBitmap(100, 100);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap)
    {
        if (isCancelled())
        {
            bitmap = null;
        }

        if (bitmap != null)
        {
            final ImageView imageView = m_weakRef.get();
            final BitmapWorkerTask task = getTask(imageView);

            if (this == task && imageView != null)
            {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    Bitmap loadBitmap(int width, int height)
    {
        Bitmap bitmap = null;
        try
        {
            bitmap = MediaStore.Images.Media.getBitmap(m_resolver, m_data.CoverArt);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

            if (m_cache.get(m_data.CoverArt) == null)
            {
                m_cache.put(m_data.CoverArt, bitmap);
            }
        }
        catch (IOException | NullPointerException e)
        {
            e.printStackTrace();
        }

        return bitmap;
    }

    private static BitmapWorkerTask getTask(ImageView imageView)
    {
        if (imageView != null)
        {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable)
            {
                final AsyncDrawable asyncDrawable = (AsyncDrawable)drawable;
                return asyncDrawable.getTask();
            }
        }
        return null;
    }

    public static boolean cancelOutstandingWork(AudioTrack data, ImageView imageView)
    {
        final BitmapWorkerTask bitmapWorkerTask = getTask(imageView);

        if (bitmapWorkerTask != null)
        {
            final AudioTrack bitmapData = bitmapWorkerTask.m_data;

            if (bitmapData != data)
            {
                bitmapWorkerTask.cancel(true);
            }
            else
            {
                return false;
            }
        }
        return true;
    }
}