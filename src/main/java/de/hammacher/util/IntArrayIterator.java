package de.hammacher.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class IntArrayIterator implements ListIterator<Integer> {

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

    public void add(final Integer o) {
        throw new UnsupportedOperationException();
    }

    public boolean hasPrevious() {
        return this.index != 0;
    }

    public int nextIndex() {
        return this.index;
    }

    public Integer previous() {
        if (!hasPrevious())
            throw new NoSuchElementException();
        return this.data[--this.index];
    }

    public int previousIndex() {
        return this.index - 1;
    }

    public void set(final Integer o) {
        throw new UnsupportedOperationException();
    }

}
