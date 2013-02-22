package org.servalproject.account;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class SyncService extends Service {
	private static SyncAdapter syncAdapter = null;

	private class SyncAdapter extends AbstractThreadedSyncAdapter {
		public SyncAdapter(Context context, boolean autoInitialize) {
			super(context, autoInitialize);
		}

		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			try {
				provider.delete(
						RawContacts.CONTENT_URI
								.buildUpon()
								.appendQueryParameter(
										ContactsContract.CALLER_IS_SYNCADAPTER,
										"true").build(),
						RawContacts.ACCOUNT_TYPE + " = ? AND "
								+ RawContacts.DELETED + " = 1",
						new String[] {
							account.type
						});
			} catch (RemoteException e) {
				Log.e("BatphoneSync",
						"Could not delete accounts marked for deletion", e);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if ("android.content.SyncAdapter".equals(intent.getAction())) {
			if (syncAdapter == null)
				syncAdapter = new SyncAdapter(this, false);

			return syncAdapter.getSyncAdapterBinder();
		}
		return null;
	}

}
