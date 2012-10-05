package org.servalproject.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.servalproject.ServalBatPhoneApplication;

import android.util.Log;

public class SymbolGenerators {
	private static FutureTask<SymbolGenerator[]> task;

	public static void init() {
		task = new FutureTask<SymbolGenerator[]>(
				new Callable<SymbolGenerator[]>() {
					@Override
					public SymbolGenerator[] call() throws Exception {
						List<SymbolGenerator> symgens = new ArrayList<SymbolGenerator>();
						for (String dict : WordGenerator.getDicts()) {
							try {
								symgens.add(new WordGenerator(dict));
							} catch (IOException e) {
								Log.e(ServalBatPhoneApplication.MSG_TAG,
										"Could not load dict "
												+ dict + ": "
												+ e.getMessage());
							}
						}
						return symgens.toArray(new SymbolGenerator[symgens
								.size()]);
					}
				});
		new Thread(task).start();
	}

	public static SymbolGenerator[] get() {
		try {
			return task.get();
		} catch (InterruptedException e) {
			Log.e(ServalBatPhoneApplication.MSG_TAG, e.toString(), e);
		} catch (ExecutionException e) {
			Log.e(ServalBatPhoneApplication.MSG_TAG, e.toString(), e);
		}
		return new SymbolGenerator[0];
	}
}
