package hageldave.jplotter.util;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.renderables.Triangles;

import javax.swing.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static java.awt.geom.Rectangle2D.*;

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
	
	public static Rectangle2D scaleRect(Rectangle2D r, double s) {
		r = copy(r);
		r.setRect(
				r.getCenterX()-r.getWidth() *0.5*s, 
				r.getCenterY()-r.getHeight()*0.5*s, 
				r.getWidth()*s, 
				r.getHeight()*s
		);
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
	public static float clamp(float lower, float v, float upper){
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
	
	/**
	 * Creates a horizontal or vertical triangle strip that shows
	 * the colors of the specified {@link ColorMap}.
	 * The produced triangle strip fits inside a unit square [0,1]x[0,1].
	 * @param cmap colormap
	 * @param vertical when true, color gradients are vertical, else horizontal
	 * @return triangle strip depicting the colors of the colormap
	 */
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
	
	/**
	 * Calculates the outcode of a specified point given a specified rectangle.
	 * If the returned outcode is not zero, the point is out of the rectangles bounds
	 * on at least 1 of the boundaries (left, right, top, bottom)
	 * @param x x-coord of point
	 * @param y y-coord of point
	 * @param xmin left bound of rectangle
	 * @param xmax right bound of rectangle
	 * @param ymin top bound of rectangle
	 * @param ymax bottom bound of rectangle
	 * @return outcode nonzero bits indicate boundary |left|right|top|bottom|
	 */
	public static int outcode(double x, double y, double xmin, double xmax, double ymin, double ymax) {
		int out = 0;
		// bit pattern is |left|right|top|bottom|
		out |= x<xmin ? 0b1000:0;
		out |= x>xmax ? 0b0100:0;
		out |= y<ymin ? 0b0010:0;
		out |= y>ymax ? 0b0001:0;
		return out;
	}
	
	/**
	 * Creates an {@link ImageObserver} that checks the for the specified infoflags.
	 * See {@link ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)}.
	 * @param flags infoflags to check 
	 * @return a new ImageObserver
	 */
	public static ImageObserver imageObserver(int flags) {
		return (image, infoflags, x, y, width, height)->(infoflags & flags)!=flags;
	}

	/**
	 * Searches for a specific method in a class (and its superclass &amp; interfaces) using reflections.
	 * It's also possible to search for the variant with the correct parameters (in case that the method is overloaded)
	 *
	 * @param toSearch the class where the method is located
	 * @param methodName name of the method that should be returned
	 * @param params parameters of the method
	 * @return the method, null if the method hasn't been found
	 */
	public static Method searchReflectionMethod(Class<?> toSearch, String methodName, Class<?>... params) {
		if (Objects.nonNull(toSearch)) {
			if (Arrays.stream(toSearch.getDeclaredMethods()).anyMatch(e -> e.getName().equals(methodName))) {
				try {
					return toSearch.getDeclaredMethod(methodName, params);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}

			if (Objects.nonNull(toSearch.getSuperclass())) {
				Method superclassMethod = searchReflectionMethod(toSearch.getSuperclass(), methodName, params);
				if (Objects.nonNull(superclassMethod)) {
					return superclassMethod;
				}
			}

			for (Class<?> interfaceClass : toSearch.getInterfaces()) {
				Method interfaceMethod = searchReflectionMethod(interfaceClass, methodName, params);
				if (Objects.nonNull(interfaceMethod)) {
					return interfaceMethod;
				}
			}
		}
		return null;
	}

	/**
	 * Searches for multiple methods that match the return type and parameters in a class (and its superclass &amp; interfaces) using reflections.
	 *
	 * @param toSearch the class where the method(s) are located
	 * @param returnType return type of the method(s) that should be returned
	 * @param params parameters of the method(s) that should be returned
	 * @return list of methods that match the return type and the parameters
	 */
	public static List<Method> getReflectionMethods(Class<?> toSearch, Class<?> returnType, Class<?>... params) {
		List<Method> toFill = new LinkedList<>();
		if (Objects.nonNull(toSearch)) {
			Method[] sameReturnType = Arrays.stream(toSearch.getDeclaredMethods()).filter(e -> e.getReturnType().equals(returnType)).toArray(Method[]::new);
			Method[] sameReturnParamTypes = Arrays.stream(sameReturnType).filter(e -> Arrays.equals(e.getParameterTypes(), params)).toArray(Method[]::new);
			Collections.addAll(toFill, sameReturnParamTypes);

			if (Objects.nonNull(toSearch.getSuperclass()))
				toFill.addAll(getReflectionMethods(toSearch.getSuperclass(), returnType, params));

			for (Class<?> interfaceClass : toSearch.getInterfaces())
				toFill.addAll(getReflectionMethods(interfaceClass, returnType, params));
		}
		return toFill;
	}

	/**
	 * Returns all methods of a class, its superclass and its interfaces using reflections.
	 *
	 * @param toSearch the class where the methods are located
	 * @return list of methods located in the class, its superclass and its interfaces
	 */
	public static List<Method> getReflectionMethods(Class<?> toSearch) {
		List<Method> toFill = new LinkedList<>();
		if (Objects.nonNull(toSearch)) {
			Method[] sameReturnType = Arrays.stream(toSearch.getDeclaredMethods()).toArray(Method[]::new);
			Method[] sameReturnParamTypes = Arrays.stream(sameReturnType).toArray(Method[]::new);
			Collections.addAll(toFill, sameReturnParamTypes);

			if (Objects.nonNull(toSearch.getSuperclass()))
				toFill.addAll(getReflectionMethods(toSearch.getSuperclass()));

			for (Class<?> interfaceClass : toSearch.getInterfaces())
				toFill.addAll(getReflectionMethods(interfaceClass));
		}
		return toFill;
	}

	/**
	 * Returns all fields of a class, its superclass and its interfaces using reflections.
	 *
	 * @param toSearch the class where the fields are located
	 * @return list of fields located in the class, its superclass and its interfaces
	 */
	public static List<Field> getReflectionFields(Class<?> toSearch) {
		List<Field> toFill = new LinkedList<>();
		if (Objects.nonNull(toSearch)) {
			Collections.addAll(toFill, Arrays.stream(toSearch.getDeclaredFields()).toArray(Field[]::new));

			if (Objects.nonNull(toSearch.getSuperclass()))
				toFill.addAll(getReflectionFields(toSearch.getSuperclass()));

			for (Class<?> interfaceClass : toSearch.getInterfaces())
				toFill.addAll(getReflectionFields(interfaceClass));
		}
		return toFill;
	}

	/**
	 * Clips the given line to the rectangle.
	 * If the line doesn't intersect the rectangle, clipping will be skipped and the given line object will be returned.
	 *
	 * @param rect {@link Rectangle2D} object where the line should be clipped to
	 * @param line {@link Line2D} object to clip
	 * @return the clipped line
	 */
	public static Line2D getClippedLine(Rectangle2D rect, Line2D line) {
		// if line does not intersect the rectangle, clipping can be skipped altogether
		if (!rect.intersectsLine(line))
			return line;

		Point2D intersectionPoint;
		double x1 = line.getX1(), y1 = line.getY1(), x2 = line.getX2(), y2 = line.getY2();

		Line2D.Double unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		Line2D.Double leftBorder = new Line2D.Double(new Point2D.Double(rect.getMinX(), rect.getMinY()), new Point2D.Double(rect.getMinX(), rect.getMaxY()));
		Line2D.Double rightBorder = new Line2D.Double(new Point2D.Double(rect.getMaxX(), rect.getMinY()), new Point2D.Double(rect.getMaxX(), rect.getMaxY()));
		Line2D.Double topBorder = new Line2D.Double(new Point2D.Double(rect.getMinX(), rect.getMinY()), new Point2D.Double(rect.getMaxX(), rect.getMinY()));
		Line2D.Double bottomBorder = new Line2D.Double(new Point2D.Double(rect.getMinX(), rect.getMaxY()), new Point2D.Double(rect.getMaxX(), rect.getMaxY()));

		int outcode = rect.outcode(x1, y1);
		if ((outcode & OUT_TOP)==OUT_TOP) {
			intersectionPoint = Utils.lineIntersection(topBorder, unclippedLine);
			x1 = Objects.requireNonNull(intersectionPoint).getX();
			y1 = Objects.requireNonNull(intersectionPoint).getY();
			unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		}
		if ((outcode & OUT_BOTTOM)==OUT_BOTTOM) {
			intersectionPoint = Utils.lineIntersection(bottomBorder, unclippedLine);
			x1 = Objects.requireNonNull(intersectionPoint).getX();
			y1 = Objects.requireNonNull(intersectionPoint).getY();
			unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		}
		if ((outcode & OUT_LEFT)==OUT_LEFT) {
			intersectionPoint = Utils.lineIntersection(leftBorder, unclippedLine);
			x1 = Objects.requireNonNull(intersectionPoint).getX();
			y1 = Objects.requireNonNull(intersectionPoint).getY();
			unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		}
		if ((outcode & OUT_RIGHT)==OUT_RIGHT) {
			intersectionPoint = Utils.lineIntersection(rightBorder, unclippedLine);
			x1 = Objects.requireNonNull(intersectionPoint).getX();
			y1 = Objects.requireNonNull(intersectionPoint).getY();
			unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		}

		outcode = rect.outcode(x2, y2);
		if ((outcode & OUT_TOP)==OUT_TOP) {
			intersectionPoint = Utils.lineIntersection(topBorder, unclippedLine);
			x2 = Objects.requireNonNull(intersectionPoint).getX();
			y2 = Objects.requireNonNull(intersectionPoint).getY();
			unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		}
		if ((outcode & OUT_BOTTOM)==OUT_BOTTOM) {
			intersectionPoint = Utils.lineIntersection(bottomBorder, unclippedLine);
			x2 = Objects.requireNonNull(intersectionPoint).getX();
			y2 = Objects.requireNonNull(intersectionPoint).getY();
			unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		}
		if ((outcode & OUT_LEFT)==OUT_LEFT) {
			intersectionPoint = Utils.lineIntersection(leftBorder, unclippedLine);
			x2 = Objects.requireNonNull(intersectionPoint).getX();
			y2 = Objects.requireNonNull(intersectionPoint).getY();
			unclippedLine = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
		}
		if ((outcode & OUT_RIGHT)==OUT_RIGHT) {
			intersectionPoint = Utils.lineIntersection(rightBorder, unclippedLine);
			x2 = Objects.requireNonNull(intersectionPoint).getX();
			y2 = Objects.requireNonNull(intersectionPoint).getY();
		}
		return new Line2D.Double(x1, y1, x2, y2);
	}

	/**
	 * Calculates intersection between to {@link Line2D} objects.
	 *
	 * @param a first Line2D object
	 * @param b second Line2D object
	 * @return intersection point of the two lines, returns null if there's no intersection
	 */
	public static Point2D lineIntersection(Line2D a, Line2D b) {
		double x1 = a.getX1(), y1 = a.getY1(), x2 = a.getX2(), y2 = a.getY2(), x3 = b.getX1(), y3 = b.getY1(),
				x4 = b.getX2(), y4 = b.getY2();
		double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		if (d == 0) {
			return null;
		}

		double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
		double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;

		return new Point2D.Double(xi, yi);
	}
}