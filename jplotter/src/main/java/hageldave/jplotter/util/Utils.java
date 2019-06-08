package hageldave.jplotter.util;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import hageldave.imagingkit.core.Pixel;

/**
 * Class containing utility methods
 * 
 * @author hageldave
 */
public class Utils {

	/**
	 * Executes the specified runnable on the AWT event dispatch thread.
	 * If called from the AWT event dispatch thread it is executed right 
	 * away, otherwise {@link SwingUtilities#invokeAndWait(Runnable)} is
	 * called.
	 * 
	 * @param runnable to be executed.
	 */
	public static void execOnAWTEventDispatch(Runnable runnable){
		if(SwingUtilities.isEventDispatchThread()){
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (InvocationTargetException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Copies the specified {@link Point2D} (calls clone) and
	 * casts the copy to the class of the original.
	 * @param p point to copy
	 * @return the copied point
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Point2D> T copy(T p){
		return (T) p.clone();
	}
	
	/**
	 * Copies the specified {@link RectangularShape} (calls clone) and
	 * casts the copy to the class of the original.
	 * @param r rectangle to copy
	 * @return the copied rectangle
	 */
	@SuppressWarnings("unchecked")
	public static <T extends RectangularShape> T copy(T r){
		return (T) r.clone();
	}
	
	/**
	 * Linearly interpolates between the two specified colors.<br>
	 * c = c1*(1-m) + c2*m
	 * @param c1 integer packed ARGB color value 
	 * @param c2 integer packed ARGB color value, e.g. 0xff00ff00 for opaque green
	 * @param m in [0,1]
	 * @return interpolated color
	 */
	public static int interpolateColor(int c1, int c2, double m){
		double r1 = Pixel.r_normalized(c1);
		double g1 = Pixel.g_normalized(c1);
		double b1 = Pixel.b_normalized(c1);
		double a1 = Pixel.a_normalized(c1);
		
		double r2 = Pixel.r_normalized(c2);
		double g2 = Pixel.g_normalized(c2);
		double b2 = Pixel.b_normalized(c2);
		double a2 = Pixel.a_normalized(c2);
		
		return Pixel.argb_fromNormalized(
				a1*(1-m)+a2*m,
				r1*(1-m)+r2*m,
				g1*(1-m)+g2*m,
				b1*(1-m)+b2*m
		);
	}
	
	/**
	 * Swaps between GL and AWT coordinates, AWT coordinate system
	 * has its origin in the top left corner of a component and downwards pointing
	 * y axis, whereas GL has its origin in the bottom left corner of the viewport
	 * (at least in JPlotter) and upwards pointing y axis.
	 * @param point to swap the y axis of
	 * @param height of the component or viewport
	 * @return point in coordinates of the other reference coordinate system.
	 */
	public static <P extends Point2D> P swapYAxis(P point, int height){
		P copy = copy(point);
		copy.setLocation(copy.getX(), height-1-copy.getY());
		return copy;
	}
	
	/**
	 * Swaps between GL and AWT coordinates, AWT coordinate system
	 * has its origin in the top left corner of a component and downwards pointing
	 * y axis, whereas GL has its origin in the bottom left corner of the viewport
	 * (at least in JPlotter) and upwards pointing y axis.
	 * @param rect rectangle to swap the y axis of
	 * @param height of the component or viewport
	 * @return rectangle in coordinates of the other reference coordinate system.
	 */
	public static <R extends Rectangle2D> R swapYAxis(R rect, int height){
		R copy = copy(rect);
		copy.setRect(copy.getX(), height-1-copy.getMaxY(), copy.getWidth(), copy.getHeight());
		return copy;
	}
	
	/**
	 * Syntactic sugar for conditional stream.parallel().
	 * @param stream to make parallel
	 * @param parallel whether to make parallel or not
	 * @return stream.parallel() if true
	 */
	public static <T> Stream<T> parallelize(Stream<T> stream, boolean parallel){
		return parallel ? stream.parallel():stream;
	}
	
	/**
	 * Clamps value between specified bounds
	 * @param lower minimum value
	 * @param v value to clamp
	 * @param upper maximum value
	 * @return max(lower,min(upper,v))
	 */
	public static double clamp(double lower, double v, double upper){
		return Math.max(lower, Math.min(upper, v));
	}
	
}
