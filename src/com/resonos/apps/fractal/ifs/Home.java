package com.resonos.apps.fractal.ifs;

import java.io.File;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.model.Gallery;
import com.resonos.apps.fractal.ifs.util.IFSFile;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.FragmentBaseActivity;
import com.resonos.apps.library.util.AppUtils;
import com.resonos.apps.library.util.M;

//----------------------------------------------------------------------

public class Home extends FragmentBaseActivity {

	// constants
	private static final String STATE_MAIN_FRAGMENT = "mainFragment",
									STATE_EDITOR_FRAGMENT = "editorFragment",
									STATE_RENDER_FRAGMENT = "renderFragment",
									STATE_GALLERY_FRAGMENT = "galleryFragment",
									STATE_TUTORIAL = "tutorial";
	public static final String PREF_COLORSCHEME = "prefColorScheme",
								DEF_COLORSCHEME = "Space";
	public static final String WEB_PAGE = "http://resonos.com/";
	
	// objects
	public FragmentMain fM;
    public FragmentEditor fE;
    public FragmentRender fR; // only for tutorial access
    private FragmentGallery fG;
//    public FragmentUpgrade fU;
    
	public Help mHelp = null;
    public View mView;

    // saved info
	private Gallery mSampleGallery, mUserGallery;
	public Gallery mGallery;
	public String mSelColors = DEF_COLORSCHEME;
	
	// vars
	boolean mIsDuringHelp = false;
	boolean mHasPro = false;
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		AppUtils.putFragment(this, outState, this, STATE_MAIN_FRAGMENT, fM);
		AppUtils.putFragment(this, outState, this, STATE_EDITOR_FRAGMENT, fE);
		AppUtils.putFragment(this, outState, this, STATE_RENDER_FRAGMENT, fR);
		AppUtils.putFragment(this, outState, this, STATE_GALLERY_FRAGMENT, fG);
		if (mHelp != null && isDuringHelp())
			outState.putBundle(STATE_TUTORIAL, mHelp.onSaveInstanceState());
	}

	/**
	 * Sets the currently used color scheme. Set here in order
	 * to maintain a global app preference for it.
	 * @param val : the id name of the color scheme.
	 */
	public void setColorScheme(String val) {
		mSelColors = val;
		AppUtils.setSavedString(mApp, PREF_COLORSCHEME, val);
		if (fG != null)
			fG.clearCache();
	}

	/**
	 * Creates a unified Gallery to look at fractals and color schemes from.
	 * Do this whenever the user has modified the user gallery.
	 */
	public void rebuildUnifiedGallery() {
		mGallery = new Gallery();
		mGallery.add(mSampleGallery);
		mGallery.add(mUserGallery);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_Sherlock_K);
        // prepare dirs
		new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/", IFSFile.DIR_EXTERNAL_FILES).mkdirs();
		new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/", IFSFile.DIR_EXTERNAL_SHARED).mkdirs();
		
		// set up files before we call the superclass, in case its restoring state
		// load database, and init it if this is the first time
		Object o = this.getLastCustomNonConfigurationInstance();
		if (o != null && o instanceof RetainedGallery) { // speeds up orientation changes
			RetainedGallery rGal = (RetainedGallery)o;
			mSampleGallery = rGal.sGal;
			mUserGallery = rGal.uGal;
			mGallery = rGal.mGal;
		} else {
			mSampleGallery = Gallery.loadSampleGallery(this);
			mUserGallery = Gallery.loadGallery(this);
			rebuildUnifiedGallery();
		}
		
		super.onCreate(savedInstanceState);
		
		// check for pro version
	    String mainAppPkg = getPackageName();
	    String keyPkg = mApp.mAppInfo.packageProName;
	    int sigMatch = getPackageManager().checkSignatures(mainAppPkg, keyPkg);
	    mHasPro = (sigMatch == PackageManager.SIGNATURE_MATCH);
		
		// restore instance state
		if (savedInstanceState != null) {
			fM = (FragmentMain)AppUtils.getFragment(this, savedInstanceState, this, STATE_MAIN_FRAGMENT);
			fE = (FragmentEditor)AppUtils.getFragment(this, savedInstanceState, this, STATE_EDITOR_FRAGMENT);
			fR = (FragmentRender)AppUtils.getFragment(this, savedInstanceState, this, STATE_RENDER_FRAGMENT);
			fG = (FragmentGallery)AppUtils.getFragment(this, savedInstanceState, this, STATE_GALLERY_FRAGMENT);
		}
		mSelColors = AppUtils.getSavedString(mApp, PREF_COLORSCHEME, DEF_COLORSCHEME);
		
		mView = getWindow().getDecorView();
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		super.onRetainCustomNonConfigurationInstance();
		return new RetainedGallery(this);
	}
	
	/**
	 * In order to provide faster configuration changes,
	 * let's not reload the gallery. Retain both sample and user
	 * galleries in one object with this class.
	 */
	public static class RetainedGallery {
		Gallery sGal, uGal, mGal;
		public RetainedGallery(Home home) {
			sGal = home.mSampleGallery;
			uGal = home.mUserGallery;
			mGal = home.mGallery;
		}
	}
	
	@Override
	public void onRestoreInstanceState(Bundle inState) {
		if (inState != null) {
			Bundle bHelp = inState.getBundle(STATE_TUTORIAL);
			if (bHelp != null) {
				mHelp = new Help(this, bHelp);
				mView.post(new Runnable() {
					public void run() {
						mHelp.resume();
					}
				});
			}
		}
	}

	/**
	 * Begin the tutorial.
	 */
	public void toHelp() {
        mHelp = new Help(this, null);
        mHelp.start();
	}

	@Override
	protected void onResume() {
		super.onResume();
 	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected FApp createApp(Bundle savedInstanceState) {
		if (mApp == null)
			return new FApp(savedInstanceState, this);
		else
			return (FApp)mApp;
	}
	
	/**
	 * Get the App
	 * @return return the App object as an FApp object
	 */
	protected FApp getApp() {
		return (FApp)mApp;
	}

	@Override
	protected void onPause() {
		super.onPause();
		mHandler.removeCallbacks(getApp().toastVersionChanged);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		M.log("Home", "onStop");
	}
	
    @Override
    protected void onDestroy() {
		mUserGallery.save(this);
        super.onDestroy();
    }

	@Override
	protected boolean onBackPressedCustom() {
		if (isDuringHelp()) {
			setDuringHelp(false);
			return true;
		}
		return false;
	}

	/**
	 * Navigate to the editor from root level to a child.
	 */
	public void toEditorFromRoot() {
		if (fE == null)
			fE = new FragmentEditor();
		toChildFragment(fE);
	}

	/**
	 * Navigate to the editor as a sibling.
	 */
	public void toEditorFromLevel1() {
		if (fE == null)
			fE = new FragmentEditor();
		toSiblingFragment(fE, false);
	}

	/**
	 * Navigate to the gallery as a child.
	 */
	public void toGallery() {
		if (fG == null)
			fG = new FragmentGallery(false);
		toChildFragment(fG);
	}
	
	/**
	 * Return to the home screen.
	 */
	public void returnToDashboard() {
		returnToFragment(fM);
	}

	/**
	 * @return true if during tutorial
	 */
	public synchronized boolean isDuringHelp() {
		return mIsDuringHelp;
	}
	
	/**
	 * @param b : Set true if we are during the tutorial
	 */
	public synchronized void setDuringHelp(boolean b) {
		mIsDuringHelp = b;
	}
	
	/**
	 * Return true if this is the pro version.
	 */
	public boolean hasPro() {
		return mHasPro;
	}

	@Override
	protected BaseFragment getMainFragment() {
		if (fM == null)
			fM = new FragmentMain();
		return fM;
	}

	@Override
	protected int getLayoutID() {
		return R.layout.main;
	}

	@Override
	protected int getFragmentContainerID() {
		return R.id.container;
	}

	@Override
	protected int getABSpacerID() {
		return R.id.spacer;
	}

	@Override
	protected Param[] getParams() {
		return new Param[] {Param.PROGRESS};
	}

	/**
	 * Gets the User gallery, wrapped in a function to warn you
	 * that it should only be used for saving
	 * @return The user gallery.
	 */
	public Gallery getUserGallery() {
		return mUserGallery;
	}
}