package org.servalproject.auth;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class SymbolArrayAdapter implements ListAdapter {
	private Context context;
	private Symbol[] symbols;
	private DataSetObservable dso = new DataSetObservable();

	public SymbolArrayAdapter(Context context, Symbol[] symbols) {
		this.context = context;
		this.symbols = symbols;
	}

	public void notifyChanged() {
		dso.notifyChanged();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		dso.registerObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		dso.unregisterObserver(observer);
	}

	@Override
	public int getCount() {
		return symbols.length;
	}

	@Override
	public Object getItem(int position) {
		return symbols[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return symbols[position].getView(context, convertView);
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return symbols.length == 0;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

}
