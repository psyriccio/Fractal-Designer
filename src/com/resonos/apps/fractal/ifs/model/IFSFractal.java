package com.resonos.apps.fractal.ifs.model;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.resonos.apps.fractal.ifs.Home;
import com.resonos.apps.library.App;

/**
 * This class represents an IFS fractal by containing a list of transformations.
 * This class can convert the fractal into the various required formats.
 * @author Chris
 */
public class IFSFractal {

	// serialized data
	private ArrayList<Transformation> mTrans;
	@SuppressWarnings("unused")
	private int _versionID = 1;
	public String _name;
	
	// data
	transient private ArrayList<float[]> mEQ = new ArrayList<float[]>();
	transient private ArrayList<float[]> mAllEQ = new ArrayList<float[]>();
	
	// vars
	transient private float[] triplet = new float[3];
	transient private float[][] rawEQ;
	
	public IFSFractal() {
		mTrans = new ArrayList<Transformation>();
		rawEQ = null;
	}

	/**
	 * Copy constructor
	 * @param trans : another fractal
	 */
	public static IFSFractal copyFromFractal(Home home, IFSFractal f) {
		IFSFractal ifs;
		try {
	    	GsonBuilder gsonBilder = new GsonBuilder();
	    	Gson gson = gsonBilder.create();
	    	ifs = gson.fromJson(f.save(), IFSFractal.class);
	    	ifs.fixOnLoading(home.mApp);
			return ifs;
		} catch (Exception ex) {
			home.mApp.mError.report("CopyIFSFractal", ex, f.save());
			ifs = new IFSFractal();
			return ifs;
		}
	}

	/**
	 * Saves the fractal to a String
	 * @return A String representation of the fractal in IFST format
	 */
	public String save() {
    	GsonBuilder gsonBilder = new GsonBuilder();
    	Gson gson = gsonBilder.create();
        return gson.toJson(this, IFSFractal.class);
	}
	
	/**
	 * Load a fractal
	 * @param data : the saved data in IFST format
	 */
	public static IFSFractal loadFromString(Home home, String data) {
		IFSFractal ifs;
		try {
	    	GsonBuilder gsonBilder = new GsonBuilder();
	    	Gson gson = gsonBilder.create();
	    	ifs = gson.fromJson(data, IFSFractal.class);
	    	ifs.fixOnLoading(home.mApp);
			return ifs;
		} catch (Exception ex) {
			home.mApp.mError.report("LoadIFSFractal", ex, data);
			ifs = new IFSFractal();
			return ifs;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mTrans.size(); i++) {
			Transformation t = mTrans.get(i);
			sb.append(t.toString()).append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * This class represents a calculated IFS fractal. It is an array of septuplets.
	 * Each septuplet represents a transformation numbers called a, b, c, d, e, f, and p
	 */
	public static class RawIFSFractal {
		public float[][] data;
		public RawIFSFractal(IFSFractal f) {
			data = f.createRaw();
		}
		/**
		 * Creates a copy of the data for the user;
		 * @return a copied multidimensional array
		 */
		public float[][] copyData() {
			float[][] ff = new float[data.length][];
			for (int i = 0; i < data.length; i++) {
				float[] f = new float[data[i].length];
				for (int j = 0; j < data[i].length; j++)
					f[j] = data[i][j];
				ff[i] = f;
			}
			return ff;
		}
	}

	/**
	 * Create the raw fractal used for rendering.
	 * @return a {@link RawIFSFractal}
	 */
	public RawIFSFractal calculateRaw() {
		return new RawIFSFractal(this);
	}

	/**
	 * Create the affine transformation functions from all of the transformations in this fractal.
	 * Each transformation is converted into 7 numbers, called a, b, c, d, e, f, and p
	 * Uses raw arrays for improved speed in loops.
	 * @return an array of float arrays representing each abcdefp septuplet
	 */
	private float[][] createRaw() {
		if (rawEQ == null)
			return null;
		mEQ = calculate();
		for (int i = 0; i < mEQ.size(); i++)
			rawEQ[i] = mEQ.get(i);
		return rawEQ;
	}

	/**
	 * Create the affine transformation functions from all of the transformations in this fractal.
	 * Each transformation is converted into 7 numbers, called a, b, c, d, e, f, and p
	 * @return an ArrayList of float arrays representing each abcdefp septuplet
	 */
	public ArrayList<float[]> calculate() {
        float total_area = 0;
        Transformation t;
        for (int i = 0; i < mTrans.size(); i++) {
        	t = mTrans.get(i);
            total_area += t.size.x*t.size.y;
        }

        // create reference triangle (0,0),(0,1),(1,1)
        float r1x = 0, r1y = 0,
        	  r2x = 0, r2y = 1,
        	  r3x = 1, r3y = 1;
        float t1x, t1y, t2x, t2y, t3x, t3y;
        float a, b, c, d, e, f;
        float[] bae, dcf;
        
        mEQ.clear();
        ArrayList<IFSCoord> coords;
        for (int i = 0; i < mTrans.size(); i++) {
        	t = mTrans.get(i);
            
            // create transformation triangle
            coords = t.getCoordsReal(); // real coords, not pixel
            t1x = coords.get(0).x;
            t1y = coords.get(0).y;
            t2x = coords.get(1).x;
            t2y = coords.get(1).y;
            t3x = coords.get(2).x;
            t3y = coords.get(2).y;
            
            // solve equation systems
            bae = solve_eq(r1x, r1y, t1x,  //eq1
                                r2x, r2y, t2x,  //eq2
                                r3x, r3y, t3x);  //eq3
            b = bae[0]; a = bae[1]; e = bae[2];
            dcf = solve_eq(r1x, r1y, t1y,  //eq1
                                r2x, r2y, t2y,  //eq2
                                r3x, r3y, t3y);  //eq3
            d = dcf[0]; c = dcf[1]; f = dcf[2];
            
            float p;
            try {
                p = (t.size.x*t.size.y)/total_area;
            }
            catch (ArithmeticException ex) {
                p = 1.0f;
            }
            mEQ.add(returnSeptuplet(mEQ.size(), a, b, c, d, e, f, p));
        }
        
        return mEQ;
	}
    
	/**
	 * This method exists to allow a function to return seven values without reallocating new memory each time.
	 * Multiple septuplets are stored in memory, indexed by the first parameter
	 * @param i : use the i'th septuplet.
	 * @return A float array containing the seven floats.
	 */
    private float[] returnSeptuplet(int i, float a, float b, float c,
			float d, float e, float f, float g) {
		while (mAllEQ.size() <= i)
			mAllEQ.add(new float[7]);
    	float[] sept = mAllEQ.get(i);
    	sept[0] = a;
    	sept[1] = b;
    	sept[2] = c;
    	sept[3] = d;
    	sept[4] = e;
    	sept[5] = f;
    	sept[6] = g;
		return sept;
	}

    /** Converts IFST data to IFS data by solving a system of equations
     * @return Returns a float triplet representing the three variables solved for. 
     */
	private float[] solve_eq(float x1, float x2, float x1h,
    		float y1, float y2, float y1h, float z1, float z2, float z1h) {
        // Solve a linear system
        // Format for the equations is this:
        // x1*a + x2*b + e = x1h
        // y1*a + y2*b + e = y1h
        // z1*a + z2*b + e = z1h
        
        float det1 = x1 * det(y2,1.0f,z2,1.0f) - x2 * det(y1,1.0f,z1,1.0f)
                    + det(y1,y2,z1,z2);
        if (det1 == 0) // avoid ZeroDivisionError
            return returnTriplet(0,0,0);
        float a = (x1h * det(y2,1.0f,z2,1.0f) - x2 * det(y1h,1.0f,z1h,1.0f)
                    + det(y1h,y2,z1h,z2))/det1;
        float b = (x1 * det(y1h,1.0f,z1h,1.0f) - x1h * det(y1,1.0f,z1,1.0f)
                    + det(y1,y1h,z1,z1h))/det1;
        float e = (x1 * det(y2,y1h,z2,z1h) - x2 * det(y1,y1h,z1,z1h)
                    + x1h*det(y1,y2,z1,z2))/det1;
        
        return returnTriplet(a, b, e);
    }

	/**
	 * This method exists to allow a function to return three values without reallocating new memory each time.
     * Must be used immediately! Values only valid until next function call
	 * @return A float array containing the three floats.
	 */
    private float[] returnTriplet(float i, float j, float k) {
		triplet[0] = i;
		triplet[1] = j;
		triplet[2] = k;
		return triplet;
	}

    /** Calculates the determinant of a 2x2 matrix.
     * @return The determinant. */
	public static float det(float a, float b, float c, float d) {
        return a*d - b*c;
    }

	/**
	 * @return Returns the number of transformations.
	 */
	public int transCount() {
		return mTrans.size();
	}

	/**
	 * Return a transformation.
	 * @param i : the index of the transformation to get
	 * @return the i'th transformation
	 */
	public Transformation getTrans(int i) {
		return mTrans.get(i);
	}

	/**
	 * Add a transformation to this fractal
	 * @param transformation : {@link Transformation}
	 */
	public void addTrans(Transformation transformation) {
		mTrans.add(transformation);
		rawEQ = new float[mTrans.size()][];
	}

	/**
	 * Remove a transformation from this fractal
	 * @param index : the index of the fractal to remove.
	 */
	public void removeTrans(int index) {
		mTrans.remove(index);
		rawEQ = new float[mTrans.size()][];
	}

	/**
	 * Removes all transformations from this fractal.
	 */
	public void clear() {
		mTrans.clear();
	}

	/**
	 * Searches for the transformation's index in the transformation list.
	 * @param t : this input {@link Transformation}
	 * @return the index, if found, or -1 if not fonud.
	 */
	public int getTransformationIndex(Transformation t) {
		return mTrans.indexOf(t);
	}

	/**
	 * This function is to do any corrections necessary after creating objects
	 * from JSON. This includes updating objects in future versions loaded from previous versions.
	 * @param app : the App object
	 */
	public void fixOnLoading(App app) {
		rawEQ = new float[mTrans.size()][];
		for (int i = 0; i < mTrans.size(); i++)
			mTrans.get(i).fixOnLoading(app);
	}
}
