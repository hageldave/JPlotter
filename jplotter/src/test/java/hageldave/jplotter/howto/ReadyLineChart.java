package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;

import javax.swing.*;
import java.awt.*;

public class ReadyLineChart {

    private static double[][] randomData(int n) {
        double[][] d = new double[n][2];
        for (int i = 0; i < n; i++) {
            d[i][0] = Math.random() * 2 - 1;
            d[i][1] = Math.random() * 2 - 1;
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
        chart.addLineSegment(seriesA, Color.RED);
        chart.addLineSegment(seriesB, Color.BLUE).setStrokePattern(0xf0f0);

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
