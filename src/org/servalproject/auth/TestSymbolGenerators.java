package org.servalproject.auth;

import java.io.IOException;

import org.servalproject.ServalBatPhoneApplication;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;

public class TestSymbolGenerators extends ListActivity {
	public static SymbolGenerator gen = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (gen == null) {
			try {
				gen = new WordGenerator("yawl");
			} catch (IOException e) {
				Log.e(ServalBatPhoneApplication.MSG_TAG, e.toString(), e);
			}
		}
		setListAdapter(gen.getSymbols(this, 4, 50));
	}

}
