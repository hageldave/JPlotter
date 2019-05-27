package hageldave.jplotter.util;

import java.awt.geom.Point2D;

/**
 * The TranslatedPoint2D is an implementation of {@link Point2D}
 * that references another Point2D and has a certain fixed
 * translation from that point.
 * When changing the location of the referenced point, this
 * point will change accordingly.
 * 
 * @author hageldave
 */
public class TranslatedPoint2D extends Point2D {
	
	public final double tx,ty;
	public final Point2D origin;
	
	/**
	 * Creates a new {@link TranslatedPoint2D} from the specified
	 * point with specified translation.
	 * @param origin the point from which the created point is translated
	 * @param xt translation in x direction
	 * @param yt translation in y direction
	 */
	public TranslatedPoint2D(Point2D origin, double xt, double yt) {
		this.tx = xt;
		this.ty = yt;
		this.origin = origin;
	}

	@Override
	public double getX() {
		return origin.getX()+tx;
	}

	@Override
	public double getY() {
		return origin.getY()+ty;
	}

	/**
	 * Sets the location of this point's {@link #origin} so that
	 * this point ends up at the desired location.
	 */
	@Override
	public void setLocation(double x, double y) {
		origin.setLocation(x-tx, y-ty);
	}

}
