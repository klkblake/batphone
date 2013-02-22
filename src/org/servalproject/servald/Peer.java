/**
 * Copyright (C) 2011 The Serval Project
 *
 * This file is part of Serval Software (http://www.servalproject.org)
 *
 * Serval Software is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.servalproject.servald;

import org.servalproject.account.Contact;
import org.servalproject.auth.AuthState;

import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;


public class Peer implements IPeer {
	public Contact contact;
	public long cacheUntil = 0;
	public long lastSeen = 0;
	public boolean reachable = false;
	public final SubscriberId sid;

	// every peer must have a sid
	Peer(ContentResolver resolver, SubscriberId sid) {
		this.sid = sid;
		contact = Contact.getContact(resolver, sid);
	}

	@Override
	public String getSortString() {
		return getName() + getDid() + sid;
	}

	public String getDisplayName() {
		String name = getName();
		if (!name.equals("")) {
			return name;
		}
		String did = getDid();
		if (did != null && !did.equals(""))
			return did;
		return sid.abbreviation();
	}

	@Override
	public boolean hasName() {
		return !getName().equals("");
	}

	public String getName() {
		String name = contact.getName();
		if (name != null)
			return name;
		return "";
	}

	// Ignore the resolved name if the contact has a name already
	public void setName(String name) {
		if (!hasName()) {
			contact.setName(name);
		}
	}

	public void setContactName(String contactName) {
		contact.setName(contactName);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof Peer))
			return false;
		Peer other = (Peer) o;
		return this.sid.equals(other.sid);
	}

	@Override
	public int hashCode() {
		return sid.hashCode();
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

	@Override
	public String getDid() {
		return contact.getNumber();
	}

	public void setDid(String did) {
		contact.setNumber(did);
	}

	public boolean stillAlive() {
		return reachable;
	}

	@Override
	public SubscriberId getSubscriberId() {
		return sid;
	}

	public AuthState getAuthState() {
		return contact.getAuthState();
	}

	public void setAuthState(AuthState authState) {
		contact.setAuthState(authState);
	}

	@Override
	public Contact getContact() {
		return contact;
	}

	@Override
	public void addContact(Context context) throws RemoteException,
			OperationApplicationException {
		contact.add(context);
	}

}
