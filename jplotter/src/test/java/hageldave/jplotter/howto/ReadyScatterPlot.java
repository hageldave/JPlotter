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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.function.IntSupplier;

import static java.awt.event.KeyEvent.*;

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
        Component canvas = plot.getCanvas().asComponent();
        SelectedPointInfo selectedSelectedPointInfo = new SelectedPointInfo(canvas);

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

        URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/shuttle/shuttle.tst");
        try (InputStream stream = statlogsrc.openStream();
             Scanner sc = new Scanner(stream)) {
            int i = 1;
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                String[] fields = nextLine.split(" ");
                int pclass = Integer.parseInt(fields[9]) - 1;

                LinkedList<double[]> list = data.get(pclass);
                double[] tempArray = new double[fields.length];
                for (int j = 0; j < fields.length; j++) {
                    tempArray[j] = Integer.parseInt(fields[j]);
                }
                list.add(tempArray);
            }

            int index = 0;
            // parse list to array so that scatterplot class can read data
            for (LinkedList<double[]> list : data) {
                Object[] array = list.toArray();
                double[][] addData = new double[list.size()][list.getFirst().length];
                for (int j = 0; j < array.length; j++) {
                    addData[j] = (double[]) array[j];
                }
                // adds data to scatter plot
                plot.addData(index, addData, 6, 7, glyphclasses[index],
                        new Color(classcolors.getColor(index)), classLabels[index]);
                index++;
            }
        }

        plot.alignCoordsys(140);
        plot.addPanning().setKeyListenerMask(new KeyListenerMask(VK_W));
        plot.addZoomViewSelector();
        plot.addScrollZoom();
        plot.addLegendBottom(50);

        plot.new PointClickedInterface(new KeyListenerMask(VK_ALT)) {
            Points points = null;
            @Override
            public void pointClicked(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                selectedSelectedPointInfo.setVisible(true);
                selectedPoint.setText(pointLocation.getX() + " " + pointLocation.getY());
                selectedSelectedPointInfo.setxPos(pointLocation.getX());
                selectedSelectedPointInfo.setyPos(pointLocation.getY());
                selectedSelectedPointInfo.setArrayIndex((int) pointDetails.arrayIndex);
                selectedSelectedPointInfo.setArray(pointDetails.arrayInformation.array);
                selectedSelectedPointInfo.setArrayText(pointDetails.description);
                selectedSelectedPointInfo.setCategory(String.valueOf(pointDetails.glyph));
                selectedSelectedPointInfo.setButtonVisible(true);
            }

            @Override
            public void pointReleased(Point mouseLocation, Point2D pointLocation, ScatterPlot.ExtendedPointDetails pointDetails) {
                selectedSelectedPointInfo.clearAll();
            }

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
                        toggleLegendItems(desaturatedPoints, renderedPoints, 5);
                    }
                }
                plot.getCanvas().scheduleRepaint();
            }

            @Override
            public void legendItemReleased(Point mouseLocation, Legend.GlyphLabel glyphLabel) {
                for (ScatterPlot.RenderedPoints renderedPoints: plot.getPointsInRenderer().values()) {
                    toggleLegendItems(desaturatedPoints, renderedPoints, 255);
                }
                plot.getCanvas().scheduleRepaint();
            }

            @Override
            public void legendItemHovered(Point mouseLocation, Legend.GlyphLabel glyphLabel) { }

            @Override
            public void legendItemLeft(Point mouseLocation, Legend.GlyphLabel glyphLabel) { }
        }.register();

        plot.new PointsSelectedInterface(new KeyListenerMask(VK_TAB)) {
            @Override
            public void pointsSelected(Rectangle2D bounds, ArrayList<double[][]> data, ArrayList<Double> dataIndices, ArrayList<ScatterPlot.ExtendedPointDetails> points) {
                System.out.println(data);
                System.out.println(dataIndices);
                System.out.println(points);
            }
        };



        // display within a JFrame
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
        frame.setTitle("Scatterplot");
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

    public static void toggleLegendItems(final HashSet<ScatterPlot.RenderedPoints> desaturatedPoints, final ScatterPlot.RenderedPoints renderedPoints, final int saturation) {
        desaturatedPoints.add(renderedPoints);
        ArrayList<Points.PointDetails> tempPointDetails = renderedPoints.points.getPointDetails();
        for (Points.PointDetails pointDetails: tempPointDetails) {
            IntSupplier detailColor = pointDetails.color;
            Color tempColor = new Color(detailColor.getAsInt());
            pointDetails.setColor(new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), saturation));
        }
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
        protected JButton jbutton;
        protected double[][] data;
        protected int index;
        protected Component canvas;

        public SelectedPointInfo(final Component canvas) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.category = new JLabel("");
            this.xPos = new JLabel("");
            this.yPos = new JLabel("");
            this.array = new JLabel("");
            this.arrayIndex = new JLabel("");
            this.jbutton = new JButton("Explore");
            this.canvas = canvas;

            JLabel pointFrom = new JLabel("Point from: ");
            JLabel positionX = new JLabel("Position: x: ");
            JLabel positionY = new JLabel(", y: ");
            JLabel foundInArr = new JLabel("Found in array: ");
            JLabel foundWithIndex = new JLabel("Found with index: ");

            this.add(combineElements(pointFrom, category));
            this.add(combineElements(positionX, xPos, positionY, yPos));
            this.add(combineElements(foundInArr, array));
            this.add(combineElements(addOpenButton()));
            this.add(combineElements(foundWithIndex, arrayIndex));
        }

        protected Box combineElements(JComponent... allLabels) {
            Box box = Box.createHorizontalBox();
            for (JComponent allLabel : allLabels) {
                allLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                box.add(allLabel);
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
            this.data = array;
        }

        public void setArrayText(String array) {
            this.array.setText(String.valueOf(array));
            this.repaint();
        }

        public void setArrayIndex(int arrayIndex) {
            this.index = arrayIndex;
            this.arrayIndex.setText(String.valueOf(arrayIndex));
            this.repaint();
        }

        public void clearAll() {
            this.category.setText("");
            this.xPos.setText("");
            this.yPos.setText("");
            this.array.setText("");
            this.arrayIndex.setText("");
            this.jbutton.setVisible(false);
        }

        public void setButtonVisible(boolean value) {
            this.jbutton.setVisible(value);
            this.repaint();
        }

        protected JButton addOpenButton() {
            this.jbutton.setVisible(false);
            jbutton.addActionListener(e -> new ArrayExplorer(data, index, canvas));

            return jbutton;
        }
    }

    public static class ArrayExplorer extends JFrame {
        Container contentPane;
        Component parentCanvas;

        public ArrayExplorer(final double[][] data, final int index, final Component canvas) {
            this.setVisible(true);
            JPanel container = new JPanel();
            container.setLayout(new BorderLayout());
            JTable table = addTable(data);
            JScrollPane scrPane = new JScrollPane(table);
            table.scrollRectToVisible(table.getCellRect(index,0, true));
            table.setRowSelectionInterval(index, index);

            this.parentCanvas = canvas;
            this.contentPane = this.getContentPane();
            this.contentPane.setLayout(new BorderLayout());
            this.contentPane.add(scrPane, BorderLayout.CENTER);
            this.setTitle("Array explorer");
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            // make visible on AWT event dispatch thread
            SwingUtilities.invokeLater(()->{
                this.pack();
                this.setVisible(true);
            });
            this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e){
                    parentCanvas.requestFocus();
                }
            });
        }

        public JTable addTable(final double[][] data) {
            String[][] tableData = new String[data.length][3];
            String[] headers = new String[3];
            headers[0] = "Index";
            headers[1] = "X";
            headers[2] = "Y";
            for (int i = 0; i < tableData.length; i++) {
                tableData[i][0] = String.valueOf(i);
                tableData[i][1] = String.valueOf(data[i][0]);
                tableData[i][2] = String.valueOf(data[i][1]);
            }
            TableModel table_model = new DefaultTableModel(tableData, headers);
            return new JTable(table_model);
        }
    }

}
