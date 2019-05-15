package jplotter.globjects;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Objects;

import org.lwjgl.opengl.GL33;

public class Triangles implements Renderable {

	VertexArray va;
	boolean isDirty;
	ArrayList<TriangleDetails> triangles = new ArrayList<>();
	
	
	public Triangles() {	
		// nothing to do
	}
	
	public Triangles addTriangle(
			double x0, double y0, int c0, 
			double x1, double y1, int c1, 
			double x2, double y2, int c2,
			int pick
	){
		this.triangles.add(new TriangleDetails((float)x0, (float)y0, c0, (float)x1, (float)y1, c1, (float)x2, (float)y2, c2, pick));
		return this;
	}
	
	public Triangles addTriangle(
			double x0, double y0, int c0, 
			double x1, double y1, int c1, 
			double x2, double y2, int c2
	){
		return this.addTriangle(x0, y0, c0, x1, y1, c1, x2, y2, c2, 0);
	}
	
	public Triangles addTriangle(
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2,
			Color color, int pickColor
	){
		int c = color.getRGB();
		return this.addTriangle(x0, y0, c, x1, y1, c, x2, y2, c, pickColor);
	}
	
	public Triangles addTriangle(
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2,
			Color color
	){
		int c = color.getRGB();
		return this.addTriangle(x0, y0, c, x1, y1, c, x2, y2, c);
	}
	
	public Triangles addTriangle(
			Point2D p0, Point2D p1,Point2D p2,
			Color color, int pickColor
	){
		int c = color.getRGB();
		return this.addTriangle(p0.getX(), p0.getY(), c, p1.getX(), p1.getY(), c, p2.getX(), p2.getY(), c, pickColor);
	}
	
	public Triangles addTriangle(
			Point2D p0, Point2D p1,Point2D p2,
			Color color
	){
		int c = color.getRGB();
		return this.addTriangle(p0.getX(), p0.getY(), c, p1.getX(), p1.getY(), c, p2.getX(), p2.getY(), c);
	}
	
	public Triangles addTriangle(
			double x0, double y0, 
			double x1, double y1, 
			double x2, double y2
	){
		int c = 0xffaaaaaa;
		return this.addTriangle(x0, y0, c, x1, y1, c, x2, y2, c);
	}
	
	public Triangles addTriangle(Point2D p0, Point2D p1,Point2D p2){
		return this.addTriangle(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY());
	}
	
	public Triangles addQuad(
			double x0, double y0,
			double x1, double y1,
			double x2, double y2,
			double x3, double y3,
			Color color, int pickColor
	){
		return this
				.addTriangle(x0, y0, x1, y1, x2, y2, color, pickColor)
				.addTriangle(x3, y3, x2, y2, x1, y1, color, pickColor);
	}
	
	public Triangles addQuad(
			double x0, double y0,
			double x1, double y1,
			double x2, double y2,
			double x3, double y3,
			Color color
	){
		return this.addQuad(x0, y0, x1, y1, x2, y2, x3, y3, color, 0);
	}
	
	public Triangles addQuad(
			double x0, double y0,
			double x1, double y1,
			double x2, double y2,
			double x3, double y3
	){
		return this
				.addTriangle(x0, y0, x1, y1, x2, y2)
				.addTriangle(x3, y3, x2, y2, x1, y1);
	}
	
	public Triangles addQuad(Point2D p0, Point2D p1, Point2D p2, Point2D p3, Color color){
		return this.addQuad(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY(), color);
	}
	
	public Triangles addQuad(Point2D p0, Point2D p1, Point2D p2, Point2D p3){
		return this.addQuad(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
	}
	
	public Triangles addStrip(Color color, int pickColor, Point2D ... points){
		if(points.length < 3){
			throw new IllegalArgumentException("not enough points for triangle strip, need at least 3 but got " + points.length);
		}
		for(int i = 0; i < points.length-2; i++){
			addTriangle(points[i], points[i+1], points[i+2], color, pickColor);
		}
		return this;
	}
	
	public Triangles addStrip(Color color, Point2D ... points){
		return this.addStrip(color, 0, points);
	}
	
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
	

	@Override
	public void close() {
		if(Objects.nonNull(va))
			va.close();
		va = null;
	}

	@Override
	public void initGL() {
		if(Objects.isNull(va)){
			va = new VertexArray(3);
		}
	}

	@Override
	public void updateGL() {
		if(Objects.nonNull(va)){
			final int numTris = triangles.size();
			float[] vertices = new float[numTris*2*3];
			int[] vColors = new int[numTris*3];
			int[] pickColor = new int[numTris];
			for(int i=0; i<numTris; i++){
				TriangleDetails tri = triangles.get(i);

				vertices[i*6+0] = tri.x0;
				vertices[i*6+1] = tri.y0;
				vertices[i*6+2] = tri.x1;
				vertices[i*6+3] = tri.y1;
				vertices[i*6+4] = tri.x2;
				vertices[i*6+5] = tri.y2;

				vColors[i*3+0] = tri.c0;
				vColors[i*3+1] = tri.c1;
				vColors[i*3+2] = tri.c2;

				pickColor[i] = tri.pick;
			}
			va.setBuffer(0, 2, vertices);
			va.setBuffer(1, 1, false, vColors);
			va.setBuffer(2, 1, false, pickColor);
			isDirty = false;
		}
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty() {
		this.isDirty = true;
	}

	static class TriangleDetails {
		public final float x0,x1,x2, y0,y1,y2;
		public final int c0,c1,c2, pick;
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
	 * returns null unless {@link #initGL()} was called
	 * @return
	 */
	public VertexArray getVertexArray() {
		return va;
	}


	/**
	 * {@link NullPointerException} unless {@link #initGL()} was called
	 */
	public void bindVertexArray() {
		va.bindAndEnableAttributes(0,1,2);
		GL33.glVertexAttribDivisor(2,1);
	}


	/**
	 * {@link NullPointerException} unless {@link #initGL()} was called
	 */
	public void releaseVertexArray() {
		va.unbindAndDisableAttributes(0,1,2,3);
	}
	
}
