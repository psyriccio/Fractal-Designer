package com.resonos.apps.fractal.ifs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

public class Loader extends Activity {
	
	public static final long DELAY = 150;
	
	Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock_K);
		super.onCreate(savedInstanceState);
		this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.loader);
		handler.postDelayed(new Runnable() {
			public void run() {
				startActivity(new Intent(Loader.this, Home.class));
				finish();
			}
		}, DELAY);
	}
}