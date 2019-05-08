package jplotter.util;

import java.awt.geom.Point2D;

public class TranslatedPoint2D extends Point2D {
	
	final double xt,yt;
	Point2D origin;
	
	public TranslatedPoint2D(Point2D origin, double xt, double yt) {
		this.xt = xt;
		this.yt = yt;
		this.origin = origin;
	}

	@Override
	public double getX() {
		return origin.getX()+xt;
	}

	@Override
	public double getY() {
		return origin.getY()+yt;
	}

	@Override
	public void setLocation(double x, double y) {
		origin.setLocation(x-xt, y-yt);
	}

}
