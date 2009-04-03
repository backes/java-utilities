package de.hammacher.util.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * A TreeSet where the entries are cached in an array, which is traversed in the
 * iterator. So adds and removed during traversal are not reflected in the iterator.
 *
 * @author Clemens Hammacher
 *
 * @param <E> the element type of this set
 */
public class CachedTreeSet<E> extends TreeSet<E> {


	public class Itr<T> implements Iterator<E> {

		private final E[] entries;
		private int cursor = 0;

		public Itr(E[] entries) {
			this.entries = entries;
		}

		public boolean hasNext() {
			return this.cursor < this.entries.length;
		}

		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return this.entries[this.cursor++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private static final long serialVersionUID = 6022268479734686946L;

	private E[] cachedEntries = null;

	public CachedTreeSet() {
		super();
	}

	public CachedTreeSet(Collection<? extends E> c) {
		super(c);
	}

	public CachedTreeSet(Comparator<? super E> c) {
		super(c);
	}

	public CachedTreeSet(SortedSet<E> s) {
		super(s);
	}

	@SuppressWarnings("unchecked")
	private E[] newArray(int numElements) {
		return (E[])new Object[numElements];
	}

	@Override
	public Iterator<E> iterator() {
		if (this.cachedEntries == null) {
			// we cannot use toArray() here, because this would again call this method...
			int size = size();
			this.cachedEntries = newArray(size);
			Iterator<E> it = super.iterator();
			for (int i = 0; i < size; ++i) {
				if (!it.hasNext())
					throw new ConcurrentModificationException();
				this.cachedEntries[i] = it.next();
			}
			if (it.hasNext())
				throw new ConcurrentModificationException();
		}
		return new Itr<E>(this.cachedEntries);
	}

	@Override
	public boolean add(E o) {
		if (super.add(o)) {
			this.cachedEntries = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		if (super.addAll(c)) {
			this.cachedEntries = null;
			return true;
		}
		return false;
	}

	@Override
	public void clear() {
		super.clear();
		this.cachedEntries = null;
	}

	@Override
	public boolean remove(Object o) {
		if (super.remove(o)) {
			this.cachedEntries = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (super.removeAll(c)) {
			this.cachedEntries = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (super.retainAll(c)) {
			this.cachedEntries = null;
			return true;
		}
		return false;
	}

}
