package hageldave.jplotter.misc;

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
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.FontProvider;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.GenericKey;

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

//	public static final String CHARACTERS = SignedDistanceCharacters.CHARACTERS;
	private static final char[] CHARACTERS = SignedDistanceCharacters.CHARACTERS.toCharArray();

	protected static final Img FONTMETRIC_IMG = new Img(32, 32);

	protected static final HashMap<Integer, HashMap<GenericKey, CharacterAtlas>> ATLAS_COLLECTION = new HashMap<>();
	
	protected static final float leftPaddingFactor = 0.1f;
	
	protected static final float rightPaddingFactor = 0.3f;
	
	protected static final float topPaddingFactor = 0.1f;
	
	protected static final float botPaddingFactor = 0.1f;

	public final Font font;
	public final int charWidth;
	public final int charHeigth;
	public final int fontSize;
	public final int style;
//	public final boolean antialiased;
	public final int owningCanvasID;
	protected int texID;
	public SignedDistanceCharacters sdChars;
	
	@GLContextRequired
	protected CharacterAtlas(int fontSize, int style, boolean antialiased) {
		int canvasID = FBOCanvas.CURRENTLY_ACTIVE_CANVAS;
		if(canvasID == 0){
			throw new IllegalStateException(
					"No active FBOCanvas, the FBOCanvas.CURRENTLY_ACTIVE_CANVAS field was 0. " +
					"This indicates that there is likely no active GL context to execute GL methods in."
			);
		}
		this.owningCanvasID = canvasID;
		this.fontSize = fontSize;
		this.style = style;

		this.font = FontProvider.getUbuntuMono(fontSize, style);
		this.sdChars = SignedDistanceCharacters.getUbuntuMonoSDC(style);
		this.texID = GLUtils.create2DTexture(sdChars.texImg, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
		
		int[] fontmetrics = {0,0};
		FONTMETRIC_IMG.paint(g->{
			FontMetrics metrics = g.getFontMetrics(this.font);
			fontmetrics[0] = metrics.charWidth('K');
			fontmetrics[1] = metrics.getHeight();
		});
		this.charWidth = fontmetrics[0];
		this.charHeigth = fontmetrics[1];
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
	 * @throws IllegalStateException when no {@link FBOCanvas} is currently active
	 */
	@GLContextRequired
	public static CharacterAtlas get(int fontSize, int style, boolean antialiased){
		int canvasID = FBOCanvas.CURRENTLY_ACTIVE_CANVAS;
		if(canvasID == 0){
			throw new IllegalStateException(
					"No active FBOCanvas, the FBOCanvas.CURRENTLY_ACTIVE_CANVAS field was 0. " +
					"This indicates that there is likely no active GL context to execute GL methods in."
			);
		}
		if(!ATLAS_COLLECTION.containsKey(canvasID)){
			ATLAS_COLLECTION.put(canvasID, new HashMap<>());
		}
		HashMap<GenericKey, CharacterAtlas> contextCollection = ATLAS_COLLECTION.get(canvasID);
		GenericKey key = new GenericKey(fontSize, style, antialiased);
		if(!contextCollection.containsKey(key)){
			contextCollection.put(key, new CharacterAtlas(fontSize, style, antialiased));
		}
		return contextCollection.get(key);
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
		char[] sampletext = new char[textlength]; Arrays.fill(sampletext, 'K');
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
		Font font = FontProvider.getUbuntuMono(fontSize, style);
		return boundsForText(textlength, font, antialiased);
	}


	public float getTexCoordXForCharLeft(char c){
		int idx = 0;
		if((idx = Arrays.binarySearch(CHARACTERS, c)) < 0){
			return 0;
		}
		return sdChars.leftBounds[idx]*1f/(sdChars.texImg.getWidth()-1);
	}
	
	public float getTexCoordXForCharRight(char c){
		int idx = 0;
		if((idx = Arrays.binarySearch(CHARACTERS, c)) < 0){
			return 0;
		}
		return sdChars.rightBounds[idx]*1f/(sdChars.texImg.getWidth()-1);
	}
	
	public float getTexCoordYForCharTop(char c){
		int idx = 0;
		if((idx = Arrays.binarySearch(CHARACTERS, c)) < 0){
			return 0;
		}
		return sdChars.topBounds[idx]*1f/(sdChars.texImg.getHeight()-1);
	}
	
	public float getTexCoordYForCharBot(char c){
		int idx = 0;
		if((idx = Arrays.binarySearch(CHARACTERS, c)) < 0){
			return 0;
		}
		return sdChars.botBounds[idx]*1f/(sdChars.texImg.getHeight()-1);
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
	 * @return GL name of the texture corresponding to this atlas which contains
	 * the rendered characters.
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
			float x0 = i*charWidth;
			float x1 = (i+1)*charWidth;
			float y0 = 0;
			float y1 = charHeigth;
			// apply padding
			x0 -= charWidth*leftPaddingFactor;
			x1 += charWidth*rightPaddingFactor;
			y0 -= charHeigth*botPaddingFactor;
			y1 += charHeigth*topPaddingFactor;
			
			// bot left
			vertices[i*2*4+0] = x0;
			vertices[i*2*4+1] = y0;
			// top left
			vertices[i*2*4+2] = x0;
			vertices[i*2*4+3] = y1;
			// bot right
			vertices[i*2*4+4] = x1;
			vertices[i*2*4+5] = y0;
			// top right
			vertices[i*2*4+6] = x1;
			vertices[i*2*4+7] = y1;
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
//	public static float[] vaTexCoordsForChars(char[] chars){
//		float[] texCoords = new float[chars.length*2*4];
//		for(int i = 0; i < chars.length; i++){
//			// y is flipped due to texture coordinates being upside down
//			// tex bot left
//			texCoords[i*2*4+0] = getTexCoordXForChar(chars[i]);
//			texCoords[i*2*4+1] = 1.0f;
//			// tex top left
//			texCoords[i*2*4+2] = getTexCoordXForChar(chars[i]);
//			texCoords[i*2*4+3] = 0.0f;
//			// tex bot right
//			texCoords[i*2*4+4] = getTexCoordXForChar(chars[i])+charTexWidth;
//			texCoords[i*2*4+5] = 1.0f;
//			// tex top right
//			texCoords[i*2*4+6] = getTexCoordXForChar(chars[i])+charTexWidth;
//			texCoords[i*2*4+7] = 0.0f;
//		}
//		return texCoords;
//	}
	
	public float[] vaTexCoordsForChars(char[] chars){
		float[] texCoords = new float[chars.length*2*4];
		for(int i = 0; i < chars.length; i++){
			// y is flipped due to texture coordinates being upside down
			float x0 = getTexCoordXForCharLeft(chars[i]);
			float x1 = getTexCoordXForCharRight(chars[i]);
			float y0 = getTexCoordYForCharBot(chars[i]);
			float y1 = getTexCoordYForCharTop(chars[i]);
			// apply padding
			float width = x1-x0;
			float height = y0-y1;
			x0 -= width*leftPaddingFactor;
			y0 += height*botPaddingFactor;
			x1 += width*rightPaddingFactor;
			y1 -= height*topPaddingFactor;
			
			// tex bot left
			texCoords[i*2*4+0] = x0;
			texCoords[i*2*4+1] = y0;
			// tex top left
			texCoords[i*2*4+2] = x0;
			texCoords[i*2*4+3] = y1;
			// tex bot right
			texCoords[i*2*4+4] = x1;
			texCoords[i*2*4+5] = y0;
			// tex top right
			texCoords[i*2*4+6] = x1;
			texCoords[i*2*4+7] = y1;
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


	/**
	 * Deletes the texture associated with this atlas and removes this atlas from
	 * the {@link #ATLAS_COLLECTION}.
	 * @throws IllegalStateException when no {@link FBOCanvas} is currently active or
	 * the active FBOCanvas does not own this atlas.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(texID != 0){
			int canvasID = FBOCanvas.CURRENTLY_ACTIVE_CANVAS;
			if(canvasID != this.owningCanvasID){
				throw new IllegalStateException(
						"The currently active canvas does not own this atlas and cannot close it though. " +
						"Currently active canvas:" + canvasID + " Owning canvas:" + this.owningCanvasID
				);
			}
			GL11.glDeleteTextures(texID);
			texID = 0;
			HashMap<GenericKey, CharacterAtlas> contextCollection = ATLAS_COLLECTION.get(this.owningCanvasID);
			contextCollection.remove(new GenericKey(fontSize, style));
		}
	}

	/**
	 * Closes and removes all {@link CharacterAtlas} instances contained in the
	 * static {@link #ATLAS_COLLECTION} that are owned by the currently active
	 * {@link FBOCanvas}. 
	 * This disposes of all GL textures associated with these CharacterAtlases.
	 */
	@GLContextRequired
	public static void clearAndCloseAtlasCollection(){
		int canvasID = FBOCanvas.CURRENTLY_ACTIVE_CANVAS;
		if(canvasID == 0){
			return;
		}
		HashMap<GenericKey, CharacterAtlas> contextCollection = ATLAS_COLLECTION.get(canvasID);
		if(contextCollection==null){
			return;
		}
		LinkedList<GenericKey> keys = new LinkedList<>(contextCollection.keySet());
		while(!keys.isEmpty()){
			GenericKey key = keys.removeFirst();
			CharacterAtlas atlas = contextCollection.get(key);
			if(Objects.nonNull(atlas))
				atlas.close();
		}
		ATLAS_COLLECTION.remove(canvasID);
	}

}
