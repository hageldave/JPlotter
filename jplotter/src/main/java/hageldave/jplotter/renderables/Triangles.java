package hageldave.jplotter.renderables;

import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * The Triangles class is a collection of 2D triangles.
 * A single triangle consists of three 2D points where each of these points
 * can be colored differently which leads to the triangle area being colored
 * by interpolating using barycentric coordinates.
 * Also a triangle can have a single picking color, which is the color with 
 * which the triangle is rendered into the (invisible) picking color attachment
 * of an {@link FBO}. 
 * This color may serve as an identifier of the object that can be queried from 
 * a location of the rendering canvas. 
 * It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values) or 0.
 * <p>
 * There is also a global alpha multiplier parameter which scales every triangle's color alpha value,
 * which can be used to introduce transparency for all triangles of this collection.
 * This may come in handy to let other rendered content under a triangle 'shine through'.
 * 
 * @author hageldave
 */
public class Triangles implements Renderable {

	protected VertexArray va;
	protected boolean isDirty = true;
	protected DoubleSupplier globalAlphaMultiplier = ()->1.0;
	protected DoubleSupplier globalSaturationMultiplier = () -> 1.0;
	protected ArrayList<TriangleDetails> triangles = new ArrayList<>();
	protected boolean useCrispEdgesForSVG = true;
	protected boolean useAAinFallback = false;
	protected boolean hidden=false;
	protected boolean isGLDoublePrecision = false;
	
	/**
	 * @return the number of triangles in this collection.
	 */
	public int numTriangles() {
		return triangles.size();
	}
	
	/**
	 * Adds a triangle to this collection.
	 * Sets the {@link #isDirty()} state to true.
	 * @param x0 x coordinate of the first triangle vertex
	 * @param y0 y coordinate of the first triangle vertex
	 * @param x1 x coordinate of the second triangle vertex
	 * @param y1 y coordinate of the second triangle vertex
	 * @param x2 x coordinate of the third triangle vertex
	 * @param y2 y coordinate of the third triangle vertex
	 * @return added triangles
	 */
	public TriangleDetails addTriangle(
			double x0, double y0,
			double x1, double y1, 
			double x2, double y2
	){
		TriangleDetails tri = new TriangleDetails(x0, y0, x1, y1, x2, y2);
		this.triangles.add(tri);
		setDirty();
		return tri;
	}
	
	/**
	 * Adds a triangle to this collection.
	 * Sets the {@link #isDirty()} state to true.
	 * @param p0 first vertex of the triangle
	 * @param p1 second vertex of the triangle
	 * @param p2 third vertex of the triangle
	 * @return added triangle
	 */
	public TriangleDetails addTriangle(Point2D p0, Point2D p1, Point2D p2){
		TriangleDetails tri = new TriangleDetails(p0,p1,p2);
		this.triangles.add(tri);
		setDirty();
		return tri;
	}
	
	/**
	 * Adds two triangles that form the specified quad.
	 * Sets the {@link #isDirty()} state to true.
	 * @param xBL bottom left x
	 * @param yBL bottom left y
	 * @param xTL top left x
	 * @param yTL top left y
	 * @param xTR top right x
	 * @param yTR top right y
	 * @param xBR bottom right x
	 * @param yBR bottom right y
	 * @return added triangles
	 */
	public ArrayList<TriangleDetails> addQuad(
			double xBL, double yBL,
			double xTL, double yTL,
			double xTR, double yTR,
			double xBR, double yBR
	){
		ArrayList<TriangleDetails> tris = new ArrayList<>(2);
		tris.add( this.addTriangle(xBL, yBL, xTL, yTL, xTR, yTR) );
		tris.add( this.addTriangle(xBL, yBL, xTR, yTR, xBR, yBR) );
		return tris;
	}
	
	/**
	 * Adds two triangles that form the specified quad.
	 * Sets the {@link #isDirty()} state to true.
	 * @param bl bottom left vertex
	 * @param tl top left vertex
	 * @param tr top right vertex
	 * @param br bottom right vertex
	 * @return added triangles
	 */
	public ArrayList<TriangleDetails> addQuad(Point2D bl, Point2D tl, Point2D tr, Point2D br){
		ArrayList<TriangleDetails> tris = new ArrayList<>(2);
		tris.add( this.addTriangle(bl, tl, tr) );
		tris.add( this.addTriangle(bl, tr, br) );
		return tris;
	}
	
	/**
	 * Adds two triangles that form the specified rectangle.
	 * Sets the {@link #isDirty()} state to true.
	 * @param rect rectangle
	 * @return added triangles
	 */
	public ArrayList<TriangleDetails> addQuad(Rectangle2D rect){
		return this.addQuad(
				rect.getMinX(), rect.getMinY(), 
				rect.getMinX(), rect.getMaxY(), 
				rect.getMaxX(), rect.getMaxY(), 
				rect.getMaxX(), rect.getMinY());
	}
	
	
	/**
	 * Adds a series of triangles called a triangle strip to {@link Triangles} object.
	 * Each vertex forms a new triangle together with the two preceding vertices.
	 * Make sure that the first vertex is the one that is not shared with the second triangle in the strip.
	 * Sets the {@link #isDirty()} state to true.
	 * @param points the vertices (at least 3)
	 * @return added triangles
	 * @throws IllegalArgumentException when less than 3 vertices were specified
	 */
	public ArrayList<TriangleDetails> addStrip(Point2D ... points){
		if(points.length < 3){
			throw new IllegalArgumentException("not enough points for triangle strip, need at least 3 but got " + points.length);
		}
		ArrayList<TriangleDetails> tris = new ArrayList<>(points.length-2);
		for(int i = 0; i < points.length-2; i++){
			TriangleDetails tri = addTriangle(points[i], points[i+1], points[i+2]);
			tris.add(tri);
		}
		return tris;
	}
	
	/**
	 * Adds a series of triangles called a triangle strip to {@link Triangles} object.
	 * Each vertex forms a new triangle together with the two preceding vertices.
	 * Make sure that the first vertex is the one that is not shared with the second triangle in the strip.
	 * Sets the {@link #isDirty()} state to true.
	 * @param coords the coordinates of the triangle vertices, a series of (x,y) pairs.
	 * Need at least 6 coordinates (3 pairs) and has to be an even number of coordinates.
	 * @return this for chaining
	 * @throws IllegalArgumentException when less than 3 vertices were specified or an odd number of coordinates
	 * was specified.
	 */
	public ArrayList<TriangleDetails> addStrip(double... coords){
		if(coords.length < 6){
			throw new IllegalArgumentException("not enough coordinates for triangle strip, need at least 6 ( 3x {x,y} ) but got " + coords.length);
		}
		if(coords.length % 2 != 0){
			throw new IllegalArgumentException("need an even number of coordinates for triangle strip ({x,y} pairs), but got " + coords.length);
		}
		ArrayList<TriangleDetails> tris = new ArrayList<>(coords.length/2-2);
		for(int i = 0; i < coords.length/2-2; i++){
			TriangleDetails tri = addTriangle(coords[i*2+0], coords[i*2+1], coords[i*2+2], coords[i*2+3], coords[i*2+4], coords[i*2+5]);
			tris.add(tri);
		}
		return tris;
	}
	
	/**
	 * Removes all triangles from this collection.
	 * Sets the {@link #isDirty()} state to true.
	 * @return this for chaining
	 */
	public Triangles removeAllTriangles() {
		triangles.clear();
		setDirty();
		return this;
	}
	
	/**
	 * Sets the global alpha multiplier parameter of this {@link Triangles} object.
	 * The value will be multiplied with each vertex' alpha color value when rendering.
	 * The triangle will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
	 * @param globalAlphaMultiplier of the triangles in this collection
	 * @return this for chaining
	 */
	public Triangles setGlobalAlphaMultiplier(double globalAlphaMultiplier) {
		return setGlobalAlphaMultiplier(()->globalAlphaMultiplier);
	}
	
	/**
	 * Sets the global alpha multiplier parameter of this {@link Triangles} object.
	 * The value will be multiplied with each vertex' alpha color value when rendering.
	 * The triangle will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
	 * @param globalAlphaMultiplier of the triangles in this collection
	 * @return this for chaining
	 */
	public Triangles setGlobalAlphaMultiplier(DoubleSupplier globalAlphaMultiplier) {
		this.globalAlphaMultiplier = globalAlphaMultiplier;
		return this;
	}

	/**
	 * @return the global alpha multiplier of the triangles in this collection
	 */
	public float getGlobalAlphaMultiplier() {
		return (float)globalAlphaMultiplier.getAsDouble();
	}

	/**
	 * Sets the saturation multiplier for this Renderable.
	 * The effective saturation of the colors results form multiplication of
	 * the respective color's saturation by this value.
	 * @param saturation change of saturation, default is 1
	 * @return this for chaining
	 */
	public Triangles setGlobalSaturationMultiplier(DoubleSupplier saturation) {
		this.globalSaturationMultiplier = saturation;
		return this;
	}

	/**
	 * Sets the saturation multiplier for this Renderable.
	 * The effective saturation of the colors results form multiplication of
	 * the respective color's saturation by this value.
	 * @param saturation change of saturation, default is 1
	 * @return this for chaining
	 */
	public Triangles setGlobalSaturationMultiplier(double saturation) {
		return setGlobalSaturationMultiplier(() -> saturation);
	}

	/** @return the saturation multiplier of this renderable */
	public float getGlobalSaturationMultiplier() {
		return (float)globalSaturationMultiplier.getAsDouble();
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}
	
	/**
	 * Hides or unhides this Triangles object, i.e. sets the {@link #isHidden()} field
	 * value. When hidden, renderers will not draw it.
	 * @param hide true when hiding
	 * @return this for chaining
	 */
	public Triangles hide(boolean hide) {
		this.hidden = hide;
		return this;
	}
	

	/**
	 * Disposes of GL resources, i.e. coloses the vertex array.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(va))
			va.close();
		va = null;
	}

	/**
	 * Allocates GL resources, i.e. creates and fills the vertex array.
	 * When already initialized, nothing happens.
	 */
	@Override
	@GLContextRequired
	public void initGL() {
		if(Objects.isNull(va)){
			va = new VertexArray(2);
			updateGL(false);
		}
	}

	/**
	 * Updates GL resources, i.e. fills the vertex array with the triangles contained
	 * in this {@link Triangles} object as well as their color and picking color attributes.
	 * Sets the {@link #isDirty()} state to false.
	 */
	@Override
	public void updateGL(boolean useGLDoublePrecision) {
		if(useGLDoublePrecision){
			updateGLDouble();
		} else {
			updateGLFloat();
		}
	}
	
	protected void updateGLFloat() {
		if(Objects.nonNull(va)){
			final int numTris = triangles.size();
			float[] vertices = new float[numTris*2*3];
			int[] vColors = new int[numTris*2*3];
			for(int i=0; i<numTris; i++){
				TriangleDetails tri = triangles.get(i);

				vertices[i*6+0] = (float) tri.p0.getX();
				vertices[i*6+1] = (float) tri.p0.getY();
				vertices[i*6+2] = (float) tri.p1.getX();
				vertices[i*6+3] = (float) tri.p1.getY();
				vertices[i*6+4] = (float) tri.p2.getX();
				vertices[i*6+5] = (float) tri.p2.getY();

				vColors[i*6+0] = tri.c0.getAsInt();
				vColors[i*6+1] = tri.pickColor;
				vColors[i*6+2] = tri.c1.getAsInt();
				vColors[i*6+3] = tri.pickColor;
				vColors[i*6+4] = tri.c2.getAsInt();
				vColors[i*6+5] = tri.pickColor;
			}
			va.setBuffer(0, 2, vertices);
			va.setBuffer(1, 2, false, vColors);
			isDirty = false;
			isGLDoublePrecision = false;
		}
	}
	
	protected void updateGLDouble() {
		if(Objects.nonNull(va)){
			final int numTris = triangles.size();
			double[] vertices = new double[numTris*2*3];
			int[] vColors = new int[numTris*2*3];
			for(int i=0; i<numTris; i++){
				TriangleDetails tri = triangles.get(i);

				vertices[i*6+0] = tri.p0.getX();
				vertices[i*6+1] = tri.p0.getY();
				vertices[i*6+2] = tri.p1.getX();
				vertices[i*6+3] = tri.p1.getY();
				vertices[i*6+4] = tri.p2.getX();
				vertices[i*6+5] = tri.p2.getY();

				vColors[i*6+0] = tri.c0.getAsInt();
				vColors[i*6+1] = tri.pickColor;
				vColors[i*6+2] = tri.c1.getAsInt();
				vColors[i*6+3] = tri.pickColor;
				vColors[i*6+4] = tri.c2.getAsInt();
				vColors[i*6+5] = tri.pickColor;
			}
			va.setBuffer(0, 2, vertices);
			va.setBuffer(1, 2, false, vColors);
			isDirty = false;
			isGLDoublePrecision = true;
		}
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	@Override
	public boolean isGLDoublePrecision() {
		return isGLDoublePrecision;
	}
	
	/**
	 * Sets the {@link #isDirty()} state of this {@link Triangles} object to true.
	 * @return this for chaining
	 */
	public Triangles setDirty() {
		this.isDirty = true;
		return this;
	}
	
	/**
	 * @return the bounding rectangle that encloses all line segments in this {@link Lines} object.
	 */
	public Rectangle2D getBounds(){
		if(numTriangles() < 1)
			return new Rectangle2D.Double();
		
		boolean useParallelStreaming = numTriangles() > 1000;
		double minX = Utils.parallelize(getTriangleDetails().stream(), useParallelStreaming)
				.flatMap(tri->Arrays.asList(tri.p0.getX(),tri.p1.getX(),tri.p2.getX()).stream())
				.mapToDouble(Double::floatValue)
				.min().getAsDouble();
		double maxX = Utils.parallelize(getTriangleDetails().stream(), useParallelStreaming)
				.flatMap(tri->Arrays.asList(tri.p0.getX(),tri.p1.getX(),tri.p2.getX()).stream())
				.mapToDouble(Double::floatValue)
				.max().getAsDouble();
		double minY = Utils.parallelize(getTriangleDetails().stream(), useParallelStreaming)
				.flatMap(tri->Arrays.asList(tri.p0.getY(),tri.p1.getY(),tri.p2.getY()).stream())
				.mapToDouble(Double::floatValue)
				.min().getAsDouble();
		double maxY = Utils.parallelize(getTriangleDetails().stream(), useParallelStreaming)
				.flatMap(tri->Arrays.asList(tri.p0.getY(),tri.p1.getY(),tri.p2.getY()).stream())
				.mapToDouble(Double::floatValue)
				.max().getAsDouble();
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}
	
	
	@Override
	public boolean intersects(Rectangle2D rect) {
		boolean useParallelStreaming = numTriangles() > 1000;
		return Utils.parallelize(getTriangleDetails().stream(), useParallelStreaming)
				.filter(tri->Utils.rectIntersectsOrIsContainedInTri(
						rect, 
						tri.p0.getX(), tri.p0.getY(), 
						tri.p1.getX(), tri.p1.getY(), 
						tri.p2.getX(), tri.p2.getY()
						))
				.findAny()
				.isPresent();
	}
	
	/**
	 * Returns the triangles that intersect or contain the specified rectangle.
	 * @param rect rectangle to test intersection
	 * @return list of intersecting triangles
	 */
	public List<TriangleDetails> getIntersectingTriangles(Rectangle2D rect){
		boolean useParallelStreaming = numTriangles() > 1000;
		return Utils.parallelize(getTriangleDetails().stream(), useParallelStreaming)
				.filter(tri->Utils.rectIntersectsOrIsContainedInTri(
						rect, 
						tri.p0.getX(), tri.p0.getY(), 
						tri.p1.getX(), tri.p1.getY(), 
						tri.p2.getX(), tri.p2.getY()
						))
				.collect(Collectors.toList());
	}
	

	/**
	 * Specification of a triangle which comprises vertex locations, colors and picking color.
	 * @author hageldave
	 */
	public static class TriangleDetails implements Cloneable {
		public Point2D p0,p1,p2;
		public IntSupplier c0,c1,c2;
		public int pickColor;
		
		public TriangleDetails(
				Point2D p0,
				Point2D p1,
				Point2D p2)
		{
			this.p0 = p0;
			this.p1 = p1;
			this.p2 = p2;
			this.c0 = c1 = c2 = ()->0xffaaaaaa;
		}
		
		public TriangleDetails(
				double x0, double y0, 
				double x1, double y1,
				double x2, double y2)
		{
			this(new Point2D.Float((float)x0, (float)y0), new Point2D.Float((float)x1, (float)y1), new Point2D.Float((float)x2, (float)y2));
		}
		
		/**
		 * Returns a shallow copy of this triangle.
		 * @return copy of this triangle
		 */
		public TriangleDetails copy() {
			return clone();
		}
		
		public TriangleDetails clone() {
			try {
	            return (TriangleDetails) super.clone();
	        } catch (CloneNotSupportedException e) {
	            // this shouldn't happen, since we are Cloneable
	            throw new InternalError(e);
	        }
		}
		
		/**
		 * Sets the picking color.
		 * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
		 * @param pickID picking color of the triangle (see {@link Triangles} for details)
		 * @return this for chaining
		 */
		public TriangleDetails setPickColor(int pickID){
			if(pickID != 0)
				pickID = pickID | 0xff000000;
			this.pickColor = pickID;
			return this;
		}

		/**
		 * Sets the color for vertex 0
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor0(IntSupplier color){
			this.c0 = color;
			return this;
		}
		
		/**
		 * Sets the color for vertex 1
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor1(IntSupplier color){
			this.c1 = color;
			return this;
		}
		
		/**
		 * Sets the color for vertex 2
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor2(IntSupplier color){
			this.c2 = color;
			return this;
		}
		
		/**
		 * Sets the color of the triangle (all vertices)
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor(IntSupplier color){
			this.c0 = this.c1 = this.c2 = color;
			return this;
		}
		
		/**
		 * Sets the color of vertex 0
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor0(int color){
			return setColor0(()->color);
		}
		
		/**
		 * Sets the color of vertex 1
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor1(int color){
			return setColor1(()->color);
		}
		
		/**
		 * Sets the color of vertex 2
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor2(int color){
			return setColor2(()->color);
		}
		
		/**
		 * Sets the color of the triangle (all vertices)
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public TriangleDetails setColor(int color){
			return setColor(()->color);
		}
		
		/**
		 * Sets the color of vertex 0
		 * @param color of v0
		 * @return this for chaining
		 */
		public TriangleDetails setColor0(Color color){
			return setColor0(color.getRGB());
		}
		
		/**
		 * Sets the color of vertex 1
		 * @param color of v1
		 * @return this for chaining
		 */
		public TriangleDetails setColor1(Color color){
			return setColor1(color.getRGB());
		}
		
		/**
		 * Sets the color of vertex 2
		 * @param color of v2
		 * @return this for chaining
		 */
		public TriangleDetails setColor2(Color color){
			return setColor2(color.getRGB());
		}
		
		/**
		 * Sets the color the triangle (all vertices)
		 * @param color of the triangle
		 * @return this for chaining
		 */
		public TriangleDetails setColor(Color color){
			return setColor(color.getRGB());
		}
	}
	
	/**
	 * @return the list of triangle details.<br>
	 * Make sure to call {@link #setDirty()} when manipulating.
	 */
	public ArrayList<TriangleDetails> getTriangleDetails() {
		return triangles;
	}

	/**
	 * Returns this object's {@link VertexArray}.
	 * The first attribute (index=0) of the VA contains the 2D vertices of the triangles of this collection.
	 * The second attribute contains 2D vertices corresponding to the first attribute that contain the
	 * unsigned integer packed ARGB values of the color (on x coordinate) and picking color (on y coordinate) 
	 * per vertex.
	 * @return the vertex array.
	 */
	public VertexArray getVertexArray() {
		return va;
	}


	/**
	 * Binds this object's vertex array and enables the corresponding attributes 
	 * (first and second attribute).
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	public void bindVertexArray() {
		va.bindAndEnableAttributes(0,1);
	}


	/**
	 * Releases this objects vertex array and disables the corresponding attributes
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	public void releaseVertexArray() {
		va.releaseAndDisableAttributes(0,1);
	}
	
	/**
	 * Returns true when crisp edge rendering for triangles in SVG should be used (=default).
	 * <p>
	 * Especially for triangle grids this option is of use as it clears the tiny space that
	 * will be visible otherwise between adjacent triangles in SVG. 
	 * When triangles are not connected this option should be set to false 
	 * ({@link #enableCrispEdgesForSVG(boolean)}) in order to have nice anti aliased edges of 
	 * the triangles in SVG.
	 * @return true when enabled.
	 */
	public boolean isCrispEdgesForSVGEnabled() {
		return useCrispEdgesForSVG;
	}
	
	/**
	 * En/Disables crisp edge rendering in SVG for this {@link Triangles} object.
	 * See {@link #isCrispEdgesForSVGEnabled()} for details.
	 * @param enable true when enabling
	 * @return this for chaining
	 */
	public Triangles enableCrispEdgesForSVG(boolean enable) {
		this.useCrispEdgesForSVG = enable;
		return this;
	}

	/**
	 * Return true when anti-aliasing is enabled for fallback (AWT) rendering (default=false).
	 * <p>
	 * Especially for triangle grids/meshes this option should be disabled to avoid visible triangle edges
	 * inside the grid/mesh.
	 * When triangles are not connected, this option can be enabled to get anti-aliased edges.
	 * @return true when enabled
	 */
	public boolean isAAinFallbackEnabled() {
		return this.useAAinFallback;
	}
	
	/**
	 * En/Disables anti-aliasing for this {@link Triangles} object during fallback rendering.
	 * See {@link #isAAinFallbackEnabled()} for details.
	 * @param enable true when enabling
	 * @return this for chaining
	 */
	public Triangles enableAAinFallback(boolean enable) {
		this.useAAinFallback = enable;
		return this;
	}
	
}
