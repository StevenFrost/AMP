package com.frost.steven.amp;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BitmapProvider
{
    private static final int m_maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
    private static final int m_cacheSize = m_maxMemory / 4;

    private Map<Uri, Worker>      m_bitmapTasks;
    private LruCache<Uri, Bitmap> m_cache;      // TODO: Support bitmaps of different sizes

    private Resources       m_resources;
    private ContentResolver m_resolver;

    public BitmapProvider(Resources resources, ContentResolver resolver)
    {
        m_resources = resources;
        m_resolver = resolver;

        m_bitmapTasks = new HashMap<>();
        m_cache = new LruCache<Uri, Bitmap>(m_cacheSize)
        {
            @Override
            protected int sizeOf(Uri key, Bitmap bitmap)
            {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public synchronized void makeRequest(ImageView imageView, Uri uri)
    {
        if (cancelOutstandingWork(imageView, uri))
        {
            // Check if the bitmap is in the cache, we can early out in that case
            Bitmap bitmap = m_cache.get(uri);
            if (bitmap != null)
            {
                imageView.setImageBitmap(bitmap);
                return;
            }

            // Now check if there are any outstanding workers processing the
            // requested bitmap
            if (m_bitmapTasks.containsKey(uri))
            {
                Worker worker = m_bitmapTasks.get(uri);
                worker.addViewReference(imageView);

                imageView.setImageDrawable(new AsyncDrawable(m_resources, null, worker));
                return;
            }

            // If the bitmap isn't in the cache and isn't in progress we should
            // start a new task to load it into memory
            Worker worker = new Worker();
            worker.addViewReference(imageView);
            imageView.setImageDrawable(new AsyncDrawable(m_resources, null, worker));
            worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
        }
    }

    private Worker getTask(ImageView view)
    {
        if (view != null)
        {
            Drawable drawable = view.getDrawable();
            if (drawable instanceof AsyncDrawable)
            {
                return ((AsyncDrawable)drawable).getWorker();
            }
        }
        return null;
    }

    private boolean cancelOutstandingWork(ImageView view, Uri uri)
    {
        Worker worker = getTask(view);

        if (worker != null)
        {
            Uri workerUri = worker.m_uri;

            if (workerUri == uri)
            {
                return false;
            }
            worker.removeViewReference(view);
        }
        return true;
    }

    private synchronized void removeFromTaskMap(Uri uri)
    {
        m_bitmapTasks.remove(uri);
    }

    private synchronized void addToCache(Uri uri, Bitmap bitmap)
    {
        m_cache.put(uri, bitmap);
    }

    private class AsyncDrawable extends BitmapDrawable
    {
        private WeakReference<Worker> m_weakRef;

        AsyncDrawable(Resources resources, Bitmap bitmap, Worker worker)
        {
            super(resources, bitmap);
            m_weakRef = new WeakReference<>(worker);
        }

        public Worker getWorker()
        {
            return m_weakRef.get();
        }
    }

    private class Worker extends AsyncTask<Uri, Void, Bitmap>
    {
        private List<WeakReference<ImageView>> m_views;

        public  Uri     m_uri;
        private Bitmap  m_bitmap = null;

        public Worker()
        {
            m_views = new ArrayList<>();
        }

        @Override
        protected Bitmap doInBackground(Uri... params)
        {
            m_uri = params[0];
            try
            {
                m_bitmap = MediaStore.Images.Media.getBitmap(m_resolver, m_uri);
                m_bitmap = Bitmap.createScaledBitmap(m_bitmap, 100, 100, true);

                addToCache(m_uri, m_bitmap);
            }
            catch (IOException | NullPointerException e)
            {
                e.printStackTrace();
            }
            return m_bitmap;
        }

        @Override
        protected synchronized void onPostExecute(Bitmap result)
        {
            for (WeakReference<ImageView> view : m_views)
            {
                ImageView imageView = view.get();
                Worker worker = getTask(imageView);

                if (imageView == null || this != worker) { continue; }

                imageView.setImageBitmap(result);
            }

            removeFromTaskMap(m_uri);
        }

        synchronized void addViewReference(ImageView imageView)
        {
            if (m_bitmap != null)
            {
                imageView.setImageBitmap(m_bitmap);
                return;
            }
            m_views.add(new WeakReference<>(imageView));
        }

        synchronized void removeViewReference(ImageView view)
        {
            List<WeakReference<ImageView>> toRemove = new ArrayList<>();

            for (WeakReference<ImageView> iv : m_views)
            {
                ImageView imageView = iv.get();
                if (imageView != null && imageView == view)
                {
                    toRemove.add(iv);
                }
            }

            // Remove the marked references
            for (WeakReference<ImageView> imageView : toRemove)
            {
                m_views.remove(imageView);
            }

            // Cancel the task if we removed the last references
            if (m_views.size() == 0)
            {
                this.cancel(true);
            }
        }
    }
}
