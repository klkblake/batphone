package org.servalproject.auth;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class WordGenerator implements SymbolGenerator {

	private String[] dict;

	public WordGenerator() {
		try {
			BufferedReader r = new BufferedReader(new FileReader(
					ServalBatPhoneApplication.context.coretask.DATA_FILE_PATH
							+ "/dict/yawl"));
			List<String> words = new ArrayList<String>();
			String line;
			while ((line = r.readLine()) != null) {
				words.add(line);
			}
			dict = words.toArray(new String[words.size()]);
		} catch (IOException e) {
			Log.e("BatPhone", e.toString());
			dict = new String[] {
					"aardvark",
					"bee",
					"car",
					"dog",
					"elephant",
					"flower",
					"gorilla",
					"hill",
					"iceberg",
					"juice",
					"klaxon",
					"lemonade",
					"moonshine",
					"nootropics",
					"octopus",
					"pterodactyl",
					"queen",
					"resource",
					"stylus",
					"terror",
					"universal",
					"vulnerable",
					"weeds",
					"xylophone",
					"yellow",
					"zebra",
			};
		}
	}

	@Override
	public ListAdapter getSymbols(Context context, long seed, int n) {
		Random rand = new Random(seed);
		String[] words = new String[n];
		for (int i = 0; i < n; i++) {
			words[i] = dict[rand.nextInt(dict.length)];
		}
		return new ArrayAdapter<String>(context,
				R.layout.rhizome_list_item, words);
	}

	@Override
	public boolean isWide() {
		return true;
	}

}
