package jplotter.globjects;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

public class Text implements AutoCloseable {

	final CharacterAtlas atlas;
	final String textstr;
	final VertexArray va;
	final Dimension textSize;
	Color color;
	Point origin;
	
	public Text(String txt, CharacterAtlas atlas) {
		this.atlas = atlas;
		this.textstr = txt;
		this.va = atlas.createVAforString(textstr);
		this.color = new Color(128,128,128);
		this.textSize = new Dimension(atlas.getCharWidth()*txt.length(), atlas.getCharHeigth());
		this.origin = new Point(0, 0);
	}
	
	public VertexArray getVertexArray() {
		return va;
	}
	
	public void bindVertexArray() {
		va.bindAndEnableAttributes(0,1);
	}
	
	public void releaseVertexArray() {
		va.unbindAndDisableAttributes(0,1);
	}
	
	public int getTextureID(){
		return atlas.getTexID();
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
	public float getColorR(){
		return color.getRed()/255f;
	}
	
	public float getColorG(){
		return color.getGreen()/255f;
	}
	
	public float getColorB(){
		return color.getBlue()/255f;
	}
	
	public String getTextstr() {
		return textstr;
	}
	
	public Dimension getTextSize() {
		return textSize;
	}
	
	public Point getOrigin() {
		return origin;
	}
	
	public void setOrigin(Point origin) {
		this.origin = origin;
	}
	
	@Override
	public void close() throws Exception {
		va.close();
	}
	
}
