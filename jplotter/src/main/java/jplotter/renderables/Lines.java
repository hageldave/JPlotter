package jplotter.renderables;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Objects;

import hageldave.imagingkit.core.Pixel;
import jplotter.Annotations.GLContextRequired;
import jplotter.globjects.FBO;
import jplotter.globjects.VertexArray;
import jplotter.util.Pair;

/**
 * The Lines class is a collection of linear line segments.
 * Each segment is defined by a 2D start and end point and can
 * be colored per point. This means that when the colors at start
 * and endpoint are differnt the line will be rendered with a
 * linear color gradient which is interpolated between the two points.
 * Per default the thickness of the line segments is 1 pixel but can be
 * altered for all segments in a {@link Lines} object (not per segment).
 * The Lines object has a global picking color (same for all line segments).
 * The picking color is the color with which the lines are rendered into the (invisible) picking color attachment
 * of an {@link FBO}. This color may serve as an identifier of the object that can be queried from a location of the
 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values).
 * 
 * @author hageldave
 */
public class Lines implements Renderable {

	protected VertexArray va;

	protected ArrayList<Pair<Point2D,Point2D>> segments = new ArrayList<>();

	protected ArrayList<Pair<Color,Color>> colors = new ArrayList<>();

	protected float thickness = 1;

	protected int pickColor;

	protected boolean isDirty;

	/**
	 * Sets the {@link #isDirty()} state of this renderable to true.
	 * This indicates that an {@link #updateGL()} call is necessary to sync GL resources.
	 * @return this for chaining
	 */
	public Lines setDirty() {
		this.isDirty = true;
		return this;
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	/**
	 * @return the number of line segments in this {@link Lines} object
	 */
	public int numSegments() {
		return segments.size();
	}

	/**
	 * Adds a new line segment to this object.
	 * Sets the {@link #isDirty()} state to true.
	 * @param p1 start point
	 * @param p2 end point
	 * @param c1 color of line at start point
	 * @param c2 color of line at end point
	 * @return this for chaining
	 */
	public Lines addSegment(Point2D p1, Point2D p2, Color c1, Color c2){
		segments.add(Pair.of(p1,p2));
		colors.add(Pair.of(c1,c2));
		setDirty();
		return this;
	}

	/**
	 * Adds a new line segment to this object.
	 * Sets the {@link #isDirty()} state to true.
	 * @param p1 start point
	 * @param p2 end point
	 * @param c color of the line
	 * @return this for chaining
	 */
	public Lines addSegment(Point2D p1, Point2D p2, Color c){
		return addSegment(p1, p2, c, c);
	}

	/**
	 * Adds a new line segment to this object.
	 * Sets the {@link #isDirty()} state to true.
	 * @param x1 x coordinate of start point
	 * @param y1 y coordinate of start point
	 * @param x2 x coordinate of end point
	 * @param y2 y coordinate of end point
	 * @param c1 integer packed ARGB color value of line at start point (e.g. 0xff00ff00 = opaque green)
	 * @param c2 integer packed ARGB color value of line at end point
	 * @return this for chaining
	 */
	public Lines addSegment(double x1, double y1, double x2, double y2, int c1, int c2){
		return addSegment(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), new Color(c1, true), new Color(c2, true));
	}

	/**
	 * Adds a new line segment to this object.
	 * Sets the {@link #isDirty()} state to true.
	 * @param x1 x coordinate of start point
	 * @param y1 y coordinate of start point
	 * @param x2 x coordinate of end point
	 * @param y2 y coordinate of end point
	 * @param c integer packed ARGB color value of line (e.g. 0xff00ff00 = opaque green)
	 * @return this for chaining
	 */
	public Lines addSegment(double x1, double y1, double x2, double y2, int c){
		return addSegment(x1, y1, x2, y2, c, c);
	}

	/**
	 * Removes a line segment from this object
	 * Sets the {@link #isDirty()} state to true if successful.
	 * @param segment pair of start and end point that defines the segment to remove
	 * @return true when successful
	 */
	public boolean removeSegment(Pair<Point2D,Point2D> segment){
		int idx = segments.indexOf(segment);
		if(idx >= 0){
			segments.remove(idx);
			colors.remove(idx);
			setDirty();
			return true;
		}
		return false;
	}

	/**
	 * Removes a line segment from this object
	 * Sets the {@link #isDirty()} state to true if successful.
	 * @param p1 start point that of the segment to remove
	 * @param p2 end point that of the segment to remove
	 * @return true when successful
	 */
	public boolean removeSegment(Point2D p1, Point2D p2){
		return removeSegment(Pair.of(p1, p2));
	}

	/**
	 * Removes all segments of this object.
	 * Sets the {@link #isDirty()} state to true.
	 */
	public void removeAllSegments() {
		this.segments.clear();
		this.colors.clear();
		setDirty();
	}

	/**
	 * @return a copy of the line segments list
	 */
	public ArrayList<Pair<Point2D, Point2D>> getSegments() {
		return new ArrayList<>(segments);
	}

	/**
	 * @return a copy of the segment color list
	 */
	public ArrayList<Pair<Color, Color>> getColors() {
		return new ArrayList<>(colors);
	}

	/**
	 * Sets the line thickness for this {@link Lines} object in pixels.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Lines setThickness(float thickness) {
		this.thickness = thickness;
		return this;
	}

	/**
	 * @return the line thickness of this {@link Lines} object
	 */
	public float getThickness() {
		return thickness;
	}

	/**
	 * Sets the picking color of this {@link Lines} object. 
	 * The picking color is the color with which the segments are rendered into the (invisible) picking color attachment
	 * of an {@link FBO}. This color may serve as an identifier of the object that can be queried from a location of the
	 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values).
	 * @param pickColor opaque integer packed RGB value, 0 or one in [0xff000001..0xffffffff].
	 * When a transparent color is specified its alpha channel will be set to 0xff to make it opaque.
	 * @return this for chaining
	 */
	public Lines setPickColor(int pickColor) {
		this.pickColor = pickColor;
		// can only use opaque colors cause transparent colors will not work on overlaps
		if(pickColor != 0)
			this.pickColor = pickColor | 0xff000000;
		return this;
	}

	/**
	 * @return the picking color of this {@link Lines} object
	 */
	public int getPickColor() {
		return pickColor;
	}

	/**
	 * @return the normalized red channel of the picking color (in [0,1])
	 */
	public float getPickColorR() {
		return Pixel.r(pickColor)/255f;
	}

	/**
	 * @return the normalized green channel of the picking color in [0,1])
	 */
	public float getPickColorG() {
		return Pixel.g(pickColor)/255f;
	}

	/**
	 * @return the normalized blue channel of the picking color in [0,1])
	 */
	public float getPickColorB() {
		return Pixel.b(pickColor)/255f;
	}

	/**
	 * disposes of the GL resources of this lines object,
	 * i.e deletes the vertex array.
	 */
	@Override
	@GLContextRequired
	public void close(){
		if(Objects.nonNull(va)){
			va.close();
			va = null;
		}
	}

	/**
	 * Allocates GL resources, i.e. creates the vertex array and fills
	 * it according to the contents of this {@link Lines} object.
	 * If the vertex array has already been created, nothing happens.
	 */
	@Override
	@GLContextRequired
	public void initGL(){
		if(Objects.isNull(va)){
			va = new VertexArray(2);
			updateGL();
		}
	}

	/**
	 * Updates the vertex array to be in sync with this lines object.
	 * This sets the {@link #isDirty()} state to false.
	 * if {@link #initGL()} has not been called yet or this object has
	 * already been closed, nothing happens
	 */
	@Override
	@GLContextRequired
	public void updateGL(){
		if(Objects.nonNull(va)){
			float[] segmentCoordBuffer = new float[segments.size()*2*2];
			int[] colorBuffer = new int[segments.size()*2];
			for(int i=0; i<segments.size(); i++){
				Pair<Point2D, Point2D> seg = segments.get(i);
				Pair<Color, Color> colorpair = colors.get(i);
				segmentCoordBuffer[i*4+0] = (float) seg.first.getX();
				segmentCoordBuffer[i*4+1] = (float) seg.first.getY();
				segmentCoordBuffer[i*4+2] = (float) seg.second.getX();
				segmentCoordBuffer[i*4+3] = (float) seg.second.getY();

				colorBuffer[i*2+0] = colorpair.first.getRGB();
				colorBuffer[i*2+1] = colorpair.second.getRGB();
			}
			va.setBuffer(0, 2, segmentCoordBuffer);
			va.setBuffer(1, 1, false, colorBuffer);
			isDirty = false;
		}
	}


	/**
	 * Returns the vertex array of this lines object.
	 * The vertex array's first attribute contains the 2D point pairs of
	 * the line segments, the second attribute contains integer packed RGB
	 * value pairs for the line segments.
	 * @return the vertex array associated with this lines object or null if
	 * {@link #initGL()} was not yet called or this object was already closed.
	 */
	public VertexArray getVertexArray() {
		return va;
	}


	/**
	 * Binds this object's vertex array and enables the corresponding attributes 
	 * (first and second attribute).
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void bindVertexArray() {
		va.bindAndEnableAttributes(0,1);
	}


	/**
	 * Releases this objects vertex array and disables the corresponding attributes
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void releaseVertexArray() {
		va.releaseAndDisableAttributes(0,1);
	}

}
