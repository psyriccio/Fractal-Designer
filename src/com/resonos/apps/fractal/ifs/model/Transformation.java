	

package com.resonos.apps.fractal.ifs.model;

import java.util.ArrayList;

import com.resonos.apps.library.App;

/**
 * This class represents a single transformation component of an IFS fractal.
 * It is represented via position, size, rotation, and skewing, and its
 * actual coordinates must be calculated.
 * @author Chris
 */
public class Transformation {
	
	transient private IFSCoord bl = new IFSCoord(), br = new IFSCoord(),
				tl = new IFSCoord(), tr = new IFSCoord();
	transient private ArrayList<IFSCoord> coords = new ArrayList<IFSCoord>();
	transient private IFSCoord centerPoint = new IFSCoord();
	
	// saved data
	@SuppressWarnings("unused")
	private int _versionID = 1;
	public IFSCoord pos;
	public IFSCoord size;
	public IFSCoord skew;
	public float angle;
	
	/**
	 * Create a new transformation in the center of the editor
	 */
	public Transformation() {
		pos = new IFSCoord(0.25f, 0.25f);
		size = new IFSCoord(0.5f, 0.5f);
		skew = new IFSCoord(0f, 0f);
		angle = 0f;
	}
	
	@Override
	public String toString() {
		return pos.toString()+","+size.toString()+","+angle+","+skew.toString();
	}
	
	/**
	 * Create a new transformation with the following parameters
	 * @param pos
	 * @param size
	 * @param angle
	 * @param skew
	 */
	public Transformation(IFSCoord pos, IFSCoord size, float angle, IFSCoord skew) {
		this.pos = new IFSCoord(pos);
		this.size = new IFSCoord(size);
		this.skew = new IFSCoord(skew);
		this.angle = angle;
	}

	/**
	 * Calculate the coordinates of this transformation, scaling to the pixel plane
	 * @param scale : the size of the smaller dimension of the editor view
	 * @return an array of coordinates representing the box the transformation fits in.
	 */
	public ArrayList<IFSCoord> getCoordsView(float scale) {
		ArrayList<IFSCoord> a = getCoordsReal();
    	bl.toPixelPlane(scale);
    	br.toPixelPlane(scale);
    	tl.toPixelPlane(scale);
    	tr.toPixelPlane(scale);
    	return a;
	}

	/**
	 * Calculate the coordinates of this transformation, performing no scaling
	 * @return an array of coordinates representing the box the transformation fits in.
	 */
	public ArrayList<IFSCoord> getCoordsReal() {
        bl.set(pos.x, pos.y);
        br.set(pos.x + size.x, pos.y + skew.y*size.y);
        tl.set(pos.x + skew.x*size.x, pos.y + size.y);
        tr.set(pos.x + size.x + skew.x*size.x, pos.y + size.y + skew.y*size.y);

        br.rotateAround(bl, angle);
        tl.rotateAround(bl, angle);
        tr.rotateAround(bl, angle);

        coords.clear();
        coords.add(bl);
        coords.add(br);
        coords.add(tr);
        coords.add(tl);
        return coords;
	}
	
	/**
	 * Calculates the center of the transformation
	 * @param real : true for real coordinates, false for pixel plane
	 * @return the center coordinate
	 */
	public IFSCoord getCenterPoint(boolean real) {
		if (real) {
			if (coords.size() == 0)
				getCoordsReal();
			centerPoint.set((coords.get(2).x + coords.get(0).x)/2.0f,
					(coords.get(2).y + coords.get(0).y)/2.0f);
		}
		else
			centerPoint.set(pos.x+(size.x + skew.x*size.x)/2,
					pos.y+(size.y + skew.y*size.y)/2);
		return centerPoint;
	}

	/**
	 * This function is to do any corrections necessary after creating objects
	 * from JSON. This includes updating objects in future versions loaded from previous versions.
	 * @param app : the App object
	 */
	public void fixOnLoading(App app) {
		//
	}
}
