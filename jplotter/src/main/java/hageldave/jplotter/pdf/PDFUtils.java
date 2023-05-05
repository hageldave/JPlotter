package hageldave.jplotter.pdf;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.font.FontProvider;
import hageldave.jplotter.renderables.NewText;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.util.Utils;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType4;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.scilab.forge.jlatexmath.DefaultTeXFont;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.scilab.forge.jlatexmath.cyrillic.CyrillicRegistration;
import org.scilab.forge.jlatexmath.greek.GreekRegistration;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

import static hageldave.jplotter.renderables.NewText.STRIKETHROUGH;
import static hageldave.jplotter.renderables.NewText.UNDERLINE;

/**
 * Utility class for PDF related methods.
 */
public class PDFUtils {

    /**
     * Creates a point at the specified position with the given radius.
     *
     * @param cs content stream that the point is appended to
     * @param x x coordinate of the point
     * @param y y coordinate of the point
     * @param radius radius of the point
     * @return resulting content stream
     * @throws IOException If there is an error while creating the point in the document
     */
    public static PDPageContentStream createPDFPoint(PDPageContentStream cs,
                                                     float x, float y, float radius) throws IOException {
        final float k = 0.552284749831f;
        cs.moveTo(x - radius, y);
        cs.curveTo(x - radius, y + k * radius, x - k * radius, y + radius, x, y + radius);
        cs.curveTo(x + k * radius, y + radius, x + radius, y + k * radius, x + radius, y);
        cs.curveTo(x + radius, y - k * radius, x + k * radius, y - radius, x, y - radius);
        cs.curveTo(x - k * radius, y - radius, x - radius, y - k * radius, x - radius, y);
        return cs;
    }

    /**
     * Creates a simple segment in the pdf document.
     *
     * @param cs content stream that the segment is appended to
     * @param p0 starting point of the segment
     * @param p1 ending point of the segment
     * @return resulting content stream
     * @throws IOException If there is an error while creating the line segment in the document
     */
    public static PDPageContentStream createPDFSegment(PDPageContentStream cs, Point2D p0,
                                                       Point2D p1) throws IOException {
        cs.moveTo((float) p0.getX(), (float) p0.getY());
        cs.lineTo((float) p1.getX(), (float) p1.getY());
        return cs;
    }

    /**
     * Creates a cubic bézier curve in the pdf document.
     *
     * @param cs content stream that the curve is appended to
     * @param p0 starting point of the curve
     * @param cP0 first control point
     * @param cP1 second control point
     * @param p1 ending point of the curve
     * @return resulting content stream
     * @throws IOException If there is an error while creating the curve in the document
     */
    public static PDPageContentStream createPDFCurve(PDPageContentStream cs, Point2D p0, Point2D cP0,
                                                    Point2D cP1, Point2D p1) throws IOException {
        cs.moveTo((float) p0.getX(), (float) p0.getY());
        cs.curveTo((float) cP0.getX(), (float) cP0.getY(), (float) cP1.getX(),
                (float) cP1.getY(), (float) p1.getX(), (float) p1.getY());
        return cs;
    }

    /**
     * Adds a gouraud shaded triangle to the pdf document.
     * More information about Gouraud shading: <a href="https://en.wikipedia.org/wiki/Gouraud_shading">...</a>
     *
     * @param doc PDF document holding the content stream
     * @param cs content stream that the shaded triangle is appended to
     * @param p0 coordinates of first vertex of the triangle
     * @param p1 coordinates of second vertex of the triangle
     * @param p2 coordinates of third vertex of the triangle
     * @param c0 color of the 'first coordinate' vertex of the triangle
     * @param c1 color of the 'second coordinate' vertex of the triangle
     * @param c2 color of the 'third coordinate' vertex of the triangle
     * @return resulting content stream
     * @throws IOException If there is an error while creating the shaded triangle
     */
    public static PDPageContentStream createPDFShadedTriangle(PDDocument doc, PDPageContentStream cs, Point2D p0,
                                                              Point2D p1, Point2D p2, Color c0, Color c1, Color c2) throws IOException {
        PDShadingType4 gouraudShading = new PDShadingType4(doc.getDocument().createCOSStream());
        gouraudShading.setShadingType(PDShading.SHADING_TYPE4);
        gouraudShading.setBitsPerFlag(8);
        gouraudShading.setBitsPerCoordinate(16);
        gouraudShading.setBitsPerComponent(8);

        COSArray decodeArray = new COSArray();
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.get(0xFFFF));
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.get(0xFFFF));
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.ONE);
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.ONE);
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.ONE);
        gouraudShading.setDecodeValues(decodeArray);
        gouraudShading.setColorSpace(PDDeviceRGB.INSTANCE);

        OutputStream os = ((COSStream) gouraudShading.getCOSObject()).createOutputStream();
        MemoryCacheImageOutputStream mcos = new MemoryCacheImageOutputStream(os);

        // Vertex 1, starts with flag1
        // (flags always 0 for vertices of start triangle)
        mcos.writeByte(0);
        // x1 y1 (left corner)
        mcos.writeShort((int) p0.getX());
        mcos.writeShort((int) p0.getY());
        // r1 g1 b1 (red)
        mcos.writeByte(c0.getRed());
        mcos.writeByte(c0.getGreen());
        mcos.writeByte(c0.getBlue());

        // Vertex 2, starts with flag2
        mcos.writeByte(0);
        // x2 y2 (top corner)
        mcos.writeShort((int) p1.getX());
        mcos.writeShort((int) p1.getY());
        // r2 g2 b2 (green)
        mcos.writeByte(c1.getRed());
        mcos.writeByte(c1.getGreen());
        mcos.writeByte(c1.getBlue());

        // Vertex 3, starts with flag3
        mcos.writeByte(0);
        // x3 y3 (right corner)
        mcos.writeShort((int) p2.getX());
        mcos.writeShort((int) p2.getY());
        // r3 g3 b3 (blue)
        mcos.writeByte(c2.getRed());
        mcos.writeByte(c2.getGreen());
        mcos.writeByte(c2.getBlue());
        mcos.close();

        os.close();
        cs.shadingFill(gouraudShading);
        return cs;
    }

    /**
     * Fills the output stream with the triangle information.
     *
     * @param outputStream holds the coordinates and colors of the triangle
     * @param p0 coordinates of first vertex of the triangle
     * @param p1 coordinates of second vertex of the triangle
     * @param p2 coordinates of third vertex of the triangle
     * @param c0 color of the 'first coordinate' vertex of the triangle
     * @param c1 color of the 'second coordinate' vertex of the triangle
     * @param c2 color of the 'third coordinate' vertex of the triangle
     * @throws IOException If there is an error while writing to the output stream
     */
    public static void writeShadedTriangle(MemoryCacheImageOutputStream outputStream, Point2D p0,
                                           Point2D p1, Point2D p2, Color c0, Color c1, Color c2) throws IOException {
        // Vertex 1, starts with flag1
        // (flags always 0 for vertices of start triangle)
        outputStream.writeByte(0);
        // x1 y1 (left corner)
        outputStream.writeShort((int) p0.getX());
        outputStream.writeShort((int) p0.getY());
        // r1 g1 b1 (red)
        outputStream.writeByte(c0.getRed());
        outputStream.writeByte(c0.getGreen());
        outputStream.writeByte(c0.getBlue());

        // Vertex 2, starts with flag2
        outputStream.writeByte(0);
        // x2 y2 (top corner)
        outputStream.writeShort((int) p1.getX());
        outputStream.writeShort((int) p1.getY());
        // r2 g2 b2 (green)
        outputStream.writeByte(c1.getRed());
        outputStream.writeByte(c1.getGreen());
        outputStream.writeByte(c1.getBlue());

        // Vertex 3, starts with flag3
        outputStream.writeByte(0);
        // x3 y3 (right corner)
        outputStream.writeShort((int) p2.getX());
        outputStream.writeShort((int) p2.getY());
        // r3 g3 b3 (blue)
        outputStream.writeByte(c2.getRed());
        outputStream.writeByte(c2.getGreen());
        outputStream.writeByte(c2.getBlue());
    }

    /**
     * Creates a text string in the pdf document.
     *
     * @param doc      PDF document holding the content stream
     * @param cs       content stream that the text is appended to
     * @param txt      text string that should be rendered in the document
     * @param position position where the text should be rendered
     * @return resulting content stream
     * @throws IOException If there is an error while creating the text in the document
     */
    public static PDPageContentStream createPDFText(PDDocument doc, PDPageContentStream cs, Text txt, Point2D position) throws IOException {
        cs.setNonStrokingColor(txt.getColor());
        cs.stroke();
        // set correct font
        PDType0Font font = (doc instanceof FontCachedPDDocument) ?
                ((FontCachedPDDocument) doc).getFont(txt.style) : createPDFont(doc, txt.style);
        cs.setFont(font, txt.fontsize);
        cs.beginText();

        AffineTransform affineTransform = AffineTransform.getTranslateInstance(position.getX(), position.getY());
        if (txt.getAngle() != 0)
            affineTransform.rotate(txt.getAngle());
        cs.setTextMatrix(new Matrix(affineTransform));

        float textHeight = font.getFontDescriptor().getDescent() / 1000 * txt.fontsize;
        for (String newLine : txt.getTextString().split("\n")) {
            cs.newLineAtOffset(0, -textHeight);
            cs.showText(newLine);
            textHeight += txt.getBounds().getHeight();
        }

        cs.endText();
        return cs;
    }

    /**
     * This helper method renders text in a PDF document.
     *
     * @param doc {@link PDDocument} where the latex text will be inserted
     * @param cs {@link PDPageContentStream} which contains the latex text
     * @param txt the text object that should be rendered
     * @param position the position where the text object should be rendered
     * @return the modified PDDocument for chaining
     * @throws IOException If there is an error while creating the text in the document
     */
    public static PDPageContentStream createPDFText(PDDocument doc, PDPageContentStream cs, NewText txt, Point2D position) throws IOException {
        cs.setNonStrokingColor(txt.getColor());
        cs.stroke();
        // set correct font
        PDType0Font font = (doc instanceof FontCachedPDDocument) ? ((FontCachedPDDocument) doc).getFont(txt.getStyle()) : createPDFont(doc, txt.getStyle());
        cs.setFont(font, txt.getFontSize());

        double verticalInset = txt.getVerticalInsets();
        double horizontalInset = txt.getHorizontalInsets();

        double fontDescent = font.getFontDescriptor().getDescent() / 1000 * txt.getFontSize();

        AffineTransform affineTransform = AffineTransform.getTranslateInstance(
                position.getX(),
                position.getY() + txt.getBounds().getHeight() - fontDescent);
        if (txt.getAngle() != 0)
            affineTransform.rotate(txt.getAngle(), txt.getTransformedExportBounds().getWidth(), txt.getTransformedExportBounds().getHeight() - txt.getBounds().getHeight());
        cs.transform(new Matrix(affineTransform));

        int textHeight = 0;
        for (NewText singleLineText : txt.generateTextObjectForEachLine()) {
            float width = font.getStringWidth(singleLineText.getTextString()) / 1000 * txt.getFontSize();

            if (txt.getBackground().getRGB() != 0) {
                cs.saveGraphicsState();
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(((float) txt.getBackground().getAlpha()) / 255);
                cs.setGraphicsStateParameters(graphicsState);
                if (singleLineText.getTextString().length() > 0) {
                    cs.transform(new Matrix(AffineTransform.getTranslateInstance(0, -singleLineText.getTextSize().getHeight() + fontDescent - textHeight)));
                    PDFUtils.createPDFPolygon(cs, new double[]{0, width + horizontalInset, width + horizontalInset, 0},
                            new double[]{-verticalInset, -verticalInset, singleLineText.getTextSize().getHeight(), singleLineText.getTextSize().getHeight()});
                }
                cs.setNonStrokingColor(new Color(txt.getBackground().getRGB(), true));
                cs.fill();
                cs.restoreGraphicsState();
            }

            cs.saveGraphicsState();
            cs.beginText();
            cs.setTextMatrix(new Matrix(AffineTransform.getTranslateInstance(txt.getInsets().left, (float) -txt.getTextSize().getHeight() - txt.getInsets().top - textHeight)));

            cs.newLine();
            cs.showText(singleLineText.getTextString());
            cs.endText();

            cs.transform(new Matrix(AffineTransform.getTranslateInstance(txt.getInsets().left, (float) -txt.getTextSize().getHeight() - txt.getInsets().top - textHeight)));
            cs.setStrokingColor(txt.getColor());
            if (txt.getTextDecoration() == UNDERLINE) {
                float descentHeight = font.getFontDescriptor().getDescent() / 1000 * txt.getFontSize();
                cs.moveTo(0, descentHeight);
                cs.lineTo(width, descentHeight);
                cs.stroke();
            } else if (txt.getTextDecoration() == STRIKETHROUGH) {
                float strikethroughHeight = (float) ((font.getFontDescriptor().getAscent() / 1000 * txt.getFontSize()) / 2.0 + (font.getFontDescriptor().getDescent() / 1000 * txt.getFontSize()));
                cs.moveTo(0, strikethroughHeight);
                cs.lineTo(width, strikethroughHeight);
                cs.stroke();
            }
            cs.restoreGraphicsState();
            cs.transform(new Matrix(AffineTransform.getTranslateInstance(0, (float) -txt.getTextSize().getHeight())));
            textHeight += verticalInset;
        }
        return cs;
    }

    /**
     * Creates a {@link PDType0Font} for the Ubuntu Mono font with the given style.
     *
     * @param doc document where the font will be placed in
     * @param style of the text that should be included
     * @return the created PDType0Font
     * @throws IOException if there's an error when loading the font
     */
    public static PDType0Font createPDFont(PDDocument doc, int style) throws IOException {
        switch (style) {
            case Font.BOLD:
                return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_BOLD_RESOURCE));
            case Font.ITALIC:
                return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_ITALIC_RESOURCE));
            case (Font.BOLD | Font.ITALIC):
                return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_BOLDITALIC_RESOURCE));
            case Font.PLAIN:
                return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_PLAIN_RESOURCE));
            default:
                throw new IllegalArgumentException(
                        "Style argument is malformed. Only PLAIN, BOLD, ITALIC or BOLD|ITALIC are accepted.");
        }
    }

    /**
     * Swaps between PDF and AWT coordinates, AWT coordinate system
     * has its origin in the top left corner of a component and downwards pointing
     * y-axis, whereas PDF has its origin in the bottom left corner of the viewport
     * (at least in JPlotter) and upwards pointing y-axis.
     *
     * @param point to swap the y-axis of
     * @param page height of the page will be used to swap the y-axis
     * @return point in coordinates of the other reference coordinate system.
     */
    public static Point2D transformPDFToCoordSys(Point2D point, PDPage page) {
        return Utils.swapYAxis(point, (int) page.getMediaBox().getHeight());
    }

    /**
     * Creates a polygon in the pdf document.
     * The x (and y) coordinates will be used counterclockwise.
     *
     * @param cs content stream that the polygon is appended to
     * @param x x coordinates of the polygon
     * @param y y coordinates of the polygon
     * @return resulting content stream
     * @throws IOException If the content stream could not be written
     */
    public static PDPageContentStream createPDFPolygon(PDPageContentStream cs, double[] x, double[] y) throws IOException {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Length of x and y coordinate arrays have to be equal!");
        }
        for (int i = 0; i < x.length; i++) {
            if (i == 0) {
                cs.moveTo((float) x[i], (float) y[i]);
            }
            else {
                cs.lineTo((float) x[i], (float) y[i]);
            }
        }
        cs.closePath();
        return cs;
    }

    /**
     * Draws all components of a {@link Container} to
     * an PDF document.
     * For this an {@link PdfBoxGraphics2D} object is used that will
     * create the PDF elements for the specified container and all of its children.
     *
     * @param c container to be converted to PDF
     * @return PDF document representing the specified container.
     * @throws IOException if something goes wrong with writing into the content stream of the PDDocument
     */
    public static PDDocument containerToPDF(Container c) throws IOException {
        PDDocument doc = new FontCachedPDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);
        page.setMediaBox(new PDRectangle(c.getWidth(), c.getHeight()));

        { // render gui components through PdfBoxGraphics2D
            PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
            PdfBoxGraphics2D g2d = new PdfBoxGraphics2D(doc, c.getWidth(), c.getHeight());
            c.paintAll(g2d);
            g2d.dispose();
            PDFormXObject xform = g2d.getXFormObject();
            cs.drawForm(xform);
            cs.close();
        }
        containerToPDF(c, doc, page, 0, 0);
        return doc;
    }

    private static void containerToPDF(Container c, PDDocument doc, PDPage page, int xOffset, int yOffset) throws IOException {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JPlotterCanvas) {
                JPlotterCanvas canvas = (JPlotterCanvas) comp;
                if (canvas.isPDFAsImageRenderingEnabled())
                    return; // was already rendered through PdfBoxGraphics2D
                canvas.paintPDF(doc, page, new Rectangle2D.Double(canvas.asComponent().getX() + xOffset, canvas.asComponent().getY() + yOffset,
                        canvas.asComponent().getWidth(), canvas.asComponent().getHeight()));
            } else {
                if(comp instanceof Container){
                    containerToPDF((Container)comp, doc, page, comp.getX()+xOffset, comp.getY()+yOffset);
                }
            }
        }
    }

    /**
     * This helper method renders latex text in a PDF document.
     *
     * @param doc {@link PDDocument} where the latex text will be inserted
     * @param cs {@link PDPageContentStream} which contains the latex text
     * @param txt the text object that should be rendered
     * @param position the position where the text object should be rendered
     * @return the modified PDDocument for chaining
     * @throws IOException If there is an error while creating the text in the document
     */
    public static PDDocument latexToPDF(PDDocument doc, PDPageContentStream cs, NewText txt, Point2D position) throws IOException {
        DefaultTeXFont.registerAlphabet(new CyrillicRegistration());
        DefaultTeXFont.registerAlphabet(new GreekRegistration());

        AffineTransform affineTransform = AffineTransform.getTranslateInstance(position.getX(), position.getY() + txt.getBounds().getHeight());
        if (txt.getAngle() != 0)
            affineTransform.rotate(txt.getAngle(), txt.getTransformedExportBounds().getWidth(), -txt.getTransformedExportBounds().getHeight() + txt.getBounds().getHeight());
        cs.transform(new Matrix(affineTransform));

        for (NewText singleLineText : txt.generateTextObjectForEachLine()) {
            TeXFormula formula = new TeXFormula(singleLineText.getTextString());
            TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, txt.getFontSize());
            icon.setInsets(new Insets(txt.getInsets().top, txt.getInsets().left, txt.getInsets().bottom, txt.getInsets().right));

            PdfBoxGraphics2D g2d = new PdfBoxGraphics2D(doc, icon.getIconWidth(), icon.getIconHeight());
            g2d.setColor(txt.getBackground());
            if (singleLineText.getTextString().length() > 0)
                g2d.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());

            cs.transform(new Matrix(AffineTransform.getTranslateInstance(0, -icon.getIconHeight())));

            JLabel jl = new JLabel();
            jl.setForeground(txt.getColor());
            icon.paintIcon(jl, g2d, 0, 0);

            g2d.dispose();
            PDFormXObject xform = g2d.getXFormObject();
            cs.drawForm(xform);
        }
        return doc;
    }
}