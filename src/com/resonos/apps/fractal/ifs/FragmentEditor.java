package com.resonos.apps.fractal.ifs;

import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.WazaBe.HoloEverywhere.HoloAlertDialogBuilder;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.model.IFSFractal;
import com.resonos.apps.fractal.ifs.util.IFSFile;
import com.resonos.apps.fractal.ifs.view.EditorView;
import com.resonos.apps.fractal.ifs.view.ToolBarEditor;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.util.AppUtils;

public class FragmentEditor extends BaseFragment {
	
	// constants
	public static final String STATE_RENDER_FRAGMENT = "renderFragment";
	public static final String STATE_CACHED_IFS = "cachedIFS";
	public static final String STATE_LAST_LOADED = "lastLoaded";
	public static final String STATE_EDITOR = "editor";
	
	// context
	public Home _home;
	
	// objects
	private EditorView mEditor;
	private RelativeLayout mEditorContainer;
	private ToolBarEditor mToolBar;
	
	// saved state
	private FragmentRender fR;
    private String mCachedIFS = null;
    private String mLastLoaded = "";
	Bundle mStateEVBundle = null;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        _home = (Home)getActivity();
	}

	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
		if (inState != null) {
			fR = (FragmentRender)AppUtils.getFragment(_home, inState, this, STATE_RENDER_FRAGMENT);
			mCachedIFS = inState.getString(STATE_CACHED_IFS);
			mLastLoaded = inState.getString(STATE_LAST_LOADED);
			mStateEVBundle = inState.getBundle(STATE_EDITOR);
		}
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_editor, null);
        
        if (mEditor == null) {
    		mEditor = new EditorView(this, mStateEVBundle);
        }

        mEditorContainer = (RelativeLayout)root.findViewById(R.id.editorContainer);
        mEditorContainer.addView(mEditor);
		
		mToolBar = (ToolBarEditor)root.findViewById(R.id.toolbar);
		mToolBar.init(mEditor);
		
        return root;
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		AppUtils.putFragment(_home, outState, this, STATE_RENDER_FRAGMENT, fR);
		outState.putString(STATE_CACHED_IFS, mCachedIFS);
		outState.putString(STATE_LAST_LOADED, mLastLoaded);
		if (mEditor != null)
			outState.putBundle(STATE_EDITOR, mEditor.onSaveInstanceState());
		else if (mStateEVBundle != null)
			outState.putBundle(STATE_EDITOR, mStateEVBundle);
	}
    
    @Override
	public void onDestroyView() {
    	super.onDestroyView();
    	mEditorContainer.removeView(mEditor);
    }
    
    /**
     * Go to the render screen, creating the fragment if necessary.
     * If the editor is dirty, or the fragment has just been created,
     * then send the fractal off for rendering.
     */
    public void gotoRender() {
    	boolean needsFractal = mEditor.isEditorDirty();
		if (fR == null) {
			fR = new FragmentRender();
			needsFractal = true;
		}
		if (needsFractal)
			fR.setFractal(mEditor.getFractal());
		mEditor.setDirty(false);
		_home.toChildFragment(fR);
    }
    
    /** enum representating all of the actionbar/menu actions */
	public enum Actions { OPEN, SAVE, RESTORE, GRID };

	@Override
	protected void onCreateOptionsMenu(ArrayList<Action> items) {
		boolean isLandscape = App.SCREEN_WIDTH > App.SCREEN_HEIGHT;
		items.add(new Action(getString(R.string.btn_open), R.drawable.ic_action_open,
				isLandscape, false, Actions.OPEN));
		items.add(new Action(getString(R.string.btn_save), R.drawable.ic_action_save,
				isLandscape, false, Actions.SAVE));
		items.add(new Action(getString(R.string.btn_fix), R.drawable.ic_action_undo,
				false, true, Actions.RESTORE));
		boolean grid = mEditor != null ? mEditor.isUsingGrid() : false;
		items.add(new Action(getString(grid ? R.string.btn_grid_hide : R.string.btn_grid_show),
				R.drawable.ic_shape_grid, true, true, Actions.GRID));
	}

	@Override
	protected void onOptionsItemSelected(Enum<?> e) {
		switch ((Actions)e) {
		case OPEN:
			_home.toSiblingFragment(new FragmentGallery(true), false);
        	break;
		case SAVE:
			if (!_home.hasPro())
				_home.mApp.tooltipToast(R.string.txt_upgrade_need_save);
			else
				saveIFS();
        	break;
		case RESTORE:
			mEditor.btnRestore();
        	break;
		case GRID:
			mEditor.setUsingGrid(!mEditor.isUsingGrid());
			mEditor.invalidate();
			_home.invalidateOptionsMenu();
			break;
		}
	}

	@Override
	public String getTitle() {
		return ""; // so we have the whole actionbar available for buttons, no distracting text
	}

	@Override
	public boolean onBackPressed() {
		return false;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (mCachedIFS != null) {
			loadIFS(mCachedIFS);
			mCachedIFS = null;
		}
		mEditor.setVisibility(View.VISIBLE);
	}

	/** Sets the editor to clear the fractal upon resuming */
	public void clearFractalOnLoad() {
		this.queueTask(FragmentEvent.OnResume, clearFractal);
	}
	
	private Runnable clearFractal = new Runnable() {
		public void run() {
			mCachedIFS = null;
			mEditor.getFractal().clear();
			mEditor.invalidate();
		}
	};
	
	@Override
	public void onPause() {
		super.onPause();
		mEditor.onPause();
		mEditor.setVisibility(View.GONE);
	}
	
	/**
	 * Load an IFS fractal -- tell the EditorView to load it
	 * @param n : fractal filename
	 */
	public void loadIFS(String n) {
		if (isPaused()) {
			mCachedIFS = n;
			return;
		}
		mEditor.loadIFS(n);
		mLastLoaded = n;
	}
	
	/**
	 * Save a fractal by giving the user a filename dialog prompt.
	 */
	public void saveIFS() {
		final IFSFractal data = mEditor.saveTrans();
		final EditText eName = new EditText(_home);
		eName.setSingleLine();
		eName.setText(IFSFile.getNameFromFileName(mLastLoaded));
		eName.setHint(R.string.txt_save_fractal_hint);
    	new HoloAlertDialogBuilder(_home)
			.setTitle(R.string.txt_save_fractal)
			.setView(eName)
			.setPositiveButton(R.string.btn_save,new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String input = eName.getText().toString();
					if (input.length() < 1) {
						_home.mApp.tooltipToast(R.string.txt_save_fractal_short);
						return;
					}
					final String saveName = IFSFile.generateIFSFileName(IFSFile.IFS_CAT_INDEX_SAVED, input);
					if (_home.getUserGallery().getFractalByName(saveName) != null) {
				    	new HoloAlertDialogBuilder(_home)
							.setTitle(R.string.btn_confirm)
							.setMessage(R.string.txt_save_fractal_overwrite)
							.setPositiveButton(R.string.btn_yes,new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									data._name = saveName;
									_home.getUserGallery().mFractals.add(data);
							        _home.getUserGallery().save(_home);
							        _home.rebuildUnifiedGallery();
								}
							})
							.setNegativeButton(R.string.btn_no,null)
							.show();
					}
					else {
						data._name = saveName;
						_home.getUserGallery().mFractals.add(data);
				        _home.getUserGallery().save(_home);
				        _home.rebuildUnifiedGallery();
					}
				}
			})
			.setNegativeButton(R.string.btn_cancel,null)
			.show();
	}
	
	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return getDefaultAnimationSlideFromRight(fa);
	}
	
	/** Get the toolbar */
	public ToolBarEditor getToolbar() {
		return mToolBar;
	}
	
	/** Get the editor */
	public EditorView getEditor() {
		return mEditor;
	}
}