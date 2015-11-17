package de.hammacher.util.maps;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class IntegerMap<V> implements Map<Integer, V>, Cloneable {

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified by either of the constructors with
     * arguments. MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    static final float DEFAULT_SWITCH_TO_MAP_RATIO = 0.05f;

    static final float DEFAULT_SWITCH_TO_LIST_RATIO = 0.3f;

    /**
     * Will switch back (from list to map) when the ratio (size/highest_int) is below this threshold.
     */
    private final float switchToMapRatio;

    /**
     * Will switch from map to list when the ratio (size/highest_int) is above this threshold.
     */
    private final float switchToListRatio;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    Entry<V>[] mapTable;

    V[] list;

    // maintained when the map is used to notice when we can switch to list
    private int minKey = Integer.MAX_VALUE;
    private int maxKey = Integer.MIN_VALUE;

    int listOffset;

    // this value is stored in the list to represent "null"
    private static final Object NULL_VALUE = new Object();

    /**
     * The number of key-value mappings contained in this map.
     */
    int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int mapThreshold;

    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;

    /**
     * The number of times this HashMap has been structurally modified Structural modifications are those that change
     * the number of mappings in the HashMap or otherwise modify its internal structure (e.g., rehash). This field is
     * used to make iterators on Collection-views of the HashMap fail-fast. (See ConcurrentModificationException).
     */
    volatile int modCount;

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial capacity and load factor.
     *
     * @param initialCapacity
     *            the initial capacity
     * @param loadFactor
     *            the load factor
     * @throws IllegalArgumentException
     *             if the initial capacity is negative or the load factor is nonpositive
     */
    public IntegerMap(final int initialCapacity, final float loadFactor, final float switchToMapRatio,
            final float switchToListRatio) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        final int initCapacity = initialCapacity > MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : initialCapacity;
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) // check for negative value or NaN
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initCapacity)
            capacity <<= 1;

        this.loadFactor = loadFactor;
        this.mapThreshold = (int) (capacity * loadFactor);
        this.mapTable = Entry.newArray(capacity);
        this.switchToMapRatio = switchToMapRatio;
        this.switchToListRatio = switchToListRatio;
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial capacity and the default load factor (0.75).
     *
     * @param initialMapCapacity
     *            the initial capacity.
     * @throws IllegalArgumentException
     *             if the initial capacity is negative.
     */
    public IntegerMap(final int initialMapCapacity) {
        this(initialMapCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_SWITCH_TO_MAP_RATIO, DEFAULT_SWITCH_TO_LIST_RATIO);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity (16) and the default load factor (0.75).
     */
    public IntegerMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return this.size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the
     * key.
     *
     * <p>
     * More formally, if this map contains a mapping from a key {@code k} to a value {@code v} such that
     * {@code (key==null ? k==null : key.equals(k))}, then this method returns {@code v}; otherwise it returns
     * {@code null}. (There can be at most one such mapping.)
     *
     * <p>
     * A return value of {@code null} does not <i>necessarily</i> indicate that the map contains no mapping for the
     * key; it's also possible that the map explicitly maps the key to {@code null}. The
     * {@link #containsKey(Object) containsKey} operation may be used to distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    @Override
    public V get(final Object key) {
        if (key instanceof Integer)
            return get(((Integer) key).intValue());
        return null;
    }

    public V get(final int key) {
        if (this.list != null) {
        	int offset = key - this.listOffset;
            if (offset >= 0 && offset < this.list.length)
                return this.list[offset] == getNullValue() ? null : this.list[offset];
            return null;
        }
        final int index = key & (this.mapTable.length - 1);
        for (Entry<V> e = this.mapTable[index]; e != null; e = e.next)
            if (key == e.key)
                return e.value;
        return null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     *
     * @param key
     *            The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     */
    @Override
    public boolean containsKey(final Object key) {
        return key instanceof Integer ? containsKey(((Integer)key).intValue()) : false;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     *
     * @param key
     *            The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     */
    public boolean containsKey(final int key) {
        if (this.list != null) {
        	int offset = key - this.listOffset;
            return offset >= 0 && offset < this.list.length && this.list[offset] != null;
        }

        final int index = key & (this.mapTable.length - 1);
        for (Entry<V> e = this.mapTable[index]; e != null; e = e.next)
            if (e.key == key)
                return true;
        return false;
    }

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * the key, the old value is replaced.
     *
     * @param key
     *            key with which the specified value is to be associated
     * @param value
     *            value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
     *         <tt>key</tt>.
     */
    @Override
    public V put(final Integer key, final V value) {
        return put(key.intValue(), value);
    }

    public V put(final int key, final V value) {
        if (this.list != null) {
        	int offset = key - this.listOffset;
            if (offset < 0) {
            	if (this.size < this.switchToMapRatio * ((double)this.list.length - offset)) {
            		switchToMap();
                    // and continue with the map code below...
            	} else {
            		int addBelow = Math.max(-2*offset, this.list.length/2);
                    int newSize = this.list.length + addBelow;
                    if (newSize < this.list.length || this.listOffset - addBelow > this.listOffset) {
                        newSize = Integer.MAX_VALUE;
                        addBelow = newSize - this.list.length;
                        if (addBelow + offset < 0)
                        	throw new RuntimeException("Map too big");
                    }
                    this.listOffset -= addBelow;
                    offset = key - this.listOffset;
                    final V[] oldList = this.list;
                    this.list = newArray(newSize);
                    System.arraycopy(oldList, 0, this.list, addBelow, oldList.length);
                    this.list[offset] = value == null ? getNullValue() : value;
                    this.minKey = Math.min(this.minKey, key);
                    ++this.size;
                    return null;
            	}
            } else if (offset >= this.list.length) {
            	if (this.size < this.switchToMapRatio * offset) {
            		switchToMap();
                    // and continue with the map code below...
            	} else {
                    int newSize = Math.max(offset+1, this.list.length * 3 / 2);
                    if (newSize <= offset) {
                        newSize = Integer.MAX_VALUE;
                    }
                    final V[] oldList = this.list;
                    this.list = newArray(newSize);
                    System.arraycopy(oldList, 0, this.list, 0, oldList.length);
                    this.list[offset] = value == null ? getNullValue() : value;
                    this.maxKey = Math.max(this.maxKey, key);
                    ++this.size;
                    return null;
            	}
            } else {
                final V old = this.list[offset];
                this.list[offset] = value == null ? getNullValue() : value;
                if (old == null) {
                    ++this.size;
                    this.minKey = Math.min(this.minKey, key);
                    this.maxKey = Math.max(this.maxKey, key);
                }
                return old == getNullValue() ? null : old;
            }
        }

        // code for hashtable-lookup:
        final int index = key & (this.mapTable.length - 1);
        for (Entry<V> e = this.mapTable[index]; e != null; e = e.next) {
            if (e.key == key) {
                final V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        this.modCount++;
        addEntry(key, value, index);
        return null;
    }

	private void switchToMap() {
        ++this.modCount;
        final double minTableSize = 1.1 * this.list.length / this.loadFactor;
        int mapTableSize = 1;
        while (mapTableSize < minTableSize) {
            if (mapTableSize == MAXIMUM_CAPACITY)
                throw new IllegalStateException("Maximum map size exceeded");
            mapTableSize <<= 1;
        }

        this.mapTable = Entry.newArray(mapTableSize);
    	this.minKey = Integer.MAX_VALUE;
        this.maxKey = Integer.MIN_VALUE;
        for (int key = 0; key < this.list.length; ++key) {
            final V value = this.list[key];
            if (value == null)
                continue;
            int realKey = key + this.listOffset;
            this.minKey = Math.min(this.minKey, realKey);
            this.maxKey = realKey;
            final int index = realKey & (mapTableSize - 1);
            this.mapTable[index] = new Entry<V>(realKey, value == getNullValue() ? null : value, this.mapTable[index]);
        }
        this.list = null;
        ++this.modCount;
    }

    /**
     * Rehashes the contents of this map into a new array with a larger capacity. This method is called automatically
     * when the number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not resize the map, but sets threshold to
     * Integer.MAX_VALUE. This has the effect of preventing future calls.
     *
     * @param newCapacity
     *            the new capacity, MUST be a power of two; must be greater than current capacity unless current
     *            capacity is MAXIMUM_CAPACITY (in which case value is irrelevant).
     */
    void resizeMap(final int newCapacity) {
        final Entry<V>[] oldTable = this.mapTable;
        final int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            this.mapThreshold = Integer.MAX_VALUE;
            return;
        }

        final Entry<V>[] newTable = Entry.newArray(newCapacity);
        transferMap(newTable);
        this.mapTable = newTable;
        this.mapThreshold = (int) (newCapacity * this.loadFactor);
    }

    /**
     * Transfers all entries from current table to newTable.
     */
    private void transferMap(final Entry<V>[] newTable) {
        final Entry<V>[] src = this.mapTable;
        final int newCapacity = newTable.length;
        this.minKey = Integer.MAX_VALUE;
        this.maxKey = Integer.MIN_VALUE;
        for (int j = 0; j < src.length; j++) {
            Entry<V> e = src[j];
            if (e != null) {
                src[j] = null;
                do {
                	this.minKey = Math.min(this.minKey, e.key);
                	this.maxKey = Math.max(this.maxKey, e.key);
                    final Entry<V> next = e.next;
                    final int newIndex = e.key & (newCapacity - 1);
                    e.next = newTable[newIndex];
                    newTable[newIndex] = e;
                    e = next;
                } while (e != null);
            }
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map. These mappings will replace any mappings that this
     * map had for any of the keys currently in the specified map.
     *
     * @param m
     *            mappings to be stored in this map
     * @throws NullPointerException
     *             if the specified map is null
     */
    @Override
    public void putAll(final Map<? extends Integer, ? extends V> m) {
        for (final Map.Entry<? extends Integer, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key
     *            key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
     *         <tt>key</tt>. (A <tt>null</tt> return can also indicate that the map previously associated
     *         <tt>null</tt> with <tt>key</tt>.)
     */
    @Override
    public V remove(final Object key) {
        if (key instanceof Integer)
            return remove(((Integer) key).intValue());
        return null;
    }

    public V remove(final int key) {
        if (this.list != null) {
        	int offset = key - this.listOffset;
            if (offset < 0 || offset >= this.list.length)
                return null;
            final V old = this.list[offset];
            if (old != null) {
                this.list[offset] = null;
                --this.size;
            }
            return old == getNullValue() ? null : old;
        }

        final int index = key & (this.mapTable.length - 1);
        Entry<V> prev = this.mapTable[index];
        Entry<V> e = prev;

        while (e != null) {
            final Entry<V> next = e.next;
            if (e.key == key) {
                ++this.modCount;
                --this.size;
                if (prev == e)
                    this.mapTable[index] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    /**
     * Removes all of the mappings from this map. The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        ++this.modCount;
        this.size = 0;
        if (this.list != null) {
            this.list = newArray(this.list.length);
        } else {
            this.mapTable = Entry.newArray(this.mapTable.length);
        }
        this.minKey = Integer.MAX_VALUE;
        this.maxKey = Integer.MIN_VALUE;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value.
     *
     * @param value
     *            value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the specified value
     */
    @Override
    public boolean containsValue(final Object value) {
        if (this.list != null) {
        	if (value == null) {
        		// search for getNullValue()
	            for (final V val : this.list)
	                if (val == getNullValue())
	                    return true;
        	} else {
	            for (final V val : this.list)
	                if (val != null && val.equals(value))
	                    return true;
        	}
            return false;
        }

        final Entry<V>[] tab = this.mapTable;
        for (int i = 0; i < tab.length; i++)
            for (Entry<V> e = tab[i]; e != null; e = e.next)
                if (value.equals(e.value))
                    return true;
        return false;
    }

    private static final class Entry<V> implements Map.Entry<Integer, V> {

        final int key;

        V value;

        Entry<V> next;

        /**
         * Creates new entry.
         */
        Entry(final int key, final V value, final Entry<V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
		public static <V> Entry<V>[] newArray(int length) {
        	return new Entry[length];
		}

		@Override
        public final Integer getKey() {
            return this.key;
        }

        @Override
        public final V getValue() {
            return this.value;
        }

        @Override
        public final V setValue(final V newValue) {
            final V oldValue = this.value;
            this.value = newValue;
            return oldValue;
        }

        @Override
        public final boolean equals(final Object o) {
            if (!(o instanceof Map.Entry<?, ?>))
                return false;
            final Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            final Integer k1 = getKey();
            final Object k2 = e.getKey();
            if (k1 == null ? k2 == null : k1.equals(k2)) {
                final Object v1 = getValue();
                final Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return this.key ^ (this.value == null ? 0 : this.value.hashCode());
        }

        @Override
        public final String toString() {
            return this.key + "=" + getValue();
        }

    }

    /**
     * Adds a new entry with the specified key, value and hash code to the specified bucket. It is the responsibility of
     * this method to resize the table if appropriate.
     */
    private void addEntry(final int key, final V value, final int index) {
        this.mapTable[index] = new Entry<V>(key, value, this.mapTable[index]);
        ++this.size;
        this.minKey = Math.min(this.minKey, key);
        this.maxKey = Math.max(this.maxKey, key);
        if (this.size > 3 && this.size > this.switchToListRatio * ((float)this.maxKey - this.minKey + 1f)) {
            switchToList();
        } else if (this.size >= this.mapThreshold)
            resizeMap(2 * this.mapTable.length);
    }

    private void switchToList() {
        ++this.modCount;
        int neededSize = this.maxKey - this.minKey + 1;
        if (neededSize <= 0)
        	throw new RuntimeException("map too big");
        // reserve 1/4 space below, but only if no underflow happens
        int spaceBelow = neededSize / 4;
        if (this.minKey < 0 && this.minKey - Integer.MIN_VALUE < spaceBelow)
        	spaceBelow = this.minKey - Integer.MIN_VALUE;
        this.listOffset = this.minKey - spaceBelow;
        // check if the offset of maxKey is still positive (no overflow)
        if (this.maxKey - this.listOffset < 0) {
        	this.listOffset = this.minKey;
        	spaceBelow = 0;
        }
        int realSize = neededSize + spaceBelow + (neededSize / 4);
        if (realSize < 0)
        	realSize = Integer.MAX_VALUE;

        this.list = newArray(realSize);
        this.minKey = Integer.MAX_VALUE;
        this.maxKey = Integer.MIN_VALUE;
        for (int j = 0; j < this.mapTable.length; j++) {
            Entry<V> e = this.mapTable[j];
            if (e != null) {
                this.mapTable[j] = null; // help GC
                do {
                	this.minKey = Math.min(this.minKey, e.key);
                	this.maxKey = Math.max(this.maxKey, e.key);
                	int offset = e.key - this.listOffset;
	                if (offset < 0 || offset >= this.list.length)
	                    throw new ConcurrentModificationException();
	                this.list[offset] = e.value == null ? getNullValue() : e.value;
	                e = e.next;
                } while (e != null);
            }
        }
        this.mapTable = null;
        ++this.modCount;
    }

    private class MapIterator implements Iterator<Map.Entry<Integer, V>> {
        Entry<V> next; // next entry to return

        int expectedModCount; // For fast-fail

        int index; // current slot

        Entry<V> current; // current entry

        MapIterator() {
            this.expectedModCount = IntegerMap.this.modCount;
            if (IntegerMap.this.size > 0) { // advance to first entry
                final Entry<V>[] t = IntegerMap.this.mapTable;
                while (this.index < t.length && (this.next = t[this.index++]) == null) {
                    continue;
                }
            }
        }

        @Override
        public Entry<V> next() {
            if (IntegerMap.this.modCount != this.expectedModCount)
                throw new ConcurrentModificationException();
            final Entry<V> e = this.next;
            if (e == null)
                throw new NoSuchElementException();

            if ((this.next = e.next) == null) {
                final Entry<V>[] t = IntegerMap.this.mapTable;
                while (this.index < t.length && (this.next = t[this.index++]) == null)
                    continue;
            }
            this.current = e;
            return e;
        }

        @Override
        public final boolean hasNext() {
            return this.next != null;
        }

        @Override
        public void remove() {
            if (this.current == null)
                throw new IllegalStateException();
            if (IntegerMap.this.modCount != this.expectedModCount)
                throw new ConcurrentModificationException();
            final int k = this.current.key;
            this.current = null;
            IntegerMap.this.remove(k);
            this.expectedModCount = IntegerMap.this.modCount;
        }

    }

    /**
     * Returns a {@link Set} view of the keys contained in this map. The set is backed by the map, so changes to the map
     * are reflected in the set, and vice-versa. If the map is modified while an iteration over the set is in progress
     * (except through the iterator's own <tt>remove</tt> operation), the results of the iteration are undefined. The
     * set supports element removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations. It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     */
    @Override
    public Set<Integer> keySet() {
        return new AbstractSet<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                if (IntegerMap.this.list != null) {
                    return new Iterator<Integer>() {

                        int nextCursor = getNextCursor(0);

                        int expectedModCount = IntegerMap.this.modCount;

                        int lastKey = -1;

                        private int getNextCursor(final int i) {
                            int next = i;
                            while (next < IntegerMap.this.list.length && IntegerMap.this.list[next] == null)
                                ++next;
                            return next;
                        }

                        @Override
                        public boolean hasNext() {
                            if (this.expectedModCount != IntegerMap.this.modCount)
                                throw new ConcurrentModificationException();
                            return this.nextCursor < IntegerMap.this.list.length;
                        }

                        @Override
                        public Integer next() {
                            if (!hasNext()) // throws ConcurrentModificationException
                                throw new NoSuchElementException();
                            this.lastKey = this.nextCursor;
                            this.nextCursor = getNextCursor(this.nextCursor + 1);
                            return this.lastKey + IntegerMap.this.listOffset;
                        }

                        @Override
                        public void remove() {
                            if (this.expectedModCount != IntegerMap.this.modCount)
                                throw new ConcurrentModificationException();
                            if (this.lastKey == -1)
                                throw new IllegalStateException();
                            IntegerMap.this.remove(this.lastKey + IntegerMap.this.listOffset);
                        }

                    };
                }
                // else:

                return new Iterator<Integer>() {
                    private final Iterator<Map.Entry<Integer, V>> i = new MapIterator();

                    @Override
                    public boolean hasNext() {
                        return this.i.hasNext();
                    }

                    @Override
                    public Integer next() {
                        return this.i.next().getKey();
                    }

                    @Override
                    public void remove() {
                        this.i.remove();
                    }
                };
            }

            @Override
            public int size() {
                return IntegerMap.this.size();
            }

            @Override
            public boolean contains(final Object k) {
                return IntegerMap.this.containsKey(k);
            }
        };
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map. The collection is backed by the map, so
     * changes to the map are reflected in the collection, and vice-versa. If the map is modified while an iteration
     * over the collection is in progress (except through the iterator's own <tt>remove</tt> operation), the results
     * of the iteration are undefined. The collection supports element removal, which removes the corresponding mapping
     * from the map, via the <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     */
    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                if (IntegerMap.this.list != null) {
                    return new Iterator<V>() {

                        int nextCursor = getNextCursor(0);

                        int expectedModCount = IntegerMap.this.modCount;

                        int lastKey = -1;

                        private int getNextCursor(final int i) {
                            int next = i;
                            while (next < IntegerMap.this.list.length && IntegerMap.this.list[next] == null)
                                ++next;
                            return next;
                        }

                        @Override
                        public boolean hasNext() {
                            if (this.expectedModCount != IntegerMap.this.modCount)
                                throw new ConcurrentModificationException();
                            return this.nextCursor < IntegerMap.this.list.length;
                        }

                        @Override
                        @SuppressWarnings("synthetic-access")
						public V next() {
                            if (!hasNext()) // throws ConcurrentModificationException
                                throw new NoSuchElementException();
                            this.lastKey = this.nextCursor;
                            this.nextCursor = getNextCursor(this.nextCursor + 1);
                            V val = IntegerMap.this.list[this.lastKey];
							return val == getNullValue() ? null : val;
                        }

                        @Override
                        public void remove() {
                            if (this.expectedModCount != IntegerMap.this.modCount)
                                throw new ConcurrentModificationException();
                            if (this.lastKey == -1)
                                throw new IllegalStateException();
                            IntegerMap.this.remove(this.lastKey + IntegerMap.this.listOffset);
                        }

                    };
                }
                // else:

                return new Iterator<V>() {
                    private final Iterator<Map.Entry<Integer, V>> i = new MapIterator();

                    @Override
                    public boolean hasNext() {
                        return this.i.hasNext();
                    }

                    @Override
                    public V next() {
                        return this.i.next().getValue();
                    }

                    @Override
                    public void remove() {
                        this.i.remove();
                    }
                };
            }

            @Override
            public int size() {
                return IntegerMap.this.size();
            }

            @Override
            public boolean contains(final Object v) {
                return IntegerMap.this.containsValue(v);
            }
        };
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map. The set is backed by the map, so changes to the
     * map are reflected in the set, and vice-versa. If the map is modified while an iteration over the set is in
     * progress (except through the iterator's own <tt>remove</tt> operation, or through the <tt>setValue</tt>
     * operation on a map entry returned by the iterator) the results of the iteration are undefined. The set supports
     * element removal, which removes the corresponding mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations. It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Map.Entry<Integer, V>> entrySet() {
        return new EntrySet();
    }

    final class EntrySet implements Set<Map.Entry<Integer, V>> {

        @Override
        public Iterator<Map.Entry<Integer, V>> iterator() {
            if (IntegerMap.this.list != null) {
                return new Iterator<Map.Entry<Integer, V>>() {

                    int nextCursor = getNextCursor(0);

                    int expectedModCount = IntegerMap.this.modCount;

                    int lastKey = -1;

                    private int getNextCursor(final int i) {
                        int next = i;
                        while (next < IntegerMap.this.list.length && IntegerMap.this.list[next] == null)
                            ++next;
                        return next;
                    }

                    @Override
                    public boolean hasNext() {
                        if (this.expectedModCount != IntegerMap.this.modCount)
                            throw new ConcurrentModificationException();
                        return this.nextCursor < IntegerMap.this.list.length;
                    }

                    @Override
                    @SuppressWarnings("synthetic-access")
					public Entry<V> next() {
                        if (!hasNext()) // throws ConcurrentModificationException
                            throw new NoSuchElementException();
                        this.lastKey = this.nextCursor;
                        this.nextCursor = getNextCursor(this.nextCursor + 1);
                        V val = IntegerMap.this.list[this.lastKey];
						return new Entry<V>(this.lastKey + IntegerMap.this.listOffset, val == getNullValue() ? null : val, null);
                    }

                    @Override
                    public void remove() {
                        if (this.expectedModCount != IntegerMap.this.modCount)
                            throw new ConcurrentModificationException();
                        if (this.lastKey == -1)
                            throw new IllegalStateException();
                        IntegerMap.this.remove(this.lastKey + IntegerMap.this.listOffset);
                    }

                };
            }
            // else:

            return new MapIterator();
        }

        @Override
        public boolean contains(final Object o) {
            if (!(o instanceof Entry<?>))
                return false;
            final Entry<?> e = (Entry<?>) o;
            V val = get(e.key);
            return val == null ? e.getValue() == null : val.equals(e.getValue());
        }

        @Override
        public boolean remove(final Object o) {
            if (o instanceof Entry<?>) {
            	Entry<?> e = (Entry<?>) o;
            	V val = get(e.key);
            	if (val == null || !val.equals(e.getValue()))
            		return false;
                return IntegerMap.this.remove(e.key) != null;
            }
            return false;
        }

        @Override
        public int size() {
            return IntegerMap.this.size;
        }

        @Override
        public void clear() {
            IntegerMap.this.clear();
        }

        @Override
        public boolean add(final java.util.Map.Entry<Integer, V> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(final Collection<? extends Map.Entry<Integer, V>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            for (final Object o : c)
                if (!contains(o))
                    return false;
            return true;
        }

        @Override
        public boolean isEmpty() {
            return IntegerMap.this.isEmpty();
        }

        @Override
        public boolean removeAll(final Collection<?> c) {
            boolean changed = false;
            for (final Object o : c)
                if (remove(o))
                    changed = true;
            return changed;
        }

        @Override
        public boolean retainAll(final Collection<?> c) {
            final Iterator<Map.Entry<Integer, V>> e = iterator();
            boolean changed = false;
            while (e.hasNext())
                if (!c.contains(e.next())) {
                    e.remove();
                    changed = true;
                }
            return changed;
        }

        @Override
        public Object[] toArray() {
            final Object[] r = new Object[size()];
            final Iterator<Map.Entry<Integer, V>> it = iterator();
            for (int i = 0; i < r.length; i++) {
                if (!it.hasNext()) { // fewer elements than expected
                    final Object[] r2 = new Object[i];
                    System.arraycopy(r, 0, r2, 0, i);
                    return r2;
                }
                r[i] = it.next();
            }
            return it.hasNext() ? finishToArray(r, it) : r;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(final T[] a) {
            final T[] r = a.length >= size() ? a : (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                    .getComponentType(), size());
            final Iterator<Map.Entry<Integer, V>> it = iterator();

            for (int i = 0; i < r.length; i++) {
                if (!it.hasNext()) { // fewer elements than expected
                    if (a != r) {
                        final T[] r2 = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                            .getComponentType(), i);
                        System.arraycopy(r, 0, r2, 0, i);
                        return r2;
                    }
                    r[i] = null; // null-terminate
                    return r;
                }
                r[i] = (T) it.next();
            }
            return it.hasNext() ? finishToArray(r, it) : r;
        }

        /**
         * Reallocates the array being used within toArray when the iterator returned more elements than expected, and
         * finishes filling it from the iterator.
         *
         * @param r
         *            the array, replete with previously stored elements
         * @param it
         *            the in-progress iterator over this collection
         * @return array containing the elements in the given array, plus any further elements returned by the iterator,
         *         trimmed to size
         */
        @SuppressWarnings("unchecked")
        private <T> T[] finishToArray(final T[] r, final Iterator<?> it) {
            T[] a = r;
            int i = a.length;
            while (it.hasNext()) {
                final int cap = a.length;
                if (i == cap) {
                    int newCap = ((cap / 2) + 1) * 3;
                    if (newCap <= cap) { // integer overflow
                        if (cap == Integer.MAX_VALUE)
                            throw new OutOfMemoryError("Required array size too large");
                        newCap = Integer.MAX_VALUE;
                    }
                    final T[] old = a;
                    a = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                        .getComponentType(), newCap);
                    System.arraycopy(old, 0, a, 0, newCap);
                }
                a[i++] = (T) it.next();
            }
            // trim if overallocated
            if (i == a.length)
                return a;
            final T[] newA = (T[]) java.lang.reflect.Array.newInstance(a.getClass()
                .getComponentType(), i);
            System.arraycopy(a, 0, newA, 0, i);
            return newA;
        }

    }

    @Override
    public String toString() {
        final Iterator<Map.Entry<Integer, V>> i = entrySet().iterator();
        if (!i.hasNext())
            return "{}";

        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        while (true) {
            final Map.Entry<Integer, V> e = i.next();
            final Integer key = e.getKey();
            final V value = e.getValue();
            sb.append(key).append('=').append(value == this ? "(this Map)" : value);
            if (!i.hasNext())
                return sb.append('}').toString();
            sb.append(", ");
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        final Iterator<Map.Entry<Integer, V>> i = entrySet().iterator();
        while (i.hasNext())
            h += i.next().hashCode();
        return h;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Map<?, ?>))
            return false;
        final Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != size())
            return false;

        try {
            final Iterator<Map.Entry<Integer, V>> i = entrySet().iterator();
            while (i.hasNext()) {
                final Map.Entry<Integer, V> e = i.next();
                final Integer key = e.getKey();
                final V value = e.getValue();
                if (value == null ? m.get(key) != null : !value.equals(m.get(key)))
                    return false;
            }
        } catch (final ClassCastException unused) {
            return false;
        } catch (final NullPointerException unused) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
	@Override
    public IntegerMap<V> clone() {
        IntegerMap<V> clone;
        try {
            clone = (IntegerMap<V>) super.clone();
        } catch (final CloneNotSupportedException e) {
            // this should never occur since we are cloneable!!
            throw new RuntimeException(e);
        }
        if (this.list != null) {
            clone.list = newArray(this.list.length);
            System.arraycopy(this.list, 0, clone.list, 0, this.list.length);
        }
        if (this.mapTable != null) {
            final Entry<V>[] newTable = Entry.newArray(this.mapTable.length);
            for (int j = 0; j < this.mapTable.length; ++j) {
                Entry<V> e = this.mapTable[j];
                while (e != null) {
                    newTable[j] = new Entry<V>(e.key, e.value, newTable[j]);
                    e = e.next;
                }
            }
            clone.mapTable = newTable;
        }
        return clone;
    }

    @SuppressWarnings("unchecked")
	private V[] newArray(int length) {
    	return (V[]) new Object[length];
    }

    @SuppressWarnings("unchecked")
	private V getNullValue() {
        // the cast of NULL_VALUE to V is removed during type erasure (ugly, but works...)
    	return (V) NULL_VALUE;
	}

}
