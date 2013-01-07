package org.servalproject.auth;

import java.util.HashMap;
import java.util.Map;

public class SymbolGenerators {
	private static Map<String, SymbolGeneratorFactory> symgens = new HashMap<String, SymbolGeneratorFactory>();

	static {
		symgens.putAll(WordGenerator.getFactories());
	}

	public static Map<String, SymbolGeneratorFactory> get() {
		return symgens;
	}
}
