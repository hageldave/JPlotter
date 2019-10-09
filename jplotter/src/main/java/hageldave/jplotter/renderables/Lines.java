package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;
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

	protected DoubleSupplier globalThicknessMultiplier = () -> 1.0;

	protected boolean isDirty;

	protected DoubleSupplier globalAlphaMultiplier = () -> 1.0;

	protected boolean useVertexRounding=false;

	protected short strokePattern = (short)0xffff;
	
	protected float strokeLength = 16;
	
	protected boolean hidden = false;

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
	 * @return the added segment
	 */
	public SegmentDetails addSegment(Point2D p1, Point2D p2){
		SegmentDetails seg = new SegmentDetails(p1, p2);
		segments.add(seg);
		setDirty();
		return seg;
	}

	/**
	 * Adds a new line segment to this object.
	 * Sets the {@link #isDirty()} state to true.
	 * @param x1 x coordinate of start point
	 * @param y1 y coordinate of start point
	 * @param x2 x coordinate of end point
	 * @param y2 y coordinate of end point
	 * @return the added segment
	 */
	public SegmentDetails addSegment(double x1, double y1, double x2, double y2){
		return addSegment(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
	}

	/**
	 * Adds a strip of line segments that connect the specified
	 * points.
	 * Sets the {@link #isDirty()} state to true.
	 * @param xCoords x coordinates of the points on the line
	 * @param yCoords y coordinates of the points on the line
	 * @return the added segments
	 */
	public ArrayList<SegmentDetails> addLineStrip(double[] xCoords, double[] yCoords){
		ArrayList<SegmentDetails> segments = new ArrayList<>(xCoords.length-1);
		for(int i = 0; i < xCoords.length-1; i++){
			segments.add( this.addSegment(xCoords[i], yCoords[i], xCoords[i+1], yCoords[i+1]) );
		}
		return segments;
	}

	/**
	 * Adds a strip of line segments that connect the specified
	 * points.
	 * Sets the {@link #isDirty()} state to true.
	 * @param points which are connected by line segments
	 * @return the added segments
	 */
	public ArrayList<SegmentDetails> addLineStrip(Point2D...points){
		ArrayList<SegmentDetails> segments = new ArrayList<>(points.length-1);
		for(int i = 0; i < points.length-1; i++){
			segments.add( this.addSegment(points[i], points[i+1]) );
		}
		return segments;
	}
	
	/**
	 * Adds a strip of line segments that connect the specified
	 * points.
	 * Sets the {@link #isDirty()} state to true.
	 * @param coords array of point coordinates, {@code x=coords[i][0]; y=coords[i][1];}
	 * @return the added segments
	 */
	public ArrayList<SegmentDetails> addLineStrip(double[][] coords){
		ArrayList<SegmentDetails> segments = new ArrayList<>(coords.length-1);
		for(int i = 0; i < coords.length-1; i++){
			SegmentDetails segment = this.addSegment(
					coords[i][0],coords[i][1],  
					coords[i+1][0],coords[i+1][1]);
			segments.add(segment);
		}
		return segments;
	}

	/**
	 * Adds a strip of line segments that connect the specified points.
	 * The point coordinates are specified as interleaved values, i.e.
	 * array of pairs of (x,y) coordinates.
	 * Sets the {@link #isDirty()} state to true.
	 * @param coords (x,y) pairs of the points to connect
	 * @return the added segments
	 * @throws IllegalArgumentException when the number of coordinate values is odd.
	 */
	public ArrayList<SegmentDetails> addLineStrip(double... coords){
		if(coords.length%2 != 0){
			throw new IllegalArgumentException("did not provide even amount of coordinate values.");
		}
		ArrayList<SegmentDetails> segments = new ArrayList<>(coords.length/2-1);
		for(int i=0; i<coords.length/2-1; i++){
			SegmentDetails seg = this.addSegment(
					coords[(i+0)*2+0], coords[(i+0)*2+1], 
					coords[(i+1)*2+0], coords[(i+1)*2+1]);
			segments.add(seg);
		}
		return segments;
	}

	/**
	 * Sets the global alpha multiplier parameter of this {@link Lines} object.
	 * The value will be multiplied with each segment point's alpha color value when rendering.
	 * The segment will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
	 * @param globalAlphaMultiplier of the segments in this collection
	 * @return this for chaining
	 */
	public Lines setGlobalAlphaMultiplier(DoubleSupplier globalAlphaMultiplier) {
		this.globalAlphaMultiplier = globalAlphaMultiplier;
		return this;
	}
	
	/**
	 * Sets the global alpha multiplier parameter of this {@link Lines} object.
	 * The value will be multiplied with each segment point's alpha color value when rendering.
	 * The segment will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
	 * @param globalAlphaMultiplier of the segments in this collection
	 * @return this for chaining
	 */
	public Lines setGlobalAlphaMultiplier(double globalAlphaMultiplier) {
		return setGlobalAlphaMultiplier(() -> globalAlphaMultiplier);
	}

	/**
	 * @return the global alpha multiplier of the segments in this collection
	 */
	public float getGlobalAlphaMultiplier() {
		return (float)globalAlphaMultiplier.getAsDouble();
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
	 * @return the line segments list.
	 * Make sure to call {@link #setDirty()} when manipulating.
	 */
	public ArrayList<SegmentDetails> getSegments() {
		return segments;
	}
	
	/**
	 * Sets the line thickness for this {@link Lines} object in pixels.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Lines setGlobalThicknessMultiplier(DoubleSupplier thickness) {
		this.globalThicknessMultiplier = thickness;
		return this;
	}

	/**
	 * Sets the line thickness for this {@link Lines} object in pixels.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Lines setGlobalThicknessMultiplier(double thickness) {
		return setGlobalThicknessMultiplier(() -> thickness); 
	}

	/**
	 * @return the line thickness of this {@link Lines} object
	 */
	public float getGlobalThicknessMultiplier() {
		return (float)globalThicknessMultiplier.getAsDouble();
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
	
	@Override
	public boolean intersects(Rectangle2D rect) {
		boolean useParallelStreaming = numSegments() > 1000;
		return Utils.parallelize(getSegments().stream(), useParallelStreaming)
				.filter(seg->rect.intersectsLine(seg.p0.getX(), seg.p0.getY(), seg.p1.getX(), seg.p1.getY()))
				.findAny()
				.isPresent();
	}
	
	/**
	 * Returns the segments that intersect the specified rectangle.
	 * @param rect rectangle to test intersection
	 * @return list of intersecting segments
	 */
	public List<SegmentDetails> getIntersectingSegments(Rectangle2D rect) {
		boolean useParallelStreaming = numSegments() > 1000;
		return Utils.parallelize(getSegments().stream(), useParallelStreaming)
				.filter(seg->rect.intersectsLine(seg.p0.getX(), seg.p0.getY(), seg.p1.getX(), seg.p1.getY()))
				.collect(Collectors.toList());
	}
	
	/**
	 * Whether this Lines object has a stroke pattern other than 0xffff (completely solid).
	 * @return true when stroke pattern != 0xffff
	 */
	public boolean hasStrokePattern() {
		return this.strokePattern != (short)0xffff;
	}
	
	/**
	 * Returns this {@link Lines} object's stroke pattern
	 * @return stroke pattern
	 */
	public short getStrokePattern() {
		return this.strokePattern;
	}
	
	/**
	 * Sets this Lines object's stroke pattern.
	 * The stroke pattern is a 16bit number that defines a sequence of solid and empty parts of a stroke.
	 * <br>
	 * <b>An error message is printed on System.err when more than the first 16bits are non zero</b> since
	 * this method only takes an int for convenience to save you the cast to (short).
	 * <p>
	 * Here are some examples:
	 * <ul>
	 * <li>{@code 0xffff = 0b1111_1111_1111_1111 =} completely solid</li>
	 * <li>{@code 0x0000 = 0b0000_0000_0000_0000 =} completely empty (invisible)</li>
	 * <li>{@code 0xff00 = 0b1111_1111_0000_0000 =} first half solid, second half empty </li>
	 * <li>{@code 0x0f0f = 0b0000_1111_0000_1111 =} first and third quarter empty, second and fourth solid</li>
	 * <li>{@code 0xaaaa = 0b1010_1010_1010_1010 =} alternating every 16th of the stroke</li>
	 * </ul>
	 * @param strokePattern 16bit pattern
	 * @return this for chaining
	 */
	public Lines setStrokePattern(int strokePattern) {
		if(strokePattern >> 16 != 0){
			System.err.println("specified stroke pattern should only be 16 bits but is " + Integer.toBinaryString(strokePattern));
		}
		this.strokePattern = (short)strokePattern;
		return this;
	}
	
	/**
	 * Returns the stroke length in pixels, which is by default 16pixels.
	 * @return stroke length
	 */
	public float getStrokeLength() {
		return strokeLength;
	}
	
	/**
	 * Sets the stroke length in pixels. The specified stroke pattern will repeat every
	 * strokeLength pixels.
	 * @param strokeLength length of the stroke
	 * @return this for chaining
	 */
	public Lines setStrokeLength(double strokeLength) {
		this.strokeLength = (float) Math.max(0, strokeLength);
		return this;
	}
	
	@Override
	public boolean isHidden() {
		return hidden;
	}
	
	/**
	 * Hides or unhides this Lines object, i.e. sets the {@link #isHidden()} field
	 * value. When hidden, renderers will not draw it.
	 * @param hide true when hiding
	 * @return this for chaining
	 */
	public Lines hide(boolean hide) {
		this.hidden = hide;
		return this;
	}

	/**
	 * Specification of a line segment which comprises vertex locations, colors and picking color.
	 * @author hageldave
	 */
	public static class SegmentDetails implements Cloneable {
		protected static final DoubleSupplier[] PREDEFINED_THICKNESSES = new DoubleSupplier[]
				{()->0f, ()->1f, ()->2f, ()->3f, ()->4f};
		
		public Point2D p0;
		public Point2D p1;
		public IntSupplier color0;
		public IntSupplier color1;
		public DoubleSupplier thickness0 = PREDEFINED_THICKNESSES[1];
		public DoubleSupplier thickness1 = PREDEFINED_THICKNESSES[1];
		public int pickColor;

		public SegmentDetails(Point2D p0, Point2D p1) {
			this.p0 = p0;
			this.p1 = p1;
			this.color0 = this.color1 = ()->0xff555555;
		}
		
		/**
		 * Returns a shallow copy of this segment with deep copied
		 * positions {@link #p0} and {@link #p1}.
		 * @return copy of this segment
		 */
		public SegmentDetails copy() {
			try {
	            SegmentDetails clone = (SegmentDetails) super.clone();
	            clone.p0 = Utils.copy(clone.p0);
	            clone.p1 = Utils.copy(clone.p1);
	            return clone;
	        } catch (CloneNotSupportedException e) {
	            // this shouldn't happen, since we are Cloneable
	            throw new InternalError(e);
	        }
		}
		
		@Override
		public SegmentDetails clone() {
			try {
				return (SegmentDetails) super.clone();
			} catch (CloneNotSupportedException e) {
				// should never happen since cloneable
				throw new InternalError(e);
			}
		}
		
		/**
		 * Sets the picking color.
		 * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
		 * @param pickID picking color of the segment (see {@link Lines} for details)
		 * @return this for chaining
		 */
		public SegmentDetails setPickColor(int pickID){
			if(pickID != 0)
				pickID = pickID | 0xff000000;
			this.pickColor = pickID;
			return this;
		}
		
		/**
		 * Sets the color at the starting point of the segment
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public SegmentDetails setColor0(IntSupplier color){
			this.color0 = color;
			return this;
		}
		
		/**
		 * Sets the color at the end point of the segment
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public SegmentDetails setColor1(IntSupplier color){
			this.color1 = color;
			return this;
		}
		
		/**
		 * Sets the color of the segment (start and end point)
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public SegmentDetails setColor(IntSupplier color){
			this.color0 = this.color1 = color;
			return this;
		}
		
		/**
		 * Sets the color at the starting point of the segment
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public SegmentDetails setColor0(int color){
			return setColor0(()->color);
		}
		
		/**
		 * Sets the color at the starting point of the segment
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public SegmentDetails setColor1(int color){
			return setColor1(()->color);
		}
		
		/**
		 * Sets the color of the segment (start and end point)
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public SegmentDetails setColor(int color){
			return setColor(()->color);
		}
		
		/**
		 * Sets the color at the starting point of the segment
		 * @param color for starting point
		 * @return this for chaining
		 */
		public SegmentDetails setColor0(Color color){
			return setColor0(color.getRGB());
		}
		
		/**
		 * Sets the color at the end point of the segment
		 * @param color for end point
		 * @return this for chaining
		 */
		public SegmentDetails setColor1(Color color){
			return setColor1(color.getRGB());
		}
		
		/**
		 * Sets the color of the segment (start and end point)
		 * @param color of the segment
		 * @return this for chaining
		 */
		public SegmentDetails setColor(Color color){
			return setColor(color.getRGB());
		}
		
		public SegmentDetails setThickness(double t){
			return setThickness(sup4thick(t));
		}
		
		public SegmentDetails setThickness(DoubleSupplier t){
			this.thickness0 = this.thickness1 = t;
			return this;
		}
		
		public SegmentDetails setThickness(double t0, double t1){
			return setThickness(sup4thick(t0), sup4thick(t1));
		}
		
		public SegmentDetails setThickness(DoubleSupplier t0, DoubleSupplier t1){
			this.thickness0 = t0;
			this.thickness1 = t1;
			return this;
		}
		
		protected static DoubleSupplier sup4thick(double t){
			if( t == ((int)t) && t >= 0 && t < PREDEFINED_THICKNESSES.length){
				return PREDEFINED_THICKNESSES[(int)t];
			}
			return ()->t;
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
			va = new VertexArray(5);
			updateGL();
		}
	}

	/**
	 * Updates the vertex array to be in sync with this lines object.
	 * This sets the {@link #isDirty()} state to false.
	 * If {@link #initGL()} has not been called yet or this object has
	 * already been closed, nothing happens
	 */
	@Override
	@GLContextRequired
	@Deprecated(/* use updateGL(scaleX,scaleY) instead */)
	public void updateGL(){
		updateGL(1, 1);
	}
	
	/**
	 * Updates the vertex array to be in sync with this lines object.
	 * This sets the {@link #isDirty()} state to false.
	 * For calculating the path length of line segments in screen space
	 * the scaling parameters of the respective view transformation need
	 * to be specified in order to realize view invariant stroke patterns.
	 * <p>
	 * If {@link #initGL()} has not been called yet or this object has
	 * already been closed, nothing happens.
	 * @param scaleX scaling of the x coordinate of the current view transform
	 * @param scaleY scaling of the y coordinate of the current view transform
	 */
	@GLContextRequired
	public void updateGL(double scaleX, double scaleY){
		if(Objects.nonNull(va)){
			float[] segmentCoordBuffer = new float[segments.size()*2*2];
			int[] colorBuffer = new int[segments.size()*2];
			int[] pickBuffer = new int[segments.size()*2];
			float[] thicknessBuffer = new float[segments.size()*2];
			float[] pathLengthBuffer = new float[segments.size()*2];
			
			double xprev = 0, yprev=0, pathLen = 0;
			for(int i=0; i<segments.size(); i++){
				SegmentDetails seg = segments.get(i);
				double x0 = seg.p0.getX();
				double y0 = seg.p0.getY();
				double x1 = seg.p1.getX();
				double y1 = seg.p1.getY();
				
				segmentCoordBuffer[i*4+0] = (float) x0;
				segmentCoordBuffer[i*4+1] = (float) y0;
				segmentCoordBuffer[i*4+2] = (float) x1;
				segmentCoordBuffer[i*4+3] = (float) y1;

				colorBuffer[i*2+0] = seg.color0.getAsInt();
				colorBuffer[i*2+1] = seg.color1.getAsInt();

				pickBuffer[i*2+0] = pickBuffer[i*2+1] = seg.pickColor;
				
				thicknessBuffer[i*2+0] = (float)seg.thickness0.getAsDouble();
				thicknessBuffer[i*2+1] = (float)seg.thickness1.getAsDouble();
				
				if(xprev != x0 || yprev != y0){
					pathLen = 0;
				}
				double segLen = Utils.hypot((x1-x0)*scaleX, (y1-y0)*scaleY);
				pathLengthBuffer[i*2+0] = (float)pathLen;
				pathLengthBuffer[i*2+1] = (float)(pathLen += segLen);
				pathLen = pathLen % strokeLength;
				xprev = x1; yprev = y1;
			}
			va.setBuffer(0, 2, segmentCoordBuffer);
			va.setBuffer(1, 1, false, colorBuffer);
			va.setBuffer(2, 1, false, pickBuffer);
			va.setBuffer(3, 1, thicknessBuffer);
			va.setBuffer(4, 1, pathLengthBuffer);
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
		va.bindAndEnableAttributes(0,1,2,3,4);
	}


	/**
	 * Releases this objects vertex array and disables the corresponding attributes
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void releaseVertexArray() {
		va.releaseAndDisableAttributes(0,1,2,3,4);
	}

}
