package hageldave.jplotter.howto;

import hageldave.jplotter.charts.BarChart;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class ReadyBarChart {
    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {

        BarChart barChart = new BarChart(false, 1);
        ColorMap classcolors = DefaultColorMap.S_VIRIDIS;

        String[] groupLabels = new String[]{
                "Iris Setosa",
                "Iris Versicolor",
                "Iris Virginica"
        };

        String[] barLabels = new String[]{
                "sl",
                "sw",
                "pl",
                "pw",
        };

        HashMap<String, Integer> keymap = new HashMap<>();
        keymap.put("Iris-setosa", 0);
        keymap.put("Iris-versicolor", 1);
        keymap.put("Iris-virginica", 2);


        LinkedList<LinkedList<String[]>> data = new LinkedList<>();
        for (int i = 0; i < 3; i++)
            data.add(new LinkedList());

        URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data");
        try (InputStream stream = statlogsrc.openStream();
             Scanner sc = new Scanner(stream)) {
            int i = 1;
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                String[] fields = nextLine.split(",");
                String groupClass = fields[fields.length-1];

                fields = Arrays.copyOf(fields, fields.length-1);

                // sort all array values to its group
                if (keymap.get(groupClass) != null)
                    data.get(keymap.get(groupClass)).add(fields);

            }

            // set up groups
            BarGroup groupSetosa = new BarGroup(groupLabels[0]);
            BarGroup groupVersicolor = new BarGroup(groupLabels[1]);
            BarGroup groupVirginica = new BarGroup(groupLabels[2]);

            double[] setosaValues = new double[4];
            double[] versicolorValues = new double[4];
            double[] virginicaValues = new double[4];

            LinkedList<BarGroup> allGroups = new LinkedList<>();
            allGroups.add(groupSetosa);
            allGroups.add(groupVersicolor);
            allGroups.add(groupVirginica);

            LinkedList<double[]> combinedVals = new LinkedList<>();
            combinedVals.add(setosaValues);
            combinedVals.add(versicolorValues);
            combinedVals.add(virginicaValues);

            // now calculate mean for all values and save it in an array
            // TODO: call count differently
            int count = 0;
            for (LinkedList<String[]> category : data) {
                for (int j = 0; j < 4; j++) {
                    double tempValue = 0;
                    int counter = 0;
                    for (String[] allValues : category) {
                        tempValue += Double.parseDouble(allValues[j]);
                        counter++;
                    }
                    if (count == 0) {
                        setosaValues[j] = tempValue/counter;
                    } else if (count == 1) {
                        versicolorValues[j] = tempValue/counter;
                    } else {
                        virginicaValues[j] = tempValue/counter;
                    }
                }
                count++;
            }

            for (int j = 0; j < combinedVals.size(); j++) {
                int index = 0;
                for (double value : combinedVals.get(j)) {
                    allGroups.get(j).addBar(index, value, new Color(classcolors.getColor(index)), barLabels[index]);
                    index++;
                }
            }

            // add all groups to the chart
            for (BarGroup group : allGroups)
                barChart.addData(group);
        }

        barChart.placeLegendBottom()
                .addBarLabel(classcolors.getColor(3), "petal width", 3)
                .addBarLabel(classcolors.getColor(2), "petal length", 2)
                .addBarLabel(classcolors.getColor(1), "sepal width", 1)
                .addBarLabel(classcolors.getColor(0), "sepal length", 0);

        barChart.getBarRenderer().setxAxisLabel("mean (in cm)");
        barChart.getBarRenderer().setyAxisLabel("mean (in cm)");

        // set up gui stuff
        Container buttonWrapper = new Container();
        JButton eachCategory = new JButton("Show each category");
        JButton combined = new JButton("Combined View");
        buttonWrapper.add(combined);
        buttonWrapper.add(eachCategory);
        buttonWrapper.setLayout(new FlowLayout());

        Container contentWrapper = new Container();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.add(barChart.getCanvas().asComponent());
        contentWrapper.add(buttonWrapper);

        JFrame frame = new JFrame();
        frame.getContentPane().add(contentWrapper);
        frame.setTitle("Comparison chart of iris plants");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        barChart.getCanvas().addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeAndWait(()->{
            frame.pack();
            frame.setVisible(true);
        });


        barChart.addBarChartMouseEventListener(new BarChart.BarChartMouseEventListener() {
            @Override
            public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
            }

            @Override
            public void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, BarGroup.Stack stack) {
            }

            @Override
            public void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
            }

            @Override
            public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, Legend.BarLabel legendElement) {
            }
        });

        //barChart.setAlignment(AlignmentConstants.HORIZONTAL);

        barChart.getBarRenderer().setCoordinateView(
                barChart.getBarRenderer().getBounds().getMinX(),
                barChart.getBarRenderer().getBounds().getMinY(),
                barChart.getBarRenderer().getBounds().getMaxX(),
                barChart.getBarRenderer().getBounds().getMaxY());

        barChart.getBarRenderer().setDirty();
        barChart.getCanvas().scheduleRepaint();

        // paint PDF to PDDocument
        PDDocument doc = barChart.getCanvas().paintPDF();
        // save file and choosing filename
        doc.save("barchart_demo.pdf");
        doc.close();

        Document doc2 = barChart.getCanvas().paintSVG();
        SVGUtils.documentToXMLFile(doc2, new File("barchart_demo.svg"));
    }
}
