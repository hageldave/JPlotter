package hageldave.jplotter.renderers;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.font.FontProvider;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.NewText;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderables.TextDecoration;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations;
import hageldave.jplotter.util.ShaderRegistry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class NewTextRenderer extends GenericRenderer<NewText> {
    protected static final char NL = '\n';

    protected static final String vertexShaderSrcD = "";
    protected static final String vertexShaderSrc = "";
    protected static final String fragmentShaderSrc = "";

    /**
     * Left (lower) cut off parameter of the smooth step function for text rendering
     * in the fragment shader.
     * This is used to tune the sharpness and aliasing of characters for font sizes 10 to 24.
     */
    protected static final double[] smoothStepLeft =
            {0.39, 0.42, 0.42, 0.40, 0.41, 0.41, 0.43, 0.43, 0.43, 0.44, 0.44, 0.44, 0.45, 0.47, 0.47};
    /**
     * Right (upper) cut off parameter of the smooth step function for text rendering
     * in the fragment shader.
     * This is used to tune the sharpness and aliasing of characters for font sizes 10 to 24.
     */
    protected static final double[] smoothStepRight =
            {0.58, 0.58, 0.58, 0.62, 0.62, 0.63, 0.61, 0.61, 0.61, 0.60, 0.59, 0.58, 0.57, 0.55, 0.55};

    protected VertexArray vaTextBackground;


    /**
     * Creates the shader if not already created and
     * calls {@link Renderable#initGL()} for all items
     * already contained in this renderer.
     * Items that are added later on will be initialized during rendering.
     */
    @Override
    @Annotations.GLContextRequired
    public void glInit() {

    }

    /**
     * Disables {@link GL11#GL_DEPTH_TEST},
     * enables {@link GL11#GL_BLEND}
     * ,sets {@link GL11#GL_SRC_ALPHA}, {@link GL11#GL_ONE_MINUS_SRC_ALPHA}
     * as blend function
     * and activates the 0th texture unit {@link GL13#GL_TEXTURE0}.
     */
    @Override
    @Annotations.GLContextRequired
    protected void renderStart(int w, int h, Shader shader) {

    }

    @Override
    @Annotations.GLContextRequired
    protected void renderItem(NewText txt, Shader shader) {

    }

    /**
     * disables {@link GL11#GL_BLEND},
     * enables {@link GL11#GL_DEPTH_TEST},
     * releases still bound texture.
     */
    @Override
    @Annotations.GLContextRequired
    protected void renderEnd() {
        GL13.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    /**
     * Disposes of GL resources, i.e. closes the shader.
     * It also deletes (closes) all {@link Text}s contained in this
     * renderer.
     */
    @Override
    @Annotations.GLContextRequired
    public void close() {
        if(Objects.nonNull(shaderF))
            ShaderRegistry.handbackShader(shaderF);
        shaderF = null;
        if(Objects.nonNull(shaderD))
            ShaderRegistry.handbackShader(shaderD);
        shaderD = null;
        if(Objects.nonNull(vaTextBackground))
            vaTextBackground.close();
        vaTextBackground = null;
        closeAllItems();
    }

    @Override
    public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
        if(!isEnabled()){
            return;
        }

        double translateX = Objects.isNull(view) ? 0:view.getX();
        double translateY = Objects.isNull(view) ? 0:view.getY();
        double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
        double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

        Rectangle vpRect = new Rectangle(w, h);

        for(NewText txt: getItemsToRender()){
            if(txt.isHidden() || txt.getTextString().isEmpty()){
                continue;
            }
            double x1,y1,angle;
            x1 = txt.getOrigin().getX();
            y1 = txt.getOrigin().getY();
            angle = txt.getAngle();

            x1-=translateX;
            y1-=translateY;
            x1*=scaleX;
            y1*=scaleY;

            y1+=1;

            double effectiveTx = x1-txt.getOrigin().getX();
            double effectiveTy = y1-txt.getOrigin().getY();
            // test if inside of view port
            Rectangle2D txtrect = txt.getBoundsWithRotation();
            txtrect.setRect(
                    txtrect.getX()+effectiveTx,
                    txtrect.getY()+effectiveTy,
                    txtrect.getWidth(), txtrect.getHeight()
            );

            if(!txtrect.intersects(vpRect)) {
                continue;
            }
            // create a proxy graphics object to draw the string to
            Graphics2D g_ = (Graphics2D) g.create();
            Graphics2D p_ = (Graphics2D) p.create();

            if (txt.isLatex()) {
                TeXFormula formula = new TeXFormula(txt.getTextString());
                TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, txt.fontsize);
                icon.setInsets(new Insets(txt.getInsets().top, txt.getInsets().left, txt.getInsets().bottom, txt.getInsets().right));
                BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                g2.setColor(txt.getBackground());
                g2.fillRect(0,0, icon.getIconWidth(), icon.getIconHeight());
                JLabel jl = new JLabel();
                jl.setForeground(txt.getColor());
                icon.paintIcon(jl, g2, 0, 0);

                AffineTransform trnsfrm = new AffineTransform();
                trnsfrm.translate(x1-txt.getPositioningRectangle().getAnchorPoint().getX(), y1/*+txt.getBounds().getHeight()*/ + icon.getIconHeight() -txt.getPositioningRectangle().getAnchorPoint().getY());
                trnsfrm.scale(1, -1);
                if(angle != 0.0)
                    trnsfrm.rotate(-angle, txt.getPositioningRectangle().getAnchorPoint().getX(), icon.getIconHeight()-txt.getPositioningRectangle().getAnchorPoint().getY());

                g_.transform(trnsfrm);
                g_.drawImage(image, null, 0, 0);

                if(txt.getPickColor() != 0) {
                    p_.transform(trnsfrm);
                    p_.setColor(new Color(txt.getPickColor()));

                    int index = 1;
                    AffineTransform initTransform = p_.getTransform();
                    for (String line : txt.getTextString().split(Pattern.quote(txt.getLineBreakSymbol()))) {
                        NewText tempText = new NewText(line, txt.fontsize, txt.style, txt.getColor());
                        TeXFormula tempFormula = new TeXFormula(tempText.getTextString());
                        TeXIcon tempIcon = tempFormula.createTeXIcon(TeXConstants.STYLE_DISPLAY, tempText.fontsize);
                        tempIcon.setInsets(new Insets(txt.getInsets().top, txt.getInsets().left, txt.getInsets().bottom, txt.getInsets().right));

                        trnsfrm = new AffineTransform();
                        trnsfrm.translate(0, tempText.getBounds().getHeight() * index);
                        trnsfrm.scale(1, -1);
                        p_.transform(trnsfrm);

                        Rectangle2D rect = new Rectangle2D.Double(0.0, 0.0, tempIcon.getIconWidth(), tempIcon.getIconHeight());
                        p_.fill(rect);
                        p_.setTransform(initTransform);
                        index++;
                    }
                }
            } else {
                Font font = FontProvider.getUbuntuMono(txt.fontsize, txt.style);
                g_.setFont(font);
                p_.setFont(font);

                if (txt.getTextDecoration() == TextDecoration.UNDERLINE) {
                    Map attributes = font.getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    g_.setFont(font.deriveFont(attributes));
                    p_.setFont(font.deriveFont(attributes));
                }
                if (txt.getTextDecoration() == TextDecoration.STRIKETHROUGH) {
                    Map attributes = font.getAttributes();
                    attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                    g_.setFont(font.deriveFont(attributes));
                    p_.setFont(font.deriveFont(attributes));
                }

                /* translate to text origin,
                 * flip vertically (AWT coordinates, so text is not upside down),
                 * rotate according to angle */
                AffineTransform trnsfrm = new AffineTransform();
                trnsfrm.translate(x1-txt.getPositioningRectangle().getAnchorPoint().getX(), y1-txt.getPositioningRectangle().getAnchorPoint().getY());
                trnsfrm.scale(1, -1);
                if(angle != 0.0)
                    trnsfrm.rotate(-angle, txt.getPositioningRectangle().getAnchorPoint().getX(), -txt.getPositioningRectangle().getAnchorPoint().getY());
                g_.transform(trnsfrm);
                p_.transform(trnsfrm);

                // draw background rectangle
                if(txt.getBackground().getRGB() != 0) {
                    g_.setColor(txt.getBackground());
                    Rectangle2D bounds = txt.getBounds();
                    float rightpadding = 0.4f*((float)bounds.getWidth()/txt.getTextString().length());
                    Rectangle2D rect = new Rectangle2D.Double(0.0, -bounds.getHeight(), bounds.getWidth()+rightpadding, bounds.getHeight());
                    g_.fill(rect);
                }
                // draw string
                int maxDescent = (int) (g_.getFontMetrics().getHeight()-txt.getBounds().getHeight() - g.getFontMetrics().getMaxDescent());
                g_.setColor(txt.getColor());

                for (String line : txt.getTextString().split(Pattern.quote(txt.getLineBreakSymbol()))) {
                    g_.drawString(line, 0, maxDescent);
                    maxDescent += g.getFontMetrics().getHeight();
                }

                if(txt.getPickColor() != 0) {
                    p_.setColor(new Color(txt.getPickColor()));
                    int index = 0;
                    for (String line : txt.getTextString().split(Pattern.quote(txt.getLineBreakSymbol()))) {
                        NewText tempText = new NewText(line, txt.fontsize, txt.style, txt.getColor());
                        Rectangle2D rect = new Rectangle2D.Double(0.0, -txt.getBounds().getHeight()+(tempText.getBounds().getHeight()*index), tempText.getBounds().getWidth(), tempText.getBounds().getHeight());
                        p_.fill(rect);
                        index++;
                    }
                }
            }
        }
    }

    @Override
    public void renderSVG(Document doc, Element parent, int w, int h) {
        if(!isEnabled()){
            return;
        }
        Element mainGroup = SVGUtils.createSVGElement(doc, "g");
        parent.appendChild(mainGroup);

        double translateX = Objects.isNull(view) ? 0:view.getX();
        double translateY = Objects.isNull(view) ? 0:view.getY();
        double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
        double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

        for(NewText txt: getItemsToRender()){
            if(txt.isHidden() || txt.getTextString().isEmpty()){
                continue;
            }
            {
                double x1,y1;
                x1 = txt.getOrigin().getX();
                y1 = txt.getOrigin().getY();

                x1-=translateX;
                y1-=translateY;
                x1*=scaleX;
                y1*=scaleY;
                y1+=1;

                // test if inside of view port
                Rectangle2D bounds = txt.getBoundsWithRotation();
                AffineTransform trnsfrm = new AffineTransform();
                trnsfrm.translate(-txt.getOrigin().getX(), -txt.getOrigin().getY());
                trnsfrm.translate(x1, y1);
                trnsfrm.translate(-txt.getPositioningRectangle().getAnchorPoint().getX(), -txt.getPositioningRectangle().getAnchorPoint().getY()-txt.getBounds().getHeight());
                bounds = trnsfrm.createTransformedShape(bounds).getBounds2D();
                Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
                if(!viewportRect.intersects(bounds)) {
                    continue;
                }

                Element textGroup = SVGUtils.createSVGElement(doc, "g");
                mainGroup.appendChild(textGroup);

                if (txt.isLatex()) {
                    try {
                        Element svgLatex = SVGUtils.latexToSVG(txt, doc, 0, 0);
                        textGroup.appendChild(svgLatex);
                        textGroup.setAttributeNS(null, "transform", "translate("+SVGUtils.svgNumber(x1-txt.getPositioningRectangle().getAnchorPoint().getX())+","+SVGUtils.svgNumber(y1-txt.getPositioningRectangle().getAnchorPoint().getY()+txt.getBounds().getHeight())+")" + "rotate(" + SVGUtils.svgNumber(txt.getAngle() * 180 / Math.PI) + ")" + "scale(1,-1)");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    double textHeight = 0;
                    double rightPadding = 6 * (txt.getBounds().getWidth()/txt.getTextString().length());

                    if(txt.getBackground().getRGB() != 0){
                        Element backgroundRect = SVGUtils.createSVGRect(doc, 0, 0, txt.getBounds().getWidth()+rightPadding, txt.getBounds().getHeight());
                        textGroup.appendChild(backgroundRect);
                        backgroundRect.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(txt.getBackground().getRGB()));
                        backgroundRect.setAttributeNS(null, "fill-opacity", ""+SVGUtils.svgNumber(Pixel.a_normalized(txt.getBackground().getRGB())));
                    }

                    for (String line : txt.getTextString().split(Pattern.quote("\n"))) {
                        NewText tempText = new NewText(line, txt.fontsize, txt.style, txt.getColor());
                        Element text = SVGUtils.createSVGElement(doc, "text");
                        textGroup.appendChild(text);

                        text.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
                        text.setTextContent(line);
                        String fontfamily = "'Ubuntu Mono', monospace";
                        text.setAttributeNS(null, "style",
                                "font-family:" + fontfamily + ";font-size:" + txt.fontsize + "px;" + SVGUtils.fontStyleAndWeightCSS(txt.style));
                        text.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(txt.getColor().getRGB()));
                        if (txt.getColorA() != 1) {
                            text.setAttributeNS(null, "fill-opacity", SVGUtils.svgNumber(txt.getColorA()));
                        }
                        text.setAttributeNS(null, "x", "" + 0);
                        text.setAttributeNS(null, "y", "-" + (txt.getTextSize().height - txt.fontsize) );

                        if (txt.getTextDecoration() == TextDecoration.UNDERLINE) {
                            text.setAttributeNS(null, "text-decoration", "underline");
                        }

                        if (txt.getTextDecoration() == TextDecoration.STRIKETHROUGH) {
                            text.setAttributeNS(null, "text-decoration", "line-through");
                        }

                        textGroup.setAttributeNS(null, "transform", "translate("+SVGUtils.svgNumber(x1-txt.getPositioningRectangle().getAnchorPoint().getX())+","+SVGUtils.svgNumber(y1-txt.getPositioningRectangle().getAnchorPoint().getY())+")" + "rotate(" + SVGUtils.svgNumber(txt.getAngle() * 180 / Math.PI)+")");
                        text.setAttributeNS(null, "transform", "translate(" + SVGUtils.svgNumber(0) + "," + SVGUtils.svgNumber(- textHeight + (txt.getBounds().getHeight()-txt.getTextSize().getHeight())) + ") scale(1,-1)");
                        textHeight += tempText.getBounds().getHeight();
                    }
                }
            }
        }
    }

    @Override
    public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
        if(!isEnabled()){
            return;
        }

        double translateX = Objects.isNull(view) ? 0:view.getX();
        double translateY = Objects.isNull(view) ? 0:view.getY();
        double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
        double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
        try {
            PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
            for(NewText txt: getItemsToRender()) {
                if (txt.isHidden() || txt.getTextString().isEmpty()) {
                    continue;
                }
                {
                    double x1, y1;
                    x1 = txt.getOrigin().getX();
                    y1 = txt.getOrigin().getY();
                    x1 -= translateX;
                    y1 -= translateY;
                    x1 *= scaleX;
                    y1 *= scaleY;

                    // test if inside of view port
                    Rectangle2D bounds = txt.getBoundsWithRotation();
                    AffineTransform trnsfrm = new AffineTransform();
                    trnsfrm.translate(-txt.getOrigin().getX(), -txt.getOrigin().getY());
                    trnsfrm.translate(x1, y1);
                    trnsfrm.translate(-txt.getPositioningRectangle().getAnchorPoint().getX(), -txt.getPositioningRectangle().getAnchorPoint().getY());
                    bounds = trnsfrm.createTransformedShape(bounds).getBounds2D();
                    Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
                    if(!viewportRect.intersects(bounds)) {
                        continue;
                    }

                    // clipping area
                    contentStream.saveGraphicsState();
                    contentStream.addRect(x, y, w, h);
                    contentStream.clip();

                    PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                    graphicsState.setNonStrokingAlphaConstant(txt.getColorA());
                    contentStream.setGraphicsStateParameters(graphicsState);

                    if (txt.isLatex()) {
                        PDFUtils.latexToPDF(doc, contentStream, txt, x1+x, y1+y);
                    } else {
                        PDFUtils.createPDFText(doc, contentStream, txt, new Point2D.Double(x1+x, y1+y));
                    }
                    // restore graphics
                    contentStream.restoreGraphicsState();
                }
            }
            contentStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Error occurred!");
        }
    }
}
