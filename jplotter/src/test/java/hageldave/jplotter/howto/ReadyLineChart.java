package hageldave.jplotter.howto;

import hageldave.jplotter.charts.LineChart;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReadyLineChart {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {

        final TreeMap<Double, Double> hpToMpg = new TreeMap<>();
        final TreeMap<Double, Double> weightToMpg = new TreeMap<>();
        final TreeMap<Double, Double> displacementToMpg = new TreeMap<>();
        final TreeMap<Double, Double> accelerationToMpg = new TreeMap<>();

        URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/auto-mpg/auto-mpg.data");
        try (InputStream stream = statlogsrc.openStream();
             Scanner sc = new Scanner(stream)) {
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                String[] fields = nextLine.replaceAll("\\s\\p{Zs}+", "  ").split("(\\s\\s)|(\")");
                if (!Objects.equals(fields[3], "?")) {
                    hpToMpg.put(Double.valueOf(fields[3]), Double.valueOf(fields[0]));
                }
                weightToMpg.put(Double.valueOf(fields[4]), Double.valueOf(fields[0]));
                displacementToMpg.put(Double.valueOf(fields[2]), Double.valueOf(fields[0]));
                accelerationToMpg.put(Double.valueOf(fields[5]), Double.valueOf(fields[0]));
            }
        }

        // display within a JFrame
        JFrame frame = new JFrame();
        LineChart normalizedChart = new LineChart(false);

        normalizedChart.getCoordsys().setxAxisLabel("Horsepower / Weight / Displacement / ...");
        normalizedChart.getCoordsys().setyAxisLabel("Miles per Gallon");

        LineChart standardChart = new LineChart(false);

        standardChart.getCoordsys().setxAxisLabel("Horsepower / Weight / Displacement / ...");
        standardChart.getCoordsys().setyAxisLabel("Miles per Gallon");

        double[][] hpToMpgArr =
                hpToMpg.entrySet().stream()
                        .map(e -> new double[]{e.getKey(), e.getValue()})
                        .toArray(double[][]::new);

        double[][] weightToMpgArr =
                weightToMpg.entrySet().stream()
                        .map(e -> new double[]{e.getKey(), e.getValue()})
                        .toArray(double[][]::new);

        double[][] displacementToMpgArr =
                displacementToMpg.entrySet().stream()
                        .map(e -> new double[]{e.getKey(), e.getValue()})
                        .toArray(double[][]::new);

        double[][] accelerationToMpgArr =
                accelerationToMpg.entrySet().stream()
                        .map(e -> new double[]{e.getKey(), e.getValue()})
                        .toArray(double[][]::new);

        standardChart.getDataModel().addData(hpToMpgArr, 0, 1, 1, "Horsepower");
        standardChart.getDataModel().addData(weightToMpgArr, 0, 1, 1, "Weight");
        standardChart.getDataModel().addData(displacementToMpgArr, 0, 1, 1, "Displacement");
        standardChart.getDataModel().addData(accelerationToMpgArr, 0, 1, 1, "Acceleration");

        standardChart.getCoordsys().setCoordinateView(-10,-1,3400,100);

        // normalize values in chart
        normalizeValues(hpToMpg, hpToMpgArr);
        normalizeValues(weightToMpg, weightToMpgArr);
        normalizeValues(displacementToMpg, displacementToMpgArr);
        normalizeValues(accelerationToMpg, accelerationToMpgArr);

        normalizedChart.getDataModel().addData(hpToMpgArr, 0, 1, 1, "Horsepower");
        normalizedChart.getDataModel().addData(weightToMpgArr, 0, 1, 1, "Weight");
        normalizedChart.getDataModel().addData(displacementToMpgArr, 0, 1, 1, "Displacement");
        normalizedChart.getDataModel().addData(accelerationToMpgArr, 0, 1, 1, "Acceleration");

        normalizedChart.getCoordsys().setCoordinateView(-0.1,-0.1,1.1,1.1);

        normalizedChart.addLineChartMouseEventListener(new LineChart.LineChartMouseEventListener() {
            Lines pointHighlight;
            boolean chunkHighlighted=false;
            {
                pointHighlight = new Lines();
                normalizedChart.getContentLayer2().lines.addItemToRender(pointHighlight);
            }
            @Override
            public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED)
                    return;
                normalizedChart.highlight();
            }

            @Override
            public void onInsideMouseEventLine(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED)
                    return;
                normalizedChart.highlight(new Pair<>(chunkIdx, pointIdx));
            }

            @Override
            public void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED || !chunkHighlighted)
                    return;

                normalizedChart.highlight();
                chunkHighlighted=false;
            }

            @Override
            public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED)
                    return;
                // on mouse over legend element of chunk: desaturate every point chunk except corresponding chunk
                List<Pair<Integer, Integer>> instancesOfChunk = IntStream.range(0, normalizedChart.getDataModel().chunkSize(chunkIdx))
                        .mapToObj(i->Pair.of(chunkIdx, i))
                        .collect(Collectors.toList());
                normalizedChart.highlight(instancesOfChunk);
                chunkHighlighted=true;
            }
        });

        standardChart.addLineChartMouseEventListener(new LineChart.LineChartMouseEventListener() {
            Lines pointHighlight;
            boolean chunkHighlighted=false;
            {
                pointHighlight = new Lines();
                standardChart.getContentLayer2().lines.addItemToRender(pointHighlight);
            }
            @Override
            public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED)
                    return;
                standardChart.highlight();
            }

            @Override
            public void onInsideMouseEventLine(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED)
                    return;
                standardChart.highlight(new Pair<>(chunkIdx, pointIdx));
            }

            @Override
            public void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED || !chunkHighlighted)
                    return;

                standardChart.highlight();
                chunkHighlighted=false;
            }

            @Override
            public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
                if(mouseEventType != MOUSE_EVENT_TYPE_CLICKED)
                    return;
                // on mouse over legend element of chunk: desaturate every point chunk except corresponding chunk
                List<Pair<Integer, Integer>> instancesOfChunk = IntStream.range(0, standardChart.getDataModel().chunkSize(chunkIdx))
                        .mapToObj(i->Pair.of(chunkIdx, i))
                        .collect(Collectors.toList());
                standardChart.highlight(instancesOfChunk);
                chunkHighlighted=true;
            }
        });

        // set up gui stuff
        Container buttonWrapper = new Container();
        JButton normalView = new JButton("Standard View");
        JButton normalizedView = new JButton("Normalized View");
        buttonWrapper.add(normalizedView);
        buttonWrapper.add(normalView);
        buttonWrapper.setLayout(new FlowLayout());

        standardChart.getCanvas().asComponent().setPreferredSize(new Dimension(700, 450));
        normalizedChart.getCanvas().asComponent().setPreferredSize(new Dimension(700, 450));
        Container contentWrapper = new Container();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.add(normalizedChart.getCanvas().asComponent());
        contentWrapper.add(buttonWrapper);

        normalView.addActionListener(e -> {
            contentWrapper.removeAll();
            contentWrapper.add(standardChart.getCanvas().asComponent());
            contentWrapper.add(buttonWrapper);
            frame.repaint();
            frame.pack();
        });

        normalizedView.addActionListener(e -> {
            contentWrapper.removeAll();
            contentWrapper.add(normalizedChart.getCanvas().asComponent());
            contentWrapper.add(buttonWrapper);
            frame.repaint();
            frame.pack();
        });

        normalizedChart.placeLegendOnBottom();
        standardChart.placeLegendOnBottom();

        frame.getContentPane().add(contentWrapper);
        frame.setTitle("MPG Chart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //canvas.addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeAndWait(() -> {
            frame.pack();
            frame.setVisible(true);
        });

        // set maximum size of the button wrapper, to force canvas to scale matching the resized window
        buttonWrapper.setMaximumSize(new Dimension(buttonWrapper.getWidth(), buttonWrapper.getHeight()));

        // add a pop up menu (on right click) for exporting to SVG
        PopupMenu menu = new PopupMenu();
        standardChart.getCanvas().asComponent().add(menu);
        MenuItem svgExport = new MenuItem("SVG export");
        menu.add(svgExport);
        svgExport.addActionListener(e->{
            Document doc2 = standardChart.getCanvas().paintSVG();
            SVGUtils.documentToXMLFile(doc2, new File("linechart_demo.svg"));
            System.out.println("exported linechart_demo.svg");
        });
        MenuItem pdfExport = new MenuItem("PDF export");
        menu.add(pdfExport);
        pdfExport.addActionListener(e->{
            try {
                PDDocument doc = standardChart.getCanvas().paintPDF();
                doc.save("linechart_demo.pdf");
                doc.close();
                System.out.println("exported linechart_demo.pdf");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        standardChart.getCanvas().asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    menu.show(standardChart.getCanvas().asComponent(), e.getX(), e.getY());
            }
        });

        // add a pop up menu (on right click) for exporting to SVG
        PopupMenu combinedMenu = new PopupMenu();
        normalizedChart.getCanvas().asComponent().add(combinedMenu);
        MenuItem combinedSvgExport = new MenuItem("SVG export");
        combinedMenu.add(combinedSvgExport);
        combinedSvgExport.addActionListener(e->{
            Document doc2 = normalizedChart.getCanvas().paintSVG();
            SVGUtils.documentToXMLFile(doc2, new File("linechart_demo.svg"));
            System.out.println("exported linechart_demo.svg");
        });
        MenuItem combinedPdfExport = new MenuItem("PDF export");
        combinedMenu.add(combinedPdfExport);
        combinedPdfExport.addActionListener(e->{
            try {
                PDDocument doc = normalizedChart.getCanvas().paintPDF();
                doc.save("linechart_demo.pdf");
                doc.close();
                System.out.println("exported linechart_demo.pdf");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        normalizedChart.getCanvas().asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    combinedMenu.show(normalizedChart.getCanvas().asComponent(), e.getX(), e.getY());
            }
        });
    }

    protected static double[][] normalizeValues(TreeMap<Double, Double> inputMap, double[][] outputArray) {
        double xAxisMax = inputMap.keySet().parallelStream().max(Double::compare).get();
        double xAxisMin = inputMap.keySet().parallelStream().min(Double::compare).get();
        double yAxisMin = inputMap.values().parallelStream().min(Double::compare).get();
        double yAxisMax = inputMap.values().parallelStream().max(Double::compare).get();
        Arrays.stream(outputArray).parallel().forEach(e -> e[0] = e[0]-xAxisMin);
        Arrays.stream(outputArray).parallel().forEach(e -> e[0] = e[0]/(xAxisMax-xAxisMin));
        Arrays.stream(outputArray).parallel().forEach(e -> e[1] = e[1]-yAxisMin);
        Arrays.stream(outputArray).parallel().forEach(e -> e[1] = e[1]/(yAxisMax-yAxisMin));
        return outputArray;
    }
}