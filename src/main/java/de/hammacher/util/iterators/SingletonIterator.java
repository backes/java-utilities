package de.hammacher.util.iterators;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class SingletonIterator<T> implements ListIterator<T> {

    private boolean atBeginning = true;
    private final T value;

    public SingletonIterator(final T value) {
        this(value, true);
    }

    public SingletonIterator(final T value, final boolean atBeginning) {
        this.value = value;
        this.atBeginning = atBeginning;
    }

    public boolean hasNext() {
        return this.atBeginning;
    }

    public T next() {
        if (!this.atBeginning)
            throw new NoSuchElementException();
        this.atBeginning = false;
        return this.value;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void add(final T e) {
        throw new UnsupportedOperationException();
    }

    public boolean hasPrevious() {
        return !this.atBeginning;
    }

    public int nextIndex() {
        return this.atBeginning ? 0 : 1;
    }

    public T previous() {
        if (this.atBeginning)
            throw new NoSuchElementException();
        this.atBeginning = true;
        return this.value;
    }

    public int previousIndex() {
        return this.atBeginning ? -1 : 0;
    }

    public void set(final T e) {
        throw new UnsupportedOperationException();
    }

}
