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
	private static FutureTask<SymbolGeneratorFactory[]> task;

	public static void init() {
		task = new FutureTask<SymbolGeneratorFactory[]>(
				new Callable<SymbolGeneratorFactory[]>() {
					@Override
					public SymbolGeneratorFactory[] call() throws Exception {
						List<SymbolGeneratorFactory> symgens = new ArrayList<SymbolGeneratorFactory>();
						for (String dict : WordGeneratorFactory.getDicts()) {
							try {
								symgens.add(new WordGeneratorFactory(dict));
							} catch (IOException e) {
								Log.e(ServalBatPhoneApplication.MSG_TAG,
										"Could not load dict "
												+ dict + ": "
												+ e.getMessage());
							}
						}
						return symgens
								.toArray(new SymbolGeneratorFactory[symgens
								.size()]);
					}
				});
		new Thread(task).start();
	}

	public static SymbolGeneratorFactory[] get() {
		try {
			return task.get();
		} catch (InterruptedException e) {
			Log.e(ServalBatPhoneApplication.MSG_TAG, e.toString(), e);
		} catch (ExecutionException e) {
			Log.e(ServalBatPhoneApplication.MSG_TAG, e.toString(), e);
		}
		return new SymbolGeneratorFactory[0];
	}
}
