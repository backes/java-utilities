package de.hammacher.util.iterators;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class EmptyIterator<T> implements ListIterator<T> {

	private static final EmptyIterator<?> instance =
		new EmptyIterator<Object>();

	private EmptyIterator() {
	    // not instantiable
	}

	@SuppressWarnings("unchecked")
	public static <T> EmptyIterator<T> getInstance() {
		return (EmptyIterator<T>) instance;
	}

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new IllegalStateException();
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public T previous() {
        throw new NoSuchElementException();
    }

    @Override
    public int nextIndex() {
        return 0;
    }

    @Override
    public int previousIndex() {
        return -1;
    }

    @Override
    public void set(T e) {
        throw new IllegalStateException();
    }

    @Override
    public void add(T e) {
        throw new UnsupportedOperationException("Cannot add elements to an " +
            EmptyIterator.class.getSimpleName());
    }

}
