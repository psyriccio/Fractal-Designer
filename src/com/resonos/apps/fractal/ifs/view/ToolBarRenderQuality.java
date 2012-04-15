package com.resonos.apps.fractal.ifs.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;

import com.resonos.apps.fractal.ifs.FragmentRender.SliderAction;
import com.resonos.apps.fractal.ifs.FragmentRender;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.util.M;
import com.resonos.apps.library.widget.SeekBar;
import com.resonos.apps.library.widget.SeekBar.OnSeekBarChangeListener;
import com.resonos.apps.library.widget.ToolBar;

/** A two row toolbar that has sliders used for controlling the quality of the render */
public class ToolBarRenderQuality extends ToolBar implements OnSeekBarChangeListener<Float> {

	// constants
	private static final float LOG_BASE = 7;
	private static final float LOG_BASE_DIV = (float)Math.log10(LOG_BASE);
	private static final float LOG_SPACER = 30;
	public static final int STEPS_MIN = 2, STEPS_MAX = 255;
	public static final int PTS_MIN = 10000, PTS_MAX = 1500000;

	// context
	private FragmentRender _f;
	
	//objects
	private SeekBar<Float> mSeekBar;
	private SeekBar<Float> mSeekBar2;
	
	// vars
	private SliderAction mAction, mAction2;
	
	public ToolBarRenderQuality(Context context) {
		super(context);
	}

	public ToolBarRenderQuality(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ToolBarRenderQuality(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/** Helper function, calculate a specific log using the constants declared above */
	private static float logBase(float f) {
		return (float)Math.log10(f+LOG_SPACER)/LOG_BASE_DIV;
	}
	
	/** initialize this toolbar */
	public void init(FragmentRender f) {
		_f = f;
    	setupSliderBar(SliderAction.POINTS, PTS_MIN, PTS_MAX, _f.getPoints());
    	setupSliderBar2(SliderAction.STEPS, STEPS_MIN, STEPS_MAX, _f.getSteps());

		super.init(_f._home, ToolBar.Param.STYLE_ACTION_MODE, ToolBar.Param.STYLE_SPLIT, ToolBar.Param.START_HIDDEN);
	}

	/** set up the second {@link SeekBar} */
	public void setupSliderBar(SliderAction action, int min, int max, int start) {
		start = M.fit(start, min, max);
		mAction = action;
    	mSeekBar = new SeekBar<Float>((float)min, (float)max, _f._home);
    	mSeekBar.setNotifyWhileDragging(false);
		mSeekBar.setProgress((float)start);
		mSeekBar.setOnSeekBarChangeListener(this);
	}
	
	/** set up the second {@link SeekBar} */
	public void setupSliderBar2(SliderAction action, int min, int max, int start) {
		start = M.fit(start, min, max);
		mAction2 = action;
    	mSeekBar2 = new SeekBar<Float>(logBase(min), logBase(max), _f._home);
    	mSeekBar2.setNotifyWhileDragging(false);
		mSeekBar2.setProgress(logBase(start));
		mSeekBar2.setOnSeekBarChangeListener(this);
	}
	
	/** actions possible in this toolbar */
	public enum Actions {POINTS, STEPS}

	@Override
	protected void onCreateMenu(ArrayList<Action> items) {
		items.add(new Action(getContext().getString(R.string.btn_detail), Action.ICON_NONE, true, false, Actions.POINTS)
			.customControl(mSeekBar, true));
		items.add(new Action(getContext().getString(R.string.btn_colorsteps), Action.ICON_NONE, true, false, Actions.STEPS)
			.customControl(mSeekBar2, true).newRow());
	}

	@Override
	protected void onItemSelected(Enum<?> action) {
		//
	}

	@Override
	public void onSeekBarValuesChanged(SeekBar<?> sb, Float value, boolean programmatic) {
		if (_f != null) {
			if (sb == mSeekBar) {
				_f.sliderBarChanged(mAction, Math.round(value));
			}
			else if (sb == mSeekBar2) {
				_f.sliderBarChanged(mAction2, Math.round(Math.pow(LOG_BASE, value) - LOG_SPACER));
			}
		}
	}
}
