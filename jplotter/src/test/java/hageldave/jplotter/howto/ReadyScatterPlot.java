package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.IrisDataset;
import hageldave.jplotter.charts.ScatterPlot;
import hageldave.jplotter.charts.ScatterPlot.PointSetSelectionListener;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotDataModel;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotDataModelListener;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotMouseEventListener;
import hageldave.jplotter.charts.ScatterPlot.ScatterPlotVisualMapping;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.interaction.kml.KeyMaskListener;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.util.Pair;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReadyScatterPlot {
	

    public static void main(String[] args) throws IOException {
//    	minimal();
    	sophisticated();
    }
    
    public static void minimal() {
    	IrisDataset dataset = new IrisDataset();
    	ScatterPlot plot = new ScatterPlot(false);
    	int xAttrib = 0;
    	int yAttrib = 1;
    	for(int label: new int[]{0,1,2}) {
    		double[][] chunk = IntStream.range(0, dataset.getNumPoints())
    				.filter(i->dataset.getLabels()[i]==label)
    				.mapToObj(i->dataset.getData()[i])
    				.toArray(double[][]::new);
    		plot.getDataModel().addData(chunk, xAttrib, yAttrib, dataset.getLabelNames()[label]);
    	}
    	plot.getCoordsys().setxAxisLabel(dataset.getFeatureNames()[xAttrib]);
    	plot.getCoordsys().setyAxisLabel(dataset.getFeatureNames()[yAttrib]);
    	plot.placeLegendOnBottom();
    	plot.getCoordsys().setLegendBottomHeight(10);
    	plot.alignCoordsys(1.1);
    	plot.display("Iris data set");
    }
    	
    public static void sophisticated() throws IOException {
        // generate or load data
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
        }

        // create scatter plot of dataset
        ScatterPlot plot = new ScatterPlot(true);
        plot.setVisualMapping(new ScatterPlotVisualMapping() {
        	// standard scatter plot glyphs excluding CROSS because we can't easily draw an outline for CROSS
        	Glyph[] glyphs = new Glyph[] {
    				DefaultGlyph.CIRCLE_F,
    				DefaultGlyph.SQUARE_F,
    				DefaultGlyph.TRIANGLE_F,
    				DefaultGlyph.CIRCLE,
    				DefaultGlyph.SQUARE,
    				DefaultGlyph.TRIANGLE,
    		};
        	@Override
        	public Glyph getGlyphForChunk(int chunkIdx, String chunkDescr) {
        		return glyphs[chunkIdx%glyphs.length];
        	}
		});
        
        // feed dataset to plot
        for(int i=0; i<data.size(); i++) {
        	LinkedList<double[]> list = data.get(i);
        	double[][] array = list.toArray(new double[0][]);
        	// adds data to scatter plot
        	plot.getDataModel().addData(array, 0, 2, classLabels[i]);
        }
        plot.alignCoordsys(1.2);
        plot.placeLegendOnBottom();
        
        // basic coordinate system interaction schemes
        plot.addPanning();
        plot.addRectangleSelectionZoom();
        plot.addScrollZoom();
        plot.getCanvas().asComponent().addMouseListener(new MouseAdapter() {
        	@Override /* get focus for key events whenever mouse enters this component */
        	public void mouseEntered(MouseEvent e) {plot.getCanvas().asComponent().requestFocus();}
		});
        plot.addRectangularPointSetSelector(new KeyMaskListener(KeyEvent.VK_S));
        plot.addRopePointSetSelector(new KeyMaskListener(KeyEvent.VK_R));

        // create a table that uses the plot's data model 
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
				if(columnIndex==0)
					return rowIndex;
				if(columnIndex==1)
					// class label
					return spdm.getChunkDescription(locator.first);
				// data
				double[] dataPoint = spdm.getDataChunk(locator.first)[locator.second];
				return (columnIndex-2 >= dataPoint.length ? null:dataPoint[columnIndex-2]);	
			}
			
			@Override
			public int getRowCount() {
				return spdm.numDataPoints();
			}
			
			@Override
			public String getColumnName(int columnIndex) {
				if(columnIndex==0)
					return "idx";
				if(columnIndex==1)
					return "class";
				return "val " + (columnIndex-1); 
			}
			
			@Override
			public int getColumnCount() {
				int maxColCount = 0;
				for(int i=0; i<spdm.numChunks(); i++)
					if(spdm.getDataChunk(i).length > 0)
						maxColCount = Math.max(maxColCount, spdm.getDataChunk(i)[0].length);
				return maxColCount+2; // plus 2 for index and class label
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if(columnIndex==0)
					return Integer.class;
				if(columnIndex==1)
					return String.class;
				return Double.class;
			}
			
			@Override
			public void addTableModelListener(TableModelListener l) {
				// creating a listener for the plot's data model instead since its the source of the table model
				TableModel self = this;
				ScatterPlotDataModelListener proxyListener = new ScatterPlotDataModelListener() {
					@Override
					public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx) {
						l.tableChanged(new TableModelEvent(self));
					}

					@Override
					public void dataChanged(int chunkIdx, double[][] chunkData, int xIdx, int yIdx) {
						l.tableChanged(new TableModelEvent(self));
					}
				};
				spdm.addListener(proxyListener);
				listenerLookup.put(l, proxyListener);
			}
		});
        
        // setup selection model for data points
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
        // couple table's selection model with the general selection model
        datasetTable.getSelectionModel().addListSelectionListener(e->{
        	if(e.getValueIsAdjusting())
        		return;
        	int[] selectedRows = datasetTable.getSelectedRows();
        	List<Pair<Integer, Integer>> selectedInstances = Arrays.stream(selectedRows).mapToObj(i->plot.getDataModel().locateGlobalIndex(i)).collect(Collectors.toList());
        	selectedDataPoints.setSelection(selectedInstances);
        });
        // couple scatterplots selection model with general selection model
        plot.addPointSetSelectionListener(new PointSetSelectionListener() {
			@Override
			public void onPointSetSelectionChanged(
					ArrayList<Pair<Integer, TreeSet<Integer>>> selectedPoints,
					Shape selectionArea) 
			{
				List<Pair<Integer, Integer>> toAccentuate = selectedPoints.stream()
						.flatMap(pair -> pair.second.stream().map(i->Pair.of(pair.first, i))).collect(Collectors.toList());
				selectedDataPoints.setSelection(toAccentuate);
			}
		});
        
        // on data point selection change: highlight in scatter plot
        selectedDataPoints.addSelectionListener(plot::accentuate);
        
        // setup mouse -> plot interaction
        plot.addScatterPlotMouseEventListener(new ScatterPlotMouseEventListener() {
        	
        	Points pointHighlight;
        	boolean chunkHighlighted=false;
        	{
        		init();
        	}
        	@SuppressWarnings("resource")
        	private void init() {
        		pointHighlight = new Points(DefaultGlyph.CIRCLE_F);
				plot.getContentLayer2().points.addItemToRender(pointHighlight);
        	}
        	
        	@Override
        	public void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
        		/* mouse interacting with point in the coordinate system */

        		if(mouseEventType==MOUSE_EVENT_TYPE_CLICKED) {
        			// on click: select data point
        			selectedDataPoints.setSelection(Pair.of(chunkIdx, pointIdx));
				}
        		
        		if(mouseEventType==MOUSE_EVENT_TYPE_MOVED) {
        			// on mouse over: highlight point under cursor
        			plot.emphasize(Pair.of(chunkIdx, pointIdx));
        		}
        	}
        	
        	@Override
        	public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
        		/* mouse in coordinate system but NOT interacting with point (is in empty area) */
        		
        		if(mouseEventType==MOUSE_EVENT_TYPE_CLICKED) {
        			// on click: empty selection
        			selectedDataPoints.setSelection();
        		}
        		if(mouseEventType==MOUSE_EVENT_TYPE_MOVED) {
//        			// remove highlight since no point is under cursor
        			plot.emphasize();
        		}
        	}
        	
        	@Override
        	public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        		/* mouse interacting with element outside the coordinate system, e.g. legend element */
        		
        		if(mouseEventType != MOUSE_EVENT_TYPE_MOVED)
        			return;
        		// on mouse over legend element of chunk: desaturate every point chunk except corresponding chunk
        		List<Pair<Integer, Integer>> instancesOfChunk = IntStream.range(0, plot.getDataModel().chunkSize(chunkIdx))
        				.mapToObj(i->Pair.of(chunkIdx, i))
        				.collect(Collectors.toList());
        		plot.highlight(instancesOfChunk);
        		chunkHighlighted=true;
        	}
        	
        	@Override
        	public void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {
        		if(mouseEventType != MOUSE_EVENT_TYPE_MOVED || !chunkHighlighted)
        			return;
        		
        		plot.highlight();
        		chunkHighlighted=false;
        	}
		});


        // display within a JFrame
        JFrame frame = new JFrame();
        frame.setSize(new Dimension(400, 400));
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        // display currently selected point
        contentPane.add(plot.getCanvas().asComponent(), BorderLayout.CENTER);
        contentPane.add(new JScrollPane(datasetTable), BorderLayout.SOUTH);
        // put dataset table on bottom
        datasetTable.setPreferredScrollableViewportSize(new Dimension(500, 150));

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


}
