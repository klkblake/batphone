package org.servalproject.auth;

import android.content.Context;
import android.widget.ListAdapter;

public interface SymbolGenerator {
	/**
	 * Generate a set of n random symbols wrapped in an adapter
	 *
	 * @param context
	 *            The context to pass to the adapter
	 * @param seed
	 *            The seed for the PRNG
	 * @param n
	 *            The number of symbols to generate
	 * @return an adapter wrapping the generated symbols
	 */
	ListAdapter getSymbols(Context context, long seed, int n);

	/**
	 * Hint as to whether each symbol should be displayed on it's own line
	 *
	 * XXX This may possibly be removable, if GridView is smart enough.
	 */
	boolean isWide();
}
