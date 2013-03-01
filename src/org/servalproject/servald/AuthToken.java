package org.servalproject.servald;

import java.nio.ByteBuffer;

// Although an AuthToken is 64 bytes long, it only has 320 bits of entropy.
public class AuthToken extends AbstractId {
	@Override
	int getBinarySize() {
		return 64;
	}

	public AuthToken(String hex) throws InvalidHexException {
		super(hex);
	}

	public AuthToken(ByteBuffer b) throws InvalidBinaryException {
		super(b);
	}

	public AuthToken(byte[] binary) throws InvalidBinaryException {
		super(binary);
	}
}
