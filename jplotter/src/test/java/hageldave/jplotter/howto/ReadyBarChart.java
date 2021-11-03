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
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

public class ReadyBarChart {
    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
        BarChart barChart = new BarChart(true, 1);
        BarChart combinedChart = new BarChart(true, 1);
        combinedChart.getCanvas().asComponent().setPreferredSize(new Dimension(900, 400));
        ColorMap classcolors = DefaultColorMap.S_VIRIDIS;

        String[] plantLabels = new String[]{
                "Iris Setosa",
                "Iris Versicolor",
                "Iris Virginica"
        };

        String[] propertyLabels = new String[]{
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
            data.add(new LinkedList<>());

        LinkedList<LinkedList<Double>> setosaHistogramValues = new LinkedList<>();
        LinkedList<LinkedList<Double>> versicolorHistogramValues = new LinkedList<>();
        LinkedList<LinkedList<Double>> virginicaHistogramValues = new LinkedList<>();

        for (int i = 0; i < 4; i++) {
            setosaHistogramValues.add(new LinkedList<>());
            versicolorHistogramValues.add(new LinkedList<>());
            virginicaHistogramValues.add(new LinkedList<>());
        }

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
            BarGroup groupSetosa = new BarGroup(plantLabels[0]);
            BarGroup groupVersicolor = new BarGroup(plantLabels[1]);
            BarGroup groupVirginica = new BarGroup(plantLabels[2]);

            double[] setosaValues = new double[4];
            double[] versicolorValues = new double[4];
            double[] virginicaValues = new double[4];

            LinkedList<BarGroup> allGroups = new LinkedList<>();
            allGroups.add(groupSetosa);
            allGroups.add(groupVersicolor);
            allGroups.add(groupVirginica);

            LinkedList<double[]> allValues = new LinkedList<>();
            allValues.add(setosaValues);
            allValues.add(versicolorValues);
            allValues.add(virginicaValues);

            LinkedList<LinkedList<LinkedList<Double>>> allHistograms = new LinkedList<>();
            allHistograms.add(setosaHistogramValues);
            allHistograms.add(versicolorHistogramValues);
            allHistograms.add(virginicaHistogramValues);

            // now calculate mean for all values and save it in an array
            int index = 0;
            for (LinkedList<String[]> category : data) {
                for (int j = 0; j < 4; j++) {
                    double addedUp = 0;
                    for (String[] singleValue : category) {
                        addedUp += Double.parseDouble(singleValue[j]);
                        allHistograms.get(index).get(j).add(Double.parseDouble(singleValue[j]));
                    }
                    allValues.get(index)[j] = addedUp/category.size();
                }
                index++;
            }

            for (int j = 0; j < allValues.size(); j++) {
                index = 0;
                for (double value : allValues.get(j)) {
                    allGroups.get(j).addBar(index, value, new Color(classcolors.getColor(index)), propertyLabels[index]);
                    index++;
                }
            }

            // add all groups to the chart
            for (BarGroup group : allGroups)
                barChart.addData(group);
        }

        // set up histogram
        BarGroup sepalLength = new BarGroup("sepal length");
        BarGroup sepalWidth = new BarGroup("sepal width");
        BarGroup petalLength = new BarGroup("petal length");
        BarGroup petalWidth = new BarGroup("petal width");

        // sort entries before calculating counts
        for (LinkedList<Double> propertyList : setosaHistogramValues)
            propertyList.sort(Comparator.naturalOrder());
        for (LinkedList<Double> propertyList : versicolorHistogramValues)
            propertyList.sort(Comparator.naturalOrder());
        for (LinkedList<Double> propertyList : virginicaHistogramValues)
            propertyList.sort(Comparator.naturalOrder());

        HashMap<Integer, String> colorStringMapping = new HashMap<>();
        colorStringMapping.put(classcolors.getColor(0), "Iris Setosa");
        colorStringMapping.put(classcolors.getColor(1), "Iris Versicolor");
        colorStringMapping.put(classcolors.getColor(2), "Iris Virginica");

        createHistogramGroup(3.4, sepalLength, classcolors, setosaHistogramValues.get(0), versicolorHistogramValues.get(0), virginicaHistogramValues.get(0));
        createHistogramGroup(1.0, sepalWidth, classcolors, setosaHistogramValues.get(1), versicolorHistogramValues.get(1), virginicaHistogramValues.get(1));
        createHistogramGroup(0.0, petalLength, classcolors, setosaHistogramValues.get(2), versicolorHistogramValues.get(2), virginicaHistogramValues.get(2));
        createHistogramGroup(0.0, petalWidth, classcolors, setosaHistogramValues.get(3), versicolorHistogramValues.get(3), virginicaHistogramValues.get(3));

        combinedChart.addData(sepalLength);
        combinedChart.addData(sepalWidth);
        combinedChart.addData(petalLength);
        combinedChart.addData(petalWidth);

        barChart.placeLegendBottom()
                .addBarLabel(classcolors.getColor(3), "petal width", 3)
                .addBarLabel(classcolors.getColor(2), "petal length", 2)
                .addBarLabel(classcolors.getColor(1), "sepal width", 1)
                .addBarLabel(classcolors.getColor(0), "sepal length", 0);

        combinedChart.placeLegendBottom()
                .addBarLabel(classcolors.getColor(0), plantLabels[0], 3)
                .addBarLabel(classcolors.getColor(1), plantLabels[1], 2)
                .addBarLabel(classcolors.getColor(2), plantLabels[2], 1);


        barChart.getBarRenderer().setxAxisLabel("mean (in cm)");
        barChart.getBarRenderer().setyAxisLabel("mean (in cm)");

        combinedChart.getBarRenderer().setxAxisLabel("number of entries");
        combinedChart.getBarRenderer().setyAxisLabel("number of entries");

        // set up gui stuff
        Container buttonWrapper = new Container();
        JButton meanView = new JButton("Mean View");
        JButton histogramView = new JButton("Histogram View");
        buttonWrapper.add(meanView);
        buttonWrapper.add(histogramView);
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

        // set maximum size of the button wrapper, to force canvas to scale matching the resized window
        buttonWrapper.setMaximumSize(new Dimension(buttonWrapper.getWidth(), buttonWrapper.getHeight()));

        // add eventlisteners to buttons
        meanView.addActionListener(e -> {
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
        histogramView.addActionListener(e -> {
            contentWrapper.removeAll();
            contentWrapper.add(combinedChart.getCanvas().asComponent());
            contentWrapper.add(buttonWrapper);
            combinedChart.getBarRenderer().setCoordinateView(
                    combinedChart.getBarRenderer().getBounds().getMinX(),
                    combinedChart.getBarRenderer().getBounds().getMinY(),
                    combinedChart.getBarRenderer().getBounds().getMaxX()+1,
                    combinedChart.getBarRenderer().getBounds().getMaxY());
            combinedChart.getBarRenderer().setDirty();
            combinedChart.getCanvas().scheduleRepaint();
            frame.repaint();
            frame.pack();
        });


        // set up interaction stuff
        combinedChart.addBarChartMouseEventListener(new BarChart.BarChartMouseEventListener() {
            BarGroup.BarStack selectedBarStack;
            final JPopupMenu popUp = new JPopupMenu("Hovered Plant");
            @Override
            public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
                selectedBarStack = null;
                popUp.setVisible(false);
            }
            @Override
            public void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, BarGroup.BarStack barStack) {
                if (barStack != selectedBarStack) {
                    selectedBarStack = barStack;
                    popUp.setFocusable(false);
                    popUp.setVisible(false);
                    popUp.removeAll();
                    JLabel label = new JLabel("Plant: " + colorStringMapping.get(barStack.stackColor.getRGB()) + ", Frequency in interval: " + barStack.length);
                    label.setBorder(new EmptyBorder(3, 12, 3, 12));
                    popUp.add(label);
                    popUp.show(combinedChart.getCanvas().asComponent(), 50, 20);
                    popUp.setVisible(true);
                }
            }
            @Override
            public void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {}
            @Override
            public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, Legend.BarLabel legendElement) {}
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

    public static void createHistogramGroup(double startBin, BarGroup barGroup, ColorMap classcolors,
                                            LinkedList<Double> setosaHistogramValues, LinkedList<Double> versicolorHistogramValues,
                                            LinkedList<Double> virginicaHistogramValues) {
        int index = 0;
        int setosaCount = 0;
        int versicolorCount = 0;
        int virginicaCount = 0;
        BigDecimal currentBin = BigDecimal.valueOf(startBin);
        while (currentBin.doubleValue() < 9) {
            createHistogram(index, setosaCount, currentBin, setosaHistogramValues, barGroup, new Color(classcolors.getColor(0)));
            createHistogram(index, versicolorCount, currentBin, versicolorHistogramValues, barGroup, new Color(classcolors.getColor(1)));
            createHistogram(index, virginicaCount, currentBin, virginicaHistogramValues, barGroup, new Color(classcolors.getColor(2)));
            index++;
            BigDecimal additor = BigDecimal.valueOf(0.5);
            currentBin = currentBin.add(additor);
        }
    }

    public static void createHistogram(int index, int plantCount, BigDecimal currentBin,
                                LinkedList<Double> plantHistogramValues, BarGroup currentProperty, Color color) {
        for (double value : plantHistogramValues) {
            if (value >= currentBin.doubleValue() && value < (currentBin.doubleValue() + 0.5)) {
                plantCount++;
            } else if (value >= (currentBin.doubleValue()+0.5)) {
                currentProperty.addBar(index, plantCount, color, String.valueOf(currentBin.doubleValue()));
                break;
            }
        }
    }
}
