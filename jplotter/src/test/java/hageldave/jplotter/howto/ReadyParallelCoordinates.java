package hageldave.jplotter.howto;

import hageldave.jplotter.charts.ParallelCoords;
import hageldave.jplotter.renderers.ParallelCoordsRenderer;
import hageldave.jplotter.util.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeSet;

public class ReadyParallelCoordinates {
    public static void main(String[] args) {
        ParallelCoords coords = new ParallelCoords(false);

        JLabel selectedAxis = new JLabel("No axis selected");
        selectedAxis.setBackground(Color.WHITE);

        ArrayList<double[]> setosaList = new ArrayList<>();
        ArrayList<double[]> versicolorList = new ArrayList<>();
        ArrayList<double[]> virginicaList = new ArrayList<>();

        try (Scanner sc = new Scanner(new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data").openStream())) {
            while(sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                if (nextLine.isEmpty()) {
                    continue;
                }
                String[] fields = nextLine.split(",");
                double[] values = new double[4];
                values[0] = Double.parseDouble(fields[0]);
                values[1] = Double.parseDouble(fields[1]);
                values[2] = Double.parseDouble(fields[2]);
                values[3] = Double.parseDouble(fields[3]);
                if(fields[4].contains("setosa")){
                    setosaList.add(values);
                } else if(fields[4].contains("versicolor")) {
                    versicolorList.add(values);
                } else {
                    virginicaList.add(values);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[][] setosa = new double[][]{};
        double[][] versicolor = new double[][]{};
        double[][] virginica = new double[][]{};

        setosa = setosaList.toArray(setosa);
        versicolor = versicolorList.toArray(versicolor);
        virginica = virginicaList.toArray(virginica);
        coords.getDataModel().addData(setosa,  "setosa");
        coords.getDataModel().addData(versicolor,  "versicolor");
        coords.getDataModel().addData(virginica,  "virginica");

        coords.getDataModel().addFeature(0, new ParallelCoordsRenderer.Feature(4.3,7.9, "sepal length"));
        coords.getDataModel().addFeature(2, new ParallelCoordsRenderer.Feature(1.0,6.9, "petal length"));
        coords.getDataModel().addFeature(3, new ParallelCoordsRenderer.Feature(0.1,2.5, "petal width"));
        coords.getDataModel().addFeature(1, new ParallelCoordsRenderer.Feature(2.0,4.4, "sepal width"));

        coords.setAxisHighlighting(true);

        coords.addParallelCoordsMouseEventListener(new ParallelCoords.ParallelCoordsMouseEventListener() {
            @Override
            public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
                ParallelCoords.ParallelCoordsMouseEventListener.super.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
            }

            @Override
            public void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int segmentIdx) {
                ParallelCoords.ParallelCoordsMouseEventListener.super.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, segmentIdx);
            }

            @Override
            public void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {
                ParallelCoords.ParallelCoordsMouseEventListener.super.onOutsideMouseEventNone(mouseEventType, e);
            }

            @Override
            public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
                ParallelCoords.ParallelCoordsMouseEventListener.super.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
            }

            @Override
            public void notifyMouseEventOnFeature(String mouseEventType, MouseEvent e, int featureIndex, double min, double max) {
                ParallelCoords.ParallelCoordsMouseEventListener.super.notifyMouseEventOnFeature(mouseEventType, e, featureIndex, min, max);
                TreeSet<Integer> values = coords.getDataModel().getIndicesOfSegmentsInRange(0, featureIndex, min, max);

                selectedAxis.setText("Min: " + String.format(Locale.US, "%.2f", min) + ", Max: " + String.format(Locale.US, "%.2f", max));

                ArrayList<Pair<Integer, Integer>> pairs = new ArrayList<>(values.size());
                for (int value: values) {
                    pairs.add(new Pair<>(0, value));
                }

                coords.highlight(pairs);

                if (mouseEventType.equals(ParallelCoords.ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_RELEASED)) {
                    System.out.println(values);
                }
            }

            @Override
            public void notifyMouseEventOffFeature(String mouseEventType, MouseEvent e) {
                ParallelCoords.ParallelCoordsMouseEventListener.super.notifyMouseEventOffFeature(mouseEventType, e);
                selectedAxis.setText("No axis selected");
                coords.highlight();
            }
        });


        coords.placeLegendOnRight();

        // display within a JFrame
        JFrame frame = new JFrame();

        JPanel container = new JPanel();
        JPanel labelWrap = new JPanel();
        labelWrap.setBorder(new EmptyBorder(10,10,10,10));
        labelWrap.add(selectedAxis);
        container.setBackground(Color.WHITE);
        container.setLayout(new BorderLayout());
        container.add(coords.getCanvas().asComponent(), BorderLayout.CENTER);
        container.add(labelWrap, BorderLayout.SOUTH);

        frame.getContentPane().add(container);
        frame.setTitle("Iris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        coords.getCanvas().addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
        });
    }
}
