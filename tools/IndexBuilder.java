import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class IndexBuilder {
	private static final String SUFFIX = ".idx";

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: IndexBuilder <file|dir>");
			System.exit(1);
		}
		File arg = new File(args[0]);
		try {
			if (arg.isDirectory()) {
				for (File file : arg.listFiles()) {
					if (!file.getName().endsWith(".idx")) {
						buildIndex(file);
					}
				}
			} else {
				buildIndex(arg);
			}
		} catch (IOException e) {
			System.err.println("Building indexes failed: " + e);
			System.exit(1);
		}
	}

	private static void buildIndex(File dict) throws IOException {
		DataOutputStream out = new DataOutputStream(new FileOutputStream(dict.getAbsolutePath() + SUFFIX));
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(dict));
		boolean readNewline = true;
		int idx = -1;
		int c = in.read();
		while (c != -1) {
			idx++;
			if (readNewline) {
				out.writeInt(idx);
				readNewline = false;
			}
			if (c == '\n') {
				readNewline = true;
			}
			c = in.read();
		}
		in.close();
		out.close();
	}

}
