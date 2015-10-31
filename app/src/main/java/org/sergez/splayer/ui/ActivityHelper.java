package org.sergez.splayer.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.sergez.splayer.R;


/**
 * @author Sergii Zhuk
 *         Date: 31.10.2014
 *         Time: 14:28
 */
public final class ActivityHelper {
	private static final String TAG = ActivityHelper.class.getName();

	private ActivityHelper() {

	}

	public static void navigateTo(FragmentActivity activity, final Fragment fragment, int containerId) {
		navigateTo(activity, fragment, null, true, false, containerId);
	}

	public static void navigateTo(FragmentActivity activity, final Fragment fragment, final boolean addToBackStack, int containerId) {
		navigateTo(activity, fragment, null, addToBackStack, false,  containerId);
	}

	public static void navigateTo(FragmentActivity activity,
	                              final Fragment fragment,
	                              final FragmentTransaction transaction,
	                              final boolean addToBackStack,
	                              final boolean allowStateLoss,
	                              int containerId) {
		final FragmentManager fm = activity.getSupportFragmentManager();
		final FragmentTransaction ft = transaction == null ? fm.beginTransaction() : transaction;
		ft.replace(containerId, fragment, fragment.getClass().getName());
		if (addToBackStack) {
			ft.addToBackStack(fragment.getClass().getName());
		}

		commitTransaction(ft, allowStateLoss);
	}


	private static void commitTransaction(final FragmentTransaction ft, final boolean allowStateLoss) {
		if (allowStateLoss) {
			ft.commitAllowingStateLoss();
		} else {
			ft.commit();
		}
	}

	public static int getMainActivityContainer() {
		return R.id.container_main;
	}

}
