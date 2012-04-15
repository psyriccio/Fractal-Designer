package com.resonos.apps.fractal.ifs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.view.ToolBarGradientList;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.util.AppUtils;
import com.resonos.apps.library.util.M;

/** A fragment that allows the user to select from a list of color schemes */
public class FragmentGradientList extends BaseFragment implements
		OnItemClickListener {

	// constants
	static final String NEW_GRADIENT = "__new__";
	private static final String GRADIENT_TITLE = "title";
	private static final String STATE_SCROLL_POS = "scrollPosition", STATE_EDIT = "editing", STATE_EDIT_INDEX = "editIndex";
	private static final String STATE_RENDER_FRAGMENT = "renderFragment";

	// used for the SimpleAdapter
	private static final String[] from = new String[] { "icon" };
	private static final int[] to = new int[] { R.id.icon };

	// context
	public Home _home;

	// objects
	private RelativeLayout mContainer;
	private ToolBarGradientList mToolBar;
	private ListView mListView;
	private SimpleAdapter adapterHistory;

	// data
	private ArrayList<HashMap<String, String>> mapIFS;
	private Map<Integer, WeakReference<Bitmap>> mImages;

	// vars
	String mEditing = "";
	private int mScrollPos = 0;
	private int mPosEditing = -1;
	boolean usesSelectionToSaveListViewPosition = false;
	
	// state
	private FragmentRender fR;

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
		_home = fR._home;
	}
	
	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
		if (inState != null) {
			mScrollPos = inState.getInt(STATE_SCROLL_POS, 0);
			mEditing = inState.getString(STATE_EDIT);
			if (mEditing == null)
				mEditing = ""; // because we use equals method, it cannot be null
			mPosEditing = inState.getInt(STATE_EDIT_INDEX, -1);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_gradient_list, null);
		_home = (Home) getActivity();

		if (mListView == null) {
			mListView = new ListView(_home);

			mListView.setBackgroundResource(R.drawable.white);
	
			mapIFS = new ArrayList<HashMap<String, String>>();
			adapterHistory = new ColorAdapter(_home, mapIFS,
					R.layout.list_item_color, from, to);
			mListView.setAdapter(adapterHistory);
	
			// this.setOnItemClickListener(a);
			mListView.setSelector(R.color.empty);
			mListView.setDividerHeight(0);
			mListView.setOnItemClickListener(this);
			mListView.setCacheColorHint(0);
	
			mImages = new HashMap<Integer, WeakReference<Bitmap>>();
	
			mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> av, View v,
						int position, long id) {
					if (!_home.hasPro()) {
						_home.mApp.tooltipToast(R.string.txt_upgrade_need_color_edit);
						return false;
					}
						
					mEditing = mapIFS.get(position).get(GRADIENT_TITLE);
					mPosEditing = position;
					toGradientEditor(mapIFS.get(position).get(GRADIENT_TITLE));
					if (!mapIFS.get(position).get(GRADIENT_TITLE).startsWith(FragmentGradientEditor.USER_GRADIENT_PREFIX)) {
						mListView.setSelection(mListView.getCount()-1);
						usesSelectionToSaveListViewPosition = true;
					}
					return true;
				}
			});
		}
		
		// put items in
		for (int i = 0; i < _home.mGallery.mColorSchemes.size(); i++)
			addColor(_home.mGallery.mColorSchemes.get(i));
		adapterHistory.notifyDataSetChanged();
		getTaskQueue().empty(); // remove the possible task to update the color list

		mContainer = (RelativeLayout) root.findViewById(R.id.container);
		mContainer.addView(mListView, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mToolBar = (ToolBarGradientList) root.findViewById(R.id.toolbar);
		mToolBar.init(this);

		return root;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		AppUtils.putFragment(_home, outState, this, STATE_RENDER_FRAGMENT, fR);
		outState.putInt(STATE_SCROLL_POS, mListView == null ? mScrollPos :
			(usesSelectionToSaveListViewPosition ? mListView.getSelectedItemPosition() : mListView.getFirstVisiblePosition()));
		usesSelectionToSaveListViewPosition = false;
		outState.putString(STATE_EDIT, mEditing);
		outState.putInt(STATE_EDIT_INDEX, mPosEditing);
	}

	@Override
	public void onActivityCreated(Bundle inState) {
		super.onActivityCreated(inState);
		if (inState != null) {
			fR = (FragmentRender)AppUtils.getFragment(_home, inState, this, STATE_RENDER_FRAGMENT);
			mListView.setSelection(mScrollPos);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
    	mContainer.removeView(mListView);
    	
		for (Entry<Integer, WeakReference<Bitmap>> e : mImages.entrySet()) {
			WeakReference<Bitmap> wrb = e.getValue();
			if (wrb != null) {
				Bitmap b = wrb.get();
				if (b != null) {
					b = M.freeBitmap(b);
				}
			}
		}
		mImages.clear();
		mapIFS.clear();
		adapterHistory.notifyDataSetChanged();
	}

	/**
	 * Navigate to the gradient editor
	 * @param edit : name of the color scheme to edit, use null to create a new color scheme
	 * @return : the gradient editor fragment
	 */
	private FragmentGradientEditor toGradientEditor(String edit) {
		FragmentGradientEditor f = new FragmentGradientEditor(this);
		f.setGradient(_home.mGallery, edit);
		_home.toChildFragment(f);
		return f;
	}

	/**
	 * Render a color scheme preview to an image view
	 * @param child : the image view
	 * @param position : the color scheme index
	 */
	private void renderGradientToListView(ImageView child, int position) {
		if (position == -1)
			return;
		ColorScheme cm = _home.mGallery.getColorSchemeByName(mapIFS.get(position)
				.get(GRADIENT_TITLE));
		if (cm != null) {
			Bitmap gradient = cm.generatePreview(App.inDP(320), cm.getGradientCount(), false, 0);
			if (child != null)
				child.setImageBitmap(gradient);
			mImages.put(position, new WeakReference<Bitmap>(gradient));
		}
		return;
	}

	/**
	 * Call this to let the list know that there has been
	 *  changes due to using the gradient editor and it should refresh.
	 */
	protected void gradientListUpdated() {
		if (!isPaused())
			onGradientListUpdated.run();
		else
			queueTask(FragmentEvent.OnResume, onGradientListUpdated);
	}
	
	/**
	 * Runnable that actually does the gradient list updating.
	 */
	private Runnable onGradientListUpdated = new Runnable() {
		public void run() {
			if (mEditing.equals(NEW_GRADIENT)) {
				addColor(_home.mGallery.mColorSchemes.get(_home.mGallery.mColorSchemes.size() - 1));
				adapterHistory.notifyDataSetChanged();
				mListView.setSelection(mListView.getCount()-1);
			} else {
				mImages.remove(mPosEditing);
				renderGradientToListView(
						(ImageView) mListView.getChildAt(
								mPosEditing - mListView.getFirstVisiblePosition())
								.findViewById(R.id.icon), mPosEditing);
			}
		}
	};

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		_home.setColorScheme(mapIFS.get(position).get(GRADIENT_TITLE));
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
		mEditing = NEW_GRADIENT;
		mPosEditing = -1;
		toGradientEditor(mEditing);
	}

	/**
	 * Add a color scheme to the list.
	 * @param colorScheme : the {@link ColorScheme} object
	 */
	private void addColor(ColorScheme colorScheme) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(GRADIENT_TITLE, colorScheme._name);
		mapIFS.add(map);
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

	/**
	 * This class extends SimpleAdapter to provide an easy list of gradients.
	 */
	class ColorAdapter extends SimpleAdapter {
		
		/** default constructor */
		public ColorAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) _home
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_item_color, null);
			}

			View child = v.findViewById(R.id.icon);
			if (child != null)
				child.setVisibility(View.VISIBLE);
			child = v.findViewById(R.id.iconborder);
			if (child != null)
				child.setVisibility(View.VISIBLE);
			TextView t = (TextView) v.findViewById(R.id.newItemText);
			if (t != null)
				t.setVisibility(View.GONE);
			for (int i = 0; i < from.length; i++) {
				child = v.findViewById(to[i]);
				if (child.getClass() == TextView.class)
					((TextView) child).setText(mapIFS.get(position)
							.get(from[i]));
				else if (child.getClass() == ImageView.class) {
					if (mImages.containsKey(position)) {
						if (mImages.get(position).get() != null)
							((ImageView) child).setImageBitmap(mImages.get(
									position).get());
						else
							renderGradientToListView((ImageView) child, position);
					} else
						renderGradientToListView((ImageView) child, position);
				}
			}

			return v;
		}
	}
	
	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return getDefaultAnimationSlideFromBottom(fa);
	}
}
