package hageldave.jplotter.renderables;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.debugging.annotations.DebugGetter;
import hageldave.jplotter.debugging.annotations.DebugSetter;
import hageldave.jplotter.debugging.panelcreators.control.*;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.util.Annotations;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * TODO
 */
public class NewText implements Renderable {
    public static final String latexInstruction = "##BEGINLATEX##";
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
    protected boolean hidden=false;
    protected boolean latex;
    protected Insets insets = new Insets(0, 0, 0, 0);
    protected TextDecoration textDecoration;
    protected PositioningRectangle positioningRectangle;

    /**
     * Creates a new Text object with the specified string and font configuration.
     * @param textstr the text to be displayed
     * @param fontsize point size of the font
     * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
     * or bitwise union BOLD|ITALIC.
     * @param textcolor color of the text
     * @param latex TODO
     */
    protected NewText(String textstr, int fontsize, int style, Color textcolor, boolean latex) {
        this.txtStr = textstr;
        this.textSize = CharacterAtlas.boundsForText(textstr.length(), fontsize, style).getBounds().getSize();
        this.fontsize = fontsize;
        this.style = style;
        this.color = textcolor;
        this.origin = new Point(0, 0);
        this.latex = latex;
    }

    /**
     * Creates a new Text object with the specified string and font configuration.
     * @param textstr the text to be displayed
     * @param fontsize point size of the font
     * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
     * or bitwise union BOLD|ITALIC.
     * @param textcolor color of the text (integer packed ARGB)
     */
    public NewText(String textstr, int fontsize, int style, int textcolor) {
        this(textstr,fontsize,style, new Color(textcolor, true), NewText.isLatex(textstr));
    }

    /**
     * Creates a new Text object with the specified string and font configuration.
     * @param textstr the text to be displayed
     * @param fontsize point size of the font
     * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
     * or bitwise union BOLD|ITALIC.
     * @param textcolor color of the text (integer packed ARGB)
     */
    public NewText(String textstr, int fontsize, int style, Color textcolor) {
        this(textstr,fontsize,style, textcolor, NewText.isLatex(textstr));
    }

    /**
     * Creates a new Text object with the specified string and font configuration.
     * @param textstr the text to be displayed
     * @param fontsize point size of the font
     * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
     * or bitwise union BOLD|ITALIC.
     */
    public NewText(String textstr, int fontsize, int style) {
        this(textstr,fontsize,style, new Color(96, 96, 96), NewText.isLatex(textstr));
    }

    /**
     * Sets the color of this text
     * @param color to set
     * @return this for chaining
     */
    @DebugSetter(ID = "color", creator = ColorPicker.class)
    public NewText setColor(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the color of this text in integer packed ARGB format.
     * e.g. 0xff00ff00 for opaque green.
     * @param argb integer packed ARGB color value
     * @return this for chaining
     */
    public NewText setColor(int argb) {
        return this.setColor(new Color(argb, true));
    }

    /**
     * @return this text's color
     */
    @DebugGetter(ID = "color")
    public Color getColor() {
        return color;
    }

    /**
     * Sets the background color of the text, per default this is
     * transparent black (0x00000000) which wont be visible.
     * @param background color
     */
    @DebugSetter(ID = "background", creator = ColorPicker.class)
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

    @DebugGetter(ID = "background")
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
    public NewText setPickColor(int pickColor) {
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
     * @return the normalized alpha channel of the picking color (in [0,1])
     */
    public float getPickColorA() {
        return Pixel.a(pickColor)/255f;
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
        double width = 0;
        double height = 0;
        for (String line : getTextString().split(Pattern.quote(getLineBreakSymbol()))) {
            NewText tempText = new NewText(line, fontsize, style, getColor(), isLatex());
            tempText.setInsets(getInsets());
            width = Math.max(width, getBounds(tempText).getWidth());
            height += getBounds(tempText).getHeight();
        }
        return new Rectangle2D.Double(getOrigin().getX(), getOrigin().getY(), width, height);
    }

    // get bounds without respect for line breaks
    private static Rectangle2D getBounds(NewText text) {
        if (text.isLatex())
            return getLatexBounds(text);
        return new Rectangle2D.Double(text.getOrigin().getX(), text.getOrigin().getY(), text.getTextSize().getWidth() + (text.getInsets().left+text.getInsets().right), text.getTextSize().getHeight() + (text.getInsets().bottom+text.getInsets().top));
    }


    /**
     * @return the bounding rectangle of this text
     */
    public Rectangle2D getBoundsPDF() {
        double width = 0;
        double height = 0;
        for (String line : getTextString().split(Pattern.quote(getLineBreakSymbol()))) {
            try {
                NewText tempText = new NewText(line, fontsize, style, getColor(), isLatex());
                width = Math.max(width, getBoundsPDF(tempText).getWidth());
                height += getBoundsPDF(tempText).getHeight();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new Rectangle2D.Double(getOrigin().getX(), getOrigin().getY(), width, height);
    }

    private static Rectangle2D getBoundsPDF(NewText text) throws IOException {
        if (text.isLatex())
            return getLatexBounds(text);
        return PDFUtils.getPDFTextLineBounds(text);
    }


    /**
     * @return the bounding rectangle of this text
     */
    public Rectangle2D getBoundsSVG() {
        double width = 0;
        double height = 0;
        for (String line : getTextString().split(Pattern.quote(getLineBreakSymbol()))) {
            try {
                NewText tempText = new NewText(line, fontsize, style, getColor(), isLatex());
                width = Math.max(width, getBoundsSVG(tempText).getWidth());
                height += getBoundsSVG(tempText).getHeight();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new Rectangle2D.Double(getOrigin().getX(), getOrigin().getY(), width, height);
    }

    private static Rectangle2D getBoundsSVG(NewText text) throws IOException {
        if (text.isLatex())
            return getLatexBounds(text);
        return PDFUtils.getPDFTextLineBounds(text);
    }

    private static Rectangle2D getLatexBounds(NewText text) {
        TeXFormula formula = new TeXFormula(text.getTextString());
        TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, text.fontsize);
        icon.setInsets(new Insets(text.getInsets().top, text.getInsets().left, text.getInsets().bottom, text.getInsets().right));
        return new Rectangle2D.Double(text.getOrigin().getX(), text.getOrigin().getY(), icon.getIconWidth(), icon.getIconHeight());
    }

    /**
     * @return the bounding rectangle of this text with its rotation taken into account.
     */
    public Rectangle2D getBoundsWithRotation() {
        Rectangle2D bounds = getBounds();
        AffineTransform transform = AffineTransform.getRotateInstance(angle);
        return transform.createTransformedShape(bounds).getBounds2D();
    }

    /**
     * @return the origin of this text object, i.e. the bottom left corner of the rectangle enclosing the text,
     * the text's location so to say
     */
    @DebugGetter(ID = "origin")
    public Point2D getOrigin() {
        return origin;
    }

    /**
     * Sets the origin of this text object, i.e. the bottom left corner of the rectangle enclosing the text,
     * the text's location so to say
     * @param origin to set
     * @return this for chaining
     */
    public NewText setOrigin(Point2D origin) {
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
    @DebugSetter(ID = "origin", creator = Coord2DCreator.class)
    public NewText setOrigin(int x, int y) {
        return this.setOrigin(new Point(x, y));
    }

    /**
     * @return the rotation angle in radian by which this text object is rotated around its origin.
     */
    @DebugGetter(ID = "angle")
    public float getAngle() {
        return angle;
    }

    /**
     * Sets the rotation angle in radian by which this text object is rotated around its origin.
     * @param angle rotation angle
     * @return this for chaining
     */
    @DebugSetter(ID = "angle", creator = AngleSliderCreator.class)
    public NewText setAngle(double angle) {
        this.angle = (float)angle;
        return this;
    }

    /**
     * @return the TextDecoration of the text object (underline or strikethrough).
     */
    public TextDecoration getTextDecoration() {
        return textDecoration;
    }

    /**
     * Sets a new {@link TextDecoration} (underline or strikethrough) to the text object.
     * Only one of both (underline or strikethrough) can be active at a time.
     *
     * @param textDecoration the new text decoration
     * this for chaining
     */
    public NewText setTextDecoration(TextDecoration textDecoration) {
        this.textDecoration = textDecoration;
        return this;
    }

    /**
     * @return the currently active {@link PositioningRectangle}
     */
    public PositioningRectangle getPositioningRectangle() {
        if (Objects.nonNull(positioningRectangle)) {
            return positioningRectangle;
        }
        return new PositioningRectangle(0,0);
    }

    /**
     * Sets the {@link PositioningRectangle}.
     *
     * @param positioningRectangle the new {@link PositioningRectangle}
     * @return this for chaining
     */
    public NewText setPositioningRectangle(PositioningRectangle positioningRectangle) {
        this.positioningRectangle = positioningRectangle;
        return this;
    }

    /**
     * Returns the line breaking symbol.
     * The symbol is different in normal mode and in latex mode,
     * so this method returns the one which is currently valid.
     *
     * @return returns the line break symbol which is currently valid
     */
    public String getLineBreakSymbol() {
        if (isLatex())
            return "\\\\";
        return "\n";
    }

    @Override
    @DebugGetter(ID = "hidden")
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Hides or unhides this Text object, i.e. sets the {@link #isHidden()} field
     * value. When hidden, renderers will not draw it.
     * @param hide true when hiding
     * @return this for chaining
     */
    @DebugSetter(ID = "hidden", creator = ButtonCreator.class)
    public NewText hide(boolean hide) {
        this.hidden = hide;
        return this;
    }

    /**
     * @return if the text object is written in latex
     */
    public boolean isLatex() {
        return latex;
    }


    /**
     * Determines if the given String will be evaluated as a latex string by the {@link NewText} object.
     * This is dependent on the beginning of the String, as it has to start with the specified {@link #latexInstruction}
     *
     * @param toEvaluate the string which will be evaluated if it will be interpreted as a latex string
     * @return true if it will be interpreted as a latex string, false if not
     */
    public static boolean isLatex(String toEvaluate) {
        return toEvaluate.startsWith(latexInstruction);
    }

    /**
     * Determines if latex mode should be switched on or off.
     *
     * @param latex true if latex mode should be switched on, false if off
     * @return this for chaining
     */
    public NewText setLatex(boolean latex) {
        this.latex = latex;
        return this;
    }

    /**
     *
     * @return
     */
    public Insets getInsets() {
        return insets;
    }

    /**
     *
     * @param insets
     * @return this for chaining
     */
    public NewText setInsets(Insets insets) {
//        System.out.println(insets);
        this.insets = insets;
        return this;
    }

    /**
     * Allocates GL resources, i.e. creates the vertex array and fills
     * it according to the contents of this {@link Text} object.
     * If the vertex array has already been created, nothing happens.
     */
    @Override
    @Annotations.GLContextRequired
    public void initGL(){
        if(Objects.isNull(va)){
            va = new VertexArray(2);
            updateGL(false);
        }
    }

    /**
     * Updates the vertex array to be in sync with this text object.
     * This sets the {@link #isDirty()} state to false.
     * if {@link #initGL()} has not been called yet or this object has
     * already been closed, nothing happens
     */
    @Override
    @Annotations.GLContextRequired
    public void updateGL(boolean useGLDoublePrecision) {
        /* We never use double precision for text vertex arrays.
         * So we only need to update when isDirty, but not on change of requested precision.
         */
        if(Objects.nonNull(va) && isDirty){
            CharacterAtlas.get(fontsize, style).createVAforString(txtStr, va);
            isDirty = false;
        }
    }

    /**
     * disposes of the GL resources of this text object,
     * i.e deletes the vertex array.
     */
    @Override
    @Annotations.GLContextRequired
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
     * This indicates that an {@link #updateGL(boolean)} call is necessary to sync GL resources.
     * @return this for chaining
     */
    public NewText setDirty() {
        this.isDirty = true;
        return this;
    }

    /**
     * Returns the GL object name of the texture of the character atlas this text's font
     * corresponds to.
     * @return the texture id to use for texturing this text object
     * @throws IllegalStateException when no {@link FBOCanvas} is currently active
     */
    @Annotations.GLContextRequired
    public int getTextureID(){
        return CharacterAtlas.get(fontsize, style).getTexID();
    }

    /**
     * @return the String this text object displays
     */
    @DebugGetter(ID ="txtStr")
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
    @DebugSetter(ID ="txtStr", creator = TextfieldCreator.class)
    public NewText setTextString(String txtStr) {
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
    @Annotations.GLContextRequired
    public void bindVertexArray() {
        va.bindAndEnableAttributes(0,1);
    }


    /**
     * Releases this objects vertex array and disables the corresponding attributes
     * @throws NullPointerException unless {@link #initGL()} was called (and this has not yet been closed)
     */
    @Annotations.GLContextRequired
    public void releaseVertexArray() {
        va.releaseAndDisableAttributes(0,1);
    }

    @Override
    public boolean isGLDoublePrecision() {
        return false;
    }
}
