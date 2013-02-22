package org.servalproject.auth;

import org.servalproject.R;
import org.servalproject.account.Contact;
import org.servalproject.servald.Peer;
import org.servalproject.servald.PeerListService;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class RevokeAuth extends Activity {
	public static final String TAG = "RevokeAuth";
	private Peer peer;
	private ImageView photo;
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
		Contact contact = Contact.getContact(r, getIntent().getData());
		peer = PeerListService.getPeer(getContentResolver(), contact.getSid());

		photo = (ImageView) findViewById(R.id.auth_revoke_image);
		name = (TextView) findViewById(R.id.auth_revoke_name);
		number = (TextView) findViewById(R.id.auth_revoke_number);

		((Button) findViewById(R.id.auth_revoke_confirm))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						peer.setAuthState(AuthState.None);
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
		Bitmap contactPhoto = peer.contact.getPhoto();
		if (contactPhoto != null) {
			photo.setImageBitmap(contactPhoto);
		}

		String contactName = peer.getName();
		if (contactName == null || contactName.equals("")) {
			contactName = peer.sid.abbreviation();
		}
		name.setText(contactName);

		number.setText(peer.getDid());
	}
}
