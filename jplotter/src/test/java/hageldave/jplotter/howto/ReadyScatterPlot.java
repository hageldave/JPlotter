package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.charts.ScatterPlot;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.KeyListenerMask;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Points;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.function.IntSupplier;

import static java.awt.event.KeyEvent.VK_ALT;

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
        JFrame frame = new JFrame();
        JLabel selectedPoint = new JLabel();
        ScatterPlot plot = new ScatterPlot(false);
        SelectedPointInfo selectedSelectedPointInfo = new SelectedPointInfo();

        double[][] dataA = randomData(50);
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

        //URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/shuttle/shuttle.tst");
        InputStream statlogsrc = Thread.currentThread().getContextClassLoader().getResourceAsStream("shuttle.tst");
            Scanner sc = new Scanner(statlogsrc);
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
            for (LinkedList<double[]> list : data) {
                Object[] array = list.toArray();
                double[][] addData = new double[list.size()][2];
                for (int j = 0; j < array.length; j++) {
                    addData[j] = (double[]) array[j];
                }
                // adds data to scatter plot
                plot.addData(index, addData, glyphclasses[index], new Color(classcolors.getColor(index)), classLabels[index]);
                index++;
            }
            // TODO discuss this - two options to get description - pointsinrenderer & extendedPointdetails
            //plot.getPointInRenderer(1).descr = "hi";


        plot.alignCoordsys(140);
        plot.addPanning();
        plot.addZoomViewSelector();
        plot.addScrollZoom();
        plot.addLegendBottom(50);


        plot.new PointClickedInterface(new KeyListenerMask(VK_ALT)) {

            @Override
            public void pointClicked(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                //Text text = new Text((pointLocation.getX() + " " + pointLocation.getY()), 17, Font.PLAIN);
                //text.setColor(pointDetails.point.color.getAsInt());
                //text.setOrigin(new Point2D.Double(pointLocation.getX(), pointLocation.getY()));

                selectedPoint.setText(pointLocation.getX() + " " + pointLocation.getY());
                selectedSelectedPointInfo.setxPos(pointLocation.getX());
                selectedSelectedPointInfo.setyPos(pointLocation.getY());
                selectedSelectedPointInfo.setArrayIndex((int) pointDetails.arrayIndex);
                selectedSelectedPointInfo.setArray(pointDetails.array);
                selectedSelectedPointInfo.setCategory(String.valueOf(pointDetails.descr));
            }

            @Override
            public void pointReleased(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                selectedSelectedPointInfo.clearAll();
            }
        }.register();

        plot.new MouseOverInterface() {
            Points points;
            @Override
            public void mouseOverPoint(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                if (points == null) {
                    points = new Points(pointDetails.pointSet.glyph);
                    Points.PointDetails pointDetail = points.addPoint(pointLocation.getX(), pointLocation.getY());
                    pointDetail.setColor(pointDetails.point.color);
                    pointDetail.setScaling(1.5);
                    plot.getContent().addItemToRender(points);
                    plot.getCanvas().scheduleRepaint();
                }
            }

            @Override
            public void mouseLeftPoint(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                plot.getContent().points.removeItemToRender(points);
                plot.getCanvas().scheduleRepaint();
                points = null;
            }
        }.register();

        plot.new LegendSelectedInterface() {
            final HashSet<ScatterPlot.RenderedPoints> desaturatedPoints = new HashSet<>();
            @Override
            public void legendItemSelected(Point mouseLocation, Legend.GlyphLabel glyphLabel) {
                for (ScatterPlot.RenderedPoints renderedPoints: plot.getPointsInRenderer().values()) {
                    if (renderedPoints.points.glyph != glyphLabel.glyph) {
                        desaturatedPoints.add(renderedPoints);
                        ArrayList<Points.PointDetails> tempPointDetails = renderedPoints.points.getPointDetails();
                        for (Points.PointDetails pointDetails: tempPointDetails) {
                            IntSupplier detailColor = pointDetails.color;
                            Color tempColor = new Color(detailColor.getAsInt());
                            pointDetails.setColor(new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), 5));
                        }
                    }
                }
                plot.getCoordsys().setDirty();
            }

            @Override
            public void legendItemReleased(Point mouseLocation, Legend.GlyphLabel glyphLabel) {
                for (ScatterPlot.RenderedPoints renderedPoints: plot.getPointsInRenderer().values()) {
                        desaturatedPoints.add(renderedPoints);
                        ArrayList<Points.PointDetails> tempPointDetails = renderedPoints.points.getPointDetails();
                        for (Points.PointDetails pointDetails: tempPointDetails) {
                            IntSupplier detailColor = pointDetails.color;
                            Color tempColor = new Color(detailColor.getAsInt());
                            pointDetails.setColor(new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), 255));
                        }
                }
                plot.getCoordsys().setDirty();
            }
        }.register();



        // display within a JFrame

        Component canvas = plot.getCanvas().asComponent();
        frame.setSize(new Dimension(400, 400));
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        Container rightPanel = setupSidepanel();

        // display currently selected point
        rightPanel.add(setupCurrentPoint());
        rightPanel.add(selectedSelectedPointInfo);


        contentPane.add(canvas, BorderLayout.CENTER);
        contentPane.add(rightPanel, BorderLayout.EAST);

        frame.setVisible(true);
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

    protected static Container setupCurrentPoint() {
        Container selectedPointWrapper = new Container();
        selectedPointWrapper.setLayout(new BoxLayout(selectedPointWrapper, BoxLayout.Y_AXIS));
        Box box = Box.createHorizontalBox();
        JLabel selectedPointLabel = new JLabel("Currently selected: ");
        selectedPointLabel.setFont(new Font("Calibri", Font.BOLD, 13));
        selectedPointLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(selectedPointLabel);
        box.add(Box.createHorizontalGlue());
        box.setBorder(new EmptyBorder(15, 15, 15, 15));
        selectedPointWrapper.add(box);

        return selectedPointWrapper;
    }

    protected static Container setupSidepanel() {
        Container boxWrapper = new Container();
        boxWrapper.setLayout(new BoxLayout(boxWrapper, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("Scatterplot Demo App");

        Box box = Box.createHorizontalBox();
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(label);
        box.add(Box.createHorizontalGlue());
        box.setBorder(new EmptyBorder(15, 15, 15, 15));
        boxWrapper.add(box);
        return boxWrapper;
    }

    public static class SelectedPointInfo extends Container {
        protected JLabel category;
        protected JLabel xPos;
        protected JLabel yPos;
        protected JLabel array;
        protected JLabel arrayIndex;

        public SelectedPointInfo() {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.category = new JLabel("");
            this.xPos = new JLabel("");
            this.yPos = new JLabel("");
            this.array = new JLabel("");
            this.arrayIndex = new JLabel("");

            JLabel pointFrom = new JLabel("Point from: ");
            JLabel positionX = new JLabel("Position: x: ");
            JLabel positionY = new JLabel(", y: ");
            JLabel foundInArr = new JLabel("Found in array: ");
            JLabel foundWithIndex = new JLabel("Found with index: ");


            this.add(combineElements(pointFrom, category));
            this.add(combineElements(new JLabel[]{positionX, xPos, positionY, yPos}));
            this.add(combineElements(foundInArr, array));
            this.add(combineElements(foundWithIndex, arrayIndex));
        }

        protected Box combineElements(JLabel first, JLabel second) {
            return combineElements(new JLabel[]{first, second});
        }

        protected Box combineElements(JLabel[] allLabels) {
                Box box = Box.createHorizontalBox();
                for (int i = 0; i < allLabels.length; i++) {
                    allLabels[i].setAlignmentX(Component.LEFT_ALIGNMENT);
                    box.add(allLabels[i]);
                }

                box.add(Box.createHorizontalGlue());
                box.setBorder(new EmptyBorder(5, 15, 5, 15));
                return box;
    }

        public void setCategory(String category) {
            this.category.setText(category);
            this.repaint();
        }

        public void setxPos(double xPos) {
            this.xPos.setText(String.valueOf(xPos));
            this.repaint();
        }

        public void setyPos(double yPos) {
            this.yPos.setText(String.valueOf(yPos));
            this.repaint();
        }

        public void setArray(double[][] array) {
            this.array.setText(String.valueOf(array));
            this.repaint();
        }

        public void setArrayIndex(int arrayIndex) {
            this.arrayIndex.setText(String.valueOf(arrayIndex));
            this.repaint();
        }

        public void clearAll() {
            this.category.setText("");
            this.xPos.setText("");
            this.yPos.setText("");
            this.array.setText("");
            this.arrayIndex.setText("");
        }
    }

}
