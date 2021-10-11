package hageldave.jplotter.howto;

import hageldave.jplotter.charts.BarChart;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.AlignmentConstants;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        BarChart barChart = new BarChart(true, 1);
        BarChart combinedChart = new BarChart(true, AlignmentConstants.HORIZONTAL);

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


            // set up second (combined) chart
            BarGroup combinedGroup = new BarGroup();
            for (int i = 0; i < 4; i++) {
                Color color = new Color(classcolors.getColor(i));
                color = color.brighter();
                for (int j = 0; j < 3; j++) {
                    combinedGroup.addBar(i, (combinedVals.get(j)[i])/3, color, barLabels[i]);
                    color = color.darker();
                }
            }
            combinedChart.addData(combinedGroup);

        }



        barChart.placeLegendBottom()
                .addBarLabel(classcolors.getColor(3), "petal width", 3)
                .addBarLabel(classcolors.getColor(2), "petal length", 2)
                .addBarLabel(classcolors.getColor(1), "sepal width", 1)
                .addBarLabel(classcolors.getColor(0), "sepal length", 0);

        combinedChart.placeLegendBottom()
                .addBarLabel(classcolors.getColor(3), "petal width", 3)
                .addBarLabel(classcolors.getColor(2), "petal length", 2)
                .addBarLabel(classcolors.getColor(1), "sepal width", 1)
                .addBarLabel(classcolors.getColor(0), "sepal length", 0);

        /*combinedChart.placeLegendOnRight()
                        .addBarLabel(new Color(170,170,170).getRGB(), groupLabels[0], 0)
                        .addBarLabel(new Color(120,120,120).getRGB(), groupLabels[1], 1)
                        .addBarLabel(new Color(70,70,70).getRGB(), groupLabels[2], 2);*/


        barChart.getBarRenderer().setxAxisLabel("mean (in cm)");
        barChart.getBarRenderer().setyAxisLabel("mean (in cm)");

        combinedChart.getBarRenderer().setxAxisLabel("mean of all plants (in cm)");
        combinedChart.getBarRenderer().setyAxisLabel("mean of all plants (in cm)");

        // set up gui stuff
        Container buttonWrapper = new Container();
        JButton eachCategory = new JButton("Show each category");
        JButton combined = new JButton("Combined View");
        buttonWrapper.add(eachCategory);
        buttonWrapper.add(combined);
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

        // add eventlisteners to buttons
        eachCategory.addActionListener(e -> {
            contentWrapper.removeAll();
            contentWrapper.add(barChart.getCanvas().asComponent());
            contentWrapper.add(buttonWrapper);
            barChart.getBarRenderer().setCoordinateView(
                    barChart.getBarRenderer().getBounds().getMinX(),
                    barChart.getBarRenderer().getBounds().getMinY(),
                    barChart.getBarRenderer().getBounds().getMaxX(),
                    barChart.getBarRenderer().getBounds().getMaxY());
            barChart.getBarRenderer().setDirty();
            barChart.getCanvas().scheduleRepaint();
            frame.repaint();
            frame.pack();
        });
        combined.addActionListener(e -> {
            contentWrapper.removeAll();
            contentWrapper.add(combinedChart.getCanvas().asComponent());
            contentWrapper.add(buttonWrapper);
            combinedChart.getBarRenderer().setCoordinateView(
                    combinedChart.getBarRenderer().getBounds().getMinX(),
                    combinedChart.getBarRenderer().getBounds().getMinY(),
                    combinedChart.getBarRenderer().getBounds().getMaxX(),
                    combinedChart.getBarRenderer().getBounds().getMaxY());
            combinedChart.getBarRenderer().setDirty();
            combinedChart.getCanvas().scheduleRepaint();
            frame.repaint();
            frame.pack();
        });



        barChart.getBarRenderer().setCoordinateView(
                barChart.getBarRenderer().getBounds().getMinX(),
                barChart.getBarRenderer().getBounds().getMinY(),
                barChart.getBarRenderer().getBounds().getMaxX(),
                barChart.getBarRenderer().getBounds().getMaxY());

        barChart.getBarRenderer().setDirty();
        barChart.getCanvas().scheduleRepaint();

        // add a pop up menu (on right click) for exporting to SVG
        PopupMenu menu = new PopupMenu();
        barChart.getCanvas().asComponent().add(menu);
        MenuItem svgExport = new MenuItem("SVG export");
        menu.add(svgExport);
        svgExport.addActionListener(e->{
            Document doc2 = barChart.getCanvas().paintSVG();
            SVGUtils.documentToXMLFile(doc2, new File("barchart_demo.svg"));
            System.out.println("exported barchart_demo.svg");
        });
        MenuItem pdfExport = new MenuItem("PDF export");
        menu.add(pdfExport);
        pdfExport.addActionListener(e->{
            try {
                PDDocument doc = barChart.getCanvas().paintPDF();
                doc.save("barchart_demo.pdf");
                doc.close();
                System.out.println("exported barchart_demo.pdf");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        barChart.getCanvas().asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    menu.show(barChart.getCanvas().asComponent(), e.getX(), e.getY());
            }
        });


        // add a pop up menu (on right click) for exporting to SVG
        PopupMenu combinedMenu = new PopupMenu();
        combinedChart.getCanvas().asComponent().add(combinedMenu);
        MenuItem combinedSvgExport = new MenuItem("SVG export");
        combinedMenu.add(combinedSvgExport);
        combinedSvgExport.addActionListener(e->{
            Document doc2 = combinedChart.getCanvas().paintSVG();
            SVGUtils.documentToXMLFile(doc2, new File("barchart_demo.svg"));
            System.out.println("exported barchart_demo.svg");
        });
        MenuItem combinedPdfExport = new MenuItem("PDF export");
        combinedMenu.add(combinedPdfExport);
        combinedPdfExport.addActionListener(e->{
            try {
                PDDocument doc = combinedChart.getCanvas().paintPDF();
                doc.save("barchart_demo.pdf");
                doc.close();
                System.out.println("exported barchart_demo.pdf");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        combinedChart.getCanvas().asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    combinedMenu.show(combinedChart.getCanvas().asComponent(), e.getX(), e.getY());
            }
        });
    }
}
