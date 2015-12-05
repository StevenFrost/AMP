package com.frost.steven.amp.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This generic class derives directly from ArrayList and provides listener
 * capabilities in order to be notified when the underlying arraylist changes
 * via add, remove or modification operations.
 *
 * @param <E> the contained type
 */
public class ListenableArrayList<E> extends ArrayList<E>
{
    List<OnCollectionChangedListener> m_listeners;

    public ListenableArrayList()
    {
        super();
        m_listeners = new ArrayList<>();
    }

    public ListenableArrayList(int capacity)
    {
        super(capacity);
        m_listeners = new ArrayList<>();
    }

    public ListenableArrayList(Collection<? extends E> collection)
    {
        super(collection);
        m_listeners = new ArrayList<>();
    }

    @Override
    public boolean add(E object)
    {
        boolean res = super.add(object);
        notifyListeners();
        return res;
    }

    @Override
    public void add(int index, E object)
    {
        super.add(index, object);
        notifyListeners();
    }

    @Override
    public boolean addAll(Collection<? extends E> collection)
    {
        boolean res = super.addAll(collection);
        notifyListeners();
        return res;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection)
    {
        boolean res = super.addAll(index, collection);
        notifyListeners();
        return res;
    }

    @Override
    public void clear()
    {
        super.clear();
        notifyListeners();
    }

    @Override
    public E remove(int index)
    {
        E res = super.remove(index);
        notifyListeners();
        return res;
    }

    @Override
    public boolean remove(Object object)
    {
        boolean res = super.remove(object);
        notifyListeners();
        return res;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex)
    {
        super.removeRange(fromIndex, toIndex);
        notifyListeners();
    }

    @Override
    public E set(int index, E object)
    {
        E res = super.set(index, object);
        notifyListeners();
        return res;
    }

    public void attachListener(OnCollectionChangedListener listener)
    {
        m_listeners.add(listener);
    }

    private void notifyListeners()
    {
        for (OnCollectionChangedListener listener : m_listeners)
        {
            listener.onPlaylistCollectionChanged(this);
        }
    }

    public interface OnCollectionChangedListener
    {
        void onPlaylistCollectionChanged(ListenableArrayList collection);
    }
}
