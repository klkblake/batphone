package org.servalproject.auth;

import android.content.Context;
import android.view.View;

public interface SymbolGenerator {
	/**
	 * Generate a view containing some random symbols
	 *
	 * The random symbols will be considered one unit for the purposes of error
	 * correction. Hence, the number of symbols should be inversely proportional
	 * to the probability of human error in reading a symbol.
	 *
	 * @param context
	 *            The context to pass to the view
	 * @return a view containing the generated symbol
	 */
	View getSymbolBlock(Context context);
}
