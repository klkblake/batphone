package org.servalproject.auth;

import android.app.Activity;

public class AuthResult {
	public static final int SUCCESS = Activity.RESULT_OK;
	public static final int FAILURE = Activity.RESULT_FIRST_USER;
	public static final int CANCELLED = Activity.RESULT_FIRST_USER + 1;
	public static final int BACK = Activity.RESULT_CANCELED;
}
