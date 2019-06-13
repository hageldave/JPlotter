package hageldave.jplotter.renderers;

import java.awt.geom.Rectangle2D;

/**
 * The AdaptableView interface defines the {@link #setView(Rectangle2D)}
 * method.
 * An implementing class of this interface is meant to be able to adjust its
 * view based on the view rectangle passed to the method.
 * This is implemented for example by {@link GenericRenderer}.
 * 
 * @author hageldave
 */
public interface AdaptableView {

	/**
	 * Sets the view rectangle that is the range of coordinates to be
	 * projected onto the view port.
	 * <p>
	 * For example when setting a view rectangle of (x=-1,y=-1,w=2,h=2)
	 * given a viewport of size (w=100,h=100), then a point with coordinates
	 * (x=0,y=0) will be projected to (x=50,y=50) on the viewport.
	 * A point with coordinates (x=-1,y=-1) will be projected to (x=0,y=0).
	 * <p>
	 * When setting the view rectangle to null, then no projection is happening
	 * and the coordinates are mapped directly to view port coordinates.
	 * E.g. coordinates (40,40) will be (40,40) on the viewport, as if the
	 * view rectangles size was coupled to the viewport size.
	 * 
	 * @param rect the view rectangle (can be null)
	 */
	public void setView(Rectangle2D view);
	
}
