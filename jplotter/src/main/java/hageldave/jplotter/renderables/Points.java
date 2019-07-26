package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL33;

import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.misc.Glyph;
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

	/**
	 * Updates GL resources, i.e. fills the vertex array (if non null) according to
	 * the state of this points object.
	 * This will set the {@link #isDirty()} state to false.
	 */
	@Override
	@GLContextRequired
	public void updateGL() {
		if(Objects.nonNull(va)){
			final int numPoints = points.size();
			float[] position = new float[numPoints*2];
			float[] rotAndScale = new float[numPoints*2];
			int[] colors = new int[numPoints*2];
			for(int i=0; i<numPoints; i++){
				PointDetails pd = points.get(i);
				position[i*2+0] = (float)pd.location.getX();
				position[i*2+1] = (float)pd.location.getY();
				rotAndScale[i*2+0] = pd.rot;
				rotAndScale[i*2+1] = pd.scale;
				colors[i*2+0] = pd.color;
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
	 * @param rot rotation of the glyph for the point in radian
	 * @param scale scaling of the glyph for the point
	 * @param color integer packed ARGB color value of the glyph for the point (e.g. 0xff00ff00 = opaque green)
	 * @param pick picking color of the point (see {@link Points} for details)
	 * @return this for chaining
	 */
	public Points addPoint(Point2D p, double rot, double scale, int color, int pick){
		this.points.add(new PointDetails(p, (float)rot, (float)scale, color, pick));
		return setDirty();
	}

	/**
	 * Adds a point to this {@link Points} object.
	 * This sets the {@link #isDirty()} state to true.
	 * @param px x coordinate of point
	 * @param py y coordinate of point
	 * @param rot rotation of the glyph for the point in radian
	 * @param scale scaling of the glyph for the point
	 * @param color integer packed ARGB color value of the glyph for the point (e.g. 0xff00ff00 = opaque green)
	 * @param pick picking color of the point (see {@link Points} for details)
	 * @return this for chaining
	 */
	public Points addPoint(double px, double py, double rot, double scale, int color, int pick){
		return addPoint(new Point2D.Double(px, py), rot, scale, color, pick);
	}

	/**
	 * Adds a point to this {@link Points} object.
	 * This sets the {@link #isDirty()} state to true.
	 * @param px x coordinate of point
	 * @param py y coordinate of point
	 * @param rot rotation of the glyph for the point in radian
	 * @param scale scaling of the glyph for the point
	 * @param color integer packed ARGB color value of the glyph for the point (e.g. 0xff00ff00 = opaque green)
	 * @return this for chaining
	 */
	public Points addPoint(double px, double py, double rot, double scale, int color){
		return addPoint(px, py, rot, scale, color, 0);
	}


	/**
	 * Adds a point to this {@link Points} object.
	 * This sets the {@link #isDirty()} state to true.
	 * @param px x coordinate of point
	 * @param py y coordinate of point
	 * @param rot rotation of the glyph for the point in radian
	 * @param scale scaling of the glyph for the point
	 * @param color of the glyph for the point
	 * @return this for chaining
	 */
	public Points addPoint(double px, double py, double rot, double scale, Color color){
		return addPoint(px, py, rot, scale, color.getRGB(), 0);
	}

	/**
	 * Adds a point to this {@link Points} object.
	 * This sets the {@link #isDirty()} state to true.
	 * @param px x coordinate of point
	 * @param py y coordinate of point
	 * @param color of the glyph for the point
	 * @return this for chaining
	 */
	public Points addPoint(double px, double py, Color color){
		return addPoint(px, py, 0, 1, color.getRGB(), 0);
	}

	/**
	 * Adds a point to this {@link Points} object.
	 * This sets the {@link #isDirty()} state to true.
	 * @param px x coordinate of point
	 * @param py y coordinate of point
	 * @return this for chaining
	 */
	public Points addPoint(double px, double py){
		return addPoint(px, py, 0, 1, 0xff555555, 0);
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
	
	public List<PointDetails> getIntersectingPoints(Rectangle2D rect) {
		boolean useParallelStreaming = numPoints() > 10000;
		return Utils.parallelize(getPointDetails().stream(), useParallelStreaming)
				.filter(p->rect.contains(p.location))
				.collect(Collectors.toList());
	}

	/**
	 * Class for storing all the details of a single point to be rendered.
	 * This comprises location, color, scaling, glyph rotation and picking color.
	 * @author hageldave
	 */
	public static class PointDetails {
		public Point2D location;
		public float rot;
		public float scale;
		public int color;
		public int pickColor;
		
		public PointDetails(Point2D location, float rot, float scale, int color, int pickColor) {
			this.location = location;
			this.rot = rot;
			this.scale = scale;
			this.color = color;
			if(pickColor != 0)
				pickColor = pickColor | 0xff000000;
			this.pickColor = pickColor;
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

}
