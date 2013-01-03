package org.servalproject.auth;

import java.util.Random;

public interface SymbolGeneratorFactory {
	/**
	 * Create a SymbolGenerator for a call
	 *
	 * @param rand
	 *            The PRNG to use
	 * @return the new SymbolGenerator
	 */
	SymbolGenerator create(Random rand);
}
