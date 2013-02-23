package org.servalproject.auth;

import org.servalproject.R;
import org.servalproject.batphone.UnsecuredCall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AuthFinished extends Activity {
	public static final String EXTRA_SUCCEEDED = "org.servalproject.auth.succeeded";
	public static final String EXTRA_IN_CONTACTS = "org.servalproject.auth.in_contacts";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.auth_finished);
		final boolean succeeded = getIntent().getBooleanExtra(EXTRA_SUCCEEDED,
				false);
		setResult(succeeded ? AuthResult.SUCCESS : AuthResult.FAILURE);

		TextView title = (TextView) findViewById(R.id.auth_finished_title);
		TextView text = (TextView) findViewById(R.id.auth_finished_text);
		final TextView contactText = (TextView) findViewById(R.id.auth_contact_text);
		final Button contactButton = (Button) findViewById(R.id.auth_contact_button);

		if (getIntent().getBooleanExtra(EXTRA_IN_CONTACTS, false)) {
			contactText.setVisibility(View.GONE);
			contactButton.setVisibility(View.GONE);
		}

		contactButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent data = new Intent();
				data.putExtra(UnsecuredCall.EXTRA_ADD_TO_CONTACTS, true);
				setResult(succeeded ? AuthResult.SUCCESS : AuthResult.FAILURE,
						data);
				contactText.setVisibility(View.GONE);
				contactButton.setVisibility(View.GONE);
			}
		});
		((Button) findViewById(R.id.auth_finished_button))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
					}
				});

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
