package jplotter.globjects;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import hageldave.imagingkit.core.Img;
import jplotter.util.GLUtils;
import jplotter.util.GenericKey;

public class CharacterAtlas implements AutoCloseable {

	protected static final String CHARACTERS = 
			" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

	protected static final float charTexWidth = 1.0f/CHARACTERS.length();

	protected static final Img FONTMETRIC_IMG = new Img(32, 32);

	protected static final HashMap<GenericKey, CharacterAtlas> ATLAS_COLLECTION = new HashMap<>();

	Font font;
	int charWidth;
	int charHeigth;
	int texID;
	final int fontSize;
	final int style;
	final boolean antialiased;

	protected CharacterAtlas(int fontSize, int style, boolean antialiased) {
		this.fontSize = fontSize;
		this.style = style;
		this.antialiased = antialiased;

		Object aahint = antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON:RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
		font = new Font(Font.MONOSPACED, style, fontSize);
		Rectangle2D bounds = boundsForText(CHARACTERS.length(), font, antialiased);
		Img img = new Img(bounds.getBounds().getSize());
		charWidth = img.getWidth()/CHARACTERS.length();
		charHeigth = img.getHeight();
		img.paint(g -> {
			g.setColor(Color.white);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aahint);
			g.setFont(font);
			g.drawString(CHARACTERS, (float)bounds.getX(),(float)-bounds.getY());
		});
		texID = GLUtils.create2DTexture(img, GL11.GL_NEAREST, GL12.GL_CLAMP_TO_EDGE);
	}

	public static CharacterAtlas get(int fontSize, int style, boolean antialiased){
		GenericKey key = new GenericKey(fontSize, style, antialiased);
		if(!ATLAS_COLLECTION.containsKey(key)){
			ATLAS_COLLECTION.put(key, new CharacterAtlas(fontSize, style, antialiased));
		}
		return ATLAS_COLLECTION.get(key);
	}

	public static Rectangle2D boundsForText(int textlength, Font font, boolean antialiased){
		Object aahint = antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON:RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
		Graphics2D g2d = FONTMETRIC_IMG.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aahint);
		FontMetrics fontMetrics = g2d.getFontMetrics(font);
		char[] sampletext = new char[textlength]; Arrays.fill(sampletext, 'A');
		Rectangle2D bounds = fontMetrics.getStringBounds(new String(sampletext), g2d);
		g2d.dispose();
		return bounds;
	}

	public static Rectangle2D boundsForText(int textlength, int fontSize, int style, boolean antialiased){
		Font font = new Font(Font.MONOSPACED, style, fontSize);
		return boundsForText(textlength, font, antialiased);
	}

	public static float getTexCoordXForChar(char c){
		if(c < 32 || c > 126){
			return 0;
		}
		return (c-32)*charTexWidth;
	}


	public Font getFont() {
		return font;
	}

	public int getCharWidth() {
		return charWidth;
	}

	public int getCharHeigth() {
		return charHeigth;
	}

	public int getTexID() {
		return texID;
	}

	public VertexArray createVAforString(String s, VertexArray va){
		if(Objects.isNull(va)){
			va = new VertexArray(2);
		} else if(va.numAttributes < 2){
			throw new IllegalArgumentException("provided vertex array does not have enough attributes, need at least 2 but got " + va.numAttributes);
		}
		float[] vertices = vaVerticesForStringLength(s.length());
		float[] texCoords = vaTexCoordsForChars(s.toCharArray());
		int[] indices = vaIndicesForStringLength(s.length());
		va.setBuffer(0, 2, vertices);
		va.setBuffer(1, 2, texCoords);
		va.setIndices(indices);
		return va;
	}

	public float[] vaVerticesForStringLength(int len){
		float[] vertices = new float[len*2*4];
		for(int i = 0; i < len; i++){
			// bot left
			vertices[i*2*4+0] = i*charWidth;
			vertices[i*2*4+1] = 0.0f;
			// top left
			vertices[i*2*4+2] = i*charWidth;
			vertices[i*2*4+3] = charHeigth;
			// bot right
			vertices[i*2*4+4] = (i+1)*charWidth;
			vertices[i*2*4+5] = 0.0f;
			// top right
			vertices[i*2*4+6] = (i+1)*charWidth;
			vertices[i*2*4+7] = charHeigth;
		}
		return vertices;
	}

	public static float[] vaTexCoordsForChars(char[] chars){
		float[] texCoords = new float[chars.length*2*4];
		for(int i = 0; i < chars.length; i++){
			// y is flipped due to texture coordinates being upside down
			// tex bot left
			texCoords[i*2*4+0] = getTexCoordXForChar(chars[i]);
			texCoords[i*2*4+1] = 1.0f;
			// tex top left
			texCoords[i*2*4+2] = getTexCoordXForChar(chars[i]);
			texCoords[i*2*4+3] = 0.0f;
			// tex bot right
			texCoords[i*2*4+4] = getTexCoordXForChar(chars[i])+charTexWidth;
			texCoords[i*2*4+5] = 1.0f;
			// tex top right
			texCoords[i*2*4+6] = getTexCoordXForChar(chars[i])+charTexWidth;
			texCoords[i*2*4+7] = 0.0f;
		}
		return texCoords;
	}
	
	public static int[] vaIndicesForStringLength(int len){
		int[] indices = new int[len*3*2];
		for(int i = 0; i < len; i++){
			// triangle 1
			indices[i*2*3+0] = i*4+0;
			indices[i*2*3+1] = i*4+1;
			indices[i*2*3+2] = i*4+2;
			// triangle 2
			indices[i*2*3+3] = i*4+2;
			indices[i*2*3+4] = i*4+1;
			indices[i*2*3+5] = i*4+3;
		}
		return indices;
	}


	@Override
	public void close() {
		if(texID != 0){
			GL11.glDeleteTextures(texID);
			texID = 0;
			ATLAS_COLLECTION.remove(new GenericKey(fontSize, style, antialiased));
		}
	}

	public static void clearAndCloseAtlasCollection(){
		LinkedList<GenericKey> keys = new LinkedList<>(ATLAS_COLLECTION.keySet());
		while(!keys.isEmpty()){
			GenericKey key = keys.removeFirst();
			CharacterAtlas atlas = ATLAS_COLLECTION.get(key);
			if(Objects.nonNull(atlas))
				atlas.close();
		}
	}

}
