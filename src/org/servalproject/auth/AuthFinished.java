package org.servalproject.auth;

import org.servalproject.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AuthFinished extends Activity {
	public static final String EXTRA_SUCCEEDED = "org.servalproject.auth.succeeded";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_finished);
		TextView title = (TextView) findViewById(R.id.auth_finished_title);
		TextView text = (TextView) findViewById(R.id.auth_finished_text);
		((Button) findViewById(R.id.auth_finished_button))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
					}
				});
		boolean succeeded = getIntent().getBooleanExtra(EXTRA_SUCCEEDED, false);
		setResult(succeeded ? AuthResult.SUCCESS : AuthResult.FAILURE);
		if (succeeded) {
			title.setText(R.string.auth_succeeded_title);
			title.setTextColor(getResources().getColor(R.color.green));
			text.setText(R.string.auth_succeeded_text);
		} else {
			title.setText(R.string.auth_failed_title);
			title.setTextColor(getResources().getColor(R.color.red));
			text.setText(R.string.auth_failed_text);
		}
	}

}
