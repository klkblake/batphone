package org.servalproject.servald;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbstractIdRandom extends Random {
	private static final long serialVersionUID = 1L;

	private Random selecter;
	private Random prngs[];

	public AbstractIdRandom(AuthToken key) {
		byte[] data = key.toByteArray();
		List<Long> seeds = new ArrayList<Long>();
		int i;
		for (i = 6; i < data.length; i += 6) {
			seeds.add((long) data[i - 6] << 40
					| (long) data[i - 5] << 32
					| data[i - 4] << 24
					| data[i - 3] << 16
					| data[i - 2] << 8
					| data[i - 1]);
		}
		if (i != data.length) {
			int shift = 40;
			long seed = 0;
			for (; i < data.length; i++) {
				seed |= (long) data[i] << shift;
				shift -= 8;
			}
			seed >>= shift + 8;
			seeds.add(seed);
		}
		selecter = new Random(seeds.remove(seeds.size() - 1));
		this.prngs = new Random[seeds.size()];
		for (i = 0; i < seeds.size(); i++) {
			this.prngs[i] = new Random(seeds.get(i));
		}
	}

	@Override
	public boolean nextBoolean() {
		return nextGen().nextBoolean();
	}

	@Override
	public void nextBytes(byte[] buf) {
		nextGen().nextBytes(buf);
	}

	@Override
	public double nextDouble() {
		return nextGen().nextDouble();
	}

	@Override
	public float nextFloat() {
		return nextGen().nextFloat();
	}

	@Override
	public double nextGaussian() {
		return nextGen().nextGaussian();
	}

	@Override
	public int nextInt() {
		return nextGen().nextInt();
	}

	@Override
	public int nextInt(int n) {
		return nextGen().nextInt(n);
	}

	@Override
	public long nextLong() {
		return nextGen().nextLong();
	}

	@Override
	public void setSeed(long seed) {
		// do nothing
	}

	private Random nextGen() {
		return prngs[selecter.nextInt(prngs.length)];
	}
}
