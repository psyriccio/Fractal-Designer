package com.resonos.apps.fractal.ifs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.WazaBe.HoloEverywhere.TextViewHolo;
import com.resonos.apps.fractal.ifs.model.ColorScheme;
import com.resonos.apps.fractal.ifs.model.ColorScheme.PreparedColorScheme;
import com.resonos.apps.fractal.ifs.model.IFSFractal;
import com.resonos.apps.fractal.ifs.util.IFSFile;
import com.resonos.apps.fractal.ifs.util.IFSRender;
import com.resonos.apps.fractal.ifs.util.IFSRender.FractalViewLock;
import com.resonos.apps.fractal.ifs.util.IFSRenderManager;
import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.media.ImageLoader;
import com.resonos.apps.library.media.ImageLoader.ImageGenerator;
import com.resonos.apps.library.tabviewpager.TabViewPagerFragment;

public class FragmentGallery extends TabViewPagerFragment implements FractalViewLock, OnItemClickListener {
	
	// contants
	private static final String ARG_FROM_EDITOR = "fromEditor";
	
	/** implementation for an "empty" item, not currently used */
	public static final String TITLE_EMPTY = "__empty__";
	
	/** keys for each HashMap backing a fractal */
	public static final String DATA_TITLE = "title";

	// objects
    public ImageLoader imageLoader;
	
	// drawing
    private ColorDrawable colorDrawable;
	public PreparedColorScheme mColorScheme;
	
	// vars
	private int mColWidth;
	
	/**
	 * Use this method to create this fragment with the proper arguments
	 * @param fromEditor : true if the editor loaded this gallery
	 * @return a new FragmentGallery
	 */
	public static FragmentGallery create(boolean fromEditor) {
		Bundle b = new Bundle();
		b.putBoolean(ARG_FROM_EDITOR, fromEditor);
		FragmentGallery f = new FragmentGallery();
		f.setArguments(b);
		return f;
	}
	
	/**
	 * Convenience for getting our parent activity
	 * @return a {@link Home} object
	 */
	public Home getHome() {
		return (Home)getActivity();
	}
	
	@Override
	protected int[] getData() {
		return IFSFile.IFS_CAT_RES_NAMES;
	}
	
	@Override
	public void onActivityCreated(Bundle icicle) {
		super.onCreate(icicle);
		updateColors();
		imageLoader = new ImageLoader(getHome().mApp, colorDrawable);
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
		ColorScheme cm = getHome().mGallery.getColorSchemeByName(getHome().mSelColors);
		mColorScheme = cm.prepareGradient(IFSRenderManager.PREVIEW_COLOR_STEPS);
		colorDrawable = new ColorDrawable(mColorScheme.mBGColor);
	}

	@Override
	protected View getView(int i) {
		GridView gv = new GridView(getHome());
		List<Map<String, String>> map = new ArrayList<Map<String, String>>();
		IFSAdapter adapter = new IFSAdapter(i, gv, map);
		gv.setAdapter(adapter);
		gv.setSelector(R.color.empty);
		gv.setCacheColorHint(getHome().getResources().getColor(R.color.gallery_bg));
		int n = App.SCREEN_WIDTH > App.SCREEN_HEIGHT ? 3 : 2;
		if (App.SCREEN_SIZE == Configuration.SCREENLAYOUT_SIZE_XLARGE)
			n++;
		mColWidth = App.SCREEN_WIDTH / n;
		gv.setNumColumns(n);
		gv.setVerticalSpacing((int)(0.5 + 1 * App.DENSITY));
		gv.setHorizontalSpacing((int)(0.5 + 1 * App.DENSITY));
		gv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		TextViewHolo etv = new TextViewHolo(getHome(), null);
		etv.setText(getHome().hasPro() ? R.string.txt_gallery_saved_empty_pro : R.string.txt_gallery_saved_empty);
		gv.setEmptyView(etv);
		gv.setOnItemClickListener(this);

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
		for (int i = 0; i < getHome().mGallery.mFractals.size(); i++) {
			name = getHome().mGallery.mFractals.get(i)._name;
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
	public void addIFS(String title, List<Map<String, String>> mapIFS) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(DATA_TITLE, title);
		mapIFS.add(map);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (getHome() == null)
			return;
		IFSAdapter a = (IFSAdapter)parent.getAdapter();
		String title = a.getItemData(position).get(DATA_TITLE);
		if (!title.equals(TITLE_EMPTY)) {
			if (getArguments().getBoolean(ARG_FROM_EDITOR) && getHome().fE != null) {
				getHome().backOneFragment();
				getHome().fE.loadIFS(IFSFile.generateIFSFileName(a._cat, title));
			} else {
				getHome().toEditorFromLevel1();
				getHome().fE.loadIFS(IFSFile.generateIFSFileName(a._cat, title));
			}
		}
	}

	/**
	 * Clear the gallery cache
	 */
	public void clearCache() {
		if (imageLoader != null)
			imageLoader.clearCache();
	}
	
	private static class ViewHolder {
		private View image;
		private TextView text;
		public ViewHolder(View v) {
			image = v.findViewById(R.id.icon);
			text = (TextView)v.findViewById(R.id.itemTitle);
		}
	}

	/**
	 * An adapter for adapter views, in this case,
	 *  an adapter to show GridViews of loadable fractals.
	 */
	class IFSAdapter extends BaseAdapter implements ImageGenerator {
		public static final String IMGEN_TAG = "ifsPreviewGen";
		private List<Map<String, String>> _map;
		private int _cat;

		public IFSAdapter(int cat, GridView gv, List<Map<String, String>> data) {
			super();
			_cat = cat;
			_map = data;
		}

		/**
		 * accessor for a single item's data
		 * @param position : item index
		 * @return a String-String map of keys and values
		 */
		public Map<String, String> getItemData(int position) {
			return _map.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			ViewHolder vh;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getHome()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_item_ifs, null);
				vh = new ViewHolder(v);
				v.setTag(vh);
			} else
				vh = (ViewHolder)v.getTag();
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, mColWidth);
	        
	        vh.image.setLayoutParams(lp);

			if (_map.get(position).get(DATA_TITLE).equals(TITLE_EMPTY)) {
				vh.text.setVisibility(View.INVISIBLE);
			} else {
				vh.text.setVisibility(View.VISIBLE);
				vh.text.setText(_map.get(position).get(DATA_TITLE));
				vh.image.setBackgroundDrawable(colorDrawable);
				imageLoader.DisplayImageGen(this, IFSFile.generateIFSFileName(_cat,
						_map.get(position).get(DATA_TITLE)), vh.image, IFSRenderManager.PREVIEW_WIDTH);
			}
			return v;
		}

		@Override
	    public int getCount() {
	        return _map.size();
	    }

		@Override
	    public Object getItem(int position) {
	        return position;
	    }

		@Override
	    public long getItemId(int position) {
	        return position;
	    }

		@Override
		public Bitmap generateImage(String param) {
			IFSFractal ifs = getHome().mGallery.getFractalByName(param);
			IFSRenderManager manager = new IFSRenderManager(FragmentGallery.this, ifs) {
				public void onCompleteRender(boolean error, boolean oome) { }
			};
			Bitmap bmp = Bitmap.createBitmap(manager.mWidth, manager.mHeight, Config.ARGB_8888);
			IFSRender render = new IFSRender(manager, bmp);
			render.renderThreadless();
			return bmp;
		}

		@Override
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

	@Override
	protected int[] getColumnData() {
		return null;
	}

	@Override
	protected int[] getIconData() {
		return null;
	}

	@Override
	protected boolean[] getVisibleData() {
		return null;
	}
}
