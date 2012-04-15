package com.resonos.apps.fractal.ifs.model;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.resonos.apps.fractal.ifs.Home;
import com.resonos.apps.library.App;
import com.resonos.apps.library.util.M;

/**
 * This class represents a color scheme for a fractal.
 * Is is composed of a background color and a gradient.
 * The gradient is made up of 2 or more keys, placed from 0 to 400.
 * Each key has a color and the colors between are interpolated.
 * @author Chris
 */
public class ColorScheme {

	// constants
	/** Color keys in gradients range from 0 to this value */
	public static final int POSITION_MAX = 400;
	public static final int DEFAULT_COLOR_STEPS = 25;

	// serialized data
	ArrayList<Gradient> _gradients;
	int _bg;
	private int _steps; // unused
	public String _name;
	private int _versionID = 1;
	
	public static class Gradient {
		private ArrayList<KeyColor> _colors;
		public Gradient() {
			_colors = new ArrayList<KeyColor>();
			_colors.add(new KeyColor(0xFF000000, 0, _colors));
			_colors.add(new KeyColor(0xFF888888, POSITION_MAX / 2, _colors));
			_colors.add(new KeyColor(0xFFFFFFFF, POSITION_MAX, _colors));
		}
		public Gradient(ArrayList<KeyColor> nkcs) {
			_colors = nkcs;
		}
		public ArrayList<KeyColor> getColors() {
			return _colors;
		}
		public int keyCount() {
			return _colors.size();
		}
		public KeyColor getKey(int i) {
			return _colors.get(i);
		}

		/**
		 * Creates a new {@link KeyColor} and adds it to the specified gradient.
		 * @param color : the color
		 * @param pos : the key position, from 0 to {@link POSITION_MAX}
		 * @return the identifier of the new key
		 */
		public KeyColor add(int color, int pos) {
			KeyColor kc = new ColorScheme.KeyColor(color, pos, _colors);
			_colors.add(kc);
			return kc;
		}

		/**
		 * Get a key from a gradient by its ID
		 * @param id : key identifier
		 * @return the {@link KeyColor} object, if found, otherwise null
		 */
		public KeyColor getKeyById(int id) {
			for (int i = 0; i < _colors.size(); i++)
				if (_colors.get(i).getID() == id)
					return _colors.get(i);
			return null;
		}

		/**
		 * Remove a key from a gradient by its ID
		 * @param id : key identifier
		 */
		public void delKeyById(int id) {
			KeyColor key;
			for (int i = 0; i < _colors.size(); i++) {
				key = _colors.get(i);
				if (key._id == id && !key.isFixed()) {
					_colors.remove(i);
					break;
				}
			}
		}
	}

	/** Creates a default color scheme. */
	public ColorScheme() {
		_gradients = new ArrayList<Gradient>();
		_bg = (0xFF000000);
		_steps = DEFAULT_COLOR_STEPS;
		setDefaultForeground();
	}

	private void setDefaultForeground() {
		_gradients.clear();
		_gradients.add(new Gradient());
	}

	/** Creates this color scheme based on another color scheme. */
	public ColorScheme(ColorScheme copy) {
		_bg = (copy.getBGColor());
		_steps = copy._steps;
		_gradients = new ArrayList<Gradient>();
		for (Gradient kcs : copy._gradients) {
			ArrayList<KeyColor> nkcs = new ArrayList<KeyColor>();
			for (KeyColor kc : kcs.getColors()) {
				KeyColor nkc = new KeyColor(kc.getColor(), kc.getPos(), nkcs);
				nkcs.add(nkc);
			}
			_gradients.add(new Gradient(nkcs));
		}
		_versionID = copy._versionID;
		_name = copy._name;
	}

	/** Loads a textual representation of a color scheme. */ 
	public static ColorScheme loadFromString(Home home, String data) {
		ColorScheme cs;
		try {
	    	GsonBuilder gsonBilder = new GsonBuilder();
	    	Gson gson = gsonBilder.create();
	    	cs = gson.fromJson(data, ColorScheme.class);
			cs.fixOnLoading(home.mApp);
			if (cs._gradients.size() == 0)
				cs.setDefaultForeground();
			return cs;
		} catch (Exception ex) {
			home.mApp.mError.report("LoadColorScheme", ex, data);
			cs = new ColorScheme();
			return cs;
		}
	}

	/** Converts this color scheme into a textual representation. */
	public String save() {
    	GsonBuilder gsonBilder = new GsonBuilder();
    	Gson gson = gsonBilder.create();
        return gson.toJson(this, ColorScheme.class);
	}

	/**
	 * @return Returns the number of gradients in this color scheme.
	 */
	public int getGradientCount() {
		return _gradients.size();
	}

	/**
	 * Retrieves a gradient
	 * @param i : the gradient index
	 * @return : the gradient, in the format of a list of {@link KeyColor}s
	 */
	public Gradient getGradient(int i) {
		return _gradients.get(i);
	}
	
	/**
	 * This class represents a "rendered" color scheme, a version of a {@link ColorScheme}
	 * that has been prepared with a certain number of steps for quick access
	 * during intense computations.
	 * @author Chris
	 */
	public static class PreparedColorScheme {
		
		// vars
		private final ArrayList<int[]> mGradientColors;
		public final int mSteps;
		
		// public vars
		public final int mBGColor;
		public final int[][] mFGColors;
		
		public PreparedColorScheme(ColorScheme cs, int stps) {
			mSteps = stps;
			mGradientColors = new ArrayList<int[]>();
			KeyColor kcl, kcc;
			for (int y = 0; y < cs._gradients.size(); y++) {
				int[] steps = new int[mSteps];
				for (float x = 0; x < mSteps; x++) {
					float pos = ((float) x) / ((float) mSteps) * POSITION_MAX;
					for (int k = 1; k < cs._gradients.get(y).keyCount(); k++) {
						kcl = cs._gradients.get(y).getKey(k - 1);
						kcc = cs._gradients.get(y).getKey(k);
						if (pos >= kcl.getPos() && pos <= kcc.getPos()) {
							float percent = ((float) (pos - kcl.getPos()))
									/ ((float) (kcc.getPos() - kcl.getPos()));

							steps[(int) x] = Color.argb(
									255,
									(int) (Color.red(kcl.getColor()) + (percent)
											* (Color.red(kcc.getColor()) - Color
													.red(kcl.getColor()))),
									(int) (Color.green(kcl.getColor()) + (percent)
											* (Color.green(kcc.getColor()) - Color
													.green(kcl.getColor()))),
									(int) (Color.blue(kcl.getColor()) + (percent)
											* (Color.blue(kcc.getColor()) - Color
													.blue(kcl.getColor()))));
						}
					}
				}
				mGradientColors.add(steps);
			}

			int[][] arr = new int[mGradientColors.size()][];
			mFGColors = mGradientColors.toArray(arr);
			mBGColor = cs.getBGColor();
		}
	}

	/**
	 * Prepare a gradient object from this color scheme based on a number of steps.
	 * @param stps : the number of color steps to calculate
	 * @return A gradient object containing precalculated data.
	 */
	public PreparedColorScheme prepareGradient(int stps) {
		return new PreparedColorScheme(this, stps);
	}

	/**
	 * Generates a bitmap preview of this color scheme.
	 * @param width : image width
	 * @param height : image height
	 * @param borders : whether or not to include a black border
	 * @param brdrColor : the color of the border
	 * @return The Bitmap image
	 */
	public Bitmap generatePreview(int width, int height, boolean borders, int brdrColor) {
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		// draw
		ColorScheme tempCM = new ColorScheme(this);
		int bgWidth = (width - 2) / 6;
		int fgWidth = width - 2 - bgWidth;
		int bgHeight = height - 2;
		tempCM.prepareGradient(fgWidth);
		Canvas canvas = new Canvas(bmp);

		// border
		if (borders)
			canvas.drawColor(brdrColor);

		// bg
		Paint paint = new Paint();
		paint.setColor(getBGColor());
		if (borders)
			canvas.drawRect(1, 1, 1 + bgWidth, 1 + bgHeight, paint);
		else
			canvas.drawRect(0, 0, 1 + bgWidth, 2 + bgHeight, paint);

		// fg
		Bitmap fg = makeGradient(fgWidth);
		canvas.drawBitmap(fg, null, borders
				? new RectF(1 + bgWidth, 1, 1 + bgWidth	+ fgWidth, 1 + bgHeight)
				: new RectF(bgWidth, 0, bgWidth + fgWidth + 2, bgHeight + 2), null);
		fg = M.freeBitmap(fg);

		return bmp;
	}

	/**
	 * Creates the Gradient portion of a color scheme preview
	 * @param width : the width of the gradient to render
	 * @return A bitmap previewing the gradient of the width input and a height with one pixel for each sub=gradient.
	 */
	private Bitmap makeGradient(int width) {
		Paint paint = new Paint();
		paint.setStyle(Style.STROKE);
		Bitmap bmp = Bitmap.createBitmap(width, _gradients.size(),
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);
		KeyColor kcl, kcc;
		for (int y = 0; y < _gradients.size(); y++) {
			for (int x = 0; x < canvas.getWidth(); x++) {
				float pos = ((float) x) / ((float) canvas.getWidth()) * 400f;
				for (int k = 1; k < _gradients.get(y).keyCount(); k++) {
					kcl = _gradients.get(y).getKey(k - 1);
					kcc = _gradients.get(y).getKey(k);
					if (pos >= kcl.getPos() && pos <= kcc.getPos()) {
						float percent = ((float) (pos - kcl.getPos()))
								/ ((float) (kcc.getPos() - kcl.getPos()));

						int color = Color.argb(
								255,
								(int) (Color.red(kcl.getColor()) + (percent)
										* (Color.red(kcc.getColor()) - Color
												.red(kcl.getColor()))),
								(int) (Color.green(kcl.getColor()) + (percent)
										* (Color.green(kcc.getColor()) - Color
												.green(kcl.getColor()))),
								(int) (Color.blue(kcl.getColor()) + (percent)
										* (Color.blue(kcc.getColor()) - Color
												.blue(kcl.getColor()))));
						paint.setColor(color);
						canvas.drawPoint(x, y, paint);
					}
				}
			}
		}
		return bmp;
	}

	/**
	 * @return the background color
	 */
	public int getBGColor() {
		return _bg;
	}

	/**
	 * This class represents a single key point in a gradient.
	 */
	public static class KeyColor implements Comparable<KeyColor> {
		private int _color;
		private int _pos;
		private int _id;

		/**
		 * Create a new KeyColor
		 * @param c : the color
		 * @param p : the position, from 0 to {@link POSITION_MAX}
		 * @param keys : the KeyColors already in the gradient, to avoid ID clashes
		 */
		public KeyColor(int c, int p, ArrayList<KeyColor> keys) {
			_color = (c < 0xFF000000) ? (0xFF000000 + c) : c;
			_pos = p;
			int i = 0;
			boolean found = false;
			while (!found) {
				boolean found2 = false;
				for (int iK = 0; iK < keys.size(); iK++) {
					if (keys.get(iK)._id == i) {
						found2 = true;
						break;
					}
				}
				if (!found2) {
					found = true;
					break;
				}
				i++;
			}
			_id = i;
		}

		/** returns the color of this key */
		public int getColor() {
			return _color | 0xFF000000; // forcefully opaque
		}

		/** returns the position of this key, from 0 to {@link POSITION_MAX} */
		public int getPos() {
			return _pos;
		}

		/** returns the unique ID of this key */
		public int getID() {
			return _id;
		}

		/**
		 * Just compares the IDs of two KeyColors
		 * @param kc : another KeyColor, or null
		 * @return true if the IDs match
		 */
		public boolean equals(KeyColor kc) {
			if (kc == null)
				return false;
			return kc.getID() == _id;
		}

		@Override
		public int compareTo(KeyColor other) {
			if (this.getPos() == other.getPos()) {
				return 0;
			} else if (this.getPos() > other.getPos()) {
				return 1;
			} else {
				return -1;
			}
		}

		/** sets the position, from 0 to {@link POSITION_MAX}, of this key */
		public void setPos(int p) {
			_pos = p;
		}

		/** sets the color of this key */
		public void setColor(int c) {
			_color = c;
		}

		public boolean isFixed() {
			return _pos == 0 || _pos == POSITION_MAX;
		}
	}

	/**
	 * This function is to do any corrections necessary after creating objects
	 * from JSON. This includes updating objects in future versions loaded from previous versions.
	 * @param app : the App object
	 */
	public void fixOnLoading(App app) {
		//
	}

	/**
	 * Gets the foreground color instructions.
	 * @return An array of gradients, each represented by an array of {@link KeyColor}s
	 */
	public ArrayList<Gradient> getFGColors() {
		return _gradients;
	}

	/**
	 * Sets the background color
	 * @param color : the new background color
	 */
	public void setBGColor(int color) {
		_bg = color;
	}
}
