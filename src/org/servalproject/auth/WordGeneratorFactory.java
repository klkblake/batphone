package org.servalproject.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Random;

import org.servalproject.ServalBatPhoneApplication;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class WordGeneratorFactory implements SymbolGeneratorFactory {
	private String filename;
	private FileChannel chan;
	private MappedByteBuffer map;
	private int[] index;

	public WordGeneratorFactory(String filename) throws IOException {
		// Due to memory constraints on low-end phones, we cannot read the
		// entire dictionary into memory. Additionally, we manually manage the
		// index array instead of using an ArrayList to avoid the significant
		// memory overhead of using Integers instead of ints.
		// The dictionary file must end with a newline.
		// TODO The index should be pregenerated
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
	public SymbolGenerator getSymbolGenerator(Random rand) {
		return new WordGenerator(rand);
	}

	@Override
	public String toString() {
		return "Words (" + filename + " dictionary)";
	}

	public static String[] getDicts() {
		File dictdir = new File(
				ServalBatPhoneApplication.context.coretask.DATA_FILE_PATH
						+ "/dict/");
		return dictdir.list();
	}

	public class WordGenerator implements SymbolGenerator {
		private static final int BLOCK_SIZE = 4;
		private Random rand;
		private ByteBuffer map;

		public WordGenerator(Random rand) {
			this.rand = rand;
			this.map = WordGeneratorFactory.this.map.asReadOnlyBuffer();
		}

		@Override
		public View getSymbolBlock(Context context) {
			TextView view = new TextView(context);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < BLOCK_SIZE; i++) {
				sb.append(getWord());
				if (i != BLOCK_SIZE - 1) {
					sb.append('\n');
				}
			}
			view.setText(sb);
			return view;
		}

		private String getWord() {
			int start = index[rand.nextInt(index.length)];
			byte[] bytes = null;
			map.position(start);
			map.mark();
			for (int len = 0; map.remaining() > 0; len++) {
				byte c = map.get();
				if (c == '\n') {
					map.reset();
					bytes = new byte[len];
					map.get(bytes, 0, len);
					break;
				}
			}
			return new String(bytes);
		}

		@Override
		public double getEntropy() {
			return BLOCK_SIZE * (Math.log(index.length) / Math.log(2));
		}
	}


}
