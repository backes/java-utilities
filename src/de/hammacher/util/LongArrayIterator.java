package de.hammacher.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LongArrayIterator implements Iterator<Long> {

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

}
