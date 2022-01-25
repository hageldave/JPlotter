package hageldave.jplotter.pdf;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.font.FontProvider;
import hageldave.jplotter.util.Utils;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType4;
import org.apache.pdfbox.util.Matrix;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

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
     * More information about Gouraud shading: https://en.wikipedia.org/wiki/Gouraud_shading
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
     * @param doc PDF document holding the content stream
     * @param cs content stream that the text is appended to
     * @param txt text string that should be rendered in the document
     * @param position position where the text should be rendered
     * @param color color of the text
     * @param fontSize size of font
     * @param style style of font
     * @param angle rotation of the text
     * @return resulting content stream
     * @throws IOException If there is an error while creating the text in the document
     */
    public static PDPageContentStream createPDFText(PDDocument doc, PDPageContentStream cs, String txt,
                                                    Point2D position, Color color, int fontSize, int style, float angle) throws IOException {
        cs.setNonStrokingColor(color);
        cs.stroke();
        // set correct font
        PDType0Font font = (doc instanceof FontCachedPDDocument) ? 
        		((FontCachedPDDocument)doc).getFont(style) : createPDFont(doc, style);
        cs.setFont(font, fontSize);
        cs.beginText();
        AffineTransform at = new AffineTransform(1, 0.0, 0.0,
               1, position.getX(), position.getY());
        at.rotate(angle);
        cs.setTextMatrix(new Matrix(at));
        cs.showText(txt);
        cs.endText();
        return cs;
    }
    
    public static PDType0Font createPDFont(PDDocument doc, int style) throws IOException {
    	switch (style) {
    	case Font.BOLD:
    		return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_BOLD_RESOURCE));
    	case Font.ITALIC:
    		return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_ITALIC_RESOURCE));
    	case (Font.BOLD|Font.ITALIC):
    		return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_BOLDITALIC_RESOURCE));
    	case Font.PLAIN:
    		return PDType0Font.load(doc, PDFUtils.class.getResourceAsStream(FontProvider.UBUNTU_MONO_PLAIN_RESOURCE));
    	default:
    		throw new IllegalArgumentException(
    				"Style argument is malformed. Only PLAIN, BOLD, ITALIC or BOLD|ITALIC are accepted.");
    	}
    }

    /**
     * Creates a text string in the pdf document with angle 0.
     *
     * @param doc PDF document holding the content stream
     * @param cs content stream that the curve is appended to
     * @param txt text string that should be rendered in the document
     * @param position position where the text should be rendered
     * @param color color of the text
     * @param fontSize size of font
     * @param style style of font
     * @return resulting content stream
     * @throws IOException If there is an error while creating the text in the document
     */
    public static PDPageContentStream createPDFText(PDDocument doc, PDPageContentStream cs, String txt,
                                                    Point2D position, Color color, int fontSize, int style) throws IOException {
        return createPDFText(doc, cs, txt, position, color, fontSize, style, 0);
    }

    /**
     * Swaps between PDF and AWT coordinates, AWT coordinate system
     * has its origin in the top left corner of a component and downwards pointing
     * y axis, whereas PDF has its origin in the bottom left corner of the viewport
     * (at least in JPlotter) and upwards pointing y axis.
     *
     * @param point to swap the y axis of
     * @param page height of the page will be used to swap the y axis
     * @return point in coordinates of the other reference coordinate system.
     */
    public static Point2D transformPDFToCoordSys(Point2D point, PDPage page) {
        return Utils.swapYAxis(point, (int) page.getMediaBox().getHeight());
    }

    /**
     * Creates a polygon in the pdf document.
     * The x (and y) coordinates will be used counter clockwise.
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
        PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
        page.setMediaBox(new PDRectangle(c.getWidth(), c.getHeight()));

        containerToPDF(c, doc, page, cs);
        { // render gui components through PdfBoxGraphics2D
            PdfBoxGraphics2D g2d = new PdfBoxGraphics2D(doc, c.getWidth(), c.getHeight());
            c.paintAll(g2d);
            g2d.dispose();
            PDFormXObject xform = g2d.getXFormObject();
            cs.drawForm(xform);
        }

        cs.close();
        return doc;
    }


    private static void containerToPDF(Container c, PDDocument doc, PDPage page, PDPageContentStream cs) throws IOException {
        for(Component comp:c.getComponents()) {
            if (comp instanceof JPlotterCanvas) {
                JPlotterCanvas canvas = (JPlotterCanvas)comp;
                if(canvas.isPDFAsImageRenderingEnabled())
                    return; // was already rendered through PdfBoxGraphics2D
                canvas.paintPDF(doc, page, cs, new Rectangle2D.Double(canvas.asComponent().getX(), canvas.asComponent().getY(),
                        canvas.asComponent().getWidth(), canvas.asComponent().getHeight()));
            } else {
                if(comp instanceof Container){
                    containerToPDF((Container)comp, doc, page, cs);
                }
            }
        }
    }
}
