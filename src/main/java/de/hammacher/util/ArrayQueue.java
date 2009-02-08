package de.hammacher.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * A Queue that internally stores the elements in an array (like ArrayDeque,
 * but java 1.5 compatible).
 */
public class ArrayQueue<E> extends AbstractCollection<E> implements Queue<E>,
        Cloneable {

    /**
     * The array in which the elements of the queue are stored. The capacity of
     * the queue is the length of this array, which is always a power of two.
     * The array is never allowed to become full, except transiently within an
     * addX method where it is resized (see doubleCapacity) immediately upon
     * becoming full, thus avoiding head and tail wrapping around to equal each
     * other. We also guarantee that all array cells not holding queue elements
     * are always null.
     */
    protected E[] elements;

    /**
     * The index of the element at the head of the queue (which is the element
     * that would be removed by remove() or pop()); or an arbitrary number equal
     * to tail if the queue is empty.
     */
    protected int head;

    /**
     * The index at which the next element would be added to the tail of the
     * queue (via addLast(E), add(E), or push(E)).
     */
    protected int tail;

    /**
     * The minimum capacity that we'll use for a newly created queue. Must be a
     * power of 2.
     */
    private static final int MIN_INITIAL_CAPACITY = 8;

    // ******  Array allocation and resizing utilities ******

    /**
     * Allocate empty array to hold the given number of elements.
     *
     * @param numElements
     *            the number of elements to hold
     */
    @SuppressWarnings("unchecked")
    private void allocateElements(final int numElements) {
        int initialCapacity = MIN_INITIAL_CAPACITY;
        // Find the best power of two to hold elements.
        // Tests "<=" because arrays aren't kept full.
        if (numElements >= initialCapacity) {
            initialCapacity = numElements;
            initialCapacity |= (initialCapacity >>> 1);
            initialCapacity |= (initialCapacity >>> 2);
            initialCapacity |= (initialCapacity >>> 4);
            initialCapacity |= (initialCapacity >>> 8);
            initialCapacity |= (initialCapacity >>> 16);
            initialCapacity++;

            if (initialCapacity < 0) // Too many elements, must back off
                initialCapacity >>>= 1;// Good luck allocating 2 ^ 30 elements
        }
        this.elements = (E[]) new Object[initialCapacity];
    }

    /**
     * Double the capacity of this queue. Call only when full, i.e., when head
     * and tail have wrapped around to become equal.
     */
    @SuppressWarnings("unchecked")
    private void doubleCapacity() {
        assert this.head == this.tail;
        final int p = this.head;
        final int n = this.elements.length;
        final int r = n - p; // number of elements to the right of p
        final int newCapacity = n << 1;
        if (newCapacity < 0)
            throw new IllegalStateException("Sorry, queue too big");
        final Object[] a = new Object[newCapacity];
        System.arraycopy(this.elements, p, a, 0, r);
        System.arraycopy(this.elements, 0, a, r, p);
        this.elements = (E[]) a;
        this.head = 0;
        this.tail = n;
    }

    /**
     * Copies the elements from our element array into the specified array, in
     * order (from first to last element in the queue). It is assumed that the
     * array is large enough to hold all elements in the queue.
     *
     * @return its argument
     */
    private <T> T[] copyElements(final T[] a) {
        if (this.head < this.tail) {
            System.arraycopy(this.elements, this.head, a, 0, size());
        } else if (this.head > this.tail) {
            final int headPortionLen = this.elements.length - this.head;
            System.arraycopy(this.elements, this.head, a, 0, headPortionLen);
            System.arraycopy(this.elements, 0, a, headPortionLen, this.tail);
        }
        return a;
    }

    /**
     * Constructs an empty array queue with an initial capacity sufficient to
     * hold 16 elements.
     */
    @SuppressWarnings("unchecked")
    public ArrayQueue() {
        this.elements = (E[]) new Object[16];
    }

    /**
     * Constructs an empty array queue with an initial capacity sufficient to
     * hold the specified number of elements.
     *
     * @param numElements
     *            lower bound on initial capacity of the queue
     */
    public ArrayQueue(final int numElements) {
        allocateElements(numElements);
    }

    /**
     * Constructs a queue containing the elements of the specified collection,
     * in the order they are returned by the collection's iterator. (The first
     * element returned by the collection's iterator becomes the first element,
     * or <i>front</i> of the queue.)
     *
     * @param c
     *            the collection whose elements are to be placed into the queue
     * @throws NullPointerException
     *             if the specified collection is null
     */
    public ArrayQueue(final Collection<? extends E> c) {
        allocateElements(c.size());
        this.elements = c.toArray(this.elements);
        this.tail = c.size();
    }

    // The main insertion and extraction methods are addFirst,
    // addLast, pollFirst, pollLast. The other methods are defined in
    // terms of these.

    /**
     * Inserts the specified element at the front of this queue.
     *
     * @param e
     *            the element to add
     * @throws NullPointerException
     *             if the specified element is null
     */
    public void addFirst(final E e) {
        if (e == null)
            throw new NullPointerException();
        this.elements[this.head = (this.head - 1) & (this.elements.length - 1)] = e;
        if (this.head == this.tail)
            doubleCapacity();
    }

    /**
     * Inserts the specified element at the end of this queue.
     *
     * <p>
     * This method is equivalent to {@link #add}.
     *
     * @param e
     *            the element to add
     * @throws NullPointerException
     *             if the specified element is null
     */
    public void addLast(final E e) {
        if (e == null)
            throw new NullPointerException();
        this.elements[this.tail] = e;
        if ((this.tail = (this.tail + 1) & (this.elements.length - 1)) == this.head)
            doubleCapacity();
    }

    /**
     * @see #poll()
     * @see #pollFirst()
     * @throws NoSuchElementException
     *             if the queue is empty
     */
    public E removeFirst() {
        final E x = pollFirst();
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    /**
     * @see #pollLast()
     * @throws NoSuchElementException
     *             if the queue is empty
     */
    public E removeLast() {
        final E x = pollLast();
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    public E pollFirst() {
        final int h = this.head;
        final E result = this.elements[h]; // Element is null if queue empty
        if (result == null)
            return null;
        this.elements[h] = null; // Must null out slot
        this.head = (h + 1) & (this.elements.length - 1);
        return result;
    }

    public E pollLast() {
        final int t = (this.tail - 1) & (this.elements.length - 1);
        final E result = this.elements[t];
        if (result == null)
            return null;
        this.elements[t] = null;
        this.tail = t;
        return result;
    }

    /**
     * @throws NoSuchElementException
     *             {@inheritDoc}
     */
    public E getFirst() {
        final E x = this.elements[this.head];
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    /**
     * @throws NoSuchElementException
     *             {@inheritDoc}
     */
    public E getLast() {
        final E x = this.elements[(this.tail - 1) & (this.elements.length - 1)];
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    public E peekFirst() {
        return this.elements[this.head]; // elements[head] is null if queue empty
    }

    public E peekLast() {
        return this.elements[(this.tail - 1) & (this.elements.length - 1)];
    }

    // *** Queue methods ***

    /**
     * Inserts the specified element at the end of this queue.
     *
     * <p>
     * This method is equivalent to {@link #addLast}.
     *
     * @param e
     *            the element to add
     * @return <code>true</code> (as specified by {@link Collection#add})
     * @throws NullPointerException
     *             if the specified element is null
     */
    @Override
    public boolean add(final E e) {
        addLast(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this queue.
     *
     * @param e
     *            the element to add
     * @return <code>true</code> (as specified by {@link Queue#offer})
     * @throws NullPointerException
     *             if the specified element is null
     */
    public boolean offer(final E e) {
        addLast(e);
        return true;
    }

    /**
     * Retrieves and removes the head of the queue represented by this queue.
     *
     * This method differs from {@link #poll poll} only in that it throws an
     * exception if this queue is empty.
     *
     * <p>
     * This method is equivalent to {@link #removeFirst}.
     *
     * @return the head of the queue represented by this queue
     * @throws NoSuchElementException
     *             {@inheritDoc}
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * Retrieves and removes the head of the queue represented by this queue (in
     * other words, the first element of this queue), or returns <code>null</code>
     * if this queue is empty.
     *
     * <p>
     * This method is equivalent to {@link #pollFirst}.
     *
     * @return the head of the queue represented by this queue, or <code>null</code>
     *         if this queue is empty
     */
    public E poll() {
        return pollFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by this
     * queue. This method differs from {@link #peek peek} only in that it throws
     * an exception if this queue is empty.
     *
     * <p>
     * This method is equivalent to {@link #getFirst}.
     *
     * @return the head of the queue represented by this queue
     * @throws NoSuchElementException
     *             {@inheritDoc}
     */
    public E element() {
        return getFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by this
     * queue, or returns <code>null</code> if this queue is empty.
     *
     * <p>
     * This method is equivalent to {@link #peekFirst}.
     *
     * @return the head of the queue represented by this queue, or <code>null</code>
     *         if this queue is empty
     */
    public E peek() {
        return peekFirst();
    }

    // *** Stack methods ***

    /**
     * Pushes an element onto the stack represented by this queue. In other
     * words, inserts the element at the front of this queue.
     *
     * <p>
     * This method is equivalent to {@link #addFirst}.
     *
     * @param e
     *            the element to push
     * @throws NullPointerException
     *             if the specified element is null
     */
    public void push(final E e) {
        addFirst(e);
    }

    /**
     * Pops an element from the stack represented by this queue. In other words,
     * removes and returns the first element of this queue.
     *
     * <p>
     * This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this queue (which is the top of the
     *         stack represented by this queue)
     * @throws NoSuchElementException
     *             {@inheritDoc}
     */
    public E pop() {
        return removeFirst();
    }

    private void checkInvariants() {
        assert this.elements[this.tail] == null;
        assert this.head == this.tail ? this.elements[this.head] == null
                           : (this.elements[this.head] != null && this.elements[(this.tail - 1)
                                   & (this.elements.length - 1)] != null);
        assert this.elements[(this.head - 1) & (this.elements.length - 1)] == null;
    }

    /**
     * Removes the element at the specified position in the elements array,
     * adjusting head and tail as necessary. This can result in motion of
     * elements backwards or forwards in the array.
     *
     * <p>
     * This method is called delete rather than remove to emphasize that its
     * semantics differ from those of {@link List#remove(int)}.
     *
     * @return true if elements moved backwards
     */
    protected boolean delete(final int i) {
        checkInvariants();
        final E[] elements0 = this.elements;
        final int mask = elements0.length - 1;
        final int h = this.head;
        final int t = this.tail;
        final int front = (i - h) & mask;
        final int back = (t - i) & mask;

        // Invariant: head <= i < tail mod circularity
        if (front >= ((t - h) & mask))
            throw new ConcurrentModificationException();

        // Optimize for least element motion
        if (front < back) {
            if (h <= i) {
                System.arraycopy(elements0, h, elements0, h + 1, front);
            } else { // Wrap around
                System.arraycopy(elements0, 0, elements0, 1, i);
                elements0[0] = elements0[mask];
                System.arraycopy(elements0, h, elements0, h + 1, mask - h);
            }
            elements0[h] = null;
            this.head = (h + 1) & mask;
            return false;
        }
        // else:
        if (i < t) { // Copy the null tail as well
            System.arraycopy(elements0, i + 1, elements0, i, back);
            this.tail = t - 1;
        } else { // Wrap around
            System.arraycopy(elements0, i + 1, elements0, i, mask - i);
            elements0[mask] = elements0[0];
            System.arraycopy(elements0, 1, elements0, 0, t);
            this.tail = (t - 1) & mask;
        }
        return true;
    }

    // *** Collection Methods ***

    /**
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this queue
     */
    @Override
    public int size() {
        return (this.tail - this.head) & (this.elements.length - 1);
    }

    /**
     * Returns <code>true</code> if this queue contains no elements.
     *
     * @return <code>true</code> if this queue contains no elements
     */
    @Override
    public boolean isEmpty() {
        return this.head == this.tail;
    }

    /**
     * Returns an iterator over the elements in this queue. The elements will be
     * ordered from first (head) to last (tail). This is the same order that
     * elements would be queued (via successive calls to {@link #remove()} or
     * popped (via successive calls to {@link #pop()}).
     *
     * @return an iterator over the elements in this queue
     */
    @Override
    public Iterator<E> iterator() {
        return new DeqIterator();
    }

    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    protected class DeqIterator implements Iterator<E> {

        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int cursor = ArrayQueue.this.head;

        /**
         * Tail recorded at construction (also in remove), to stop iterator and
         * also to check for comodification.
         */
        private int fence = ArrayQueue.this.tail;

        /**
         * Index of element returned by most recent call to next. Reset to -1 if
         * element is deleted by a call to remove.
         */
        private int lastRet = -1;

        public boolean hasNext() {
            return this.cursor != this.fence;
        }

        public E next() {
            if (this.cursor == this.fence)
                throw new NoSuchElementException();
            final E result = ArrayQueue.this.elements[this.cursor];
            // This check doesn't catch all possible comodifications,
            // but does catch the ones that corrupt traversal
            if (ArrayQueue.this.tail != this.fence || result == null)
                throw new ConcurrentModificationException();
            this.lastRet = this.cursor;
            this.cursor = (this.cursor + 1) & (ArrayQueue.this.elements.length - 1);
            return result;
        }

        public void remove() {
            if (this.lastRet < 0)
                throw new IllegalStateException();
            if (delete(this.lastRet)) { // if left-shifted, undo increment in next()
                this.cursor = (this.cursor - 1) & (ArrayQueue.this.elements.length - 1);
                this.fence = ArrayQueue.this.tail;
            }
            this.lastRet = -1;
        }
    }

    protected class DescendingIterator implements Iterator<E> {

        /*
         * This class is nearly a mirror-image of DeqIterator, using
         * tail instead of head for initial cursor, and head instead of
         * tail for fence.
         */
        private int cursor = ArrayQueue.this.tail;
        private int fence = ArrayQueue.this.head;
        private int lastRet = -1;

        public boolean hasNext() {
            return this.cursor != this.fence;
        }

        public E next() {
            if (this.cursor == this.fence)
                throw new NoSuchElementException();
            this.cursor = (this.cursor - 1) & (ArrayQueue.this.elements.length - 1);
            final E result = ArrayQueue.this.elements[this.cursor];
            if (ArrayQueue.this.head != this.fence || result == null)
                throw new ConcurrentModificationException();
            this.lastRet = this.cursor;
            return result;
        }

        public void remove() {
            if (this.lastRet < 0)
                throw new IllegalStateException();
            if (!delete(this.lastRet)) {
                this.cursor = (this.cursor + 1) & (ArrayQueue.this.elements.length - 1);
                this.fence = ArrayQueue.this.head;
            }
            this.lastRet = -1;
        }
    }

    /**
     * Returns <code>true</code> if this queue contains the specified element.
     * More formally, returns <code>true</code> if and only if this queue contains
     * at least one element <code>e</code> such that <code>o.equals(e)</code>.
     *
     * @param o
     *            object to be checked for containment in this queue
     * @return <code>true</code> if this queue contains the specified element
     */
    @Override
    public boolean contains(final Object o) {
        if (o == null)
            return false;
        final int mask = this.elements.length - 1;
        int i = this.head;
        E x;
        while ((x = this.elements[i]) != null) {
            if (o.equals(x))
                return true;
            i = (i + 1) & mask;
        }
        return false;
    }

    /**
     * Removes a single instance of the specified element from this queue. If
     * the queue does not contain the element, it is unchanged. More formally,
     * removes the first element <code>e</code> such that <code>o.equals(e)</code>
     * (if such an element exists). Returns <code>true</code> if this queue
     * contained the specified element (or equivalently, if this queue changed
     * as a result of the call).
     *
     * @param o
     *            element to be removed from this queue, if present
     * @return <code>true</code> if this queue contained the specified element
     */
    @Override
    public boolean remove(final Object o) {
        if (o == null)
            return false;
        final int mask = this.elements.length - 1;
        int i = this.head;
        E x;
        while ((x = this.elements[i]) != null) {
            if (o.equals(x)) {
                delete(i);
                return true;
            }
            i = (i + 1) & mask;
        }
        return false;
    }

    /**
     * Removes all of the elements from this queue. The queue will be empty
     * after this call returns.
     */
    @Override
    public void clear() {
        final int h = this.head;
        final int t = this.tail;
        if (h != t) { // clear all cells
            this.head = this.tail = 0;
            int i = h;
            final int mask = this.elements.length - 1;
            do {
                this.elements[i] = null;
                i = (i + 1) & mask;
            } while (i != t);
        }
    }

    /**
     * Returns an array containing all of the elements in this queue in proper
     * sequence (from first to last element).
     *
     * <p>
     * The returned array will be "safe" in that no references to it are
     * maintained by this queue. (In other words, this method must allocate a
     * new array). The caller is thus free to modify the returned array.
     *
     * <p>
     * This method acts as bridge between array-based and collection-based APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    @Override
    public Object[] toArray() {
        return copyElements(new Object[size()]);
    }

    /**
     * Returns an array containing all of the elements in this queue in proper
     * sequence (from first to last element); the runtime type of the returned
     * array is that of the specified array. If the queue fits in the specified
     * array, it is returned therein. Otherwise, a new array is allocated with
     * the runtime type of the specified array and the size of this queue.
     *
     * <p>
     * If this queue fits in the specified array with room to spare (i.e., the
     * array has more elements than this queue), the element in the array
     * immediately following the end of the queue is set to <code>null</code>.
     *
     * <p>
     * Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs. Further, this method allows
     * precise control over the runtime type of the output array, and may, under
     * certain circumstances, be used to save allocation costs.
     *
     * <p>
     * Suppose <code>x</code> is a queue known to contain only strings. The
     * following code can be used to dump the queue into a newly allocated array
     * of <code>String</code>:
     *
     * <pre>
     * String[] y = x.toArray(new String[0]);
     * </pre>
     *
     * Note that <code>toArray(new Object[0])</code> is identical in function to
     * <code>toArray()</code>.
     *
     * @param a
     *            the array into which the elements of the queue are to be
     *            stored, if it is big enough; otherwise, a new array of the
     *            same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException
     *             if the runtime type of the specified array is not a supertype
     *             of the runtime type of every element in this queue
     * @throws NullPointerException
     *             if the specified array is null
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(final T[] a) {
        final int size = size();
        final T[] a0 = a.length < size
            ? (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                        .getComponentType(), size)
            : a;
        copyElements(a0);
        if (a0.length > size)
            a0[size] = null;
        return a0;
    }

    // *** Object methods ***

    /**
     * Returns a copy of this queue.
     *
     * @return a copy of this queue
     */
    @Override
    public ArrayQueue<E> clone() {
        return new ArrayQueue<E>(this);
    }

}
