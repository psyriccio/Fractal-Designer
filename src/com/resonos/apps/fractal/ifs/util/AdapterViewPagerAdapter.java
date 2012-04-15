package com.resonos.apps.fractal.ifs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;

import com.viewpagerindicator.TitleProvider;

/**
 * The name of this class might be a bit confusing.
 * It simply is an extension of the PagerAdapter designed
 *  specifically to work with AdapterViews.
 * Thus it is kind of a double-layer adapter of adapters.
 * @author Chris
 */
public class AdapterViewPagerAdapter extends PagerAdapter implements TitleProvider,
		OnItemClickListener {
	
	/** title data backing the adapter -- just an array of Strings */
	private String[] mTitleData;
	
	/** most adapterviews scroll, so we'll save their scroll positions as well */
	private int[] scrollPosition;
	
	/** listener to funnel item clicks to */
	private AdapterViewPagerAdapterListener mListener;

	/** the actual data, the adapter view adapters, associated with their indeces */
	Map<Integer, BaseAdapterViewAdapter> mAdapters = new HashMap<Integer, BaseAdapterViewAdapter>();

	/**
	 * The implementation of an "Adapter View Adapter"
	 */
	public interface AdapterViewPagerAdapterListener {
		/**
		 * Creates an adapter view based on an index
		 * @param i : the index
		 * @return the new AdapterView
		 */
		public AdapterView<?> onCreateAdapterView(int i);
		
		/**
		 * A comprehensive item click method.
		 * @param adapter : the AdapterView adapter
		 * @param adapterView : the AdapterView itself
		 * @param v : the item view that was clicked
		 * @param adapterIndex : the index of the AdapterView
		 * @param position : the index of the item that was clicked
		 */
		public void onItemClick(BaseAdapterViewAdapter adapter, AdapterView<?> adapterView, View v,
				int adapterIndex, int position);
	}

	/**
	 * The base AdapterView adapter.
	 */
	public abstract static class BaseAdapterViewAdapter extends BaseAdapter {
		private AdapterView<?> mParent;

		/** Constructor, taking a single AdapterView */
		public BaseAdapterViewAdapter(AdapterView<?> lv) {
			super();
			mParent = lv;
		}

		/** Returns the parent AdapterView */
		public AdapterView<?> getAdapterView() {
			return mParent;
		}
	}

	/**
	 * Constructor for an AdapterViewViewAdapter
	 * @param input : a String array representing titles of each AdapterView
	 * @param creator : the implementation -- see {@link AdapterViewPagerAdapterListener}
	 */
	public AdapterViewPagerAdapter(String[] input, AdapterViewPagerAdapterListener creator) {
		super();
		mListener = creator;
		mTitleData = input;
		scrollPosition = new int[mTitleData.length];
		for (int i = 0; i < mTitleData.length; i++) {
			scrollPosition[i] = 0;
		}
	}

	/**
	 * Constructor for an AdapterView
	 * @param res : the app's resources
	 * @param input : a String array representing resource IDs of titles of each AdapterView
	 * @param creator : the implementation -- see {@link AdapterViewPagerAdapterListener}
	 */
	public AdapterViewPagerAdapter(Resources res, int[] input, AdapterViewPagerAdapterListener creator) {
		super();
		mListener = creator;
		mTitleData = new String[input.length];
		for (int i = 0; i < input.length; i++)
			mTitleData[i] = res.getString(input[i]);
		scrollPosition = new int[mTitleData.length];
		for (int i = 0; i < mTitleData.length; i++) {
			scrollPosition[i] = 0;
		}
	}

	@Override
	public Object instantiateItem(View collection, final int position) {
		AdapterView<?> layout = mListener.onCreateAdapterView(position);
		layout.setOnItemClickListener(this);
		mAdapters.put(position, (BaseAdapterViewAdapter) layout.getAdapter());
		((ViewPager) collection).addView(layout);
		layout.setSelection(scrollPosition[position]);
		if (layout instanceof AbsListView)
			((AbsListView)layout).setOnScrollListener(new OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				}
	
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					scrollPosition[position] = firstVisibleItem;
				}
			});
		return layout;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public void restoreState(Parcelable p, ClassLoader c) {
		if (p instanceof ScrollState) {
			scrollPosition = ((ScrollState) p).getScrollPos();
		}
	}

	@Override
	public Parcelable saveState() {
		return new ScrollState(scrollPosition);
	}

	@Override
	public int getItemPosition(Object item) {
		for (int i = 0; i < getCount(); i++) {
			BaseAdapterViewAdapter a = mAdapters.get(i);
			if (a != null) {
				if (a.getAdapterView() == item)
					return i;
			}
		}
		return -1;
	}

	@Override
	public int getCount() {
		return mTitleData.length;
	}

	@Override
	public String getTitle(int position) {
		return mTitleData[position % mTitleData.length].toUpperCase();
	}

	@Override
	public boolean isViewFromObject(View view, Object item) {
		return view.equals(item);
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		for (Entry<Integer, BaseAdapterViewAdapter> e : mAdapters.entrySet()) {
			BaseAdapterViewAdapter adapter = e.getValue();
			int index = e.getKey();
			if (adapter.getAdapterView() == l) {
				mListener.onItemClick(adapter, adapter.getAdapterView(), v, index,
						position);
			}
		}
	}

	/** This class is used to save the scroll positions of each AdapterView.
	 * Based on the following blog post: http://blog.stylingandroid.com/archives/577 */
	public static class ScrollState implements Parcelable {
		private int[] scrollPos;

		public static Parcelable.Creator<ScrollState> CREATOR = new Parcelable.Creator<ScrollState>() {

			@Override
			public ScrollState createFromParcel(Parcel source) {
				int size = source.readInt();
				int[] scrollPos = new int[size];
				source.readIntArray(scrollPos);
				return new ScrollState(scrollPos);
			}

			@Override
			public ScrollState[] newArray(int size) {
				return new ScrollState[size];
			}
		};

		/** Init the parcelable data */
		public ScrollState(int[] scrollPos) {
			this.scrollPos = scrollPos;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(scrollPos.length);
			dest.writeIntArray(scrollPos);
		}

		/**
		 * @return The saved scroll positions.
		 */
		public int[] getScrollPos() {
			return scrollPos;
		}
	}
}
