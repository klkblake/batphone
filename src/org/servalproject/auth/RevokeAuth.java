package org.servalproject.auth;

import org.servalproject.R;
import org.servalproject.account.AccountService;
import org.servalproject.servald.Peer;
import org.servalproject.servald.PeerListService;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RevokeAuth extends Activity {
	public static final String TAG = "RevokeAuth";
	private long contactId;
	private Peer peer;
	private TextView name;
	private TextView number;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		String action = intent.getAction();
		if (!action.equals(Intent.ACTION_VIEW)) {
			Log.e(TAG, "Invoked with invalid action: " + action);
			finish();
		}
		Uri data = intent.getData();
		if (!data.getScheme().equals("content")
				|| !data.getAuthority().equals(ContactsContract.AUTHORITY)) {
			Log.e(TAG, "Invoked with invalid data: " + data);
			finish();
		}
		setContentView(R.layout.auth_revoke);
		ContentResolver r = getContentResolver();
		Cursor c = r.query(getIntent().getData(), new String[] {
			ContactsContract.Data.CONTACT_ID
		}, null, null, null);
		if (c.moveToNext() == false) {
			Log.e(TAG, "Could not get associated contact for " + data);
			finish();
		}
		contactId = c.getLong(0);
		peer = PeerListService.getPeer(getContentResolver(),
				AccountService.getContactSid(r, contactId));

		name = (TextView) findViewById(R.id.auth_revoke_name);
		number = (TextView) findViewById(R.id.auth_revoke_number);

		((Button) findViewById(R.id.auth_revoke_confirm))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						peer.authState = AuthState.None;
						peer.updateAuthState(RevokeAuth.this);
						finish();
					}
				});

		((Button) findViewById(R.id.auth_revoke_cancel))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
					}
				});

		updateContactDisplay();

		if (peer.cacheUntil < SystemClock.elapsedRealtime()) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected void onPostExecute(Void result) {
					updateContactDisplay();
				}

				@Override
				protected Void doInBackground(Void... params) {
					PeerListService.resolve(peer);
					return null;
				}
			}.execute();
		}
	}

	private void updateContactDisplay() {
		String contactName = peer.getContactName();
		if (contactName == null || contactName.equals("")) {
			contactName = peer.sid.abbreviation();
		}
		name.setText(contactName);

		String contactNumber = peer.did;
		if (contactNumber == null || contactNumber.equals("")) {
			contactNumber = AccountService.getContactNumber(
					getContentResolver(), contactId);
		}
		number.setText(contactNumber);
	}
}
