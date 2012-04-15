package com.resonos.apps.fractal.ifs.model;

import com.resonos.apps.fractal.ifs.view.EditorView;
import com.resonos.apps.library.model.Coord;
import com.resonos.apps.library.model.ImmutableCoord;
import com.resonos.apps.library.util.M;
import com.resonos.apps.library.util.TouchViewWorker;

/**
 * This class extends the Coord class to add conversion between
 * two coordinate systems: the pixel plane and the "cartesian" plane.
 * Although the cartesian plane is flipped on the y axis, this class
 * does not need to account for that because that is handled
 * by the {@link TouchViewWorker} class.
 * @author Chris
 *
 */
public class IFSCoord extends Coord {
	
	public IFSCoord() {
		super();
	}
	
	public IFSCoord(float x, float y) {
		super(x, y);
	}
	
	public IFSCoord(IFSCoord c) {
		super(c);
	}

    @Override
	public IFSCoord set(float x, float y) {
		return (IFSCoord)super.set(x, y);
	}

    @Override
	public IFSCoord set(ImmutableCoord c) {
		return (IFSCoord)super.set(c);
	}
	
    /**
     * Converts a cartesian coordinate to a window coordinate.
     * @param size : the square size, in pixels, of the editor view. This is the minimum between height and width. 
     * @return The same Coord, available for chaining
     */
    public IFSCoord toPixelPlane(float size) {
    	float box_size = size / EditorView.IFS_VISIBLE_SPACE;
        float margin = (size - size / EditorView.IFS_VISIBLE_SPACE) / 2f;
        return set(this.x*box_size + margin, this.y*box_size + margin);
    }

    /**
     * Converts a window coordinate to a cartesian coordinate.
     * @param size : the square size, in pixels, of the editor view. This is the minimum between height and width. 
     * @return The same Coord, available for chaining
     */
    public IFSCoord toRealPlane(float size) {
    	float box_size = size / EditorView.IFS_VISIBLE_SPACE;
        float margin = (size - size / EditorView.IFS_VISIBLE_SPACE) / 2f;
        return set((this.x-margin)/box_size, (this.y-margin)/box_size);
    }

    @Override
	public String toString() {
		return "("+M.printFloat(x, 1)+","+M.printFloat(y, 1)+")";
	}
}
