package org.servalproject.account;

import org.servalproject.Main;
import org.servalproject.wizard.Wizard;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;

public class AccountService extends Service {
	private static AccountAuthenticator authenticator = null;
	public static final String ACTION_ADD = "org.servalproject.account.add";
	public static final String TYPE = "org.servalproject.account";

	public static Account getAccount(Context context) {
		AccountManager manager = AccountManager.get(context);
		Account[] accounts = manager.getAccountsByType(AccountService.TYPE);
		if (accounts == null || accounts.length == 0)
			return null;
		return accounts[0];
	}

	public static void enableSync(Account account) {
		ContentResolver.setIsSyncable(account,
				ContactsContract.AUTHORITY, 1);
		ContentResolver.setSyncAutomatically(account,
				ContactsContract.AUTHORITY, true);
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
