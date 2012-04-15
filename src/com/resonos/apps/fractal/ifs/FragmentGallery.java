package com.resonos.apps.fractal.ifs;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.WazaBe.HoloEverywhere.TextViewHolo;
import com.resonos.apps.fractal.ifs.R;
import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.model.ColorScheme.PreparedColorScheme;
import com.resonos.apps.fractal.ifs.model.IFSFractal.RawIFSFractal;
import com.resonos.apps.fractal.ifs.util.AdapterViewPagerAdapter;
import com.resonos.apps.fractal.ifs.util.AdapterViewPagerAdapter.AdapterViewPagerAdapterListener;
import com.resonos.apps.fractal.ifs.util.AdapterViewPagerAdapter.BaseAdapterViewAdapter;
import com.resonos.apps.fractal.ifs.util.IFSFile;
import com.resonos.apps.fractal.ifs.util.IFSRender;
import com.resonos.apps.fractal.ifs.util.IFSRender.FractalViewLock;
import com.resonos.apps.fractal.ifs.util.IFSRenderManager;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.media.ImageLoader;
import com.resonos.apps.library.media.ImageLoader.ImageGenerator;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TabPageIndicator;

public class FragmentGallery extends BaseFragment implements FractalViewLock, AdapterViewPagerAdapterListener {
	
	// contants
	private static final String STATE_FROM_EDITOR = "fromEditor";
	
	/** implementation for an "empty" item, not currently used */
	public static final String TITLE_EMPTY = "__empty__";
	
	/** keys for each HashMap backing a fractal */
	public static final String DATA_TITLE = "title";

	// context
	public Home _home;

	// objects
	private AdapterViewPagerAdapter mAdapter;
	private ViewPager mPager;
	private PageIndicator mIndicator;
    public ImageLoader imageLoader;
	
	// drawing
    private ColorDrawable colorDrawable;
	public PreparedColorScheme mColorScheme;
	
	// state
	private boolean mFromEditor = false;
	
	// vars
	private int mColWidth;

	/**
	 * This constructor is available for the Fragment library's reinstantiation after onSaveInstanceState.
	 * It shouldn't be used directly.
	 */
	public FragmentGallery() {
		super();
	}
	
	public FragmentGallery(boolean fromEditor) {
		super();
		mFromEditor = fromEditor;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null) {
			mFromEditor = icicle.getBoolean(STATE_FROM_EDITOR, false);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_FROM_EDITOR, mFromEditor);
	}

	@Override
	public View onCreateView(LayoutInflater l, ViewGroup container,
			Bundle icicle) {
		_home = (Home) getActivity();
		View root = l.inflate(R.layout.fragment_gallery, null);
		mAdapter = new AdapterViewPagerAdapter(_home.getResources(), IFSFile.IFS_CAT_RES_NAMES, this);
		mPager = (ViewPager) root.findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);
		mIndicator = (TabPageIndicator) root.findViewById(R.id.indicator);
		mIndicator.setViewPager(mPager);
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle icicle) {
		super.onCreate(icicle);
		_home = (Home) getActivity();
		updateColors();
		imageLoader = new ImageLoader(_home.mApp, colorDrawable);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateColors();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (imageLoader != null)
			imageLoader.clearCache();
	}
	
	/** update the colors to be used in this gallery */
	private synchronized void updateColors() {
		ColorScheme cm = _home.mGallery.getColorSchemeByName(_home.mSelColors);
		mColorScheme = cm.prepareGradient(IFSRenderManager.PREVIEW_COLOR_STEPS);
		colorDrawable = new ColorDrawable(mColorScheme.mBGColor);
	}

	@Override
	public AdapterView<?> onCreateAdapterView(int i) {
		GridView gv = new GridView(_home);
		ArrayList<HashMap<String, String>> map = new ArrayList<HashMap<String, String>>();
		IFSAdapter adapter = new IFSAdapter(i, gv, map);
		gv.setAdapter(adapter);
		gv.setSelector(R.color.empty);
		gv.setCacheColorHint(_home.getResources().getColor(R.color.gallery_bg));
		int n = App.SCREEN_WIDTH > App.SCREEN_HEIGHT ? 3 : 2;
		if (App.SCREEN_SIZE == Configuration.SCREENLAYOUT_SIZE_XLARGE)
			n++;
		mColWidth = App.SCREEN_WIDTH / n;
		gv.setNumColumns(n);
		gv.setVerticalSpacing((int)(0.5 + 1 * App.DENSITY));
		gv.setHorizontalSpacing((int)(0.5 + 1 * App.DENSITY));
		gv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		TextViewHolo etv = new TextViewHolo(_home, null);
		etv.setText(_home.hasPro() ? R.string.txt_gallery_saved_empty_pro : R.string.txt_gallery_saved_empty);
		gv.setEmptyView(etv);

		addItemsFromCategory(adapter, i);
		return gv;
	}

	/**
	 * Fills in an IFSAdapter with its fractals
	 * @param a : the adapter
	 * @param index : the category index
	 */
	private void addItemsFromCategory(IFSAdapter a, int index) {
		a._map.clear();
		String name;
		for (int i = 0; i < _home.mGallery.mFractals.size(); i++) {
			name = _home.mGallery.mFractals.get(i)._name;
			int cat = IFSFile.getCatFromFileName(name);
			String subname = IFSFile.getNameFromFileName(name);
			if (cat == index)
				addIFS(subname, a._map);
		}
		a.notifyDataSetChanged();
	}

	/**
	 * Adds a single IFS to the data backing an adapter
	 * @param title : the name of the IFS
	 * @param mapIFS : the data to add it to.
	 */
	public void addIFS(String title, ArrayList<HashMap<String, String>> mapIFS) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(DATA_TITLE, title);
		mapIFS.add(map);
	}

	@Override
	public void onItemClick(BaseAdapterViewAdapter adapter, AdapterView<?> lv, View v, int listIndex, int position) {
		IFSAdapter a = (IFSAdapter)adapter;
		String title = a._map.get(position).get(DATA_TITLE);
		if (!title.equals(TITLE_EMPTY)) {
			if (mFromEditor) {
				_home.onBackPressed();
				_home.fE.loadIFS(IFSFile.generateIFSFileName(a._cat, title));
			} else {
				_home.toEditorFromLevel1();
				_home.fE.loadIFS(IFSFile.generateIFSFileName(a._cat, title));
			}
		}
	}

	/**
	 * An adapter for adapter views, in this case,
	 *  an adapter to show GridViews of loadable fractals.
	 * @author Chris
	 */
	class IFSAdapter extends BaseAdapterViewAdapter implements ImageGenerator {
		public static final String IMGEN_TAG = "ifsPreviewGen";
		private ArrayList<HashMap<String, String>> _map;
		private int _cat;

		public IFSAdapter(int cat, GridView gv, ArrayList<HashMap<String, String>> data) {
			super(gv);
			_cat = cat;
			_map = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) _home
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_item_ifs, null);
			}
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, mColWidth);

	        View image = v.findViewById(R.id.icon);
	        TextView text = (TextView)v.findViewById(R.id.itemTitle);
	        
	        image.setLayoutParams(lp);

			if (_map.get(position).get(DATA_TITLE).equals(TITLE_EMPTY)) {
				text.setVisibility(View.INVISIBLE);
			} else {
				text.setVisibility(View.VISIBLE);
				text.setText(_map.get(position).get(DATA_TITLE));
				image.setBackgroundDrawable(colorDrawable);
				imageLoader.DisplayImageGen(this, IFSFile.generateIFSFileName(_cat,
						_map.get(position).get(DATA_TITLE)), image, IFSRenderManager.PREVIEW_WIDTH);
			}
			return v;
		}

	    public int getCount() {
	        return _map.size();
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }

		@Override
		public Bitmap generateImage(String param) {
			RawIFSFractal ifs = _home.mGallery.getFractalByName(param).calculateRaw();
			IFSRenderManager manager = new IFSRenderManager(FragmentGallery.this, ifs) {
				public void onCompleteRender(boolean error, boolean oome) { }
			};
			Bitmap bmp = Bitmap.createBitmap(manager.mWidth, manager.mHeight, Config.ARGB_8888);
			IFSRender render = new IFSRender(manager, bmp);
			render.renderThreadless();
			return bmp;
		}

		public String getTag() {
			return IMGEN_TAG;
		}
	}

	@Override
	public String getTitle() {
		return getString(R.string.title_gallery);
	}

	@Override
	public boolean onBackPressed() {
		return false;
	}

	@Override
	protected void onCreateOptionsMenu(ArrayList<Action> items) {
		// nothing
	}

	@Override
	protected void onOptionsItemSelected(Enum<?> action) {
		// nothing
	}
	
	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return getDefaultAnimationSlideFromRight(fa);
	}

	public void clearCache() {
		if (imageLoader != null)
			imageLoader.clearCache();
	}
}
