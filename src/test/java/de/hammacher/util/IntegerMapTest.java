package de.hammacher.util;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class IntegerMapTest {

	private static interface IntGenerator {
		int nextInt();
	}

	@Test
	public void testOperations1() {
		Random seedRand = new Random();
		int seed = seedRand.nextInt();
		System.out.println("Seed: "+seed);
		final Random rand = new Random(seed);

		testOperations(1000000, new IntGenerator() {
			public int nextInt() {
				// key:
				return rand.nextInt(1000);
			}
		}, new IntGenerator() {
			public int nextInt() {
				// value:
				return rand.nextInt(1000);
			}
		}, new IntGenerator() {
			public int nextInt() {
				// operation:
				return rand.nextInt(1000);
			}
		}, -1);
	}

	@Test
	public void testOperations2() {
		Random seedRand = new Random();
		int seed = seedRand.nextInt();
		System.out.println("Seed: "+seed);
		final Random rand = new Random(seed);

		final AtomicInteger operations = new AtomicInteger(0);
		testOperations(1000000, new IntGenerator() {
			public int nextInt() {
				// key:
				return rand.nextInt(1+(operations.get()/5));
			}
		}, new IntGenerator() {
			public int nextInt() {
				// value:
				return rand.nextInt(10);
			}
		}, new IntGenerator() {
			public int nextInt() {
				// operation:
				operations.incrementAndGet();
				return rand.nextInt(1000);
			}
		}, -1);
	}

	@Test
	public void testOperations3() {
		Random seedRand = new Random();
		int seed = seedRand.nextInt();
		System.out.println("Seed: "+seed);
		final Random rand = new Random(seed);

		testOperations(1000000, new IntGenerator() {
			public int nextInt() {
				// key:
				return rand.nextInt();
			}
		}, new IntGenerator() {
			public int nextInt() {
				// value:
				return rand.nextInt(10);
			}
		}, new IntGenerator() {
			public int nextInt() {
				// operation:
				return rand.nextInt(1000);
			}
		}, -1);
	}

	private void testOperations(int numOperations, IntGenerator keyGen,
			IntGenerator valGen, IntGenerator opGen, int debugKey) {
		HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
		IntegerMap<Integer> intMap = new IntegerMap<Integer>();

		int opCnt = 0;
		while (opCnt++ < numOperations) {
			int op = opGen.nextInt();
			if (op < 350) {
				int key = keyGen.nextInt();
				Integer val = valGen.nextInt();
				if (key == debugKey)
					System.out.format("put: %d -> %d%n", key, val.intValue());
				Integer old1 = hashMap.put(key, val);
				Integer old2 = intMap.put(key, val);
				Assert.assertEquals(old1, old2);
			} else if (op < 600) {
				int key = keyGen.nextInt();
				if (key == debugKey)
					System.out.format("remove %d%n", key);
				Integer old1 = hashMap.remove(key);
				Integer old2 = intMap.remove(key);
				Assert.assertEquals(old1, old2);
			} else if (op < 700) {
				int size1 = hashMap.size();
				int size2 = intMap.size();
				Assert.assertEquals(size1, size2);
			} else if (op < 800) {
				int key = keyGen.nextInt();
				Integer val1 = hashMap.get(key);
				Integer val2 = intMap.get(key);
				Assert.assertEquals(val1, val2);
			} else if (op < 900) {
				int key = keyGen.nextInt();
				boolean val1 = hashMap.containsKey(key);
				boolean val2 = intMap.containsKey(key);
				Assert.assertEquals(val1, val2);
			} else if (op < 999) {
				int key = keyGen.nextInt();
				boolean val1 = hashMap.containsValue(key);
				boolean val2 = intMap.containsValue(key);
				Assert.assertEquals(val1, val2);
			} else {
				hashMap.clear();
				intMap.clear();
			}
		}
	}

}
