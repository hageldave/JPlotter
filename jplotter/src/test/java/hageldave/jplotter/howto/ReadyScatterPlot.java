package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.charts.ScatterPlot;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotDataModel;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotDataModel.ScatterPlotDataModelListener;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotMouseEventListener;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.interaction.SimpleSelectionModel.SimpleSelectionListener;
import hageldave.jplotter.interaction.kml.KeyMaskListener;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.util.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

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

        LinkedList<LinkedList<double[]>> data = new LinkedList<>();

        String[] classLabels = new String[]{
                "Rad Flow",
                "Fpv Close",
                "Fpv Open",
                "High",
                "Bypass",
                "Bpv Close",
                "Bpv Open",
        };
        
        for (int i = 0; i < classLabels.length; i++)
            data.add(new LinkedList<>());

        URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/shuttle/shuttle.tst");
        try (InputStream stream = statlogsrc.openStream();
             Scanner sc = new Scanner(stream)) {
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
        
        JTable datasetTable = new JTable(new TableModel() {
			
        	private ScatterPlotDataModel spdm = plot.getDataModel();
        	private HashMap<TableModelListener, ScatterPlotDataModelListener> listenerLookup = new HashMap<>();
        	
			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void removeTableModelListener(TableModelListener l) {
				ScatterPlotDataModelListener proxy = listenerLookup.remove(l);
				if(proxy != null) 
					spdm.removeListener(proxy);
			}
			
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				Pair<Integer, Integer> locator = spdm.locateGlobalIndex(rowIndex);
				if(columnIndex==0) {
					// class label
					return spdm.getChunkDescription(locator.first);
				} else {
					double[] dataPoint = spdm.getDataChunk(locator.first)[locator.second];
					return (dataPoint.length < columnIndex ? null:dataPoint[columnIndex-1]);
				}
				
			}
			
			@Override
			public int getRowCount() {
				return spdm.numDataPoints();
			}
			
			@Override
			public String getColumnName(int columnIndex) {
				if(columnIndex==0)
					return "class";
				else
					return "feature " + columnIndex; 
			}
			
			@Override
			public int getColumnCount() {
				int maxColCount = 0;
				for(int i=0; i<spdm.numChunks(); i++)
					if(spdm.getDataChunk(i).length > 0)
						maxColCount = Math.max(maxColCount, spdm.getDataChunk(i)[0].length);
				return maxColCount+1; // plus 1 for class label
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if(columnIndex==0)
					return String.class;
				else
					return Double.class;
			}
			
			@Override
			public void addTableModelListener(TableModelListener l) {
				TableModel self = this;
				ScatterPlotDataModelListener proxyListener = new ScatterPlotDataModelListener() {
					@Override
					public void dataChanged(int chunkIdx, double[][] chunkData) {
						l.tableChanged(new TableModelEvent(self));
					}
					@Override
					public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx) {
						l.tableChanged(new TableModelEvent(self));
					}
				};
				spdm.addListener(proxyListener);
				listenerLookup.put(l, proxyListener);
			}
		});

        plot.alignCoordsys(1.2);
        plot.addPanning().setKeyListenerMask(new KeyMaskListener(VK_W));
        plot.addRectangleSelectionZoom();
        plot.addScrollZoom();
        plot.placeLegendOnBottom();
        
        SimpleSelectionModel<Pair<Integer, Integer>> selectedDataPoints = new SimpleSelectionModel<Pair<Integer,Integer>>();
        {
        	selectedDataPoints.addSelectionListener(s->{
        		int[] selectedRows = s.stream().mapToInt(pair->plot.getDataModel().getGlobalIndex(pair.first, pair.second)).toArray();
        		datasetTable.getSelectionModel().setValueIsAdjusting(true);
        		datasetTable.getSelectionModel().clearSelection();
        		for(int i:selectedRows)
        			datasetTable.getSelectionModel().addSelectionInterval(i,i);
        		datasetTable.getSelectionModel().setValueIsAdjusting(false);
        		if(selectedRows.length == 1) {
        			datasetTable.scrollRectToVisible(datasetTable.getCellRect(selectedRows[0],0, true));
        		}
        	});
        	
        }
        datasetTable.getSelectionModel().addListSelectionListener(e->{
        	if(e.getValueIsAdjusting())
        		return;
        	int[] selectedRows = datasetTable.getSelectedRows();
        	List<Pair<Integer, Integer>> selectedInstances = Arrays.stream(selectedRows).mapToObj(i->plot.getDataModel().locateGlobalIndex(i)).collect(Collectors.toList());
        	selectedDataPoints.setSelection(selectedInstances);
        });
        
        selectedDataPoints.addSelectionListener(new SimpleSelectionListener<Pair<Integer,Integer>>() {
        	
        	// TODO: bring selected points to front
        	Points backlight = new Points(DefaultGlyph.SQUARE_F);
        	
        	{
        		plot.getContentHighlight().addItemToRender(backlight);
        	}
        	
			@Override
			public void selectionChanged(SortedSet<Pair<Integer, Integer>> selection) {
				backlight.removeAllPoints();
				for(Pair<Integer, Integer> instance : selection) {
					PointDetails point = plot.getPointsForChunk(instance.first).getPointDetails().get(instance.second);
					backlight.addPoint(point.location).setScaling(point.scale.getAsDouble()*1.2).setColor(Color.LIGHT_GRAY);
				}
				plot.getCanvas().scheduleRepaint();
			}
		});
        
        plot.addScatterPlotMouseEventListener(new ScatterPlotMouseEventListener() {
        	
        	Points pointHighlight;
        	boolean chunkHighlighted=false;
        	{
        		pointHighlight = new Points(DefaultGlyph.CIRCLE_F);
				plot.getContentHighlight().points.addItemToRender(pointHighlight);
        	}
        	
        	@Override
        	public void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
        		if(mouseEventType==MOUSE_EVENT_TYPE_CLICKED) {
        			selectedDataPoints.setSelection(Pair.of(chunkIdx, pointIdx));
        		}
        		
        		if(mouseEventType==MOUSE_EVENT_TYPE_MOVED) {
        			PointDetails visiblePoint = plot.getPointsForChunk(chunkIdx).getPointDetails().get(pointIdx);
        			pointHighlight.removeAllPoints();
        			Points.PointDetails pointDetail = pointHighlight.addPoint(visiblePoint.location);
        			pointDetail.setColor(visiblePoint.color);
        			pointDetail.setScaling(1.5);
        			plot.getCanvas().scheduleRepaint();
        		}
        	}
        	
        	@Override
        	public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
        		if(mouseEventType==MOUSE_EVENT_TYPE_CLICKED)
        			selectedDataPoints.setSelection();
        		if(mouseEventType==MOUSE_EVENT_TYPE_MOVED) {
        			pointHighlight.removeAllPoints();
        			plot.getCanvas().scheduleRepaint();
        		}
        	}
        	
        	@Override
        	public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        		if(mouseEventType != MOUSE_EVENT_TYPE_MOVED)
        			return;
        		// desaturate everything except corresponding chunk
        		for(int chunk=0; chunk<plot.getDataModel().numChunks(); chunk++) {
        			Points p = plot.getPointsForChunk(chunk);
        			plot.getContentHighlight().points.removeItemToRender(p);
        			double alpha;
        			if(chunk==chunkIdx) {
        				alpha = 1.0;
        				plot.getContentHighlight().addItemToRender(p);
        			} else {
        				alpha = 0.1;
        			}
        			p.setGlobalAlphaMultiplier(alpha).setGlobalSaturationMultiplier(alpha);
        		}
        		chunkHighlighted=true;
        		plot.getCanvas().scheduleRepaint();
        	}
        	
        	@Override
        	public void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        		if(mouseEventType != MOUSE_EVENT_TYPE_MOVED || !chunkHighlighted)
        			return;
        		// resaturate everything
        		for(int chunk=0; chunk<plot.getDataModel().numChunks(); chunk++) {
        			Points p = plot.getPointsForChunk(chunk);
        			p.setGlobalAlphaMultiplier(1.0).setGlobalSaturationMultiplier(1.0);
        			plot.getContentHighlight().points.removeItemToRender(p);
        		}
        		chunkHighlighted=false;
        		plot.getCanvas().scheduleRepaint();
        	}
		});


        // display within a JFrame
        frame.setSize(new Dimension(400, 400));
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        Container bottomPanel = setupSidepanel();

        // display currently selected point
        bottomPanel.add(setupCurrentPoint());
        contentPane.add(canvas, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        // put dataset table on bottom
        datasetTable.setPreferredScrollableViewportSize(new Dimension(400, 150));
        bottomPanel.add(new JScrollPane(datasetTable));

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


}
