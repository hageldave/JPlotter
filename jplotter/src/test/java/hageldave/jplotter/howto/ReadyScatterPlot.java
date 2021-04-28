package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.charts.ScatterPlot;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Text;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.Scanner;

public class ReadyScatterPlot {

    private static double[][] randomData(int n){
        double[][] d = new double[n][3];
        for(int i=0; i<n; i++){
            d[i][0]=Math.random()*200-1;
            d[i][1]=Math.random()*200-1;
            d[i][2]=(d[i][1]+1)/2;
        }
        return d;
    }

    public static void main(String[] args) throws IOException {
        // generate or get data

        ScatterPlot plot = new ScatterPlot(false);

        DefaultGlyph[] glyphclasses = new DefaultGlyph[]{
                DefaultGlyph.CIRCLE,
                DefaultGlyph.CIRCLE_F,
                DefaultGlyph.SQUARE,
                DefaultGlyph.SQUARE_F,
                DefaultGlyph.TRIANGLE,
                DefaultGlyph.TRIANGLE_F,
                DefaultGlyph.CROSS
        };
        LinkedList<LinkedList<double[]>> data = new LinkedList<>();
        for (int i = 0; i < 7; i++)
            data.add(new LinkedList());

        Points[] pointclasses = new Points[]{
                new Points(DefaultGlyph.CIRCLE),
                new Points(DefaultGlyph.CIRCLE_F),
                new Points(DefaultGlyph.SQUARE),
                new Points(DefaultGlyph.SQUARE_F),
                new Points(DefaultGlyph.TRIANGLE),
                new Points(DefaultGlyph.TRIANGLE_F),
                new Points(DefaultGlyph.CROSS)
        };

        ColorMap classcolors = DefaultColorMap.Q_12_PAIRED;
        String[] classLabels = new String[]{
                "Rad Flow",
                "Fpv Close",
                "Fpv Open",
                "High",
                "Bypass",
                "Bpv Close",
                "Bpv Open",
        };

        URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/shuttle/shuttle.tst");
        try (InputStream stream = statlogsrc.openStream();
             Scanner sc = new Scanner(stream)) {
                int i = 1;
                while (sc.hasNextLine()) {
                    String nextLine = sc.nextLine();
                    String[] fields = nextLine.split(" ");
                    int pclass = Integer.parseInt(fields[9]) - 1;

                    // get corresponding array and fill with data
                    LinkedList<double[]> list = data.get(pclass);
                    double[] tempArray = new double[2];
                    // fills array with x pos
                    tempArray[0] = Integer.parseInt(fields[6]);
                    // fills array with y pos
                    tempArray[1] = Integer.parseInt(fields[7]);
                    // add 1D array to list
                    list.add(tempArray);
                }

                int index = 0;
                // parse list to array so that scatterplot class can read data
                for (LinkedList<double[]> list: data) {
                    Object[] array = list.toArray();
                    double[][] addData = new double[list.size()][2];
                    for(int j =0; j < array.length; j++) {
                        addData[j] = (double[]) array[j];
                    }
                    // adds data to scatter plot
                    plot.addData(index, addData, glyphclasses[index], new Color(classcolors.getColor(index)));
                    index++;
                }
                plot.getPointInRenderer(1).descr = "hi";
            }

        plot.alignCoordsys(140);
        plot.addPanning();
        plot.addScrollZoom();
        plot.addLegendBottom(50, true);

        plot.new PointClickedInterface() {
            {extModifierMask = KeyEvent.VK_K; }
            @Override
            public void pointClicked(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                Text text = new Text((pointLocation.getX() + " " + pointLocation.getY()), 17, Font.PLAIN);
                System.out.println(pointDetails.point.color.getAsInt());
                System.out.println("index " + pointDetails.arrayIndex);
                System.out.println("index " + pointDetails.array.length);
                text.setColor(pointDetails.point.color.getAsInt());
                text.setOrigin(new Point2D.Double(pointLocation.getX(), pointLocation.getY()));
                plot.getContent().addItemToRender(text);
                plot.getCanvas().scheduleRepaint();
            }

            @Override
            public void pointReleased(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {

            }
        }.register();

        plot.new MouseOverInterface() {
            Points points;
            @Override
            public void mouseOverPoint(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails/*Points.PointDetails pointDetails, double[][] data, int dataIndex*/) {
                if (points == null) {
                    System.out.println("mouse over point");
                    points = new Points(pointDetails.glyph);
                    Points.PointDetails pointDetail = points.addPoint(pointLocation.getX(), pointLocation.getY());
                    pointDetail.setColor(pointDetails.point.color);
                    pointDetail.setScaling(1.5);
                    plot.getContent().addItemToRender(points);
                    plot.getCanvas().scheduleRepaint();
                }
            }

            @Override
            public void mouseLeftPoint(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                System.out.println("called");
                plot.getContent().points.removeItemToRender(points);
                plot.getCanvas().scheduleRepaint();
                points = null;
            }
        }.register();


        // display within a JFrame
        JFrame frame = new JFrame();
        frame.getContentPane().add(plot.getCanvas().asComponent());
        frame.setTitle("scatterplot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        plot.getCanvas().addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
        });

        long t=System.currentTimeMillis()+2000;
        while(t>System.currentTimeMillis());
        if("false".equals("true"))
            SwingUtilities.invokeLater(()->{
                Img img = new Img(frame.getSize());
                img.paint(g2d->frame.paintAll(g2d));
                ImageSaver.saveImage(img.getRemoteBufferedImage(), "scatterplot.png");
            });
    }
}
