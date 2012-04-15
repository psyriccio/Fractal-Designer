package com.resonos.apps.fractal.ifs.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;

import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.FragmentRender;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.widget.ToolBar;

/** The auxillary toolbar for the {@link FractalView} */
public class ToolBarRender extends ToolBar {

	private FragmentRender _f;
	
	public ToolBarRender(Context context) {
		super(context);
	}

	public ToolBarRender(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ToolBarRender(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/** initialize the toolbar */
	public void init(FragmentRender f) {
		_f = f;
		super.init(_f._home, ToolBar.Param.STYLE_ACTION_MODE, ToolBar.Param.STYLE_SPLIT);
	}
	
	/** the toolbar's actions */
	public enum Actions {COLORS, QUALITY}

	@Override
	protected void onCreateMenu(ArrayList<Action> items) {
		if (_f.getToolbarQuality() == null || !_f.getToolbarQuality().isShowing())
			items.add(new Action(getContext().getString(R.string.btn_quality), Action.ICON_NONE, true, false, Actions.QUALITY));
		items.add(new Action(getContext().getString(R.string.btn_colorscheme), generateColorSchemeIcon(), true, false, Actions.COLORS));
	}

	/**
	 * Generate a preview of a color scheme suitable for a button.
	 * @return the image as a bitmap
	 */
	public Bitmap generateColorSchemeIcon() {
		FractalView fv = _f.getFractalView();
		ColorScheme cm = fv.mCM;
		return cm.generatePreview(48*3, 48, true, Color.BLACK);
	}

	@Override
	public void onItemSelected(Enum<?> action) {
		Actions a = (Actions)action;
		switch (a) {
		case COLORS:
			_f.toGradientList();
			break;
		case QUALITY:
			_f.toggleQualityToolBar();
			invalidateToolbar();
			break;
		}
	}
}
