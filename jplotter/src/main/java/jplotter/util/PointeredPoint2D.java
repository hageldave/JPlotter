package jplotter.util;

import java.awt.geom.Point2D;

public class PointeredPoint2D extends Point2D {
	
	public final double[] x;
	public final double[] y;
	
	public PointeredPoint2D() {
		x = new double[1];
		y = new double[1];
	}
	
	public PointeredPoint2D(double x, double y) {
		this();
		setLocation(x, y);
	}
	
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

}
