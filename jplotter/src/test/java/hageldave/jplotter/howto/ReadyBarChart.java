package hageldave.jplotter.howto;


import hageldave.jplotter.charts.BarChart;
import hageldave.jplotter.util.AlignmentConstants;

import javax.swing.*;
import java.awt.*;
import java.util.stream.IntStream;

@Deprecated(/*still in development, don't use this yet*/)
public class ReadyBarChart {


    @SuppressWarnings("unused"/*still in development so we don't care about warnings here*/)
	public static void main(String[] args) {
        String[] cases = {"A","B","C","D1","D2*"};
        double[] scores = IntStream.range(0, cases.length)
                .mapToDouble(i->Math.random()).toArray();
        //scores[scores.length-2] = -5.269;
        scores[scores.length-1] = -5.22;
        int[] ids = IntStream.range(0, cases.length).toArray();

        Color[] color = new Color[]{Color.RED, Color.RED, Color.RED, Color.RED, Color.RED};

        BarChart barChart = new BarChart(false, AlignmentConstants.VERTICAL);

        BarChart.BarGroup group1 = barChart.createGroup(1);
        BarChart.BarGroup group2 = barChart.createGroup(2);

        group1.addData(ids, scores, color, cases);

        group1.addBar(9, 8, Color.DARK_GRAY);
        group1.addBar(19, 16, Color.BLACK, "test2");
        group2.addBar(20, 6, Color.YELLOW, "test3");
        group2.addBar(14, 7, Color.ORANGE, "test14");


        BarChart.BarStruct barStruct = group1.getBarsInGroup().get(9);
        group1.addBar(9, 5, Color.GREEN);
        group1.addBar(19, 50, Color.CYAN);



        group1.removeBars(14);
        //group2.sortBars();

        /*barChart.sortBars();
        group1.addBar(29, 5, Color.MAGENTA, "jef").setBarContentBoundaries().setTickmarks();
        barChart.sortBars();
        barChart.sortBarsIDs();*/

        /*barChart.createGroup(9, 19, 20, 14).setBarContentBoundaries();

        barChart.createGroup(3, 9, 29).setTickmarks();*/


        group2.sortBarsIDs();
        barChart.setBarContentBoundaries();

        group1.getBarsInGroup().get(19).stacks.get(1).getTriangleDetails().get(0).setColor(Color.BLUE);

        barChart.renderBars();

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
