package com.resonos.apps.fractal.ifs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import br.com.dina.ui.BasicItem;
import br.com.dina.ui.UITableView;
import br.com.dina.ui.UITableView.ClickListener;

import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.util.AppUtils;
import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;

/**
 * This is the introduction fragment and is mostly for navigation and displaying information.
 * @author Chris
 */
public class FragmentMain extends BaseFragment implements ClickListener {

	/** the built-in categories */
	private static final int[] PAGE_TITLES_RES = new int[] {
		R.string.txt_page01, R.string.txt_page02, R.string.txt_page03,
		R.string.txt_page04, R.string.txt_page05, R.string.txt_page06};
	private enum Page {HOME, AUTHOR, UPGRADE, CHANGELOG, PERMISSIONS, LICENSE};
	
	// context
	public Home _home;

	// objects
	UITableViewAdapter mAdapter;
	ViewPager mPager;
	PageIndicator mIndicator;
	UITableView uiMainBlock;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null) {
			//
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater l, ViewGroup container,
			Bundle icicle) {
		_home = (Home) getActivity();
		View root = l.inflate(R.layout.fragment_main, null);
		mAdapter = new UITableViewAdapter(PAGE_TITLES_RES);
		mPager = (ViewPager)root.findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);
		mIndicator = (TabPageIndicator)root.findViewById(R.id.indicator);
		mIndicator.setViewPager(mPager);
		return root;
	}
	
	@Override
	public void onActivityCreated(Bundle icicle) {
		super.onCreate(icicle);
		_home = (Home)getActivity();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	/**
	 * Create the UI for this view.
	 * Done rather manually, it might be better to use a markup language...
	 * @param position : the page to create
	 * @return
	 */
	private View createUITableView(Page page) {
		ScrollView sv = new ScrollView(_home);
		sv.setFillViewport(true);
		LinearLayout ll = new LinearLayout(_home);
		ll.setGravity(Gravity.CENTER);
		ll.setOrientation(LinearLayout.VERTICAL);
		sv.addView(ll);
		UITableView v;

		int actionsize = App.inDP(32);
		int iconsize = App.inDP(48);
		
		int maxWidth = App.inDP(560);
		LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(
				App.SCREEN_WIDTH > maxWidth ? maxWidth : LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
		
		switch (page) {
		case HOME:
			uiMainBlock = new UITableView(_home, null);
			setupMainBlock(uiMainBlock);
			ll.addView(uiMainBlock, lllp);

			v = new UITableView(_home, null);
			v.addBasicItem(new BasicItem(getString(R.string.txt_like_title,
					_home.mApp.getAppName())).setClickable(false).setGravity(Gravity.CENTER));
			v.addBasicItem(new BasicItem(R.drawable.ic_action_share,
					getString(R.string.txt_like_share), getString(R.string.txt_like_share_desc))
					.setTag(Actions.APP_SHARE).setDrawableSize(actionsize).setIconColor(Color.BLACK));
			v.addBasicItem(new BasicItem(R.drawable.ic_action_rate_up,
					getString(R.string.txt_like_rate, _home.mApp.getAppName()),
					getString(R.string.txt_like_rate_desc)).setTag(Actions.APP_RATE)
					.setDrawableSize(actionsize).setIconColor(Color.BLACK));
			v.addBasicItem(new BasicItem(R.drawable.ic_action_send,
					getString(R.string.txt_like_send), getString(R.string.txt_like_send_desc))
					.setTag(Actions.APP_FEEDBACK).setDrawableSize(actionsize).setIconColor(Color.BLACK));
			v.setClickListener(this);
			v.commit();
			ll.addView(v, lllp);
			break;
		case AUTHOR:
			v = new UITableView(_home, null);
			v.addBasicItem(new BasicItem(getString(R.string.txt_author),
					getString(R.string.txt_author_desc)).setGravity(Gravity.CENTER).setClickable(false));
			v.addBasicItem(new BasicItem(R.drawable.icon_resonos,
					getString(R.string.txt_author_site), getString(R.string.txt_author_site_desc))
					.setTag(Actions.WEBSITE).setDrawableSize(iconsize));
			v.setClickListener(this);
			v.commit();
			ll.addView(v, lllp);

			v = new UITableView(_home, null);
			v.addBasicItem(new BasicItem(getString(R.string.txt_apps))
					.setGravity(Gravity.CENTER).setClickable(false));
			v.addBasicItem(new BasicItem(R.drawable.icon_kaleidoscope,
					getString(R.string.txt_apps_k), getString(R.string.txt_apps_k_desc))
					.setTag(Actions.APP_KSCOPE).setDrawableSize(iconsize));
			v.addBasicItem(new BasicItem(R.drawable.icon_bgo,
					getString(R.string.txt_apps_bgo), getString(R.string.txt_apps_bgo_desc))
					.setTag(Actions.APP_BGO).setDrawableSize(iconsize));
			v.addBasicItem(new BasicItem(R.drawable.icon_jb,
					getString(R.string.txt_apps_jb), getString(R.string.txt_apps_jb_desc))
					.setTag(Actions.APP_JB).setDrawableSize(iconsize));
			v.addBasicItem(new BasicItem(R.drawable.icon_bball,
					getString(R.string.txt_apps_bb), getString(R.string.txt_apps_bb_desc))
					.setTag(Actions.APP_BBALL).setDrawableSize(iconsize));
			v.setClickListener(this);
			v.commit();
			ll.addView(v, lllp);
			break;
		case UPGRADE:
			if (_home.hasPro()) {
				v = new UITableView(_home, null);
				v.addBasicItem(new BasicItem(R.drawable.icon_pro, getString(R.string.txt_upgrade_haveit),
						getString(R.string.txt_upgrade_haveit_desc)).setGravity(Gravity.CENTER)
						.setClickable(false).setDrawableSize(iconsize));
				v.setClickListener(this);
				v.commit();
				ll.addView(v, lllp);
			} else {
				v = new UITableView(_home, null);
				v.addBasicItem(new BasicItem(getString(R.string.txt_upgrade),
						getString(R.string.txt_upgrade_sub)).setGravity(Gravity.CENTER).setClickable(false));
				v.addBasicItem(new BasicItem(null,
						getString(R.string.txt_upgrade_desc)).setClickable(false));
				v.addBasicItem(new BasicItem(getString(R.string.txt_upgrade_features),
						getString(R.string.txt_upgrade_features_desc)).setClickable(false));
				v.addBasicItem(new BasicItem(R.drawable.icon_pro,
						getString(R.string.txt_upgrade_getit), getString(R.string.txt_upgrade_getit_desc))
						.setTag(Actions.UPGRADE).setDrawableSize(iconsize));
				v.setClickListener(this);
				v.commit();
				ll.addView(v, lllp);
			}
			break;
		case CHANGELOG:
			v = new UITableView(_home, null);
			addDataFromString(v, R.string.txt_changelog);
			v.commit();
			ll.addView(v, lllp);
			break;
		case PERMISSIONS:
			v = new UITableView(_home, null);
			addDataFromString(v, R.string.txt_permissions);
			v.commit();
			ll.addView(v, lllp);
			break;
		case LICENSE:
			v = new UITableView(_home, null);
			v.addBasicItem(new BasicItem(getString(R.string.txt_license_title),
					getString(R.string.txt_license_desc)).setGravity(Gravity.CENTER).setClickable(false));
			addDataFromString(v, R.string.txt_license);
			v.commit();
			ll.addView(v, lllp);
			break;
		}
		return sv;
	}

	/**
	 * Some of the data to display in stored as string resources in the format:
	 *    title1|desc1|title2|desc2|etc....
	 * This will add that data in rows to a UITableView
	 * @param v : the UITableView
	 * @param txt : the string resource to load from
	 */
	private void addDataFromString(UITableView v, int txt) {
		String[] lines = getString(txt).split("\\|");
		for (int i = 0; i < lines.length/2; i++)
			v.addBasicItem(new BasicItem((lines[i*2].trim().equals("") ? null : lines[i*2]),
					(lines[i*2+1].trim().equals("") ? null : lines[i*2+1])).setClickable(false));
	}

	/** 
	 * call this when the app finds out it is an old version,
	 * to refresh the home page UITableView 
	 * @param newVersionID : the new version
	 */
	public void onOldVersion(String newVersionID) {
		_home.invalidateOptionsMenu();
		setupMainBlock(uiMainBlock);
	}
	
	/**
	 * Set up the main block on Page.HOME.
	 * We've separated out this function because
	 *	when the app receives data from the internet, it may
	 *  invalidate this table to insert a line.
	 * @param v
	 */
	private void setupMainBlock(UITableView v) {
		int actionsize = App.inDP(32);
		
		v.clear();
		v.addBasicItem(new BasicItem(_home.mApp.getAppName(), "Version " + _home.mApp.getVersionID()).setClickable(false).setGravity(Gravity.CENTER));

		_home = (Home)getActivity();
		if (_home != null)
			if (_home.mApp.isOldVersion())
				v.addBasicItem(new BasicItem(R.drawable.ic_action_info, "Update Available!", "Version " + _home.mApp.mNewVersionID + " is out, get it now!").setTag(Actions.UPDATE).setDrawableSize(actionsize).setColor(0xFF880000).setIconColor(0xFF880000));
		
		v.addBasicItem(new BasicItem(R.drawable.home_btn_loadpic, "Editor", "Open up the editor to get started!").setTag(Actions.TO_EDITOR).setIconColor(0xFF99CC00));
		v.addBasicItem(new BasicItem(R.drawable.home_btn_gallery, "Gallery", "Check out the fractal gallery.").setTag(Actions.TO_GALLERY).setIconColor(0xFF33B5E5));
		v.addBasicItem(new BasicItem(R.drawable.home_btn_help, "Tutorial", "Confused? Get some help here.").setTag(Actions.TO_TUTORIAL).setIconColor(0xFFAA66CC));
		if (!_home.hasPro())
			v.addBasicItem(new BasicItem(R.drawable.home_btn_upgrade, "Upgrade", "Unlock the full potential of this app!").setTag(Actions.TO_UPGRADE).setIconColor(0xFFFFBB33));
		v.setClickListener(this);
		v.commit();
	}

	/** an enum representing the possible actions, either from the actioni bar or the UITableViews */
	public enum Actions {UPGRADE, UPDATE, TO_EDITOR, TO_GALLERY, TO_TUTORIAL, TO_UPGRADE, APP_SHARE, APP_RATE, APP_FEEDBACK, WEBSITE, APP_KSCOPE, APP_BGO, APP_JB, APP_BBALL};

	@Override
	public void onClick(UITableView tv, Enum<?> tag, int position) {
		Actions a = (Actions)tag;
		switch (a) {
		case UPDATE:
			AppUtils.launchMarketThisApp(_home.mApp);
			break;
		case TO_EDITOR:
            _home.toEditorFromRoot();
			break;
		case TO_GALLERY:
            _home.toGallery();
			break;
		case TO_TUTORIAL:
            _home.toHelp();
			break;
		case TO_UPGRADE:
			if (mPager.getCurrentItem() != Page.UPGRADE.ordinal())
				mPager.setCurrentItem(Page.UPGRADE.ordinal(), true);
			break;
		case UPGRADE:
			AppUtils.launchMarketPro(_home.mApp);
			break;
		case APP_SHARE:
        	Intent imgIntent = new Intent(android.content.Intent.ACTION_SEND);  
        	imgIntent.setType("text/plain");
        	imgIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.txt_share, 
        			AppUtils.MARKET_URL_PREFIX + _home.mApp.mAppInfo._packageName));  
        	startActivity(Intent.createChooser(imgIntent, "Share..."));
			break;
		case APP_RATE:
			AppUtils.launchMarketThisApp(_home.mApp);
			break;
		case APP_FEEDBACK:
			AppUtils.launchHelpEmail(_home.mApp);
			break;
		case WEBSITE:
			AppUtils.loadPage(_home.mApp, Home.WEB_PAGE);
			break;
		case APP_KSCOPE:
			AppUtils.launchMarket(_home, "com.resonos.apps.kaleidoscope");
			break;
		case APP_BGO:
			AppUtils.loadPage(_home.mApp, "http://download.resonos.com/");
			break;
		case APP_JB:
			AppUtils.launchMarket(_home, "com.resonos.games.jewelblaster");
			break;
		case APP_BBALL:
			AppUtils.launchMarket(_home, "com.resonos.games.basketball");
			break;
		}
	}

	@Override
	public boolean onBackPressed() {
		if (mPager.getCurrentItem() != Page.HOME.ordinal()) {
			mPager.setCurrentItem(Page.HOME.ordinal(), true);
			return true;
		}
		return false;
	}

	@Override
	protected void onCreateOptionsMenu(ArrayList<Action> items) {
		if (_home != null)
			if (_home.mApp.isOldVersion())
				items.add(new Action(getString(R.string.btn_update), 0, true, false, Actions.UPDATE));
	}

	@Override
	protected void onOptionsItemSelected(Enum<?> e) {
		switch ((Actions)e) {
		case UPDATE:
			AppUtils.launchMarketThisApp(_home.mApp);
        	break;
		}
	}
	
	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return 0;
	}

	/**
	 * This is a simple extension of a PagerAdapter to show {@link UITableView}s
	 */
	public class UITableViewAdapter extends PagerAdapter implements TitleProvider {
		
		// data
		private String[] mContent;
		private Map<Integer, View> mViews = new HashMap<Integer, View>();

		public UITableViewAdapter(int[] pageTitlesRes) {
			super();
			mContent = new String[pageTitlesRes.length];
			for (int i = 0; i < pageTitlesRes.length; i++)
				mContent[i] = _home.getString(pageTitlesRes[i]);
		}

		@Override
		public Object instantiateItem(View collection, final int position) {
			View layout = createUITableView(Page.values()[position]);
			mViews.put(position, layout);
			((ViewPager) collection).addView(layout);
			return layout;
		}

		@Override
		public void destroyItem(View collection, int position, Object view) {
			((ViewPager) collection).removeView((View) view);
		}

		@Override
		public int getItemPosition(Object item) {
			for (int i = 0; i < getCount(); i++) {
				if (mViews.get(i) == item)
					return i;
			}
			return -1;
		}

		@Override
		public int getCount() {
			return mContent.length;
		}

		@Override
		public String getTitle(int position) {
			return mContent[position % mContent.length].toUpperCase();
		}

		@Override
		public boolean isViewFromObject(View view, Object item) {
			return view.equals(item);
		}
	}
}
