package org.servalproject.auth;

import java.util.ArrayList;
import java.util.List;

public class SymbolGenerators {
	private static SymbolGeneratorFactory symgens[];

	static {
		List<SymbolGeneratorFactory> l = new ArrayList<SymbolGeneratorFactory>();

		l.add(WordGeneratorFactory.load("normal"));
		l.add(WordGeneratorFactory.load("basic"));
		l.add(WordGeneratorFactory.load("comprehensive"));

		symgens = l.toArray(new SymbolGeneratorFactory[l.size()]);
	}

	public static SymbolGeneratorFactory[] get() {
		return symgens;
	}
}
