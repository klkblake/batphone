package org.servalproject.auth;

import java.util.Random;

public abstract class SymbolGeneratorFactory {
	String description;

	/**
	 * The amount of entropy in each symbol block in bits
	 */
	double entropy;

	/**
	 * Create a SymbolGenerator for a call
	 *
	 * @param rand
	 *            The PRNG to use
	 * @return the new SymbolGenerator
	 */
	public abstract SymbolGenerator create(Random rand);

	@Override
	public String toString() {
		return description;
	}
}
