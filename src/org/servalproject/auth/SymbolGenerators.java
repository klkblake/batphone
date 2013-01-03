package org.servalproject.auth;

import java.util.HashMap;
import java.util.Map;

public class SymbolGenerators {
	private static Map<String, SymbolGeneratorFactory> symgens = new HashMap<String, SymbolGeneratorFactory>();

	public static void init() {
		// XXX We should find a way to reduce the init time so we don't need a
		// new thread.
		new Thread() {
			@Override
			public void run() {
				symgens.putAll(WordGenerator.getFactories());
			}
		}.start();
	}

	public static Map<String, SymbolGeneratorFactory> get() {
		return symgens;
	}
}
