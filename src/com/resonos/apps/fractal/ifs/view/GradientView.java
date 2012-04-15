package com.resonos.apps.fractal.ifs.view;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.resonos.apps.fractal.ifs.FragmentGradientEditor;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.model.ColorScheme.Gradient;
import com.resonos.apps.fractal.ifs.model.ColorScheme.KeyColor;
import com.resonos.apps.library.App;
import com.resonos.apps.library.util.AppUtils;
import com.resonos.apps.library.util.M;

public class GradientView extends View implements OnClickListener {

	private static final int THUMB_RADIUS_SEL = App.inDP(16);
	private static final int THUMB_RADIUS = App.inDP(10);
	private static final int MARGIN = THUMB_RADIUS_SEL * 3;
	
	Button bDel;
	Paint paint;
	Paint tabPaintOB, tabPaintI, tabPaintOW, solidPaint;

	int mHeight = -1;
	int mWidth = -1;
	
	Bitmap mBmp;
	
	public KeyColor mSelKey = null;
	boolean mTouchingKey = false;
	
	FragmentGradientEditor _f;
	
	boolean mEdit = false;
    
	Context mContext;
	public View mMainContainer;
	ImageButton bRemove;
	Gradient colors;
	
	int position;

	OnRemoveListener mRListener;
	GradientEditListener mGEListener;
	
	boolean mVertical = false;
	
	public interface OnRemoveListener {
		public void onRemove(GradientView gradientView);
	}
	
	public interface GradientEditListener {
		public void onSelectedColorChanged(GradientView gradientView, KeyColor sel);
	}

	/**
	 * Create a non-editable, but deletable Gradient
	 * @param f : the parent fragment
	 * @param rlis : a listener to see if it is deleted
	 * @param isDeletable 
	 */
	public GradientView(FragmentGradientEditor f, OnRemoveListener rlis, boolean isDeletable) {
		super(f.mActivity);
		_f = f;
		mContext = f._home;
		paint = new Paint();
		paint.setStyle(Style.FILL);
		
		mEdit = false;
		mRListener = rlis;

		// create a more complex view structure
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMainContainer = (LinearLayout) inflater.inflate(R.layout.list_item_gradient, null);
		bRemove = (ImageButton)mMainContainer.findViewById(R.id.bDelete);
		if (!isDeletable)
			bRemove.setVisibility(View.GONE);
		else {
			Drawable dr = AppUtils.getMutableDrawable(getContext(), R.drawable.ic_action_remove);
			bRemove.setImageDrawable(dr);
			bRemove.setColorFilter(new LightingColorFilter(0xFF000000, 0));
		}
		RelativeLayout rl = (RelativeLayout)mMainContainer.findViewById(R.id.gradientContainer);
		rl.addView(this, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, App.inDP(56)));
		bRemove.setOnClickListener(this);
	}

	/**
	 * Create an editable GradientView
	 * @param f : parent fragment
	 * @param listener : listener to use for changing {@link KeyColor}s
	 */
	public GradientView(FragmentGradientEditor f, GradientEditListener listener) {
		super(f.mActivity);
		_f = f;
		mContext = f._home;
		paint = new Paint();
		paint.setStyle(Style.FILL);
		
		mEdit = true;
		mGEListener = listener;
		
		mVertical = App.SCREEN_HEIGHT > App.SCREEN_WIDTH;

		// edit ui paints
		tabPaintOB = new Paint();
		tabPaintOB.setStyle(Style.STROKE);
		tabPaintOB.setColor(0xFF000000);
		tabPaintOB.setStrokeWidth(2*App.DENSITY);
		tabPaintOW = new Paint();
		tabPaintOW.setStyle(Style.FILL);
		tabPaintOW.setColor(0xFFFFFFFF);
		tabPaintI = new Paint();
		tabPaintI.setStyle(Style.FILL);
		tabPaintI.setColor(0xFFDDFFCC);
		solidPaint = new Paint();
	}

	@Override
	public void onClick(View v) {
		if (v == bRemove) {
			mRListener.onRemove(this);
		}
	}
	
	public View getMasterView() {
		return getEditing() ? this : mMainContainer;
	}
	
	public void setColors(int pos, ColorScheme colorMap) {
		position = pos;
		colors = colorMap.getFGColors().get(pos);
	}

	public Gradient getColors() {
		return colors;
	}
	
	public boolean getEditing() {
		return mEdit;
	}

	public int getPosition() {
		return position;
	}

	@Override
	protected void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w,h,ow,oh);
		mWidth = w;
		mHeight = h;
	}

	ArrayList<KeyColor> drawList = new ArrayList<KeyColor>();
	Rect rect = new Rect();
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mHeight == -1)
			return;
		Gradient g = getColors();
		ArrayList<KeyColor> fgColors = g.getColors();
		
		int size = (mVertical ? mHeight : mWidth) - MARGIN * 2;
		
		// get bitmap
		if (mBmp == null)
			mBmp = Bitmap.createBitmap(size, 1, Bitmap.Config.ARGB_8888);
		if (mBmp.getWidth() != size) {
			mBmp = M.freeBitmap(mBmp);
			mBmp = Bitmap.createBitmap(size, 1, Bitmap.Config.ARGB_8888);
		}
		
		// draw bitmap
		KeyColor kcl, kcc;
		for (int x = 0; x < mBmp.getWidth(); x++) {
			float pos = (float)x/size*ColorScheme.POSITION_MAX;
			for (int k = 1; k < fgColors.size(); k++) {
				kcl = fgColors.get(k-1);
				kcc = fgColors.get(k);
				if (pos >= kcl.getPos() && pos <= kcc.getPos()) {
					float percent = ((float)(pos - kcl.getPos()))/((float)(kcc.getPos() - kcl.getPos()));
					int color = M.blend(kcl.getColor(), kcc.getColor(), percent);
					mBmp.setPixel(x, 0, color);
				}
			}
		}
		
		// draw the gradient, complete with padding

		int drawWidth = mVertical ? mHeight : mWidth;
		int drawHeight = mVertical ? mWidth : mHeight;
		if (mVertical) {
			canvas.save();
			canvas.rotate(90, mWidth/2f, mHeight/2f);
			canvas.translate(mWidth/2f - mHeight/2f, mHeight/2f - mWidth/2f);
		}
		
		if (getEditing()) {
			solidPaint.setColor(mBmp.getPixel(0, 0));
			rect.set(0, 0, MARGIN, drawHeight);
			canvas.drawRect(rect, solidPaint);
	
			rect.set(MARGIN, 0, drawWidth - MARGIN, drawHeight);
			canvas.drawBitmap(mBmp, null, rect, null);
	
			solidPaint.setColor(mBmp.getPixel(mBmp.getWidth() - 1, 0));
			rect.set(drawWidth - MARGIN, 0, drawWidth, drawHeight);
			canvas.drawRect(rect, solidPaint);
		} else {
			rect.set(0, 0, drawWidth, drawHeight);
			canvas.drawBitmap(mBmp, null, rect, null);
		}
		
		if (mVertical)
			canvas.restore();
		
		if (!getEditing())
			return;
		
		// make list of tabs to draw
		drawList.clear();
		KeyColor kc;
		for (int i = 0; i < fgColors.size(); i++) {
			kc = fgColors.get(i);
			if (kc.isFixed() && !kc.equals(mSelKey))
				drawList.add(kc);
		}
		for (int i = 0; i < fgColors.size(); i++) {
			kc = fgColors.get(i);
			if (!kc.isFixed() && !kc.equals(mSelKey))
				drawList.add(kc);
		}
		if (mSelKey != null)
			drawList.add(mSelKey);
		
		// now actually draw the tabs
		for (int k = 0; k < drawList.size(); k++) {
			KeyColor drawKC = drawList.get(k);
			int clr = drawList.get(k).getColor();
			int xx = (int)Math.round(((float)drawList.get(k).getPos())/ColorScheme.POSITION_MAX*(drawWidth - (2*MARGIN))) + MARGIN;
			int yy = drawHeight/2;
			float radius;
			if (drawKC.equals(mSelKey))
				radius = THUMB_RADIUS_SEL;
			else
				radius = THUMB_RADIUS;
			
			float pad = 2 * App.DENSITY;
			
			int contrastColor = M.getContrastColorYIQ(clr);
			int negative = contrastColor == Color.BLACK ? Color.WHITE : Color.BLACK;
			tabPaintOW.setColor(negative);
			tabPaintOB.setColor(contrastColor);
			tabPaintI.setColor(clr);
			
			if (mVertical) {
				canvas.drawRect(0, xx - pad, getWidth(), xx + pad, drawKC.equals(mSelKey) ? tabPaintI : tabPaintOW);
				canvas.drawLine(0, xx - pad, getWidth(), xx - pad, tabPaintOB);
				canvas.drawLine(0, xx + pad, getWidth(), xx + pad, tabPaintOB);
				canvas.drawCircle(yy, xx, radius + pad, tabPaintI);
				canvas.drawCircle(yy, xx, radius + pad, tabPaintOB);
			} else {
				canvas.drawRect(xx - pad, 0, xx + pad, getHeight(), drawKC.equals(mSelKey) ? tabPaintI : tabPaintOW);
				canvas.drawLine(xx - pad, 0, xx - pad, getHeight(), tabPaintOB);
				canvas.drawLine(xx + pad, 0, xx + pad, getHeight(), tabPaintOB);
				canvas.drawCircle(xx, yy, radius + pad, tabPaintI);
				canvas.drawCircle(xx, yy, radius + pad, tabPaintOB);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (!getEditing())
			return false;
		Gradient g = getColors();
		ArrayList<KeyColor> fgColors = g.getColors();
		float colorTouchPos = (mVertical ? e.getY() : e.getX()) - MARGIN;
		float colorAmount = mBmp.getWidth();
		float otherTouchPos = (mVertical ? e.getX() : e.getY());
//		int drawWidth = mVertical ? mHeight : mWidth;
		int drawHeight = mVertical ? mWidth : mHeight;
		
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mTouchingKey = false;
			if (otherTouchPos >= 0 && otherTouchPos <= drawHeight) {
				// make list of tabs to search
				drawList.clear();
				KeyColor kc;
				if (mSelKey != null)
					drawList.add(mSelKey);
				for (int i = 0; i < fgColors.size(); i++) {
					kc = fgColors.get(i);
					if (!kc.isFixed() && !kc.equals(mSelKey))
						drawList.add(kc);
				}
				for (int i = 0; i < fgColors.size(); i++) {
					kc = fgColors.get(i);
					if (kc.isFixed() && !kc.equals(mSelKey))
						drawList.add(kc);
				}
				
				// search for nearby keys
				float bestDist = 99999; // sufficiently large number;
				mSelKey = null;
				for (int k = 0; k < drawList.size(); k++) {
					float xx = (float)drawList.get(k).getPos()/ColorScheme.POSITION_MAX*colorAmount;
					if (Math.abs(colorTouchPos - xx) <= (THUMB_RADIUS_SEL)) {
						// got a handle
						float curDist = Math.abs(colorTouchPos - xx);
						if (curDist < bestDist) {
							bestDist = curDist;
							mSelKey = drawList.get(k);
						}
					}
				}
				
				// found a nearby key
				if (mSelKey != null) {
					mTouchingKey = true;
					mGEListener.onSelectedColorChanged(this, mSelKey);
			    	_f._home.invalidateOptionsMenu();
					invalidate();
					return true;
				}
				
				// now just create a key
				if (colorTouchPos > 0 && colorTouchPos < colorAmount) {
					int color = 0xFF000000;
					float pos = colorTouchPos/colorAmount*ColorScheme.POSITION_MAX;
					KeyColor kcl, kcc;
					for (int k = 1; k < fgColors.size(); k++) {
						kcl = fgColors.get(k-1);
						kcc = fgColors.get(k);
						if (pos >= kcl.getPos() && pos <= kcc.getPos()) {
							float percent = ((float)(pos - kcl.getPos()))/((float)(kcc.getPos() - kcl.getPos()));
							color = M.blend(kcl.getColor(), kcc.getColor(), percent);
						}
					}
					mSelKey = colors.add(color, (int)Math.round(pos));
					_f._home.invalidateOptionsMenu();
					mGEListener.onSelectedColorChanged(this, mSelKey);
					mTouchingKey = true;
					invalidate();
					return true;
				}
			}
			return false;
		case MotionEvent.ACTION_MOVE:
			if (mSelKey != null && mTouchingKey) {
				if (mSelKey != null) {
					if (!mSelKey.isFixed()) {
						int pos = Math.round(colorTouchPos/colorAmount*ColorScheme.POSITION_MAX);
						int pad = Math.round(THUMB_RADIUS/(float)ColorScheme.POSITION_MAX*colorAmount);
						mSelKey.setPos(Math.max(pad, Math.min(ColorScheme.POSITION_MAX - pad, pos)));
						Collections.sort(fgColors);
					}
				}
			}
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			mTouchingKey = false;
			invalidate();
			break;
		}
		return true;
	}
}