package com.resonos.apps.fractal.ifs.util;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.RectF;
import android.view.View;

import com.resonos.apps.fractal.ifs.FragmentGallery;
import com.resonos.apps.fractal.ifs.Home;
import com.resonos.apps.fractal.ifs.model.ColorScheme.PreparedColorScheme;
import com.resonos.apps.fractal.ifs.model.IFSFractal;
import com.resonos.apps.fractal.ifs.model.IFSFractal.RawIFSFractal;
import com.resonos.apps.fractal.ifs.util.IFSRender.FractalViewLock;
import com.resonos.apps.fractal.ifs.view.FractalView;
import com.resonos.apps.fractal.ifs.view.EditorView;
import com.resonos.apps.library.App;
import com.resonos.apps.library.model.Coord;
import com.resonos.apps.library.util.M;
import com.resonos.apps.library.util.TouchViewWorker;

/**
 * This class manages one or more Fractal renders.
 * @author Chris
 */
public abstract class IFSRenderManager implements FractalViewLock {
	
	/** value used to normalize the bounds of fractals a little bit */
	public static final float FRACTAL_MOD = 0.005f;
	
	/** points used to calculate bounds */
	public static final int IFS_POINTS_BOUNDS = 1000;

	/** margin in pixels that we would like to achieve for rendering */
	public static final float FRACTAL_MARGIN_GOAL = 25.0f;

	public static final int IFS_PREVIEW_POINTS = 9000;
	public static final int PREVIEW_WIDTH = App.inDP(110), PREVIEW_HEIGHT = App.inDP(110);
	public static final int PREVIEW_COLOR_STEPS = 8;
	
	// context 
	Home _home;
	View _v;
	
	// vars
	public PreparedColorScheme mColorScheme;
	public int drawBGColor;

	public RawIFSFractal mTrans;
	public int mWidth, mHeight, mPoints;
	private RectF mBounds = null;
	
	public FractalViewLock mLock;
	private Random mRnd;
	
	final boolean isPreview;
	private final TouchViewWorker mTVW;
	
	/**
	 * Create the render manager based on a fractal view
	 * @param fv : the fractal view
	 */
	public IFSRenderManager(FractalView fv) { // normal render
		_home = fv._home;
		isPreview = false;
		mTVW = fv.mTVW;
		_v = fv;
		mLock = fv;
		
		onUpdateColors(fv);
		init();
	}
	
	/**
	 * Called when the colors in the FractalView are updated
	 * @param fv : the fractal view
	 */
	public void onUpdateColors(FractalView fv) {
		mColorScheme = fv.mCM.prepareGradient(fv.mColorSteps);
		drawBGColor = 0;
	}

	/**
	 * finish setting up the manager based on a fractal view
	 * @param fv : the parent fractal view
	 */
	public void finishSetup(FractalView fv) {
		mWidth = fv._w;
		mHeight = fv._h;
		mPoints = fv.mPoints;
	}
	
	/**
	 * Create the render manager based on a gallery and raw fractal
	 * @param fg : the gallery fragment
	 * @param ifs : a raw calculated fractal
	 */
	public IFSRenderManager(FragmentGallery fg, IFSFractal ifs) { // preview render
		_home = fg.getHome();
		isPreview = true;
		mTVW = null;
		mLock = fg;
		
		mWidth = PREVIEW_WIDTH;
		mHeight = PREVIEW_HEIGHT;
		mPoints = IFS_PREVIEW_POINTS;
		
		mColorScheme = fg.mColorScheme;
		drawBGColor = mColorScheme.mBGColor;
		
		mTrans = ifs.calculateRaw();
		init();
	}

	/**
	 * Finish initializing the render manager.
	 */
	private void init() {
		mRnd = new Random();
	}
	
	/**
	 * Reset the render manager.
	 */
	public void reset() {
		mBounds = null;
	}

	/**
	 * Setup positioning.
	 * @param zWindow : the RectF to fill with bounds of what will be rendered
	 * @return the same RectF for chaining
	 */
	public RectF setupPositioning(RectF zWindow) {
		RectF bounds = getBounds();
		
		if (isPreview) {
			zWindow.set(bounds);
			zWindow = M.fitRectInWindow(new Coord(mWidth, mHeight), zWindow);
		} else { // otherwise, update the touchviewworker window
			// this will use tSize and tPos set in FractalView (see FractalView.TouchViewWorker.getWindow(RectF))
			// the touchviewworker will retain it's own pan and zooms
			// which are applied to tPos/tSize to create the "zoomed" viewing window
			mTVW.updateWindowParameters();
			// we retrieve the zoomed viewing window here into zWindow
			mTVW.getRectModel(zWindow);
		}
		return zWindow;
	}

	/**
	 * Retrieve the bounds, calculating them if necessary.
	 * @return The bounds of the fractal to be rendered.
	 */
	public RectF getBounds() {
		if (mBounds == null)
			mBounds = calculateBounds();
		return mBounds;
	}
	
	/**
	 * Calculate the bounds of the fractal to be rendered.
	 * @return a RectF with the bounds.
	 */
	private RectF calculateBounds() {
		// make sure the ifs is valid
		int tcount = mTrans.data.length;
		if (tcount == 0) {
			ArrayList<float[]> deftrans = IFSFile.decodeIFS(_home, IFSFile.IFS_TRI);
			mTrans.data = new float[deftrans.size()][];
			mTrans.data = deftrans.toArray(mTrans.data);
			tcount = mTrans.data.length;
		}
		
		float x = EditorView.IFS_XSTART;
		float y = EditorView.IFS_YSTART;
		float xsMax = -999, xsMin = 999, ysMax = -999, ysMin = 999;
		for (int i = 0; i < IFS_POINTS_BOUNDS; i++) {
			int t = 0;
			float prob = mRnd.nextFloat();
			while (t < tcount) { // pick a random transformation to use
				prob -= mTrans.data[t][EditorView.IFS_P];
				if (prob <= 0)
					break;
				t++;
			}
			if (t >= tcount) // won't happen with a proper ifs
				t = tcount - 1;
			float a = mTrans.data[t][EditorView.IFS_A];
			float b = mTrans.data[t][EditorView.IFS_B];
			float c = mTrans.data[t][EditorView.IFS_C];
			float d = mTrans.data[t][EditorView.IFS_D];
			float e = mTrans.data[t][EditorView.IFS_E];
			float f = mTrans.data[t][EditorView.IFS_F];
			float newx = a * x + b * y + e;
			float newy = c * x + d * y + f;
			x = newx;
			y = newy;

			xsMax = Math.max(xsMax, x);
			xsMin = Math.min(xsMin, x);
			ysMax = Math.max(ysMax, y);
			ysMin = Math.min(ysMin, y);
		}

		// calculate standard area for this IFS
		float xdist = (float) (xsMax - xsMin);
		float ydist = (float) (ysMax - ysMin);
		float sxmin = xsMin;
		float symin = ysMin;
		float sxmax = sxmin + xdist;
		float symax = symin + ydist;

		// add some spacing
		float spacing = FRACTAL_MARGIN_GOAL;
		float xspace = xdist / spacing;
		float yspace = ydist / spacing;
		sxmin = sxmin - xspace;
		symin = symin - yspace;
		sxmax = sxmax + xspace;
		symax = symax + yspace;
		sxmin = sxmin - (sxmin % FRACTAL_MOD);
		symin = symin - (symin % FRACTAL_MOD);
		sxmax = sxmax + (FRACTAL_MOD - (sxmax % FRACTAL_MOD));
		symax = symax + (FRACTAL_MOD - (symax % FRACTAL_MOD));
		
		RectF r = new RectF(sxmin, symin, sxmax, symax);
		
		float _w = (float)mWidth, _h = (float)mHeight;
		
		// now make it the same aspect ratio as the view
		float renderRatio = _w / _h;
		float ifsRatio = r.width() / r.height();
		if (renderRatio < ifsRatio) {
			float extra = (_h/_w*r.width() - r.height())/2;
			r.set(r.left, r.top - extra,
					r.right, r.bottom + extra);
		} else {
			float extra = (_w/_h*r.height() - r.width())/2;
			r.set(r.left - extra, r.top,
					r.right + extra, r.bottom);
		}
		
		return r;
	}

	/**
	 * @return Get the current zoom of the renderer.
	 */
	public float getCurScale() {
		return isPreview ? 1 : mTVW.getCurScale();
	}

	/**
	 * Get the current pan amount of the renderer.
	 * @param c : a coordinate to put the pan amount in
	 * @return the same coordinate for chaining
	 */
	public Coord getCurPan(Coord c) {
		return isPreview ? c.set(0,0) : c.set(mTVW.getCurPanView());
	}

	/**
	 * Invalidate the visual representation of this render, if there is one.
	 */
	public void invalidate() {
		if (!isPreview)
			_v.postInvalidate();
	}

	/**
	 * Override this to be notified of when the render finishes and the status.
	 * @param error : true if the render failed due to a bad navigational location or unrenderable fractal
	 * @param oome : true if the render failed due to an out of memory error
	 */
	public abstract void onCompleteRender(boolean error, boolean oome);
}
