package de.hammacher.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class EmptyIterator<T> implements Iterator<T> {

	private static final EmptyIterator<Object> instance =
		new EmptyIterator<Object>();

	@SuppressWarnings("unchecked")
	public static <T> EmptyIterator<T> getInstance() {
		return (EmptyIterator<T>) instance;
	}

    public boolean hasNext() {
        return false;
    }

    public T next() {
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new IllegalStateException();
    }

}
