package org.servalproject.auth;

import org.servalproject.R;

public enum AuthState {
	Failed(R.string.auth_state_failed, R.color.red),
	None(R.string.auth_state_none, R.color.red),
	Voice(R.string.auth_state_voice, R.color.orange),
	Full(R.string.auth_state_full, R.color.green);

	public final int text;
	public final int color;

	AuthState(int text, int color) {
		this.text = text;
		this.color = color;
	}
}
