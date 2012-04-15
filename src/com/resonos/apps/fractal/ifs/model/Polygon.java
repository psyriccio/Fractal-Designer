package com.resonos.apps.fractal.ifs.model;

/**
 * Minimum Polygon class for Android.
 * From http://alienryderflex.com/polygon/
 */
public class Polygon {
    /**
     * Checks if the Polygon contains a point.
     * @see "http://alienryderflex.com/polygon/"
     * @param px Polygon y coords.
     * @param py Polygon x coords.
     * @param ps Polygon sides count.
     * @param x Point horizontal pos.
     * @param y Point vertical pos.
     * @return Point is in Poly flag.
     */
	public static boolean contains(float[] polyX, float[] polyY, int polySides,
			float x, float y) {
		boolean oddTransitions = false;
		for (int i = 0, j = polySides - 1; i < polySides; j = i++) {
			if ((polyY[i] < y && polyY[j] >= y)
					|| (polyY[j] < y && polyY[i] >= y)) {
				if (polyX[i] + (y - polyY[i]) / (polyY[j] - polyY[i])
						* (polyX[j] - polyX[i]) < x) {
					oddTransitions = !oddTransitions;
				}
			}
		}
		return oddTransitions;
	}   
}