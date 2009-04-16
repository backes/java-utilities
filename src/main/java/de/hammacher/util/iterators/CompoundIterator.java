package de.hammacher.util.iterators;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CompoundIterator<T> implements Iterator<T> {

	private final Iterator<? extends T>[] iterators;
	private int nextIteratorPos = 0;
	private Iterator<? extends T> currentIterator = null;

	public CompoundIterator(Iterator<? extends T>... iterators) {
		this.iterators = iterators;
	}

	public CompoundIterator(Collection<Iterator<? extends T>> iterators) {
		this.iterators = iterators.toArray(newIteratorArray(iterators.size()));
	}

	public CompoundIterator(Iterable<? extends T>... iterables) {
		int numIterators = iterables.length;
		this.iterators = newIteratorArray(numIterators);
		for (int i = 0; i < numIterators; ++i)
			this.iterators[i] = iterables[i].iterator();
	}

	@SuppressWarnings("unchecked")
	private Iterator<? extends T>[] newIteratorArray(int size) {
		return (Iterator<? extends T>[])new Iterator<?>[size];
	}

	public boolean hasNext() {
		while (this.currentIterator == null || !this.currentIterator.hasNext()) {
			if (this.nextIteratorPos >= this.iterators.length)
				return false;
			this.currentIterator = this.iterators[this.nextIteratorPos++];
		}
		return true;
	}

	public T next() {
		if (!hasNext())
			throw new NoSuchElementException();
		return this.currentIterator.next();
	}

	public void remove() {
		if (this.currentIterator == null)
			throw new NoSuchElementException();
		this.currentIterator.remove();
	}

}
