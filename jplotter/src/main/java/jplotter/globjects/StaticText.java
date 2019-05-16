package jplotter.globjects;

import java.awt.Color;
import java.awt.Point;
import java.util.Objects;

public class StaticText extends Text {

	final String textstr;
	
	public StaticText(String txt, int fontsize, int style, boolean antialiased) {
		super(fontsize,style,antialiased, CharacterAtlas.boundsForText(txt.length(), fontsize, style, antialiased).getBounds().getSize());
		this.textstr = txt;
	}
	
	public void initGL(){
		if(Objects.isNull(va))
			va = CharacterAtlas.get(fontsize, style, antialiased).createVAforString(textstr, null);
		
	}
	
	public int getTextureID(){
		return CharacterAtlas.get(fontsize, style, antialiased).getTexID();
	}
	
	public String getTextString() {
		return textstr;
	}
	
	@Override
	public StaticText setColor(Color color) {
		super.setColor(color);
		return this;
	}
	
	@Override
	public StaticText setColor(int argb) {
		super.setColor(argb);
		return this;
	}
	
	@Override
	public StaticText setOrigin(int x, int y) {
		super.setOrigin(x, y);
		return this;
	}
	
	@Override
	public StaticText setOrigin(Point origin) {
		super.setOrigin(origin);
		return this;
	}
	
	@Override
	public StaticText setPickColor(int pickColor) {
		super.setPickColor(pickColor);
		return this;
	}
	
	@Override
	public void close() {
		if(Objects.nonNull(va)){
			va.close();
			va = null;
		}
	}

	@Override
	public void updateGL() { /* nothing to do (static text) */ }

	@Override
	public boolean isDirty() {
		return false;
	}
	
}
