package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL33;

import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderers.PointsRenderer;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.Utils;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The Points class is a collection of 2D points that are to be represented
 * using the same {@link Glyph} (the graphical representation of a point).
 * A point instance in this collection has the following attributes.
 * <ul>
 * <li>position - the 2D location of the point</li>
 * <li>scaling - the scaling of the glyph it is represented with</li>
 * <li>rotation - the rotation of the glyph it is represented with</li>
 * <li>color - the color with wich the glyph it is renered</li>
 * <li>picking color - the picking color with which the glyph is rendered into the (invisible) picking color attachment
 * of an {@link FBO}. This color may serve as an identifier of the object that can be queried from a location of the
 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values) or 0.
 * </li>
 * </ul>
 * Apart from these per point attributes, this {@link Points} class features a global scaling parameter by which all
 * point instances of this collection will be scaled at rendering ({@link #setGlobalScaling(double)}).
 * Also a global alpha multiplier which scales every points color alpha value, which can be used to introduce transparency
 * for all points of this collection, which may come in handy to visualize density when plotting a huge amount of points.
 * 
 * @author hageldave
 */
public class Points implements Renderable {

	public final Glyph glyph;
	protected VertexArray va;
	protected boolean isDirty;
	protected float globalScaling = 1f;
	protected float globalAlphaMultiplier = 1f;
	protected ArrayList<PointDetails> points = new ArrayList<>();
	protected boolean hidden=false;
	protected boolean useVertexRounding=false;

	/**
	 * Creates a new {@link Points} object which uses the specified {@link Glyph} for displaying its points.
	 * @param glyph to be used for rendering single points
	 */
	public Points(Glyph glyph) {
		this.glyph = glyph;
	}

	/**
	 * Disposes of GL resources, i.e. deletes the vertex array.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(va)){
			va.close();
			va = null;
		}
	}

	/**
	 * Allocates GL resources, i.e. creates the vertex array
	 * and fills it according to the contents and glyph of this points object.
	 * when already initialized, nothing happens.
	 */
	@Override
	@GLContextRequired
	public void initGL() {
		if(Objects.isNull(va)){
			va = new VertexArray(4);
			glyph.fillVertexArray(va);
			updateGL();		
		}
	}

	@Override
	@Deprecated(/* use updateGLDouble() or updateGLFloat() instead */)
	public void updateGL()
	{
		if (GLUtils.USE_GL_DOUBLE_PRECISION) // SFM
		{
			updateGLDouble();	
		}
		else
		{
			updateGLFloat();
		}   
	}

	/**
	 * Updates GL resources, i.e. fills the vertex array (if non null) according to
	 * the state of this points object.
	 * This will set the {@link #isDirty()} state to false.
	 */
	@GLContextRequired
	public void updateGLFloat() {
		if(Objects.nonNull(va)){
			final int numPoints = points.size();
			float[] position = new float[numPoints*2];
			float[] rotAndScale = new float[numPoints*2];
			int[] colors = new int[numPoints*2];
			for(int i=0; i<numPoints; i++){
				PointDetails pd = points.get(i);
				position[i*2+0] = (float)pd.location.getX();
				position[i*2+1] = (float)pd.location.getY();
				rotAndScale[i*2+0] = (float) pd.rot.getAsDouble();
				rotAndScale[i*2+1] = (float) pd.scale.getAsDouble();
				colors[i*2+0] = pd.color.getAsInt();
				colors[i*2+1] = pd.pickColor;
			}
			va.setBuffer(1, 2, position);
			va.setBuffer(2, 2, rotAndScale);
			va.setBuffer(3, 2, false, colors);
			isDirty = false;
		}
	}

	/**
	 * Updates GL resources, i.e. fills the vertex array (if non null) according to
	 * the state of this points object.
	 * This will set the {@link #isDirty()} state to false.
	 */
	@GLContextRequired
	public void updateGLDouble() {
		if(Objects.nonNull(va)){
			final int numPoints = points.size();
			double[] position = new double[numPoints*2];
			float[] rotAndScale = new float[numPoints*2];
			int[] colors = new int[numPoints*2];
			for(int i=0; i<numPoints; i++){
				PointDetails pd = points.get(i);
				position[i*2+0] = pd.location.getX();
				position[i*2+1] = pd.location.getY();
				rotAndScale[i*2+0] = (float) pd.rot.getAsDouble();
				rotAndScale[i*2+1] = (float) pd.scale.getAsDouble();
				colors[i*2+0] = pd.color.getAsInt();
				colors[i*2+1] = pd.pickColor;
			}
			va.setBuffer(1, 2, position);
			va.setBuffer(2, 2, rotAndScale);
			va.setBuffer(3, 2, false, colors);
			isDirty = false;
		}
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	/**
	 * Sets the {@link #isDirty()} state to true.
	 * @return this for chaining
	 */
	public Points setDirty() {
		this.isDirty = true;
		return this;
	}
	
	/**
	 * Adds a point to this {@link Points} object.
	 * This sets the {@link #isDirty()} state to true.
	 * <p>
	 * When keeping a reference to the location and changing it later on,
	 * {@link #setDirty()} needs to be called in order for this {@link Points}
	 * object to update and reflect the changed location.
	 * @param p point location
	 * @return added PointDetails
	 */
	public PointDetails addPoint(Point2D p){
		PointDetails pd = new PointDetails(p);
		this.points.add(pd);
		setDirty();
		return pd;
	}
	
	/**
	 * Adds a point to this {@link Points} object.
	 * This sets the {@link #isDirty()} state to true.
	 * <p>
	 * When keeping a reference to the location and changing it later on,
	 * {@link #setDirty()} needs to be called in order for this {@link Points}
	 * object to update and reflect the changed location.
	 * @param x coordinate of point
	 * @param y coordinate of point
	 * @return added PointDetails
	 */
	public PointDetails addPoint(double x, double y){
		return addPoint(new Point2D.Double(x, y));
	}

	/**
	 * Clears this collection of points.
	 * This set the {@link #isDirty()} to true.
	 * @return this for chaining
	 */
	public Points removeAllPoints(){
		this.points.clear();
		return setDirty();
	}

	/**
	 * @return the number of points in this in this {@link Points} object.
	 */
	public int numPoints(){
		return points.size();
	}

	/**
	 * Sets the global scaling parameter of this {@link Points} object.
	 * The value will be multiplied with each point instance's scaling parameter when rendering.
	 * The glyph will then be scaled by the factor {@code f = globalScaling * point.scaling * renderer.scaling}.
	 * @param globalScaling of the points in this collection.
	 * @return this for chaining
	 */
	public Points setGlobalScaling(double globalScaling) {
		this.globalScaling = (float)globalScaling;
		return this;
	}

	/**
	 * @return the global scaling factor of the points in this collection.
	 */
	public float getGlobalScaling() {
		return globalScaling;
	}

	/**
	 * Sets the global alpha multiplier parameter of this {@link Points} object.
	 * The value will be multiplied with each points alpha color value when rendering.
	 * The glyph will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
	 * @param globalAlphaMultiplier of the points in this collection
	 * @return this for chaining
	 */
	public Points setGlobalAlphaMultiplier(double globalAlphaMultiplier) {
		this.globalAlphaMultiplier = (float)globalAlphaMultiplier;
		return this;
	}

	/**
	 * @return the global alpha multiplier of the points in this collection
	 */
	public float getGlobalAlphaMultiplier() {
		return globalAlphaMultiplier;
	}
	
	/**
	 * @return the bounding rectangle that encloses all points in this {@link Points} object.
	 */
	public Rectangle2D getBounds(){
		if(numPoints() < 1)
			return new Rectangle2D.Double();
		
		boolean useParallelStreaming = numPoints() > 1000;
		double minX = Utils.parallelize(getPointDetails().stream(), useParallelStreaming)
				.map(pd->pd.location)
				.mapToDouble(Point2D::getX)
				.min().getAsDouble();
		double maxX = Utils.parallelize(getPointDetails().stream(), useParallelStreaming)
				.map(pd->pd.location)
				.mapToDouble(Point2D::getX)
				.max().getAsDouble();
		double minY = Utils.parallelize(getPointDetails().stream(), useParallelStreaming)
				.map(pd->pd.location)
				.mapToDouble(Point2D::getY)
				.min().getAsDouble();
		double maxY = Utils.parallelize(getPointDetails().stream(), useParallelStreaming)
				.map(pd->pd.location)
				.mapToDouble(Point2D::getY)
				.max().getAsDouble();
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}
	
	@Override
	public boolean intersects(Rectangle2D rect) {
		boolean useParallelStreaming = numPoints() > 10000;
		return Utils.parallelize(getPointDetails().stream(), useParallelStreaming)
				.filter(p->rect.contains(p.location))
				.findAny()
				.isPresent();
	}
	
	/**
	 * Returns the points that are contained in the specified rectangle.
	 * @param rect rectangle to test intersection
	 * @return list of contained points
	 */
	public List<PointDetails> getIntersectingPoints(Rectangle2D rect) {
		boolean useParallelStreaming = numPoints() > 10000;
		return Utils.parallelize(getPointDetails().stream(), useParallelStreaming)
				.filter(p->rect.contains(p.location))
				.collect(Collectors.toList());
	}
	
	@Override
	public boolean isHidden() {
		return hidden;
	}
	
	/**
	 * Hides or unhides this Points object, i.e. sets the {@link #isHidden()} field
	 * value. When hidden, renderers will not draw it.
	 * @param hide true when hiding
	 * @return this for chaining
	 */
	public Points hide(boolean hide) {
		this.hidden = hide;
		return this;
	}
	

	/**
	 * Class for storing all the details of a single point to be rendered.
	 * This comprises location, color, scaling, glyph rotation and picking color.
	 * @author hageldave
	 */
	public static class PointDetails implements Cloneable {
		public Point2D location;
		public DoubleSupplier rot;
		public DoubleSupplier scale;
		public IntSupplier color;
		public int pickColor;
		
		public PointDetails(Point2D location) {
			this.location = location;
			this.rot = ()->0;
			this.scale = ()->1;
			this.color = ()->0xff555555;
		}
		
		/**
		 * Returns a shallow copy of this point with deep copied
		 * {@link #location}.
		 * @return copy of this point
		 */
		public PointDetails copy() {
			try {
				PointDetails clone = (PointDetails) super.clone();
				clone.location = Utils.copy(clone.location);
				return clone;
			} catch (CloneNotSupportedException e) {
				// this shouldn't happen, since we are Cloneable
				throw new InternalError(e);
			}
		}
		
		public PointDetails clone() {
			try {
				PointDetails clone = (PointDetails) super.clone();
				return clone;
			} catch (CloneNotSupportedException e) {
				// this shouldn't happen, since we are Cloneable
				throw new InternalError(e);
			}
		}
		
		/**
		 * Sets the rotation of the glyph for this point
		 * @param rot rotation in radian
		 * @return this for chaining
		 */
		public PointDetails setRotation(double rot){
			return setRotation(()->rot);
		}
		
		/**
		 * Sets the rotation of the glyph for this point
		 * @param rotation in radian
		 * @return this for chaining
		 */
		public PointDetails setRotation(DoubleSupplier rotation){
			this.rot = rotation;
			return this;
		}
		
		/**
		 * Sets the scaling of this point's glyph		
		 * @param scale scaling
		 * @return this for chaining
		 */
		public PointDetails setScaling(DoubleSupplier scale){
			this.scale = scale;
			return this;
		}
		
		/**
		 * Sets the scaling of this point's glyph		
		 * @param scale scaling
		 * @return this for chaining
		 */
		public PointDetails setScaling(double scale){
			return setScaling(()->scale);
		}
		
		/**
		 * Sets this point's color
		 * @param color integer packed ARGB color value of the glyph for the point (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public PointDetails setColor(IntSupplier color){
			this.color = color;
			return this;
		}
		
		/**
		 * Sets this point's color
		 * @param color integer packed ARGB color value of the glyph for the point (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public PointDetails setColor(int color){
			return setColor(()->color);
		}
		
		/**
		 * Sets this point's color
		 * @param color of this point's glyph
		 * @return this for chaining
		 */
		public PointDetails setColor(Color color){
			return setColor(color.getRGB());
		}
		
		/**
		 * Sets the picking color.
		 * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
		 * @param pickID picking color of the point (see {@link Points} for details)
		 * @return this for chaining
		 */
		public PointDetails setPickColor(int pickID){
			if(pickID != 0)
				pickID = pickID | 0xff000000;
			this.pickColor = pickID;
			return this;
		}
	}
	
	/**
	 * @return the list of point details.<br>
	 * Make sure to call {@link #setDirty()} when manipulating.
	 */
	public ArrayList<PointDetails> getPointDetails() {
		return points;
	}

	/**
	 * Returns the vertex array of this {@link Points} object.
	 * <br>
	 * The first attribute (index 0) of the VA contains the 2D vertices that make up
	 * the {@link Glyph} that visually represents a point.
	 * <br>
	 * The second attribute contains the 2D vertices that are the locations of all point instances
	 * in this {@link Points} object.
	 * <br>
	 * The third attribute contains 2D vertices that contain the rotation (x coordinate of vertex) 
	 * and scaling (y coordinate of vertex) for all point instances in this object (corresponding to the second attribute).
	 * <br>
	 * The fourth attribute contains 2D vertices with unsigned integer packed ARGB color (on x xoordinate) 
	 * and picking color (on y coordinate) values of all point instances.
	 * 
	 * @return the {@link VertexArray} associated with this {@link Points} object or null if
	 * {@link #initGL()} was not yet called or this object was already closed.
	 */
	public VertexArray getVertexArray() {
		return va;
	}


	/**
	 * Binds this object's vertex array and enables the corresponding attributes 
	 * (attribute indices 0,1,2,3).
	 * For attributes indices 1,2,3 (corresponding to single point details) the vertex attribute divisor
	 * is set to 1 in order for an instanced draw call to increment these after the glyph on attribute index 0
	 * has been drawn fully.
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void bindVertexArray() {
		va.bindAndEnableAttributes(0,1,2,3);
		GL33.glVertexAttribDivisor(1,1);
		GL33.glVertexAttribDivisor(2,1);
		GL33.glVertexAttribDivisor(3,1);
	}


	/**
	 * Releases this objects vertex array and disables the corresponding attributes.
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void releaseVertexArray() {
		va.releaseAndDisableAttributes(0,1,2,3);
	}

	/**
	 * @return whether vertex rounding is enabled. This indicates if
	 * the {@link PointsRenderer}'s shader will round vertex positions of 
	 * the glyph's vertices to integer values.
	 * <p>
	 * This has the effect of sharpening horizontal and vertical lines, but
	 * can affect shape. 
	 */
	public boolean isVertexRoundingEnabled() {
		return useVertexRounding;
	}

	/**
	 * En/Disables vertex rounding for this Points object. This indicates if
	 * the {@link PointsRenderer}'s shader will round vertex positions of 
	 * the glyph's vertices to integer values.
	 * <p>
	 * This has the effect of sharpening horizontal and vertical lines, but
	 * can affect shape. 
	 * <p>
	 * Also this only makes sense for some glyphs like SQUARE.
	 * @param useVertexRounding will enable if true
	 * @return this for chaining
	 */
	public Points setVertexRoundingEnabled(boolean useVertexRounding) {
		this.useVertexRounding = useVertexRounding;
		return this;
	}
	
	

}
