package hageldave.jplotter.util;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.renderables.Triangles;

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
	 * 
	 * @param <T> type of Point2D
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
	 * 
	 * @param <T> type of RectangularShape
	 */
	@SuppressWarnings("unchecked")
	public static <T extends RectangularShape> T copy(T r){
		return (T) r.clone();
	}
	
	public static <T extends Point2D> T translate(T p, double dx, double dy){
		p.setLocation(p.getX()+dx, p.getY()+dy);
		return p;
	}
	
	public static <T extends Rectangle2D> T translate(T r, double dx, double dy){
		r.setRect(r.getX()+dx, r.getY()+dy, r.getWidth(), r.getHeight());
		return r;
	}
	
	/**
	 * Swaps between GL and AWT coordinates, AWT coordinate system
	 * has its origin in the top left corner of a component and downwards pointing
	 * y axis, whereas GL has its origin in the bottom left corner of the viewport
	 * (at least in JPlotter) and upwards pointing y axis.
	 * @param point to swap the y axis of
	 * @param height of the component or viewport
	 * @return point in coordinates of the other reference coordinate system.
	 * 
	 * @param <P> type of Point2D
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
	 *
	 * @param <R> type of Rectangle2D
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
	 * 
	 * @param <T> element type of stream
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
	
	/**
	 * Clamps value between specified bounds
	 * @param lower minimum value
	 * @param v value to clamp
	 * @param upper maximum value
	 * @return max(lower,min(upper,v))
	 */
	public static int clamp(int lower, int v, int upper){
		return Math.max(lower, Math.min(upper, v));
	}
	
	/**
	 * Returns the minimum of 3 values
	 * @param v0 value
	 * @param v1 value
	 * @param v2 value
	 * @return minimum
	 */
	public static double min3(double v0, double v1, double v2){
		return Math.min(Math.min(v0, v1), v2);
	}
	
	/**
	 * Returns the maximum of 3 values
	 * @param v0 value
	 * @param v1 value
	 * @param v2 value
	 * @return maximum
	 */
	public static double max3(double v0, double v1, double v2){
		return Math.max(Math.max(v0, v1), v2);
	}
	
	/**
	 * Calculates the average (arithmetic mean) color of the
	 * specified colors. Each channel (ARGB) is treated separately.
	 * @param argbValues ARGB color values
	 * @return color consisting of ARGB channel means
	 */
	public static int averageColor(int...argbValues){
		int a,r,g,b; a=r=g=b=0;
		for(int argb : argbValues){
			a += Pixel.a(argb);
			r += Pixel.r(argb);
			g += Pixel.g(argb);
			b += Pixel.b(argb);
		}
		int n = argbValues.length;
		return Pixel.argb_fast(a/n, r/n, g/n, b/n);
	}
	
	/**
	 * Checks if specified iterator is sorted according to natural ordering.
	 * @param iter iterator
	 * @return true if sorted, false otherwise
	 * @param <T> element type that implements {@link Comparable}
	 */
	public static <T extends Comparable<T>> boolean isSorted(Iterator<T> iter){
		T prev = iter.next();
		while(iter.hasNext()){
			T next = iter.next();
			if(next.compareTo(prev) < 0){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Calculates sqrt(x*x + y*y) which is the hypotenuse length given catheti x and y.
	 * @param x a value
	 * @param y a value
	 * @return hypotenuse length
	 */
	public static final double hypot(double x, double y){
		return Math.sqrt(x*x + y*y);
	}
	
	/**
	 * tests intersection or containment of rectangle and triangle.
	 * @param rect rectangle to test
	 * @param x0 x coordinate of 0th triangle vertex
	 * @param y0 y coordinate of 0th triangle vertex
	 * @param x1 x coordinate of 1st triangle vertex
	 * @param y1 y coordinate of 1st triangle vertex
	 * @param x2 x coordinate of 2nd triangle vertex
	 * @param y2 y coordinate of 2nd triangle vertex
	 * @return true when intersecting
	 */
	public static boolean rectIntersectsOrIsContainedInTri(
			Rectangle2D rect, 
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2)
	{
		
		return 
				rect.intersectsLine(x0, y0, x1, y1) || 
				rect.intersectsLine(x0, y0, x2, y2) ||
				rect.intersectsLine(x2, y2, x1, y1) ||
				pointInTri(rect.getX(), rect.getY(), x0, y0, x1, y1, x2, y2);
	}
	
	
	private static double sign (
			double x1, double y1, 
			double x2, double y2, 
			double x3, double y3)
	{
	    return (x1 - x3) * (y2 - y3) - (x2 - x3) * (y1 - y3);
	}
	
	private static boolean pointInTri(
			double px, double py, 
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2)
	{
		double d1, d2, d3;
	    boolean has_neg, has_pos;

	    d1 = sign(px,py, x0,y0, x1,y1);
	    d2 = sign(px,py, x1,y1, x2,y2);
	    d3 = sign(px,py, x2,y2, x0,y0);

	    has_neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
	    has_pos = (d1 > 0) || (d2 > 0) || (d3 > 0);

	    return !(has_neg && has_pos);
	}
	
	/**
	 * Creates the union rectangle that encloses all specified rectangles.
	 * @param rects to unite
	 * @return smallest rectangle containing all specified rectangles.
	 * @throws ArrayIndexOutOfBoundsException when no argument was provided
	 */
	public static Rectangle2D mergeRectangles(Rectangle2D ... rects){
		return Arrays.stream(rects).reduce(rects[0], Rectangle2D::createUnion);
	}
	
	public static BufferedImage remoteRGBImage(Img img) {
		DirectColorModel cm = new DirectColorModel(24,
				0x00ff0000,       // Red
                0x0000ff00,       // Green
                0x000000ff,       // Blue
                0x00000000        // Alpha
                );
		DataBufferInt buffer = new DataBufferInt(img.getData(), img.numValues());
		WritableRaster raster = Raster.createPackedRaster(buffer, img.getWidth(), img.getHeight(), img.getWidth(), cm.getMasks(), null);
		BufferedImage bimg = new BufferedImage(cm, raster, false, null);
		return bimg;
	}
	
	public static Triangles colormap2Tris(ColorMap cmap, boolean vertical){
		Triangles tris = new Triangles();
		for(int i=0; i<cmap.numColors()-1;i++){
			int c1 = cmap.getColor(i);
			int c2 = cmap.getColor(i+1);
			double d1 = cmap.getLocation(i);
			double d2 = cmap.getLocation(i+1);
			if(vertical){
				tris.addTriangle(0, d1, 1, d1, 0, d2)
					.setColor0(c1).setColor1(c1).setColor2(c2);
				tris.addTriangle(1, d2, 1, d1, 0, d2)
					.setColor0(c2).setColor1(c1).setColor2(c2);
			} else {
				tris.addTriangle(d1, 0, d1, 1, d2, 0)
					.setColor0(c1).setColor1(c1).setColor2(c2);
				tris.addTriangle(d2, 1, d1, 1, d2, 0)
					.setColor0(c2).setColor1(c1).setColor2(c2);
			}
		}
		return tris;
	}
	
	public static Point2D plus(Point2D a, double m, Point2D b){
		if(a == null){
			return new Point2D.Double(m*b.getX(), m*b.getY());
		}
		return new Point2D.Double(a.getX()+m*b.getX(), a.getY()+m*b.getY());
	}
	
	public static int outcode(double x, double y, double xmin, double xmax, double ymin, double ymax) {
		int out = 0;
		// bit pattern is |left|right|top|bottom
		out |= x<xmin ? 0b1000:0;
		out |= x>xmax ? 0b0100:0;
		out |= y<ymin ? 0b0010:0;
		out |= y>ymax ? 0b0001:0;
		return out;
	}
	
	
}





