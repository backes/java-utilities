package de.hammacher.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import de.hammacher.util.maps.IntegerMap;


public class LongMapPerformance {

	private long fillMap(Map<Integer, Object> map, int num, int max) {
		Object o = new Object();
		Random rand = new Random();
		long startTime = System.nanoTime();

		for (int i = 0; i < num; ++i)
			map.put(rand.nextInt(max), o);

		return System.nanoTime() - startTime;
	}

	private void compareContinuous(int max, int num, int runs) {
		long hashMapTime = 0;
		long intMapTime = 0;

		for (int i = 0; i < runs; ++i) {
			hashMapTime += fillMap(new HashMap<Integer, Object>(), num, max);
			intMapTime += fillMap(new IntegerMap<Object>(), num, max);
		}

		System.out.format("(%10d, %10d) HashMap: %.3f sec; LongMap: %.3f sec%n",
				max, num, 1e-9*hashMapTime, 1e-9*intMapTime);
	}

	@Test
	public void continuousRange_10000_x05() {
		compareContinuous(10000, 5000, 20);
	}
	@Test
	public void continuousRange_10000_x1() {
		compareContinuous(10000, 10000, 20);
	}
	@Test
	public void continuousRange_10000_x5() {
		compareContinuous(10000, 50000, 20);
	}
	@Test
	public void continuousRange_10000_x50() {
		compareContinuous(10000, 500000, 20);
	}
	@Test
	public void continuousRange_10000_x200() {
		compareContinuous(10000, 2000000, 20);
	}

	@Test
	public void continuousRange_100000_x05() {
		compareContinuous(100000, 50000, 3);
	}
	@Test
	public void continuousRange_100000_x1() {
		compareContinuous(100000, 100000, 3);
	}
	@Test
	public void continuousRange_100000_x5() {
		compareContinuous(100000, 500000, 3);
	}
	@Test
	public void continuousRange_100000_x50() {
		compareContinuous(100000, 5000000, 3);
	}
	@Test
	public void continuousRange_100000_x200() {
		compareContinuous(100000, 20000000, 1);
	}

	@Test
	public void uncontinuousRange_10000() {
		compareContinuous(Integer.MAX_VALUE, 10000, 20);
	}
	@Test
	public void uncontinuousRange_100000() {
		compareContinuous(Integer.MAX_VALUE, 100000, 3);
	}
	@Test
	public void uncontinuousRange_1000000() {
		compareContinuous(Integer.MAX_VALUE, 1000000, 3);
	}
	@Test
	public void uncontinuousRange_10000000() {
		compareContinuous(Integer.MAX_VALUE, 10000000, 2);
	}
}
