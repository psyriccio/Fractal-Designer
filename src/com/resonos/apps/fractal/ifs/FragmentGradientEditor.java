package com.resonos.apps.fractal.ifs;

import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerDialog;
import net.margaritov.preference.colorpicker.ColorPickerDialog.OnColorChangedListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.model.ColorScheme.Gradient;
import com.resonos.apps.fractal.ifs.model.ColorScheme.KeyColor;
import com.resonos.apps.fractal.ifs.model.Gallery;
import com.resonos.apps.fractal.ifs.view.GradientView;
import com.resonos.apps.fractal.ifs.view.GradientView.GradientEditListener;
import com.resonos.apps.fractal.ifs.view.GradientView.OnRemoveListener;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.widget.FormBuilder;
import com.resonos.apps.library.widget.FormElement;
import com.resonos.apps.library.widget.ListFormBuilder;
import com.resonos.apps.library.widget.ListFormBuilder.OnFormItemClickListener;

public class FragmentGradientEditor extends BaseFragment implements
				OnFormItemClickListener, OnRemoveListener {
	
	// constants
	public static final String USER_GRADIENT_PREFIX = "__user";
	private static final String STATE_COLORSCHEME_NAME = "colorSchemeName",
		STATE_SEL = "sel", STATE_SUB_SEL = "subSel";
	
    // state
	private String mColorMapName;
    private ColorScheme mColorMap;
    
    // vars
    private ArrayList<Integer> mGradientIndex = new ArrayList<Integer>();

	// views
    private View mRoot;
    private RelativeLayout mContainer;
    private FormBuilder mainPage;
    private ScrollView mRootView;

    // objects
    private Animation mFadeInAnimation;
    private Animation mFadeOutAnimation;
    
    // action mode
    private ActionMode mActionMode;
    private GradientActionModeCallBack mActionModeCallback;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_COLORSCHEME_NAME, mColorMapName);
		outState.putInt(STATE_SEL, (mActionModeCallback == null) ? -1 : mActionModeCallback.getIndex());
		outState.putInt(STATE_SUB_SEL, (mActionModeCallback == null) ? -1 : mActionModeCallback.getSel());
	}

	@Override
	public void onActivityCreated(Bundle inState) {
		super.onActivityCreated(inState);
	}

	/** get the host activity as its derived class */
	public Home getHome() {
		return (Home)getActivity();
	}
	
	@Override
	public View onCreateView(LayoutInflater l, ViewGroup container, Bundle inState) {
		mRoot = l.inflate(R.layout.fragment_gradient_editor, null);

        mFadeInAnimation = AnimationUtils.loadAnimation(getHome(), android.R.anim.fade_in);
        mFadeOutAnimation = AnimationUtils.loadAnimation(getHome(), android.R.anim.fade_out);
        
        int sel = -1, subSel = -1;
		if (inState != null) {
			mColorMapName = inState.getString(STATE_COLORSCHEME_NAME);
			sel = inState.getInt(STATE_SEL, -1);
			subSel = inState.getInt(STATE_SUB_SEL, -1);
			mColorMap = getHome().mGallery.getColorSchemeByName(mColorMapName);
		}

        mContainer = (RelativeLayout)mRoot.findViewById(R.id.gradientContainer);
        
		int maxWidth = App.inDP(560);
		LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(
				App.SCREEN_WIDTH > maxWidth ? maxWidth : LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);

        mRootView = new ScrollView(getHome());
        mRootView.setFillViewport(true);
		LinearLayout ll = new LinearLayout(getHome());
		ll.setGravity(Gravity.CENTER);
		ll.setOrientation(LinearLayout.VERTICAL);
		mRootView.addView(ll);
        mainPage = new ListFormBuilder(getActivity(), null, this);
		ll.addView(setupMainBlock(), lllp);

        mContainer.addView(mRootView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // resume action mode
		if (sel >= 0) {
			editGradient(sel);
			if (subSel >= 0)
				mActionModeCallback.setSel(subSel);
		}
        
        return mRoot;
    }

	@Override
	public String getTitle() {
		return getString(R.string.title_color_editor);
	}
	
	/** the possible actions through the action bar in this fragment */
	public enum Actions {ADD_GRADIENT, EDIT_COLOR, EDIT_BGCOLOR, DELETE_COLOR, SELECT_GRADIENT};
	
	/**
	 * Set up the UI.
	 */
	private View setupMainBlock() {
		int actionsize = App.inDP(32);
        FormBuilder b = mainPage;
		
		b.clear();
		b.newSection("Background");
		b.newItem().title("Background Color").onClick(Actions.EDIT_BGCOLOR)
			.drawable(new BitmapDrawable(getHome().getResources(),
					generateColorBox(mColorMap.getBGColor(), actionsize)))
			.textColor(Color.BLACK);
		
		b.newSection("Foreground Color Gradients");
		mGradientIndex.clear();
		for (int i = 0; i < mColorMap.getGradientCount(); i++) {
			GradientView gv = getGradientView(i);
			FormElement fe = b.newItem().view(gv.getMasterView())
					.onClick(Actions.SELECT_GRADIENT).textColor(Color.BLACK);
			mGradientIndex.add(fe.getData().mIndex);
		}
		b.newItem().icon(R.drawable.ic_action_new).text("Add", "Create another foreground gradient")
			.onClick(Actions.ADD_GRADIENT).textColor(Color.BLACK).drawColor(Color.BLACK);
		
		return mainPage.finish();
	}

	/**
	 * Create a gradient view for use in the list of gradients
	 * @param i : index
	 * @return the new {@link GradientView}
	 */
	private GradientView getGradientView(int i) {
		GradientView ge = new GradientView(this, this, mColorMap.getGradientCount() > 1);
		ge.setColors(i, mColorMap);
		return ge;
	}

	@Override
	public void onRemove(GradientView gv) {
		mColorMap.getFGColors().remove(gv.getPosition());
		invalidateEditor();
	}

	/**
	 * Generate a bitmap of a solid color with a black single pixel outline
	 * @param bgColor : the solid color
	 * @param size : the width and height of the output image
	 * @return the output Bitmap
	 */
	private Bitmap generateColorBox(int bgColor, int size) {
		Bitmap bmp = Bitmap.createBitmap(size, size, Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);
		Paint p = new Paint();
		p.setColor(Color.BLACK);
		Rect r = new Rect(0, 0, size, size);
		canvas.drawRect(r, p);
		p.setColor(bgColor);
		r.inset(1, 1);
		canvas.drawRect(r, p);
		return bmp;
	}

	@Override
	public void onClick(Enum<?> tag, int pos, FormElement fe) {
		Actions a = (Actions)tag;
		switch (a) {
		case ADD_GRADIENT:
			ArrayList<KeyColor> kcs = new ArrayList<KeyColor>();
			kcs.add(new KeyColor(0xFF000000, 0, kcs));
			kcs.add(new KeyColor(0xFFFFFFFF, ColorScheme.POSITION_MAX, kcs));
			mColorMap.getFGColors().add(new Gradient(kcs));
			invalidateEditor();
			break;
		case EDIT_BGCOLOR:
			ColorPickerDialog picker = new ColorPickerDialog(getHome(), mColorMap.getBGColor());
			picker.setOnColorChangedListener(new OnColorChangedListener() {
				public void onColorChanged(int color) {
		        	mColorMap.setBGColor(color);
		        	invalidateEditor();
		        	if (mActionMode != null)
		        		mActionMode.invalidate();
				}
			});
			picker.show();
			break;
		case SELECT_GRADIENT:
			int sel = mGradientIndex.indexOf(pos);
			if (sel >= 0)
				editGradient(sel);
			break;
		}
	}
	
	/**
	 * Initiate an ActionMode used to edit a gradient
	 * @param gradient : the gradient index
	 */
	private void editGradient(int gradient) {
		mActionModeCallback = new GradientActionModeCallBack(mColorMap, gradient);
		mActionMode = getHome().startActionMode(mActionModeCallback);
		mActionModeCallback.setActionMode(mActionMode);
    	invalidateEditor();
	}

	/**
	 * Trigger a redraw the gradient.
	 */
	private void invalidateEditor() {
		setupMainBlock();
		getHome().invalidateOptionsMenu();
	}

	/**
	 * Load a gradient from the Gallery
	 * @param gal : the {@link Gallery} object
	 * @param name : the name of the ColorMap
	 * @return true if we are changing this to be edited as a new gradient (built in gradient behavior)
	 */
	public boolean setGradient(Home home, Gallery gal, String name) {
        mColorMapName = name;
        if (mColorMapName.equals(FragmentGradientList.NEW_GRADIENT))
        	mColorMap = new ColorScheme();
        else
        	mColorMap = gal.getColorSchemeByName(mColorMapName);
        if (!mColorMapName.startsWith(USER_GRADIENT_PREFIX)) { // editing a sample CS = making a new CS
        	mColorMapName = FragmentGradientList.NEW_GRADIENT;
        	mColorMap = new ColorScheme(mColorMap);
        	save(home);
        	return true;
        }
    	save(home);
        return false;
	}
	
	@Override
	public void onPause() {
		if (getHome() != null)
			save(getHome());
		super.onPause();
	}

	/**
	 * Save any changes to this gradient.
	 */
	public void save(Home home) {
        if (mColorMapName.equals(FragmentGradientList.NEW_GRADIENT)) {
			int i = 0;
			while (home.mGallery.getColorSchemeByName(USER_GRADIENT_PREFIX+i) != null) {
				i++;
			}
			mColorMapName = USER_GRADIENT_PREFIX+i;
        }
        mColorMap._name= mColorMapName;
        if (!home.getUserGallery().hasColorScheme(mColorMap)) {
			home.getUserGallery().mColorSchemes.add(mColorMap);
        }
        home.getUserGallery().save(home);
        home.rebuildUnifiedGallery();
        home.updateGradientList(true);
	}

	@Override
	public boolean onBackPressed() {
		save(getHome());
		return false;
	}
	
	/**
	 * This is an ActionMode callback that manages editing a gradient.
	 */
    public final class GradientActionModeCallBack implements ActionMode.Callback, GradientEditListener {
    	
    	// objects and vars
    	private GradientView gv;
    	private ActionMode _am;
    	private KeyColor mKeyColor;
    	private int _pos;
    	
    	/**
    	 * Init the ActionMode callback
    	 * @param colorMap : the {@link ColorScheme} the gradient is from
    	 * @param pos : the index of the gradient
    	 */
    	public GradientActionModeCallBack(ColorScheme colorMap, int pos) {
    		_pos = pos;
    		if (gv == null) {
    			gv = new GradientView(FragmentGradientEditor.this, this);
    			mContainer.addView(gv.getMasterView());
    		}
    		gv.setColors(pos, colorMap);
			gv.setVisibility(View.GONE);
    	}
    	
    	/**
    	 * Set the selected {@link KeyColor} id
    	 * @param id : the id of the KeyColor to select
    	 */
    	public void setSel(int id) {
    		mKeyColor = gv.getColors().getKeyById(id);
    		gv.setSelKey(mKeyColor);
			_am.invalidate();
    	}
    	
    	/**
    	 * Get the selected Key
    	 * @return the selected Key as a {@link KeyColor}
    	 */
        public int getSel() {
			return (mKeyColor == null) ? -1 : mKeyColor.getID();
		}

        /**
         * Get the index of this gradient.
         * @return the gradient index
         */
		public int getIndex() {
			return _pos;
		}

		@Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			gv.startAnimation(mFadeInAnimation);
			gv.setVisibility(View.VISIBLE);
			mRootView.startAnimation(mFadeOutAnimation);
			mRootView.setVisibility(View.INVISIBLE);

			createMenu(menu);
            return true;
        }
        
		/**
		 * Update the ActionMode menu
		 * @param menu : the supplied menu object
		 */
        private void createMenu(Menu menu) {
        	menu.clear();
    		if (mKeyColor != null) {
	            menu.add(Menu.NONE, Actions.EDIT_COLOR.ordinal(), Menu.NONE, R.string.btn_change)
	                .setIcon(new BitmapDrawable(getHome().getResources(),
	                		generateColorBox(mKeyColor.getColor(), App.inDP(32))))
	                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    			if (!mKeyColor.isFixed())
    	            menu.add(Menu.NONE, Actions.DELETE_COLOR.ordinal(), Menu.NONE, R.string.btn_remove)
		                .setIcon(R.drawable.ic_action_trash)
		                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    		}
		}

        /**
         * Set the ActionMode this callback is for.
         * Required after initiating the ActionMode for proper behavior.
         * @param am
         */
		public void setActionMode(ActionMode am) {
        	_am = am;
        }

		@Override
		public void onSelectedColorChanged(GradientView gv, KeyColor sel) {
			mKeyColor = sel;
			_am.invalidate();
		}

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        	createMenu(menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        	if (item.getItemId() == Actions.EDIT_COLOR.ordinal()) {
    			if (gv != null && gv.getSelKey() != null) {
    				ColorPickerDialog picker = new ColorPickerDialog(getHome(),
    						mKeyColor.getColor());
    				picker.setOnColorChangedListener(new OnColorChangedListener() {
    					public void onColorChanged(int color) {
    						mKeyColor.setColor(color);
    						gv.invalidate();
    						_am.invalidate();
    					}
    				});
    				picker.show();
    			}
        	} else if (item.getItemId() == Actions.DELETE_COLOR.ordinal()) {
    			if (gv != null && gv.getSelKey() != null) {
    				mColorMap.getGradient(gv.getPosition()).delKeyById(gv.getSelKey().getID());
    		    	gv.setSelKey(null);
    		    	mKeyColor = null;
					gv.invalidate();
					_am.invalidate();
    			}
        	}
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        	mKeyColor = null;
			gv.startAnimation(mFadeOutAnimation);
			gv.setVisibility(View.INVISIBLE);
			mRootView.startAnimation(mFadeInAnimation);
			mRootView.setVisibility(View.VISIBLE);
			
			// make this mode unusable anymore
			_pos = -1;
			mActionModeCallback = null;
			mActionMode = null;
        }
    }

	@Override
	protected void onCreateOptionsMenu(ArrayList<Action> items) {
		//
	}

	@Override
	protected void onOptionsItemSelected(Enum<?> action) {
		//
	}
	
	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return getDefaultAnimationSlideFromRight(fa);
	}
}
