package hageldave.jplotter.renderables;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.debugging.annotations.DebugGetter;
import hageldave.jplotter.debugging.annotations.DebugSetter;
import hageldave.jplotter.debugging.panelcreators.control.*;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.pdf.FontCachedPDDocument;
import hageldave.jplotter.renderers.NewTextRenderer;
import hageldave.jplotter.util.Annotations;
import hageldave.jplotter.util.Pair;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Abstract class for {@link Renderable}s representing text that can be rendered using the
 * {@link NewTextRenderer}.
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
 * <li>latex - true if latex mode is on, false if it's off</li>
 * <li>text decoration - the text decoration of the text, which can be either set to underline or strikethrough</li>
 * <li>insets - the padding of the text object</li>
 * <li>transformationCenter - the transformation center of the text object, so that the text object can be aligned at its right upper corner for example</li>
 * </ul>
 * @author hageldave
 */
public class NewText implements Renderable, Cloneable {
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
    protected int textDecoration = -1;
    protected Pair<Double, Double> transformationCenter = new Pair<>(0.0, 0.0);
    public final static int UNDERLINE = 0, STRIKETHROUGH = 1;

    /**
     * Creates a new Text object with the specified string and font configuration.
     * @param textstr the text to be displayed
     * @param fontsize point size of the font
     * @param style of the font - one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
     * or bitwise union BOLD|ITALIC.
     * @param textcolor color of the text
     * @param latex determines if latex mode will be turned on or not
     */
    protected NewText(String textstr, int fontsize, int style, Color textcolor, boolean latex) {
        this.fontsize = fontsize;
        this.style = style;
        this.color = textcolor;
        this.origin = new Point(0, 0);
        this.latex = latex;
        setTextString(textstr);
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        NewText clonedTextObject = (NewText) super.clone();
        clonedTextObject.textSize = (Dimension) clonedTextObject.getTextSize().clone();
        clonedTextObject.setOrigin((Point2D) clonedTextObject.getOrigin().clone());
        clonedTextObject.setInsets((Insets) clonedTextObject.getInsets().clone());
        return clonedTextObject;
    }

    /**
     * As the {@link NewText} object supports line breaks,
     * this method splits the text object into multiple new ones at each line break symbol (e.g. \n in normal text rendering or \\ in latex rendering).
     * The resulting text objects only contain single lines.
     *
     * @return array containing each single line {@link NewText} object
     */
    public NewText[] generateTextObjectForEachLine() {
        List<NewText> singleLineTextObjects = new LinkedList<>();
        for (String newLine : getTextString().split(Pattern.quote(getLineBreakSymbol()))) {
            try {
                NewText singleLineText = (NewText) this.clone();
                singleLineText.setTextString(newLine);
                singleLineTextObjects.add(singleLineText);
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
        return singleLineTextObjects.toArray(new NewText[0]);
    }

    /**
     * @return the bounding rectangle of this text
     */
    public Rectangle2D getBounds() {
        double width = 0;
        double height = 0;
        for (NewText lineTextObject : generateTextObjectForEachLine()) {
            if (lineTextObject.isLatex()) {
                Rectangle2D latexBounds = getLatexBounds(lineTextObject);
                width = Math.max(width, latexBounds.getWidth());
                height += latexBounds.getHeight();
            } else {
                width = Math.max(width, lineTextObject.getTextSize().getWidth() + getHorizontalInsets());
                height += lineTextObject.getTextSize().getHeight() + getVerticalInsets();
            }
        }
        return new Rectangle2D.Double(getOrigin().getX(), getOrigin().getY(), width, height);
    }

    /**
     * @return the bounding rectangle of this text, when exported
     * The bounds can differ from the normal {{@link #getBounds()}} method because of font rendering differences.
     */
    public Rectangle2D getExportBounds() {
        double width = 0;
        double height = 0;
        for (NewText lineTextObject : generateTextObjectForEachLine()) {
            Rectangle2D boundsExport;
            if (lineTextObject.isLatex()) {
                boundsExport = getLatexBounds(lineTextObject);
            } else {
                boundsExport = getPDFTextBoundsWithoutLineBreaks(lineTextObject);
            }
            width = Math.max(width, boundsExport.getWidth());
            height += boundsExport.getHeight();
        }
        return new Rectangle2D.Double(getOrigin().getX(), getOrigin().getY(), width, height);
    }


    protected static Rectangle2D getPDFTextBoundsWithoutLineBreaks(NewText txt) {
        FontCachedPDDocument doc = new FontCachedPDDocument();
        PDType0Font font = doc.getFont(txt.style);
        try {
            double width = font.getStringWidth(txt.getTextString()) / 1000 * txt.fontsize;
            double height = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * txt.fontsize;
            doc.close();
            return new Rectangle2D.Double(txt.getOrigin().getX(), txt.getOrigin().getY(), width + txt.getHorizontalInsets(), height + txt.getVerticalInsets());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Rectangle2D getLatexBounds(NewText text) {
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
        AffineTransform transform = AffineTransform.getRotateInstance(angle, getTransformedBounds().getX(), getTransformedBounds().getY());
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
     * @return the TextDecoration of the text object (underline [0] or strikethrough [1]).
     */
    @DebugGetter(ID = "textDecoration")
    public int getTextDecoration() {
        return textDecoration;
    }

    /**
     * Sets a new text decoration (underline [0] or strikethrough [1]) to the text object.
     * Only one of both (underline or strikethrough) can be active at a time.
     * <br>
     * Warning: This does not style the text object if it's in latex mode,
     * as the styling has to be done in latex commands.
     *
     * @param textDecoration the new text decoration
     * this for chaining
     */
    @DebugSetter(ID = "textDecoration", creator = TextDecorationCreator.class)
    public NewText setTextDecoration(int textDecoration) {
        if (textDecoration > 1 || textDecoration < -1) {
            throw new IllegalArgumentException("Only value between -1 and 1 allowed");
        }
        this.textDecoration = textDecoration;
        return this;
    }

    /**
     * @return the bounds of the text object with the transformation center taken into account
     */
    public Point2D.Double getTransformedBounds() {
        return new Point2D.Double(this.getBounds().getWidth()*transformationCenter.first, this.getBounds().getHeight()*transformationCenter.second);
    }

    /**
     * @return the export bounds of the text object with the transformation center taken into account
     */
    public Point2D.Double getTransformedExportBounds() {
        return new Point2D.Double(this.getExportBounds().getWidth()*transformationCenter.first, this.getExportBounds().getHeight()*transformationCenter.second);
    }

    /**
     * @return the transformation center of this text object
     */
    @DebugGetter(ID = "transformationCenter")
    public Pair<Double, Double> getTransformationCenter() {
        return this.transformationCenter;
    }

    /**
     * Sets the transformation center of the text object, which influences both rotation and translation.
     * The translation center determines at which point the text object will be translated.
     * The x and y parameters therefore can be between 0 and 1, where
     * - for x 0 means left, 0.5 middle and 1 right
     * - for y 0 means bottom, 0.5 middle and 1 top.
     * <br>
     * If a text object should be aligned with its right border at the x-coordinate 2.0,
     * the origin (2, 2) can be set, with a transformation center of (1, 0).
     * <br>
     * The default behavior is a transformation center of (0, 0), where each transformation will be oriented around the lower left corner.
     *
     * @param transformationCenter the values for the transformation center that influences the x and y coordinates
     *                             The first value of the {@link Pair} data structure correlates to the x coordinate, the second to the y coordinate.
     * @return this for chaining
     */
    public NewText setTransformationCenter(Pair<Double, Double> transformationCenter) {
        if (transformationCenter.first < 0 || transformationCenter.first > 1 || transformationCenter.second < 0 || transformationCenter.second > 1) {
            throw new IllegalArgumentException("Values have to be between 0 and 1");
        }
        this.transformationCenter = transformationCenter;
        return this;
    }

    /**
     * Sets the transformation center of the text object, which influences both rotation and translation.
     * The translation center determines at which point the text object will be translated.
     * The x and y parameters therefore can be between 0 and 1, where
     * - for x 0 means left, 0.5 middle and 1 right
     * - for y 0 means bottom, 0.5 middle and 1 top.
     * <br>
     * If a text object should be aligned with its right border at the x-coordinate 2.0,
     * the origin (2, 2) can be set, with a transformation center of (1, 0).
     * <br>
     * The default behavior is a transformation center of (0, 0), where each transformation will be oriented around the lower left corner.
     *
     * @param x the value for the transformation center that influences the x coordinate
     * @param y the value for the transformation center that influences the y coordinate
     * @return this for chaining
     */
    @DebugSetter(ID = "transformationCenter", creator = Interpolation2DCreator.class)
    public NewText setTransformationCenter(double x, double y) {
        return setTransformationCenter(new Pair<>(x, y));
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
    @DebugGetter(ID = "latex")
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
    @DebugSetter(ID = "latex", creator = ButtonCreator.class)
    public NewText setLatex(boolean latex) {
        this.latex = latex;
        return this;
    }

    /**
     * @return the {@link Insets} of the text object
     */
    @DebugGetter(ID = "insets")
    public Insets getInsets() {
        return insets;
    }

    /**
     * Sets the {@link Insets} of the {@link NewText} object.
     * The insets define the padding between the actual text and the border of the text object.
     *
     * @param insets Insets object defining the size of the insets
     * @return this for chaining
     */
    @DebugSetter(ID = "insets", creator = InsetsCreator.class)
    public NewText setInsets(Insets insets) {
        this.insets = insets;
        return this;
    }

    /**
     * Returns the height of the vertical insets, which are the sum of the top and bottom insets.
     *
     * @return the sum of the top and bottom inset
     */
    public double getVerticalInsets() {
        return getInsets().top + getInsets().bottom;
    }

    /**
     * Returns the width of the horizontal insets, which are the sum of the left and right insets.
     *
     * @return the sum of the left and right inset
     */
    public double getHorizontalInsets() {
        return getInsets().left + getInsets().right;
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
     * The method also detects if the string starts with the {@link #latexInstruction} and renders it in latex if it's the case.
     * This set the {@link #isDirty()} state of this {@link Renderable} to true.
     * @param txtStr the text string this object should display.
     * @return this for chaining
     */
    @DebugSetter(ID ="txtStr", creator = TextfieldCreator.class)
    public NewText setTextString(String txtStr) {
        if (latex || txtStr.startsWith(latexInstruction)) {
            this.txtStr = txtStr.replace(latexInstruction, "");
        } else {
            this.txtStr = txtStr;
        }
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
