package uk.org.ngo.squeezer.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds a list of
 */
public class ServiceCallbackList<T extends ServiceCallback> implements Iterable<T> {
    private ServicePublisher publisher;
    private List<T> items = new ArrayList<T>();

    public ServiceCallbackList(ServicePublisher publisher) {
        this.publisher = publisher;
    }

    public int count() {
        return items.size();
    }

    public ServiceCallbackList<T> register(T item) {
        publisher.addClient(this, item);
        items.add(item);
        return this;
    }

    public ServiceCallbackList<T> unregister(T item) {
        publisher.removeClient(item);
        items.remove(item);
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    public interface ServicePublisher {
        void addClient(ServiceCallbackList callbackList, ServiceCallback item);
        void removeClient(ServiceCallback item);
    }
}
