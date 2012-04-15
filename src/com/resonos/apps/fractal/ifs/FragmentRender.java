package com.resonos.apps.fractal.ifs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.model.IFSFractal;
import com.resonos.apps.fractal.ifs.util.IFSFile;
import com.resonos.apps.fractal.ifs.view.FractalView;
import com.resonos.apps.fractal.ifs.view.ToolBarRender;
import com.resonos.apps.fractal.ifs.view.ToolBarRenderQuality;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.media.MediaScannerNotifier;
import com.resonos.apps.library.util.AppUtils;
import com.resonos.apps.library.util.M;

/**
 * This class is a Fragment that shows and allows the user to interact
 * with a Fractal render using a {@link FractalView}
 * @author Chris
 */
public class FragmentRender extends BaseFragment {

	// constants
	public static final String PREF_POINTS = "savedPoints", PREF_STEPS = "savedSteps";
	public static final int DEFAULT_POINTS = 200000, DEFAULT_STEPS = 25;
	public static final String STATE_FRACTAL = "ifsFractal", STATE_FV = "fractalView",
							STATE_QUALITY_TOOLBAR = "qualityToolBarShowing";

	// context
	public Home _home;
	
	// UI
	private FractalView mFractalView;
	private ToolBarRender mToolBar;
	private ToolBarRenderQuality mToolBarQuality;
	private LinearLayout mRenderContainer, mTBContainer;
	
	// vars
	private boolean mUsingOneToolbar = false;
	
	// saved state
	private IFSFractal mFractal;
	private boolean mRendered = false;
	Bundle mStateFVBundle = null;

	public void setFractal(IFSFractal fractal) {
		mRendered = false;
		mFractal = fractal;
		if (mFractalView != null) {
			mFractalView.reset();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mFractal != null)
			outState.putString(STATE_FRACTAL, mFractal.save());
		if (mFractalView != null)
			outState.putBundle(STATE_FV, mFractalView.onSaveInstanceState());
		else if (mStateFVBundle != null)
			outState.putBundle(STATE_FV, mStateFVBundle);
		if (mToolBarQuality != null)
			outState.putBoolean(STATE_QUALITY_TOOLBAR, mToolBarQuality.isShowing());
	}

	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
        _home = (Home)getActivity();
		if (inState != null) {
			mStateFVBundle = inState.getBundle(STATE_FV);
			mFractal = IFSFractal.loadFromString(_home, inState.getString(STATE_FRACTAL));
			mRendered = false; // to force a render
		}
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_render, null);
        _home = (Home)getActivity();
        _home.fR = this;
        
        mUsingOneToolbar = App.SCREEN_WIDTH > App.SCREEN_HEIGHT;
		
        if (mFractalView == null) {
			mFractalView = new FractalView(this, mStateFVBundle, getPoints(), getSteps());
        }

        mRenderContainer = (LinearLayout)root.findViewById(R.id.renderContainer);
        mRenderContainer.addView(mFractalView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		mToolBar = (ToolBarRender)root.findViewById(R.id.toolbar);
		mToolBar.setVisibility(mUsingOneToolbar ? View.GONE : View.VISIBLE);
		mToolBar.init(this);

        mTBContainer = (LinearLayout)root.findViewById(R.id.tbContainer);
		mToolBarQuality = new ToolBarRenderQuality(_home);
        mTBContainer.addView(mToolBarQuality, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mToolBarQuality.init(this);
		if (savedInstanceState != null)
			if (savedInstanceState.getBoolean(STATE_QUALITY_TOOLBAR, false))
				mToolBarQuality.show();
		
        return root;
    }
    
    @Override
	public void onDestroyView() {
    	super.onDestroyView();
    	mRenderContainer.removeView(mFractalView);
    }

    /**
     * Navigate to the color scheme selector.
     */
	public void toGradientList() {
		_home.toChildFragment(new FragmentGradientList(this));
	}

	/**
	 * @return Get the color steps preference.
	 */
	public int getSteps() {
		return M.fit(AppUtils.getSavedInt(_home.mApp, PREF_STEPS, DEFAULT_STEPS), ToolBarRenderQuality.STEPS_MIN,
				ToolBarRenderQuality.STEPS_MAX);
	}

	/**
	 * @return Get the render points preference.
	 */
	public int getPoints() {
		return M.fit(AppUtils.getSavedInt(_home.mApp, PREF_POINTS, DEFAULT_POINTS),
				ToolBarRenderQuality.PTS_MIN, ToolBarRenderQuality.PTS_MAX);
	}

	/**
	 * Set the current color scheme.
	 * OK to do so even when fragment not running
	 */
	protected void onColorSchemeUpdated() {
		if (!isPaused())
			onColorSchemeUpdated.run();
		else
			queueTask(FragmentEvent.OnResume, onColorSchemeUpdated);
	}
	
	private Runnable onColorSchemeUpdated = new Runnable() {
		public void run() {
			ColorScheme cm = _home.mGallery.getColorSchemeByName(_home.mSelColors);
			boolean fgCountMatches = cm.getGradientCount() == mFractalView.mCM.getGradientCount();
			mFractalView.loadColors(cm);
			if (fgCountMatches) {
				if (mFractalView.mRender != null)
					mFractalView.mRender.onColorMapUpdated(mFractalView);
			} else if (mFractalView.mRender != null)
				mFractalView.scheduleDrawFractal(mFractal);
			mToolBar.invalidateToolbar();
		}
	};
    
    @Override
    public void onResume() {
    	super.onResume();
		mFractalView.setVisibility(View.VISIBLE);
		if (mFractal != null && !mRendered) {
			mFractalView.scheduleDrawFractal(mFractal);
			mRendered = true;
		}
		mToolBar.invalidateToolbar();
		_home.invalidateOptionsMenu();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
		mFractalView.setVisibility(View.GONE);
    }
    
    @Override
    public boolean onBackPressed() {
		if (mToolBarQuality.isShowing()) {
			mToolBarQuality.hide();
			mToolBar.invalidateToolbar();
			_home.invalidateOptionsMenu();
			return true;
		}
		mRendered = false;
    	return mFractalView.onBackPressed();
    }
    
    /** an enum representing the actions possible in the sliders */
    public enum SliderAction{POINTS, STEPS};

    /**
     * A method called when the quality toolbar's sliders are moved
     * @param action : the slider action
     * @param value : the new value
     */
	public void sliderBarChanged(SliderAction action, float value) {
		switch (action) {
		case POINTS:
			AppUtils.setSavedInt(_home.mApp, PREF_POINTS, (int)value);
			mFractalView.setPoints((int)value);
			break;
		case STEPS:
			AppUtils.setSavedInt(_home.mApp, PREF_STEPS, (int)value);
			mFractalView.setSteps((int)value);
			break;
		}
	}
    
	/**
	 * Toggle visibility of the quality toolbar
	 */
    public void toggleQualityToolBar() {
    	if (mToolBarQuality.isShowing())
    		mToolBarQuality.hide();
    	else
    		mToolBarQuality.show();
    }

    /** an enum representing the possible action bar items */
	public enum Actions { SAVE, SHARE, WALLPAPER, COLORS, QUALITY };

	@Override
	protected void onCreateOptionsMenu(ArrayList<Action> items) {
		if (mUsingOneToolbar) {
			if (!mToolBarQuality.isShowing())
				items.add(new Action(getString(R.string.btn_quality), Action.ICON_NONE, true, false, Actions.QUALITY));
			items.add(new Action(getString(R.string.btn_colorscheme), mFractalView.mCM.generatePreview(48*3, 48, true, Color.BLACK), false, false, Actions.COLORS));
			// _home.getResources().getColor(R.color.abs__holo_blue_light)
		}
		items.add(new Action(getString(R.string.btn_save), R.drawable.ic_action_save, false, false, Actions.SAVE));
		items.add(new Action(getString(R.string.btn_share), R.drawable.ic_action_share, false, false, Actions.SHARE));
		items.add(new Action(getString(R.string.btn_wallpaper), R.drawable.ic_image_picture, false, true, Actions.WALLPAPER));
	}

	@Override
	protected void onOptionsItemSelected(Enum<?> e) {
		switch ((Actions)e) {
		case SAVE:
			if (mFractalView.getRenderedBitmap() != null)
				saveImage(mFractalView.getRenderedBitmap(), true);
        	break;
		case SHARE:
			if (mFractalView.getRenderedBitmap() != null) {
				String file = saveImage(mFractalView.getRenderedBitmap(), false);
				if (file != "") {
		        	Intent imgIntent = new Intent(android.content.Intent.ACTION_SEND);  
		        	imgIntent.setType("image/jpeg");
		        	imgIntent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse("file://"+file));
		    		imgIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.txt_share_fractal_desc));
		        	startActivity(Intent.createChooser(imgIntent, getString(R.string.txt_share_fractal)));
				}
			}
        	break;
		case WALLPAPER:
			if (mFractalView.getRenderedBitmap() != null)
				try {
					_home.setWallpaper(mFractalView.getRenderedBitmap());
				} catch (Exception ex) {
					_home.mApp.toast("Unknown error setting wallpaper", false);
					_home.mApp.mError.report("SetWallpaper", ex);
				}
        	break;
		case COLORS:
			toGradientList();
			break;
		case QUALITY:
			toggleQualityToolBar();
			_home.invalidateOptionsMenu();
			break;
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mFractalView != null)
			mFractalView.onDestroy();
	}

	/**
	 * Save a bitmap to the shared external storage directory for this app
	 * @param _app : the App object
	 * @param b : the bitmap
	 * @param fn : the name
	 * @return the entire path given to the image
	 */
	private String saveBitmapShared(App _app, Bitmap b, final String fn) {
		File extDir = Environment.getExternalStorageDirectory();
		String path = IFSFile.DIR_EXTERNAL_SHARED+fn+".jpg";
		File fullPath = new File(extDir, path);
		String filePath = fullPath.getPath();
		
		try {
			FileOutputStream fos = new FileOutputStream(fullPath);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			b.compress(Bitmap.CompressFormat.JPEG, 90, bos);
		} catch (Exception ex) {
			_home.mApp.mError.report("SaveBitmapShared", ex, fn);
			return null;
		}
		final Context acx = _app.getContext().getApplicationContext();
		MediaScannerNotifier.run(acx, new Handler(), filePath, null);
		return filePath;
	}
	
	/**
	 * Save a bitmap to external storage
	 * @param myImage : the bitmap
	 * @param toast : true to toast upon success or failure
	 * @return the path given to the image
	 */
	private String saveImage(Bitmap myImage, boolean toast) {
		String fn = saveBitmapShared(_home.mApp, myImage, "" + new Date().getTime());
		if (toast)
			_home.mApp.toast((fn != null) ? R.string.txt_save_image_success : R.string.txt_save_image_fail, false);
		return fn;
	}
	
	/** Get the toolbar */
	public ToolBarRender getToolbar() {
		return mToolBar;
	}
	
	/** Get the quality toolbar */
	public ToolBarRenderQuality getToolbarQuality() {
		return mToolBarQuality;
	}
	
	/** Get the fractal view */
	public FractalView getFractalView() {
		return mFractalView;
	}

	@Override
	public String getTitle() {
		return "";
	}
	
	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return getDefaultAnimationSlideFromRight(fa);
	}
}