package com.frost.steven.amp.helpers;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.LruCache;
import android.widget.ImageView;

import com.frost.steven.amp.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is a helper to aid bitmap resolution from a Uri. The public
 * interface is extremely simple - one request needs to be made for an
 * ImageView object and the bitmap will be resolved asynchronously.
 *
 * This class contains an LRU cache and management of any async tasks which
 * means that bitmaps will only be loaded once (even if multiple requests are
 * made for the same Uri simultaneously) and all requests will be filled
 * once the bitmap has finished loading.
 */
public class BitmapResolver
{
    private static final int m_maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
    private static final int m_cacheSize = m_maxMemory / 4;

    private Bitmap m_placeholderBitmap;

    private Map<String, Worker>      m_bitmapTasks;
    private LruCache<String, Bitmap> m_cache;

    private Resources       m_resources;
    private ContentResolver m_resolver;

    /**
     * Constructor
     *
     * @param resources The application resources object
     * @param resolver  The content resolver, used to resolve bitmaps
     */
    public BitmapResolver(Resources resources, ContentResolver resolver)
    {
        m_resources = resources;
        m_resolver = resolver;

        m_bitmapTasks = new HashMap<>();
        m_cache = new LruCache<String, Bitmap>(m_cacheSize)
        {
            @Override
            protected int sizeOf(String key, Bitmap bitmap)
            {
                return bitmap.getByteCount() / 1024;
            }
        };

        // Placeholder bitmap
        m_placeholderBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_album_placeholder);
    }

    /**
     * Requests that the given ImageView has the bitmap at the given uri and
     * size loaded at some time in the future.
     *
     * If the ImageView object already has outstanding tasks against it they
     * will be canceled in favour of the new task (provided they are different)
     * otherwise the cache will be checked, the list of existing requests will
     * be checked and finally if the bitmap was not being retrieved in either
     * of those then a new task will begin to load the bitmap into memory.
     *
     * @param imageView The ImageView to set the bitmap for
     * @param uri       The image URI to load
     * @param size      The size of the image
     */
    public synchronized void makeRequest(ImageView imageView, Uri uri, int size)
    {
        if (uri == null)
        {
            imageView.setImageBitmap(m_placeholderBitmap);
            return;
        }

        String key = uri.toString() + size;
        if (cancelOutstandingWork(imageView, key))
        {
            // Check if the bitmap is in the cache, we can early out in that case
            Bitmap bitmap = m_cache.get(key);
            if (bitmap != null)
            {
                imageView.setImageBitmap(bitmap);
                return;
            }

            // Now check if there are any outstanding workers processing the
            // requested bitmap
            if (m_bitmapTasks.containsKey(key))
            {
                Worker worker = m_bitmapTasks.get(key);
                worker.addViewReference(imageView);

                imageView.setImageDrawable(new AsyncDrawable(m_resources, null, worker));
                return;
            }

            // If the bitmap isn't in the cache and isn't in progress we should
            // start a new task to load it into memory
            Worker worker = new Worker(uri, size, key);
            worker.addViewReference(imageView);
            imageView.setImageDrawable(new AsyncDrawable(m_resources, null, worker));
            worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * Gets the worker task for the given ImageView object
     *
     * @param view The ImageView for which to retrieve the task
     *
     * @return The worker task assigned to the ImageView
     */
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

    /**
     * Cancels any outstanding tasks given that the keys are different.
     *
     * @param view ImageView to check cancellation for
     * @param key  The key of the new task
     *
     * @return True if the task was cancelled or there was no active task,
     *         False otherwise.
     */
    private boolean cancelOutstandingWork(ImageView view, String key)
    {
        Worker worker = getTask(view);

        if (worker != null)
        {
            String workerKey = worker.m_key;

            if (workerKey.equals(key))
            {
                return false;
            }
            worker.removeViewReference(view);
        }
        return true;
    }

    /**
     * Removes a bitmap task from the map given the key
     *
     * @param key key of the task to remove
     */
    private synchronized void removeFromTaskMap(String key)
    {
        m_bitmapTasks.remove(key);
    }

    /**
     * Adds a new bitmap to the LRU cache
     *
     * @param key    Bitmap key
     * @param bitmap Bitmap object
     */
    private synchronized void addToCache(String key, Bitmap bitmap)
    {
        m_cache.put(key, bitmap);
    }

    /**
     * This class holds a weak reference to the worker task that is loading
     * the bitmap for the ImageView object. It is used to check for
     * cancellation as the reference to the worker needs to be retrieved
     * from an ImageView object.
     */
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

    /**
     * Async task representing the bitmap retrieval process. Additional listeners
     * can be attached to this class such that we can update multiple ImageView
     * objects after loading a shared bitmap rather than spawning multiple tasks
     * for identical operations.
     */
    private class Worker extends AsyncTask<Void, Void, Bitmap>
    {
        private List<WeakReference<ImageView>> m_views;

        public Uri     m_uri;
        public int     m_size;
        public String  m_key;

        private Bitmap m_bitmap = null;

        /**
         * Constructor
         *
         * @param uri  The URI of the bitmap to load
         * @param size The size of the bitmap
         * @param key  The cache key for the bitmap
         */
        public Worker(Uri uri, int size, String key)
        {
            m_uri   = uri;
            m_size  = size;
            m_key   = key;
            m_views = new ArrayList<>();
        }

        @Override
        protected Bitmap doInBackground(Void... params)
        {
            try
            {
                m_bitmap = MediaStore.Images.Media.getBitmap(m_resolver, m_uri);
                m_bitmap = Bitmap.createScaledBitmap(m_bitmap, m_size, m_size, true);

                addToCache(m_key, m_bitmap);
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

            removeFromTaskMap(m_key);
        }

        /**
         * Adds an ImageView to the list of objects to be notified on task
         * completion. If the task is already complete (because it hasn't been
         * removed from the task list yet) the ImageView will be updated
         * immediately.
         *
         * @param imageView The ImageView to bind
         */
        synchronized void addViewReference(ImageView imageView)
        {
            if (m_bitmap != null)
            {
                imageView.setImageBitmap(m_bitmap);
                return;
            }
            m_views.add(new WeakReference<>(imageView));
        }

        /**
         * Removes an ImageView reference from the list of listeners on this
         * task. If the last listener is removed the task is cancelled.
         *
         * @param view The ImageView reference to remove
         */
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
