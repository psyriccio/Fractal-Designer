package com.resonos.apps.fractal.ifs.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;

import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.widget.ToolBar;

/**
 * Toolbar used with the {@link EditorView}.
 * @author Chris
 */
public class ToolBarEditor extends ToolBar {

	private EditorView mEditor;
	
	public ToolBarEditor(Context context) {
		super(context);
	}

	public ToolBarEditor(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ToolBarEditor(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	/**
	 * Initializes the toolbar.
	 * @param editor : the EditorView associated with this toolbar
	 */
	public void init(EditorView editor) {
		mEditor = editor;
		super.init(editor._f._home, ToolBar.Param.STYLE_ACTION_MODE, ToolBar.Param.STYLE_SPLIT);
	}
	
	/** an enum representing the actions possible in this toolbar */
	public enum Actions {ADD_TRANS, REM_TRANS, RENDER}

	@Override
	protected void onCreateMenu(ArrayList<Action> items) {
		items.add(new Action(getContext().getString(R.string.btn_add),
				R.drawable.ic_av_stop, true, false, Actions.ADD_TRANS));
		if (mEditor.hasSelection())
			items.add(new Action(getContext().getString(R.string.btn_remove),
					R.drawable.ic_av_stop, true, false, Actions.REM_TRANS));
		items.add(new Action(getContext().getString(R.string.btn_render), 0, true, false, Actions.RENDER));
	}

	@Override
	public void onItemSelected(Enum<?> action) {
		Actions a = (Actions)action;
		switch (a) {
		case ADD_TRANS:
	        // some default arbitrary values
			mEditor.addTrans();
			invalidateToolbar();
			break;
		case REM_TRANS:
			mEditor.removeTrans();
			invalidateToolbar();
			break;
		case RENDER:
			mEditor._f.gotoRender();
			break;
		}
	}
}
