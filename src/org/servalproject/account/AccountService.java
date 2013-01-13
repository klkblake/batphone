package org.servalproject.account;

import java.util.ArrayList;

import org.servalproject.Main;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.auth.AuthState;
import org.servalproject.servald.SubscriberId;
import org.servalproject.wizard.Wizard;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class AccountService extends Service {
	private static AccountAuthenticator authenticator = null;
	public static final String ACTION_ADD = "org.servalproject.account.add";
	public static final String TYPE = "org.servalproject.account";

	public static final String SID_FIELD_MIMETYPE_PREFIX = "vnd.android.cursor.item/org.servalproject.sidAuth";
	public static final String SID_FIELD_MIMETYPE_PATTERN = SID_FIELD_MIMETYPE_PREFIX
			+ "%";
	public static final String AUTH_FIELD_MIMETYPE = "vnd.android.cursor.item/org.servalproject.authState";
	public static final String REVOKE = "Revoke Authentication";

	public static long getContactId(ContentResolver resolver,
			SubscriberId sid) {
		Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				new String[] {
					ContactsContract.Data.CONTACT_ID
				},
				"UPPER(" + ContactsContract.Data.DATA1 + ") = ? AND "
						+ ContactsContract.Data.MIMETYPE + " LIKE ?",
				new String[] {
						sid.toString(), SID_FIELD_MIMETYPE_PATTERN
				}, null);
		try {
			if (!cursor.moveToNext()) {
				return -1;
			}

			return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	public static long getContactId(ContentResolver resolver,
			String did) {
		Cursor cursor = resolver
				.query(ContactsContract.Data.CONTENT_URI,
						new String[] {
							ContactsContract.Data.CONTACT_ID
						},
						ContactsContract.CommonDataKinds.Phone.NUMBER
								+ " = ? AND " + ContactsContract.Data.MIMETYPE
								+ " = ?",
						new String[] {
								did,
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
						},
						null);

		try {
			if (!cursor.moveToNext())
				return -1;
			return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	public static long getRawContactId(ContentResolver resolver,
			long contactId) {
		Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				new String[] {
					ContactsContract.Data.RAW_CONTACT_ID
				},
				ContactsContract.Data.CONTACT_ID + " = ? AND "
						+ ContactsContract.Data.MIMETYPE + " LIKE ?",
				new String[] {
						Long.toString(contactId), SID_FIELD_MIMETYPE_PATTERN
				}, null);
		try {
			if (!cursor.moveToNext()) {
				return -1;
			}

			return cursor.getLong(0);
		} finally {
			cursor.close();
		}
	}

	public static String getContactName(ContentResolver resolver,
			long contactId) {
		Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
				new String[] {
					ContactsContract.Contacts.DISPLAY_NAME
				},
				"_ID = ?", new String[] {
					Long.toString(contactId)
				}, null);

		try {
			if (!cursor.moveToNext()) {
				Log.w("BatPhone", "Could not find contact name for "
						+ contactId);
				return null;
			}

			return cursor.getString(0);
		} finally {
			cursor.close();
		}
	}

	public static SubscriberId getContactSid(ContentResolver resolver,
			long contactId) {
		Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				new String[] {
					ContactsContract.Data.DATA1
				},
				ContactsContract.Data.CONTACT_ID + " = ? AND "
						+ ContactsContract.Data.MIMETYPE + " LIKE ?",
				new String[] {
						Long.toString(contactId), SID_FIELD_MIMETYPE_PATTERN
				},
				null);
		try {
			if (cursor.moveToNext())
				return new SubscriberId(cursor.getString(0));
		} catch (SubscriberId.InvalidHexException e) {
			Log.e("BatPhone", "Invalid SID", e);
		} finally {
			cursor.close();
		}
		return null;
	}

	public static String getContactNumber(ContentResolver resolver,
			long contactId) {
		long rawId = getRawContactId(resolver, contactId);
		Cursor cursor = resolver
				.query(ContactsContract.Data.CONTENT_URI,
						new String[] {
							ContactsContract.Data.DATA1
						},
						ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
								+ ContactsContract.Data.MIMETYPE + " = ?",
						new String[] {
								Long.toString(rawId),
								ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
						},
						null);
		try {
			if (cursor.moveToNext())
				return cursor.getString(0);
		} finally {
			cursor.close();
		}
		return null;
	}

	public static AuthState getContactAuthState(ContentResolver resolver,
			long contactId) {
		Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI,
				new String[] {
					ContactsContract.Data.DATA1
				},
				ContactsContract.Data.CONTACT_ID + " = ? AND "
						+ ContactsContract.Data.MIMETYPE + " = ?",
				new String[] {
						Long.toString(contactId), AUTH_FIELD_MIMETYPE
				},
				null);
		try {
			if (cursor.moveToNext())
				return AuthState.valueOf(cursor.getString(0));
		} catch (IllegalArgumentException e) {
			Log.e("BatPhone", "Invalid auth state", e);
		} finally {
			cursor.close();
		}
		return AuthState.None;
	}

	public static void setContactAuthState(ContentResolver resolver,
			long contactId, AuthState authState) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		ContentProviderOperation.Builder b = ContentProviderOperation
				.newDelete(ContactsContract.Data.CONTENT_URI);
		b.withSelection(
				ContactsContract.Data.CONTACT_ID + " = ? AND "
						+ ContactsContract.Data.MIMETYPE + " = ?",
				new String[] {
						Long.toString(contactId), AUTH_FIELD_MIMETYPE
				});
		ops.add(b.build());

		if (authState != AuthState.None) {
			b = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			b.withValue(ContactsContract.Data.RAW_CONTACT_ID,
					getRawContactId(resolver, contactId));
			b.withValue(ContactsContract.Data.MIMETYPE, AUTH_FIELD_MIMETYPE);
			b.withValue(ContactsContract.Data.DATA1, authState.name());
			b.withValue(ContactsContract.Data.DATA2, REVOKE);
			b.withValue(
					ContactsContract.Data.DATA3,
					ServalBatPhoneApplication.context.getResources().getString(
							authState.text));
			ops.add(b.build());
		}

		b = ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI);
		b.withSelection(
				ContactsContract.Data.CONTACT_ID + " = ? AND "
						+ ContactsContract.Data.MIMETYPE + " LIKE ?",
				new String[] {
						Long.toString(contactId), SID_FIELD_MIMETYPE_PATTERN
				});
		b.withValue(ContactsContract.Data.MIMETYPE, SID_FIELD_MIMETYPE_PREFIX
				+ authState.name());
		ops.add(b.build());

		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, ops);
		} catch (RemoteException e) {
			Log.e("BatPhone", e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e("BatPhone", e.getMessage(), e);
		}
	}

	public static Account getAccount(Context context) {
		AccountManager manager = AccountManager.get(context);
		Account[] accounts = manager.getAccountsByType(AccountService.TYPE);
		if (accounts == null || accounts.length == 0)
			return null;
		return accounts[0];
	}

	public static long addContact(Context context, String name,
			SubscriberId sid, String did, AuthState authState)
			throws RemoteException, OperationApplicationException {
		ContentResolver resolver = context.getContentResolver();
		Account account = getAccount(context);
		if (account == null)
			throw new IllegalStateException();
		return addContact(resolver, account, name, sid, did, authState);
	}

	public static long addContact(ContentResolver resolver, Account account,
			String name, SubscriberId sid, String did, AuthState authState)
			throws RemoteException, OperationApplicationException {
		Log.i("BatPhone", "Adding contact: " + name);
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

		// Create our RawContact
		ContentProviderOperation.Builder builder = ContentProviderOperation
				.newInsert(RawContacts.CONTENT_URI);
		builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
		builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
		builder.withValue(RawContacts.VERSION, 1);
		operationList.add(builder.build());

		// Create a Data record of common type 'StructuredName' for our
		// RawContact
		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(
				ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID,
				0);
		builder.withValue(
				ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		if (name != null && !name.equals(""))
			builder.withValue(
					ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
					name);
		operationList.add(builder.build());

		// Create a Data record for the subscriber id
		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE,
				SID_FIELD_MIMETYPE_PREFIX + authState.name());
		builder.withValue(ContactsContract.Data.DATA1, sid.toString());
		builder.withValue(ContactsContract.Data.DATA2, "Call Mesh");
		builder.withValue(ContactsContract.Data.DATA3, sid.abbreviation());
		operationList.add(builder.build());

		// Create a Data record for their phone number
		builder = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE,
				ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, did);
		builder.withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
				ContactsContract.CommonDataKinds.Phone.TYPE_MAIN);
		operationList.add(builder.build());

		// Create a Data record for the authentication state
		if (authState != AuthState.None) {
			builder = ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI);
			builder.withValueBackReference(
					ContactsContract.Data.RAW_CONTACT_ID, 0);
			builder.withValue(ContactsContract.Data.MIMETYPE,
					AUTH_FIELD_MIMETYPE);
			builder.withValue(ContactsContract.Data.DATA1, authState.name());
			builder.withValue(ContactsContract.Data.DATA2, REVOKE);
			builder.withValue(
					ContactsContract.Data.DATA3,
					ServalBatPhoneApplication.context.getResources().getText(
							authState.text));
			operationList.add(builder.build());
		}

		resolver.applyBatch(
				ContactsContract.AUTHORITY,
				operationList);

		return getContactId(resolver, sid);
	}

	private class AccountAuthenticator extends AbstractAccountAuthenticator {
		Context context;

		public AccountAuthenticator(Context context) {
			super(context);
			this.context = context;
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,
				String accountType, String authTokenType,
				String[] requiredFeatures, Bundle options)
				throws NetworkErrorException {

			Intent intent = new Intent(context, Wizard.class);
			intent.setAction(ACTION_ADD);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
					response);
			Bundle reply = new Bundle();
			reply.putParcelable(AccountManager.KEY_INTENT, intent);
			return reply;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,
				Account account, Bundle options) throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,
				String accountType) {
			Intent intent = new Intent(context, Main.class);
			Bundle reply = new Bundle();
			reply.putParcelable(AccountManager.KEY_INTENT, intent);
			return reply;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,
				Account account, String[] features)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (intent.getAction().equals(
				AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
			if (authenticator == null)
				authenticator = new AccountAuthenticator(this);
			return authenticator.getIBinder();
		}
		return null;
	}

}
