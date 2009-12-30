package de.hammacher.util.collections;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * An adapter for a queue to add blocking support.
 *
 * @author Clemens Hammacher
 */
public class BlockingQueueWrapper<E>
		extends AbstractQueue<E>
		implements BlockingQueue<E> {

	private final Queue<E> queue;
	private Semaphore freeSem;
	private Semaphore occSem;

	public BlockingQueueWrapper(Queue<E> queue, int capacity) {
		this(queue, capacity, false);
	}

	public BlockingQueueWrapper(Queue<E> queue, int capacity, boolean fair) {
		this.queue = queue;
		this.freeSem = new Semaphore(capacity, fair);
		this.occSem = new Semaphore(0, fair);
	}

	@Override
	public Iterator<E> iterator() {
		return this.queue.iterator();
	}

	@Override
	public int size() {
		return this.queue.size();
	}

	public int drainTo(Collection<? super E> c) {
		if (c == null)
			throw new NullPointerException();
		if (c == this)
			throw new IllegalArgumentException();

		int count = this.occSem.drainPermits();
		for (int i = 0; i < count; ++i) {
			E obj = this.queue.poll();
			if (obj == null) {
				this.occSem.release(count - i);
				count = i;
				break;
			} else
				this.freeSem.release();
			c.add(obj);
		}

		this.freeSem.release(count);
		return count;
	}

	public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == null)
			throw new NullPointerException();
		if (c == this)
			throw new IllegalArgumentException();

		int count = maxElements;
		do {
			count = Math.min(count, this.occSem.availablePermits());
		} while (!this.occSem.tryAcquire(count));

		for (int i = 0; i < count; ++i) {
			E obj = this.queue.poll();
			if (obj == null) {
				this.occSem.release(count - i);
				count = i;
				break;
			} else
				this.freeSem.release();
			c.add(obj);
		}

		this.freeSem.release(count);
		return count;
	}

	public boolean offer(E o) {
		if (o == null)
			throw new NullPointerException();

		if (!this.freeSem.tryAcquire())
			return false;

		if (this.queue.add(o)) {
			this.occSem.release();
			return true;
		} else {
			this.freeSem.release();
			return false;
		}
	}

	public boolean offer(E o, long timeout, TimeUnit unit)
			throws InterruptedException {
		if (o == null)
			throw new NullPointerException();

		if (!this.freeSem.tryAcquire(timeout, unit))
			return false;

		if (this.queue.add(o)) {
			this.occSem.release();
			return true;
		} else {
			this.freeSem.release();
			return false;
		}
	}

	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		if (!this.occSem.tryAcquire(timeout, unit))
			return null;

		E obj = this.queue.poll();
		if (obj == null)
			this.occSem.release();
		else
			this.freeSem.release();
		return obj;
	}

	public void put(E o) throws InterruptedException {
		if (o == null)
			throw new NullPointerException();

		this.freeSem.acquire();

		if (this.queue.add(o)) {
			this.occSem.release();
		} else {
			this.freeSem.release();
		}
	}

	public int remainingCapacity() {
		return this.freeSem.availablePermits();
	}

	public E take() throws InterruptedException {
		for (;;) {
			this.occSem.acquire();

			E obj = this.queue.poll();
			if (obj == null) {
				this.occSem.release();
			} else {
				this.freeSem.release();
				return obj;
			}
		}
	}

	public E peek() {
		return this.queue.peek();
	}

	public E poll() {
		if (!this.occSem.tryAcquire())
			return null;

		E obj = this.queue.poll();
		if (obj == null)
			this.occSem.release();
		else
			this.freeSem.release();
		return obj;
	}

}

