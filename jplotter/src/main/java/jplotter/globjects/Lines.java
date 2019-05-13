package jplotter.globjects;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Objects;

import hageldave.imagingkit.core.Pixel;
import jplotter.util.Pair;

public class Lines implements Renderable {

	VertexArray va;
	
	protected ArrayList<Pair<Point2D,Point2D>> segments = new ArrayList<>();
	
	protected ArrayList<Pair<Color,Color>> colors = new ArrayList<>();
	
	protected float thickness = 1;
	
	protected int pickColor;
	
	protected boolean isDirty;
	
	
	public Lines setDirty() {
		this.isDirty = true;
		return this;
	}
	
	public boolean isDirty() {
		return isDirty;
	}
	
	public int numSegments() {
		return segments.size();
	}
	
	public Lines addSegment(Point2D p1, Point2D p2, Color c1, Color c2){
		segments.add(Pair.of(p1,p2));
		colors.add(Pair.of(c1,c2));
		setDirty();
		return this;
	}
	
	public Lines addSegment(Point2D p1, Point2D p2, Color c){
		return addSegment(p1, p2, c, c);
	}
	
	public Lines addSegment(double x1, double y1, double x2, double y2, int c1, int c2){
		return addSegment(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2), new Color(c1, true), new Color(c2, true));
	}
	
	public Lines addSegment(double x1, double y1, double x2, double y2, int c){
		return addSegment(x1, y1, x2, y2, c, c);
	}
	
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
	
	public boolean removeSegment(Point2D p1, Point2D p2){
		return removeSegment(Pair.of(p1, p2));
	}
	
	public void removeAllSegments() {
		this.segments.clear();
		this.colors.clear();
		setDirty();
	}
	
	public ArrayList<Pair<Point2D, Point2D>> getSegments() {
		return new ArrayList<>(segments);
	}
	
	public ArrayList<Pair<Color, Color>> getColors() {
		return new ArrayList<>(colors);
	}
	
	public Lines setThickness(float thickness) {
		this.thickness = thickness;
		return this;
	}
	
	public float getThickness() {
		return thickness;
	}
	
	public Lines setPickColor(int pickColor) {
		this.pickColor = pickColor;
		// can only use opaque colors cause transparent colors will not work on overlaps
		if(pickColor != 0)
			this.pickColor = pickColor | 0xff000000;
		return this;
	}

	public int getPickColor() {
		return pickColor;
	}
	
	public float getPickColorR() {
		return Pixel.r(pickColor)/255f;
	}


	public float getPickColorG() {
		return Pixel.g(pickColor)/255f;
	}


	public float getPickColorB() {
		return Pixel.b(pickColor)/255f;
	}

	@Override
	public void close(){
		if(Objects.nonNull(va)){
			va.close();
			va = null;
		}
	}
	
	public void initGL(){
		if(Objects.isNull(va)){
			va = new VertexArray(2);
			updateGL();
		}
	}
	
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
		va.bindAndEnableAttributes(0,1);
	}


	/**
	 * {@link NullPointerException} unless {@link #initGL()} was called
	 */
	public void releaseVertexArray() {
		va.unbindAndDisableAttributes(0,1);
	}
	
}
