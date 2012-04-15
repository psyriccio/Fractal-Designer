package com.resonos.apps.fractal.ifs.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;

import com.WazaBe.HoloEverywhere.HoloAlertDialogBuilder;
import com.resonos.apps.fractal.ifs.Home;
import com.resonos.apps.fractal.ifs.R;

/**
 * A grouping of constants and functions related to IFS files
 * @author Chris
 */
public class IFSFile {
	
	/** External storage directory for saved files */
	public static final String DIR_EXTERNAL_FILES = "Android/data/com.resonos.apps.fractal.ifs/files/";
	
	/** Assets directory for samples */
	public static final String DIR_ASSETS_SAMPLE = "samples/";
	
	/** External storage directory for media */
	public static final String DIR_EXTERNAL_SHARED = "DCIM/FractalDesigner/";
	
	/** the sierpinski triangle in abcdefp format */
	public static final String IFS_TRI = "  .5  0  0  .5	0  0  .33\n  .5  0  0  .5	0  1  .33\n  .5  0  0  .5	1  1  .34";
	
	/** a swirl in abcdefp format */
	public static final String IFS_SWIRL = "   .787879 -.424242 .242424 .859848  1.758647 1.408065 .895652\n  -.121212  .257576 .151515 .053030 -6.721654 1.377236 .052174\n   .181818 -.136364 .090909 .181818  6.086107 1.568035 .052174";

	/** the built-in categories raw names */
	public static final String[] IFS_CAT_RAW_NAMES = new String[] { "Spiral", "Square",
		"Triangular", "Organic", "Polygonal", "Other", "Saved" };

	/** the built-in categories resource names */
	public static final int[] IFS_CAT_RES_NAMES = new int[] {
		R.string.txt_cat_def01,
		R.string.txt_cat_def02,
		R.string.txt_cat_def03,
		R.string.txt_cat_def04,
		R.string.txt_cat_def05,
		R.string.txt_cat_def06,
		R.string.txt_cat_saved
	};
	
	/** saved IFSs are named as "cat:name" */
	public static final char FILENAME_SEPARATOR = ':';
	
	/** the cat index that user fractals go in */
	public static final int IFS_CAT_INDEX_SAVED = 6;

	/**
	 * Reads a saved file, and if it doesn't exist, creates it from a resource
	 * @param home : the main activity
	 * @param fileName : the file name
	 * @return The file to be read, or the raw resource if it does not exist, or null on error
	 */
	public static String readPrivateFile(Home home, String fileName) {
		String data = null;
		FileInputStream fis;
		try {
			fis = home.openFileInput(fileName);
			int size = fis.available();
			byte[] buffer = new byte[size];
			fis.read(buffer);
			fis.close();
			data = new String(buffer);
		}
		catch (IOException ex) {
			return null;
		}
		return data;
	}

	/**
	 * Writes to a saved private file
	 * @param home : the main activity
	 * @param fileName : the file name
	 * @param data : the data to write
	 * @return true if successful
	 */
	public static boolean writePrivateFile(Home home, String file, String data) {
		try {
			FileOutputStream fos = home.openFileOutput(file, Context.MODE_PRIVATE);
			fos.write(data.getBytes());
			fos.close();
		}
		catch (IOException exc) {
			return false;
		}
		return true;
	}

	/**
	 * Turns a saved IFS fractal into a multidimensional array ready to be rendered
	 * @param cx : context
	 * @param IFS : the raw IFS data (this is not pos/size/rotate/skew data, it is calculated abcdefp data)
	 * @return an ArrayList<float[]> containing the decoded data
	 */
	public static ArrayList<float[]> decodeIFS(Context cx, String IFS) {
		String[] lines = IFS.split("[\\r\\n]+");
		ArrayList<float[]> trans = new ArrayList<float[]>();
		boolean error = false;
		for (String line : lines) {
			String[] comments = line.split("\\;");
			line = comments[0];
			if (line.trim().length() <= 1)
				continue;
			String[] words = line.trim().split("\\s+");
			if (words.length == 7) {
				float[] newt = { 0f, 0f, 0f, 0f, 0f, 0f, 0f };
				try {
					for (int i = 0; i < 7; i++) {
						newt[i] = Float.parseFloat(words[i]);
					}
				} catch (NumberFormatException ex) {
					error = true;
				}
				trans.add(newt);
			}
		}
		if (error)
			new HoloAlertDialogBuilder(cx)
					.setTitle(" Error")
					.setMessage(
							"Error parsing IFS data.  The rendered fractal may "
									+ "not appear as expected.")
					.setPositiveButton("Ok", null).show();
		return trans;
	}

	/**
	 * Returns a human-readable category name.
	 * @param cx : app context
	 * @param s : category index
	 * @return The localized category name.
	 */
	public static String getCategoryName(Context cx, int s) {
		return cx.getString(IFS_CAT_RES_NAMES[s]);
	}

	/**
	 * Returns a raw category name for filenaming purposes
	 * @param s : category index
	 * @return the raw name
	 */
	public static String getCategoryRawName(int s) {
		return IFS_CAT_RAW_NAMES[s];
	}

	/**
	 * Generates a name in cat:name format for saving IFST files
	 * @param cat : category index
	 * @param title : IFS name
	 * @return
	 */
	public static String generateIFSFileName(int cat, String title) {
		return getCategoryRawName(cat) + FILENAME_SEPARATOR + title;
	}

	/**
	 * Extracts the category index from names in cat:name format
	 * @param fileName : the saved name
	 * @return the category index, or -1 if not found
	 */
	public static int getCatFromFileName(String fileName) {
		int sep = fileName.indexOf(FILENAME_SEPARATOR);
		if (sep > 0) {
			String cat = fileName.substring(0, sep);
			for (int i = 0; i < IFS_CAT_RAW_NAMES.length; i++) {
				if (cat.equals(IFS_CAT_RAW_NAMES[i]))
					return i;
			}
		}
		return -1;
	}

	/**
	 * Extracts the name part from names in cat:name format.
	 * @param fileName : the full name
	 * @return anything after the first : symbol.
	 */
	public static String getNameFromFileName(String fileName) {
		int sep = fileName.indexOf(FILENAME_SEPARATOR);
		if (sep >= 0)
			return fileName.substring(sep + 1);
		else
			return fileName;
	}
}
