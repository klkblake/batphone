package org.servalproject.auth;


public interface SymbolGenerator {
	/**
	 * Generate a random symbol
	 *
	 * The random symbols will be considered one unit for the purposes of error
	 * correction. Hence, the number of symbols should be inversely proportional
	 * to the probability of human error in reading a symbol.
	 *
	 * @return a view containing the generated symbol
	 */
	Symbol next();

	/**
	 * Return the amount of entropy in each symbol block
	 *
	 * @return the amount of entropy in bits
	 */
	double getEntropy();
}
