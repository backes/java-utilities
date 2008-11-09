package de.hammacher.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntArrayIterator implements Iterator<Integer> {

    private final int[] data;
    private int index;

    public IntArrayIterator(final int[] data) {
        this.data = data;
        this.index = 0;
    }

    public boolean hasNext() {
        return this.index < this.data.length;
    }

    public Integer next() {
        if (!hasNext())
            throw new NoSuchElementException();
        return this.data[this.index++];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
