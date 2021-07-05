package hageldave.jplotter.pdf;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.CurvesRenderer;
import hageldave.jplotter.renderers.LinesRenderer;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class PDFDemo {

    private static double[] randomData(int n){
        double[] d = new double[n];
        for(int i=0; i<n; i++){
            d[i]=Math.random()*100-1;
        }
        return d;
    }


    public void create(String file) throws IOException, InterruptedException, InvocationTargetException {

        PDDocument document = null;
        try {

            double[] seriesA_y = randomData(20);
            double[] seriesA_x = IntStream.range(0, 20).mapToDouble(i->i/1.0).toArray();
            double[] seriesB_y = randomData(30);
            double[] seriesB_x = randomData(30);
            Arrays.sort(seriesB_x);
            // create Lines objects, one solid the other dashed
            Lines lineA = new Lines();
            Lines lineB = new Lines();
            lineB.setStrokePattern(0xf0f0);
            // add line segments to A
            for(int i=0; i<seriesA_y.length-1; i++){
                double x1=seriesA_x[i], x2=seriesA_x[i+1];
                double y1=seriesA_y[i], y2=seriesA_y[i+1];
                Lines.SegmentDetails segment = lineA.addSegment(x1, y1, x2, y2);
                segment.setColor(Color.RED);
            }
            // add line segments to B (the short way)
            ArrayList<Lines.SegmentDetails> segmentsB = lineB.addLineStrip(seriesB_x, seriesB_y);
            segmentsB.forEach(seg->seg.setColor(Color.BLUE));
            // use a coordinate system for display
            // set the content renderer of the coordinate system
            // we want to render Lines objects

            Curves curve = new Curves();
            curve.addCurve(new Point2D.Double(20, 20), new Point2D.Double(40, 30),
                    new Point2D.Double(50, 50), new Point2D.Double(60, 20)).setColor(Color.RED);
            curve.addCurve(new Point2D.Double(10, 20), new Point2D.Double(220, 30),
                    new Point2D.Double(50, 50), new Point2D.Double(60, 20)).setColor(Color.GREEN);

            CurvesRenderer curveCont = new CurvesRenderer();
            curveCont.addItemToRender(curve);

            LinesRenderer lineContent = new LinesRenderer();
            lineA.addSegment(new Point2D.Double(-20, 20), new Point2D.Double(40, 20)).setColor(Color.ORANGE);
            lineContent.addItemToRender(lineA).addItemToRender(lineB);



            CoordSysRenderer renderer = new CoordSysRenderer();
            renderer.setCoordinateView(-10,-10,100,100);
            renderer.setContent(curveCont);

            JFrame frame = new JFrame();
            boolean useOpenGL = false;
            JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
            canvas.setRenderer(renderer);
            canvas.asComponent().setPreferredSize(new Dimension(400, 400));
            canvas.asComponent().setBackground(Color.WHITE);
            frame.getContentPane().add(canvas.asComponent());
            frame.setTitle("scatterplot");
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

        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
        PDFDemo creator = new PDFDemo();
        creator.create("triangle_image.pdf");
    }
}
