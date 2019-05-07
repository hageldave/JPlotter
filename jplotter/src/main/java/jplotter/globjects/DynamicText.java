package jplotter.globjects;

import java.util.Arrays;
import java.util.Objects;

public class DynamicText extends Text {
	
	final char[] text;
	
	public DynamicText(char[] text, int fontsize, int style, boolean antialiased) {
		super(fontsize,style,antialiased, CharacterAtlas.boundsForText(text.length, fontsize, style, antialiased).getBounds().getSize());
		this.text = text;
	}

	@Override
	public void close() {
		if(Objects.nonNull(va)){
			va.close();
			va = null;
		}
	}

	@Override
	public void initGL() {
		if(Objects.isNull(va))
			va = CharacterAtlas.get(fontsize, style, antialiased).createVAforString(getTextString());
	}
	
	public void updateGL(){
		if(Objects.nonNull(va)){
			float[] texcoords = CharacterAtlas.vaTexCoordsForChars(text);
			va.setBuffer(1, 2, texcoords);
		}
	}
	
	@Override
	public int getTextureID() {
		return CharacterAtlas.get(fontsize, style, antialiased).getTexID();
	}

	@Override
	public String getTextString() {
		return new String(text);
	}
	
	public char[] getCharArray() {
		return text;
	}
	
	public DynamicText setTextFromString(String str){
		Arrays.fill(text, ' ');
		int len = Math.min(str.length(), text.length);
		for(int i=0; i<len; i++){
			text[i] = str.charAt(i);
		}
		return this;
	}
	
	@Override
	public DynamicText setPickColor(int pickColor) {
		super.setPickColor(pickColor);
		return this;
	}
	
	@Override
	public boolean isDirty() {
		// TODO: detect if actually dirty
		return true;
	}

}
