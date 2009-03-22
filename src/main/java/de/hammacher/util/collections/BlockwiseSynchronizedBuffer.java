package de.hammacher.util.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A BlockingQueue which is optimized for being used as a one-directional buffer
 * between two threads. Input and output are blockwise buffered, so that memory
 * synchronization is only necessary if a full block is appended to the buffer
 * or the next block is read from the buffer.
 *
 * Please note that this collection is only partwise threadsafe: There must only
 * be one reading, and one writing thread (or you have to synchronize between all
 * readers and all writers).
 *
 * Elements are only considered to be inside this queue after the block has been
 * submitted (either because it was full or because one of the flush methods has
 * been called). So contains() and size() do not see data which is buffered on the
 * writers side.
 *
 * @author Clemens Hammacher
 * @param <E> the type of elements to be added to this buffer
 */
public class BlockwiseSynchronizedBuffer<E> implements BlockingQueue<E> {

	private static class Itr<E> implements Iterator<E> {

		private int outPos;
		private E[] out;
		private final Iterator<E[]> nextBlocksIt;

		public Itr(int outPos, E[] out, Iterator<E[]> nextBlocksIt) {
			this.outPos = outPos;
			this.out = out;
			this.nextBlocksIt = nextBlocksIt;
		}

		public boolean hasNext() {
			if (this.outPos == this.out.length) {
				if (!this.nextBlocksIt.hasNext())
					return false;
				this.out = this.nextBlocksIt.next();
				assert this.out.length > 0;
				this.outPos = 0;
			}
			return true;
		}

		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return this.out[this.outPos++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private final BlockingQueue<E[]> arrQueue;
	private final AtomicInteger arrQueueSize = new AtomicInteger(0); // number of elements (NOT arrays!!) in the arrQueue

	private int inPos = 0;
	private E[] in; // size of this array == blockSize
	private int outPos = 0;
	private E[] out = newArray(0);
	private final Semaphore spaceAvailable;

	public BlockwiseSynchronizedBuffer(int blockSize, int maxBufferLength) {
		if (blockSize < 1)
			throw new IllegalArgumentException("blockSize must be > 0");
		if (blockSize > maxBufferLength)
			throw new IllegalArgumentException("blockSize must be <= maxBufferLength");
		this.arrQueue = new ArrayBlockingQueue<E[]>(maxBufferLength); // this is just another limit, but it should never get reached
		this.spaceAvailable = new Semaphore(maxBufferLength);
		this.in = newArray(blockSize);
	}

	@SuppressWarnings("unchecked")
	private E[] newArray(int size) {
		return (E[]) new Object[size];
	}

	public void flush() throws InterruptedException {
		if (this.inPos == this.in.length) {
			this.spaceAvailable.acquire(this.inPos);
			this.arrQueue.put(this.in);
			this.arrQueueSize.addAndGet(this.inPos);
			this.in = newArray(this.inPos);
			this.inPos = 0;
		} else if (this.inPos != 0) {
			this.spaceAvailable.acquire(this.inPos);
			E[] tmp = newArray(this.inPos);
			System.arraycopy(this.in, 0, tmp, 0, this.inPos);
			this.arrQueue.put(tmp);
			this.arrQueueSize.addAndGet(this.inPos);
			this.inPos = 0;
		}
	}

	public boolean tryFlush() {
		if (this.inPos == this.in.length) {
			if (!this.spaceAvailable.tryAcquire(this.inPos))
				return false;
			this.arrQueue.add(this.in);
			this.arrQueueSize.addAndGet(this.inPos);
			this.in = newArray(this.inPos);
			this.inPos = 0;
		} else if (this.inPos != 0) {
			if (!this.spaceAvailable.tryAcquire(this.inPos))
				return false;
			E[] tmp = newArray(this.inPos);
			System.arraycopy(this.in, 0, tmp, 0, this.inPos);
			this.arrQueue.add(tmp);
			this.arrQueueSize.addAndGet(this.inPos);
			this.inPos = 0;
		}
		return true;
	}

	public boolean tryFlush(long timeout, TimeUnit unit) throws InterruptedException {
		if (this.inPos == this.in.length) {
			if (!this.spaceAvailable.tryAcquire(this.inPos, timeout, unit))
				return false;
			this.arrQueue.add(this.in);
			this.arrQueueSize.addAndGet(this.inPos);
			this.in = newArray(this.inPos);
			this.inPos = 0;
		} else if (this.inPos != 0) {
			if (!this.spaceAvailable.tryAcquire(this.inPos, timeout, unit))
				return false;
			E[] tmp = newArray(this.inPos);
			System.arraycopy(this.in, 0, tmp, 0, this.inPos);
			this.arrQueue.add(tmp);
			this.arrQueueSize.addAndGet(this.inPos);
			this.inPos = 0;
		}
		return true;
	}

	public int drainTo(Collection<? super E> c) {
		if (c == null)
			throw new NullPointerException();
		if (c == this)
			throw new IllegalArgumentException();

		int added1 = 0;
		if (this.outPos != this.out.length) {
			added1 = this.out.length - this.outPos;
			if (this.outPos == 0 && this.out.length == this.out.length)
				c.addAll(Arrays.asList(this.out));
			else
				c.addAll(Arrays.asList(this.out).subList(this.outPos, this.out.length));
			this.outPos = this.out.length;
		}
		int added2 = 0;
		E[] tmp;
		while ((tmp = this.arrQueue.poll()) != null) {
			this.arrQueueSize.addAndGet(-tmp.length);
			added2 += tmp.length;
			c.addAll(Arrays.asList(tmp));
		}

		this.spaceAvailable.release(added2);

		return added1 + added2;
	}

	public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == null)
			throw new NullPointerException();
		if (c == this)
			throw new IllegalArgumentException();

		int added1 = 0;
		if (this.outPos != this.out.length) {
			added1 = this.out.length - this.outPos;
			if (added1 > maxElements) {
				added1 = maxElements;
				c.addAll(Arrays.asList(this.out).subList(this.outPos, this.outPos + maxElements));
			} else if (this.outPos == 0) {
				c.addAll(Arrays.asList(this.out));
			} else {
				c.addAll(Arrays.asList(this.out).subList(this.outPos, this.out.length));
			}
			this.outPos += added1;
		}

		int added2 = 0;
		E[] tmp;
		while (added2 < maxElements && (tmp = this.arrQueue.poll()) != null) {
			this.arrQueueSize.addAndGet(-tmp.length);
			if (added2 + tmp.length > maxElements) {
				this.out = tmp;
				this.outPos = maxElements - added2;
				added2 = maxElements;
				c.addAll(Arrays.asList(tmp).subList(0, this.outPos));
			} else {
				added2 += tmp.length;
				c.addAll(Arrays.asList(tmp));
			}
		}

		this.spaceAvailable.release(added2);

		return added1 + added2;
	}

	public boolean add(E o) {
		if (o == null)
			throw new NullPointerException();
		this.in[this.inPos] = o;
		if (++this.inPos == this.in.length && !tryFlush()) {
			--this.inPos;
			throw new IllegalStateException();
		}
		return true;
	}

	public boolean offer(E o) {
		if (o == null)
			throw new NullPointerException();
		this.in[this.inPos] = o;
		if (++this.inPos == this.in.length && !tryFlush()) {
			--this.inPos;
			return false;
		}
		return true;
	}

	public boolean offer(E o, long timeout, TimeUnit unit)
			throws InterruptedException {
		if (o == null)
			throw new NullPointerException();
		this.in[this.inPos] = o;
		if (++this.inPos == this.in.length && !tryFlush(timeout, unit)) {
			--this.inPos;
			return false;
		}
		return true;
	}

	public void put(E o) throws InterruptedException {
		if (o == null)
			throw new NullPointerException();
		this.in[this.inPos] = o;
		if (++this.inPos == this.in.length) {
			try {
				flush(); // throws InterruptedException
			} catch (InterruptedException e) {
				--this.inPos;
				throw e;
			}
		}
		assert this.inPos == 0;
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public E take() throws InterruptedException {
		if (this.outPos == this.out.length) {
			this.out = this.arrQueue.take(); // throws InterruptedException
			this.arrQueueSize.addAndGet(-this.out.length);
			this.spaceAvailable.release(this.out.length);
			assert this.out.length > 0;
			this.outPos = 0;
		}
		return this.out[this.outPos++];
	}

	public E element() {
		if (this.outPos == this.out.length) {
			this.out = this.arrQueue.remove(); // throws NoSuchElementException
			this.arrQueueSize.addAndGet(-this.out.length);
			this.spaceAvailable.release(this.out.length);
			assert this.out.length > 0;
			this.outPos = 0;
		}
		return this.out[this.outPos];
	}

	public E peek() {
		if (this.outPos == this.out.length) {
			E[] nextBlock = this.arrQueue.poll();
			if (nextBlock == null)
				return null;
			this.out = nextBlock;
			this.arrQueueSize.addAndGet(-this.out.length);
			this.spaceAvailable.release(this.out.length);
			assert this.out.length > 0;
			this.outPos = 0;
		}
		return this.out[this.outPos];
	}

	public E poll() {
		if (this.outPos == this.out.length) {
			E[] nextBlock = this.arrQueue.poll();
			if (nextBlock == null)
				return null;
			this.out = nextBlock;
			this.arrQueueSize.addAndGet(-this.out.length);
			this.spaceAvailable.release(this.out.length);
			assert this.out.length > 0;
			this.outPos = 0;
		}
		return this.out[this.outPos++];
	}

	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		if (this.outPos == this.out.length) {
			E[] nextBlock = this.arrQueue.poll(timeout, unit);
			if (nextBlock == null)
				return null;
			this.out = nextBlock;
			this.arrQueueSize.addAndGet(-this.out.length);
			this.spaceAvailable.release(this.out.length);
			assert this.out.length > 0;
			this.outPos = 0;
		}
		return this.out[this.outPos++];
	}

	public E remove() {
		if (this.outPos == this.out.length) {
			this.out = this.arrQueue.remove(); // throws NoSuchElementException
			this.arrQueueSize.addAndGet(-this.out.length);
			this.spaceAvailable.release(this.out.length);
			assert this.out.length > 0;
			this.outPos = 0;
		}
		return this.out[this.outPos++];
	}

	public int remainingCapacity() {
		return this.spaceAvailable.availablePermits() - this.inPos;
	}

	public void clear() {
		this.outPos = this.out.length;
		int removed = 0;
		E[] rem;
		while ((rem = this.arrQueue.poll()) != null)
			removed += rem.length;
		this.arrQueueSize.addAndGet(-removed);
	}

	public boolean contains(Object o) {
		for (int i = this.outPos; i < this.out.length; ++i)
			if (this.out[i].equals(o))
				return true;
		for (E[] tmp: this.arrQueue)
			for (E e: tmp)
				if (e.equals(o))
					return true;
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		if (c.isEmpty())
			return true;
		Set<?> remaining = c instanceof Set<?> ? (Set<?>)c : new HashSet<Object>(c);
		if (remaining.removeAll(Arrays.asList(this.out).subList(this.outPos, this.out.length))
				&& remaining.isEmpty())
			return true;
		for (E[] tmp: this.arrQueue)
			if (remaining.removeAll(Arrays.asList(tmp)) && remaining.isEmpty())
				return true;
		// should not be empty here, but just re-check:
		return remaining.isEmpty();
	}

	public boolean isEmpty() {
		return this.outPos == this.out.length && this.arrQueue.isEmpty();
	}

	public Iterator<E> iterator() {
		return new Itr<E>(this.outPos, this.out, this.arrQueue.iterator());
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return this.out.length - this.outPos + this.arrQueueSize.get();
	}

	public Object[] toArray() {
		int size = size();
		Object[] arr = new Object[size];
		int offset = this.out.length - this.outPos;
		if (offset != 0)
			System.arraycopy(this.out, this.outPos, arr, 0, offset);
		for (E[] tmp: this.arrQueue) {
			System.arraycopy(tmp, 0, arr, offset, tmp.length);
			offset += tmp.length;
		}
		assert offset == size;
		return arr;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		int size = size();
		T[] arr = a.length >= size ? a
			: (T[])Array.newInstance(a.getClass().getComponentType(), size);
		int offset = this.out.length - this.outPos;
		if (offset != 0)
			System.arraycopy(this.out, this.outPos, arr, 0, offset);
		for (E[] tmp: this.arrQueue) {
			System.arraycopy(tmp, 0, arr, offset, tmp.length);
			offset += tmp.length;
		}
		assert offset == size;
		if (size < arr.length)
			arr[size] = null;
		return arr;
	}

}
