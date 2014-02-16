package uk.org.ngo.squeezer.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds a list of
 */
public class ServiceCallbackList<T extends ServiceCallback> implements Iterable<T> {
    private List<T> items = new ArrayList<T>();

    public int count() {
        return items.size();
    }

    public ServiceCallbackList<T> register(T item) {
        items.add(item);
        return this;
    }

    public ServiceCallbackList<T> unregister(T item) {
        items.remove(item);
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}
