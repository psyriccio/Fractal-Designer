package com.resonos.apps.fractal.ifs.util;

import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.AsyncTask;

import com.resonos.apps.fractal.ifs.model.IFSFractal.RawIFSFractal;
import com.resonos.apps.fractal.ifs.view.EditorView;
import com.resonos.apps.fractal.ifs.view.FractalView;
import com.resonos.apps.library.model.Coord;
import com.resonos.apps.library.util.ErrorReporter;

/**
 * This class does all of the work of rendering an IFS Fractal.
 * It interacts with the {@link IFSRenderManager} and
 * initiates the {@link IFSRenderTask}.
 * @author Chris
 */
public class IFSRender {
	
	// constants
	
	/** The number of points at which we should see a trend before determining that an IFS is bunk */
	public static final int IFS_WARN = 10000;
	
	// objects
	IFSRenderManager mManager;
	IFSRenderTask mTask;
	public Bitmap mIm;
	
	// state
	boolean bReadyToDraw = false;
	boolean bOOME;
	public boolean bError, bFinished = false;
	private int _progress = 0;
	public Coord _pan = new Coord();
	public float _scale = 1;
	
	// vars
	public RectF zWindow = new RectF();
	int mStartingPointCount = 0;
	
	public Coord tPos = new Coord();
	public Coord tSize = new Coord();
	
	public interface FractalViewLock { /* empty */ }
	
	private byte[][][] mImage = null;
	private int points, pointsDrawn;
	
	/**
	 * Create a render object
	 * @param man : the render manager
	 * @param im : an allocated bitmap
	 */
	public IFSRender(IFSRenderManager man, Bitmap im) {
		mIm = im;
		mManager = man;
	}

	/**
	 * Start the render
	 * @param mRawFractal : a raw calculated fractal object 
	 */
	public void startRender(RawIFSFractal mRawFractal) {
		synchronized (mManager.mLock) {
			mManager.mTrans = mRawFractal;
			mStartingPointCount = 0;
			if (mTask != null)
				mTask.mCancel = true;
		} 
		mTask = new IFSRenderTask();
		mTask.execute();
	}
	
	/**
	 * Cancel rendering task thread.
	 */
	public void cancelRender() {
		synchronized (mManager.mLock) {
			mTask.mCancel = true;
		}
	}
	
	/**
	 * Get the rendering progress
	 * @return progress, from 0 to 100
	 */
	public int getProgress() {
		synchronized (mManager.mLock) {
			return _progress;
		}
	}
	
	/**
	 * do the work of drawing an IFS
	 * @param task : the parent task
	 */
	public void drawIFS(IFSRenderTask task) {
		long starttime = System.currentTimeMillis();
		float percentdone = 0f;
		
		float dim_x, dim_y;
		float dim_xo = 0;
		float dim_yo = 0;

		RectF bounds;
		synchronized (mManager.mLock) {
			dim_x = mManager.mWidth;
			dim_y = mManager.mHeight;
			points = mManager.mPoints + EditorView.IFS_BUFFER_POINTS;
			
			if (mIm == null || mIm.isRecycled()) {
				task.mCancel = true;
				return;
			}
			
			if (mStartingPointCount == 0)
				mIm.eraseColor(mManager.drawBGColor);
	
			if (mStartingPointCount == 0)
				mImage = new byte[Math.min(mManager.mTrans.data.length, mManager.mColorScheme.mFGColors.length)][(int) dim_x][(int) dim_y];

			bounds = mManager.getBounds();
		}

		// modify window based on zoom parameters
		int pts, pointsmod, i, j;
		float[][] trans;
		int tc, colorSteps;
		int[][] fgColors;
		synchronized (mManager.mLock) {
			fgColors = mManager.mColorScheme.mFGColors;
			colorSteps = mManager.mColorScheme.mSteps;
			
			// calculate this IFS's bounds
			tSize.set(bounds.width(), bounds.height());
			tPos.set(bounds.centerX(), bounds.centerY());
			
			zWindow = mManager.setupPositioning(zWindow);
			// get drawing helper params
			_scale = mManager.getCurScale();
			_pan = mManager.getCurPan(_pan);
			
			bReadyToDraw = true;
			pts = points;
			pointsmod = points / 100;
			i = mStartingPointCount;
			j = mStartingPointCount;
			pointsDrawn = i;
			trans = mManager.mTrans.copyData();
			tc = trans.length;
		}

		float scalex = dim_x / zWindow.width();
		float scaley = dim_y / zWindow.height();
		
		Random rnd = new Random();

		float x = 0.5f;
		float y = 0.5f;
		float prob, a, b, c, d, e, f, newx, newy, div;
		int t, plotx, ploty, ir, ig, ib, table, pix, clr;
		int[] clrtab;
		while (i < pts) {
			// update in case weve changed stuff
			synchronized (mManager.mLock) {
				fgColors = mManager.mColorScheme.mFGColors;
				colorSteps = mManager.mColorScheme.mSteps;
				pts = points;
				pointsmod = points / 100;
				if (task != null)
					if (task.mCancel)
						return;
			}
			
			// update status
			if ((j % pointsmod) == 0) {
				percentdone = (float) (i) / (float) (points) * 100;
				if (task != null)
					task.renderingStatus(starttime, percentdone);
				mManager.invalidate();
			}
			
			// pick a random transformation to use
			t = 0;
			prob = rnd.nextFloat();
			while (t < tc) {
				prob -= trans[t][EditorView.IFS_P];
				if (prob <= 0)
					break;
				t++;
			}
			if (t >= tc) // won't happen with a proper ifs
				t = tc - 1;
			a = trans[t][EditorView.IFS_A];
			b = trans[t][EditorView.IFS_B];
			c = trans[t][EditorView.IFS_C];
			d = trans[t][EditorView.IFS_D];
			e = trans[t][EditorView.IFS_E];
			f = trans[t][EditorView.IFS_F];
			newx = a * x + b * y + e;
			newy = c * x + d * y + f;
			x = newx;
			y = newy;
			// simplified rounding by adding 0.5 and flooring
			// to remove floor, we just cast to int
			// but now we need to do the math offset 1
			// due to differences between floor and (int) in negative #s
			plotx = (int) ((x - zWindow.left) * scalex + 1.5f) - 1;
			ploty = (int) (dim_y - ((int) ((y - zWindow.top) * scaley + 1.5f) - 1));
			if (plotx >= 0 && ploty >= 0 && plotx < dim_x && ploty < dim_y) {
				synchronized (mManager.mLock) {
					if (task != null)
						if (task.mCancel)
							return;
					mImage[t % mImage.length][plotx][ploty]++;
					i++;
					pointsDrawn = i;
				}

				// update bitmap
				ir = 0;
				ig = 0;
				ib = 0;
				table = 0;
				div = 0.0f;
				synchronized (mManager.mLock) {
					if (task != null)
						if (task.mCancel)
							return;
					for (int pix_i = 0; pix_i < mImage.length; pix_i++) {
						pix = mImage[pix_i][plotx][ploty];
						table = (int) (table % fgColors.length);
						if (pix > 0) {
							clrtab = fgColors[table];
							pix = (pix < colorSteps && pix >= 0) ? pix : (colorSteps - 1);
							clr = clrtab[pix];
							div += pix;
							ir += ((clr >> 16) & 0xFF) * pix;
							ig += ((clr >> 8) & 0xFF) * pix;
							ib += (clr & 0xFF) * pix;
						}
						table += 1;
					}
				}
				if (div > 0 && i >= EditorView.IFS_BUFFER_POINTS) {
					clr = (0xFF000000) | ((int) (ir / div) << 16) | ((int) (ig / div) << 8) | (int) (ib / div);
					synchronized (mManager.mLock) {
						if (task != null)
							if (task.mCancel)
								return;
						if (mIm != null && !mIm.isRecycled())
							mIm.setPixel((int) (plotx + dim_xo),
									(int) (ploty + dim_yo), clr);
					}
				}
			}

			// abort
			if (j > IFS_WARN && i < 10) {
				bError = true;
				return;
			}

			j++;
			
			if (i >= pts) {
				synchronized (mManager.mLock) {
					bFinished = true;
				}
			}
		}
		
		if (mManager.isPreview) // release memory
			mImage = null;

		if (task != null)
			task.renderingStatus(starttime, 100);
		mManager.invalidate();
		return;
	}
	
	/**
	 * Update the number of points to render,
	 *  restarting the render if applicable.
	 * @param newPts : the new number of points
	 */
	public void setPoints(int newPts) {
		synchronized (mManager.mLock) {
			mStartingPointCount = (newPts > points) ? points : 0;
			points = newPts;
			if (bFinished || pointsDrawn > newPts) {
				mTask.mCancel = true;
				mTask = new IFSRenderTask();
				mTask.execute();
			}
		}
	}
	
	/**
	 * Resume a render that stopped because it was done,
	 *  but now we've increased the number of points to render.
	 */
	public void resume() {
		synchronized (mManager.mLock) {
			if (pointsDrawn >= points)
				return;
			mStartingPointCount = pointsDrawn;
			mTask = new IFSRenderTask();
			mTask.execute();
		}
	}
	
	/**
	 * Update the colors when the color scheme is updated.
	 * @param fv : the parent {@link FractalView}
	 */
	public void onColorMapUpdated(FractalView fv) {
		synchronized (mManager.mLock) {
			if (mIm == null || mIm.isRecycled() || mImage == null)
				return;
			try {
				int[][] fgColors = mManager.mColorScheme.mFGColors;
				int colorSteps = mManager.mColorScheme.mSteps;
				float div;
				int ir, ig, ib, table, pix, clr;
				for (int x = 0; x < mManager.mWidth; x++) {
					for (int y = 0; y < mManager.mHeight; y++) {
						// update bitmap
						ir = 0;
						ig = 0;
						ib = 0;
						table = 0;
						div = 0.0f;
						for (int pix_i = 0; pix_i < mImage.length; pix_i++) {
							pix = mImage[pix_i][x][y];
							table = (int) (table % fgColors.length);
							if (pix > 0) {
								clr = fgColors[table][(pix < colorSteps) ? pix : (colorSteps - 1)];
								div += pix;
								ir += ((clr >> 16) & 0xFF) * pix;
								ig += ((clr >> 8) & 0xFF) * pix;
								ib += (clr & 0xFF) * pix;
							}
							table += 1;
						}
						if (div > 0) {
							clr = (0xFF000000) | ((int) (ir / div) << 16) | ((int) (ig / div) << 8) | (int) (ib / div);
							mIm.setPixel(x, y, clr);
						} else {
							mIm.setPixel(x, y, 0);
						}
					}
				}
			} catch (Exception ex) {
				ErrorReporter.getInstance().report("UpdateColorScheme", ex);
			}
		}
		mManager.invalidate();
	}

	/**
	 * Directly render the fractal. Only call this outside of the UI thread!
	 */
	public void renderThreadless() {
		synchronized (mManager.mLock) {
			mStartingPointCount = 0;
			bError = false;
			_progress = 0;
			bFinished = false;
			bOOME = false;
		}
		try {
			drawIFS(null);
		} catch (OutOfMemoryError err) {
			bOOME = true;
		} catch (Exception ex) {
			ErrorReporter.getInstance().report("RenderThreadless", ex);
		}
		synchronized (mManager.mLock) {
			_progress = 100;
			mManager.onCompleteRender(bError, bOOME);
		}
		mManager.invalidate();
	}

	/**
	 * An AsyncTask that manages rendering a fractal.
	 */
	public class IFSRenderTask extends AsyncTask<Void, Integer, Void> {
		
		public boolean mCancel;
		
		@Override
		protected void onPreExecute() {
			mManager._home.setSupportProgress(1);
			mManager._home.setSupportProgressBarVisibility(true);
			synchronized (mManager.mLock) {
				bError = false;
				mCancel = false;
				_progress = 0;
				bFinished = false;
				bOOME = false;
			}
		}

		@Override
		protected Void doInBackground(Void... v) {
			try {
				drawIFS(this);
			} catch (OutOfMemoryError err) {
				bOOME = true;
			} catch (Exception ex) {
				ErrorReporter.getInstance().report("Render", ex);
			}
			return null;
		}
	
		@Override
		protected void onProgressUpdate(Integer... p) {
			synchronized (mManager.mLock) {
				_progress = p[0];
			}
			mManager._home.setSupportProgress((p[0] == 0) ? 1 : (p[0] * 100));
		}
	
		@Override
		protected void onPostExecute(Void v) {
			int p;
			synchronized (mManager.mLock) {
				p = _progress = 100;
			}
			mManager._home.setSupportProgress(p * 100);
			mManager._home.setSupportProgressBarVisibility(false);
			mManager.onCompleteRender(bError, bOOME);
			mManager.invalidate();
		}

		/**
		 * Update the rendering status
		 * @param time : time elapsed
		 * @param percent : percent from 0 to 100
		 */
		public void renderingStatus(long time, float percent) {
			publishProgress((int) percent);
		}
	}

	/**
	 * Return true if everything is set up so that progress can be drawn.
	 * @return true if ready
	 */
	public boolean readyToDraw() {
		synchronized (mManager.mLock) {
			return bReadyToDraw;
		}
	}
}
