package org.servalproject.auth;


public class AuthToken {
	private byte data[];

	public AuthToken(String hexData) {
		data = new byte[hexData.length() / 2];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (decodeNibble(hexData.charAt(i * 2)) << 4
					| decodeNibble(hexData.charAt(i * 2 + 1)));
		}

	}

	public byte[] getData() {
		return data;
	}

	private byte decodeNibble(char hex) {
		if (hex == ' ') {
			return 0;
		}
		byte num = (byte) (hex - '0');
		if (num >= 0 && num <= 9) {
			return num;
		}
		byte letter = (byte) (hex - 'a');
		if (letter >= 0 && letter <= 6) {
			return (byte) (letter + 10);
		}
		letter = (byte) (hex - 'A');
		if (letter >= 0 && letter <= 6) {
			return (byte) (letter + 10);
		}
		throw new NumberFormatException("Illegal hex char: " + hex);
	}
}
