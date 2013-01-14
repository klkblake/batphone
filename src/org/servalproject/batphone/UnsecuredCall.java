package org.servalproject.batphone;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.account.AccountService;
import org.servalproject.auth.AuthIntro;
import org.servalproject.auth.AuthResult;
import org.servalproject.auth.AuthState;
import org.servalproject.servald.PeerListService;
import org.servalproject.servald.SubscriberId;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

public class UnsecuredCall extends Activity {

	private static final int AUTH_REQUEST = 1;

	ServalBatPhoneApplication app;
	CallHandler callHandler;

	private ImageView remote_photo_1;
	private TextView remote_name_1;
	private TextView remote_number_1;
	private TextView callstatus_1;
	private TextView action_1;
	private ImageView remote_photo_2;
	private TextView remote_name_2;
	private TextView remote_number_2;
	private TextView callstatus_2;
	private TextView authState;

	// Create runnable for posting
	final Runnable updateCallStatus = new Runnable() {
		@Override
		public void run() {
			updateUI();
		}
	};
	private Button endButton;
	private Button incomingEndButton;
	private Button incomingAnswerButton;
	private Chronometer chron;
	private Button authButton;

	private String stateSummary()
	{
		return callHandler.local_state.code + "."
				+ callHandler.remote_state.code;
	}

	private void updateUI()
	{
		final Window win = getWindow();
		int incomingCallFlags =
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

		Log.d("VoMPCall", "Updating UI for state " + stateSummary());

		updateAuth();
		showSubLayout();

		if (callHandler.local_state == VoMP.State.RingingIn)
			win.addFlags(incomingCallFlags);
		else
			win.clearFlags(incomingCallFlags);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("VoMPCall", "Activity started");

		app = (ServalBatPhoneApplication) this.getApplication();

		if (app.servaldMonitor==null){
			app.displayToastMessage("Unable to place a call at this time");
			finish();
			return;
		}

		if (app.callHandler == null) {
			SubscriberId sid = null;
			try {
				Intent intent = this.getIntent();
				String action = intent.getAction();

				if (Intent.ACTION_VIEW.equals(action)) {
					// This activity has been triggered from clicking on a SID
					// in contacts.
					Cursor cursor = getContentResolver().query(
								intent.getData(),
								new String[] {
									ContactsContract.Data.DATA1
								},
								ContactsContract.Data.MIMETYPE + " LIKE ?",
								new String[] {
									AccountService.SID_FIELD_MIMETYPE_PATTERN
								},
								null);
					try {
						if (cursor.moveToNext())
							sid = new SubscriberId(cursor.getString(0));
					} finally {
						cursor.close();
					}

				} else {
					String sidString = intent.getStringExtra("sid");
					if (sidString != null)
						sid = new SubscriberId(sidString);
				}

				if (sid == null)
					throw new IllegalArgumentException("Missing argument sid");

				CallHandler.dial(this, PeerListService.getPeer(
						getContentResolver(), sid));

			} catch (Exception e) {
				ServalBatPhoneApplication.context.displayToastMessage(e
						.getMessage());
				Log.e("BatPhone", e.getMessage(), e);
				finish();
				return;
			}
		} else {
			app.callHandler.setCallUI(this);
		}

		this.callHandler = app.callHandler;

		Log.d("VoMPCall", "Setup keepalive timer");

		setContentView(R.layout.call_layered);

		chron = (Chronometer) findViewById(R.id.call_time);
		remote_photo_1 = (ImageView) findViewById(R.id.caller_image);
		remote_name_1 = (TextView) findViewById(R.id.caller_name);
		remote_number_1 = (TextView) findViewById(R.id.ph_no_display);
		callstatus_1 = (TextView) findViewById(R.id.call_status);
		action_1 = (TextView) findViewById(R.id.call_action_type);
		remote_photo_2 = (ImageView) findViewById(R.id.caller_image_incoming);
		remote_name_2 = (TextView) findViewById(R.id.caller_name_incoming);
		remote_number_2 = (TextView) findViewById(R.id.ph_no_display_incoming);
		callstatus_2 = (TextView) findViewById(R.id.call_status_incoming);
		authState = (TextView) findViewById(R.id.auth_state);

		authButton = (Button) findViewById(R.id.auth_button);
		authButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(
						ServalBatPhoneApplication.context, AuthIntro.class),
						AUTH_REQUEST);
			}
		});

		updatePeerDisplay();

		if (callHandler.remotePeer.cacheUntil < SystemClock.elapsedRealtime()) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected void onPostExecute(Void result) {
					updatePeerDisplay();
				}

				@Override
				protected Void doInBackground(Void... params) {
					PeerListService.resolve(callHandler.remotePeer);
					return null;
				}
			}.execute();
		}
		updateUI();

		endButton = (Button) this.findViewById(R.id.cancel_call_button);
		endButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				callHandler.hangup();
			}
		});

		incomingEndButton = (Button) this.findViewById(R.id.incoming_decline);
		incomingEndButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				callHandler.hangup();
			}
		});

		incomingAnswerButton = (Button) this
				.findViewById(R.id.answer_button_incoming);
		incomingAnswerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				callHandler.pickup();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == AUTH_REQUEST) {
			switch (resultCode) {
			case AuthResult.SUCCESS:
				callHandler.remotePeer.authState = AuthState.Voice;
				break;
			case AuthResult.FAILURE:
				callHandler.remotePeer.authState = AuthState.Failed;
				break;
			case AuthResult.CANCELLED:
			case AuthResult.BACK:
				break;
			}
			callHandler.remotePeer.updateAuthState(this);
			updateAuth();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		finishActivity(AUTH_REQUEST);
	}

	private void updatePeerDisplay() {
		if (callHandler.remotePeer.contactId != -1) {
			Bitmap photo = AccountService.getContactPhoto(
					getContentResolver(), callHandler.remotePeer.contactId);
			if (photo != null) {
				remote_photo_1.setImageBitmap(photo);
				remote_photo_2.setImageBitmap(photo);
			}
		}
		remote_name_1.setText(callHandler.remotePeer.getContactName());
		remote_number_1.setText(callHandler.remotePeer.did);
		remote_name_2.setText(callHandler.remotePeer.getContactName());
		remote_number_2.setText(callHandler.remotePeer.did);

		// Update the in call notification, but only if the call is still
		// ongoing.
		if (callHandler.local_state.ordinal() < VoMP.State.CallEnded.ordinal()) {
			Notification inCall = new Notification(
					android.R.drawable.stat_sys_phone_call,
					callHandler.remotePeer.getDisplayName(),
					System.currentTimeMillis());

			Intent intent = new Intent(app, UnsecuredCall.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			inCall.setLatestEventInfo(app, "Serval Phone Call",
					callHandler.remotePeer.getDisplayName(),
					PendingIntent.getActivity(app, 0,
							intent,
							PendingIntent.FLAG_UPDATE_CURRENT));

			NotificationManager nm = (NotificationManager) app
					.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify("Call", 0, inCall);
		}

		updateAuth();
	}

	private void updateAuth() {
		AuthState s = callHandler.remotePeer.authState;
		authState.setText(s.text);
		authState.setTextColor(getResources().getColor(s.color));
		if (s == AuthState.None
				&& callHandler.local_state != VoMP.State.CallEnded
				&& callHandler.local_state != VoMP.State.Error) {
			authButton.setVisibility(View.VISIBLE);
		} else {
			authButton.setVisibility(View.GONE);
		}
		if (callHandler.local_state == VoMP.State.RingingIn ||
				callHandler.local_state == VoMP.State.CallEnded ||
				callHandler.local_state == VoMP.State.Error) {
			finishActivity(AUTH_REQUEST);
		}
	}

	private void showSubLayout() {
		View incoming = findViewById(R.id.incoming);
		View incall = findViewById(R.id.incall);

		chron.setBase(callHandler.getCallStarted());

		switch (callHandler.local_state) {
		case RingingIn:
			callstatus_2
					.setText(getString(callHandler.local_state.displayResource)
							+ " ("
					+ stateSummary()
					+ ")...");
			incall.setVisibility(View.GONE);
			incoming.setVisibility(View.VISIBLE);
			break;

		case NoSuchCall:
		case NoCall:
		case CallPrep:
		case RingingOut:
		case InCall:
			action_1.setText(getString(callHandler.local_state.displayResource));
			callstatus_1
					.setText(getString(callHandler.local_state.displayResource)
							+ " ("
					+ stateSummary()
					+ ")...");
			incall.setVisibility(View.VISIBLE);
			incoming.setVisibility(View.GONE);
			break;

		case CallEnded:
		case Error:
			// The animation when switching to the call ended
			// activity is annoying, but I don't know how to fix it.
			incoming.setVisibility(View.GONE);
			incall.setVisibility(View.GONE);

			Log.d("VoMPCall", "Calling finish()");

			finish();
			callHandler.setCallUI(null);
			break;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		chron.stop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		chron.setBase(callHandler.getCallStarted());
		chron.start();
	}
}
