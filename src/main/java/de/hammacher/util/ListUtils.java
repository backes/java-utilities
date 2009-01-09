package de.hammacher.util;

import java.util.AbstractList;
import java.util.List;

public abstract class ListUtils {

	public static class LongList extends AbstractList<Long> {

		private final long[] arr;

		public LongList(long[] arr) {
			this.arr = arr;
		}

		@Override
		public Long get(int index) {
			return this.arr[index];
		}

		@Override
		public int size() {
			return this.arr.length;
		}

	}

	public static class IntegerList extends AbstractList<Integer> {

		private final int[] arr;

		public IntegerList(int[] arr) {
			this.arr = arr;
		}

		@Override
		public Integer get(int index) {
			return this.arr[index];
		}

		@Override
		public int size() {
			return this.arr.length;
		}

	}

	private ListUtils() {
		// prevent instantiation
	}

	public static List<Long> longArrayAsList(long... arr) {
		return new LongList(arr);
	}

	public static List<Integer> intArrayAsList(int... arr) {
		return new IntegerList(arr);
	}

}
