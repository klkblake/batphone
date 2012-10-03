package org.servalproject.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Random;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class WordGenerator implements SymbolGenerator {
	private String filename;
	private FileChannel chan;
	private MappedByteBuffer map;
	private int[] index;

	public WordGenerator(String filename) throws IOException {
		// Due to memory constraints on low-end phones, we cannot read the
		// entire dictionary into memory. Additionally, we manually manage the
		// index array instead of using an ArrayList to avoid the significant
		// memory overhead of using Integers instead of ints.
		// The dictionary file must end with a newline.
		this.filename = filename;
		chan = new FileInputStream(
				ServalBatPhoneApplication.context.coretask.DATA_FILE_PATH
						+ "/dict/" + filename).getChannel();
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

	@Override
	public ListAdapter getSymbols(Context context, long seed, int n) {
		Random rand = new Random(seed);
		String[] words = new String[n];
		for (int i = 0; i < n; i++) {
			int start = index[rand.nextInt(index.length)];
			byte[] bytes = null;
			map.position(start);
			map.mark();
			for (int len = 0; map.remaining() > 0; len++) {
				byte c = map.get();
				if (c == '\n') {
					len--;
					map.reset();
					bytes = new byte[len];
					map.get(bytes, 0, len);
					break;
				}
			}
			words[i] = new String(bytes);
		}
		return new ArrayAdapter<String>(context,
				R.layout.rhizome_list_item, words);
	}

	@Override
	public boolean isWide() {
		return true;
	}

	@Override
	public String toString() {
		return "Words (" + filename + " dictionary)";
	}

}
