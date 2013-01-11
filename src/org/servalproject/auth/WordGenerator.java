package org.servalproject.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class WordGenerator implements SymbolGenerator {
	private static final int BLOCK_SIZE = 4;
	private Random rand;
	private Dictionary dict;

	public WordGenerator(Random rand, Dictionary dict) {
		this.rand = rand;
		this.dict = dict;
	}

	public static Map<String, SymbolGeneratorFactory> getFactories() {
		Map<String, SymbolGeneratorFactory> factories = new HashMap<String, SymbolGeneratorFactory>();
		File dictdir = new File(
				ServalBatPhoneApplication.context.coretask.DATA_FILE_PATH
						+ "/dict/");
		for (File file : dictdir.listFiles()) {
			if (file.getName().endsWith(".idx")) {
				continue;
			}
			final Dictionary dict;
			try {
				dict = new Dictionary(file);
			} catch (IOException e) {
				Log.e("WordGeneratorFactory", "Could not load dictionary "
						+ file + ": " + e);
				continue;
			}
			String name = file.getName();
			name = name.substring(0, 1).toUpperCase() + name.substring(1);
			factories.put(
					"Words (" + name + " dictionary, " + dict.size()
							+ " words)",
					new SymbolGeneratorFactory() {
						@Override
						public SymbolGenerator create(Random rand) {
							return new WordGenerator(rand, dict);
						}
					});
		}
		return factories;
	}

	@Override
	public Symbol next() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < BLOCK_SIZE; i++) {
			sb.append(dict.get(rand.nextInt(dict.size())));
			if (i != BLOCK_SIZE - 1) {
				sb.append('\n');
			}
		}
		return new WordSymbol(sb.toString());
	}

	@Override
	public double getEntropy() {
		return BLOCK_SIZE * (Math.log(dict.size()) / Math.log(2));
	}
}

class WordSymbol implements Symbol {
	private String words;

	public WordSymbol(String words) {
		this.words = words;
	}

	@Override
	public View getView(Context context, View convertView) {
		TextView view;
		if (convertView != null) {
			view = (TextView) convertView;
		} else {
			view = new TextView(context);
		}
		view.setText(words);
		view.setBackgroundResource(R.drawable.border);
		return view;
	}

}

class Dictionary {
	private MappedByteBuffer dict;
	private IntBuffer index;

	public Dictionary(File file) throws IOException {
		// Due to memory constraints on low-end phones, we cannot read the
		// entire dictionary into memory.
		// The dictionary file must end with a newline.
		FileChannel chan = new FileInputStream(file).getChannel();
		dict = chan.map(MapMode.READ_ONLY, 0, chan.size());
		chan = new FileInputStream(file.getAbsolutePath() + ".idx")
				.getChannel();
		index = chan.map(MapMode.READ_ONLY, 0, chan.size()).asIntBuffer();
	}

	public StringBuilder get(int idx) {
		StringBuilder sb = new StringBuilder();
		dict.position(index.get(idx));
		byte c = dict.get();
		while (dict.remaining() > 0 && c != '\n') {
			sb.append((char) c);
			c = dict.get();
		}
		return sb;
	}

	public int size() {
		return index.capacity();
	}

}
