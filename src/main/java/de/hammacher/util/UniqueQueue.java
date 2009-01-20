package de.hammacher.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * A Queue in which every element is only inserted once.
 *
 * A boolean flag controls whether elements can be reinserted
 * after they have been taken out of the queue.
 *
 * @author Clemens Hammacher
 */
public class UniqueQueue<E> extends ArrayQueue<E> {

    private final Set<E> seen;
    private final boolean allowReinsertion;

    public UniqueQueue() {
    	this(false);
    }

    public UniqueQueue(boolean allowReinsertion) {
        super();
        this.allowReinsertion = allowReinsertion;
        this.seen = new HashSet<E>();
    }

    public UniqueQueue(final Collection<? extends E> c, boolean allowReinsertion) {
        super(c);
        this.allowReinsertion = allowReinsertion;
        this.seen = new HashSet<E>(c);
    }

    public UniqueQueue(final int initialCapacity, boolean allowReinsertion) {
        super(initialCapacity);
        this.allowReinsertion = allowReinsertion;
        this.seen = new HashSet<E>(initialCapacity);
    }

    /**
     * Returns a set of all elements that are not allowed to be reinserted at the moment.
     * If reinsertion is not allowed, this set contains all elements that have ever been
     * inserted into the queue, otherwise it contains exactly the elements of this queue.
     * @return a set of all elements that are not allowed to be reinserted
     */
    public Set<E> getSeenElements() {
        return Collections.unmodifiableSet(this.seen);
    }

    @Override
    public void addFirst(final E e) {
        if (this.seen.add(e))
            super.addFirst(e);
    }

    @Override
    public void addLast(final E e) {
        if (this.seen.add(e))
            super.addLast(e);
    }

    @Override
    public boolean add(final E e) {
        if (!this.seen.add(e))
            return false;
        super.addLast(e);
        return true;
    }

    @Override
    public boolean offer(final E e) {
        if (!this.seen.add(e))
            return false;
        super.addLast(e);
        return true;
    }

    public boolean offerFirst(final E e) {
        if (!this.seen.add(e))
            return false;
        super.addFirst(e);
        return true;
    }

    public boolean offerLast(final E e) {
        if (!this.seen.add(e))
            return false;
        super.addLast(e);
        return true;
    }

    /**
     * Resets the set of seen elements.
     * After this operation, every elements is again accepted exactly once.
     */
    public void clearSeen() {
        this.seen.clear();
    }

	@Override
	public void clear() {
		super.clear();
		this.seen.clear();
	}

	@Override
	public E pollFirst() {
		E e = super.pollFirst();
		if (this.allowReinsertion)
			this.seen.remove(e);
		return e;
	}

	@Override
	public E pollLast() {
		E e = super.pollLast();
		if (this.allowReinsertion)
			this.seen.remove(e);
		return e;
	}

	@Override
	public boolean remove(Object o) {
		if (!super.remove(o))
			return false;

		if (this.allowReinsertion)
			this.seen.remove(o);
		return true;
	}

	@Override
	public E removeFirst() {
		E e = super.removeFirst();
		if (this.allowReinsertion)
			this.seen.remove(e);
		return e;
	}

	@Override
	public E removeLast() {
		E e = super.removeLast();
		if (this.allowReinsertion)
			this.seen.remove(e);
		return e;
	}

}
