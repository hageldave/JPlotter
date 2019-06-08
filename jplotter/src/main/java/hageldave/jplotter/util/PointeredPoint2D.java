package hageldave.jplotter.util;

import java.awt.geom.Point2D;
import java.util.Locale;

/**
 * The PointeredPoint2D class is an implementation of the {@link Point2D}
 * class that uses two double arrays of size 1 to store its x and y coordinate.
 * This allows for referencing the x or y coordinate elsewhere 
 * (kind of as a pointer to the value) e.g. in another PointeredPoint2D
 * so that 2 points can share a coordinate for example points on the same axis.
 * 
 * @author hageldave
 */
public class PointeredPoint2D extends Point2D {
	
	/** 'pointer' to x coordinate */
	public final double[] x;
	/** 'pointer' to y coordinate */
	public final double[] y;
	
	/**
	 * Creates point at (0,0)
	 */
	public PointeredPoint2D() {
		x = new double[1];
		y = new double[1];
	}
	
	/**
	 * Creates point at (x,y)
	 * @param x coordinate
	 * @param y coordinate
	 */
	public PointeredPoint2D(double x, double y) {
		this();
		setLocation(x, y);
	}
	
	/**
	 * Creates point using the specified arrays as pointers for this point's coordinates
	 * @param xptr array to use as pointer to x coordinate
	 * @param yptr array to use as pointer to y coordinate
	 */
	public PointeredPoint2D(double[] xptr, double[] yptr){
		x = xptr;
		y = yptr;
	}

	@Override
	public double getX() {
		return x[0];
	}

	@Override
	public double getY() {
		return y[0];
	}

	@Override
	public void setLocation(double x, double y) {
		this.x[0] = x;
		this.y[0] = y;
	}
	
	@Override
	public Object clone() {
		return new PointeredPoint2D(getX(), getY());
	}
	
	@Override
	public String toString() {
		return String.format(Locale.US, "%s[%f, %f]", getClass().getSimpleName(), getX(),getY());
	}

}
