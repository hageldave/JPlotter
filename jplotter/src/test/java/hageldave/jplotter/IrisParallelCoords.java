package hageldave.jplotter;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.renderers.ParallelCoordsRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class IrisParallelCoords {
    public static void main(String[] args) throws MalformedURLException {
        ParallelCoordsRenderer parallelCoords = new ParallelCoordsRenderer();

        ParallelCoordsRenderer.Feature sl = new ParallelCoordsRenderer.Feature(4.3,7.9, "sepal length");
        ParallelCoordsRenderer.Feature sw = new ParallelCoordsRenderer.Feature(2.0,4.4, "sepal width");
        ParallelCoordsRenderer.Feature pl = new ParallelCoordsRenderer.Feature(1.0,6.9, "petal length");
        ParallelCoordsRenderer.Feature pw = new ParallelCoordsRenderer.Feature(0.1,2.5, "petal width");
        parallelCoords.addFeature(sl, sw, pl, pw);

        LinesRenderer lines = new LinesRenderer();
        parallelCoords.setContent(lines);

        ArrayList<double[]> irisData = new ArrayList<>(150);

        try (Scanner sc = new Scanner(new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data").openStream())) {
            while(sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                if (nextLine.isEmpty()) {
                    continue;
                }
                String[] fields = nextLine.split(",");
                double[] values = new double[5];
                values[0] = Double.parseDouble(fields[0]);
                values[1] = Double.parseDouble(fields[1]);
                values[2] = Double.parseDouble(fields[2]);
                values[3] = Double.parseDouble(fields[3]);
                if(fields[4].contains("setosa")){
                    values[4] = 0; // setosa class
                } else if(fields[4].contains("versicolor")) {
                    values[4] = 1; // versicolor class
                } else {
                    values[4] = 2; // virginica class
                }
                irisData.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: values have to be normalized automatically
        for (double[] entry : irisData) {
            Lines lineSegment = new Lines();
            lineSegment.addLineStrip(
                    new Point2D.Double(0.0/3, normalizeValue(entry[0], sl)),
                    new Point2D.Double(1.0/3, normalizeValue(entry[1], sw)),
                    new Point2D.Double(2.0/3, normalizeValue(entry[2], pl)),
                    new Point2D.Double(3.0/3, normalizeValue(entry[3], pw))
            );

            switch((int) entry[4]) {
                case 0:
                    for(Lines.SegmentDetails det : lineSegment.getSegments())
                        det.setColor(DefaultColorMap.Q_8_ACCENT.getColor(0));
                    break;
                case 1:
                    for(Lines.SegmentDetails det : lineSegment.getSegments())
                        det.setColor(DefaultColorMap.Q_8_ACCENT.getColor(1));
                    break;
                case 2:
                    for(Lines.SegmentDetails det : lineSegment.getSegments())
                        det.setColor(DefaultColorMap.Q_8_ACCENT.getColor(2));
                    break;
            }
            lines.addItemToRender(lineSegment);
        }

        // display within a JFrame
        JFrame frame = new JFrame();
        boolean useOpenGL = false;
        JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        canvas.setRenderer(parallelCoords);
        canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        canvas.asComponent().setBackground(Color.WHITE);
        frame.getContentPane().add(canvas.asComponent());
        frame.setTitle("scatterplot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
        });

        long t=System.currentTimeMillis()+2000;
        while(t>System.currentTimeMillis());
        if("false".equals("true"))
            SwingUtilities.invokeLater(()->{
                Img img = new Img(frame.getSize());
                img.paint(g2d->frame.paintAll(g2d));
                ImageSaver.saveImage(img.getRemoteBufferedImage(), "scatterplot.png");
            });
    }


    private static double normalizeValue(double value, ParallelCoordsRenderer.Feature feature) {
        return (value - feature.min)/(feature.max - feature.min);
    }
}
