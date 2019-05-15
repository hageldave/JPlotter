package jplotter.globjects;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Objects;

import org.lwjgl.opengl.GL33;

public class Points implements Renderable {

	final Glyph glyph;
	VertexArray va;
	boolean isDirty;
	float globalScaling = 1f;
	float globalAlphaMultiplier = 1f;

	ArrayList<PointDetails> points = new ArrayList<>();

	public Points(Glyph glyph) {
		this.glyph = glyph;
	}

	@Override
	public void close() {
		removeAllPoints();
		if(Objects.nonNull(va)){
			va.close();
		}
	}

	@Override
	public void initGL() {
		if(Objects.isNull(va)){
			va = new VertexArray(5);
			glyph.fillVertexArray(va);
			updateGL();
		}
	}

	@Override
	public void updateGL() {
		if(Objects.nonNull(va)){
			final int numPoints = points.size();
			float[] position = new float[numPoints*2];
			float[] rotAndScale = new float[numPoints*2];
			int[] colors = new int[numPoints*2];
			for(int i=0; i<numPoints; i++){
				PointDetails pd = points.get(i);
				position[i*2+0] = pd.px;
				position[i*2+1] = pd.py;
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

	public void setDirty() {
		this.isDirty = true;
	}


	public Points addPoint(double px, double py, double rot, double scale, int color, int pick){
		this.points.add(new PointDetails((float)px, (float)py, (float)rot, (float)scale, color, pick));
		setDirty();
		return this;
	}

	public Points addPoint(double px, double py, double rot, double scale, int color){
		return addPoint(px, py, rot, scale, color, 0);
	}

	public Points addPoint(double px, double py, double rot, double scale, Color color){
		return addPoint(px, py, rot, scale, color.getRGB(), 0);
	}

	public Points addPoint(double px, double py, Color color){
		return addPoint(px, py, 0, 1, color.getRGB(), 0);
	}

	public Points addPoint(double px, double py){
		return addPoint(px, py, 0, 1, 0xff555555, 0);
	}

	public void removeAllPoints(){
		this.points.clear();
		setDirty();
	}

	public int numPoints(){
		return points.size();
	}

	public void setGlobalScaling(float globalScaling) {
		this.globalScaling = globalScaling;
	}

	public float getGlobalScaling() {
		return globalScaling;
	}

	public void setGlobalAlphaMultiplier(float globalAlphaMultiplier) {
		this.globalAlphaMultiplier = globalAlphaMultiplier;
	}

	public float getGlobalAlphaMultiplier() {
		return globalAlphaMultiplier;
	}

	public Glyph getGlyph() {
		return glyph;
	}

	public static class PointDetails {
		public final float px;
		public final float py;
		public final float rot;
		public final float scale;
		public final int color;
		public final int pickColor;
		public PointDetails(float px, float py, float rot, float scale, int color, int pickColor) {
			this.px = px;
			this.py = py;
			this.rot = rot;
			this.scale = scale;
			this.color = color;
			if(pickColor != 0)
				pickColor = pickColor | 0xff000000;
			this.pickColor = pickColor;
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
		va.bindAndEnableAttributes(0,1,2,3);
		GL33.glVertexAttribDivisor(1,1);
		GL33.glVertexAttribDivisor(2,1);
		GL33.glVertexAttribDivisor(3,1);
	}


	/**
	 * {@link NullPointerException} unless {@link #initGL()} was called
	 */
	public void releaseVertexArray() {
		va.unbindAndDisableAttributes(0,1,2,3);
	}

}
