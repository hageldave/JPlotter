package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.globjects.FBO;
import hageldave.jplotter.globjects.VertexArray;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.util.Utils;

/**
 * The Lines class is a collection of linear line segments.
 * Each segment is defined by a 2D start and end point and can
 * be colored per point. This means that when the colors at start
 * and endpoint are different the line will be rendered with a
 * linear color gradient which is interpolated between the two points.
 * Per default the thickness of the line segments is 1 pixel but can be
 * altered for all segments in a {@link Lines} object (not per segment).
 * Each segment has a single picking color.
 * The picking color is the color with which the lines are rendered into the (invisible) picking color attachment
 * of an {@link FBO}. This color may serve as an identifier of the object that can be queried from a location of the
 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values) or 0.
 * <p>
 * There is also a global alpha multiplier which scales every segments color alpha value, which can be used to introduce transparency
 * for all segments of this collection. This may come in handy to visualize density when plotting a huge amount of lines.
 * 
 * @author hageldave
 */
public class Lines implements Renderable {

	protected VertexArray va;

	protected ArrayList<SegmentDetails> segments = new ArrayList<>();

	protected float thickness = 1;

	protected boolean isDirty;

	protected float globalAlphaMultiplier=1;

	protected boolean useVertexRounding=false;

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
	 * @param c1 integer packed ARGB color value of line at start point (e.g. 0xff00ff00 = opaque green)
	 * @param c2 integer packed ARGB color value of line at end point
	 * @param pickingColor the picking color of the segment. 
	 * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
	 * @return this for chaining
	 */
	public Lines addSegment(Point2D p1, Point2D p2, int c1, int c2, int pickingColor){
		segments.add(new SegmentDetails(p1, p2, c1, c2, pickingColor));
		setDirty();
		return this;
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
		return this.addSegment(p1, p2, c1.getRGB(), c2.getRGB(), 0);
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
	 * @param pickingColor the picking color of the segment. 
	 * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
	 * @return this for chaining
	 */
	public Lines addSegment(double x1, double y1, double x2, double y2, int c1, int c2, int pickingColor){
		return addSegment(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), c1, c2, pickingColor);
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
		return addSegment(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), c1, c2, 0);
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
	 * Adds a strip of line segments that connect the specified
	 * points.
	 * Sets the {@link #isDirty()} state to true.
	 * @param c color of the line
	 * @param xCoords x coordinates of the points on the line
	 * @param yCoords y coordinates of the points on the line
	 * @return this for chaining
	 */
	public Lines addLineStrip(int c, double[] xCoords, double[] yCoords){
		for(int i = 0; i < xCoords.length-1; i++){
			this.addSegment(xCoords[i], yCoords[i], xCoords[i+1], yCoords[i+1], c);
		}
		return this;
	}

	/**
	 * Adds a strip of line segments that connect the specified
	 * points.
	 * Sets the {@link #isDirty()} state to true.
	 * @param c color of the line
	 * @param points which are connected by line segments
	 * @return this for chaining
	 */
	public Lines addLineStrip(Color c, Point2D...points){
		for(int i = 0; i < points.length-1; i++){
			this.addSegment(points[i], points[i+1], c);
		}
		return this;
	}

	/**
	 * Adds a strip of line segments that connect the specified points.
	 * The point coordinates are specified as interleaved values, i.e.
	 * array of pairs of (x,y) coordinates.
	 * Sets the {@link #isDirty()} state to true.
	 * @param c color of the line
	 * @param coords (x,y) pairs of the points to connect
	 * @return this for chaining
	 * @throws IllegalArgumentException when the number of coordinate values is odd.
	 */
	public Lines addLineStrip(int c, double... coords){
		if(coords.length%2 != 0){
			throw new IllegalArgumentException("did not provide even amount of coordinate values.");
		}
		for(int i=0; i<coords.length/2-1; i++){
			this.addSegment(
					coords[(i+0)*2+0], coords[(i+0)*2+1], 
					coords[(i+1)*2+0], coords[(i+1)*2+1], 
					c
					);
		}
		return this;
	}

	/**
	 * Removes a line segment from this object
	 * Sets the {@link #isDirty()} state to true if successful.
	 * @param segment pair of start and end point that defines the segment to remove
	 * @return true when successful
	 */
	public boolean removeSegment(SegmentDetails segment){
		int idx = segments.indexOf(segment);
		if(idx >= 0){
			segments.remove(idx);
			setDirty();
			return true;
		}
		return false;
	}

	/**
	 * Sets the global alpha multiplier parameter of this {@link Lines} object.
	 * The value will be multiplied with each segment point's alpha color value when rendering.
	 * The segment will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
	 * @param globalAlphaMultiplier of the triangles in this collection
	 * @return this for chaining
	 */
	public Lines setGlobalAlphaMultiplier(double globalAlphaMultiplier) {
		this.globalAlphaMultiplier = (float)globalAlphaMultiplier;
		return this;
	}

	/**
	 * @return the global alpha multiplier of the segments in this collection
	 */
	public float getGlobalAlphaMultiplier() {
		return globalAlphaMultiplier;
	}

	/**
	 * Removes all segments of this object.
	 * Sets the {@link #isDirty()} state to true.
	 * @return this for chaining
	 */
	public Lines removeAllSegments() {
		this.segments.clear();
		return setDirty();
	}

	/**
	 * @return a copy of the line segments list
	 */
	public ArrayList<SegmentDetails> getSegments() {
		return segments;
	}

	/**
	 * Sets the line thickness for this {@link Lines} object in pixels.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Lines setThickness(double thickness) {
		this.thickness = (float)thickness;
		return this;
	}

	/**
	 * @return the line thickness of this {@link Lines} object
	 */
	public float getThickness() {
		return thickness;
	}

	/**
	 * @return whether vertex rounding is enabled. This indicates if
	 * the {@link LinesRenderer}'s shader will round vertex positions of 
	 * the quad vertices (that a segment is expanded to) to integer values.
	 * <p>
	 * This has the effect of sharpening horizontal and vertical lines, but
	 * can affect differently oriented lines to shrink in thickness or even vanish. 
	 */
	public boolean isVertexRoundingEnabled() {
		return useVertexRounding;
	}

	/**
	 * En/Disables vertex rounding for this Lines object. This indicates if
	 * the {@link LinesRenderer}'s shader will round vertex positions of 
	 * the quad vertices (that a segment is expanded to) to integer values.
	 * <p>
	 * This has the effect of sharpening horizontal and vertical lines, but
	 * can affect differently oriented lines to shrink in thickness or even vanish. 
	 * <p>
	 * Also this only makes sense when the Lines object is of integer valued thickness.
	 * @param useVertexRounding will enable if true
	 * @return this for chaining
	 */
	public Lines setVertexRoundingEnabled(boolean useVertexRounding) {
		this.useVertexRounding = useVertexRounding;
		return this;
	}

	/**
	 * @return the bounding rectangle that encloses all line segments in this {@link Lines} object.
	 */
	public Rectangle2D getBounds(){
		if(numSegments() < 1)
			return new Rectangle2D.Double();
		
		boolean useParallelStreaming = numSegments() > 1000;
		double minX = Utils.parallelize(getSegments().stream(), useParallelStreaming)
				.flatMap(seg->Arrays.asList(seg.p0,seg.p1).stream())
				.mapToDouble(Point2D::getX)
				.min().getAsDouble();
		double maxX = Utils.parallelize(getSegments().stream(), useParallelStreaming)
				.flatMap(seg->Arrays.asList(seg.p0,seg.p1).stream())
				.mapToDouble(Point2D::getX)
				.max().getAsDouble();
		double minY = Utils.parallelize(getSegments().stream(), useParallelStreaming)
				.flatMap(seg->Arrays.asList(seg.p0,seg.p1).stream())
				.mapToDouble(Point2D::getY)
				.min().getAsDouble();
		double maxY = Utils.parallelize(getSegments().stream(), useParallelStreaming)
				.flatMap(seg->Arrays.asList(seg.p0,seg.p1).stream())
				.mapToDouble(Point2D::getY)
				.max().getAsDouble();
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}

	/**
	 * Specification of a line segment which comprises vertex locations, colors and picking color.
	 * @author hageldave
	 */
	public static class SegmentDetails {
		public Point2D p0;
		public Point2D p1;
		public int color0;
		public int color1;
		public int pickingColor;

		public SegmentDetails(Point2D p0, Point2D p1, int color0, int color1, int pickingColor) {
			this.p0 = p0;
			this.p1 = p1;
			this.color0 = color0;
			this.color1 = color1;
			if(pickingColor != 0)
				pickingColor = pickingColor | 0xff000000;
			this.pickingColor = pickingColor;
		}
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
			va = new VertexArray(3);
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
			int[] pickBuffer = new int[segments.size()*2];
			for(int i=0; i<segments.size(); i++){
				SegmentDetails seg = segments.get(i);
				segmentCoordBuffer[i*4+0] = (float) seg.p0.getX();
				segmentCoordBuffer[i*4+1] = (float) seg.p0.getY();
				segmentCoordBuffer[i*4+2] = (float) seg.p1.getX();
				segmentCoordBuffer[i*4+3] = (float) seg.p1.getY();

				colorBuffer[i*2+0] = seg.color0;
				colorBuffer[i*2+1] = seg.color1;

				pickBuffer[i*2+0] = pickBuffer[i*2+1] = seg.pickingColor;
			}
			va.setBuffer(0, 2, segmentCoordBuffer);
			va.setBuffer(1, 1, false, colorBuffer);
			va.setBuffer(2, 1, false, pickBuffer);
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
		va.bindAndEnableAttributes(0,1,2);
	}


	/**
	 * Releases this objects vertex array and disables the corresponding attributes
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void releaseVertexArray() {
		va.releaseAndDisableAttributes(0,1,2);
	}

}
