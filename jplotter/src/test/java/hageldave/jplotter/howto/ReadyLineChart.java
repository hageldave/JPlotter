package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Lines;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

// TODO echter Datensatz
public class ReadyLineChart {

    private static double[][] randomData(int n) {
        double[][] d = new double[n][2];
        for (int i = 0; i < n; i++) {
            d[i][0] = Math.random() * 200 - 1;
            d[i][1] = Math.random() * 200 - 1;
        }
        return d;
    }

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        // obtain data series
        double[][] seriesA = randomData(30);
        double[][] seriesB = randomData(30);

        // display within a JFrame
        JFrame frame = new JFrame();

        hageldave.jplotter.charts.LineChart chart = new hageldave.jplotter.charts.LineChart(false);
        chart.addLineSegment(1, seriesA, Color.RED);
        chart.addLineSegment(2, seriesB, Color.BLUE).setStrokePattern(0xf0f0);

        chart.highlightDatapoints(DefaultGlyph.CIRCLE, Color.green);
        chart.alignCoordsys();

        chart.new LineClickedInterface() {
            { extModifierMask = KeyEvent.VK_K; }
            @Override
            public void segmentClicked(Point mouseLocation, Lines.SegmentDetails line, double[][] data, int startIndex, int endIndex) {
                System.out.println(line);
                System.out.println(startIndex);
                System.out.println(endIndex);
            }
        }.register();

        chart.new PointsSelectedInterface() {
            @Override
            public void pointsSelected(Rectangle2D bounds, ArrayList<double[][]> data, ArrayList<Integer> dataIndices) {

            }
        };

        frame.getContentPane().add(chart.getCanvas().asComponent());
        frame.setTitle("linechart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //canvas.addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeLater(() -> {
            frame.pack();
            frame.setVisible(true);
        });

        long t = System.currentTimeMillis() + 2000;
        while (t > System.currentTimeMillis()) ;
        if ("false".equals("true"))
            SwingUtilities.invokeLater(() -> {
                Img img = new Img(frame.getSize());
                img.paint(g2d -> frame.paintAll(g2d));
                ImageSaver.saveImage(img.getRemoteBufferedImage(), "linechart.png");
            });
    }
}
