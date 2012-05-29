package com.resonos.apps.fractal.ifs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.view.ToolBarGradientList;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.tabviewpager.TabViewPagerFragment;
import com.resonos.apps.library.util.AppUtils;

/** A fragment that allows the user to select from a list of color schemes */
public class FragmentGradientList extends TabViewPagerFragment implements
		OnItemClickListener, OnItemLongClickListener {

	// constants
	static final String NEW_GRADIENT = "__new__";
	private static final String GRADIENT_TITLE = "title";
	private static final String STATE_SCROLL_POS = "scrollPosition", STATE_EDIT_INDEX = "editIndex";
	private static final String STATE_RENDER_FRAGMENT = "renderFragment", STATE_SCROLLUSER = "scrollToUser";
	public static final int[] COLOR_CAT_RES_NAMES = new int[] {
		R.string.txt_color_def,
		R.string.txt_color_user};

	private static final int VIEW_USER = 1;//, VIEW_PRESETS = 0;
	private static final int POS_END = -2;
	private static final int POS_NONE = -1;

	// context
	public Home _home;

	// objects
	private ToolBarGradientList mToolBar;

	// data
	public ArrayList<ColorScheme> csPresets = new ArrayList<ColorScheme>();
	public ArrayList<ColorScheme> csCustom = new ArrayList<ColorScheme>();
	private Map<String, WeakReference<Bitmap>> mImages;

	// vars
	private int mScrollPos = 0;
	private int mPosEditing = -1;
	boolean usesSelectionToSaveListViewPosition = false;
	
	// state
	private FragmentRender fR;
	private int mScrollToOnUserList = POS_NONE;

	/**
	 * This constructor is available for the Fragment library's reinstantiation after onSaveInstanceState.
	 * It shouldn't be used directly.
	 */
	public FragmentGradientList() {
		super();
	}

	/**
	 * Create the color scheme chooser fragment
	 * @param fragmentRender : the parent render fragment
	 */
	public FragmentGradientList(FragmentRender fragmentRender) {
		super();
		fR = fragmentRender;
		_home = fR.getHome();
	}

	@Override
	protected int[] getData() {
		return COLOR_CAT_RES_NAMES;
	}

	@Override
	protected View getView(int position) {
		ListView mListView = new ListView(_home);
		mListView.setBackgroundResource(R.drawable.white);
		ArrayList<ColorScheme> cs = position == VIEW_USER ? csCustom : csPresets;
		ArrayList<HashMap<String, String>> mapIFS = new ArrayList<HashMap<String, String>>();
		ColorAdapter adapter = new ColorAdapter(_home, cs, mapIFS);
		mListView.setAdapter(adapter);
		mListView.setSelector(R.color.empty);
		mListView.setDividerHeight(0);
		mListView.setOnItemClickListener(this);
		mListView.setCacheColorHint(0);

		mImages = new HashMap<String, WeakReference<Bitmap>>();

		mListView.setOnItemLongClickListener(this);
		
		return mListView;
	}

	private void updateAdapterData(ColorAdapter adapter,
			ArrayList<ColorScheme> cs, int position) {
		adapter.getData().clear();
		
		for (int i = 0; i < cs.size(); i++)
			addColor(adapter, cs.get(i), position == VIEW_USER);
		adapter.notifyDataSetChanged();
	}

	/**
	 * Add a color scheme to the list.
	 * @param adapter 
	 * @param colorScheme : the {@link ColorScheme} object
	 */
	private void addColor(ColorAdapter adapter, ColorScheme colorScheme, boolean userGradients) {
		boolean isUser = colorScheme._name.startsWith(FragmentGradientEditor.USER_GRADIENT_PREFIX);
		if (isUser ^ userGradients)
			return;
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(GRADIENT_TITLE, colorScheme._name);
		adapter.getData().add(map);
	}
	
	@Override
	public void onShowView(int position, Object view) {
		super.onShowView(position, view);
		updateGradients();
		final ListView lv = ((ListView)view);
		updateAdapterData((ColorAdapter)lv.getAdapter(), _home.mGallery.mColorSchemes, position);
		
		if (position == VIEW_USER && mScrollToOnUserList != POS_NONE) {
			lv.post(new Runnable() {
				public void run() {
					try {
						lv.setSelection(mScrollToOnUserList == POS_END
								? (lv.getCount() - 1) : mScrollToOnUserList);
					} catch (Exception ex) {
						// no worries
					}
				}
			});
		}
	}
	
	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
		_home = (Home) getActivity();
		
		updateGradients();
		
		if (inState != null) {
			mScrollPos = inState.getInt(STATE_SCROLL_POS, 0);
			mPosEditing = inState.getInt(STATE_EDIT_INDEX, -1);
			mScrollToOnUserList = inState.getInt(STATE_SCROLLUSER, POS_NONE);
			_home.setGradientEditing("");
		}
	}
	
	private void updateGradients() {
		csPresets.clear();
		csCustom.clear();
		for (int i = 0; i < _home.mGallery.mColorSchemes.size(); i++) {
			ColorScheme cs = _home.mGallery.mColorSchemes.get(i);
			if (cs._name.startsWith(FragmentGradientEditor.USER_GRADIENT_PREFIX))
				csCustom.add(cs);
			else
				csPresets.add(cs);
		}
	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> av, View v,
			int position, long id) {
		if (!_home.hasPro()) {
			_home.mApp.tooltipToast(R.string.txt_upgrade_need_color_edit);
			return false;
		}

		ColorAdapter ca = (ColorAdapter) av.getAdapter();
		_home.setGradientEditing(ca.getData().get(position).get(GRADIENT_TITLE));
		mScrollToOnUserList = mPosEditing = position;
		toGradientEditor(ca.getData().get(position).get(GRADIENT_TITLE));
		if (!ca.getData().get(position).get(GRADIENT_TITLE)
				.startsWith(FragmentGradientEditor.USER_GRADIENT_PREFIX)) {
			usesSelectionToSaveListViewPosition = true;
			mScrollToOnUserList = POS_END;
			this.getPager(0).setCurrentItem(VIEW_USER);
		}
		return true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		mToolBar = new ToolBarGradientList(getActivity());
		((ViewGroup)getActivity().findViewById(R.id.ad_container)).addView(mToolBar,
				new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mToolBar.init(this);

		return root;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		AppUtils.putFragment(_home, outState, this, STATE_RENDER_FRAGMENT, fR);
		try {
			AdapterView<?> av = (AdapterView<?>) findView(getPager(0).getCurrentItem());
			outState.putInt(STATE_SCROLL_POS, findView(getPager(0).getCurrentItem()) == null ? mScrollPos :
				(usesSelectionToSaveListViewPosition ? av.getSelectedItemPosition() : av.getFirstVisiblePosition()));
		} catch (Exception ex) {}
		usesSelectionToSaveListViewPosition = false;
		outState.putInt(STATE_EDIT_INDEX, mPosEditing);
		outState.putInt(STATE_SCROLLUSER, mScrollToOnUserList);
	}

	@Override
	public void onActivityCreated(Bundle inState) {
		super.onActivityCreated(inState);
		if (inState != null) {
			fR = (FragmentRender)AppUtils.getFragment(_home, inState, this, STATE_RENDER_FRAGMENT);
			try {
				final AdapterView<?> av = (AdapterView<?>) findView(getPager(0).getCurrentItem());
				av.setSelection(mScrollPos);
				av.post(new Runnable() {
					public void run() {
						try {
							av.setSelection(mScrollPos);
						} catch (Exception ex) {
							// no worries
						}
					}
				});
			} catch (Exception ex) {}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		((ViewGroup)getActivity().findViewById(R.id.ad_container)).removeView(mToolBar);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
    	
//		for (Entry<Integer, WeakReference<Bitmap>> e : mImages.entrySet()) {
//			WeakReference<Bitmap> wrb = e.getValue();
//			if (wrb != null) {
//				Bitmap b = wrb.get();
//				if (b != null) {
//					b = M.freeBitmap(b);
//				}
//			}
//		}
	}

	/**
	 * Navigate to the gradient editor
	 * @param edit : name of the color scheme to edit, use null to create a new color scheme
	 * @return : the gradient editor fragment
	 */
	private FragmentGradientEditor toGradientEditor(String edit) {
		FragmentGradientEditor f = new FragmentGradientEditor();
		if (f.setGradient(_home, _home.mGallery, edit))
        	_home.setGradientEditing(FragmentGradientList.NEW_GRADIENT);
		_home.toChildFragment(f);
		return f;
	}

	@Override
	public void onItemClick(AdapterView<?> av, View view, int position,
			long id) {
		ColorAdapter ca = (ColorAdapter)av.getAdapter();
		_home.setColorScheme(ca.getData().get(position).get(GRADIENT_TITLE));
		if (fR != null)
			fR.onColorSchemeUpdated();
		_home.onBackPressed();
	}

	/**
	 * Add a new color scheme and begin editing it.
	 */
	public void newGradient() {
		if (!_home.hasPro()) {
			_home.mApp.tooltipToast(R.string.txt_upgrade_need_color_new);
			return;
		}
		_home.setGradientEditing(NEW_GRADIENT);
		mPosEditing = -1;
		toGradientEditor(_home.getGradientEditing());
	}

	@Override
	public boolean onBackPressed() {
		return false;
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
	public String getTitle() {
		return getString(R.string.title_color_schemes);
	}
	
	private static class ViewHolder {
		private View iconBorder;
		private ImageView icon;
		private TextView text;
		public ViewHolder(View v) {
			icon = (ImageView)v.findViewById(R.id.icon);
			iconBorder = v.findViewById(R.id.iconborder);
			text = (TextView)v.findViewById(R.id.newItemText);
		}
	}

	/**
	 * This class extends SimpleAdapter to provide an easy list of gradients.
	 */
	class ColorAdapter extends BaseAdapter {
		
		ArrayList<HashMap<String, String>> mData;
		ArrayList<ColorScheme> cs;
		
		/** default constructor 
		 * @param cs */
		public ColorAdapter(Context context,
				ArrayList<ColorScheme> cs, ArrayList<HashMap<String, String>> data) {
			mData = data;
			this.cs = cs;
		}

		public ArrayList<HashMap<String, String>> getData() {
			return mData;
		}
		
		@Override
	    public int getCount() {
	        return mData.size();
	    }

		@Override
	    public Object getItem(int position) {
	        return mData.get(position);
	    }

		@Override
	    public long getItemId(int position) {
	        return position;
	    }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			ViewHolder vh;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) _home
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_item_color, null);
				vh = new ViewHolder(v);
				v.setTag(vh);
			} else
				vh = (ViewHolder)v.getTag();
			
			vh.icon.setVisibility(View.VISIBLE);
			vh.iconBorder.setVisibility(View.VISIBLE);
			vh.text.setVisibility(View.GONE);
			if (mImages.containsKey(position)) {
				if (mImages.get(position).get() != null)
					vh.icon.setImageBitmap(mImages.get(position).get());
				else
					renderGradientToListView(vh.icon, position);
			} else
				renderGradientToListView(vh.icon, position);

			return v;
		}

		/**
		 * Render a color scheme preview to an image view
		 * @param child : the image view
		 * @param position : the color scheme index
		 */
		private void renderGradientToListView(ImageView child, int position) {
			if (position == -1)
				return;
			ColorScheme cm = _home.mGallery.getColorSchemeByName(mData.get(position)
					.get(GRADIENT_TITLE));
			if (cm != null) {
				Bitmap gradient = cm.generatePreview(App.inDP(320), cm.getGradientCount(), false, 0);
				if (child != null)
					child.setImageBitmap(gradient);
				mImages.put(mData.get(position)
						.get(GRADIENT_TITLE), new WeakReference<Bitmap>(gradient));
			}
			return;
		}
	}
	
	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return getDefaultAnimationSlideFromBottom(fa);
	}
	
	@Override
	protected int[] getIconData() {
		return null;
	}

	@Override
	protected boolean[] getVisibleData() {
		return null;
	}

	@Override
	protected int[] getColumnData() {
		return null;
	}
}
