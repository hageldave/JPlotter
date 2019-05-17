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
import org.lwjgl.opengl.GL15;

import hageldave.imagingkit.core.Img;
import jplotter.Annotations.GLContextRequired;
import jplotter.util.GLUtils;
import jplotter.util.GenericKey;

/**
 * The CharacterAtlas class is a texture atlas for looking up character textures.
 * That is a texture containing rendered characters at specific positions.
 * This is used to be able to render an arbitrary sequence of characters by looking up
 * the corresponding area in the texture.
 * A {@link CharacterAtlas} is defined by:
 * <ul>
 * <li>its font which is always mono spaced in this implementation</li>
 * <li>its font size</li>
 * <li>its font style (PLAIN, ITALIC, BOLD)</li>
 * <li>antialiasing - whether the texture uses antialiased characters</li>
 * </ul>
 * This implementation limits the possible characters to ASCII displayable ones.
 * More exact the range of ASCII characters [32..126] that is the white space character
 * up to the tilda character.
 * Any other character will be mapped to white space and will this be invisible in the
 * render.
 * To obtain an character atlas use the static {@link CharacterAtlas#get(int, int, boolean)}
 * method.
 * A {@link VertexArray} with 2D vertices on the first attribute and 2D texture coordinates
 * on the second can be retrieved for a specified string using 
 * {@link CharacterAtlas#createVAforString(String, VertexArray)}.
 * 
 * @author hageldave
 */
public class CharacterAtlas implements AutoCloseable {

	protected static final String CHARACTERS = 
			" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

	public static final float charTexWidth = 1.0f/CHARACTERS.length();

	protected static final Img FONTMETRIC_IMG = new Img(32, 32);

	protected static final HashMap<GenericKey, CharacterAtlas> ATLAS_COLLECTION = new HashMap<>();

	Font font;
	int charWidth;
	int charHeigth;
	int texID;
	final int fontSize;
	final int style;
	final boolean antialiased;

	@GLContextRequired
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

	/**
	 * Retrieves an already existing {@link CharacterAtlas} for the specified
	 * parameters from the internal collection of atlases, or creates a new one
	 * which will then be added to the collection. 
	 * @param fontSize point size of the font
	 * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
	 * or bitwise union BOLD|ITALIC.
	 * @param antialiased whether the characters of the texture are antialiased or not.
	 * @return matching {@link CharacterAtlas}.
	 */
	@GLContextRequired
	public static CharacterAtlas get(int fontSize, int style, boolean antialiased){
		GenericKey key = new GenericKey(fontSize, style, antialiased);
		if(!ATLAS_COLLECTION.containsKey(key)){
			ATLAS_COLLECTION.put(key, new CharacterAtlas(fontSize, style, antialiased));
		}
		return ATLAS_COLLECTION.get(key);
	}

	/**
	 * Calculates the bounding rectangle for a specific number of characters in the specified font.
	 * The returned bounds are baseline relative which means that the origin may have a negative
	 * y coordinate that is the distance of the descent line from the baseline.
	 * @param textlength number of characters
	 * @param font to measure with (has to be monospaced, or else bounds will be incorrect)
	 * @param antialiased whether the font is intended to be drawn with antialiasing (may not make a difference but who knows).
	 * @return bounding rectangle for a text of specified length and font.
	 */
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

	/**
	 * Calls {@link #boundsForText(int, Font, boolean)} with {@code ont(Font.MONOSPACED, style, fontSize)}.
	 * @param textlength number of characters
	 * @param fontSize point size of the font
	 * @param style of the font e.g. {@link Font#PLAIN}.
	 * @param antialiased whether the font is intended to be drawn with antialiasing (may not make a difference but who knows).
	 * @return bounding rectangle for a text of specified length and font.
	 */
	public static Rectangle2D boundsForText(int textlength, int fontSize, int style, boolean antialiased){
		Font font = new Font(Font.MONOSPACED, style, fontSize);
		return boundsForText(textlength, font, antialiased);
	}

	/**
	 * Calculates the x coordinate of the texture origin for the specified character within the atlas texture.
	 * The corresponding y coordinate is always zero, the height of the texture is 1 and the width for one character is
	 * {@link #charTexWidth}. So the bounds for the character in the texture will be {@code (x,0, charTexWidth, 1)}
	 * Characters not in ASCII range [32..126] will be mapped to white space.
	 * @param c character
	 * @return x coordinate for the origin of the bounds of the specified character in the atlas texture
	 */
	public static float getTexCoordXForChar(char c){
		if(c < 32 || c > 126){
			return 0;
		}
		return (c-32)*charTexWidth;
	}

	/**
	 * @return the font of this character atlas
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * @return pixel width of a single character in the font of this atlas.
	 */
	public int getCharWidth() {
		return charWidth;
	}

	/**
	 * @return pixel height of a single character in the font of this atlas 
	 * (descent line to ascent line).
	 */
	public int getCharHeigth() {
		return charHeigth;
	}

	/**
	 * GL name of the texture corresponding to this atlas which contains
	 * the rendered characters.
	 * @return
	 */
	public int getTexID() {
		return texID;
	}
	
	/**
	 * Creates or fills a {@link VertexArray} with vertex quads for each
	 * character of the specified string and corresponding texture coordinates
	 * that each quad uses in order to be textured with the correct character.
	 * <p>
	 * The first attribute (index=0) of the vertex array will be a sequence of equally sized
	 * quads of {@link #charWidth} and {@link #charHeigth} starting at (0,0).
	 * The second attribute (index=1) will contain texture coordinates for each vertex of the
	 * quads in the first attribute.
	 * @param s string
	 * @param va (optional) vertex array that has at least 2 attributes or null in which case
	 * a new VA is created.
	 * 
	 * @return the specified or new VA.
	 * @throws IllegalArgumentException if the specified VertexArray has less than 2 attributes.
	 */
	@GLContextRequired
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

	/**
	 * Creates the 2D vertex coordinates for a string of specified
	 * length. The vertices describe a sequence of len equally sized
	 * quads of {@link #charWidth} {@link #charHeigth}.
	 * @param len number of quads (i.e. characters)
	 * @return content for a vertex array
	 */
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

	/**
	 * Creates the 2D texture coordinates for the specified array of characters
	 * corresponding to each vertex created with {@link #vaVerticesForStringLength(int)}.
	 * The texture coordinates map to the respectively corresponding character enclosing 
	 * rectangle within a CharacterAtlas' texture.
	 * @param chars the sequence of characters
	 * @return texture coordinates for the sequence of characters.
	 */
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
	
	/**
	 * Creates the indices for the {@link GL15#GL_ELEMENT_ARRAY_BUFFER} of the
	 * {@link VertexArray} that defines the vertex indices to draw the triangles
	 * corresponding to the quads associated with the vertices generated by
	 * {@link #vaVerticesForStringLength(int)}.
	 * @param len number of quads (i.e. characters)
	 * @return indices for the VertexArray
	 */
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
	@GLContextRequired
	public void close() {
		if(texID != 0){
			GL11.glDeleteTextures(texID);
			texID = 0;
			ATLAS_COLLECTION.remove(new GenericKey(fontSize, style, antialiased));
		}
	}

	@GLContextRequired
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
