package de.hammacher.util.iterators;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class LongArrayIterator implements ListIterator<Long> {

    private final long[] data;
    private int index;

    public LongArrayIterator(final long[] data) {
        this.data = data;
        this.index = 0;
    }

    public boolean hasNext() {
        return this.index < this.data.length;
    }

    public Long next() {
        if (!hasNext())
            throw new NoSuchElementException();
        return this.data[this.index++];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void add(final Long o) {
        throw new UnsupportedOperationException();
    }

    public boolean hasPrevious() {
        return this.index != 0;
    }

    public int nextIndex() {
        return this.index;
    }

    public Long previous() {
        if (!hasPrevious())
            throw new NoSuchElementException();
        return this.data[--this.index];
    }

    public int previousIndex() {
        return this.index - 1;
    }

    public void set(final Long o) {
        throw new UnsupportedOperationException();
    }

}
