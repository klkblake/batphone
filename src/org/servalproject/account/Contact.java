package org.servalproject.account;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.auth.AuthState;
import org.servalproject.servald.SubscriberId;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class Contact {
	public static final String SID_FIELD_MIMETYPE_PREFIX = "vnd.android.cursor.item/org.servalproject.sidAuth";
	public static final String SID_FIELD_MIMETYPE_PATTERN = SID_FIELD_MIMETYPE_PREFIX
			+ "%";
	public static final String AUTH_FIELD_MIMETYPE = "vnd.android.cursor.item/org.servalproject.authState";
	public static final String REVOKE = "Revoke Authentication";
	public static final String CALL_MESH = "Call Mesh";
	public static Pattern SUBSTITUTE = Pattern.compile("\\?\\?");

	private static final Map<Long, Contact> contacts = new HashMap<Long, Contact>();

	private ContentResolver resolver;
	private Uri lookupUri = null;
	private Uri rawContactUri = null;
	private Uri rawContactDataUri = null;
	private long rawContactId = -1;
	private SubscriberId sid = null;
	private String did = null;
	private String name = null;
	private AuthState authState = null;
	private boolean changed = false;

	private Contact(ContentResolver resolver) {
		this.resolver = resolver;
	}

	public static Contact getContact(ContentResolver resolver, Uri rawDataUri) {
		Contact contact = new Contact(resolver);
		Cursor cursor = contact.query(rawDataUri, new String[] {
				Data.RAW_CONTACT_ID, Data.CONTACT_ID, Data.LOOKUP_KEY
		}, null);
		try {
			if (cursor != null) {
				contact.setIds(cursor.getLong(0), cursor.getLong(1),
						cursor.getString(2));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return getContact(contact);
	}

	public static Contact getContact(ContentResolver resolver, SubscriberId sid) {
		Contact contact = new Contact(resolver);
		contact.sid = sid;
		Cursor cursor = contact.query(Data.CONTENT_URI,
				new String[] {
						Data.RAW_CONTACT_ID, Data.CONTACT_ID,
						Data.LOOKUP_KEY
				}, "UPPER(??) = ? AND ?? LIKE ?",
				Data.DATA1, Data.MIMETYPE,
				sid.toString(), SID_FIELD_MIMETYPE_PATTERN);
		try {
			if (cursor != null) {
				contact.setIds(cursor.getLong(0), cursor.getLong(1),
						cursor.getString(2));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return getContact(contact);
	}

	public static Contact getContact(ContentResolver resolver, String did) {
		Contact contact = new Contact(resolver);
		contact.did = did;
		Cursor cursor = contact.query(Data.CONTENT_URI,
				new String[] {
						Data.RAW_CONTACT_ID, Data.CONTACT_ID,
						Data.LOOKUP_KEY, Data.IS_SUPER_PRIMARY,
				}, "?? = ? AND ?? = ?",
				Phone.NUMBER, Data.MIMETYPE,
				did, Phone.CONTENT_ITEM_TYPE);
		if (cursor == null) {
			return contact;
		}
		try {
			// XXX There doesn't seem to be a way to do a proper join. This
			// method is inefficient.
			for (cursor.moveToFirst(); cursor.isAfterLast(); cursor
					.moveToNext()) {
				Cursor c = contact.query(RawContacts.CONTENT_URI,
						new String[] {}, "?? = ? AND ?? = ?",
						RawContacts._ID, RawContacts.ACCOUNT_TYPE,
						Long.toString(cursor.getLong(0)),
						AccountService.TYPE);
				try {
					if (c != null) {
						contact.setIds(cursor.getLong(0),
								cursor.getLong(1), cursor.getString(2));
						break;
					}
				} finally {
					if (c != null) {
						c.close();
					}
				}
			}
			if (!contact.isAdded()) {
				long rawId = -1;
				for (cursor.moveToFirst(); cursor.isAfterLast(); cursor
						.moveToNext()) {
					if (cursor.getInt(3) == 1) {
						rawId = cursor.getLong(0);
						break;
					}
				}
				if (rawId == -1 && cursor.moveToFirst()) {
					rawId = cursor.getLong(0);
				}
				contact.setIds(rawId, cursor.getLong(1),
						cursor.getString(2));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return getContact(contact);
	}

	private static synchronized Contact getContact(Contact contact) {
		if (contact.isAdded()) {
			if (contacts.containsKey(contact.rawContactId)) {
				return contacts.get(contact.rawContactId);
			}
			contacts.put(contact.rawContactId, contact);
		}
		return contact;
	}

	private void setIds(long rawId) {
		Cursor cursor = null;
		try {
			cursor = query(Data.CONTENT_URI,
					new String[] {
							Data.CONTACT_ID, Data.LOOKUP_KEY
					}, "?? = ?", Data.RAW_CONTACT_ID, Long.toString(rawId));
			if (cursor != null) {
				setIds(rawId, cursor.getLong(0), cursor.getString(1));
			} else {
				setIds(rawId, -1, null);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void setIds(long rawId, long id, String lookupKey) {
		rawContactId = rawId;
		if (rawId != -1) {
			rawContactUri = Uri.withAppendedPath(RawContacts.CONTENT_URI,
					Long.toString(rawContactId));
			rawContactDataUri = Uri.withAppendedPath(rawContactUri,
					RawContacts.Data.CONTENT_DIRECTORY);
			lookupUri = Contacts.getLookupUri(id, lookupKey);
		}
	}

	public boolean isAdded() {
		return rawContactId != -1;
	}

	public boolean hasChanged() {
		boolean c = changed;
		changed = false;
		if (isAdded()) {
			Cursor cursor = query(rawContactUri, new String[] {
				RawContacts.DELETED
			}, null);
			try {
				if (cursor == null || cursor.getInt(0) == 1) {
					c = true;
					setIds(-1);
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		return c;
	}

	public Uri getUri() {
		return lookupUri;
	}

	// Get a URI that can be used to get this object
	public Uri getRawDataUri() {
		return rawContactDataUri;
	}

	public SubscriberId getSid() {
		if (sid != null || !isAdded()) {
			return sid;
		}
		String sidhex = queryString(Data.DATA1,
				"?? LIKE ?",
				Data.MIMETYPE,
				SID_FIELD_MIMETYPE_PATTERN);
		try {
			sid = new SubscriberId(sidhex);
		} catch (SubscriberId.InvalidHexException e) {
			Log.e("BatPhone", "Invalid SID", e);
		}
		return sid;
	}

	public void setSid(SubscriberId sid) {
		if (!isAdded()) {
			this.sid = sid;
			return;
		}
		ContentValues values = new ContentValues();
		values.put(Data.DATA1, sid.toString());
		values.put(Data.DATA3, sid.abbreviation());
		int count = resolver.update(rawContactDataUri, values, Data.MIMETYPE
				+ " LIKE ?",
				new String[] {
					SID_FIELD_MIMETYPE_PATTERN
				});

		if (count == 0) {
			values.put(Data.MIMETYPE, SID_FIELD_MIMETYPE_PREFIX
					+ getAuthState().name());
			values.put(Data.DATA2, CALL_MESH);
			resolver.insert(rawContactDataUri, values);
		}

		this.sid = sid;
	}

	public String getNumber() {
		if (did != null || !isAdded()) {
			return did;
		}
		did = queryString(Phone.NUMBER, "?? = ?", Data.MIMETYPE,
				Phone.CONTENT_ITEM_TYPE);
		if (did == null) {
			did = queryAggregateString(Phone.NUMBER, "?? = 1 AND ?? = ?",
					Data.IS_SUPER_PRIMARY, Data.MIMETYPE,
					Phone.CONTENT_ITEM_TYPE);
			if (did == null) {
				did = queryAggregateString(Phone.NUMBER, "?? = ?",
						Data.MIMETYPE,
						Phone.CONTENT_ITEM_TYPE);
			}
		}
		return did;
	}

	public void setNumber(String did) {
		if (!isAdded()) {
			this.did = did;
			return;
		}
		ContentValues values = new ContentValues();
		values.put(Phone.NUMBER, did);
		int count = resolver.update(rawContactDataUri, values, Data.MIMETYPE
				+ " LIKE ?",
				new String[] {
					Phone.CONTENT_ITEM_TYPE
				});

		if (count == 0) {
			values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			values.put(Phone.TYPE, Phone.TYPE_MAIN);
			resolver.insert(rawContactDataUri, values);
		}

		this.did = did;
	}

	public String getName() {
		// This can be changed from outside Serval
		if (!isAdded()) {
			return name;
		}
		name = queryString(Contacts.DISPLAY_NAME, null);
		if (name == null) {
			Log.w("BatPhone", "Could not find contact name for "
					+ rawContactUri);
		}
		return name;
	}

	public void setName(String name) {
		String name_ = name == null ? "" : name;
		String curname_ = this.name == null ? "" : this.name;
		if (!name_.equals(curname_)) {
			changed = true;
		}
		if (!isAdded()) {
			this.name = name;
			return;
		}
		ContentValues values = new ContentValues();
		if (name != null && !name.equals("")) {
			values.put(StructuredName.DISPLAY_NAME, name);
		} else {
			values.putNull(StructuredName.DISPLAY_NAME);
		}
		resolver.update(rawContactDataUri, values, Data.MIMETYPE + " LIKE ?",
				new String[] {
					StructuredName.CONTENT_ITEM_TYPE
				});

		this.name = name;
	}

	public Bitmap getPhoto() {
		if (!isAdded()) {
			return null;
		}
		// This can be changed from outside Serval
		try {
			InputStream photo = Contacts.openContactPhotoInputStream(resolver,
					lookupUri);
			return BitmapFactory.decodeStream(photo);
		} catch (Exception e) {
			// catch any security exceptions in APIv14
			Log.e("Contact", e.getMessage(), e);
			return null;
		}
	}

	public AuthState getAuthState() {
		if (authState != null) {
			return authState;
		}
		if (!isAdded()) {
			return AuthState.None;
		}
		String state = queryString(Data.DATA1, "?? = ?", Data.MIMETYPE,
				AUTH_FIELD_MIMETYPE);
		if (state != null) {
			try {
				authState = AuthState.valueOf(state);
			} catch (IllegalArgumentException e) {
				Log.e("BatPhone", "Invalid auth state", e);
				return AuthState.None;
			}
		} else {
			return AuthState.None;
		}
		return authState;
	}

	public void setAuthState(AuthState authState) {
		if (!isAdded()) {
			this.authState = authState;
			return;
		}
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		// Using the rawContactUri for delete operations fails with an
		// UnsupportedOperationException.
		Builder b = ContentProviderOperation.newDelete(Data.CONTENT_URI);
		b.withSelection(
				Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?",
				new String[] {
						Long.toString(rawContactId),
						AUTH_FIELD_MIMETYPE,
				});
		ops.add(b.build());

		if (authState != AuthState.None) {
			b = ContentProviderOperation.newInsert(rawContactDataUri);
			b.withValue(Data.MIMETYPE, AUTH_FIELD_MIMETYPE);
			b.withValue(Data.DATA1, authState.name());
			b.withValue(Data.DATA2, REVOKE);
			b.withValue(Data.DATA3, ServalBatPhoneApplication.context
					.getResources().getString(authState.text));
			ops.add(b.build());
		}

		b = ContentProviderOperation.newUpdate(rawContactDataUri);
		b.withSelection(Data.MIMETYPE + " LIKE ?",
				new String[] {
					SID_FIELD_MIMETYPE_PATTERN
				});
		b.withValue(Data.MIMETYPE, SID_FIELD_MIMETYPE_PREFIX + authState.name());
		ops.add(b.build());

		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, ops);
			this.authState = authState;
		} catch (RemoteException e) {
			Log.e("BatPhone", e.getMessage(), e);
		} catch (OperationApplicationException e) {
			Log.e("BatPhone", e.getMessage(), e);
		}
	}

	public void add(Context context) throws RemoteException,
			OperationApplicationException {
		if (isAdded()) {
			return;
		}
		Account account = AccountService.getAccount(context);
		if (account == null) {
			throw new IllegalStateException();
		}
		Log.i("BatPhone", "Adding contact: " + name + " (" + rawContactUri
				+ ")");
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		// Create our RawContact
		Builder b = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
		b.withValue(RawContacts.ACCOUNT_NAME, account.name);
		b.withValue(RawContacts.ACCOUNT_TYPE, account.type);
		b.withValue(RawContacts.VERSION, 1);
		ops.add(b.build());

		// Create a Data record of common type 'StructuredName' for our
		// RawContact
		b = ContentProviderOperation.newInsert(Data.CONTENT_URI);
		b.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
		b.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		if (name != null && !name.equals("")) {
			b.withValue(StructuredName.DISPLAY_NAME, name);
		}
		ops.add(b.build());

		ContentProviderResult[] results = resolver.applyBatch(
				ContactsContract.AUTHORITY, ops);
		setIds(ContentUris.parseId(results[0].uri));
		if (sid != null) {
			setSid(sid);
		}
		if (did != null) {
			setNumber(did);
		}
		if (authState != null) {
			setAuthState(authState);
		}
		contacts.put(rawContactId, this);
	}

	private Cursor query(Uri uri, String columns[], String where,
			String... params) {
		Cursor cursor;
		if (where == null) {
			cursor = resolver.query(uri, columns, where, params, null);
		} else {
			int i;
			Matcher matcher = SUBSTITUTE.matcher(where);
			StringBuffer sb = new StringBuffer(where.length());
			for (i = 0; matcher.find(); i++) {
				matcher.appendReplacement(sb, params[i]);
			}
			matcher.appendTail(sb);
			String[] remainder = new String[params.length - i];
			System.arraycopy(params, i, remainder, 0, remainder.length);
			cursor = resolver.query(uri, columns, sb.toString(), remainder,
					null);
		}
		if (cursor != null && cursor.moveToNext()) {
			return cursor;
		} else {
			return null;
		}
	}

	private String queryAggregateString(String column, String where,
			String... params) {
		Cursor cursor = query(lookupUri,
				new String[] {
					column
				}, where, params);
		if (cursor == null) {
			return null;
		}
		try {
			return cursor.getString(0);
		} finally {
			cursor.close();
		}
	}

	private Cursor queryContact(String column, String where, String... params) {
		return query(rawContactDataUri,
				new String[] {
					column
				}, where, params);
	}

	private String queryString(String column, String where,
			String... params) {
		Cursor cursor = queryContact(column, where, params);
		if (cursor == null) {
			return null;
		}
		try {
			return cursor.getString(0);
		} finally {
			cursor.close();
		}
	}
}
