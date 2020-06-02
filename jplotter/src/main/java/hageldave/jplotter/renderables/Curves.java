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
	
	
	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return true;
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
			va.setBuffer(0, 2, segmentCoordBuffer);
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
	}

}
