package org.servalproject.servald;

import java.nio.ByteBuffer;

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
