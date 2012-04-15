package com.resonos.apps.fractal.ifs.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;

import com.resonos.apps.fractal.ifs.FragmentGradientList;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.widget.ToolBar;

/** A simple toolbar for the gradient list fragment */
public class ToolBarGradientList extends ToolBar {

	FragmentGradientList _f;
	
	public ToolBarGradientList(Context context) {
		super(context);
	}

	public ToolBarGradientList(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ToolBarGradientList(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/** initializes the toolbar */
	public void init(FragmentGradientList f) {
		_f = f;
		super.init(_f._home, ToolBar.Param.STYLE_ACTION_MODE, ToolBar.Param.STYLE_SPLIT);
	}
	
	/** all possible actions for this toolbar */
	public enum Actions {NEW}

	@Override
	protected void onCreateMenu(ArrayList<Action> items) {
		items.add(new Action(getContext().getString(R.string.btn_new_gradient), 0, true, false, Actions.NEW));
	}

	@Override
	protected void onItemSelected(Enum<?> action) {
		Actions a = (Actions)action;
		switch (a) {
		case NEW:
			_f.newGradient();
			break;
		}
	}
}
