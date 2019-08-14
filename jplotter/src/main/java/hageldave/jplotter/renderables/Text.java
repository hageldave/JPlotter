package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.misc.CharacterAtlas;
import hageldave.jplotter.renderers.TextRenderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;

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
 * <li>picking color - the picking color with which the text is rendered into the (invisible) picking color attachment
 * of an {@link FBO}. This color may serve as an identifier of the object that can be queried from a location of the
 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values) or 0.
 * </li>
 * </ul>
 * 
 * @author hageldave
 */
public class Text implements Renderable {

	public final int fontsize; 
	public final int style;
	protected Dimension textSize;
	protected Color color;
	protected Color background = new Color(0, true);
	protected int pickColor;
	protected Point2D origin;
	protected VertexArray va=null;
	protected float angle=0;
	protected String txtStr;
	protected boolean isDirty=true;
	
	/**
	 * Creates a new Text object with the specified string and font configuration.
	 * @param textstr the text to be displayed
	 * @param fontsize point size of the font
	 * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
	 * or bitwise union BOLD|ITALIC.
	 */
	public Text(String textstr, int fontsize, int style) {
		this.txtStr = textstr;
		this.textSize = CharacterAtlas.boundsForText(textstr.length(), fontsize, style).getBounds().getSize();
		this.fontsize = fontsize;
		this.style = style;
		this.color = new Color(128,128,128);
		this.origin = new Point(0, 0);
	}

	/**
	 * Sets the color of this text
	 * @param color to set
	 * @return this for chaining
	 */
	public Text setColor(Color color) {
		this.color = color;
		return this;
	}

	/**
	 * Sets the color of this text in integer packed ARGB format.
	 * e.g. 0xff00ff00 for opaque green.
	 * @param argb integer packed ARGB color value
	 * @return this for chaining
	 */
	public Text setColor(int argb) {
		return this.setColor(new Color(argb, true));
	}

	/**
	 * @return this text's color
	 */
	public Color getColor() {
		return color;
	}
	
	/**
	 * Sets the background color of the text, per default this is
	 * transparent black (0x00000000) which wont be visible.
	 * @param background color
	 */
	public void setBackground(Color background) {
		this.background = background;
	}

	/**
	 * Sets the background color of the text, per default this is
	 * transparent black (0x00000000) which wont be visible.
	 * @param argb integer packed ARGB color value
	 */
	public void setBackground(int argb) {
		this.background = new Color(argb, true);
	}
	
	public Color getBackground() {
		return background;
	}
	
	/**
	 * @return normalized red channel of this text's color (in [0,1])
	 */
	public float getColorR() {
		return color.getRed()/255f;
	}

	/**
	 * @return normalized green channel of this text's color (in [0,1])
	 */
	public float getColorG() {
		return color.getGreen()/255f;
	}

	/**
	 * @return normalized blue channel of this text's color (in [0,1])
	 */
	public float getColorB() {
		return color.getBlue()/255f;
	}

	/**
	 * @return normalized alpha channel of this text's color (in [0,1])
	 */
	public float getColorA() {
		return color.getAlpha()/255f;
	}
	
	/**
	 * Sets the picking color of this {@link Text} object. 
	 * The picking color is the color with which quads of the individual characters are rendered into the
	 * (invisible) picking color attachment of an {@link FBO}. 
	 * This color may serve as an identifier of the object that can be queried from a location of the
	 * rendering canvas. It may take on a value in range of 0xff000001 to 0xffffffff (16.777.214 possible values).
	 * @param pickColor opaque integer packed RGB value, 0 or one in [0xff000001..0xffffffff]. 
	 * When a transparent color is specified its alpha channel will be set to 0xff to make it opaque.
	 * @return this for chaining
	 */
	public Text setPickColor(int pickColor) {
		this.pickColor = pickColor;
		// can only use opaque colors cause transparent colors will not work on overlaps
		if(pickColor != 0)
			this.pickColor = pickColor | 0xff000000;
		return this;
	}

	/**
	 * @return the picking color of this {@link Text} object
	 */
	public int getPickColor() {
		return pickColor;
	}
	
	/**
	 * @return the normalized red channel of the picking color (in [0,1])
	 */
	public float getPickColorR() {
		return Pixel.r(pickColor)/255f;
	}

	/**
	 * @return the normalized green channel of the picking color (in [0,1])
	 */
	public float getPickColorG() {
		return Pixel.g(pickColor)/255f;
	}

	/**
	 * @return the normalized blue channel of the picking color (in [0,1])
	 */
	public float getPickColorB() {
		return Pixel.b(pickColor)/255f;
	}

	/**
	 * @return the dimensions in pixels of this text object
	 */
	public Dimension getTextSize() {
		return textSize;
	}
	
	/**
	 * @return the bounding rectangle of this text
	 */
	public Rectangle2D getBounds() {
		return new Rectangle2D.Double(origin.getX(), origin.getY(), textSize.getWidth(), textSize.getHeight());
	}
	
	/**
	 * @return the bounding rectangle of this text with its rotation taken into account.
	 */
	public Rectangle2D getBoundsWithRotation() {
		Rectangle2D bounds = new Rectangle2D.Double(0, 0, getTextSize().width, getTextSize().height);
		AffineTransform transform = new AffineTransform();
		transform.rotate(angle);
		transform.translate(origin.getX(), origin.getY());
		return transform.createTransformedShape(bounds).getBounds2D();
	}

	/**
	 * @return the origin of this text object, i.e. the bottom left corner of the rectangle enclosing the text,
	 * the text's location so to say
	 */
	public Point2D getOrigin() {
		return origin;
	}

	/**
	 * Sets the origin of this text object, i.e. the bottom left corner of the rectangle enclosing the text,
	 * the text's location so to say
	 * @param origin to set
	 * @return this for chaining
	 */
	public Text setOrigin(Point2D origin) {
		this.origin = origin;
		return this;
	}

	/**
	 * Sets the origin of this text object, i.e. the bottom left corner of the rectangle enclosing the text,
	 * the text's location so to say
	 * @param x coordinate of origin
	 * @param y coordinate of origin
	 * @return this for chaining
	 */
	public Text setOrigin(int x, int y) {
		return this.setOrigin(new Point(x, y));
	}
	
	/**
	 * @return the rotation angle in radian by which this text object is rotated around its origin.
	 */
	public float getAngle() {
		return angle;
	}
	
	/**
	 * Sets the rotation angle in radian by which this text object is rotated around its origin.
	 * @param angle rotation angle
	 * @return this for chaining
	 */
	public Text setAngle(double angle) {
		this.angle = (float)angle;
		return this;
	}
	
	/**
	 * Allocates GL resources, i.e. creates the vertex array and fills
	 * it according to the contents of this {@link Text} object.
	 * If the vertex array has already been created, nothing happens.
	 */
	@Override
	@GLContextRequired
	public void initGL(){
		if(Objects.isNull(va)){
			va = new VertexArray(2);
			updateGL();
		}
	}
	
	/**
	 * Updates the vertex array to be in sync with this text object.
	 * This sets the {@link #isDirty()} state to false.
	 * if {@link #initGL()} has not been called yet or this object has
	 * already been closed, nothing happens
	 */
	@Override
	@GLContextRequired
	public void updateGL() {
		if(Objects.nonNull(va)){
			CharacterAtlas.get(fontsize, style).createVAforString(txtStr, va);
			isDirty = false;
		}
	}
	
	/**
	 * disposes of the GL resources of this text object,
	 * i.e deletes the vertex array.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(va)){
			va.close();
			va = null;
		}
	}
	
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	/**
	 * Sets the {@link #isDirty()} state of this renderable to true.
	 * This indicates that an {@link #updateGL()} call is necessary to sync GL resources.
	 * @return this for chaining
	 */
	public Text setDirty() {
		this.isDirty = true;
		return this;
	}
	
	/**
	 * Returns the GL object name of the texture of the character atlas this text's font
	 * corresponds to.
	 * @return the texture id to use for texturing this text object
	 * @throws IllegalStateException when no {@link FBOCanvas} is currently active
	 */
	@GLContextRequired
	public int getTextureID(){
		return CharacterAtlas.get(fontsize, style).getTexID();
	}
	
	/**
	 * @return the String this text object displays
	 */
	public String getTextString(){
		return txtStr;
	}
	
	/**
	 * Sets the string of this text.
	 * Only characters that are ASCII printable (more precisely ASCII characters [32..126]) will be
	 * displayed, other characters are mapped to whitespace for rendering.
	 * This set the {@link #isDirty()} state of this {@link Renderable} to true.
	 * @param txtStr the text string this object should display.
	 * @return this for chaining
	 */
	public Text setTextString(String txtStr) {
		this.txtStr = txtStr;
		this.textSize = CharacterAtlas.boundsForText(txtStr.length(), fontsize, style).getBounds().getSize();
		return setDirty();
	}
	
	@Override
	public boolean intersects(Rectangle2D rect) {
		if(getAngle()==0){
			Rectangle2D bounds = getBounds();
			return rect.intersects(bounds) || bounds.intersects(rect);
		} else {
			Rectangle2D bounds = getBoundsWithRotation();
			return rect.intersects(bounds) || bounds.intersects(rect);
		}
	}


	/**
	 * Returns the vertex array of this text object.
	 * The vertex array's first attribute contains the 2D vertices for the quads
	 * that will be textured according to the character of this text object.
	 * The second attribute contains the texture coordinates for the vertices in
	 * the first attribute.
	 * @return the vertex array associated with this text object or null if
	 * {@link #initGL()} was not yet called.
	 */
	public VertexArray getVertexArray() {
		return va;
	}


	/**
	 * Binds this object's vertex array and enables the corresponding attributes 
	 * (first and second attribute).
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void bindVertexArray() {
		va.bindAndEnableAttributes(0,1);
	}


	/**
	 * Releases this objects vertex array and disables the corresponding attributes
	 * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
	 */
	@GLContextRequired
	public void releaseVertexArray() {
		va.releaseAndDisableAttributes(0,1);
	}
	
	
}
