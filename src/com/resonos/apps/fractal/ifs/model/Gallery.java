package com.resonos.apps.fractal.ifs.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.resonos.apps.fractal.ifs.Home;
import com.resonos.apps.fractal.ifs.util.IFSFile;
import com.resonos.apps.library.App;

/**
 * This class represents data to be loaded and saved by the class.
 * It is generally used in two forms, a sample gallery (which loads from assets),
 *  and the user gallery (which loads from external storage).
 * Most of the work is done in static functions.
 * The saved data includes both color schemes and fractals.
 * @author Chris
 *
 */
public class Gallery {
	
	// saved data
	@SuppressWarnings("unused")
	private int _versionID = 1;
	public ArrayList<ColorScheme> mColorSchemes;
	public ArrayList<IFSFractal> mFractals;
	
	/** Assets directory for samples */
	public static final String DIR_ASSETS_SAMPLE = "samples/";
	
	/** External storage directory for media */
	public static final String DIR_EXTERNAL_SHARED = "DCIM/FractalDesigner/";

	public static final String USER_GALLERY_FILE = "gallery.dat";
	public static final String SAMPLE_GALLERY_FILE = "gallery.dat";
	
	public Gallery() {
		mColorSchemes = new ArrayList<ColorScheme>();
		mFractals = new ArrayList<IFSFractal>();
	}

	/**
	 * In order to create a unified gallery, add another gallery to this one.
	 * @param gal
	 */
	public void add(Gallery gal) {
		mColorSchemes.addAll(gal.mColorSchemes);
		mFractals.addAll(gal.mFractals);
	}

	public static boolean isExternalStorageAvailable() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}

	/**
	 * Save this gallery to external storage, toasting if asked.
	 * @param app : the App object
	 */
	public boolean save(Home home) {
		return IFSFile.writePrivateFile(home, USER_GALLERY_FILE, saveToString());
	}
	
	/**
	 * Returns the fractal matching the requested name.
	 * @param name : the String name
	 * @return the matching IFSFractal, or null if none found
	 */
	public IFSFractal getFractalByName(String name) {
		for (int i = 0; i < mFractals.size(); i++)
			if (mFractals.get(i)._name.equals(name))
				return mFractals.get(i);
		return null;
	}
	
	/**
	 * Returns the color scheme matching the requested name.
	 * @param name : the String name
	 * @return the matching ColorScheme, or null if none found
	 */
	public ColorScheme getColorSchemeByName(String name) {
		for (int i = 0; i < mColorSchemes.size(); i++)
			if (mColorSchemes.get(i)._name.equals(name))
				return mColorSchemes.get(i);
		return null;
	}

	/**
	 * Save the contents of this gallery to a JSON string.
	 * @return JSON data
	 */
	public String saveToString() {
    	GsonBuilder gsonBilder = new GsonBuilder();
    	Gson gson = gsonBilder.create();
        return gson.toJson(this, Gallery.class);
	}

	/**
	 * Create a gallery from a JSON string
	 * @param home : the App object
	 * @param s : the JSON string
	 * @return : a new Gallery object
	 */
	public static Gallery loadGalleryFromString(Home home, String s) {
		try {
	    	GsonBuilder gsonBilder = new GsonBuilder();
	    	Gson gson = gsonBilder.create();
	    	Gallery g = gson.fromJson(s, Gallery.class);
			g.fixOnLoading(home.mApp);
			return g;
		} catch (Exception ex) {
			home.mApp.mError.report("loadGalleryFromString", ex, s);
			Gallery g = new Gallery();
			return g;
		}
	}

	/**
	 * Load the user internal storage Gallery
	 * @param app : the App object
	 * @return : a new Gallery object
	 */
	public static Gallery loadGallery(Home home) {
		String data = IFSFile.readPrivateFile(home, USER_GALLERY_FILE);
		if (data != null) {
	    	return loadGalleryFromString(home, data);
		} else {
			Gallery g = new Gallery();
			g.save(home);
			return g;
		}
	}

	/**
	 * Load the sample Gallery from assets
	 * @param app : the App object
	 * @return : a new Gallery object
	 */
	public static Gallery loadSampleGallery(Home home) {
		InputStream is = null;
		Gallery g = null;
		try {
			is = home.getAssets().open(Gallery.SAMPLE_GALLERY_FILE);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			String data = new String(buffer);
			g = loadGalleryFromString(home, data);
		} catch (IOException e) {
			home.mApp.mError.report("LoadSampleGallery", e);
		} finally {
			try {
				is.close(); // stop reading
			} catch (IOException ex) {
				home.mApp.mError.report("LoadSampleGallery", ex);
			}
		}
		return g;
	}

	/**
	 * This function is to do any corrections necessary after creating objects
	 * from JSON. This includes updating objects in future versions loaded from previous versions.
	 * @param app : the App object
	 */
	private void fixOnLoading(App app) {
		for (int i = 0; i < mColorSchemes.size(); i++)
			mColorSchemes.get(i).fixOnLoading(app);
		for (int i = 0; i < mFractals.size(); i++)
			mFractals.get(i).fixOnLoading(app);
	}

	public boolean hasColorScheme(ColorScheme cm) {
		for (int i = 0; i < mColorSchemes.size(); i++)
			if (mColorSchemes.get(i)._name.equals(cm._name))
				return true;
		return false;
	}
}
