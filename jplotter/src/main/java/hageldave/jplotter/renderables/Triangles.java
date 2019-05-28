package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Objects;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.globjects.FBO;
import hageldave.jplotter.globjects.VertexArray;

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
 * It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values).
 * 
 * @author hageldave
 */
public class Triangles implements Renderable {

	protected VertexArray va;
	protected boolean isDirty;
	protected ArrayList<TriangleDetails> triangles = new ArrayList<>();
	
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
	 * @param c0 integer packed ARGB color value of the first triangle vertex (0xff00ff00 = opaque green)
	 * @param x1 x coordinate of the second triangle vertex
	 * @param y1 y coordinate of the second triangle vertex
	 * @param c1 integer packed ARGB color value of the second triangle vertex
	 * @param x2 x coordinate of the third triangle vertex
	 * @param y2 y coordinate of the third triangle vertex
	 * @param c2 integer packed ARGB color value of the third triangle vertex
	 * @param pick picking color of the triangle
	 * @return this for chaining
	 */
	public Triangles addTriangle(
			double x0, double y0, int c0, 
			double x1, double y1, int c1, 
			double x2, double y2, int c2,
			int pick
	){
		this.triangles.add(new TriangleDetails((float)x0, (float)y0, c0, (float)x1, (float)y1, c1, (float)x2, (float)y2, c2, pick));
		setDirty();
		return this;
	}
	
	/**
	 * Adds a triangle to this collection.
	 * Sets the {@link #isDirty()} state to true.
	 * @param x0 x coordinate of the first triangle vertex
	 * @param y0 y coordinate of the first triangle vertex
	 * @param c0 integer packed ARGB color value of the first triangle vertex (0xff00ff00 = opaque green)
	 * @param x1 x coordinate of the second triangle vertex
	 * @param y1 y coordinate of the second triangle vertex
	 * @param c1 integer packed ARGB color value of the second triangle vertex
	 * @param x2 x coordinate of the third triangle vertex
	 * @param y2 y coordinate of the third triangle vertex
	 * @param c2 integer packed ARGB color value of the third triangle vertex
	 * @return this for chaining
	 */
	public Triangles addTriangle(
			double x0, double y0, int c0, 
			double x1, double y1, int c1, 
			double x2, double y2, int c2
	){
		return this.addTriangle(x0, y0, c0, x1, y1, c1, x2, y2, c2, 0);
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
	 * @param color of the triangle
	 * @param pick picking color of the triangle
	 * @return this for chaining
	 */
	public Triangles addTriangle(
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2,
			Color color, int pickColor
	){
		int c = color.getRGB();
		return this.addTriangle(x0, y0, c, x1, y1, c, x2, y2, c, pickColor);
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
	 * @param color of the triangle
	 * @return this for chaining
	 */
	public Triangles addTriangle(
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2,
			Color color
	){
		int c = color.getRGB();
		return this.addTriangle(x0, y0, c, x1, y1, c, x2, y2, c);
	}
	
	/**
	 * Adds a triangle to this collection.
	 * Sets the {@link #isDirty()} state to true.
	 * @param p0 first vertex of the triangle
	 * @param p1 second vertex of the triangle
	 * @param p2 third vertex of the triangle
	 * @param color of the triangle
	 * @param pickColor picking color of the triangle
	 * @return this for chaining
	 */
	public Triangles addTriangle(
			Point2D p0, Point2D p1,Point2D p2,
			Color color, int pickColor
	){
		int c = color.getRGB();
		return this.addTriangle(p0.getX(), p0.getY(), c, p1.getX(), p1.getY(), c, p2.getX(), p2.getY(), c, pickColor);
	}
	
	/**
	 * Adds a triangle to this collection.
	 * Sets the {@link #isDirty()} state to true.
	 * @param p0 first vertex of the triangle
	 * @param p1 second vertex of the triangle
	 * @param p2 third vertex of the triangle
	 * @param color of the triangle
	 * @return this for chaining
	 */
	public Triangles addTriangle(
			Point2D p0, Point2D p1,Point2D p2,
			Color color
	){
		int c = color.getRGB();
		return this.addTriangle(p0.getX(), p0.getY(), c, p1.getX(), p1.getY(), c, p2.getX(), p2.getY(), c);
	}
	
	/**
	 * Adds a triangle to this collection.
	 * Triangle will be colored with 0xffaaaaaa.
	 * Sets the {@link #isDirty()} state to true.
	 * @param x0 x coordinate of the first triangle vertex
	 * @param y0 y coordinate of the first triangle vertex
	 * @param x1 x coordinate of the second triangle vertex
	 * @param y1 y coordinate of the second triangle vertex
	 * @param x2 x coordinate of the third triangle vertex
	 * @param y2 y coordinate of the third triangle vertex
	 * @return this for chaining
	 */
	public Triangles addTriangle(
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2
	){
		int c = 0xffaaaaaa;
		return this.addTriangle(x0, y0, c, x1, y1, c, x2, y2, c);
	}
	
	/**
	 * Adds a triangle to this collection.
	 * Triangle will be colored with 0xffaaaaaa.
	 * Sets the {@link #isDirty()} state to true.
	 * @param p0 first vertex of the triangle
	 * @param p1 second vertex of the triangle
	 * @param p2 third vertex of the triangle
	 * @return this for chaining
	 */
	public Triangles addTriangle(Point2D p0, Point2D p1,Point2D p2){
		return this.addTriangle(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY());
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
	 * @param color of the quad
	 * @param pickColor picking color of the quad
	 * @return this for chaining
	 */
	public Triangles addQuad(
			double xBL, double yBL,
			double xTL, double yTL,
			double xTR, double yTR,
			double xBR, double yBR,
			Color color, int pickColor
	){
		return this
				.addTriangle(xBL, yBL, xTL, yTL, xTR, yTR, color, pickColor)
				.addTriangle(xBL, yBL, xTR, yTR, xBR, yBR, color, pickColor);
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
	 * @param color of the quad
	 * @return this for chaining
	 */
	public Triangles addQuad(
			double xBL, double yBL,
			double xTL, double yTL,
			double xTR, double yTR,
			double xBR, double yBR,
			Color color
	){
		return this.addQuad(xBL, yBL, xTL, yTL, xTR, yTR, xBR, yBR, color, 0);
	}
	
	/**
	 * Adds two triangles that form the specified quad.
	 * The quad will be colored with 0xffaaaaaa.
	 * Sets the {@link #isDirty()} state to true.
	 * @param xBL bottom left x
	 * @param yBL bottom left y
	 * @param xTL top left x
	 * @param yTL top left y
	 * @param xTR top right x
	 * @param yTR top right y
	 * @param xBR bottom right x
	 * @param yBR bottom right y
	 * @return this for chaining
	 */
	public Triangles addQuad(
			double xBL, double yBL,
			double xTL, double yTL,
			double xTR, double yTR,
			double xBR, double yBR
	){
		return this
				.addTriangle(xBL, yBL, xTL, yTL, xTR, yTR)
				.addTriangle(xBL, yBL, xTR, yTR, xBR, yBR);
	}
	
	/**
	 * Adds two triangles that form the specified quad.
	 * Sets the {@link #isDirty()} state to true.
	 * @param bl bottom left vertex
	 * @param tl top left vertex
	 * @param tr top right vertex
	 * @param br bottom right vertex
	 * @param color of the quad
	 * @return this for chaining
	 */
	public Triangles addQuad(Point2D bl, Point2D tl, Point2D tr, Point2D br, Color color){
		return this.addQuad(bl.getX(), bl.getY(), tl.getX(), tl.getY(), tr.getX(), tr.getY(), br.getX(), br.getY(), color);
	}
	
	/**
	 * Adds two triangles that form the specified quad.
	 * The quad will be colored with 0xffaaaaaa.
	 * Sets the {@link #isDirty()} state to true.
	 * @param bl bottom left vertex
	 * @param tl top left vertex
	 * @param tr top right vertex
	 * @param br bottom right vertex
	 * @return this for chaining
	 */
	public Triangles addQuad(Point2D bl, Point2D tl, Point2D tr, Point2D br){
		return this.addQuad(bl.getX(), bl.getY(), tl.getX(), tl.getY(), tr.getX(), tr.getY(), br.getX(), br.getY());
	}
	
	/**
	 * Adds a series of triangles called a triangle strip to {@link Triangles} object.
	 * Each vertex forms a new triangle together with the two preceding vertices.
	 * Make sure that the first vertex is the one that is not shared with the second triangle in the strip.
	 * Sets the {@link #isDirty()} state to true.
	 * @param color of the triangles in the strip
	 * @param pickColor picking color of the strip
	 * @param points the vertices (at least 3)
	 * @return this for chaining
	 * @throws IllegalArgumentException when less than 3 vertices were specified
	 */
	public Triangles addStrip(Color color, int pickColor, Point2D ... points){
		if(points.length < 3){
			throw new IllegalArgumentException("not enough points for triangle strip, need at least 3 but got " + points.length);
		}
		for(int i = 0; i < points.length-2; i++){
			addTriangle(points[i], points[i+1], points[i+2], color, pickColor);
		}
		return this;
	}
	
	/**
	 * Adds a series of triangles called a triangle strip to {@link Triangles} object.
	 * Each vertex forms a new triangle together with the two preceding vertices.
	 * Make sure that the first vertex is the one that is not shared with the second triangle in the strip.
	 * The triangles will be colored with 0xffaaaaaa.
	 * Sets the {@link #isDirty()} state to true.
	 * @param points the vertices (at least 3)
	 * @return this for chaining
	 * @throws IllegalArgumentException when less than 3 vertices were specified
	 */
	public Triangles addStrip(Color color, Point2D ... points){
		return this.addStrip(color, 0, points);
	}
	
	/**
	 * Adds a series of triangles called a triangle strip to {@link Triangles} object.
	 * Each vertex forms a new triangle together with the two preceding vertices.
	 * Make sure that the first vertex is the one that is not shared with the second triangle in the strip.
	 * Sets the {@link #isDirty()} state to true.
	 * @param color of the triangles in the strip
	 * @param pickColor picking color of the strip
	 * @param coordinates the coordinates of the triangle vertices, a series of (x,y) pairs.
	 * Need at least 6 coordinates (3 pairs) and has to be an even number of coordinates.
	 * @return this for chaining
	 * @throws IllegalArgumentException when less than 3 vertices were specified or an odd number of coordinates
	 * was specified.
	 */
	public Triangles addStrip(Color color, int pickColor, double... coords){
		if(coords.length < 6){
			throw new IllegalArgumentException("not enough coordinates for triangle strip, need at least 6 ( 3x {x,y} ) but got " + coords.length);
		}
		if(coords.length % 2 != 0){
			throw new IllegalArgumentException("need an even number of coordinates for triangle strip ({x,y} pairs), but got " + coords.length);
		}
		for(int i = 0; i < coords.length/2-2; i++){
			addTriangle(coords[i*2+0], coords[i*2+1], coords[i*2+2], coords[i*2+3], coords[i*2+4], coords[i*2+5], color, pickColor);
		}
		return this;
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
			updateGL();
		}
	}

	/**
	 * Updates GL resources, i.e. fills the vertex array with the triangles contained
	 * in this {@link Triangles} object as well as their color and picking color attributes.
	 * Sets the {@link #isDirty()} state to false.
	 */
	@Override
	public void updateGL() {
		if(Objects.nonNull(va)){
			final int numTris = triangles.size();
			float[] vertices = new float[numTris*2*3];
			int[] vColors = new int[numTris*2*3];
			for(int i=0; i<numTris; i++){
				TriangleDetails tri = triangles.get(i);

				vertices[i*6+0] = tri.x0;
				vertices[i*6+1] = tri.y0;
				vertices[i*6+2] = tri.x1;
				vertices[i*6+3] = tri.y1;
				vertices[i*6+4] = tri.x2;
				vertices[i*6+5] = tri.y2;

				vColors[i*6+0] = tri.c0;
				vColors[i*6+1] = tri.pick;
				vColors[i*6+2] = tri.c1;
				vColors[i*6+3] = tri.pick;
				vColors[i*6+4] = tri.c2;
				vColors[i*6+5] = tri.pick;
			}
			va.setBuffer(0, 2, vertices);
			va.setBuffer(1, 2, false, vColors);
			isDirty = false;
		}
	}

	@Override
	public boolean isDirty() {
		return isDirty;
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
	 * Specification of a Triangle which comprises vertex locations, colors and picking color.
	 * @author hageldave
	 */
	public static class TriangleDetails {
		public float x0,x1,x2, y0,y1,y2;
		public int c0,c1,c2, pick;
		public TriangleDetails(
				float x0, float y0, int c0, 
				float x1, float y1, int c1, 
				float x2, float y2, int c2,
				int pick) 
		{
			this.x0 = x0;
			this.x1 = x1;
			this.x2 = x2;
			this.y0 = y0;
			this.y1 = y1;
			this.y2 = y2;
			this.c0 = c0;
			this.c1 = c1;
			this.c2 = c2;
			if(pick != 0)
				pick = pick | 0xff000000;
			this.pick = pick;
		}
	}
	
	/**
	 * @return the list of triangle details.<br>
	 * Make sure to call {@link #setDirty()} when manipulating.
	 */
	public ArrayList<TriangleDetails> getTriangles() {
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
	
}
