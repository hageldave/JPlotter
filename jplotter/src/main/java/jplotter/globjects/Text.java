package jplotter.globjects;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;

import hageldave.imagingkit.core.Pixel;
import jplotter.renderers.TextRenderer;

/**
 * Abstract class for {@link Renderable}s representing text that can be rendered using the
 * {@link TextRenderer}.
 * A text object describes a line of characters together with the following attributes:
 * <ul>
 * <li>fontsize (e.g. 12 pts.)</li>
 * <li>font style (e.g. Font{@link Font#BOLD})</li>
 * <li>color</li>
 * <li>origin - the bottom left corner of the rectangle enclosing the text</li>
 * <li>angle - the rotation of the text (around origin)</li>
 * <li>antialiasing - whether the font texture is antialiased or not</li>
 * <li>picking color - the picking color with which the text is rendered into the (invisible) picking color attachment
 * of an {@link FBO}. This color may serve as an identifier of the object that can be queried from a location of the
 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values).
 * </li>
 * </ul>
 * 
 * @author hageldave
 */
public abstract class Text implements Renderable {

	protected final Dimension textSize;
	protected final int fontsize; 
	protected final int style;
	protected final boolean antialiased;
	protected Color color;
	protected int pickColor;
	protected Point origin;
	protected VertexArray va=null;
	protected float angle=0;
	
	public Text(int fontsize, int style, boolean antialiased, Dimension textSize) {
		this.textSize = textSize;
		this.fontsize = fontsize;
		this.style = style;
		this.antialiased = antialiased;
		this.color = new Color(128,128,128);
		this.origin = new Point(0, 0);
	}


	public int getStyle() {
		return style;
	}


	public int getFontsize() {
		return fontsize;
	}


	public boolean isAntialiased() {
		return antialiased;
	}


	public Text setColor(Color color) {
		this.color = color;
		return this;
	}


	public Text setColor(int argb) {
		return this.setColor(new Color(argb, true));
	}


	public Color getColor() {
		return color;
	}


	public float getColorR() {
		return color.getRed()/255f;
	}


	public float getColorG() {
		return color.getGreen()/255f;
	}


	public float getColorB() {
		return color.getBlue()/255f;
	}


	public float getColorA() {
		return color.getAlpha()/255f;
	}
	
	public Text setPickColor(int pickColor) {
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

	public Dimension getTextSize() {
		return textSize;
	}


	public Point getOrigin() {
		return origin;
	}


	public Text setOrigin(Point origin) {
		this.origin = origin;
		return this;
	}


	public Text setOrigin(int x, int y) {
		return this.setOrigin(new Point(x, y));
	}
	
	public float getAngle() {
		return angle;
	}
	
	public void setAngle(float angle) {
		this.angle = angle;
	}
	
	/**
	 * initiaizes gl datastructures, especially the vertex array {@link #va}.
	 */
	public abstract void initGL();
	
	public abstract int getTextureID();
	
	public abstract String getTextString();


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
