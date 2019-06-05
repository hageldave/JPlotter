package hageldave.jplotter.util;

import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;

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
	
}
