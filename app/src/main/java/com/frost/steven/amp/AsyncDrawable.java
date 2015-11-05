package com.frost.steven.amp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.lang.ref.WeakReference;

class AsyncDrawable extends BitmapDrawable
{
    private final WeakReference<BitmapWorkerTask> m_weakRef;

    public AsyncDrawable(Resources resources, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask)
    {
        super(resources, bitmap);
        m_weakRef = new WeakReference<>(bitmapWorkerTask);
    }

    public BitmapWorkerTask getTask()
    {
        return m_weakRef.get();
    }
}