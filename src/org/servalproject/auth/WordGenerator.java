package org.servalproject.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
			final Dictionary dict;
			try {
				dict = new Dictionary(file);
			} catch (IOException e) {
				Log.e("WordGeneratorFactory", "Could not load dictionary "
						+ file + ": " + e);
				continue;
			}
			factories.put("Words (" + file.getName() + " dictionary)",
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
	private FileChannel chan;
	private MappedByteBuffer map;
	private int[] index;

	public Dictionary(File file) throws IOException {
		// Due to memory constraints on low-end phones, we cannot read the
		// entire dictionary into memory. Additionally, we manually manage the
		// index array instead of using an ArrayList to avoid the significant
		// memory overhead of using Integers instead of ints.
		// The dictionary file must end with a newline.
		// TODO The index should be pregenerated
		chan = new FileInputStream(file).getChannel();
		map = chan.map(MapMode.READ_ONLY, 0, chan.size());
		index = new int[1];
		int word = 0;
		int mark = 0;
		while (map.remaining() > 0) {
			byte c = map.get();
			if (c == '\n') {
				index[word++] = mark;
				mark = map.position();
				if (word == index.length) {
					int[] newIndex = new int[index.length * 2];
					System.arraycopy(index, 0, newIndex, 0, index.length);
					index = newIndex;
				}
			}
		}
		if (word < index.length) {
			int[] newIndex = new int[word];
			System.arraycopy(index, 0, newIndex, 0, word);
			index = newIndex;
		}
	}

	public StringBuilder get(int idx) {
		StringBuilder sb = new StringBuilder();
		map.position(index[idx]);
		byte c = map.get();
		while (map.remaining() > 0 && c != '\n') {
			sb.append((char) c);
			c = map.get();
		}
		return sb;
	}

	public int size() {
		return index.length;
	}

}
