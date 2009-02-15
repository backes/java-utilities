package de.hammacher.util;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class LongMapTest {

	private static interface LongGenerator {
		long nextLong();
	}

	@Test
	public void testOperations1() {
		Random seedRand = new Random();
		int seed = seedRand.nextInt();
		System.out.println("Seed: "+seed);
		final Random rand = new Random(seed);

		testOperations(1000000, new LongGenerator() {
			public long nextLong() {
				// key:
				return rand.nextInt(1000);
			}
		}, new LongGenerator() {
			public long nextLong() {
				// value:
				return rand.nextInt(1000);
			}
		}, new LongGenerator() {
			public long nextLong() {
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
		testOperations(1000000, new LongGenerator() {
			public long nextLong() {
				// key:
				return rand.nextInt(1+(operations.get()/5));
			}
		}, new LongGenerator() {
			public long nextLong() {
				// value:
				return rand.nextInt(10);
			}
		}, new LongGenerator() {
			public long nextLong() {
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

		testOperations(1000000, new LongGenerator() {
			public long nextLong() {
				// key:
				return rand.nextLong();
			}
		}, new LongGenerator() {
			public long nextLong() {
				// value:
				return rand.nextInt(10);
			}
		}, new LongGenerator() {
			public long nextLong() {
				// operation:
				return rand.nextInt(1000);
			}
		}, -1);
	}

	private void testOperations(int numOperations, LongGenerator keyGen,
			LongGenerator valGen, LongGenerator opGen, int debugKey) {
		HashMap<Long, Long> hashMap = new HashMap<Long, Long>();
		LongMap<Long> intMap = new LongMap<Long>();

		int opCnt = 0;
		while (opCnt++ < numOperations) {
			long op = opGen.nextLong();
			if (op < 350) {
				long key = keyGen.nextLong();
				Long val = valGen.nextLong();
				if (key == debugKey)
					System.out.format("put: %d -> %d%n", key, val.intValue());
				Long old1 = hashMap.put(key, val);
				Long old2 = intMap.put(key, val);
				Assert.assertEquals(old1, old2);
			} else if (op < 600) {
				long key = keyGen.nextLong();
				if (key == debugKey)
					System.out.format("remove %d%n", key);
				Long old1 = hashMap.remove(key);
				Long old2 = intMap.remove(key);
				Assert.assertEquals(old1, old2);
			} else if (op < 700) {
				int size1 = hashMap.size();
				int size2 = intMap.size();
				Assert.assertEquals(size1, size2);
			} else if (op < 800) {
				long key = keyGen.nextLong();
				Long val1 = hashMap.get(key);
				Long val2 = intMap.get(key);
				Assert.assertEquals(val1, val2);
			} else if (op < 900) {
				long key = keyGen.nextLong();
				boolean val1 = hashMap.containsKey(key);
				boolean val2 = intMap.containsKey(key);
				Assert.assertEquals(val1, val2);
			} else if (op < 999) {
				long key = keyGen.nextLong();
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
