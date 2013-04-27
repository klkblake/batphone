package org.servalproject.auth;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.servalproject.batphone.CallHandler;

import android.util.Log;

@SuppressWarnings("serial")
public class AuthRandom extends Random {

	private CallHandler call;
	private BlockingQueue<Long> queue;

	public AuthRandom(CallHandler callHandler) {
		call = callHandler;
		queue = callHandler.authRandQueue;
		call.startAuth();
		call.authNext();
	}

	@Override
	protected int next(int bits) {
		return (int) nextLong() & ((1 << bits) - 1);
	}

	@Override
	public double nextDouble() {
		return ((nextLong() & ((1L << 53) - 1)) / (double) (1L << 53));
	}

	@Override
	public synchronized long nextLong() {
		call.authNext();
		while (true) {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				Log.e("AuthRandom", "Interrupted, restarting...");
			}
		}
	}

}
