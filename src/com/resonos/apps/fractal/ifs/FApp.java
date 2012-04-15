package com.resonos.apps.fractal.ifs;

import java.util.Map;

import android.os.Bundle;

import com.resonos.apps.library.App;

public class FApp extends App {
	
	// contstants
	private static final String APP_NAME = "Fractal Designer";
	private static final String FLURRY_ID = "HXT67A2SFD7NCNHH8ID3";
	private static final String PACKAGE_PRO = "com.resonos.apps.fractal.ifs.pro";
	private static final String CONTACT_EMAIL = "help@resonos.com";
	private static final String ERROR_URL = "http://core.resonos.com/error.php";
	private static final String INFO_URL = "http://static.resonos.com/info/fractaldesigner.htm";
	public static boolean DEBUG = false;
	
	// context
	Home _home;

	public FApp(Bundle savedInstanceState, Home home) {
		super(savedInstanceState, home);
		_home = home;
	}
	
	@Override
	protected void getAppParameters(AppInfo ai) {
		ai.appName = APP_NAME;
		ai.contactEmailHelp = CONTACT_EMAIL;
		ai.debug = DEBUG;
		ai.flurryID = FLURRY_ID;
		ai.mFragmentContainerID = R.id.container;
		ai.mRootID = R.id.master;
		ai.packageProName = PACKAGE_PRO;
		ai.useExecutorService = true;
		ai.useNetworkClient = true;
		ai.useNetworkGetInfo = true;
		ai.errorURL = ERROR_URL;
		ai.infoURL = INFO_URL;
		ai.askUserToRateEvery = 0;
	}

	@Override
	protected void onGetInfo(boolean success, String response,
			boolean oldVersion, Map<String, String> info) {
		if (oldVersion) {
			if (_home.fM != null)
				if (!_home.fM.isPaused())
					_home.fM.onOldVersion(mNewVersionID);
		}
	}

	@Override
	protected void onNewVersionFirstLoad() {
		mHandler.postDelayed(toastVersionChanged, 250);
	}
	
	Runnable toastVersionChanged = new Runnable() {
		public void run() {
			tooltipToast(R.string.txt_version_changes);
		}
	};
}
