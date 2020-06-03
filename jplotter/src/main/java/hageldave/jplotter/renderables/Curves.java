package hageldave.jplotter.renderables;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.Utils;

public class Curves implements Renderable {

	protected VertexArray va;
	protected ArrayList<CurveDetails> curves = new ArrayList<>();
	protected short strokePattern = (short)0xffff;
	protected float strokeLength = 16;
	protected boolean isDirty;
	protected boolean hidden = false;
	protected DoubleSupplier globalAlphaMultiplier = () -> 1.0;
	protected DoubleSupplier globalThicknessMultiplier = () -> 1.0;
	
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	/**
	 * Sets the {@link #isDirty()} state of this renderable to true.
	 * This indicates that an {@link #updateGL()} call is necessary to sync GL resources.
	 * @return this for chaining
	 */
	public Curves setDirty() {
		this.isDirty = true;
		return this;
	}
	
	@Override
	public void initGL() {
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
			float[] segmentCoordBuffer = new float[curves.size()*4*2];
			int[] colorBuffer = new int[curves.size()*2];
			int[] pickBuffer = new int[curves.size()*2];
			float[] thicknessBuffer = new float[curves.size()*2];
			float[] pathLengthBuffer = new float[curves.size()*2];
			
			double xprev = 0, yprev=0, pathLen = 0;
			for(int i=0; i<curves.size(); i++){
				CurveDetails seg = curves.get(i);
				double x0 = seg.p0.getX();
				double y0 = seg.p0.getY();
				double x1 = seg.p1.getX();
				double y1 = seg.p1.getY();
				double xc0 = seg.pc0.getX();
				double yc0 = seg.pc0.getY();
				double xc1 = seg.pc1.getX();
				double yc1 = seg.pc1.getY();
				
				// start & end point
				segmentCoordBuffer[i*8+0] = (float) x0;
				segmentCoordBuffer[i*8+1] = (float) y0;
				segmentCoordBuffer[i*8+2] = (float) x1;
				segmentCoordBuffer[i*8+3] = (float) y1;
				// control points
				segmentCoordBuffer[i*8+4] = (float) xc0;
				segmentCoordBuffer[i*8+5] = (float) yc0;
				segmentCoordBuffer[i*8+6] = (float) xc1;
				segmentCoordBuffer[i*8+7] = (float) yc1;

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
			va.setBuffer(0, 4, segmentCoordBuffer);
			va.setBuffer(1, 1, false, colorBuffer);
			va.setBuffer(2, 1, false, pickBuffer);
			va.setBuffer(3, 1, thicknessBuffer);
			va.setBuffer(4, 1, pathLengthBuffer);
			isDirty = false;
		}
	}

	@Override
	public void close() {
		if(Objects.nonNull(va)){
			va.close();
			va = null;
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


	@Override
	public boolean intersects(Rectangle2D rect) {
		// TODO Auto-generated method stub
		return true;
	}
	
	public static class CurveDetails {
		protected static final DoubleSupplier[] PREDEFINED_THICKNESSES = new DoubleSupplier[]
				{()->0f, ()->1f, ()->2f, ()->3f, ()->4f};
		
		public Point2D p0;
		public Point2D pc0;
		public Point2D pc1;
		public Point2D p1;
		public IntSupplier color0;
		public IntSupplier color1;
		public DoubleSupplier thickness0 = PREDEFINED_THICKNESSES[1];
		public DoubleSupplier thickness1 = PREDEFINED_THICKNESSES[1];
		public int pickColor;
		
		public CurveDetails(Point2D p0, Point2D pc0, Point2D pc1, Point2D p1) {
			this.p0=p0;
			this.p1=p1;
			this.pc0=pc0;
			this.pc1=pc1;
			this.color0 = this.color1 = ()->0xff555555;
		}
		
	}

	public float getGlobalThicknessMultiplier() {
		return (float)globalThicknessMultiplier.getAsDouble();
	}
	
	/**
	 * Sets the line thickness for this {@link Lines} object in pixels.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Curves setGlobalThicknessMultiplier(DoubleSupplier thickness) {
		this.globalThicknessMultiplier = thickness;
		return this;
	}

	/**
	 * Sets the line thickness for this {@link Lines} object in pixels.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Curves setGlobalThicknessMultiplier(double thickness) {
		return setGlobalThicknessMultiplier(() -> thickness); 
	}

	
	public float getGlobalAlphaMultiplier() {
		return (float)globalAlphaMultiplier.getAsDouble();
	}

	public boolean isVertexRoundingEnabled() {
		return false;
	}

	public short getStrokePattern() {
		return strokePattern;
	}

	public float getStrokeLength() {
		return strokeLength;
	}
	
	/**
	 * Whether this Lines object has a stroke pattern other than 0xffff (completely solid).
	 * @return true when stroke pattern != 0xffff
	 */
	public boolean hasStrokePattern() {
		return this.strokePattern != (short)0xffff;
	}

	public int numSegments() {
		return curves.size();
	}
	
	public CurveDetails addCurve(CurveDetails cd){
		this.curves.add(cd);
		setDirty();
		return cd;
	}
	
	public CurveDetails addCurve(Point2D p0, Point2D cp0, Point2D cp1, Point2D p1){
		return this.addCurve(new CurveDetails(p0, cp0, cp1, p1));
	}
	
	public CurveDetails addCurve(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3){
		return this.addCurve(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), new Point2D.Double(x3, y3));
	}
	
	public ArrayList<CurveDetails> addCurveStrip(Point2D... points){
		if((points.length-1)%3 != 0){
			throw new IllegalArgumentException("Not enough points for curve strip. Need 4+n*3, but provided " + points.length + ", missing " + (3-((points.length-1)%3)) + ".");
		}
		int n = points.length/3;
		ArrayList<CurveDetails> curves = new ArrayList<>(n);
		for(int i=0; i < n; i++){
			curves.add(this.addCurve(points[i*3+0], points[i*3+1], points[i*3+2], points[i*3+3]));
		}
		return curves;
	}
	
	public ArrayList<CurveDetails> addCurveStrip(double... coords){
		if(coords.length % 2 != 0){
			throw new IllegalArgumentException("Need to provide even number of coordinates. Provided " + coords.length);
		}
		int m = coords.length/2;
		if((m-1)%3 != 0){
			throw new IllegalArgumentException("Not enough points for curve strip. Need 4+n*3, but provided " + m + ", missing " + (3-((m-1)%3)) + ".");
		}
		int n = m/3;
		ArrayList<CurveDetails> curves = new ArrayList<>(n);
		for(int i=0; i < n; i++){
			curves.add(this.addCurve(coords[i*6+0],coords[i*6+1], coords[i*6+2],coords[i*6+3], coords[i*6+4],coords[i*6+5], coords[i*6+6],coords[i*6+7]));
		}
		return curves;
	}

	public CurveDetails addStraight(Point2D p0, Point2D p1){
		return addCurve(new CurveDetails(p0,p0,p1,p1));
	}
	
	public CurveDetails addStraight(double x0, double y0, double x1, double y1){
		return addStraight(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1));
	}
	
}
