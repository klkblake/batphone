package org.servalproject.auth;

import android.app.ListActivity;
import android.os.Bundle;

public class TestSymbolGenerators extends ListActivity {
	static SymbolGenerator gen = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (gen == null) {
			gen = new WordGenerator();
		}
		setListAdapter(gen.getSymbols(this, 4, 50));
	}

}
