package com.resonos.apps.fractal.ifs;

import java.util.ArrayList;

import android.content.Intent;
import android.view.View;

import com.resonos.apps.library.Action;
import com.resonos.apps.library.App;
import com.resonos.apps.library.BaseFragment;
import com.resonos.apps.library.tabviewpager.TabViewPagerFragment;
import com.resonos.apps.library.util.AppUtils;
import com.resonos.apps.library.util.M;
import com.resonos.apps.library.widget.FormBuilder;
import com.resonos.apps.library.widget.FormElement;
import com.resonos.apps.library.widget.ListFormBuilder;
import com.resonos.apps.library.widget.ListFormBuilder.OnFormItemClickListener;

/**
 * This is the introduction fragment and is mostly for navigation and displaying
 * information.
 * 
 * @author Chris
 */
public class FragmentMain extends TabViewPagerFragment implements
		OnFormItemClickListener {

	/** the built-in categories */
	private static final int[] PAGE_TITLES_RES = new int[] {
			R.string.txt_page01, R.string.txt_page02, R.string.txt_page03,
			R.string.txt_page04, R.string.txt_page05, R.string.txt_page06 };

	private enum Page {
		HOME, AUTHOR, UPGRADE, CHANGELOG, PERMISSIONS, LICENSE
	};

	private static final int[] PAGE_COLUMNS = new int[] { 0, 1, 1, 1, 1, 1 };

	FormBuilder mainPage;

	@Override
	protected int[] getData() {
		return PAGE_TITLES_RES;
	}

	@Override
	protected int[] getColumnData() {
		return PAGE_COLUMNS;
	}

	@Override
	protected int[] getIconData() {
		return null;
	}

	@Override
	protected boolean[] getVisibleData() {
		return null;
	}

	/**
	 * Convenience to get the host activity as its main class
	 * 
	 * @return a {@link Home} object
	 */
	public Home getHome() {
		return (Home) getActivity();
	}

	@Override
	protected View getView(int position) {
		int iconsize = App.inDP(48);
		Page p = Page.values()[position];
		switch (p) {
		case HOME:
			mainPage = new ListFormBuilder(getActivity(), null, this);
			FormBuilder b = mainPage;
			return buildMainPage(b);
		case AUTHOR:
			b = new ListFormBuilder(getActivity(), null, this);
			b.newSection(getString(R.string.txt_author));
			b.newItem()
					.text(R.string.txt_author_site,
							R.string.txt_author_site_desc)
					.icon(R.drawable.icon_resonos).drawSize(iconsize)
					.onClick(Actions.WEBSITE);

			b.newSection(getString(R.string.txt_apps));
			b.newItem().text(R.string.txt_apps_k, R.string.txt_apps_k_desc)
					.icon(R.drawable.icon_kaleidoscope).drawSize(iconsize)
					.onClick(Actions.APP_KSCOPE);
			b.newItem().text(R.string.txt_apps_bgo, R.string.txt_apps_bgo_desc)
					.icon(R.drawable.icon_bgo).drawSize(iconsize)
					.onClick(Actions.APP_BGO);
			b.newItem().text(R.string.txt_apps_jb, R.string.txt_apps_jb_desc)
					.icon(R.drawable.icon_jb).drawSize(iconsize)
					.onClick(Actions.APP_JB);
			b.newItem().text(R.string.txt_apps_bb, R.string.txt_apps_bb_desc)
					.icon(R.drawable.icon_bball).drawSize(iconsize)
					.onClick(Actions.APP_BBALL);
			return b.finish();
		case UPGRADE:
			b = new ListFormBuilder(getActivity(), null, this);
			if (getHome().hasPro()) {
				b.newItem()
						.text(R.string.txt_upgrade_haveit,
								R.string.txt_upgrade_haveit_desc).center()
						.icon(R.drawable.icon_pro).drawSize(iconsize);
			} else {
				b.newItem()
						.text(R.string.txt_upgrade, R.string.txt_upgrade_sub)
						.center();
				b.newItem().subtitle(R.string.txt_upgrade_desc);
				b.newItem().text(R.string.txt_upgrade_features,
						R.string.txt_upgrade_features_desc);
				b.newItem()
						.text(R.string.txt_upgrade_getit,
								R.string.txt_upgrade_getit_desc).center()
						.icon(R.drawable.icon_pro).drawSize(iconsize)
						.onClick(Actions.UPGRADE);
			}
			return b.finish();
		case CHANGELOG:
			b = new ListFormBuilder(getActivity(), null, this);
			addDataFromString(b, R.string.txt_changelog);
			return b.finish();
		case PERMISSIONS:
			b = new ListFormBuilder(getActivity(), null, this);
			addDataFromString(b, R.string.txt_permissions);
			return b.finish();
		case LICENSE:
			b = new ListFormBuilder(getActivity(), null, this);
			b.newItem()
					.text(R.string.txt_license_title, R.string.txt_license_desc)
					.center();
			addDataFromString(b, R.string.txt_license);
			return b.finish();
		}
		M.loge("FragmentPublic", "getView() returning NULL!!! pos = "
				+ position);
		return null;
	}

	/**
	 * Set up the main block on Page.HOME. We've separated out this function
	 * because when the app receives data from the internet, it may invalidate
	 * this table to insert a line.
	 * 
	 * @param b
	 *            : the form builder
	 */
	private View buildMainPage(FormBuilder b) {
		int actionsize = App.inDP(32);
		int iconsize = App.inDP(48);
		Home home = getHome();

		b.clear();

		b.newSection(R.string.txt_main_welcome);

		if (home != null)
			if (home.mApp.isOldVersion())
				b.newItem()
						.text(getString(R.string.txt_main_update),
								getString(R.string.txt_main_update_desc,
										home.mApp.mNewVersionID))
						.onClick(Actions.UPDATE)
						.icon(R.drawable.ic_action_download)
						.drawColor(0xFFFF0000).drawSize(actionsize)
						.textColor(0xFFFFDDDD);

		b.newItem().icon(R.drawable.home_btn_loadpic)
				.text(R.string.txt_main_editor, R.string.txt_main_editor_desc)
				.onClick(Actions.TO_EDITOR).drawColor(0xFF99CC00).drawSize(iconsize);
		b.newItem()
				.icon(R.drawable.home_btn_gallery)
				.text(R.string.txt_main_gallery, R.string.txt_main_gallery_desc)
				.onClick(Actions.TO_GALLERY).drawColor(0xFF33B5E5).drawSize(iconsize);
		b.newItem()
				.icon(R.drawable.home_btn_help)
				.text(R.string.txt_main_tutorial,
						R.string.txt_main_tutorial_desc)
				.onClick(Actions.TO_TUTORIAL).drawColor(0xFFAA66CC).drawSize(iconsize);
		if (!getHome().hasPro())
			b.newItem()
					.icon(R.drawable.home_btn_upgrade)
					.text(R.string.txt_main_upgrade,
							R.string.txt_main_upgrade_desc)
					.onClick(Actions.TO_UPGRADE).drawColor(0xFFFFBB33).drawSize(iconsize);

		b.newSection(getString(R.string.txt_like_title,
				getHome().mApp.getAppName()));
		b.newItem().text(R.string.txt_like_share, R.string.txt_like_share_desc)
				.onClick(Actions.APP_SHARE).icon(R.drawable.ic_action_share)
				.drawSize(actionsize);
		b.newItem()
				.text(getString(R.string.txt_like_rate,
						getHome().mApp.getAppName()),
						getString(R.string.txt_like_rate_desc))
				.onClick(Actions.APP_RATE).icon(R.drawable.ic_action_rate_up)
				.drawSize(actionsize);
		b.newItem().text(R.string.txt_like_send, R.string.txt_like_send_desc)
				.onClick(Actions.APP_FEEDBACK).icon(R.drawable.ic_action_send)
				.drawSize(actionsize);

		b.newSection(getString(R.string.txt_thanks_to));
		b.newItem().subtitle(R.string.txt_brazil).onClick(Actions.TO_BRAZIL);
		return b.finish();
	}

	/**
	 * Some of the data to display in stored as string resources in the format:
	 * title1|desc1|title2|desc2|etc.... This will add that data in rows to a
	 * UITableView
	 * 
	 * @param v
	 *            : the FormBuilder
	 * @param txt
	 *            : the string resource to load from
	 */
	private void addDataFromString(FormBuilder b, int txt) {
		String[] lines = getString(txt).split("\\|");
		for (int i = 0; i < lines.length / 2; i++)
			b.newItem().text(
					(lines[i * 2].trim().equals("") ? null : lines[i * 2]),
					(lines[i * 2 + 1].trim().equals("") ? null
							: lines[i * 2 + 1]));
	}

	/**
	 * call this when the app finds out it is an old version, to refresh the
	 * home page UITableView
	 * 
	 * @param newVersionID
	 *            : the new version
	 */
	public void onOldVersion(String newVersionID) {
		getHome().invalidateOptionsMenu();
		invalidate();
	}

	/**
	 * Rebuild anything that needs to be rebuilt
	 */
	public void invalidate() {
		if (mainPage != null)
			buildMainPage(mainPage);
	}

	/**
	 * an enum representing the possible actions, either from the actioni bar or
	 * the UITableViews
	 */
	public enum Actions {
		UPGRADE, UPDATE, TO_EDITOR, TO_GALLERY, TO_TUTORIAL, TO_UPGRADE, APP_SHARE, APP_RATE, APP_FEEDBACK, WEBSITE, APP_KSCOPE, APP_BGO, APP_JB, APP_BBALL, TO_BRAZIL
	};

	@Override
	public void onClick(Enum<?> tag, int pos, FormElement fe) {
		Actions a = (Actions) tag;
		Home home = getHome();
		switch (a) {
		case UPDATE:
			AppUtils.launchMarketThisApp(home.mApp);
			break;
		case TO_EDITOR:
			home.toEditorFromRoot();
			break;
		case TO_GALLERY:
			home.toGallery();
			break;
		case TO_TUTORIAL:
			home.toHelp();
			break;
		case TO_UPGRADE:
			// hacky method of getting to the upgrade page whether we're in one
			// or two column mode
			int col = getColumnCount() - 1;
			int colSub = getColumnCount() - 1;
			if (getPager(col).getCurrentItem() != (Page.UPGRADE.ordinal() - colSub))
				getPager(col).setCurrentItem((Page.UPGRADE.ordinal() - colSub),
						true);
			break;
		case UPGRADE:
			AppUtils.launchMarketPro(home.mApp);
			break;
		case APP_SHARE:
			Intent imgIntent = new Intent(android.content.Intent.ACTION_SEND);
			imgIntent.setType("text/plain");
			imgIntent.putExtra(
					android.content.Intent.EXTRA_TEXT,
					getString(R.string.txt_share, AppUtils.MARKET_URL_PREFIX
							+ home.mApp.mAppInfo._packageName));
			startActivity(Intent.createChooser(imgIntent,
					getString(R.string.txt_share_short)));
			break;
		case APP_RATE:
			AppUtils.launchMarketThisApp(home.mApp);
			break;
		case APP_FEEDBACK:
			AppUtils.launchHelpEmail(home.mApp);
			break;
		case WEBSITE:
			AppUtils.loadPage(home.mApp, Home.WEB_PAGE);
			break;
		case TO_BRAZIL:
			AppUtils.loadPage(home.mApp, Home.URL_BRAZIL);
			break;
		case APP_KSCOPE:
			AppUtils.launchMarket(home, Home.PCKG_KALEIDO);
			break;
		case APP_BGO:
			AppUtils.loadPage(home.mApp, Home.URL_BGO);
			break;
		case APP_JB:
			AppUtils.launchMarket(home, Home.PCKG_JB);
			break;
		case APP_BBALL:
			AppUtils.launchMarket(home, Home.PCKG_BBALL);
			break;
		}
	}

	@Override
	public boolean onBackPressed() {
		if (getPager(0).getCurrentItem() != Page.HOME.ordinal()) {
			// technically not proper use of Column API, but it works because
			// it's all 0's
			getPager(0).setCurrentItem(Page.HOME.ordinal(), true);
			return true;
		}
		return false;
	}

	@Override
	protected void onCreateOptionsMenu(ArrayList<Action> items) {
		if (getHome() != null)
			if (getHome().mApp.isOldVersion())
				items.add(new Action(getString(R.string.btn_update), 0, true,
						false, Actions.UPDATE));
	}

	@Override
	protected void onOptionsItemSelected(Enum<?> e) {
		switch ((Actions) e) {
		case UPDATE:
			AppUtils.launchMarketThisApp(getHome().mApp);
			break;
		}
	}

	@Override
	protected int getAnimation(FragmentAnimation fa, BaseFragment f) {
		return 0;
	}
}
