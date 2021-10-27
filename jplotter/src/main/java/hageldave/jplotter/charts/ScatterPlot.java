package hageldave.jplotter.charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.interaction.kml.CoordSysPanning;
import hageldave.jplotter.interaction.kml.CoordSysScrollZoom;
import hageldave.jplotter.interaction.kml.CoordSysViewSelector;
import hageldave.jplotter.interaction.kml.DynamicCoordsysScrollZoom;
import hageldave.jplotter.interaction.kml.KeyMaskListener;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.PointsRenderer;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.PickingRegistry;
import hageldave.jplotter.util.Utils;

/**
 *
 * The ScatterPlot class provides an easy way to quickly create a ScatterPlot.
 * It includes a JPlotterCanvas, CoordSysRenderer and a PointsRenderer,
 * which are all set up automatically.
 * To edit those, they can be returned with their respective getter methods.
 * When data points are added to the ScatterPlot, they are stored in the pointMap.
 * <p>
 * To add a Dataset to the pointMap, an ID has to be defined as a key.
 * With this ID the Dataset can be removed later on.
 * <p>
 *
 * CHANGELOG
 * - legend now also has a interaction interface (+ its elements are added to registry, etc. pp)
 * - KeylistenerMask has now its own class and is implemented in all interaction interfaces
 * - KeylistenerMask now can store > 1 keys
 * - ExtendedPointDetails stores now its label and Points
 * - update legend automatically if data is added
 * - Continued working on Scatterplot demo class
 *
 * Changelog
 *  - Barchart Implemenation
 *  - Bug mit Legenden items und focus gefixt - Registry reduziert
 *  - neue DataAdd Methode
 *  - Verbesserungen bei InteractionInterface
 *
 * @author lucareichmann
 */
public class ScatterPlot {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected CompleteRenderer contentLayer0;
    protected CompleteRenderer contentLayer1;
    protected CompleteRenderer contentLayer2;
    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();

    final protected ScatterPlotDataModel dataModel = new ScatterPlotDataModel();
    final protected ArrayList<Points> pointsPerDataChunk = new ArrayList<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();
    final protected Legend legend = new Legend();
    protected ScatterPlotVisualMapping visualMapping = new ScatterPlotVisualMapping(){};
    final protected LinkedList<ScatterPlotMouseEventListener> mouseEventListeners = new LinkedList<>();
    final protected LinkedList<PointSetSelectionListener> pointSetSelectionListeners = new LinkedList<>();
    final protected LinkedList<PointSetSelectionListener> pointSetSelectionOngoingListeners = new LinkedList<>();
	private int legendRightWidth = 100;
	private int legendBottomHeight = 60;
	
	protected static final String CUE_HIGHLIGHT = "HIGHLIGHT";
	protected static final String CUE_ACCENTUATE = "ACCENTUATE";
	protected static final String CUE_EMPHASIZE = "EMPHASIZE";
	
	protected HashMap<String, HashMap<Glyph, Points>> glyph2pointMaps = new HashMap<>();
	protected HashMap<String, SimpleSelectionModel<Pair<Integer, Integer>>> cueSelectionModels = new HashMap<>();
//	private HashMap<Glyph, Points> accentuation_glyph2points = new HashMap<Glyph, Points>();
//	protected SimpleSelectionModel<Pair<Integer, Integer>> accentuationSelection = new SimpleSelectionModel<>();
//	
//	private HashMap<Glyph, Points> emphasis_glyph2points = new HashMap<Glyph, Points>();
//	protected SimpleSelectionModel<Pair<Integer, Integer>> emphasisSelection = new SimpleSelectionModel<>();
    
    public ScatterPlot(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public ScatterPlot(final boolean useOpenGL, final String xLabel, final String yLabel) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), xLabel, yLabel);
    }

    public ScatterPlot(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.coordsys = new CoordSysRenderer();
        this.contentLayer0 = new CompleteRenderer();
        this.contentLayer1 = new CompleteRenderer();
        this.contentLayer2 = new CompleteRenderer();
        this.coordsys.setCoordinateView(-1, -1, 1, 1);
        this.coordsys.setContent(contentLayer0.withAppended(contentLayer1).withAppended(contentLayer2));
        this.canvas.setRenderer(coordsys);
        this.coordsys.setxAxisLabel(xLabel);
        this.coordsys.setyAxisLabel(yLabel);
        
        this.dataModel.addListener(new ScatterPlotDataModel.ScatterPlotDataModelListener() {
			@Override
			public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx) {
				onDataAdded(chunkIdx, chunkData, chunkDescription, xIdx, yIdx);
			}
        	@Override
			public void dataChanged(int chunkIdx, double[][] chunkData) {
				onDataChanged(chunkIdx, chunkData);
			}
		});
        
        createMouseEventHandler();
        createRectangularPointSetSelectionCapabilities();
        
        for(String cueType : Arrays.asList(CUE_EMPHASIZE, CUE_ACCENTUATE, CUE_HIGHLIGHT)){
        	this.glyph2pointMaps.put(cueType, new HashMap<>());
        	this.cueSelectionModels.put(cueType, new SimpleSelectionModel<>());
        	this.cueSelectionModels.get(cueType).addSelectionListener(selection->createCue(cueType));
        }
    }
    
    public ScatterPlotVisualMapping getVisualMapping() {
		return visualMapping;
	}
    
    public void setVisualMapping(ScatterPlotVisualMapping visualMapping) {
		this.visualMapping = visualMapping;
		for(Points p:pointsPerDataChunk)
			p.setDirty();
		this.canvas.scheduleRepaint();
	}
    
    public ScatterPlotDataModel getDataModel() {
		return dataModel;
	}
    
    public JPlotterCanvas getCanvas() {
	    return canvas;
	}

	public CoordSysRenderer getCoordsys() {
	    return coordsys;
	}

	public CompleteRenderer getContent() {
	    return getContentLayer0();
	}
	
	public CompleteRenderer getContentLayer0() {
		return contentLayer0;
	}
	
	public CompleteRenderer getContentLayer1() {
		return contentLayer1;
	}
	
	public CompleteRenderer getContentLayer2() {
		return contentLayer2;
	}

	protected synchronized int registerInPickingRegistry(Object obj) {
    	int id = freedPickIds.isEmpty() ? pickingRegistry.getNewID() : freedPickIds.pollFirst();
    	pickingRegistry.register(obj, id);
    	return id;
    }
    
    protected synchronized Object deregisterFromPickingRegistry(int id) {
    	Object old = pickingRegistry.lookup(id);
    	pickingRegistry.register(null, id);
    	freedPickIds.add(id);
    	return old;
    }
    
    protected synchronized void onDataAdded(int chunkIdx, double[][] dataChunk, String chunkDescription, int xIdx, int yIdx) {
    	Points points = new Points(getVisualMapping().getGlyphForChunk(chunkIdx, chunkDescription));
    	pointsPerDataChunk.add(points);
    	contentLayer0.addItemToRender(points);
    	for(int i=0; i<dataChunk.length; i++) {
    		int i_=i;
    		double[] datapoint = dataChunk[i];
    		PointDetails pointDetails = points.addPoint(datapoint[xIdx], datapoint[yIdx]);
    		pointDetails.setColor(()->getVisualMapping().getColorForDataPoint(chunkIdx, chunkDescription, dataChunk, i_));
    		pointDetails.setPickColor(registerInPickingRegistry(new int[]{chunkIdx,i}));
    	}
    	// create a picking ID for use in legend for this data chunk
    	this.legendElementPickIds.add(registerInPickingRegistry(chunkIdx));
    	visualMapping.createLegendElementForChunk(legend, chunkIdx, chunkDescription, legendElementPickIds.get(chunkIdx));
    	// TODO: update highlighting
    	this.canvas.scheduleRepaint();
    }
    
    protected synchronized void onDataChanged(int chunkIdx, double[][] dataChunk) {
    	Points points = pointsPerDataChunk.get(chunkIdx);
    	// collect pick id's from current points for later reuse
    	for(PointDetails pd:points.getPointDetails()) {
    		int pickId = pd.pickColor;
    		if(pickId != 0) {
    			deregisterFromPickingRegistry(pickId);
    		}
    	}
    	points.removeAllPoints();
    	// add changed data
    	for(int i=0; i<dataChunk.length; i++) {
    		int i_=i;
    		double[] datapoint = dataChunk[i];
    		PointDetails pointDetails = points.addPoint(datapoint[dataModel.getXIdx(chunkIdx)], datapoint[dataModel.getYIdx(chunkIdx)]);
    		pointDetails.setColor(()->getVisualMapping().getColorForDataPoint(chunkIdx, dataModel.getChunkDescription(chunkIdx), dataChunk, i_));
    		pointDetails.setPickColor(registerInPickingRegistry(new int[]{chunkIdx,i}));
    	}
    	// TODO: update highlighting
    	
    	this.canvas.scheduleRepaint();
    }
    
    public static class ScatterPlotDataModel {
    	protected ArrayList<double[][]> dataChunks = new ArrayList<>();
    	protected ArrayList<Pair<Integer, Integer>> xyIndicesPerChunk = new ArrayList<>();;
    	protected ArrayList<String> descriptionPerChunk = new ArrayList<>();
    	
    	protected LinkedList<ScatterPlotDataModelListener> listeners = new LinkedList<>();
    	
    	public static interface ScatterPlotDataModelListener {
    		public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx);
    		
    		public void dataChanged(int chunkIdx, double[][] chunkData);
    	}
    	
    	public synchronized void addData(double[][] dataChunk, int xIdx, int yIdx, String chunkDescription) {
    		int chunkIdx = this.dataChunks.size();
    		this.dataChunks.add(dataChunk);
    		this.xyIndicesPerChunk.add(Pair.of(xIdx, yIdx));
    		this.descriptionPerChunk.add(chunkDescription);
    		
    		notifyDataAdded(chunkIdx);
    	}
    	
    	public int numChunks() {
    		return dataChunks.size();
    	}
    	
    	public double[][] getDataChunk(int chunkIdx){
    		return dataChunks.get(chunkIdx);
    	}
    	
    	public int chunkSize(int chunkIdx) {
    		return getDataChunk(chunkIdx).length;
    	}
    	
    	public synchronized void setDataChunk(int chunkIdx, double[][] dataChunk){
    		if(chunkIdx >= numChunks())
    			throw new ArrayIndexOutOfBoundsException("specified chunkIdx out of bounds: " + chunkIdx);
    		this.dataChunks.set(chunkIdx, dataChunk);
    		this.notifyDataChanged(chunkIdx);
    	}
    	
    	public int getXIdx(int chunkIdx) {
    		return xyIndicesPerChunk.get(chunkIdx).first;
    	}
    	
    	public int getYIdx(int chunkIdx) {
    		return xyIndicesPerChunk.get(chunkIdx).second;
    	}
    	
    	public String getChunkDescription(int chunkIdx) {
    		return descriptionPerChunk.get(chunkIdx);
    	}
    	
    	public TreeSet<Integer> getIndicesOfPointsInArea(int chunkIdx, Rectangle2D area){
    		// naive search for contained points
    		// TODO: quadtree supported search (quadtrees per chunk have to be kept up to date)
    		int xIdx = getXIdx(chunkIdx);
    		int yIdx = getYIdx(chunkIdx);
    		double[][] data = getDataChunk(chunkIdx);
    		TreeSet<Integer> containedPointIndices = new TreeSet<>();
    		for(int i=0; i<data.length; i++) {
    			if(area.contains(data[i][xIdx], data[i][yIdx]))
    				containedPointIndices.add(i);
    		}
    		return containedPointIndices;
    	}
    	
    	public int getGlobalIndex(int chunkIdx, int idx) {
    		int globalIdx=0;
    		for(int i=0; i<chunkIdx; i++) {
    			globalIdx += chunkSize(i);
    		}
    		return globalIdx + idx;
    	}
    	
    	public Pair<Integer, Integer> locateGlobalIndex(int globalIdx){
    		int chunkIdx=0;
    		while(globalIdx >= chunkSize(chunkIdx)) {
    			globalIdx -= chunkSize(chunkIdx);
    			chunkIdx++;
    		}
    		return Pair.of(chunkIdx, globalIdx);
    	}
    	
    	public int numDataPoints() {
    		int n = 0;
    		for(int i=0; i<numChunks(); i++)
    			n+=chunkSize(i);
    		return n;
    	}
     	
    	public synchronized ScatterPlotDataModelListener addListener(ScatterPlotDataModelListener l) {
    		listeners.add(l);
    		return l;
    	}
    	
    	public synchronized void removeListener(ScatterPlotDataModelListener l) {
    		listeners.remove(l);
    	}
    	
    	public synchronized void notifyDataAdded(int chunkIdx) {
    		for(ScatterPlotDataModelListener l:listeners)
    			l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx), getXIdx(chunkIdx), getYIdx(chunkIdx));
    	}
    	
    	public synchronized void notifyDataChanged(int chunkIdx) {
    		for(ScatterPlotDataModelListener l:listeners)
    			l.dataChanged(chunkIdx, getDataChunk(chunkIdx));
    	}
    	
    }
    

    
    public static interface ScatterPlotVisualMapping {
    	
    	public default Glyph getGlyphForChunk(int chunkIdx, String chunkDescr) {
    		Glyph[] usualScatterPlotGlyphs = { 
        			DefaultGlyph.CIRCLE_F, DefaultGlyph.SQUARE_F,DefaultGlyph.TRIANGLE_F,
        			DefaultGlyph.CROSS,
        			DefaultGlyph.CIRCLE,DefaultGlyph.SQUARE,DefaultGlyph.TRIANGLE
        	};
    		return usualScatterPlotGlyphs[chunkIdx%usualScatterPlotGlyphs.length];
    	}
    	
    	public default int getColorForDataPoint(int chunkIdx, String chunkDescr, double[][] dataChunk, int pointIdx) {
    		DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
    		return colorMap.getColor(chunkIdx%colorMap.numColors());
    	}
    	
    	public default void createLegendElementForChunk(Legend legend, int chunkIdx, String chunkDescr, int pickColor) {
    		Glyph glyph = getGlyphForChunk(chunkIdx, chunkDescr);
    		int color = getColorForDataPoint(chunkIdx, chunkDescr, null, -1);
    		legend.addGlyphLabel(glyph, color, chunkDescr, pickColor);	
    	}
    	
    	public default void createGeneralLegendElements(Legend legend) {};
    	
    }
    

    public ScatterPlot alignCoordsys(final double scaling) {
        Rectangle2D union = null;
    	for (Points points: pointsPerDataChunk) {
            if(union==null)
            	union = points.getBounds();
            else
            	Rectangle2D.union(points.getBounds(), union, union);
        }
    	if(union != null)
    		this.coordsys.setCoordinateView(Utils.scaleRect(union, scaling));
    	this.canvas.scheduleRepaint();
        return this;
    }

    public ScatterPlot alignCoordsys() {
        return alignCoordsys(1);
    }

    public void placeLegendOnRight() {
    	if(coordsys.getLegendBottom() == legend) {
    		coordsys.setLegendBottom(null);
    		coordsys.setLegendBottomHeight(0);
    	}
    	coordsys.setLegendRight(legend);
    	coordsys.setLegendRightWidth(this.legendRightWidth);
    }
    
    public void placeLegendOnBottom() {
    	if(coordsys.getLegendRight() == legend) {
    		coordsys.setLegendRight(null);
    		coordsys.setLegendRightWidth(0);
    	}
    	coordsys.setLegendBottom(legend);
    	coordsys.setLegendBottomHeight(this.legendBottomHeight);
    }
    
    public void placeLegendNowhere() {
    	if(coordsys.getLegendRight() == legend) {
    		coordsys.setLegendRight(null);
    		coordsys.setLegendRightWidth(0);
    	}
    	if(coordsys.getLegendBottom() == legend) {
    		coordsys.setLegendBottom(null);
    		coordsys.setLegendBottomHeight(0);
    	}
    }

    /**
     * Adds a scroll zoom to the Scatterplot
     *
     * @return the {@link CoordSysScrollZoom} so that it can be further customized
     */
    public DynamicCoordsysScrollZoom addScrollZoom() {
        return new DynamicCoordsysScrollZoom(this.canvas, this.coordsys).register();
    }

    public CoordSysScrollZoom addScrollZoom(final KeyMaskListener keyListenerMask) {
        return new CoordSysScrollZoom(this.canvas, this.coordsys, keyListenerMask).register();
    }

    /**
     *
     * Adds panning functionality to the Scatterplot
     *
     * @return the {@link CoordSysPanning} so that it can be further customized
     */
    public CoordSysPanning addPanning() {
        return new CoordSysPanning(this.canvas, this.coordsys).register();
    }

    public CoordSysPanning addPanning(final KeyMaskListener keyListenerMask) {
        return new CoordSysPanning(this.canvas, this.coordsys, keyListenerMask).register();
    }

    /**
     * Adds a zoom functionality by selecting a rectangle.
     *
     * @return the {@link CoordSysViewSelector} so that it can be further customized
     */
    public CoordSysViewSelector addRectangleSelectionZoom() {
        return new CoordSysViewSelector(this.canvas, this.coordsys) {
            @Override
            public void areaSelected(double minX, double minY, double maxX, double maxY) {
                coordsys.setCoordinateView(minX, minY, maxX, maxY);
            }
        }.register();
    }

    public CoordSysViewSelector addRectangleSelectionZoom(final KeyMaskListener keyListenerMask) {
        return new CoordSysViewSelector(this.canvas, this.coordsys, keyListenerMask) {
            @Override
            public void areaSelected(double minX, double minY, double maxX, double maxY) {
                coordsys.setCoordinateView(minX, minY, maxX, maxY);
            }
        }.register();
    }
    
    
    protected void createMouseEventHandler() {
    	MouseAdapter mouseEventHandler = new MouseAdapter() {
    		@Override
    		public void mouseMoved(MouseEvent e) { mouseAction(ScatterPlotMouseEventListener.MOUSE_EVENT_TYPE_MOVED, e); }
    		
    		@Override
    		public void mouseClicked(MouseEvent e) { mouseAction(ScatterPlotMouseEventListener.MOUSE_EVENT_TYPE_CLICKED, e); }
    		
    		@Override
    		public void mousePressed(MouseEvent e) { mouseAction(ScatterPlotMouseEventListener.MOUSE_EVENT_TYPE_PRESSED, e); }
    		
    		@Override
    		public void mouseReleased(MouseEvent e) { mouseAction(ScatterPlotMouseEventListener.MOUSE_EVENT_TYPE_RELEASED, e); }
    		
    		@Override
    		public void mouseDragged(MouseEvent e) { mouseAction(ScatterPlotMouseEventListener.MOUSE_EVENT_TYPE_DRAGGED, e); }
    		
    		
    		private void mouseAction(String eventType, MouseEvent e) {
    			/* TODO: check key mask listeners of panning, zooming, and rectangular point selection
    			 * to figure out if the mouse event is being handled by them. If not handled by any of them
    			 * then go on with the following.
    			 */
    			if(Utils.swapYAxis(coordsys.getCoordSysArea(),canvas.asComponent().getHeight()).contains(e.getPoint())) {
    				/* mouse inside coordinate area */
    				Point2D coordsysPoint = coordsys.transformAWT2CoordSys(e.getPoint(), canvas.asComponent().getHeight());
    				// get pick color under cursor
    				int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
    				if((pixel & 0x00ffffff) == 0) {
    					notifyInsideMouseEventNone(eventType, e, coordsysPoint);
    				} else {
    					Object pointLocalizer = pickingRegistry.lookup(pixel);
    					if(pointLocalizer instanceof int[]) {
    						int chunkIdx = ((int[])pointLocalizer)[0];
    						int pointIdx = ((int[])pointLocalizer)[1];
    						notifyInsideMouseEventPoint(eventType, e, coordsysPoint, chunkIdx, pointIdx);
    					}
    				}
    			} else {
    				/* mouse outside coordinate area */
    				// get pick color under cursor
    				int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
    				if((pixel & 0x00ffffff) == 0) {
    					notifyOutsideMouseEventeNone(eventType, e);
    				} else {
    					Object miscLocalizer = pickingRegistry.lookup(pixel);
    					if(miscLocalizer instanceof Integer) {
    						int chunkIdx = (int)miscLocalizer;
    						notifyOutsideMouseEventElement(eventType, e, chunkIdx);
    					}
    				}
    			}
    		}
    		
		};
    	this.canvas.asComponent().addMouseListener(mouseEventHandler);
    	this.canvas.asComponent().addMouseMotionListener(mouseEventHandler);
    }
    
    protected synchronized void notifyInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
    	for(ScatterPlotMouseEventListener l:mouseEventListeners)
    		l.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
    }
    
    protected synchronized void notifyInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
    	for(ScatterPlotMouseEventListener l:mouseEventListeners)
    		l.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, pointIdx);
    }
    
    protected synchronized void notifyOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
    	for(ScatterPlotMouseEventListener l:mouseEventListeners)
    		l.onOutsideMouseEventeNone(mouseEventType, e);
    }
    
    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
    	for(ScatterPlotMouseEventListener l:mouseEventListeners)
    		l.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
    }
    
    public static interface ScatterPlotMouseEventListener {
    	static final String MOUSE_EVENT_TYPE_MOVED="moved";
    	static final String MOUSE_EVENT_TYPE_CLICKED="clicked";
    	static final String MOUSE_EVENT_TYPE_PRESSED="pressed";
    	static final String MOUSE_EVENT_TYPE_RELEASED="released";
    	static final String MOUSE_EVENT_TYPE_DRAGGED="dragged";
    	
    	public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {}
        
        public default void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {}
        
        public default void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {}
        
        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {}
    }
    
    public synchronized ScatterPlotMouseEventListener addScatterPlotMouseEventListener(ScatterPlotMouseEventListener l) {
    	this.mouseEventListeners.add(l);
    	return l;
    }
    
    public synchronized boolean removeScatterPlotMouseEventListener(ScatterPlotMouseEventListener l) {
    	return this.mouseEventListeners.remove(l);
    }

    public ArrayList<Pair<Integer, TreeSet<Integer>>> getIndicesOfPointsInArea(Rectangle2D area){
    	ArrayList<Pair<Integer, TreeSet<Integer>>> pointLocators = new ArrayList<>();
    	for(int chunkIdx=0; chunkIdx<dataModel.numChunks(); chunkIdx++) {
    		TreeSet<Integer> containedPointIndices = getDataModel().getIndicesOfPointsInArea(chunkIdx, area);
    		if(!containedPointIndices.isEmpty())
    			pointLocators.add(Pair.of(chunkIdx, containedPointIndices));
    	}
    	return pointLocators;
    }
    
    protected void createRectangularPointSetSelectionCapabilities() {
    	Comparator<Pair<Integer, TreeSet<Integer>>> comparator = new Comparator<Pair<Integer, TreeSet<Integer>>>() {
			@Override
			public int compare(Pair<Integer, TreeSet<Integer>> o1, Pair<Integer, TreeSet<Integer>> o2) {
				int chunkIdxComp = o1.first.compareTo(o2.first);
				if(chunkIdxComp != 0)
					return chunkIdxComp;
				int setSizeComp = Integer.compare(o1.second.size(),o2.second.size());
				if(setSizeComp != 0)
					return setSizeComp;
				// compare elements of both sets in ascending order until mismatch
				Iterator<Integer> it1 = o1.second.iterator();
				Iterator<Integer> it2 = o2.second.iterator();
				int elementComp=0;
				while(it1.hasNext() && elementComp==0) {
					elementComp=it1.next().compareTo(it2.next());
				}
				return elementComp;
			}
		};
    	SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPointsOngoing = new SimpleSelectionModel<>(comparator);
    	SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPoints = new SimpleSelectionModel<>(comparator);
    	
    	Rectangle2D[] selectionRectMemory = {null,null};
    	
    	KeyMaskListener keyMask = new KeyMaskListener(KeyEvent.VK_S);
    	CoordSysViewSelector selector = new CoordSysViewSelector(this.canvas, this.coordsys, keyMask) {
    		@Override
    		public void areaSelectedOnGoing(double minX, double minY, double maxX, double maxY) {
    			if(pointSetSelectionOngoingListeners.isEmpty())
    				return;
    			Rectangle2D area = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
    			selectionRectMemory[0] = area;
    			selectedPointsOngoing.setSelection(getIndicesOfPointsInArea(area));
    		}
    		
    		@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
    			if(pointSetSelectionListeners.isEmpty())
    				return;
    			Rectangle2D area = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
    			selectionRectMemory[1] = area;
    			selectedPoints.setSelection(getIndicesOfPointsInArea(area));
			}
		};
		selector.register();
		
		selectedPointsOngoing.addSelectionListener(s->{
			ArrayList<Pair<Integer, TreeSet<Integer>>> list = new ArrayList<>(s);
			notifyPointSetSelectionChangeOngoing(list, selectionRectMemory[0]);
		});
		selectedPoints.addSelectionListener(s->{
			ArrayList<Pair<Integer, TreeSet<Integer>>> list = new ArrayList<>(s);
			notifyPointSetSelectionChange(list, selectionRectMemory[1]);
		});
    }
    
    public static interface PointSetSelectionListener {
    	public void onPointSetSelectionChanged(ArrayList<Pair<Integer, TreeSet<Integer>>> selectedPoints, Rectangle2D selectionArea);
    }
    
    protected synchronized void notifyPointSetSelectionChangeOngoing(ArrayList<Pair<Integer, TreeSet<Integer>>> list, Rectangle2D rect) {
    	for(PointSetSelectionListener l:pointSetSelectionOngoingListeners) {
			l.onPointSetSelectionChanged(list, rect);
		}
    }
    
    protected synchronized void notifyPointSetSelectionChange(ArrayList<Pair<Integer, TreeSet<Integer>>> list, Rectangle2D rect) {
    	for(PointSetSelectionListener l:pointSetSelectionListeners) {
			l.onPointSetSelectionChanged(list, rect);
		}
    }
    
    public synchronized void addPointSetSelectionListener(PointSetSelectionListener l) {
    	this.pointSetSelectionListeners.add(l);
    }
    
    public synchronized void addPointSetSelectionOngoingListener(PointSetSelectionListener l) {
    	this.pointSetSelectionOngoingListeners.add(l);
    }

	public Points getPointsForChunk(int chunkIdx) {
		return this.pointsPerDataChunk.get(chunkIdx);
	}
	
	/**
	 * Sets up JFrame boilerplate, puts this plot into it, and sets the
	 * frame visible on the AWT event dispatch thread.
	 * @param title of the window
	 * @return the JFrame
	 */
	public JFrame display(String title) {
		JFrame frame = new JFrame();
		frame.setTitle(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(this.canvas.asComponent());
		this.canvas.addCleanupOnWindowClosingListener(frame);
		
		SwingUtilities.invokeLater( ()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		return frame;
	}

	@SafeVarargs
	public final void accentuate(Pair<Integer, Integer> ... toAccentuate) {
		accentuate(Arrays.asList(toAccentuate));
	}
	
	public void accentuate(Iterable<Pair<Integer, Integer>> toAccentuate) {
		SimpleSelectionModel<Pair<Integer,Integer>> selectionModel = this.cueSelectionModels.get(CUE_ACCENTUATE);
		selectionModel.setSelection(toAccentuate);
	}
	
	@SafeVarargs
	public final void emphasize(Pair<Integer, Integer> ... toEmphasize) {
		emphasize(Arrays.asList(toEmphasize));
	}
	
	public void emphasize(Iterable<Pair<Integer, Integer>> toEmphasize) {
		SimpleSelectionModel<Pair<Integer,Integer>> selectionModel = this.cueSelectionModels.get(CUE_EMPHASIZE);
		selectionModel.setSelection(toEmphasize);
	}
	
	@SafeVarargs
	public final void highlight(Pair<Integer, Integer> ... toHighlight) {
		highlight(Arrays.asList(toHighlight));
	}
	
	public void highlight(Iterable<Pair<Integer, Integer>> toHighlight) {
		SimpleSelectionModel<Pair<Integer,Integer>> selectionModel = this.cueSelectionModels.get(CUE_HIGHLIGHT);
		selectionModel.setSelection(toHighlight);
	}
	
	protected void createCue(final String cueType) {
		SimpleSelectionModel<Pair<Integer, Integer>> selectionModel = this.cueSelectionModels.get(cueType);
		SortedSet<Pair<Integer, Integer>> instancesToCue = selectionModel.getSelection();
		
		clearCue(cueType);
		
		switch (cueType) {
		case CUE_ACCENTUATE:
		{
			// emphasis: show point in top layer with outline
			for(Pair<Integer, Integer> instance : instancesToCue) {
				Points points = getPointsForChunk(instance.first);
				PointDetails p = points.getPointDetails().get(instance.second);
				Points front = getOrCreateCuePointsForGlyph(cueType, points.glyph);
				// fake outline by putting slightly larger point behind
				front.addPoint(p.location).setColor(this.coordsys.getColorScheme().getColor1()).setScaling(1.2);
				front.addPoint(p.location).setColor(p.color);
			}
		}
		break;
		case CUE_EMPHASIZE:
		{
			// accentuation: show enlarged point in top layer
			for(Pair<Integer, Integer> instance : instancesToCue) {
				Points points = getPointsForChunk(instance.first);
				PointDetails p = points.getPointDetails().get(instance.second);
				Points front = getOrCreateCuePointsForGlyph(cueType, points.glyph);
				front.addPoint(p.location).setColor(p.color).setScaling(1.5);
			}
		}
		break;
		case CUE_HIGHLIGHT:
		{
			if(instancesToCue.isEmpty()) {
				for(int chunk = 0; chunk < getDataModel().numChunks(); chunk++)
					greyOutChunk(chunk, false);
			} else {
				for(int chunk = 0; chunk < getDataModel().numChunks(); chunk++)
					greyOutChunk(chunk, true);
				for(Pair<Integer, Integer> instance : instancesToCue) {
					Points points = getPointsForChunk(instance.first);
					PointDetails p = points.getPointDetails().get(instance.second);
					Points front = getOrCreateCuePointsForGlyph(cueType, points.glyph);
					front.addPoint(p.location).setColor(p.color);
				}
			}
		}
		break;
		default:
			throw new IllegalStateException("Unhandled cue type " + cueType);
		}
		
		this.getCanvas().scheduleRepaint();
	}
	
	protected void greyOutChunk(int chunkIdx, boolean greyedOut) {
		double factor = greyedOut ? 0.1 : 1.0;
		getPointsForChunk(chunkIdx).setGlobalSaturationMultiplier(factor).setGlobalAlphaMultiplier(factor);
	}
	
	
//	protected void createHighlighting(SortedSet<Pair<Integer, Integer>> instancesToHighlight) {
//		if(instancesToHighlight.isEmpty()) {
//			for(int chunk = 0; chunk < getDataModel().numChunks(); chunk++) {
//				getPointsForChunk(chunk).set
//			}
//		}
//	}
//	
//	protected void createAccentuation() {
//		createAccentutation(this.accentuationSelection.getSelection());
//	}
//	
//	protected void createAccentutation(Iterable<Pair<Integer, Integer>> toAccentuate) {
//		clearAccentuation();
//		for(Pair<Integer, Integer> instance : toAccentuate) {
//			Points points = getPointsForChunk(instance.first);
//			PointDetails p = points.getPointDetails().get(instance.second);
//			Points front = getOrCreateAccentuationPointsForGlyph(points.glyph);
//			front.addPoint(p.location).setColor(this.coordsys.getColorScheme().getColor1()).setScaling(1.2);
//			front.addPoint(p.location).setColor(p.color);
//		}
//		this.getCanvas().scheduleRepaint();
//	}
//	
//	protected void createEmphasis() {
//		createEmphasis(this.emphasisSelection.getSelection());
//	}
//	
//	protected void createEmphasis(Iterable<Pair<Integer, Integer>> toHighlight) {
//		clearEmphasis();
//		for(Pair<Integer, Integer> instance : toHighlight) {
//			Points points = getPointsForChunk(instance.first);
//			PointDetails p = points.getPointDetails().get(instance.second);
//			Points front = getOrCreateEmphasisPointsForGlyph(points.glyph);
//			front.addPoint(p.location).setColor(p.color).setScaling(1.5);
//		}
//		this.getCanvas().scheduleRepaint();
//	}
	
	private Points getOrCreateCuePointsForGlyph(String cue, Glyph g) {
		HashMap<Glyph, Points> glyph2points = this.glyph2pointMaps.get(cue);
		if(!glyph2points.containsKey(g)) {
			Points points = new Points(g);
			glyph2points.put(g, points);
			switch (cue) {
			case CUE_ACCENTUATE: // fallthrough
			case CUE_EMPHASIZE:
			{
				points.setGlobalScaling(1.2);
				getContentLayer2().addItemToRender(points);
			}
			break;
			case CUE_HIGHLIGHT:
			{
				getContentLayer1().addItemToRender(points);
			}
			break;
			default:
				throw new IllegalStateException("unhandled cue case " + cue);
			}
		}
		
		return glyph2points.get(g);
	}
	
	private void clearCue(String cue) {
		HashMap<Glyph, Points> glyph2points = this.glyph2pointMaps.get(cue);
		glyph2points.values().forEach(p->p.removeAllPoints());
	}
    
    
}