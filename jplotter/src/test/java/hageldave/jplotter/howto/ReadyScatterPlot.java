package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.charts.ScatterPlot;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotMouseEventListener;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.kml.KeyMaskListener;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
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
        ScatterPlot plot = new ScatterPlot(true);
        Component canvas = plot.getCanvas().asComponent();
        SelectedPointInfo selectedSelectedPointInfo = new SelectedPointInfo(canvas);

        double[][] dataA = randomData(50);
        LinkedList<LinkedList<double[]>> data = new LinkedList<>();
        for (int i = 0; i < 7; i++)
            data.add(new LinkedList());

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
                double[][] array = list.toArray(new double[0][]);
                // adds data to scatter plot
                plot.getDataModel().addData(array, 6, 7, classLabels[index++]);
            }
        }

        plot.alignCoordsys(140);
        plot.addPanning().setKeyListenerMask(new KeyMaskListener(VK_W));
        plot.addRectangleSelectionZoom();
        plot.addScrollZoom();
        plot.placeLegendOnBottom();

        KeyMaskListener mousePointInteractionKeyMask = new KeyMaskListener(VK_ALT);
        plot.getCanvas().asComponent().addKeyListener(mousePointInteractionKeyMask);
        plot.addScatterPlotMouseEventListener(new ScatterPlotMouseEventListener() {
        	
        	Points highlight = null;
        	
        	@Override
        	public void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
        		if(!mousePointInteractionKeyMask.isKeysPressed())
        			return;
        		if(mouseEventType==MOUSE_EVENT_TYPE_CLICKED) {
        			double[] datapoint = plot.getDataModel().getDataChunk(chunkIdx)[pointIdx];
        			selectedSelectedPointInfo.setVisible(true);
        			selectedPoint.setText(coordsysPoint.getX() + " " + coordsysPoint.getY());
        			selectedSelectedPointInfo.setxPos(coordsysPoint.getX());
        			selectedSelectedPointInfo.setyPos(coordsysPoint.getY());
        			selectedSelectedPointInfo.setArrayIndex(pointIdx);
        			selectedSelectedPointInfo.setArray(plot.getDataModel().getDataChunk(chunkIdx));
        			selectedSelectedPointInfo.setArrayText(plot.getDataModel().getChunkDescription(chunkIdx));
        			selectedSelectedPointInfo.setCategory("");
        			selectedSelectedPointInfo.setButtonVisible(true);
        		}
        		
        		if(mouseEventType==MOUSE_EVENT_TYPE_MOVED) {
        			PointDetails visiblePoint = plot.getPointsForChunk(chunkIdx).getPointDetails().get(pointIdx);
        			if (highlight == null) {
        				highlight = new Points(DefaultGlyph.CIRCLE_F);
        				plot.getContent().points.addItemToRender(highlight);
        			}
        			highlight.removeAllPoints();
        			Points.PointDetails pointDetail = highlight.addPoint(visiblePoint.location);
        			pointDetail.setColor(visiblePoint.color);
        			pointDetail.setScaling(1.5);
        			plot.getCanvas().scheduleRepaint();
        		}
        	}
        	
        	@Override
        	public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
        		if(mouseEventType==MOUSE_EVENT_TYPE_CLICKED)
        			selectedSelectedPointInfo.clearAll();
        		if(mouseEventType==MOUSE_EVENT_TYPE_MOVED) {
        			if(highlight != null) {
        				highlight.removeAllPoints();
        				plot.getCanvas().scheduleRepaint();
        			}
        		}
        	}
        	
        	@Override
        	public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        		if(mouseEventType != MOUSE_EVENT_TYPE_MOVED)
        			return;
        		// TODO: desaturate everything except corresponding chunk, for the time being we change alpha instead
        		for(int chunk=0; chunk<plot.getDataModel().numChunks(); chunk++) {
        			double alpha = 0.1;
        			if(chunk==chunkIdx)
        				alpha = 1.0;
        			plot.getPointsForChunk(chunk).setGlobalAlphaMultiplier(alpha).setGlobalSaturationMultiplier(alpha);
        		}
        		plot.getCanvas().scheduleRepaint();
        	}
        	
        	@Override
        	public void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        		if(mouseEventType != MOUSE_EVENT_TYPE_MOVED)
        			return;
        		// TODO: resaturate everything, for the time being we change alpha instead
        		for(int chunk=0; chunk<plot.getDataModel().numChunks(); chunk++) {
        			plot.getPointsForChunk(chunk).setGlobalAlphaMultiplier(1.0).setGlobalSaturationMultiplier(1.0);
        		}
        		plot.getCanvas().scheduleRepaint();
        	}
		});

//        plot.new LegendSelectedInterface() {
//            final HashSet<ScatterPlot.RenderedPoints> desaturatedPoints = new HashSet<>();
//            @Override
//            public void legendItemSelected(Point mouseLocation, Legend.GlyphLabel glyphLabel) {
//                for (ScatterPlot.RenderedPoints renderedPoints: plot.getPointsInRenderer().values()) {
//                    if (renderedPoints.points.glyph != glyphLabel.glyph) {
//                        toggleLegendItems(desaturatedPoints, renderedPoints, 5);
//                    }
//                }
//                plot.getCanvas().scheduleRepaint();
//            }
//
//            @Override
//            public void legendItemReleased(Point mouseLocation, Legend.GlyphLabel glyphLabel) {
//                for (ScatterPlot.RenderedPoints renderedPoints: plot.getPointsInRenderer().values()) {
//                    toggleLegendItems(desaturatedPoints, renderedPoints, 255);
//                }
//                plot.getCanvas().scheduleRepaint();
//            }
//
//            @Override
//            public void legendItemHovered(Point mouseLocation, Legend.GlyphLabel glyphLabel) { }
//
//            @Override
//            public void legendItemLeft(Point mouseLocation, Legend.GlyphLabel glyphLabel) { }
//        }.register();

//        plot.new PointsSelectedInterface(new KeyMaskListener(VK_TAB)) {
//            @Override
//            public void pointsSelected(Rectangle2D bounds, ArrayList<double[][]> data, ArrayList<Double> dataIndices, ArrayList<ScatterPlot.ExtendedPointDetails> points) {
//                System.out.println(data);
//                System.out.println(dataIndices);
//                System.out.println(points);
//            }
//        };



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

//    public static void toggleLegendItems(final HashSet<ScatterPlot.RenderedPoints> desaturatedPoints, final ScatterPlot.RenderedPoints renderedPoints, final int saturation) {
//        desaturatedPoints.add(renderedPoints);
//        ArrayList<Points.PointDetails> tempPointDetails = renderedPoints.points.getPointDetails();
//        for (Points.PointDetails pointDetails: tempPointDetails) {
//            IntSupplier detailColor = pointDetails.color;
//            Color tempColor = new Color(detailColor.getAsInt());
//            pointDetails.setColor(new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), saturation));
//        }
//    }

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
