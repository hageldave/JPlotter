package hageldave.jplotter.pdf;

import hageldave.jplotter.canvas.JPlotterCanvas;
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
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType4;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

public class PDFUtils {


    public static PDPageContentStream createPDFRect(PDPageContentStream cs,
                                                    double x, double y, double w, double h)
                                                    throws IOException {
        cs.addRect((float) x, (float) y, (float) w, (float) h);
        return cs;
    }

    public static PDPageContentStream createPDFPoint(PDPageContentStream cs,
                                                     Point2D point, double rad) throws IOException {
        float cx = (float) point.getX();
        float cy = (float) point.getY();
        float r = (float) rad;
        final float k = 0.552284749831f;
        cs.moveTo(cx - r, cy);
        cs.curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r);
        cs.curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy);
        cs.curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r);
        cs.curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy);
        return cs;
    }

    public static PDPageContentStream createPDFSegment(PDPageContentStream cs, Point2D p0,
                                                       Point2D p1) throws IOException {
        cs.moveTo((float) p0.getX(), (float) p0.getY());
        cs.lineTo((float) p1.getX(), (float) p1.getY());
        return cs;
    }

    public static PDPageContentStream createPDFCurve(PDPageContentStream cs, Point2D p0, Point2D cP0,
                                                    Point2D cP1, Point2D p1) throws IOException {
        cs.moveTo((float) p0.getX(), (float) p0.getY());
        cs.curveTo((float) cP0.getX(), (float) cP0.getY(), (float) cP1.getX(),
                (float) cP1.getY(), (float) p1.getX(), (float) p1.getY());
        return cs;
    }

    public static PDPageContentStream createPDFShadedTriangle(PDPageContentStream cs, Point2D p0,
                                                              Point2D p1, Point2D p2, Color c0, Color c1, Color c2) throws IOException {
        // See PDF 32000 specification,
        // 8.7.4.5.5 Type 4 Shadings (Free-Form Gouraud-Shaded Triangle Meshes)
        PDShadingType4 gouraudShading = new PDShadingType4(new COSStream());
        gouraudShading.setShadingType(PDShading.SHADING_TYPE4);
        // we use multiple of 8, so that no padding is needed
        gouraudShading.setBitsPerFlag(8);
        gouraudShading.setBitsPerCoordinate(16);
        gouraudShading.setBitsPerComponent(8);

        COSArray decodeArray = new COSArray();
        // coordinates x y map 16 bits 0..FFFF to 0..FFFF to make your life easy
        // so no calculation is needed, but you can only use integer coordinates
        // for real numbers, you'll need smaller bounds, e.g. 0xFFFF / 0xA = 0x1999
        // would allow 1 point decimal result coordinate.
        // See in PDF specification: 8.9.5.2 Decode Arrays
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.get(0xFFFF));
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.get(0xFFFF));
        // colors r g b map 8 bits from 0..FF to 0..1
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.ONE);
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.ONE);
        decodeArray.add(COSInteger.ZERO);
        decodeArray.add(COSInteger.ONE);
        gouraudShading.setDecodeValues(decodeArray);
        gouraudShading.setColorSpace(PDDeviceRGB.INSTANCE);

        // Function is not required for type 4 shadings and not really useful,
        // because if a function would be used, each edge "color" of a triangle would be one value,
        // which would then transformed into n color components by the function so it is
        // difficult to get 3 "extremes".
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


    public static PDPageContentStream createPDFText(PDDocument document, PDPageContentStream cs, String txt,
                                                    Point2D point, Color color, int fontSize, int style, float angle) throws IOException {
        cs.setNonStrokingColor(color);
        cs.stroke();
        // set correct font
        if (style==1) {
            PDType0Font font = PDType0Font.load(document, PDFUtils.class.getResourceAsStream("/font/UbuntuMono-B.ttf"));
            cs.setFont(font, fontSize);
        } else if (style==2) {
            PDType0Font font = PDType0Font.load(document, PDFUtils.class.getResourceAsStream("/font/UbuntuMono-RI.ttf"));
            cs.setFont(font, fontSize);
        } else if (style==(1|2)) {
            PDType0Font font = PDType0Font.load(document, PDFUtils.class.getResourceAsStream("/font/UbuntuMono-BI.ttf"));
            cs.setFont(font, fontSize);
        } else {
            PDType0Font font = PDType0Font.load(document, PDFUtils.class.getResourceAsStream("/font/UbuntuMono-R.ttf"));
            cs.setFont(font, fontSize);
        }

        cs.beginText();
        AffineTransform at = new AffineTransform(1, 0.0, 0.0,
               1, point.getX(), point.getY());
        at.rotate(angle);
        cs.setTextMatrix(at);
        cs.showText(txt);
        cs.endText();
        return cs;
    }

    public static PDPageContentStream createPDFText(PDDocument document, PDPageContentStream cs, String txt,
                                                    Point2D point, Color color, Dimension textSize, int fontSize, int style) throws IOException {
        return createPDFText(document, cs, txt, point, color, fontSize, style, 0);
    }

    public static Point2D transformPDFToCoordSys(Point2D point, PDPage page) {
        return Utils.swapYAxis(point, (int) page.getMediaBox().getHeight());
    }

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

    public static PDDocument containerToPDF(Container c) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
        page.setMediaBox(new PDRectangle(c.getWidth(), c.getHeight()));
        containerToPDF(c, doc, page, cs);
        cs.close();
        return doc;
    }

    private static void containerToPDF(Container c, PDDocument doc, PDPage page, PDPageContentStream cs) throws IOException {
        for(Component comp:c.getComponents()) {
            if (comp instanceof JPlotterCanvas) {
                JPlotterCanvas canvas = (JPlotterCanvas)comp;
                System.out.println(comp.getClass() + " " + comp.getY());
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
