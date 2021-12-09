package hageldave.jplotter;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.*;
import hageldave.jplotter.renderers.*;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class PDFDemo {

    private static double[] randomData(int n){
        double[] d = new double[n];
        for(int i=0; i<n; i++){
            d[i]=Math.random()*200-1;
        }
        return d;
    }

    public void create(String file) throws IOException, InterruptedException, InvocationTargetException {

        PDDocument document = null;
        try {

            double[] seriesA_y = randomData(30);
            double[] seriesA_x = IntStream.range(0, 30).mapToDouble(i->i/1.0).toArray();
            double[] seriesB_y = randomData(6);
            double[] seriesB_x = randomData(6);
            Arrays.sort(seriesB_x);
            // create Lines objects, one solid the other dashed
            Lines lineA = new Lines();
            Lines lineB = new Lines();
            lineB.setStrokePattern(0xf0f0);
            lineB.setGlobalThicknessMultiplier(33);
            lineB.setGlobalAlphaMultiplier(0.2);
            // add line segments to A
            for(int i=0; i<seriesA_y.length-1; i++){
                double x1=seriesA_x[i], x2=seriesA_x[i+1];
                double y1=seriesA_y[i], y2=seriesA_y[i+1];
                Lines.SegmentDetails segment = lineA.addSegment(x1, y1, x2, y2);
                segment.setColor(Color.RED);
            }
            // add line segments to B (the short way)
            ArrayList<Lines.SegmentDetails> segmentsB = lineB.addLineStrip(seriesB_x, seriesB_y);
            segmentsB.forEach(seg->seg.setColor1(Color.BLUE).setColor0(Color.RED));
            // use a coordinate system for display
            // set the content renderer of the coordinate system
            // we want to render Lines objects

            CompleteRenderer compr = new CompleteRenderer();

            Curves curve = new Curves();
            curve.addCurve(new Point2D.Double(20, 20), new Point2D.Double(4000, 30),
                    new Point2D.Double(50, 50), new Point2D.Double(60, 20)).setColor(Color.RED);
            curve.addCurve(new Point2D.Double(10, 20), new Point2D.Double(220, 30),
                    new Point2D.Double(50, 50), new Point2D.Double(60, 20)).setColor(new Color(0,0,255,30));
            curve.setStrokePattern(0xf0f0);

            curve.setGlobalSaturationMultiplier(0.2);
            curve.setGlobalAlphaMultiplier(0.4);
            curve.setGlobalThicknessMultiplier(20);

            CurvesRenderer curveCont = new CurvesRenderer();
            curveCont.addItemToRender(curve);

            LinesRenderer lineContent = new LinesRenderer();
            lineA.setGlobalSaturationMultiplier(0.8);
            lineA.setGlobalAlphaMultiplier(0.8);
            lineA.setGlobalThicknessMultiplier(7);
            lineA.addSegment(new Point2D.Double(20, 20), new Point2D.Double(190, 90))
                    .setThickness(9, 9).setColor0(new Color(0,255,0,30)).setColor1(Color.RED);
            lineContent.addItemToRender(lineA).addItemToRender(lineB);

            Triangles tri = new Triangles();
            tri.setGlobalAlphaMultiplier(0.9);
            tri.setGlobalSaturationMultiplier(0.8);
            tri.addTriangle(new Point2D.Double(0,0), new Point2D.Double(50, 50),
                    new Point2D.Double(100, 60)).setColor0(new Color(255,0,0,20)).setColor1(Color.BLUE)
                    .setColor2(new Color(0,255,0,40));

            tri.addQuad(new Rectangle2D.Double(30, 30, 100, 100));
            TrianglesRenderer renderer2 = new TrianglesRenderer();
            renderer2.addItemToRender(tri);

            SplitScreenRenderer splitScreenRenderer = new SplitScreenRenderer();
            splitScreenRenderer.setR1(renderer2).setR2(lineContent);
            splitScreenRenderer.setVerticalSplit(true);
            splitScreenRenderer.setDividerLocation(0.15);

            ChainedRenderer chainedRenderer = new ChainedRenderer(lineContent, curveCont);

            TextRenderer textRenderer = new TextRenderer();
            Text txt = new Text("hallo", 19, 1, new Color(255, 0,0, 50));
            txt.setOrigin(30,30);
            txt.setAngle(2.3);
            txt.setBackground(new Color(0,255,0, 255));
            textRenderer.addItemToRender(txt);


            Points p = new Points(DefaultGlyph.CIRCLE_F);
            p.setGlobalAlphaMultiplier(0.9);
            p.setGlobalScaling(1.4);
            p.setGlobalSaturationMultiplier(0.2);
            p.addPoint(new Point2D.Double(20, 20)).setColor(Color.RED).setScaling(1.9).setRotation(1.8).setScaling(5);
            PointsRenderer pr = new PointsRenderer();
            pr.setGlyphScaling(3);
            pr.addItemToRender(p);

            compr.addItemToRender(curve).addItemToRender(lineA).addItemToRender(tri).addItemToRender(lineB).addItemToRender(p).addItemToRender(txt);


            CoordSysRenderer renderer = new CoordSysRenderer();
            Legend lg = new Legend();
            lg.addGlyphLabel(DefaultGlyph.TRIANGLE, Color.RED.getRGB(), "Item 1");
            lg.addLineLabel(3, Color.RED.getRGB(), "Item 2");
            lg.addLineLabel(5, Color.RED.getRGB(), "Item 3");
            renderer.setLegendRight(lg);
            renderer.setCoordinateView(-10,-10,100,100);
            renderer.setContent(compr);

            JFrame frame = new JFrame();
            boolean useOpenGL = false;
            JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
            canvas.setRenderer(renderer);
            canvas.asComponent().setPreferredSize(new Dimension(400, 400));
            //canvas.asComponent().setBackground(Color.BLACK);
            frame.getContentPane().add(canvas.asComponent());
            frame.setTitle("PDF Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            canvas.addCleanupOnWindowClosingListener(frame);
            // make visible on AWT event dispatch thread
            SwingUtilities.invokeAndWait(()->{
                frame.pack();
                frame.setVisible(true);
            });

            PDDocument doc = canvas.paintPDF();
            doc.save(file);
            doc.close();

            Document doc2 = canvas.paintSVG();
            SVGUtils.documentToXMLFile(doc2, new File("svgtest.svg"));
            System.out.println("svg exported:");
            //System.out.println(SVGUtils.documentToXMLString(doc2));

        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
        PDFDemo creator = new PDFDemo();
        creator.create("pdf_test.pdf");
    }
}
