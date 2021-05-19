package hageldave.jplotter.howto;


import hageldave.jplotter.charts.BarChart;
import hageldave.jplotter.charts.BarChart.Alignment;

import javax.swing.*;
import java.awt.*;
import java.util.stream.IntStream;

public class ReadyBarChart {


    public static void main(String[] args) {
        String[] cases = {"A","B","C","D1","D2*"};
        double[] scores = IntStream.range(0, cases.length)
                .mapToDouble(i->Math.random()).toArray();
        scores[scores.length-2] = -6;
        scores[scores.length-1] = -5;
        int[] ids = IntStream.range(0, cases.length).toArray();

        Color[] color = new Color[]{Color.RED, Color.RED, Color.RED, Color.RED, Color.RED};

        BarChart barChart = new BarChart(false, Alignment.HORIZONTAL);

        barChart.addData(ids, scores, color, cases);
        barChart.addBar(9, 8, Color.DARK_GRAY, "test");

        BarChart.BarStruct barStruct = barChart.trianglesInRenderer.get(9);
        barStruct.tri.getTriangleDetails().forEach(e -> e.setColor(Color.BLUE));

        barChart.orderBars();

        JFrame frame = new JFrame();
        frame.getContentPane().add(barChart.getCanvas().asComponent());
        frame.setTitle("barchart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        barChart.getCanvas().addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
        });
    }
}
