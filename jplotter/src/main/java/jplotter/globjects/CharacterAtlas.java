package jplotter.globjects;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.util.ImageFrame;
import jplotter.util.GLUtils;

public class CharacterAtlas implements AutoCloseable {

	private static final String CHARACTERS = 
			" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
	private static final Img FONTMETRIC_IMG = new Img(32, 32);

	Font font;
	int charWidth;
	int charHeigth;
	int texID;

	public CharacterAtlas(int fontSize, int style, boolean antialiased) {
		Object aahint = antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON:RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
		font = new Font(Font.MONOSPACED, style, fontSize);
		Graphics2D g2d = FONTMETRIC_IMG.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aahint);
		FontMetrics fontMetrics = g2d.getFontMetrics(font);
		Rectangle2D bounds = fontMetrics.getStringBounds(CHARACTERS, g2d);
		Rectangle intbounds = bounds.getBounds();
		g2d.dispose();
		Img img = new Img(intbounds.width, intbounds.height);
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

	public VertexArray createVAforString(String s){
		int[] charIndices = new int[s.length()];
		for(int i = 0; i < s.length(); i++){
			charIndices[i] = Math.max(0, CHARACTERS.indexOf(s.charAt(i)));
		}
		float[] vertices = new float[s.length()*2*4];
		float[] texCoords = new float[s.length()*2*4];
		int[] indices = new int[s.length()*3*2];
		int stringWidth = s.length()*charWidth;
		float charWidthF = 1.0f/CHARACTERS.length();
		for(int i = 0; i < s.length(); i++){
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

			// y is flipped due to texture coordinates being upside down
			// tex bot left
			texCoords[i*2*4+0] = charIndices[i]*charWidthF;
			texCoords[i*2*4+1] = 1.0f;
			// tex top left
			texCoords[i*2*4+2] = charIndices[i]*charWidthF;
			texCoords[i*2*4+3] = 0.0f;
			// tex bot right
			texCoords[i*2*4+4] = (charIndices[i]+1)*charWidthF;
			texCoords[i*2*4+5] = 1.0f;
			// tex top right
			texCoords[i*2*4+6] = (charIndices[i]+1)*charWidthF;
			texCoords[i*2*4+7] = 0.0f;

			// triangle 1
			indices[i*2*3+0] = i*4+0;
			indices[i*2*3+1] = i*4+1;
			indices[i*2*3+2] = i*4+2;
			// triangle 2
			indices[i*2*3+3] = i*4+2;
			indices[i*2*3+4] = i*4+1;
			indices[i*2*3+5] = i*4+3;
		}
		VertexArray va = new VertexArray(2);
		va.setBuffer(0, 2, vertices);
		va.setBuffer(1, 2, texCoords);
		va.setIndices(indices);
		return va;
	}


	@Override
	public void close() {
		GL11.glDeleteTextures(texID);
		texID = 0;
	}

}
