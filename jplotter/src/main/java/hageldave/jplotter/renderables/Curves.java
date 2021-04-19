package hageldave.jplotter.renderables;

import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

/**
 * The Curves class is a collection of cubic Bezier curves.
 * Each curve is defined by a 2D start and end point as well as two
 * control points that affect the tangents at start/end.
 * A curve also has a color and thickness attribute, as well as a
 * picking color with which the curve is rendered into the (invisible) picking color attachment
 * of an {@link FBO}. This color may serve as an identifier of the object that can be queried from a location of the
 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values) or 0.
 * <p>
 * There is also a global alpha multiplier ({@link #setGlobalAlphaMultiplier(double)}) 
 * which scales every curves color alpha value, which can be used to introduce transparency for all segments of this 
 * collection. This may come in handy to visualize density when plotting a huge amount of curves. <br>
 * Similarly, the global thickness multiplier ({@link #setGlobalThicknessMultiplier(double)}) can be used to
 * scale every curve's thickness of this Curves object by a specific factor.
 * <p>
 * The curves of this object can be rendered using a stroke pattern ({@link #setStrokePattern(int)}) to draw
 * dashed or dotted lines.
 * 
 * @author hageldave
 */
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
			va = new VertexArray(6);
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
		updateGL(1, 1, 0,3000, 0,3000);
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
	public void updateGL(double scaleX, double scaleY, double xmin, double xmax, double ymin, double ymax){
		if(Objects.nonNull(va)){
			final double sx=scaleX, sy=scaleY;
			double[] clip = new double[]{xmin,xmax,ymin,ymax};
			//final double isx=1.0/scaleX, isy=1.0/scaleY;
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
				double xc0 = seg.pc0.getX();
				double yc0 = seg.pc0.getY();
				double xc1 = seg.pc1.getX();
				double yc1 = seg.pc1.getY();
				subdivideCubicBezier(x0*sx, y0*sy, xc0*sx, yc0*sy, xc1*sx, yc1*sy, x1*sx, y1*sy, 0,1, segments, clip);
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
			float[] paramBuffer = new float[n*2];
			
			double xprev = 0, yprev=0, pathLen = 0;
			int i=0;
			for(int j=0; j<curves.size(); j++){
				CurveDetails curv = curves.get(j);
				double x0_ = curv.p0.getX();
				double y0_ = curv.p0.getY();
				double x1_ = curv.p1.getX();
				double y1_ = curv.p1.getY();
				double xc0_ = curv.pc0.getX();
				double yc0_ = curv.pc0.getY();
				double xc1_ = curv.pc1.getX();
				double yc1_ = curv.pc1.getY();
				for(int k=0; k<numSegs[j]; k++){
					// segment from subdivision (aspect ratio scaled)
					double x0 = segments.get(i*6+0);
					double y0 = segments.get(i*6+1);
					double x1 = segments.get(i*6+2);
					double y1 = segments.get(i*6+3);
					double tS = segments.get(i*6+4);
					double tE = segments.get(i*6+5);

					// start & end point
					segmentCoordBuffer[i*8+0] = (float) (x0_);
					segmentCoordBuffer[i*8+1] = (float) (y0_);
					segmentCoordBuffer[i*8+2] = (float) (x1_);
					segmentCoordBuffer[i*8+3] = (float) (y1_);
					// control point
					segmentCoordBuffer[i*8+4] = (float) (xc0_);
					segmentCoordBuffer[i*8+5] = (float) (yc0_);
					segmentCoordBuffer[i*8+6] = (float) (xc1_);
					segmentCoordBuffer[i*8+7] = (float) (yc1_);
					// parameters
					paramBuffer[i*2+0] = (float)tS;
					paramBuffer[i*2+1] = (float)tE;
							
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
			va.setBuffer(5, 1, paramBuffer);
			isDirty = false;
		}
	}
	
	private static void subdivideCubicBezier(
			double x1, double y1, 
			double x2, double y2, 
			double x3, double y3,
			double x4, double y4, 
			double tS, double tE,
			ArrayList<Double> list,
			double[] clip)
	{
		// check intersection with clip rect
		int out = 0b1111;
		out &= Utils.outcode(x1, y1, clip[0], clip[1], clip[2], clip[3]);
		out &= Utils.outcode(x2, y2, clip[0], clip[1], clip[2], clip[3]);
		out &= Utils.outcode(x3, y3, clip[0], clip[1], clip[2], clip[3]);
		out &= Utils.outcode(x4, y4, clip[0], clip[1], clip[2], clip[3]);
		if(out != 0) { /* all points are out on the same boundary */
			return;
		}
		// calc distances
		double dx12 = (x2-x1);
		double dy12 = (y2-y1);
		double dx23 = (x3-x2);
		double dy23 = (y3-y2);
		double dx34 = (x4-x3);
		double dy34 = (y4-y3);
		if(dx12*dx12+dy12*dy12 < 2.0 && dx23*dx23+dy23*dy23 < 2.0 && dx34*dx34+dy34*dy34 < 2.0){
			list.add(x1); list.add(y1);
			list.add(x4); list.add(y4);
			list.add(tS); list.add(tE);
			return;
		}
		// calc midpoint
		double xA = x1+dx12*.5;
		double yA = y1+dy12*.5;
		double xB = x2+dx23*.5;
		double yB = y2+dy23*.5;
		double xC = x3+dx34*.5;
		double yC = y3+dy34*.5;
		double xAB = xA+(xB-xA)*.5;
		double yAB = yA+(yB-yA)*.5;
		double xBC = xB+(xC-xB)*.5;
		double yBC = yB+(yC-yB)*.5;
		double x = xAB+(xBC-xAB)*.5;
		double y = yAB+(yBC-yAB)*.5;
		double t = tS+(tE-tS)*.5;
		if(tE-tS > 0.25){
			// not enough subdivisions yet (want at least 4 segments)
			subdivideCubicBezier(x1, y1, xA, yA, xAB, yAB, x, y, tS, t, list, clip);
			subdivideCubicBezier(x, y, xBC, yBC, xC, yC, x4, y4, t, tE, list, clip);
		} else {
			// calc pseudo curvature
			double ux = x-x1; double uy = y-y1; 
			double vx = x4-x; double vy = y4-y;
			double wx = x4-x1;double wy = y4-y1;
			double l1 = ux*ux+uy*uy;
			double l2 = vx*vx+vy*vy;
			double l3 = (wx*wx*.25+wy*wy*.25)*2;
			/* curvature = (l1+l2)/l3; */
			// subdivide if segments are longer than 32px (32^2=1024) or if curvature is too extreme
			if(l1 > 1024.0 || l2 > 1024.0 || (l1+l2)/l3 > 1.005 ){
				subdivideCubicBezier(x1, y1, xA, yA, xAB, yAB, x, y, tS, t, list, clip);
				subdivideCubicBezier(x, y, xBC, yBC, xC, yC, x4, y4, t, tE, list, clip);
			} else {
				list.add(x1); list.add(y1);
				list.add(x);  list.add(y);
				list.add(tS); list.add(t);

				list.add(x);  list.add(y);
				list.add(x4); list.add(y4);
				list.add(t);  list.add(tE);
			}
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
		va.bindAndEnableAttributes(0,1,2,3,4,5);
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
		return streamIntersecting(rect)
				.findAny()
				.isPresent();
	}
	
	public Stream<CurveDetails> streamIntersecting(Rectangle2D rect) {
		boolean useParallelStreaming = numCurves() > 100;
		return Utils.parallelize(getCurveDetails().stream(), useParallelStreaming)
				.filter(tri->
					Utils.rectIntersectsOrIsContainedInTri(
						rect, 
						tri.p0.getX(), tri.p0.getY(), 
						tri.pc1.getX(), tri.pc1.getY(), 
						tri.pc0.getX(), tri.pc0.getY()
						) || Utils.rectIntersectsOrIsContainedInTri(
						rect, 
						tri.p1.getX(), tri.p1.getY(), 
						tri.pc1.getX(), tri.pc1.getY(), 
						tri.pc0.getX(), tri.pc0.getY()
				));
	}

	/**
	 * @return the line thickness multiplier of this {@link Curves} object
	 */
	public float getGlobalThicknessMultiplier() {
		return (float)globalThicknessMultiplier.getAsDouble();
	}
	
	/**
	 * Sets the line thickness multiplier for this {@link Curves} object in pixels.
	 * The effective thickness of a segment results from multiplication of its 
	 * thickness with this value.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Curves setGlobalThicknessMultiplier(DoubleSupplier thickness) {
		this.globalThicknessMultiplier = thickness;
		return this;
	}

	/**
	 * Sets the line thickness multiplier for this {@link Curves} object in pixels.
	 * The effective thickness of a segment results from multiplication of its 
	 * thickness with this value.
	 * @param thickness of the lines, default is 1.
	 * @return this for chaining
	 */
	public Curves setGlobalThicknessMultiplier(double thickness) {
		return setGlobalThicknessMultiplier(() -> thickness); 
	}

	
	/**
	 * Sets the global alpha multiplier parameter of this {@link Curves} object.
	 * The value will be multiplied with each curve's alpha color value when rendering.
	 * @param globalAlphaMultiplier of the curves in this collection
	 * @return this for chaining
	 */
	public Curves setGlobalAlphaMultiplier(DoubleSupplier globalAlphaMultiplier) {
		this.globalAlphaMultiplier = globalAlphaMultiplier;
		return this;
	}
	
	/**
	 * Sets the global alpha multiplier parameter of this {@link Curves} object.
	 * The value will be multiplied with each curve's alpha color value when rendering.
	 * @param globalAlphaMultiplier of the curves in this collection
	 * @return this for chaining
	 */
	public Curves setGlobalAlphaMultiplier(double globalAlphaMultiplier) {
		return setGlobalAlphaMultiplier(() -> globalAlphaMultiplier);
	}

	/**
	 * @return the global alpha multiplier of the curves in this collection
	 */
	public float getGlobalAlphaMultiplier() {
		return (float)globalAlphaMultiplier.getAsDouble();
	}
	
	/**
	 * Sets this Curves object's stroke pattern.
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
	public Curves setStrokePattern(int strokePattern) {
		if(strokePattern >> 16 != 0){
			System.err.println("specified stroke pattern should only be 16 bits but is " + Integer.toBinaryString(strokePattern));
		}
		this.strokePattern = (short)strokePattern;
		return this;
	}

	/**
	 * Returns this {@link Curves} object's stroke pattern
	 * @return stroke pattern
	 */
	public short getStrokePattern() {
		return strokePattern;
	}

	/**
	 * Returns the stroke length in pixels, which is by default 16 pixels.
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
	public Curves setStrokeLength(double strokeLength) {
		this.strokeLength = (float) Math.max(0, strokeLength);
		return this;
	}
	
	/**
	 * Whether this Curves object has a stroke pattern other than 0xffff (completely solid).
	 * @return true when stroke pattern != 0xffff
	 */
	public boolean hasStrokePattern() {
		return this.strokePattern != (short)0xffff;
	}

	/**
	 * @return the number of curve elements contained in this object.
	 */
	public int numCurves() {
		return curves.size();
	}
	
	/**
	 * @return the individual curve detail objects contained in this object
	 */
	public ArrayList<CurveDetails> getCurveDetails() {
		return curves;
	}

	/**
	 * Adds a curve to the collection
	 * @param cd curve to add
	 * @return the argument
	 */
	public CurveDetails addCurve(CurveDetails cd){
		this.curves.add(cd);
		setDirty();
		return cd;
	}
	
	/**
	 * Adds a cubic Bezier curve to this {@link Curves} object. 
	 * @param p0 start point
	 * @param cp0 1st control point (affects tangent direction from start)
	 * @param cp1 2nd control point (affects tangent direction to end)
	 * @param p1 end point
	 * @return the added {@link CurveDetails} object for further settings
	 */
	public CurveDetails addCurve(Point2D p0, Point2D cp0,Point2D cp1, Point2D p1){
		return this.addCurve(new CurveDetails(p0, cp0,cp1, p1));
	}
	
	/**
	 * Adds a cubic Bezier curve to this {@link Curves} object. 
	 * @param x0 start point x
	 * @param y0 start point y
	 * @param x1 1st control point x
	 * @param y1 1st control point y
	 * @param x2 2nd control point x
	 * @param y2 2nd control point y
	 * @param x3 end point x
	 * @param y3 end point y
	 * @return the added {@link CurveDetails} object for further settings
	 */
	public CurveDetails addCurve(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3){
		return this.addCurve(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), new Point2D.Double(x3, y3));
	}
	
	/**
	 * Adds a sequence of Bezier curves to this {@link Curves} object.
	 * The first point is the start point, followed by a sequence of point triplets 
	 * (2 control points and 1 intermediate/end point).
	 * The number of specified points has to be 1+n*3 for n curves.
	 * @param points sequence of points and control points defining the bezier curves to be added
	 * @return list of added {@link CurveDetails}
	 * @throws IllegalArgumentException when number of specified points is wrong.
	 */
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
	
	/**
	 * Adds a sequence of Bezier curves to this {@link Curves} object.
	 * The first point is the start point, followed by a sequence of point triplets 
	 * (2 control points and 1 intermediate/end point).
	 * The number of specified points has to be 1+n*3 (2+n*6 coordinates) for n curves.
	 * @param coords sequence of point and control point coordinates defining the bezier curves to be added. 
	 * Coordinates are specified as (x,y) pairs so that x coordinates have even (y coordinates odd) indices.
	 * @return list of added {@link CurveDetails}
	 * @throws IllegalArgumentException when number of specified points is wrong or number of coordinates is odd.
	 */
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
	
	/**
	 * Adds a sequence of Bezier curves to smoothly connect the specified points.
	 * The control points are calculated in such a way that the tangents are perpendicular
	 * to the angle bisector between 3 consecutive points.
	 * @param points the points through which a continuous smooth curve is created using
	 * a cubic Bezier curve between each consecutive pair of points. 
	 * @return list of added {@link CurveDetails}
	 */
	public ArrayList<CurveDetails> addCurvesThrough(Point2D... points){
		int nPoints = points.length;
		int nControl = (nPoints-1)*2;
		Point2D[] curves = new Point2D[nPoints+nControl];
		final double eps=1e-6;
		for(int i=0; i<nPoints; i++) {
			int i_prev = i-1;
			int i_next = i+1;
			Point2D curr = points[i];
			Point2D prev = i_prev < 0        ? curr:points[i_prev];
			Point2D next = i_next >= nPoints ? curr:points[i_next];
			// vector coords
			double xA=prev.getX();
			double yA=prev.getY();
			double xB=curr.getX();
			double yB=curr.getY();
			double xC=next.getX();
			double yC=next.getY();
			// connecting vectors
			double xBA = xA-xB;
			double yBA = yA-yB;
			double xBC = xC-xB;
			double yBC = yC-yB;
			double xAC = xC-xA;
			double yAC = yC-yA;
			
			if(i==0) {
				curves[i*3+0]=curr;
				curves[i*3+1]=new Point2D.Double(xB+.3*xBC, yB+.3*yBC);
				continue;
			}
			if(i==nPoints-1) {
				curves[i*3+0]=curr;
				curves[i*3-1]=new Point2D.Double(xB+.3*xBA, yB+.3*yBA);
				continue;
			}
			
			// normalization
			double lBA = Utils.hypot(xBA, yBA);
			double lBC = Utils.hypot(xBC, yBC);
			double lAC = Utils.hypot(xAC, yAC);
			if(lBA > eps) {xBA/=lBA; yBA/=lBA;}
			if(lBC > eps) {xBC/=lBC; yBC/=lBC;}
			if(lAC > eps) {xAC/=lAC; yAC/=lAC;}
			// halfway vector & tangent
			double xH=xBA+xBC;
			double yH=yBA+yBC;
			double lH=Utils.hypot(xH, yH);
			double xT,yT;
			if(lH > eps) {
				xH/=lH; yH/=lH;
				// rotate clckws
				xT=yH; yT=-xH;
				// check if correctly rotated
				if(xT*xBA+yT*yBA > eps){
					xT=-xT;
					yT=-yT;
				}
				if(lAC < eps && i > 0){
					Point2D ctrl_prev = curves[i*3-2];
					double xBP=ctrl_prev.getX()-xB;
					double yBP=ctrl_prev.getY()-yB;
					if(xT*xBP+yT*yBP > eps){
						xT=-xT;
						yT=-yT;
					}
				}
			} else {
				xT=xBC; yT=yBC;
			}
			
			curves[i*3+0]=curr;
			curves[i*3-1]=new Point2D.Double(xB-xT*.3*lBA, yB-yT*.3*lBA);
			curves[i*3+1]=new Point2D.Double(xB+xT*.3*lBC, yB+yT*.3*lBC);
		}
		return addCurveStrip(curves);
	}

	/**
	 * Adds a straight line to this Curves object.
	 * The specified start and end point are used as their respective control points.
	 * @param p0 start point
	 * @param p1 end point
	 * @return the added {@link CurveDetails} object
	 */
	public CurveDetails addStraight(Point2D p0, Point2D p1){
		return addCurve(new CurveDetails(p0,p0,p1,p1));
	}
	
	/**
	 * Adds a straight line to this Curves object.
	 * The specified start and end point are used as their respective control points.
	 * @param x0 start point x
	 * @param y0 start point y
	 * @param x1 end point x
	 * @param y1 end point y
	 * @return the added {@link CurveDetails} object
	 */
	public CurveDetails addStraight(double x0, double y0, double x1, double y1){
		return addStraight(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1));
	}
	
	/**
	 * Removes all curves of this object.
	 * Sets the {@link #isDirty()} state to true.
	 * @return this for chaining
	 */
	public Curves removeAllCurves() {
		this.curves.clear();
		return this.setDirty();
	}
	
	/**
	 * Specification of a cubic Bezier curve element, comprising start/end point positions,
	 * control point positions, as well as, line color, picking color and thickness.
	 * @author hageldave
	 */
	public static class CurveDetails implements Cloneable {
		protected static final DoubleSupplier[] PREDEFINED_THICKNESSES = new DoubleSupplier[]
				{()->0f, ()->1f, ()->2f, ()->3f, ()->4f};
		
		public Point2D p0;
		public Point2D pc0;
		public Point2D pc1;
		public Point2D p1;
		public IntSupplier color;
		public DoubleSupplier thickness = PREDEFINED_THICKNESSES[1];
		public int pickColor;
		
		public CurveDetails(Point2D p0, Point2D pc0, Point2D pc1, Point2D p1) {
			this.p0=p0;
			this.p1=p1;
			this.pc0=pc0;
			this.pc1=pc1;
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
			clone.pc0 = Utils.copy(clone.pc0);
			clone.pc1 = Utils.copy(clone.pc1);
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
		
		/**
		 * Sets the picking color.
		 * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
		 * @param pickID picking color of the curve (see {@link Curves} for details)
		 * @return this for chaining
		 */
		public CurveDetails setPickColor(int pickID){
			if(pickID != 0)
				pickID = pickID | 0xff000000;
			this.pickColor = pickID;
			return this;
		}

		/**
		 * Sets the color of the curve
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public CurveDetails setColor(IntSupplier color){
			this.color = color;
			return this;
		}
		
		/**
		 * Sets the color of the curve
		 * @param color integer packed ARGB color value (e.g. 0xff00ff00 = opaque green)
		 * @return this for chaining
		 */
		public CurveDetails setColor(int color){
			return setColor(()->color);
		}
		
		/**
		 * Sets the color of the curve
		 * @param color of the curve
		 * @return this for chaining
		 */
		public CurveDetails setColor(Color color){
			return setColor(color.getRGB());
		}
		
		public CurveDetails setThickness(DoubleSupplier t){
			this.thickness = t;
			return this;
		}
		
		public CurveDetails setThickness(double t){
			return setThickness(sup4thick(t));
		}
		
		protected static DoubleSupplier sup4thick(double t){
			if( t == ((int)t) && t >= 0 && t < PREDEFINED_THICKNESSES.length){
				return PREDEFINED_THICKNESSES[(int)t];
			}
			return ()->t;
		}
	}
}
