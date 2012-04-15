package com.resonos.apps.fractal.ifs.view;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.View;

import com.resonos.apps.fractal.ifs.FragmentEditor;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.model.IFSCoord;
import com.resonos.apps.fractal.ifs.model.IFSFractal;
import com.resonos.apps.fractal.ifs.model.IFSFractal.RawIFSFractal;
import com.resonos.apps.fractal.ifs.model.Polygon;
import com.resonos.apps.fractal.ifs.model.Transformation;
import com.resonos.apps.library.App;
import com.resonos.apps.library.model.Coord;
import com.resonos.apps.library.model.ImmutableCoord;
import com.resonos.apps.library.util.AppUtils;
import com.resonos.apps.library.util.M;
import com.resonos.apps.library.util.TouchViewWorker;
import com.resonos.apps.library.util.TouchViewWorker.Param;
import com.resonos.apps.library.util.TouchViewWorker.TouchMode;
import com.resonos.apps.library.util.TouchViewWorker.TouchViewReceiver;

/**
 * This class creates a view that can edit an {@link IFSFractal}
 * using visual representations of {@link Transformation}s.
 * @author Chris
 */
public class EditorView extends View {

	// constants
	
	/** The default number of points to render */
	public static final int DEF_IFS_EDITOR_POINTS = 10000;
	
	/** The default grid unit for the editor */
	public static final float DEF_IFS_EDITOR_GRID = 0.03125f;
	
	/** The minimum unit for skew parameters */
	public static final float MIN_IFS_EDITOR_SKEW_SIZE = 0.001f;
	
	/** The starting x coordinate for rendering a fractal */
	public static final float IFS_XSTART = 0.5f;
	
	/** The starting y coordinate for rendering a fractal */
	public static final float IFS_YSTART = 0.5f;
	
	/** Names for indeces in transformation septuplets */
	public static final int IFS_A = 0, IFS_B = 1, IFS_C = 2, IFS_D = 3, IFS_E = 4, IFS_F = 5, IFS_P = 6;
	
	/** The default size of visible coordinate space */
	public static final float IFS_VISIBLE_SPACE = 1.25f;
	
	/** The size of the margin by default in coordinate space */
	public static final float IFS_MARGIN = 0.125f;
	
	/** The maximum unit size for transformations when restoring */
	public static final float IFS_RESTORE_MAX_UNIT_SIZE = 1.2f;
	
	/** The amount of points to throw away at the beginning of a render */
	public static final int IFS_BUFFER_POINTS = 15;
	
	/** The default touch threshold for grabbing a handle */
	public static final float TOUCH_THRESHOLD = App.DENSITY * 15;
	public static final float TOUCH_RADIUS_EXPAND = 0.6f;
	
	/** Default IFS to load */
	public static final String DEFAULT_IFS = "Spiral:Spoked Spiral 1";
	
	/** colors to draw */
	public static final int COLOR_LINE = Color.BLACK, COLOR_LINE_DIM = 0xFF999999,
				COLOR_LINE_HILITE = 0xFF0000FF, COLOR_LINE_HILITE_DIM = 0xFF0000AA;

	// preferences and state
	private static final String PREF_GRID = "prefEditorGrid";
	private static final String STATE_TRANS = "editorTrans";
	private static final String STATE_SEL = "indexSel";
	private static final String STATE_SEL_HANDLE = "handleSel";
	private static final String STATE_TVW = "tvw";
	
	/** an Enum representing all possible interactive handles on a transformation */
	public enum Handle {ROTATE(0, Color.MAGENTA), MOVE(-1, Color.BLUE), SIZE2(2, 0xFF22FC33),
			SKEW1(1, Color.RED), SKEW2(3, Color.RED), NONE(-2, Color.WHITE);
		private int index, selClr;
		/**
		 * Init a handle definition
		 * @param i : a unique index
		 * @param selC : the color when selected
		 */
		Handle(int i, int selC) {
			index = i;
			selClr = selC;
		}
	};
	
	// context
	FragmentEditor _f;

	// object
	private Paint paintBox, paintGrid, paintLine, paintLineSel,
			paintHandle, paintHandleSel, paintHandleSelOutline;
	private TouchViewWorker mTVW;
	private ArrayList<Transformation> tOrder = new ArrayList<Transformation>();
	private ArrayList<Transformation> tSearch = new ArrayList<Transformation>();
	private Random mRnd = new Random();
	
	// vars
	private int mEditorSize;
	private int mOffX, mOffY;
	private float mGridSize = DEF_IFS_EDITOR_GRID;
	private boolean bDirty = true;
	private final int pointCount;
	private final float[] points;
	private IFSCoord ctrPt;
	private float[] mXS = new float[4];
	private float[] mYS = new float[4];
	private RectF inputR = new RectF();
	private IFSCoord mouse = new IFSCoord(),
					origin = new IFSCoord();
	private Drawable mDrawRotate, mDrawMove, mDrawScale; 

	// saved state
	private boolean mUsingGrid = false; // sharedprefs
	private int mIndexSel;

	private Handle mHandleSel = Handle.NONE;
	private IFSFractal trans;
	
	@Override
	public Bundle onSaveInstanceState() {
		Bundle outState = new Bundle();
		if (mTVW != null)
			outState.putBundle(STATE_TVW, mTVW.onSaveInstanceState());
		outState.putInt(STATE_SEL, mIndexSel);
		outState.putInt(STATE_SEL_HANDLE, mHandleSel.ordinal());
		outState.putString(STATE_TRANS, trans.save());
		return outState;
	}
	
	/**
	 * Called when the parent fragment is pausing.
	 */
	public void onPause() {
		AppUtils.setSavedBoolean(_f._home.mApp, PREF_GRID, mUsingGrid);
	}

	/**
	 * Create the IFS Editor
	 * @param f : parent fragment
	 * @param savedInstanceState : state, if any
	 */
	public EditorView(FragmentEditor f, Bundle savedInstanceState) {
		super(f.getActivity());
		_f = f;
		
		pointCount = DEF_IFS_EDITOR_POINTS + IFS_BUFFER_POINTS;
		points = new float[(pointCount - IFS_BUFFER_POINTS) * 2];
		
		// restore state
		mUsingGrid = AppUtils.getSavedBoolean(_f._home.mApp, PREF_GRID, false);
		if (savedInstanceState != null) {
			mIndexSel = savedInstanceState.getInt(STATE_SEL);
			try {
				mHandleSel = Handle.values()[savedInstanceState.getInt(STATE_SEL_HANDLE)];
			} catch (Exception ex) {
				mHandleSel = Handle.NONE;
			}
			trans = IFSFractal.loadFromString(f._home, savedInstanceState.getString(STATE_TRANS));
		} else {
	        loadIFS(DEFAULT_IFS);
		}
		
        setFocusableInTouchMode(false);
        setOnKeyListener(null);
        setFocusable(false);

		paintBox = createPaint(Style.STROKE, COLOR_LINE);
		paintLine = createPaint(Style.STROKE, COLOR_LINE_DIM);
		paintGrid = createPaint(Style.STROKE, COLOR_LINE);
		paintLineSel = createPaint(Style.STROKE, COLOR_LINE_HILITE);
		paintHandle = createPaint(Style.FILL_AND_STROKE, COLOR_LINE_DIM);
		paintHandleSel = createPaint(Style.FILL_AND_STROKE, COLOR_LINE_HILITE);
		paintHandleSelOutline = createPaint(Style.STROKE, COLOR_LINE_HILITE_DIM);

		mDrawRotate = AppUtils.getMutableDrawable(_f._home, R.drawable.ic_action_rotate_left);
		mDrawMove = AppUtils.getMutableDrawable(_f._home, R.drawable.ic_action_fullscreen);
		mDrawScale = AppUtils.getMutableDrawable(_f._home, R.drawable.ic_action_fullscreen);
		mDrawRotate.setBounds(0, 0, App.inDP(24), App.inDP(24));
		mDrawMove.setBounds(0, 0, App.inDP(24), App.inDP(24));
		mDrawScale.setBounds(0, 0, App.inDP(24), App.inDP(24));
		mDrawRotate.setColorFilter(new LightingColorFilter(Handle.ROTATE.selClr, 0));
		mDrawMove.setColorFilter(new LightingColorFilter(Handle.MOVE.selClr, 0));
		mDrawScale.setColorFilter(new LightingColorFilter(Handle.SIZE2.selClr, 0));
		
		// set up touch handling
		Bundle touchBundle = (savedInstanceState == null) ? null : savedInstanceState.getBundle(STATE_TVW);
		mTVW = new TouchViewWorker(_f._home.mApp, this, touchBundle,
				new TouchViewReceiver() {
					private boolean noHandle = false;
					/**
					 * We override this method to determine if a transformation should be interacted with.
					 * If not, than we let the default behavior of the touchview continue.
					 */
					public boolean onTouchDown(Coord touchPointView, Coord touchPointModel) {
						Transformation t;
						tSearch.clear();
						if (mIndexSel != -1)
							tSearch.add(trans.getTrans(mIndexSel)); // search selected trans first
						for (int iT = 0; iT < trans.transCount(); iT++) {
							if (iT != mIndexSel)
								tSearch.add(trans.getTrans(iT));
						}
						int suggest_index_sel = -1;
						int changed = -1;
						
						// loop through all the touch handles twice to find a touched
						// match, with a bigger tolerance the second time searching 
						for (float mult = 1; mult <= (1 + TOUCH_RADIUS_EXPAND/2); mult+=TOUCH_RADIUS_EXPAND) { // misc values to optimize touching
							for (int iT = 0; iT < tSearch.size(); iT++) {
								t = tSearch.get(iT);
								int i = trans.getTransformationIndex(t);
								ArrayList<IFSCoord> coords = t.getCoordsView(EditorView.this.mEditorSize); // pixel
								ctrPt = t.getCenterPoint(true);
						        float point_angle_x = coords.get(0).x + (coords.get(2).x - coords.get(0).x)/2.0f,
			   						point_angle_y = coords.get(0).y + (coords.get(2).y - coords.get(0).y)/2.0f;
						        if (Math.abs(touchPointView.x - coords.get(2).x - mOffX) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()
						        		&& Math.abs(touchPointView.y - coords.get(2).y - mOffY) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()) {
						        	mHandleSel = Handle.SIZE2;
									changed = mIndexSel = i;
									break;
								}
								else if (Math.abs(touchPointView.x - coords.get(0).x - mOffX) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()
										&& Math.abs(touchPointView.y - coords.get(0).y - mOffY) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()) {
									mHandleSel = Handle.ROTATE;
									changed = mIndexSel = i;
									break;
								}
								else if (Math.abs(touchPointView.x - point_angle_x - mOffX) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()
										&& Math.abs(touchPointView.y - point_angle_y - mOffY) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()) {
									mHandleSel = Handle.MOVE;
									changed = mIndexSel = i;
									break;
								}
								else if (Math.abs(touchPointView.x - coords.get(1).x - mOffX) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()
										&& Math.abs(touchPointView.y - coords.get(1).y - mOffY) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()) {
									mHandleSel = Handle.SKEW1;
									changed = mIndexSel = i;
									break;
								}
								else if (Math.abs(touchPointView.x - coords.get(3).x - mOffX) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()
										&& Math.abs(touchPointView.y - coords.get(3).y - mOffY) <= TOUCH_THRESHOLD*mult/mTVW.getCurScale()) {
									mHandleSel = Handle.SKEW2;
									changed = mIndexSel = i;
									break;
								}
								
								for (int ii = 0; ii < coords.size(); ii++) {
									mXS[ii] = coords.get(ii).x;
									mYS[ii] = coords.get(ii).y;
								}
								if (Polygon.contains(mXS, mYS, 4, touchPointView.x-mOffX, touchPointView.y-mOffY))
									suggest_index_sel = i;
							}
						}
						noHandle = false;
						if (changed == -1 && suggest_index_sel != -1) {
							mIndexSel = suggest_index_sel;
							noHandle = true;
						}
						else if (changed == -1)
							mIndexSel = -1;
						_f.getToolbar().invalidateToolbar();
					    invalidate();
						return false;
					}
					
					/** don't pan if we've got something selected */
					public boolean startPan(Coord coord) {
						if (mIndexSel >= 0 && !noHandle)
							return false;
						return true;
					}
					
					/** don't zoom if we've got something selected */
					public boolean startScale(float curScale) {
						if (mIndexSel >= 0 && !noHandle)
							return false;
						return true;
					}
					
					/** redraw to reflect changes */
					public void changePan(Coord newPos) {
						invalidate();
					}
					
					/** redraw to reflect changes */
					public void changeScale(float newScale) {
						invalidate();
					}
					
					/** clear selection */
					public void onTouchUp() {
						mHandleSel = Handle.NONE;
				    }
					
					/** if we've got something selected, handle it ourselves, otherwise, pass it through */
					public void onTouchMove(Coord touchPoint, Coord touchStartModel) {
						if (mIndexSel >= 0) {
							Transformation t = trans.getTrans(mIndexSel);
							mouse.set(touchPoint.x-mOffX, touchPoint.y-mOffY).toRealPlane(EditorView.this.mEditorSize);
							
							float sx, sy, w, h;
							ArrayList<IFSCoord> coords;
							switch (mHandleSel) {
							case MOVE:
					            coords = t.getCoordsReal(); // real plane
					            t.pos.set(mouse.x - (t.size.x + t.skew.x*t.size.x)/2f,
					            		mouse.y - (t.size.y + t.skew.y*t.size.y)/2f)
				                       	.rotateAround(mouse, t.angle);
					            if (isUsingGrid()) {
					                t.pos.x += mGridSize/2f;
					                t.pos.x -= t.pos.x % mGridSize;
					                t.pos.y += mGridSize/2f;
					                t.pos.y -= t.pos.y % mGridSize;
					            }
					            setDirty(true);
						        invalidate();
						        break;
							case ROTATE:
					            coords = t.getCoordsReal(); // real plane
					            origin.set((coords.get(0).x + coords.get(2).x)/2f,
					                        					(coords.get(0).y + coords.get(2).y)/2f);
					            t.angle = ((float)Math.atan2((mouse.y-origin.y),(mouse.x-origin.x))
					            				- (float)Math.atan2(t.size.y + t.skew.y*t.size.y,
					                                t.size.x + t.skew.x*t.size.x) - (float)Math.PI ) % (2*(float)Math.PI);
	
					            if (isUsingGrid()) {
					            	t.angle += Math.toRadians(2.5);
					                t.angle -= t.angle % Math.toRadians(5);
					                t.angle = t.angle % (2*(float)Math.PI);
					            }
					                
					            float opp_angle = (t.angle + (float)Math.PI) % (2*(float)Math.PI);
					            t.pos.set(origin.x + (t.size.x + t.skew.x*t.size.x)/2f,
				                       	origin.y + (t.size.y + t.skew.y*t.size.y)/2f)
				                       	.rotateAround(origin, opp_angle);
					            setDirty(true);
						        invalidate();
						        break;
							case SIZE2:
					            mouse.rotateAround(t.pos, -t.angle);
					            ctrPt.set(t.getCenterPoint(false));
					            w = Math.max(2*(mouse.x-ctrPt.x)/(1+t.skew.x),MIN_IFS_EDITOR_SKEW_SIZE);
					            h = Math.max(2*(mouse.y-ctrPt.y)/(1+t.skew.y),MIN_IFS_EDITOR_SKEW_SIZE);
	
					            if (isUsingGrid()) {
					                w += mGridSize/2f;
					                w -= w % mGridSize;
					                h += mGridSize/2f;
					                h -= h % mGridSize;
					            }
	
					            ctrPt.set(ctrPt.x - (1+t.skew.x)*w/2, ctrPt.y - (1+t.skew.y)*h/2)
					            		.rotateAround(t.pos, t.angle);
					            t.pos.set(ctrPt);
					            t.size.set(w, h);
					            setDirty(true);
						        invalidate();
						        break;
							case SKEW1:
					            mouse.rotateAround(t.pos, -t.angle);
					            ctrPt.set(t.pos.x + t.size.x, t.pos.y + t.skew.y*t.size.y);
					            sx = t.skew.x;
					            sy = (mouse.y - t.pos.y)/t.size.y;
					            if (sx > 0)
					                sy = Math.min(sy, 1.0f/sx);
					            if (sx < 0)
					                sy = Math.max(sy, 1.0f/sx);
	
					            if (isUsingGrid()) {
					                sy += mGridSize;
					                sy -= sy % (mGridSize*2);
					            }
					            
					            t.skew.set(sx, sy);
					            setDirty(true);
						        invalidate();
						        break;
							case SKEW2:
					            mouse.rotateAround(t.pos, -t.angle);
					            ctrPt.set(t.pos.x + t.skew.x*t.size.x, t.pos.y + t.size.y);
					            sx = (mouse.x - t.pos.x)/t.size.x;
					            sy = t.skew.y;
					            if (sy > 0)
					                sx = Math.min(sx, 1.0f/sy);
					            if (sy < 0)
					                sx = Math.max(sx, 1.0f/sy);
	
					            if (isUsingGrid()) {
					                sx += mGridSize;
					                sx -= sx % (mGridSize*2);
					            }
					            
					            t.skew.set(sx, sy);
					            setDirty(true);
						        invalidate();
						        break;
							}
						}
					}
					
					/** Use the defaults to create a coordinate space */
					public void getWindow(RectF w) {
						mTVW.fitSizeInWindow(new Coord(IFS_XSTART, IFS_YSTART), new Coord(IFS_VISIBLE_SPACE, IFS_VISIBLE_SPACE), w);
					}
					
					// none of these methods we care about
					public void pointerDown() { }
					public void pointerUp(TouchMode tm) { }
					public boolean startRotate(float startingAngle) { return false; }
					public void changeRotate(float newAngle) { }
				},
				
				// these are all the features we need
				Param.PAN, Param.ZOOM, Param.FLIPY, Param.PAN_AFTER_TWO_FINGERS);
	}
	
	/**
	 * Resets the editor back to its starting pan and zoom.
	 */
	public void resetNavigation() {
		mTVW.resetAlteredWindow();
	}
	
	/**
	 * Helper to create a paint quickly
	 * @param s : paint style
	 * @param c : paint color
	 * @return
	 */
	private Paint createPaint(Style s, int c) {
		Paint p = new Paint();
		p.setStyle(s);
		p.setColor(c);
		return p;
	}

	@Override
	protected void onSizeChanged(int w, int h, int ow, int oh) {
		mEditorSize = Math.min(w, h);
		mOffX = (w - mEditorSize)/2;
		mOffY = (h - mEditorSize)/2;
		mTVW.updateWindowParameters();
	}

	/*
	 * these are just some equations to keep in mind
	 * float ox = (ix - w/2f) * scale + w/2f - panned.x / scale;
	 * float oy = (iy - h/2f) * scale + h/2f - panned.y / scale;
	 * float ix = (ox + panned.x / scale - w/2f) / scale + w/2f;
	 * float iy = (oy + panned.y / scale - h/2f) / scale + h/2f;
	 */
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (!_f.isCurrentFragment())
			return;
		
		mTVW.beginDrawing(canvas);
		
		drawPreview(canvas);
		
		ImmutableCoord panned = mTVW.getCurPanView();
		float scale = mTVW.getCurScale();
		
		float w = mEditorSize + mOffX * 2, h = mEditorSize + mOffY * 2;
		float brdr = this.mEditorSize/(10.0f);
		
		// draw grid
		if (isUsingGrid()) {
			inputR.left = (0 + panned.x - w/2f) / scale + w/2f;
			inputR.top = (0 + panned.y - h/2f) / scale + h/2f;
			inputR.right = (w + panned.x - w/2f) / scale + w/2f;
			inputR.bottom = (h + panned.y - h/2f) / scale + h/2f;

			float unit = (2*DEF_IFS_EDITOR_GRID * this.mEditorSize*(8f/10));
			mGridSize = DEF_IFS_EDITOR_GRID;
			
			int min = 22, max = 2*min;
			int count = Math.round(Math.min(inputR.height(),inputR.width())/unit);
			while (count < min) {
				count *= 2;
				unit /= 2;
				mGridSize /= 2;
			}
			while (count > max) {
				count /= 2;
				unit *= 2;
				mGridSize *= 2;
			}

			inputR.offset(-(mOffX+brdr), -(mOffY+brdr));
			inputR.left = inputR.left - (inputR.left % (unit)) - (unit);
			inputR.top = inputR.top - (inputR.top % (unit)) - (unit);
			inputR.right = inputR.right - (inputR.right % (unit)) + (unit);
			inputR.bottom = inputR.bottom - (inputR.bottom % (unit)) + (unit);
			inputR.offset((mOffX+brdr), (mOffY+brdr));
			
			for (float i = inputR.left; i <= inputR.right; i += unit) {
				float ox = (i + panned.x - w/2f) / scale + w/2f;//(i - w/2f) * scale + w/2f - panned.x / scale;
				int clr = Math.round(0xD5 + (0xFF - 0xD5) * M.fit(Math.abs(ox - w/2)/(2*mEditorSize) * scale - scale, 0, 1));
				paintGrid.setColor(Color.argb(255, clr, clr, clr));
				canvas.drawLine(i, inputR.top, i, inputR.bottom, paintGrid);
			}
			for (float i = inputR.top; i <= inputR.bottom; i += unit) {
				float oy = (i + panned.y - h/2f) / scale + h/2f;//(i - h/2f) * scale + h/2f - panned.y / scale;
				int clr = Math.round(0xD5 + (0xFF - 0xD5) * M.fit(Math.abs(oy - h/2)/(2*mEditorSize) * scale - scale, 0, 1));
				paintGrid.setColor(Color.argb(255, clr, clr, clr));
				canvas.drawLine(inputR.left, i, inputR.right, i, paintGrid);
			}
		}
		
		// draw bg
		canvas.drawRect(this.mOffX+this.mEditorSize/10.0f, this.mOffY+this.mEditorSize/10.0f,
						this.mOffX+9*this.mEditorSize/10.0f, this.mOffY+9*this.mEditorSize/10.0f, paintBox);
		
		// reorder transformations
		tOrder.clear();
		for (int i = 0; i < this.trans.transCount(); i++) {
			if (mIndexSel != i)
				tOrder.add(this.trans.getTrans(i));
		}
		if (mIndexSel >= 0 && mIndexSel < this.trans.transCount())
			tOrder.add(this.trans.getTrans(mIndexSel));
		else
			mIndexSel = -1;

		// draw transformations
		ArrayList<IFSCoord> coords;
		float ctrX, ctrY;
		int selected = (mIndexSel == -1) ? -1 : (tOrder.size() - 1);
		for (int i = 0; i < tOrder.size(); i++) {
			// get coords
			coords = tOrder.get(i).getCoordsView(this.mEditorSize); // pixel plane
	        ctrX = (coords.get(2).x + coords.get(0).x)/2.0f;
			ctrY = (coords.get(2).y + coords.get(0).y)/2.0f;
			
			// draw lines
			canvas.drawLine(this.mOffX+coords.get(0).x, this.mOffY+coords.get(0).y,
						this.mOffX+coords.get(1).x, this.mOffY+coords.get(1).y, (selected==i)?paintLineSel:paintLine);
			canvas.drawLine(this.mOffX+coords.get(1).x,this.mOffY+coords.get(1).y,
						this.mOffX+coords.get(2).x, this.mOffY+coords.get(2).y, (selected==i)?paintLineSel:paintLine);
			canvas.drawLine(this.mOffX+coords.get(2).x,this.mOffY+coords.get(2).y,
						this.mOffX+coords.get(3).x, this.mOffY+coords.get(3).y, (selected==i)?paintLineSel:paintLine);
			canvas.drawLine(this.mOffX+coords.get(3).x,this.mOffY+coords.get(3).y,
						this.mOffX+coords.get(0).x, this.mOffY+coords.get(0).y, (selected==i)?paintLineSel:paintLine);
			canvas.drawLine(this.mOffX+coords.get(0).x,this.mOffY+coords.get(0).y,
						this.mOffX+ctrX, this.mOffY+ctrY, (selected==i)?paintLineSel:paintLine);
			
			// draw handles
			drawHandle(canvas, coords, scale, Handle.SKEW1, (selected == i));
			drawHandle(canvas, coords, scale, Handle.SKEW2, (selected == i));
			drawHandle(canvas, coords, scale, Handle.ROTATE, (selected == i));
			drawHandle(canvas, coords, scale, Handle.MOVE, (selected == i));
			drawHandle(canvas, coords, scale, Handle.SIZE2, (selected == i));
			
			if (selected == i) {
				int size = App.inDP(24);
				int sizeZ = (int)(0.5f + FloatMath.sqrt(size * size + size * size));
				
				IFSCoord pt = coords.get(Handle.ROTATE.index);
				float drawX = (pt.x - ctrX), drawY = (pt.y - ctrY);
				float drawZ = FloatMath.sqrt(drawX * drawX + drawY * drawY);
				int tX = (int)(0.5f + pt.x + drawX * size / drawZ);
				int tY = (int)(0.5f + pt.y + drawY * size / drawZ);
				mDrawRotate.setBounds(this.mOffX+tX - size/2, this.mOffY+tY - size/2,
						this.mOffX+tX + size/2, this.mOffY+tY + size/2);
				canvas.save();
				canvas.scale(1f/scale, 1f/scale, mOffX + pt.x, mOffY + pt.y);
				mDrawRotate.draw(canvas);
				canvas.restore();

				pt = coords.get(Handle.SIZE2.index);
				tX = (int)(0.5f + pt.x - drawX * size / drawZ);
				tY = (int)(0.5f + pt.y - drawY * size / drawZ);
				mDrawScale.setBounds(this.mOffX+tX - size/2, this.mOffY+tY - size/2,
						this.mOffX+tX + size/2, this.mOffY+tY + size/2);
				canvas.save();
				canvas.scale(1f/scale, 1f/scale, mOffX + pt.x, mOffY + pt.y);
				mDrawScale.draw(canvas);
				canvas.restore();
				
				if (drawZ*scale >= (sizeZ*1.5f)) {
					tX = (int)(0.5f + ctrX - drawX * size / drawZ);
					tY = (int)(0.5f + ctrY - drawY * size / drawZ);
					mDrawMove.setBounds(this.mOffX+tX - size/2, this.mOffY+tY - size/2,
							this.mOffX+tX + size/2, this.mOffY+tY + size/2);
					canvas.save();
					canvas.scale(1f/scale, 1f/scale, mOffX + ctrX, mOffY + ctrY);
					canvas.rotate(45, mOffX + tX, mOffY + tY);
					mDrawMove.draw(canvas);
					canvas.restore();
				}
			}
		}

		mTVW.endDrawing(canvas);
//		mTVW.debugDraw(canvas, false);
	}
	
	/**
	 * Helper function to draw a handle
	 * @param canvas : the canvas to draw on
	 * @param coords : an array of coords representing a transformation
	 * @param scale : the zoom level to compensate for canvas matrix transformations
	 * @param h : the handle type
	 * @param sel : true if handle is selected
	 */
	private void drawHandle(Canvas canvas, ArrayList<IFSCoord> coords, float scale, Handle h, boolean sel) {
		float size = (sel ? 4 : 3) * App.DENSITY / scale;
		paintHandleSel.setColor(h.selClr);
		if (h == Handle.MOVE) {
	    	float point_angle_x = (coords.get(2).x + coords.get(0).x)/2.0f;
	    	float point_angle_y = (coords.get(2).y + coords.get(0).y)/2.0f;
			canvas.drawCircle(this.mOffX+point_angle_x, this.mOffY+point_angle_y,
					size, sel ? paintHandleSel : paintHandle);
			if (sel)
				canvas.drawCircle(this.mOffX+point_angle_x, this.mOffY+point_angle_y,
					size, paintHandleSelOutline);
		} else {
			canvas.drawCircle(this.mOffX+coords.get(h.index).x, this.mOffY+coords.get(h.index).y,
					size, sel ? paintHandleSel : paintHandle);
			if (sel)
				canvas.drawCircle(this.mOffX+coords.get(h.index).x, this.mOffY+coords.get(h.index).y,
						size, paintHandleSelOutline);
		}
	}
	
	/**
	 * Draws a preview of the fractal in this object.
	 * @param canvas : the canvas to draw on
	 */
	void drawPreview(Canvas canvas) {
        if (this.trans.transCount() == 0)
            return;
        
        // calc eqs
        RawIFSFractal fr = trans.calculateRaw();
        float[][] ts = fr.data;
        
        // clear
        canvas.drawColor(Color.WHITE);
        
        // draw preview
        if (this.trans.transCount() < 2)
            return;

        float xdist = 1.25f;
        float ydist = 1.25f;
        float scale = this.mEditorSize/Math.max(xdist,ydist);
        float xmin = -0.125f;
        float ymin = -0.125f;
        
//        int optAddXoff = Math.abs(xOff) + 1;
        
        try {
            float x = 0.5f;
            float y = 0.5f;
            int tcount = trans.transCount();
            
            int i = 0, t;
            float a,b,c,d,e,f,newx,newy, prob;
            while (i < pointCount) {
                t = 0;
                prob = mRnd.nextFloat();
                while (t < tcount) { // pick a random transformation to use
                    prob -= ts[t][IFS_P];
                    if (prob <= 0)
                        break;
                    t++;
                }
                if (t >= tcount) // won't happen with a proper ifs
                    t = tcount - 1;
                a = ts[t][IFS_A];
                b = ts[t][IFS_B];
                c = ts[t][IFS_C];
                d = ts[t][IFS_D];
                e = ts[t][IFS_E];
                f = ts[t][IFS_F];
                newx = a*x + b*y + e;
                newy = c*x + d*y + f;
                x = newx;
                y = newy;
                
                // draw
                if (i >= IFS_BUFFER_POINTS) {
					points[(i-IFS_BUFFER_POINTS)*2] = this.mOffX + (x - xmin) * scale;
					points[(i-IFS_BUFFER_POINTS)*2+1] = this.mOffY + (y - ymin) * scale;
                }
                i++;
            }
            canvas.drawPoints(points, paintBox);
        } catch (ArithmeticException ex) {
            // transformation not affine.
            return;
        }
	}
	
	/**
	 * Load an IFS from the fractal gallery
	 * @param name : the IFS name
	 */
	public void loadIFS(String name) {
		trans = IFSFractal.copyFromFractal(_f._home, _f._home.mGallery.getFractalByName(name));
	    setDirty(true);
        invalidate();
	}
	
	/**
	 * Restore transformations that may have been lost.
	 */
	public void btnRestore() {
		Transformation t;
        for (int i = 0; i < this.trans.transCount(); i++) {
        	t = this.trans.getTrans(i);
            if (t.pos.x < -IFS_MARGIN)
                t.pos.x = 0.0f;
            if (t.pos.y < -IFS_MARGIN)
            	t.pos.y = 0.0f;
            if (t.pos.x > IFS_VISIBLE_SPACE - IFS_MARGIN)
            	t.pos.x = 1.0f;
            if (t.pos.y > IFS_VISIBLE_SPACE - IFS_MARGIN)
            	t.pos.y = 1.0f;
            if (t.size.x > IFS_RESTORE_MAX_UNIT_SIZE)
            	t.size.x = 1.0f;
            if (t.size.y > IFS_RESTORE_MAX_UNIT_SIZE)
            	t.size.y = 1.0f;
        }
        setDirty(true);
        mTVW.resetAlteredWindow();
        invalidate();
	}
	
	/**
	 * Set the editor's new information state
	 * @param true for dirty
	 */
	public void setDirty(boolean dirty) {
		this.bDirty = dirty;
	}

	/**
	 * @return true if the editor's fractal has changed since last render or if there has not been a render
	 */
	public boolean isEditorDirty() {
		return bDirty;
	}

	/**
	 * Add a default transformation to the editor and its fractal.
	 */
	public void addTrans() {
		trans.addTrans(new Transformation(new IFSCoord(0.25f,0.25f),
				  new IFSCoord(0.5f,0.5f),
				  0f,
				  new IFSCoord(0f,0f)));
		mIndexSel = trans.transCount() - 1;
		invalidate();
	}

	/**
	 * Remove the selected transformations from the editor and its fractal.
	 */
	public void removeTrans() {
		if (mIndexSel != -1) {
			trans.removeTrans(mIndexSel);
			mIndexSel = -1;
			invalidate();
		}
	}

	/**
	 * @return true if a transformation is selected
	 */
	public boolean hasSelection() {
		return (mIndexSel != -1);
	}

	/**
	 * Set grid properties
	 * @param grid : true to show a grid and snap to point
	 */
	public void setUsingGrid(boolean grid) {
		this.mUsingGrid = grid;
	}

	/**
	 * @return whether we are using the grid or not
	 */
	public boolean isUsingGrid() {
		return mUsingGrid;
	}

	/**
	 * Returns the raw IFST data representing the fractal for saving.
	 * @return The IFST data in String form
	 */
	public IFSFractal saveTrans() {
		// currently we just return a copy
		return IFSFractal.copyFromFractal(_f._home, trans);
	}

	/**
	 * Returns the fractal this editor is showing.
	 * @return an {@link IFSFractal} object
	 */
	public IFSFractal getFractal() {
		return trans;
	}
	
	/**
	 * @return The currently selected transformation, or -1 if none.
	 */
	public int getSelectedTrans() {
		return mIndexSel;
	}

	/**
	 * Sets the currently selected transformation
	 * @param s : transformation index, or -1 for none
	 */
	public void setSelectedTrans(int s) {
		this.mIndexSel = s;
	}

	/**
	 * Sets whether the editor will interact with the user's touches
	 * @param b : true to allow interaction
	 */
	public void setAcceptingInput(boolean b) {
		mTVW.setEnabled(b);
	}
}
