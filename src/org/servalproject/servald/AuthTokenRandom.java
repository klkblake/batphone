package org.servalproject.servald;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import android.util.Log;

public class AuthTokenRandom extends Random {
	private static final long serialVersionUID = 1L;

	private SecureRandom prngs[] = new SecureRandom[2];
	private int idx = 0;

	// We rely on the fact that the AuthToken contains 320 bits of entropy.
	public AuthTokenRandom(AuthToken key) {
		byte[] data = key.toByteArray();
		byte[] seed1 = new byte[data.length / 2];
		byte[] seed2 = new byte[seed1.length];
		System.arraycopy(data, 0, seed1, 0, seed1.length);
		System.arraycopy(data, seed1.length, seed2, 0, seed1.length);
		try {
			prngs[0] = SecureRandom.getInstance("SHA1PRNG");
			prngs[1] = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			Log.wtf("AuthTokenRandom",
					"SHA1PRNG not implemented on this system??");
		}
		prngs[0].setSeed(seed1);
		prngs[1].setSeed(seed2);
	}

	@Override
	public boolean nextBoolean() {
		return prng().nextBoolean();
	}

	@Override
	public void nextBytes(byte[] buf) {
		prng().nextBytes(buf);
	}

	@Override
	public double nextDouble() {
		return prng().nextDouble();
	}

	@Override
	public float nextFloat() {
		return prng().nextFloat();
	}

	@Override
	public double nextGaussian() {
		return prng().nextGaussian();
	}

	@Override
	public int nextInt() {
		return prng().nextInt();
	}

	@Override
	public int nextInt(int n) {
		return prng().nextInt(n);
	}

	@Override
	public long nextLong() {
		return prng().nextLong();
	}

	@Override
	public void setSeed(long seed) {
		// do nothing
	}

	private SecureRandom prng() {
		idx = 1 - idx;
		return prngs[idx];
	}
}
