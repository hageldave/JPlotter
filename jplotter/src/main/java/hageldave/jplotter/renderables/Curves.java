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
	protected int numEffectiveSegments = 0;
	
	
	public int getNumEffectiveSegments() {
		return numEffectiveSegments;
	}
	
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
			final double sx=scaleX, sy=scaleY, isx=1.0/scaleX, isy=1.0/scaleY;
			// subdivide bezier curves
			ArrayList<Double> segments = new ArrayList<>(curves.size()*6*32);
			int[] numSegs = new int[curves.size()];
			int n = 0;
			for(int i=0; i<curves.size(); i++){
				CurveDetails seg = curves.get(i);
				double x0 = seg.p0.getX();
				double y0 = seg.p0.getY();
				double x1 = seg.p1.getX();
				double y1 = seg.p1.getY();
				double xc = seg.pc.getX();
				double yc = seg.pc.getY();
				subdivideCubicBezier(x0*sx, y0*sy, xc*sx, yc*sy, x1*sx, y1*sy, 0,1, segments);
				int segs = (segments.size()/6)-n;
				numSegs[i] = segs;
				n += segs;
			}
			this.numEffectiveSegments = n;
			
			// create buffers for vertex array
			float[] segmentCoordBuffer = new float[n*8];
			int[] colorBuffer = new int[n*2];
			int[] pickBuffer = new int[n*2];
			float[] thicknessBuffer = new float[n*2];
			float[] pathLengthBuffer = new float[n*2];
			
			double xprev = 0, yprev=0, pathLen = 0;
			int i=0;
			for(int j=0; j<curves.size(); j++){
				CurveDetails curv = curves.get(j);
				for(int k=0; k<numSegs[j]; k++){
					double x0 = segments.get(i*6+0);
					double y0 = segments.get(i*6+1);
					double xc = segments.get(i*6+2);
					double yc = segments.get(i*6+3);
					double x1 = segments.get(i*6+4);
					double y1 = segments.get(i*6+5);

					// start & end point
					segmentCoordBuffer[i*8+0] = (float) (x0*isx);
					segmentCoordBuffer[i*8+1] = (float) (y0*isy);
					segmentCoordBuffer[i*8+2] = (float) (x1*isx);
					segmentCoordBuffer[i*8+3] = (float) (y1*isy);
					// control point
					segmentCoordBuffer[i*8+4] = (float) (xc*isx);
					segmentCoordBuffer[i*8+5] = (float) (yc*isy);
					segmentCoordBuffer[i*8+6] = 0f;
					segmentCoordBuffer[i*8+7] = 0f;
							
					int color = curv.color.getAsInt();
					colorBuffer[i*2+0] = color;
					colorBuffer[i*2+1] = color;

					pickBuffer[i*2+0] = pickBuffer[i*2+1] = curv.pickColor;

					float thickness = (float)curv.thickness.getAsDouble();
					thicknessBuffer[i*2+0] = thickness;
					thicknessBuffer[i*2+1] = thickness;

					if(xprev != x0 || yprev != y0){
						pathLen = 0;
					}
					double segLen = Utils.hypot((x1-x0), (y1-y0));
					pathLengthBuffer[i*2+0] = (float)pathLen;
					pathLengthBuffer[i*2+1] = (float)(pathLen += segLen);
					pathLen = pathLen % strokeLength;
					xprev = x1; yprev = y1;
					
					i++;
				}
			}
			va.setBuffer(0, 4, segmentCoordBuffer);
			va.setBuffer(1, 1, false, colorBuffer);
			va.setBuffer(2, 1, false, pickBuffer);
			va.setBuffer(3, 1, thicknessBuffer);
			va.setBuffer(4, 1, pathLengthBuffer);
			isDirty = false;
		}
	}
	
	private static void subdivideCubicBezier(
			double x1, double y1, 
			double x2, double y2, 
			double x3, double y3, 
			double tS, double tE,
			ArrayList<Double> list)
	{
		// calc distances
		double dx12 = (x2-x1);
		double dy12 = (y2-y1);
		double dx23 = (x3-x2);
		double dy23 = (y3-y2);
		if(dx12*dx12+dy12*dy12 < 2.0 && dx23*dx23+dy23*dy23 < 2.0){
			list.add(x1); list.add(y1);
			list.add(x2); list.add(y2);
			list.add(x3); list.add(y3);
			return;
		}
		// calc midpoint
		double xA = x1+dx12*.5;
		double yA = y1+dy12*.5;
		double xB = x2+dx23*.5;
		double yB = y2+dy23*.5;
		double x = xA+(xB-xA)*.5;
		double y = yA+(yB-yA)*.5;
		double t = tS+(tE-tS)*.5;
		// calc pseudo curvature
		double ux = x-x1; double uy = y-y1; 
		double vx = x3-x; double vy = y3-y;
		double wx = x3-x1;double wy = y3-y1;
		double l1 = ux*ux+uy*uy;
		double l2 = vx*vx+vy*vy;
		double l3 = (wx*wx*.25+wy*wy*.25)*2;
		/* curvature = (l1+l2)/l3; */
		// subdivide if segments are longer than 32px (32^2=1024) or if curvature is too extreme
		if(l1 > 1024.0 || l2 > 1024.0 || (l1+l2)/l3 > 1.005 ){
			subdivideCubicBezier(x1, y1, xA, yA, x, y, tS, t, list);
			subdivideCubicBezier(x, y, xB, yB, x3, y3, t, tE, list);
		} else {
			list.add(x1); list.add(y1);
			list.add(xA); list.add(yA);
			list.add(x); list.add(y);
			
			list.add(x); list.add(y);
			list.add(xB); list.add(yB);
			list.add(x3); list.add(y3);
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
		boolean useParallelStreaming = numCurves() > 1000;
		return Utils.parallelize(getCurveDetails().stream(), useParallelStreaming)
				.filter(tri->Utils.rectIntersectsOrIsContainedInTri(
						rect, 
						tri.p0.getX(), tri.p0.getY(), 
						tri.p1.getX(), tri.p1.getY(), 
						tri.pc.getX(), tri.pc.getY()
						))
				.findAny()
				.isPresent();
	}
	
	public static class CurveDetails implements Cloneable {
		protected static final DoubleSupplier[] PREDEFINED_THICKNESSES = new DoubleSupplier[]
				{()->0f, ()->1f, ()->2f, ()->3f, ()->4f};
		
		public Point2D p0;
		public Point2D pc;
		public Point2D p1;
		public IntSupplier color;
		public DoubleSupplier thickness = PREDEFINED_THICKNESSES[1];
		public int pickColor;
		
		public CurveDetails(Point2D p0, Point2D pc0, Point2D p1) {
			this.p0=p0;
			this.p1=p1;
			this.pc=pc0;
			this.color = ()->0xff555555;
		}
		
		/**
		 * Returns a shallow copy of this curve with deep copied
		 * positions {@link #p0} and {@link #p1}.
		 * @return copy of this curve
		 */
		public CurveDetails copy() {
			CurveDetails clone = this.clone();
			clone.p0 = Utils.copy(clone.p0);
			clone.pc = Utils.copy(clone.pc);
			clone.p1 = Utils.copy(clone.p1);
			return clone;
		}
		
		@Override
		public CurveDetails clone() {
			try {
				return (CurveDetails) super.clone();
			} catch (CloneNotSupportedException e) {
				// should never happen since cloneable
				throw new InternalError(e);
			}
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

	public int numCurves() {
		return curves.size();
	}
	
	public ArrayList<CurveDetails> getCurveDetails() {
		return curves;
	}
	
	public CurveDetails addCurve(CurveDetails cd){
		this.curves.add(cd);
		setDirty();
		return cd;
	}
	
	public CurveDetails addCurve(Point2D p0, Point2D cp0, Point2D p1){
		return this.addCurve(new CurveDetails(p0, cp0, p1));
	}
	
	public CurveDetails addCurve(double x0, double y0, double x1, double y1, double x2, double y2){
		return this.addCurve(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
	}
	
	public ArrayList<CurveDetails> addCurveStrip(Point2D... points){
		if((points.length%2) != 1){
			throw new IllegalArgumentException("Not enough points for curve strip. Need 3+n*2, but provided " + points.length + ", missing 1.");
		}
		int n = points.length/2;
		ArrayList<CurveDetails> curves = new ArrayList<>(n);
		for(int i=0; i < n; i++){
			curves.add(this.addCurve(points[i*2+0], points[i*2+1], points[i*2+2]));
		}
		return curves;
	}
	
	public ArrayList<CurveDetails> addCurveStrip(double... coords){
		if(coords.length % 2 != 0){
			throw new IllegalArgumentException("Need to provide even number of coordinates. Provided " + coords.length);
		}
		int m = coords.length/2;
		if((m%2) != 1){
			throw new IllegalArgumentException("Not enough points for curve strip. Need 3+n*2, but provided " + m + ", missing 1.");
		}
		int n = m/2;
		ArrayList<CurveDetails> curves = new ArrayList<>(n);
		for(int i=0; i < n; i++){
			curves.add(this.addCurve(coords[i*4+0],coords[i*4+1], coords[i*4+2],coords[i*4+3], coords[i*4+4],coords[i*4+5]));
		}
		return curves;
	}

	public CurveDetails addStraight(Point2D p0, Point2D p1){
		return addCurve(new CurveDetails(p0,p0,p1));
	}
	
	public CurveDetails addStraight(double x0, double y0, double x1, double y1){
		return addStraight(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1));
	}
	
}
