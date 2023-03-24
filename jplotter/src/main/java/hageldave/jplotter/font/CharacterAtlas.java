package hageldave.jplotter.font;

import hageldave.imagingkit.core.Img;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.GenericKey;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

/**
 * The CharacterAtlas class is a texture atlas for looking up character textures.
 * That is a texture containing rendered characters at specific positions.
 * This is used to be able to render an arbitrary sequence of characters by looking up
 * the corresponding area in the texture.
 * A {@link CharacterAtlas} is defined by:
 * <ul>
 * <li>its font size</li>
 * <li>its font style (PLAIN, ITALIC, BOLD, BOLD|ITALIC)</li>
 * </ul>
 * The font used is Ubuntu Mono which is a monospaced font, which can be accessed through
 * the {@link FontProvider}.
 * This implementation limits the possible characters to the ones listed in 
 * {@link SignedDistanceCharacters#CHARACTERS}.
 * Any other character will be mapped to white space and will thus be invisible in the
 * render.
 * To obtain a character atlas use the static {@link CharacterAtlas#get(int, int)}
 * method.
 * A {@link VertexArray} with 2D vertices on the first attribute and 2D texture coordinates
 * on the second can be retrieved for a specified string using 
 * {@link CharacterAtlas#createVAforString(String, VertexArray)}.
 * 
 * @author hageldave
 */
public class CharacterAtlas implements AutoCloseable {

	private static final char[] CHARACTERS = SignedDistanceCharacters.CHARACTERS.toCharArray();

	protected static final Img FONTMETRIC_IMG = new Img(32, 32);

	protected static final HashMap<Integer, HashMap<GenericKey, CharacterAtlas>> ATLAS_COLLECTION = new HashMap<>();
	
	protected static final HashMap<Integer, HashMap<Integer, int[]>> CONTEXT_2_STYLE_2_TEXTUREREF = new HashMap<>();
	
	protected static final float leftPaddingFactor = 0.1f;
	
	protected static final float rightPaddingFactor = 0.3f;
	
	protected static final float topPaddingFactor = 0.1f;
	
	protected static final float botPaddingFactor = 0.1f;

	public final Font font;
	public final int charWidth;
	public final int charHeigth;
	public final int fontSize;
	public final int style;
	public final int owningCanvasID;
	protected int texID;
	public SignedDistanceCharacters sdChars;
	
	@GLContextRequired
	protected CharacterAtlas(int fontSize, int style) {
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
		HashMap<Integer, int[]> textures = getOrAllocateTextureReferenceMap(canvasID);
		if(!textures.containsKey(style)){
			int texID = GLUtils.create2DTexture(sdChars.texImg, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
			textures.put(style, new int[]{texID,1});
			this.texID = texID;
		} else {
			int[] texref = textures.get(style);
			this.texID = texref[0];
			texref[1]++;
		}
		
		int[] fontmetrics = {0,0};
		FONTMETRIC_IMG.paint(g->{
			FontMetrics metrics = g.getFontMetrics(this.font);
			fontmetrics[0] = metrics.charWidth('K');
			fontmetrics[1] = metrics.getHeight();
		});
		this.charWidth = fontmetrics[0];
		this.charHeigth = fontmetrics[1];
	}
	
	private static HashMap<Integer, int[]> getOrAllocateTextureReferenceMap(int context){
		if(!CONTEXT_2_STYLE_2_TEXTUREREF.containsKey(context)){
			CONTEXT_2_STYLE_2_TEXTUREREF.put(context, new HashMap<>());
		}
		return CONTEXT_2_STYLE_2_TEXTUREREF.get(context);
	}

	/**
	 * Retrieves an already existing {@link CharacterAtlas} for the specified
	 * parameters from the internal collection of atlases, or creates a new one
	 * which will then be added to the collection. 
	 * @param fontSize point size of the font
	 * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
	 * or bitwise union BOLD|ITALIC.
	 * @return matching {@link CharacterAtlas}.
	 * @throws IllegalStateException when no {@link FBOCanvas} is currently active
	 */
	@GLContextRequired
	public static CharacterAtlas get(int fontSize, int style){
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
		GenericKey key = new GenericKey(fontSize, style);
		if(!contextCollection.containsKey(key)){
			contextCollection.put(key, new CharacterAtlas(fontSize, style));
		}
		return contextCollection.get(key);
	}

	/**
	 * Calculates the bounding rectangle for a specific number of characters in the specified font.
	 * The returned bounds are baseline relative which means that the origin may have a negative
	 * y coordinate that is the distance of the descent line from the baseline.
	 * @param textlength number of characters
	 * @param font to measure with (has to be monospaced, or else bounds will be incorrect)
	 * @return bounding rectangle for a text of specified length and font.
	 */
	public static Rectangle2D boundsForText(int textlength, Font font){
		Graphics2D g2d = FONTMETRIC_IMG.createGraphics();
		FontMetrics fontMetrics = g2d.getFontMetrics(font);
		char[] sampletext = new char[textlength]; Arrays.fill(sampletext, 'K');
		Rectangle2D bounds = fontMetrics.getStringBounds(new String(sampletext), g2d);
		g2d.dispose();
		return bounds;
	}

	/**
	 * Calls {@link #boundsForText(int, Font)} with corresponding Ubuntu Mono font.
	 * @param textlength number of characters
	 * @param fontSize point size of the font
	 * @param style of the font e.g. {@link Font#PLAIN}.
	 * @return bounding rectangle for a text of specified length and font.
	 */
	public static Rectangle2D boundsForText(int textlength, int fontSize, int style){
		Font font = FontProvider.getUbuntuMono(fontSize, style);
		return boundsForText(textlength, font);
	}

	/**
	 * Returns the {@link FontMetrics} of Ubuntu Mono given the font size and -style,
	 * which is constructed from a {@link Graphics2D} object.
	 *
	 * @param fontSize point size of the font
	 * @param style of the font e.g. {@link Font#PLAIN}.
	 * @return Ubuntu Mono FontMetrics given the parameters
	 */
	public static FontMetrics getFontMetrics(int fontSize, int style) {
		Font font = FontProvider.getUbuntuMono(fontSize, style);
		Graphics2D g2d = FONTMETRIC_IMG.createGraphics();
		return g2d.getFontMetrics(font);
	}
	
	/**
	 * index for char in {@link #CHARACTERS}.
	 * @param c char to search for
	 * @return index or negative number if not contained
	 */
	protected static int indexForChar(char c){
		return Arrays.binarySearch(CHARACTERS, c);
	}

	protected float getTexCoordXForCharLeft(int idx){
		return sdChars.leftBounds[idx]*1f/(sdChars.texImg.getWidth()-1);
	}
	
	protected float getTexCoordXForCharRight(int idx){
		return sdChars.rightBounds[idx]*1f/(sdChars.texImg.getWidth()-1);
	}
	
	protected float getTexCoordYForCharTop(int idx){
		return sdChars.topBounds[idx]*1f/(sdChars.texImg.getHeight()-1);
	}
	
	protected float getTexCoordYForCharBot(int idx){
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
	public float[] vaTexCoordsForChars(char[] chars){
		float[] texCoords = new float[chars.length*2*4];
		for(int i = 0; i < chars.length; i++){
			int charIDX = indexForChar(chars[i]);
			charIDX = charIDX < 0 ? 0:charIDX;
			// y is flipped due to texture coordinates being upside down
			float x0 = getTexCoordXForCharLeft(charIDX);
			float x1 = getTexCoordXForCharRight(charIDX);
			float y0 = getTexCoordYForCharBot(charIDX);
			float y1 = getTexCoordYForCharTop(charIDX);
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
			HashMap<Integer, int[]> textures = CONTEXT_2_STYLE_2_TEXTUREREF.get(canvasID);
			int[] texref = textures.get(style);
			if(--texref[1] == 0){
				GL11.glDeleteTextures(texID);
				textures.remove(style);
			}
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
