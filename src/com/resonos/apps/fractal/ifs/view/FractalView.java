package com.resonos.apps.fractal.ifs.view;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;

import com.WazaBe.HoloEverywhere.HoloAlertDialogBuilder;
import com.resonos.apps.fractal.ifs.FragmentRender;
import com.resonos.apps.fractal.ifs.Home;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.model.IFSFractal;
import com.resonos.apps.fractal.ifs.model.IFSFractal.RawIFSFractal;
import com.resonos.apps.fractal.ifs.util.IFSRender;
import com.resonos.apps.fractal.ifs.util.IFSRender.FractalViewLock;
import com.resonos.apps.fractal.ifs.util.IFSRenderManager;
import com.resonos.apps.library.model.Coord;
import com.resonos.apps.library.model.ImmutableCoord;
import com.resonos.apps.library.util.M;
import com.resonos.apps.library.util.TouchViewWorker;
import com.resonos.apps.library.util.TouchViewWorker.Param;
import com.resonos.apps.library.util.TouchViewWorker.TouchMode;
import com.resonos.apps.library.util.TouchViewWorker.TouchViewReceiver;

/**
 * This class represents a view that displays a fractal render, even in progress.
 * It also allows zooming and panning, and will reinitiate a render for each
 * new navigational point at any time.  All of this can happen fluidly with
 * no breaks or pauses for the user.
 * @author Chris
 */
public class FractalView extends View implements FractalViewLock {

	// constants
	static final float MIN_ZOOM = 0.01f;
	static final float MAX_ZOOM = 100f;
	
	// context
	public Home _home;
	
	// objects
	public TouchViewWorker mTVW;
	private Bitmap mIm = null, mImPrev = null;
	public ColorScheme mCM = null;
	private RawIFSFractal mRawFractal;
	
	// vars
	private boolean bDoDrawOnAttach = false;
	private boolean bViewChanged;
	

	// render info
	public int _w;
	public int _h;
	public int mPoints;
	public int mColorSteps;
	
	public IFSRender mRender;
	public IFSRender mLastRender;
	
	private Paint mAlphaPaint, mBGPaint;
	
	IFSRenderManager mManager;

	private static final String STATE_TVW = "tvw";
	
	@Override
	public Bundle onSaveInstanceState() {
		Bundle outState = new Bundle();
		if (mTVW != null)
			outState.putBundle(STATE_TVW, mTVW.onSaveInstanceState());
		synchronized (this) {
			if (mRender != null)
				mRender.cancelRender();
		}
		return outState;
	}

	/**
	 * Create a fractal rendering view
	 * @param fragmentRender : the parent fragment
	 * @param inState : state, if any
	 * @param pts : the starting number of points to draw
	 * @param colorSteps : the starting number of color steps to use
	 */
	public FractalView(FragmentRender fragmentRender, Bundle inState, int pts, int colorSteps) {
		super(fragmentRender._home);
		_home = fragmentRender._home;
		mPoints = pts;
		mColorSteps = colorSteps;
		init();
		
		loadColors(fragmentRender._home.mGallery.getColorSchemeByName(fragmentRender._home.mSelColors));
		
		// set up touch handling
		Bundle touchBundle = (inState == null) ? null : inState.getBundle(STATE_TVW);
		mTVW = new TouchViewWorker(fragmentRender._home.mApp, this, touchBundle,
				new TouchViewReceiver() {
					/** Overridden to observe that we are beginning a pan/zoom, and to create a second bitmap if needed */
					public boolean onTouchDown(Coord touchPointView, Coord touchPointModel) {
						synchronized (FractalView.this) {
							if (mImPrev == null) 
								mImPrev = Bitmap.createBitmap(_w, _h, Config.ARGB_8888);
						}
						bViewChanged = false;
						return false;
					}
					
					/** Always allow */
					public boolean startPan(Coord curPanModel) {
						return true;
					}

					/** Always allow */
					public boolean startScale(float curScale) {
						return true;
					}

					/** Redraw and note that we have navigated */
					public void changePan(Coord newPos) {
						bViewChanged = true;
						invalidate();
					}
					
					/** Redraw and note that we have navigated, also set boundaries on the zoom level. */
					public void changeScale(float newScale) {
						float xyz = M.fit(mTVW.getCurScale(), MIN_ZOOM, MAX_ZOOM);
						mTVW.setCurScale(xyz);
						bViewChanged = true;
						invalidate();
					}
					
					/** If we have been navigating, it is now complete.  Rerender the fractal! */
					public void onTouchUp() {
						if (mRawFractal != null && bViewChanged) {
							scheduleDrawFractal(mRawFractal);
						}
					}
					
					/** specify a coordinate system: use the values tPos and tSize,
					 *  which have been calculated in {@link IFSRender.drawIFS} */
					public void getWindow(RectF w) {
						synchronized (FractalView.this) {
							if (mRender != null)
								mTVW.fitSizeInWindow(mRender.tPos, mRender.tSize, w);
						}
					}
					
					// we don't care about any of these methods
					public void onTouchMove(Coord touchPointView, Coord touchPointModel) { }
					public void pointerDown() { }
					public void pointerUp(TouchMode tm) { }
					public boolean startRotate(float startingAngle) { return false; }
					public void changeRotate(float newAngle) { }
				},
				
				// these are the parameters we need
				Param.PAN, Param.ZOOM, Param.FLIPY, Param.PAN_AFTER_TWO_FINGERS);
		
		mManager = new IFSRenderManager(this) {
//			boolean mErrorLastRender = false;
			public void onCompleteRender(boolean error, boolean oome) {
				if (oome) {
					new HoloAlertDialogBuilder(_home)
							.setTitle(R.string.txt_oome)
							.setMessage(R.string.txt_oome_desc)
							.setPositiveButton(R.string.btn_continue, null).show();
					return;
				}
				
				// the below behavior informs the user he has navigated out of bounds
				// however, currently it seems fine to just stop the render and say nothing
				// all visual appearance to the user will remain seamless anyway
				
//				if (error) {
//					if (mErrorLastRender) {
//						new HoloAlertDialogBuilder(_home)
//						.setTitle("Cannot Continue!")
//						.setMessage(
//								"This fractal has no renderable space.")
//						.setPositiveButton("Ok", null).show();
//						return;
//					}
//					new HoloAlertDialogBuilder(_home)
//					.setTitle("Cannot Continue!")
//					.setMessage(
//							"Due to the current zoom location, this render will "
//									+ "never draw enough dots to finish.")
//					.setPositiveButton("Ok", null).show();
//					mErrorLastRender = true;
//					if (mLastRender != null) {
//						mRender = mLastRender;
//						mLastRender = null;
//						swapImages();
//						if (mRender != null) {
//							mTVW.setCurScale(mRender._scale);
//							mTVW.setCurPanView(mRender._pan);
//							invalidate();
//							if (!mRender.bFinished) {
//								mRender.resume();
//							}
//						}
//					} else {
//						if (mRender != null) {
//							mTVW.resetAlteredWindow();
//							invalidate();
//							if (!mRender.bFinished) {
//								mRender.resume();
//							}
//						}
//					}
//				} else {
//					mErrorLastRender = false;
//				}
			}
		};
	}

	/**
	 * Reset this FractalView back to its starting state.
	 */
	public synchronized void reset() {
		if (mRender != null)
			mRender.cancelRender();
		mLastRender = null;
		mRender = null;
		mManager.reset();
		mTVW.resetAlteredWindow();
	}
	
	/**
	 * called when it should be destroyed.
	 */
	public void onDestroy() {
		dispose();
	}

	/**
	 * Set the number of color steps to use, updating the render even if in progress.
	 * @param value
	 */
	public synchronized void setSteps(int value) {
		mColorSteps = value;
		mManager.onUpdateColors(this);
		mRender.onColorMapUpdated(this);
	}

	/**
	 * Sets the number of points to render and passes that info along to the renderer to take further action.
	 * @param value
	 */
	public synchronized void setPoints(int value) {
		mManager.mPoints = value;
		mRender.setPoints(value);
		mPoints = value;
	}
	
	/**
	 * Initiates properties of this view.
	 */
	public void init() {
		mAlphaPaint = new Paint();
		mBGPaint = new Paint();
		
		// set up view properties
		setFocusableInTouchMode(false);
		setOnKeyListener(null);
		setFocusable(false);
	}

	/**
	 * Schedule a fractal to be drawn as soon as possible.
	 * @param f : the {@link IFSFractal} to be rendered
	 */
	public void scheduleDrawFractal(IFSFractal f) {
		mRawFractal = f.calculateRaw();
		scheduleDrawFractal(mRawFractal);
	}

	/**
	 * Schedule raw fractal data to be drawn as soon as possible.
	 * @param f : the {@link RawIFSFractal} to be rendered
	 */
	public synchronized void scheduleDrawFractal(RawIFSFractal fs) {
		invalidate();
		mRawFractal = fs;
		doScheduleDrawFractal();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH) {
		synchronized (this) {
			_w = w;
			_h = h;
			mTVW.updateWindowParameters();
		}
		if (bDoDrawOnAttach) {
			bDoDrawOnAttach = false;
			doScheduleDrawFractal();
		}
	}

	/**
	 * Do a fractal render, or schedule it if the view is not yet in place.
	 */
	private void doScheduleDrawFractal() {
		if (!hasImage()) {
			_w = getWidth();
			_h = getHeight();
			if (_w == 0 || _h == 0) {
				bDoDrawOnAttach = true; // not ready, cache til we are
				return;
			}
			synchronized (FractalView.this) {
				mIm = Bitmap.createBitmap(_w, _h, Config.ARGB_8888);
			}
		}
		System.gc();
		synchronized (FractalView.this) {
			if (mRender != null) {
				mRender.cancelRender();
				mLastRender = mRender;
				mRender = null;
			}
			// we've moved the viewport, swap images now
			swapImages();
			mManager.finishSetup(this);
			mRender = new IFSRender(mManager, mIm);
		}
		mRender.startRender(mRawFractal);
		invalidate();
	}

	/**
	 * Swap the two bitmaps used when moving between navigational states.
	 */
	private synchronized void swapImages() {
		if (mIm != null && mImPrev != null) {
			Bitmap bmp = mIm;
			mIm = mImPrev;
			mImPrev = bmp;
		}
	}

	/**
	 * Free memory when closing this view.
	 */
	private synchronized void dispose() {
		mIm = M.freeBitmap(mIm);
		mImPrev = M.freeBitmap(mImPrev);
	}

	/**
	 * Called when the fragment receives notification that back was pressed.
	 * @return true to capture the event
	 */
	public synchronized boolean onBackPressed() {
		if (mRender != null)
			mRender.cancelRender();
		mRender = null;
		mLastRender = null;
		return false;
	}

	@Override
	protected synchronized void onDraw(final Canvas canvas) {
		// background
		if (mCM != null)
			canvas.drawColor(mCM.getBGColor());
		
		ImmutableCoord curPan = mTVW.getCurPanView();
		float curScale = mTVW.getCurScale();
		
		if (hasImage()) {
			if (mLastRender != null && mLastRender.mIm != null
					&& !mLastRender.mIm.isRecycled() && mRender.getProgress() < 100) {
				// last image
				canvas.save();
				float rsc = curScale/mLastRender._scale;
				canvas.scale(rsc, rsc,
						_w/2 - mLastRender._pan.x,
						_h/2 + mLastRender._pan.y);
				canvas.translate((mLastRender._pan.x - curPan.x)/(rsc), (curPan.y - mLastRender._pan.y)/(rsc));
				canvas.drawBitmap(mLastRender.mIm, 0, 0, null);
				canvas.restore();
				
				// fade out
				int alpha = M.fit(Math.round((float)mRender.getProgress()/100f * 255), 0, 255);
				mBGPaint.setColor(mCM.getBGColor());
				mBGPaint.setAlpha(alpha);
				canvas.drawPaint(mBGPaint);
			}

			// new image
			if (mRender.readyToDraw() && mRender.mIm != null && !mRender.mIm.isRecycled()) {
				canvas.save();
				float rsc = curScale/mRender._scale;
				canvas.scale(rsc, rsc,
						_w/2 - mRender._pan.x, _h/2 + mRender._pan.y);
				canvas.translate((mRender._pan.x - curPan.x)/(rsc), (curPan.y - mRender._pan.y)/(rsc));
				canvas.drawBitmap(mRender.mIm, 0, 0, mAlphaPaint);
				canvas.restore();
			}
		}
	}

	/**
	 * @return true if the bitmap was already allocated
	 */
	private synchronized boolean hasImage() {
		return mIm != null;
	}

	/**
	 * Load a color scheme
	 * @param colorMap : the {@link ColorScheme} object
	 */
	public synchronized void loadColors(ColorScheme colorMap) {
		mCM = colorMap;
		mCM.prepareGradient(mColorSteps); // isPreview() ? PREVIEW_COLOR_STEPS : mColorSteps
		if (mManager != null)
			mManager.onUpdateColors(this);
	}

	/**
	 * @return the bitmap, which is subject to change, so don't keep it around
	 */
	public Bitmap getRenderedBitmap() {
		return mIm;
	}
}
