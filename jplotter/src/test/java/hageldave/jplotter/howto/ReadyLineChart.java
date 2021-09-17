package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.charts.LineChart;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

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
        double[][] seriesA = randomData(10);
        double[][] seriesB = randomData(10);
        double[][] seriesC = randomData(2);
        double[][] seriesD = randomData(10);
        double[][] seriesE = randomData(10);
        double[][] seriesF = randomData(10);
        double[][] seriesG = randomData(10);
        double[][] seriesZ = randomData(10);

        // display within a JFrame
        JFrame frame = new JFrame();

        hageldave.jplotter.charts.LineChart chart = new hageldave.jplotter.charts.LineChart(false);

        chart.getDataModel().addData(seriesA, 0, 1, 1, "test");
        chart.getDataModel().addData(seriesB, 0, 1, 1, "test2");
        chart.getDataModel().addData(seriesD, 0, 1, 1, "test3");
        chart.getDataModel().addData(seriesE, 0, 1, 1, "test4");
        chart.getDataModel().addData(seriesF, 0, 1, 1, "test5");
        chart.getDataModel().addData(seriesG, 0, 1, 1, "test6");
        chart.getDataModel().addData(seriesZ, 0, 1, 1, "test7");
        chart.getDataModel().addData(seriesA, 0, 1, 1, "test88");
        chart.getDataModel().addData(seriesB, 0, 1, 1, "test99");
        chart.getDataModel().addData(seriesD, 0, 1, 1, "test999");
        chart.getDataModel().addData(seriesE, 0, 1, 1, "test1111");

        /*double[][] dm = *///chart.getDataModel().getDataChunk(0)[0][0] = 0;
        chart.getDataModel().setDataChunk(0, seriesC);

        chart.getCoordsys().setCoordinateView(-10,-10,200,200);
        chart.placeLegendOnBottom();

        chart.addLineChartMouseEventListener(new LineChart.LineChartMouseEventListener() {
            @Override
            public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
                LineChart.LineChartMouseEventListener.super.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
            }

            @Override
            public void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
                LineChart.LineChartMouseEventListener.super.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, pointIdx);

            }

            @Override
            public void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
                LineChart.LineChartMouseEventListener.super.onOutsideMouseEventeNone(mouseEventType, e);
            }

            @Override
            public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
                LineChart.LineChartMouseEventListener.super.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
            }
        });

        /*chart.addLineSegment(1, seriesA, Color.RED);
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
        };*/

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
