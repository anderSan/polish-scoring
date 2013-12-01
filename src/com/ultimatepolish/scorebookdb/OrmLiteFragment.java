package com.ultimatepolish.scorebookdb;

import android.app.Fragment;

import com.j256.ormlite.android.apptools.OpenHelperManager;

public class OrmLiteFragment extends Fragment {

	private DatabaseHelper databaseHelper = null;

	protected DatabaseHelper getHelper() {
		if (databaseHelper == null) {
			databaseHelper = OpenHelperManager.getHelper(getActivity(),
					DatabaseHelper.class);
		}
		return databaseHelper;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (databaseHelper != null) {
			OpenHelperManager.releaseHelper();
			databaseHelper = null;
		}
	}
}