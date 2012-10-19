package org.servalproject.auth;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

public class ViewArrayAdapter implements ListAdapter {
	private Context context;
	private View[] views;
	private DataSetObservable dso = new DataSetObservable();
	private int background;

	public ViewArrayAdapter(Context context, View[] views, int background) {
		this.context = context;
		this.views = views;
		this.background = background;
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
		return views.length;
	}

	@Override
	public Object getItem(int position) {
		return views[position];
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
		FrameLayout view;
		if (convertView != null) {
			view = (FrameLayout) convertView;
		} else {
			view = new FrameLayout(context);
		}
		view.setBackgroundResource(background);
		view.removeAllViews();
		view.addView(views[position]);
		return view;
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
		return views.length == 0;
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
