package com.resonos.apps.fractal.ifs;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.Help.ActionModeCallBack.LoopingRunnable;
import com.resonos.apps.fractal.ifs.model.IFSCoord;
import com.resonos.apps.fractal.ifs.model.IFSFractal;
import com.resonos.apps.fractal.ifs.model.Transformation;
import com.resonos.apps.fractal.ifs.view.EditorView;
import com.resonos.apps.fractal.ifs.view.ToolBarEditor;
import com.resonos.apps.library.util.AppUtils;
import com.resonos.apps.library.util.M;
import com.resonos.apps.library.widget.QuickAction3D;
import com.resonos.apps.library.widget.QuickAction3D.OnDismissListener;

public class Help implements OnDismissListener {
	
	// constants
	private static final int[] TUTORIAL_TEXTS = {
			R.string.help01, R.string.help02, R.string.help03, R.string.help04, R.string.help05,
			R.string.help06, R.string.help07, R.string.help08, R.string.help09, R.string.help10,
			R.string.help11, R.string.help12, R.string.help13, R.string.help14, R.string.help15,
			R.string.help16, R.string.help17, R.string.help18, R.string.help19, R.string.help20,
			R.string.help21, R.string.help22, R.string.help23, R.string.help24
	};
	private static final int[] TUTORIAL_TEXTS_ALT = {
			0, R.string.help02a, 0, 0, 0,
			0, 0, 0, 0, 0,
			0, 0, 0, 0, 0,
			0, 0, 0, R.string.help19a, 0,
			0, 0, 0, R.string.help24a
	};
	
	public static final String STATE_POS = "pos", STATE_BREAK = "onBreak";

	// context
	private Home _home;

	// vars
	private int mPos = 0;
	private boolean mOnBreak = false;
	private ActionModeCallBack mCurAM = null;
	
	/** Create a tutorial object. */
	public Help(Home home, Bundle inState) {
		_home = home;
		if (inState != null) {
			mPos = inState.getInt(STATE_POS);
			mOnBreak = inState.getBoolean(STATE_BREAK);
		}
	}
	
	/** Save the state of this tutorial for screen rotation. */
	public Bundle onSaveInstanceState() {
		Bundle outState = new Bundle();
		outState.putInt(STATE_POS, mPos);
		outState.putBoolean(STATE_BREAK, mOnBreak);
		if (mCurAM != null)
			mCurAM.stopTask();
		return outState;
	}

	/** resumes the tutorial after a configuration change */
	public void resume() {
		if (mOnBreak) {
			mOnBreak = false;
		} else {
			mPos--;
			mOnBreak = true;
		}
		_home.setDuringHelp(true);
		next();
	}

	/** Begins the tutorial. */
	public void start() {
		mPos = 0;
		_home.setDuringHelp(true);
		next();
	}

	/** moves to the next page of the tutorial */
	private void next() {
		if (mPos >= TUTORIAL_TEXTS.length) {
			finish();
			return;
		}
		if (createTooltip(helpLineAction(mPos, mOnBreak))) {
			mPos++;
		} else {
			return;
		}
	}

	/** end the tutorial */
	private void finish() {
		// if the tutorial altered the editor's fractal, just reload the default
		if (mPos >= 3 && _home.fE != null) {
			EditorView ev = _home.fE.getEditor();
			if (ev != null)
				ev.loadIFS(EditorView.DEFAULT_IFS);
		}
		
		mPos = TUTORIAL_TEXTS.length + 1;
		_home.setDuringHelp(false);
		_home.returnToDashboard();
	}

	/**
	 * Creates a floating window based on the input structure
	 * @param tti : a {@link TooltipInfo} describing the window to create
	 * @return true if the window was created, false if the input was null
	 */
	public boolean createTooltip(TooltipInfo tti) {
		if (tti == null) {// delay next part of tutorial
			mOnBreak = true;
			return false;
		}
		mOnBreak = false;
	    QuickAction3D qa = new QuickAction3D(_home);
	    int txt = (_home.hasPro() && TUTORIAL_TEXTS_ALT[mPos] != 0)
	    				? TUTORIAL_TEXTS_ALT[mPos] : TUTORIAL_TEXTS[mPos];
	    if (tti._img != null)
	    	qa.addInfo(new BitmapDrawable(_home.getResources(), tti._img), txt, this);
	    else
	    	qa.addInfo(tti._imgID , txt, this);
	    if (tti._v != null)
	    	qa.show(tti._v);
	    else if (tti._centered)
	    	qa.showCentered(_home.mView, tti._high);
	    else
	    	qa.showAt(_home.mView, tti._x, tti._y);
	    return true;
	}

	@Override
	public void onDismiss(boolean backHit) {
		if (!backHit)
			next();
		else
			finish();
	}

	/**
	 * This is the real meat of the tutorial. This function steps us through the application,
	 * and creates all of the tooltip information structures along the way.
	 * @param i : the step of the tutorial we are one.
	 * @param fromBreak : true if we are coming back from an ActionMode, or a Configuration change
	 * @return the {@link ToolTipInfo} structure describing the tooltip to create
	 */
	public TooltipInfo helpLineAction(int i, boolean fromBreak) {
		IFSFractal trans;
		switch (i) {
		case 0:
			return new TooltipInfo().setCentered();
		case 1:
			return new TooltipInfo().setCentered().setAtTop().setImage(R.drawable.home_btn_upgrade);
		case 2:
			return new TooltipInfo().setCentered().setAtTop().setImage(R.drawable.home_btn_loadpic);
		case 3:
			if (!fromBreak) {
				_home.toEditorFromRoot();
				_home.fE.clearFractalOnLoad();
			}
			return new TooltipInfo().setCentered().setAtTop();
		case 4:
			if (!fromBreak) {
				_home.fE.getToolbar().hide();
				_home.startActionMode(mCurAM = new ActionModeCallBack(this)).setTitle(R.string.help_action_mode);
				return null;
			}
			else {
				_home.fE.getToolbar().show();
				_home.fE.getEditor().resetNavigation();
				return new TooltipInfo(_home.fE.getToolbar().getChildFromTag(ToolBarEditor.Actions.ADD_TRANS));
			}
		case 5:
			if (!fromBreak) {
				_home.fE.getToolbar().onItemSelected(ToolBarEditor.Actions.ADD_TRANS);
			}
			return new TooltipInfo().setCentered().setAtTop();
		case 6:
			if (!fromBreak) {
				_home.fE.getToolbar().hide();
				_home.startActionMode(mCurAM = new ActionModeCallBack(this)).setTitle(R.string.help_action_mode);
				return null;
			}
			else {
				_home.fE.getToolbar().show();
				_home.fE.getEditor().resetNavigation();
				return new TooltipInfo().setCentered();
			}
		case 7:
			if (!fromBreak) {
				trans = _home.fE.getEditor().getFractal();
				trans.clear();
				trans.addTrans(new Transformation(new IFSCoord(0.2139f, -0.1156f),
						new IFSCoord(0.96f, 0.96f), 0.350f, new IFSCoord(0, 0)));
				_home.fE.getEditor().invalidate();
			}
			return new TooltipInfo().setCentered().setAtTop();
		case 8:
			if (!fromBreak) {
				trans = _home.fE.getEditor().getFractal();
				trans.clear();
				trans.addTrans(new Transformation(new IFSCoord(0.2139f, -0.1156f),
						new IFSCoord(0.96f, 0.96f), 0.350f, new IFSCoord(0, 0)));
				trans.addTrans(new Transformation());
				_home.fE.getEditor().setSelectedTrans(1);
				_home.fE.getEditor().invalidate();
			}
			return new TooltipInfo().setCentered().setAtTop();
		case 9:
			if (!fromBreak) {
				trans = _home.fE.getEditor().getFractal();
				trans.clear();
				trans.addTrans(new Transformation(new IFSCoord(0.2139f, -0.1156f),
						new IFSCoord(0.96f, 0.96f), 0.350f, new IFSCoord(0, 0)));
				trans.addTrans(new Transformation(new IFSCoord(0.0800f, 0.0800f),
						new IFSCoord(0.21f, 0.21f), 0, new IFSCoord(0, 0)));
				_home.fE.getEditor().invalidate();
			}
			return new TooltipInfo().setCentered().setAtTop();
		case 10:
			if (!fromBreak) {
				_home.fE.getToolbar().hide();
				_home.startActionMode(mCurAM = new ActionModeCallBack(this,
						new RotatingIFSRunnable(_home))).setTitle(R.string.help_action_mode);
				_home.fE.getEditor().setAcceptingInput(false);
				return null;
			}
			else {
				_home.fE.getEditor().setAcceptingInput(true);
				_home.fE.getToolbar().show();
				_home.fE.getEditor().resetNavigation();
				return new TooltipInfo().setCentered().setAtTop();
			}
		case 11:
			return new TooltipInfo().setCentered().setAtTop();
		case 12:
			return new TooltipInfo(_home.getActionBarButtonX(0), _home.getActionBarButtonY());
		case 13:
			return new TooltipInfo().setCentered().setAtTop();
		case 14:
			return new TooltipInfo(_home.fE.getToolbar().getChildFromTag(ToolBarEditor.Actions.RENDER));
		case 15:
			if (!fromBreak) {
				AppUtils.setSavedInt(_home.mApp, FragmentRender.PREF_POINTS, FragmentRender.DEFAULT_POINTS);
				AppUtils.setSavedInt(_home.mApp, FragmentRender.PREF_STEPS, FragmentRender.DEFAULT_STEPS);
				_home.mSelColors = Home.DEF_COLORSCHEME;
				_home.fE.getToolbar().onItemSelected(ToolBarEditor.Actions.RENDER);
			}
			return new TooltipInfo().setCentered().setAtTop();
		case 16:
			_home.fR.getToolbarQuality().show();
			return new TooltipInfo().setCentered().setAtTop();
		case 17:
			return new TooltipInfo().setCentered().setAtTop();
		case 18:
			_home.fR.getToolbarQuality().hide();
			return new TooltipInfo().setCentered().setAtTop().setImage(
					_home.fR.getToolbar().generateColorSchemeIcon());
		case 19:
			return new TooltipInfo().setCentered().setAtTop();
		case 20:
			return new TooltipInfo().setCentered();
		case 21:
			if (!fromBreak) {
				_home.returnToDashboard();
			}
			return new TooltipInfo().setCentered().setAtTop().setImage(R.drawable.home_btn_gallery);
		case 22:
			if (!fromBreak) {
				_home.toGallery();
			}
			return new TooltipInfo().setCentered().setAtTop();
		case 23:
			if (!fromBreak) {
				_home.returnToDashboard();
			}
			return new TooltipInfo().setCentered();
		}
		return null;
	}
	
	/**
	 * {@link LoopingRunnable} that shows the user a slowly rotating fractal.
	 */
	public static class RotatingIFSRunnable extends LoopingRunnable {
		private static final float ROTATION_SPEED = 1/16f;
		private static final IFSCoord CENTER_POINT = new IFSCoord(0.5f, 0.5f);
		private IFSFractal ifs;
		private EditorView editor;
		
		public RotatingIFSRunnable(Home home) {
			ifs = home.fE.getEditor().getFractal();
			editor = home.fE.getEditor();
		}

		@Override
		public void step(float elapsed) {
			Transformation tr = ifs.getTrans(0);
			float newAngle = (tr.angle + (2*M.PI) * ROTATION_SPEED * elapsed) % (2*M.PI);
			tr.pos.set(CENTER_POINT).sub((tr.size.x + tr.skew.x*tr.size.x)/2f,
					(tr.size.y + tr.skew.y*tr.size.y)/2f).rotateAround(CENTER_POINT, newAngle);
			tr.angle = newAngle % (2*M.PI);
			editor.invalidate();
		}
	};

	/**
	 * This class represents an action for the user to do during the tutorial.
	 * It is achieved through the use of Android {@link ActionMode}s.
	 * You can also supply a {@link LoopingRunnable} for code to run continuously
	 * during the ActionMode.
	 * @author Chris
	 *
	 */
    public static final class ActionModeCallBack implements ActionMode.Callback {
    	/**
    	 * A class representing a continuously looping runnable.
    	 */
    	public static abstract class LoopingRunnable {
    		/**
    		 * The code to be repeated goes here.
    		 * @param elapsed : the time, in seconds, since the last loop started
    		 */
    		public abstract void step(float elapsed);
    	};
    	
    	// constants
    	public static final float MAX_LOOP_LENGTH = 0.5f;
    	private static final long STANDARD_DELAY = 33;
    	
    	// objects
    	private LoopingRunnable task;
    	private Handler handler;
    	
    	// context
    	private Help mHelp;
    	
    	// vars
    	private boolean looping = false;
    	private long lastRun = 0;
    	
    	/**
    	 * Create a default ActionMode
    	 * @param help : the tutorial context
    	 */
    	public ActionModeCallBack(Help help) {
    		this(help, null);
    	}
    	
    	/**
    	 * Create an ActionMode with code looping in the background.
    	 * @param help : the tutorial context
    	 * @param lr : a {@Link LoopingRunnable} object
    	 */
    	public ActionModeCallBack(Help help, LoopingRunnable lr) {
    		mHelp = help;
    		if (lr != null) {
	    		task = lr;
	    		lastRun = System.currentTimeMillis();
	    		handler = new Handler();
	    		looping = true;
	    		handler.postDelayed(taskLooper, STANDARD_DELAY);
    		}
    	}
    	
    	/**
    	 * This Runnable does the looping work for the LoopingRunnable.
    	 */
    	Runnable taskLooper = new Runnable() {
			public void run() {
				long cur = System.currentTimeMillis();
				float elapsed = M.fit((cur - lastRun)/1000f, 0, MAX_LOOP_LENGTH);
				task.step(elapsed);
				lastRun = cur;
				if (looping)
					handler.postDelayed(this, STANDARD_DELAY);
			}
    	};

        /**
         * Stop the looping task, if any.
         */
		private void stopTask() {
        	looping = false;
        	if (handler != null)
        		handler.removeCallbacks(taskLooper);
        	if (mHelp.mCurAM == this)
        		mHelp.mCurAM = null;
		}
    	
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        	stopTask();
        	mHelp.next();
        }
    }
	
	/**
	 * This class describes a single tooltip or "page" in the tutorial.
	 */
	class TooltipInfo {
		public int _imgID = 0;
		private View _v;
		private int _x = -1, _y = -1;
		private boolean _centered = false;
		private boolean _high = false;
		private Bitmap _img = null;
		
		TooltipInfo(View v) {
			_v = v;
			if (v == null) // fail-safe in case the view does not exist yet
				setCentered();
		}
		
		TooltipInfo(int x, int y) {
			_x = x;
			_y = y;
		}
		
		TooltipInfo() {
			//
		}
		
		/** make the tooltip centered */
		public TooltipInfo setCentered() {
			_centered = true;
			return this;
		}

		/** make the tooltip vertically aligned at the top */
		public TooltipInfo setAtTop() {
			_high = true;
			return this;
		}

		/** set a resource drawable for the tooltip to display */
		public TooltipInfo setImage(int img) {
			_imgID = img;
			return this;
		}

		/** set a bitmap for the tooltip to display */
		public TooltipInfo setImage(Bitmap img) {
			_img = img;
			return this;
		}
	}
}
