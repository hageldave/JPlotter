package hageldave.jplotter.howto;

import hageldave.jplotter.charts.CombinedBarChart;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.IntStream;

public class ReadyCombinedBarChart {
    public static void main(String[] args) throws IOException {
        String[] cases = {"A","B","C","D1","D2*"};
        double[] scores = IntStream.range(0, cases.length)
                .mapToDouble(i->Math.random()).toArray();
        int[] ids = IntStream.range(0, cases.length).toArray();

        Color[] color = new Color[]{Color.RED, Color.RED, Color.RED, Color.RED, Color.RED};

        CombinedBarChart barChart = new CombinedBarChart(false);

        BarGroup group1 = new BarGroup("test1");
        group1.addBar(1, 8, Color.RED, "bar2");
        group1.addBar(2, 3, Color.GREEN, "bar1");
        group1.addBar(2, -3, Color.BLACK, "bar1");
        group1.addBar(2, 1, Color.BLUE);

        BarGroup group2 = new BarGroup("test2");
        group2.addBar(1, 6, Color.GREEN, "ba34");
        group2.addBar(2, -4, Color.MAGENTA, "");
        group2.addBar(3, 5, Color.RED, "4");
        group2.addBar(3, 3, Color.ORANGE, "bar1");
        group2.addBar(3, -3, Color.ORANGE, "bar1");
        group2.addBar(7, 17, Color.CYAN, "bar4");
        group2.addBar(33, 80, Color.RED, "bar7");
        group2.addBar(35, -80, Color.PINK, "bar788888888");
        group2.addBar(55, 4, Color.BLUE, "bar29");

        group1.sortBars(Comparator.comparing(o -> o.getBounds().second));
        group2.sortBars(Comparator.comparing(o -> o.getBounds().second));

        BarGroup group3 = new BarGroup();
        group3.addBar(5, 9, Color.DARK_GRAY, "bar4");
        group3.addBar(6, 10, Color.DARK_GRAY, "bar5");

        barChart.addData(group1);
        barChart.addData(group2);
        barChart.addData(group1);
        barChart.placeLegendOnRight()
                .addBarLabel(Color.RED.getRGB(), "test1", 5)
                .addBarLabel(Color.BLUE.getRGB(), "test2", 6);

        group1.getGroupedBars().get(1).stacks.get(0).stackColor = Color.YELLOW;


       barChart.getBarRenderer().setCoordinateView(
                barChart.getBarRenderer().getBounds().getMinX(),
                barChart.getBarRenderer().getBounds().getMinY(),
                barChart.getBarRenderer().getBounds().getMaxX(),
                barChart.getBarRenderer().getBounds().getMaxY());

       barChart.getBarRenderer().setDirty();
        barChart.getCanvas().scheduleRepaint();

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


        // paint PDF to PDDocument
        PDDocument doc = barChart.getCanvas().paintPDF();
        // save file and choosing filename
        doc.save("barchart_demo.pdf");
        doc.close();

        Document doc2 = barChart.getCanvas().paintSVG();
        SVGUtils.documentToXMLFile(doc2, new File("barchart_demo.svg"));
    }

}
