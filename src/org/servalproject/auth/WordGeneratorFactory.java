package org.servalproject.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Random;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class WordGeneratorFactory extends SymbolGeneratorFactory {
	public static final String DICT_PATH = ServalBatPhoneApplication.context.coretask.DATA_FILE_PATH
			+ "/dict/";
	private static final int BLOCK_SIZE = 4;
	private Dictionary dict;

	public static WordGeneratorFactory load(String dictFile) {
		WordGeneratorFactory wgf = new WordGeneratorFactory();
		Dictionary dict;
		try {
			dict = new Dictionary(DICT_PATH + dictFile);
		} catch (IOException e) {
			Log.e("WordGeneratorFactory", "Could not load dictionary: "
					+ dictFile, e);
			return null;
		}
		wgf.dict = dict;
		wgf.entropy = BLOCK_SIZE * dict.entropy();
		wgf.description = String
				.format(
						"%s dictionary\n%d words, %.2f bits/word group",
						Character.toUpperCase(dictFile.charAt(0))
								+ dictFile.substring(1),
						dict.size(),
						wgf.entropy);
		return wgf;
	}

	@Override
	public SymbolGenerator create(final Random rand) {
		return new SymbolGenerator() {
			@Override
			public Symbol next() {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < WordGeneratorFactory.BLOCK_SIZE; i++) {
					sb.append(dict.get(rand.nextInt(dict.size())));
					if (i != WordGeneratorFactory.BLOCK_SIZE - 1) {
						sb.append('\n');
					}
				}
				final String words = sb.toString();
				return new Symbol() {
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
				};
			}
		};
	}
}

class Dictionary {
	private MappedByteBuffer dict;
	private IntBuffer index;

	public Dictionary(String file) throws IOException {
		// Due to memory constraints on low-end phones, we cannot read the
		// entire dictionary into memory.
		// The dictionary file must end with a newline.
		FileChannel chan = new FileInputStream(file).getChannel();
		dict = chan.map(MapMode.READ_ONLY, 0, chan.size());
		chan = new FileInputStream(file + ".idx")
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

	public double entropy() {
		return Math.log(size()) / Math.log(2);
	}

	public int size() {
		return index.capacity();
	}
}
