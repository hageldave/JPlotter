package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.interaction.kml.*;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.PickingRegistry;
import hageldave.jplotter.util.QuadTree;
import hageldave.jplotter.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Predicate;
import java.util.*;
import java.util.stream.Collectors;

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
    
    public ScatterPlot(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public ScatterPlot(final boolean useOpenGL, final String xLabel, final String yLabel) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), xLabel, yLabel);
    }

	/**
	 * Creates a new {@link ScatterPlot} object.
	 *
	 * The ScatterPlot consists of a {@link CoordSysRenderer} and multiple content layers.
	 * It also has a data model (see {@link ScatterPlotDataModel}) and a listener (see {@link ScatterPlotDataModelListener})
	 * linked to it, listening for data changes.
	 *
	 * There is also a mouse event handler created in the constructor,
	 * which can be used to listen the events of the {@link ScatterPlotMouseEventListener}.
	 *
	 * @param canvas displaying the {@link ScatterPlot}
	 * @param xLabel label of the x-axis
	 * @param yLabel label of the y-axis
	 */
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
        
        this.dataModel.addListener(new ScatterPlotDataModelListener() {
			@Override
			public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx) {
				onDataAdded(chunkIdx, chunkData, chunkDescription, xIdx, yIdx);
			}
        	@Override
			public void dataChanged(int chunkIdx, double[][] chunkData, int xIdx, int yIdx) {
				onDataChanged(chunkIdx, chunkData);
			}
		});
        
        createMouseEventHandler();
        createSelectionCapabilities();

        for(String cueType : Arrays.asList(CUE_EMPHASIZE, CUE_ACCENTUATE, CUE_HIGHLIGHT)){
        	this.glyph2pointMaps.put(cueType, new HashMap<>());
        	this.cueSelectionModels.put(cueType, new SimpleSelectionModel<>());
        	this.cueSelectionModels.get(cueType).addSelectionListener(selection->createCue(cueType));
        }
    }

	/**
	 * @return the visual mapping {@link ScatterPlotVisualMapping} of the scatter plot
	 */
	public ScatterPlotVisualMapping getVisualMapping() {
		return visualMapping;
	}
    
    public void setVisualMapping(ScatterPlotVisualMapping visualMapping) {
		this.visualMapping = visualMapping;
		for(Points p:pointsPerDataChunk)
			p.setDirty();
		this.canvas.scheduleRepaint();
	}

	/**
	 * @return the data model (see {@link ScatterPlotVisualMapping}) linked to the scatter plot
	 */
	public ScatterPlotDataModel getDataModel() {
		return dataModel;
	}

	/**
	 * @return the underlying canvas on which the scatter plot will be rendered on
	 */
	public JPlotterCanvas getCanvas() {
	    return canvas;
	}

	/**
	 * @return the underlying coordinate system renderer (see {@link CoordSysRenderer}) on which the scatter plot items will be placed on
	 */
	public CoordSysRenderer getCoordsys() {
	    return coordsys;
	}

	/**
	 * @return the content layer
	 */
	public CompleteRenderer getContent() {
	    return getContentLayer0();
	}

	/**
	 * @return the first content layer
	 */
	public CompleteRenderer getContentLayer0() {
		return contentLayer0;
	}

	/**
	 * @return the second content layer, which will overlay contentLayer0
	 */
	public CompleteRenderer getContentLayer1() {
		return contentLayer1;
	}

	/**
	 * @return the third content layer, which will overlay contentLayer0 & contentLayer1
	 */
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
    	// update cues (which may have been in place before)
    	for(String cueType : Arrays.asList(CUE_ACCENTUATE, CUE_EMPHASIZE, CUE_HIGHLIGHT)) {
    		// check cue selections for out of index bounds (in case chunk got smaller)
    		SimpleSelectionModel<Pair<Integer,Integer>> selectionModel = this.cueSelectionModels.get(cueType);
    		SortedSet<Pair<Integer, Integer>> chunkCues = selectionModel.getSelection().subSet(Pair.of(chunkIdx, 0), Pair.of(chunkIdx+1, 0));
    		SortedSet<Pair<Integer, Integer>> invalidCues = chunkCues.tailSet(Pair.of(chunkIdx, getDataModel().chunkSize(chunkIdx)));
    		// remove cues (selection model will not fire in this case, but thats okay since we will call createCue ourselves)
    		invalidCues.clear();
    		this.createCue(cueType);
    	}
    	
    	this.canvas.scheduleRepaint();
    }

	/**
	 * The ScatterPlotDataModel is the inner data model of the ScatterPlot.
	 *
	 * It consists of multiple so-called dataChunks,
	 * which are 2D double arrays holding the data that is used to render the data points in the ScatterPlot.
	 * Therefore, the data model needs to know where the x/y coordinates are located in the double array.
	 * This information has to be specified when data is added to the ScatterPlot (see {@link ScatterPlotDataModel#addData(double[][], int, int, java.lang.String)})
	 * and it's saved int the "xyIndicesPerChunk" array.
	 *
	 * To access any data point later again (to highlight it for example [see {@link ScatterPlot#highlight(java.lang.Iterable)}]), we differentiate the term chunkIdx and pointIdx.
	 * The chunkIdx is the index of the data chunk in the array, where all the added data chunks are saved (so the first added data chunk has chunkIdx 0, the second then chunkIdx 1, ...).
	 * The pointIdx then is the index inside the data chunk, as expected.
	 *
	 * The data model consists also of listeners (see {@link ScatterPlotDataModelListener}),
	 * which methods are called if data is added or changed. The listeners methods then cause a repaint of the ScatterPlot (including legend, etc.),
	 * with the new or changed data.
	 *
	 */
	public class ScatterPlotDataModel {
    	protected ArrayList<double[][]> dataChunks = new ArrayList<>();
    	protected ArrayList<Pair<Integer, Integer>> xyIndicesPerChunk = new ArrayList<>();;
    	protected ArrayList<String> descriptionPerChunk = new ArrayList<>();
    	protected LinkedList<ScatterPlotDataModelListener> listeners = new LinkedList<>();
		protected ArrayList<QuadTree<Pair<double[], Integer>>> quadTreePerChunk = new ArrayList<>();

		/**
		 * Adds data to the data model of the scatter plot.
		 *
		 * @param dataChunk 2D array containing the data that will be rendered in the ScatterPlot
		 * @param xIdx location of the x coordinate in the chunkData array
		 * @param yIdx location of the y coordinate in the chunkData array
		 * @param chunkDescription description of the chunk which will also be shown in the legend
		 */
    	public synchronized void addData(double[][] dataChunk, int xIdx, int yIdx, String chunkDescription) {
    		int chunkIdx = this.dataChunks.size();
    		this.dataChunks.add(dataChunk);
    		this.xyIndicesPerChunk.add(Pair.of(xIdx, yIdx));
    		this.descriptionPerChunk.add(chunkDescription);
			updateQuadTree(chunkIdx, dataChunk);
    		notifyDataAdded(chunkIdx);
    	}

		/**
		 * @return number of data chunks added to the data model
		 */
		public int numChunks() {
    		return dataChunks.size();
    	}

		/**
		 * Returns the dataChunk which has the chunkIdx in the data model.
		 *
		 * @param chunkIdx of the desired dataChunk
		 * @return the dataChunk
		 */
    	public double[][] getDataChunk(int chunkIdx){
    		return dataChunks.get(chunkIdx);
    	}

		/**
		 * Returns the size of the dataChunk which has the chunkIdx in the data model.
		 *
		 * @param chunkIdx of the desired dataChunk
		 * @return size of the data chunk
		 */
    	public int chunkSize(int chunkIdx) {
    		return getDataChunk(chunkIdx).length;
    	}

		/**
		 * Update the data of the dataChunk with the given chunkIdx
		 *
		 * @param chunkIdx id of the chunk that should be updated
		 * @param dataChunk updated data array
		 */
    	public synchronized void setDataChunk(int chunkIdx, double[][] dataChunk){
    		if(chunkIdx >= numChunks())
    			throw new ArrayIndexOutOfBoundsException("specified chunkIdx out of bounds: " + chunkIdx);
    		this.dataChunks.set(chunkIdx, dataChunk);
			updateQuadTree(chunkIdx, dataChunk);
    		this.notifyDataChanged(chunkIdx);
    	}

		/**
		 * Change the x/y coordinate lookup indices of the dataChunk with the given chunkIdx.
		 * @param chunkIdx id of the dataChunk that should be changed
		 * @param xIdx x coordinate lookup index
		 * @param yIdx y coordinate lookup index
		 */
		public synchronized void setXYIndicesPerChunk(int chunkIdx, int xIdx, int yIdx) {
			this.xyIndicesPerChunk.set(chunkIdx, Pair.of(xIdx, yIdx));
			notifyDataChanged(chunkIdx);
		}

		/**
		 * @param chunkIdx id of the dataChunk whose x coordinate lookup index should be returned
		 * @return x coordinate lookup index
		 */
    	public int getXIdx(int chunkIdx) {
    		return xyIndicesPerChunk.get(chunkIdx).first;
    	}

		/**
		 * @param chunkIdx id of the dataChunk whose y coordinate lookup index should be returned
		 * @return y coordinate lookup index
		 */
    	public int getYIdx(int chunkIdx) {
    		return xyIndicesPerChunk.get(chunkIdx).second;
    	}

		/**
		 * @param chunkIdx id of the dataChunk whose chunk description should be returned
		 * @return chunk description of the dataChunk with the given chunkIdx
		 */
    	public String getChunkDescription(int chunkIdx) {
    		return descriptionPerChunk.get(chunkIdx);
    	}

		public QuadTree<Pair<double[], Integer>> getQuadTree(int chunkIdx) {
			return quadTreePerChunk.get(chunkIdx);
		}
    	
    	public TreeSet<Integer> getIndicesOfPointsInArea_naive(int chunkIdx, Shape area){
    		// naive search for contained points
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

    	public TreeSet<Integer> getIndicesOfPointsInArea(int chunkIdx, Rectangle2D area) {
			QuadTree<Pair<double[], Integer>> quadTree = getQuadTree(chunkIdx);
			List<Pair<double[], Integer>> containedPointIndices = QuadTree.getPointsInArea(quadTree, area);
    		return containedPointIndices.stream().map(e -> e.second).collect(Collectors.toCollection(TreeSet::new));
    	}

		public void updateQuadTree(int chunkIndex, double[][] dataChunk) {
			int xIdx = getDataModel().getXIdx(chunkIndex);
			int yIdx = getDataModel().getYIdx(chunkIndex);

			double minX = Integer.MAX_VALUE;
			double maxX = Integer.MIN_VALUE;
			double minY = Integer.MAX_VALUE;
			double maxY = Integer.MIN_VALUE;

			for (double[] coords: dataChunk) {
				minX = Math.min(minX, coords[xIdx]);
				maxX = Math.max(maxX, coords[xIdx]);
				minY = Math.min(minY, coords[yIdx]);
				maxY = Math.max(maxY, coords[yIdx]);
			}

			Rectangle2D boundingBox = new Rectangle2D.Double(minX, minY, maxX-minX+0.01, maxY-minY+0.01);
			QuadTree<Pair<double[], Integer>> qt = new QuadTree<>(4, boundingBox, (Pair<double[], Integer> entry) -> entry.first[0], (Pair<double[], Integer> entry) -> entry.first[1]);
			for (int j = 0; j < dataChunk.length; j++) {
				double[] actualCoordinates = new double[]{dataChunk[j][xIdx], dataChunk[j][yIdx]};
				QuadTree.insert(qt, new Pair<>(actualCoordinates, j));
			}

			if (this.quadTreePerChunk.size() > chunkIndex) {
				this.quadTreePerChunk.set(chunkIndex, qt);
			} else {
				this.quadTreePerChunk.add(qt);
			}
		}

		/**
		 * The dataChunks can be seen as ordered one after the other.
		 * Therefore, an index of a datapoint in a dataChunk can also have a global index.
		 * To calculate the global index, the local index of a dataChunk is added to the sizes of each previous dataChunk.
		 *
		 * @param chunkIdx dataChunk whose local index (idx parameter) is given
		 * @param idx local index of the dataChunk with the given chunkIdx
		 * @return the global index of the idx in the dataChunk with the given chunkIdx
		 */
    	public int getGlobalIndex(int chunkIdx, int idx) {
    		int globalIdx=0;
    		for(int i=0; i<chunkIdx; i++) {
    			globalIdx += chunkSize(i);
    		}
    		return globalIdx + idx;
    	}

		/**
		 * Locates the chunkIdx and pointIdx of a specified globalIdx.
		 * As the data chunks are added sequentially to the data model,
		 * the data implicitly has also a global index. As there is no way to
		 *
		 * @param globalIdx global index which should be mapped to chunkIdx and pointIdx
		 * @return chunkIdx and pointIdx of the given globalIdx
		 */
    	public Pair<Integer, Integer> locateGlobalIndex(int globalIdx){
    		int chunkIdx=0;
    		while(globalIdx >= chunkSize(chunkIdx)) {
    			globalIdx -= chunkSize(chunkIdx);
    			chunkIdx++;
    		}
    		return Pair.of(chunkIdx, globalIdx);
    	}

		/**
		 * @return the number of data points contained in the data model
		 */
		public int numDataPoints() {
    		int n = 0;
    		for(int i=0; i<numChunks(); i++)
    			n+=chunkSize(i);
    		return n;
    	}

		/**
		 * Adds a {@link ScatterPlotDataModelListener} to the ScatterPlot.
		 *
		 * @param l listener to be added
		 * @return the added ScatterPlotDataModelListener
		 */
    	public synchronized ScatterPlotDataModelListener addListener(ScatterPlotDataModelListener l) {
    		listeners.add(l);
    		return l;
    	}

		/**
		 * Removes a {@link ScatterPlotDataModelListener} from the ScatterPlot.
		 *
		 * @param l ScatterPlotDataModelListener to be removed
		 */
		public synchronized void removeListener(ScatterPlotDataModelListener l) {
    		listeners.remove(l);
    	}

		/**
		 * Calls the {@link ScatterPlotDataModelListener#dataAdded(int, double[][], String, int, int)} interface of all registered {@link ScatterPlotDataModelListener}.
		 *
		 * @param chunkIdx data chunk id of the added data
		 */
		public synchronized void notifyDataAdded(int chunkIdx) {
    		for(ScatterPlotDataModelListener l:listeners)
    			l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx), getXIdx(chunkIdx), getYIdx(chunkIdx));
    	}

		/**
		 * Calls the {@link ScatterPlotDataModelListener#dataChanged(int, double[][], int, int)} interface of all registered {@link ScatterPlotDataModelListener}.
		 *
		 * @param chunkIdx data chunk id of the changed data
		 */
		public synchronized void notifyDataChanged(int chunkIdx) {
    		for(ScatterPlotDataModelListener l:listeners)
    			l.dataChanged(chunkIdx, getDataChunk(chunkIdx), getXIdx(chunkIdx), getYIdx(chunkIdx));
    	}
    }

	/**
	 * The ScatterPlotDataModelListener consists of multiple listener interfaces, which are called when the data model is manipulated.
	 */
	public static interface ScatterPlotDataModelListener {
		/**
		 * Whenever data is added to the ScatterPlot, this interface will be called.
		 *
		 * @param chunkIdx index of the dataChunk in the array where all dataChunks are stored
		 * @param chunkData 2D array containing the data that will be rendered in the ScatterPlot
		 * @param chunkDescription label of the data chunk which will also be shown in the legend
		 * @param xIdx location of the x coordinate in the chunkData array
		 * @param yIdx location of the y coordinate in the chunkData array
		 */
		public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx);

		/**
		 * Whenever data is updated in the ScatterPlot, this interface will be called.
		 *
		 * @param chunkIdx index of the data chunk that should be updated
		 * @param chunkData 2D array containing the updated data
		 * @param xIdx index of the x coordinate in the chunkData
		 * @param yIdx index of the y coordinate in the chunkData
		 */
		public void dataChanged(int chunkIdx, double[][] chunkData, int xIdx, int yIdx);
	}

	/**
	 * The ScatterPlotVisualMapping is responsible for mapping the chunks to a glyph and a color.
	 *
	 */
	public static interface ScatterPlotVisualMapping {

		/**
		 * This method returns a glyph to the given chunkIdx.
		 *
		 * As there is only a limited number of glyphs, they are repeated in the same order,
		 * if all of them have been used.
		 *
		 * @param chunkIdx id of the data chunk
		 * @param chunkDescr the label of the data chunk
		 * @return a glyph matching the given chunkIdx
		 */
    	public default Glyph getGlyphForChunk(int chunkIdx, String chunkDescr) {
    		Glyph[] usualScatterPlotGlyphs = { 
        			DefaultGlyph.CIRCLE_F, DefaultGlyph.SQUARE_F,DefaultGlyph.TRIANGLE_F,
        			DefaultGlyph.CROSS,
        			DefaultGlyph.CIRCLE,DefaultGlyph.SQUARE,DefaultGlyph.TRIANGLE
        	};
    		return usualScatterPlotGlyphs[chunkIdx%usualScatterPlotGlyphs.length];
    	}

		/**
		 * This method returns a color to the given chunkIdx.
		 *
		 * As there is only a limited number of colors in the color map (see {@link DefaultColorMap}),
		 * they are repeated in the same order, if all of them have been used.
		 *
		 * @param chunkIdx id of the data chunk
		 * @param chunkDescr the label of the data chunk
		 * @param dataChunk 2D double array containing the data to be rendered
		 * @param pointIdx id of the point in the array
		 * @return color in an integer packed ARGB format
		 */
    	public default int getColorForDataPoint(int chunkIdx, String chunkDescr, double[][] dataChunk, int pointIdx) {
    		DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
    		return colorMap.getColor(chunkIdx%colorMap.numColors());
    	}

		/**
		 * Adds a glyph label element to the legend of the ScatterPlot for a data chunk, using the {@link ScatterPlotVisualMapping#getGlyphForChunk(int, java.lang.String)} &
		 * {@link ScatterPlotVisualMapping#getColorForDataPoint(int, String, double[][], int)} methods.
		 *
		 * @param legend where the new element will be added
		 * @param chunkIdx id of the data chunk
		 * @param chunkDescr description of the data chunk
		 * @param pickColor pick color of the legend element
		 */
    	public default void createLegendElementForChunk(Legend legend, int chunkIdx, String chunkDescr, int pickColor) {
    		Glyph glyph = getGlyphForChunk(chunkIdx, chunkDescr);
    		int color = getColorForDataPoint(chunkIdx, chunkDescr, null, 0);
    		legend.addGlyphLabel(glyph, color, chunkDescr, pickColor);	
    	}
    	
    	public default void createGeneralLegendElements(Legend legend) {};
    	
    }

	/**
	 * Sets the coordinate view (see {@link CoordSysRenderer#setCoordinateView(Rectangle2D)})
	 * accordingly to the bounds of all points in the scatter plot.
	 *
	 * @param scaling scales the coordinate view rectangle by this factor, default is 1
	 * @return this for chaining
	 */
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

	/**
	 * Sets the coordinate view (see {@link CoordSysRenderer#setCoordinateView(Rectangle2D)})
	 * accordingly to the bounds of all points in the scatter plot.
	 *
	 * @return this for chaining
	 */
    public ScatterPlot alignCoordsys() {
        return alignCoordsys(1);
    }

	/**
	 * Sets the legend on the right of the scatter plot.
	 * This replaces the legend on the bottom, if it was set before.
	 */
    public void placeLegendOnRight() {
    	if(coordsys.getLegendBottom() == legend) {
    		coordsys.setLegendBottom(null);
    		coordsys.setLegendBottomHeight(0);
    	}
    	coordsys.setLegendRight(legend);
    	coordsys.setLegendRightWidth(this.legendRightWidth);
    }

	/**
	 * Sets the legend on the bottom of the scatter plot.
	 * This replaces the legend on the right, if it was set before.
	 */
    public void placeLegendOnBottom() {
    	if(coordsys.getLegendRight() == legend) {
    		coordsys.setLegendRight(null);
    		coordsys.setLegendRightWidth(0);
    	}
    	coordsys.setLegendBottom(legend);
    	coordsys.setLegendBottomHeight(this.legendBottomHeight);
    }

	/**
	 * Removes all legends of the scatter plot.
	 */
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
	 * @see ScatterPlot#addScrollZoom(KeyMaskListener)
	 */
	public CoordSysScrollZoom addScrollZoom() {
		return addScrollZoom(false);
	}

	/**
	 * @see ScatterPlot#addScrollZoom(KeyMaskListener)
	 */
    public CoordSysScrollZoom addScrollZoom(boolean mouseFocused) {
        return new CoordSysScrollZoom(this.canvas, this.coordsys, mouseFocused).register();
    }

	/**
	 * Adds a scroll zoom to the Scatterplot
	 *
	 * @param keyMaskListener defines which keys have to pressed during scrolling to initiate the zoom.
	 * @return the {@link CoordSysScrollZoom} so that it can be further customized
	 */
    public CoordSysScrollZoom addScrollZoom(final KeyMaskListener keyMaskListener) {
        return new CoordSysScrollZoom(this.canvas, this.coordsys, keyMaskListener).register();
    }

	/**
	 * @see ScatterPlot#addPanning(KeyMaskListener)
	 */
    public CoordSysPanning addPanning() {
        return new CoordSysPanning(this.canvas, this.coordsys).register();
    }

	/**
	 * Adds panning functionality to the Scatterplot.
	 *
	 * @param keyMaskListener defines which keys have to pressed to initiate the panning.
	 * @return the {@link CoordSysPanning} so that it can be further customized
	 */
    public CoordSysPanning addPanning(final KeyMaskListener keyMaskListener) {
        return new CoordSysPanning(this.canvas, this.coordsys, keyMaskListener).register();
    }

    /**
     * @see ScatterPlot#addRectangleSelectionZoom(KeyMaskListener)
     */
    public CoordSysViewSelector addRectangleSelectionZoom() {
        return new CoordSysViewSelector(this.canvas, this.coordsys) {
            @Override
            public void areaSelected(double minX, double minY, double maxX, double maxY) {
                coordsys.setCoordinateView(minX, minY, maxX, maxY);
            }
        }.register();
    }

	/**
	 * Adds a zoom functionality by selecting a rectangle.
	 *
	 * @param keyMaskListener defines which keys have to pressed during the selection to initiate the zoom.
	 * @return the {@link CoordSysViewSelector} so that it can be further customized
	 */
    public CoordSysViewSelector addRectangleSelectionZoom(final KeyMaskListener keyMaskListener) {
        return new CoordSysViewSelector(this.canvas, this.coordsys, keyMaskListener) {
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
    		l.onOutsideMouseEventNone(mouseEventType, e);
    }
    
    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
    	for(ScatterPlotMouseEventListener l:mouseEventListeners)
    		l.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
    }

	/**
	 * The ScatterPlotMouseEventListener interface contains multiple methods,
	 * notifying if an element has been hit or not (inside and outside the coordsys).
	 */
	public static interface ScatterPlotMouseEventListener {
    	static final String MOUSE_EVENT_TYPE_MOVED="moved";
    	static final String MOUSE_EVENT_TYPE_CLICKED="clicked";
    	static final String MOUSE_EVENT_TYPE_PRESSED="pressed";
    	static final String MOUSE_EVENT_TYPE_RELEASED="released";
    	static final String MOUSE_EVENT_TYPE_DRAGGED="dragged";

		/**
		 * Called whenever the mouse pointer doesn't hit a data point of the ScatterPlot while being inside the coordsys.
		 *
		 * @param mouseEventType type of the mouse event
		 * @param e passed on mouse event of the mouse adapter registering the mouse movements
		 * @param coordsysPoint coordinates of the mouse event inside the coordinate system
		 */
    	public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {}

		/**
		 * Called when the mouse pointer does hit a data point of the ScatterPlot.
		 *
		 * @param mouseEventType type of the mouse event
		 * @param e passed on mouse event of the mouse adapter registering the mouse movements
		 * @param coordsysPoint coordinates of the mouse event inside the coordinate system
		 * @param chunkIdx id of the data chunk
		 * @param pointIdx id of the data point inside the data chunk
		 */
        public default void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {}

		/**
		 * Called when the mouse pointer doesn't hit an element (e.g. legend elements) of the ScatterPlot while being outside the coordsys.
		 *
		 * @param mouseEventType type of the mouse event
		 * @param e passed on mouse event of the mouse adapter registering the mouse movements
		 */
		public default void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {}

		/**
		 * Called when the mouse pointer hits an element (e.g. legend elements) of the ScatterPlot while being outside the coordsys.
		 *
		 * @param mouseEventType type of the mouse event
		 * @param e passed on mouse event of the mouse adapter registering the mouse movements
		 * @param chunkIdx id of the data chunk
		 */
        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {}
    }

	/**
	 * Adds a {@link ScatterPlotMouseEventListener} to the ScatterPlot.
	 *
	 * @param l {@link ScatterPlotMouseEventListener} that implements the interface methods which is called whenever one of the defined mouse events happens
	 * @return {@link ScatterPlotMouseEventListener} for chaining
	 */
    public synchronized ScatterPlotMouseEventListener addScatterPlotMouseEventListener(ScatterPlotMouseEventListener l) {
    	this.mouseEventListeners.add(l);
    	return l;
    }

	/**
	 * Removes the specified {@link ScatterPlotMouseEventListener} from the ScatterPlot.
	 *
	 * @param l {@link ScatterPlotMouseEventListener} that should be removed
	 * @return true if the {@link ScatterPlotMouseEventListener} was added to the ScatterPlot before
	 */
    public synchronized boolean removeScatterPlotMouseEventListener(ScatterPlotMouseEventListener l) {
    	return this.mouseEventListeners.remove(l);
    }

	/**
	 * Return the indices of all points contained in the specified area.
	 *
	 * @param area where point indices are collected
	 * @return List of {@link Pair}, which consists of chunkIds and the corresponding point indices contained in the area
	 */
    public ArrayList<Pair<Integer, TreeSet<Integer>>> getIndicesOfPointsInArea(Shape area){
    	ArrayList<Pair<Integer, TreeSet<Integer>>> pointLocators = new ArrayList<>();
    	for(int chunkIdx=0; chunkIdx<dataModel.numChunks(); chunkIdx++) {
    		// get candidates that are in bounding rectangle of shape
    		TreeSet<Integer> containedPointIndices = getDataModel().getIndicesOfPointsInArea(chunkIdx, area.getBounds2D());
    		// check candidates against shape
    		if(!(area instanceof Rectangle2D)) {
    			int chnkidx = chunkIdx;
    			containedPointIndices.removeIf(new Predicate<Integer>() {
    				double[][] chunk = getDataModel().getDataChunk(chnkidx);
    				int xidx = getDataModel().getXIdx(chnkidx);
    				int yidx = getDataModel().getYIdx(chnkidx);
    				public boolean test(Integer i) {
    					return !area.contains(chunk[i][xidx], chunk[i][yidx]);
    				};
    			});
    		}
    		if(!containedPointIndices.isEmpty())
    			pointLocators.add(Pair.of(chunkIdx, containedPointIndices));
    	}
    	return pointLocators;
    }

	protected void createSelectionCapabilities() {
		Comparator<Pair<Integer, TreeSet<Integer>>> comparator = (o1, o2) -> {
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
		};

		SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPointsOngoing = new SimpleSelectionModel<>(comparator);
		SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPoints = new SimpleSelectionModel<>(comparator);
		Shape[] selectionMemory = {null,null};

		createRectangularPointSetSelectionCapabilities(selectedPointsOngoing, selectedPoints, selectionMemory);
		createRopeSelectionCapabilities(selectedPointsOngoing, selectedPoints, selectionMemory);

		selectedPointsOngoing.addSelectionListener(s->{
			ArrayList<Pair<Integer, TreeSet<Integer>>> list = new ArrayList<>(s);
			notifyPointSetSelectionChangeOngoing(list, selectionMemory[0]);
		});
		selectedPoints.addSelectionListener(s->{
			ArrayList<Pair<Integer, TreeSet<Integer>>> list = new ArrayList<>(s);
			notifyPointSetSelectionChange(list, selectionMemory[1]);
		});
	}

    protected void createRectangularPointSetSelectionCapabilities(SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPointsOngoing,
																  SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPoints, Shape[] selectionMemory) {
    	KeyMaskListener keyMask = new KeyMaskListener(KeyEvent.VK_S);
    	CoordSysViewSelector selector = new CoordSysViewSelector(this.canvas, this.coordsys, keyMask) {
    		@Override
    		public void areaSelectedOnGoing(double minX, double minY, double maxX, double maxY) {
    			if(pointSetSelectionOngoingListeners.isEmpty())
    				return;

    			Rectangle2D area = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
				selectionMemory[0] = area;
    			selectedPointsOngoing.setSelection(getIndicesOfPointsInArea(area));
    		}
    		
    		@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
    			if(pointSetSelectionListeners.isEmpty())
    				return;

    			Rectangle2D area = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
				selectionMemory[1] = area;
    			selectedPoints.setSelection(getIndicesOfPointsInArea(area));
			}
		};
		selector.register();
    }

	protected void createRopeSelectionCapabilities(SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPointsOngoing,
												   SimpleSelectionModel<Pair<Integer, TreeSet<Integer>>> selectedPoints, Shape[] selectionMemory) {
		CoordSysRopeSelection selector = new CoordSysRopeSelection(this.canvas, this.coordsys) {
			@Override
			public void areaSelected(Path2D selectedArea) {
				if(pointSetSelectionListeners.isEmpty())
					return;
				selectionMemory[1] = selectedArea;
				selectedPoints.setSelection(getIndicesOfPointsInArea(selectedArea));
			}

			@Override
			public void areaSelectedOnGoing(Path2D selectedArea) {
				if(pointSetSelectionOngoingListeners.isEmpty())
					return;
				selectionMemory[0] = selectedArea;
				selectedPointsOngoing.setSelection(getIndicesOfPointsInArea(selectedArea));
			}
		};
		selector.register();
	}

	/**
	 * The PointSetSelectionListener interface is responsible for notifying of point set changes.
	 */
    public static interface PointSetSelectionListener {
    	/**
    	 * This method is called whenever the selection of points has changed.
    	 *
    	 * @param selectedPoints a list of pairs which map chunk indices to the set of points which have been selected of the corresponding chunk
    	 * @param selectionArea the area of the selection
    	 */
    	public void onPointSetSelectionChanged(ArrayList<Pair<Integer, TreeSet<Integer>>> selectedPoints, Shape selectionArea);
    }
    
    protected synchronized void notifyPointSetSelectionChangeOngoing(ArrayList<Pair<Integer, TreeSet<Integer>>> list, Shape rect) {
    	for(PointSetSelectionListener l:pointSetSelectionOngoingListeners) {
			l.onPointSetSelectionChanged(list, rect);
		}
    }
    
    protected synchronized void notifyPointSetSelectionChange(ArrayList<Pair<Integer, TreeSet<Integer>>> list, Shape rect) {
    	for(PointSetSelectionListener l:pointSetSelectionListeners) {
			l.onPointSetSelectionChanged(list, rect);
		}
    }

	/**
	 * Adds a new {@link PointSetSelectionListener} to the ScatterPlot.
	 * The added listener will be triggered, when the selection is done.
	 *
	 * @param l {@link PointSetSelectionListener} to add
	 */
	public synchronized void addPointSetSelectionListener(PointSetSelectionListener l) {
    	this.pointSetSelectionListeners.add(l);
    }

	/**
	 * Adds a new {@link PointSetSelectionListener} to the ScatterPlot.
	 * The added listener will be triggered, while the selection is ongoing.
	 *
	 * @param l {@link PointSetSelectionListener} to add
	 */
	public synchronized void addPointSetSelectionOngoingListener(PointSetSelectionListener l) {
    	this.pointSetSelectionOngoingListeners.add(l);
    }

	/**
	 *
	 * @param chunkIdx specifies which {@link Points} object should be returned
	 * @return {@link Points} object connected to the chunkIdx
	 */
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

	/**
	 * @see ScatterPlot#accentuate(java.lang.Iterable)
	 */
	@SafeVarargs
	public final void accentuate(Pair<Integer, Integer> ... toAccentuate) {
		accentuate(Arrays.asList(toAccentuate));
	}

	/**
	 * "Accentuates" all the Points which match the input parameters.
	 * The accentuation effect adds an outline to the specified point(s).
	 *
	 * @param toAccentuate pair of chunkIdx and pointIdx defining the points to accentuate
	 */
	public void accentuate(Iterable<Pair<Integer, Integer>> toAccentuate) {
		SimpleSelectionModel<Pair<Integer,Integer>> selectionModel = this.cueSelectionModels.get(CUE_ACCENTUATE);
		selectionModel.setSelection(toAccentuate);
	}

	/**
	 * @see ScatterPlot#emphasize(java.lang.Iterable)
	 */
	@SafeVarargs
	public final void emphasize(Pair<Integer, Integer> ... toEmphasize) {
		emphasize(Arrays.asList(toEmphasize));
	}

	/**
	 * "Emphasizes" all the Points which match the input parameters.
	 * The emphasizing effect enlarges the specified point(s).
	 *
	 * @param toEmphasize pair of chunkIdx and pointIdx defining the points to emphasize
	 */
	public void emphasize(Iterable<Pair<Integer, Integer>> toEmphasize) {
		SimpleSelectionModel<Pair<Integer,Integer>> selectionModel = this.cueSelectionModels.get(CUE_EMPHASIZE);
		selectionModel.setSelection(toEmphasize);
	}

	/**
	 * @see ScatterPlot#highlight(java.lang.Iterable)
	 */
	@SafeVarargs
	public final void highlight(Pair<Integer, Integer> ... toHighlight) {
		highlight(Arrays.asList(toHighlight));
	}

	/**
	 * "Highlights" all the Points which match the input parameters.
	 * The highlighting effect greys out all other points other than the specified point(s).
	 *
	 * @param toHighlight pair of chunkIdx and pointIdx defining the points to highlight
	 */
	public void highlight(Iterable<Pair<Integer, Integer>> toHighlight) {
		SimpleSelectionModel<Pair<Integer,Integer>> selectionModel = this.cueSelectionModels.get(CUE_HIGHLIGHT);
		selectionModel.setSelection(toHighlight);
	}
	
	protected SortedSet<Pair<Integer, Integer>> sanitizeCueSet(SortedSet<Pair<Integer, Integer>> cueset) {
		return cueset.stream()
		.filter(p->p.first >= 0 && p.second >= 0 && p.first < dataModel.numChunks())
		.filter(p->p.second < dataModel.chunkSize(p.first))
		.collect(Collectors.toCollection(TreeSet::new));
	}
	
	protected void createCue(final String cueType) {
		SimpleSelectionModel<Pair<Integer, Integer>> selectionModel = this.cueSelectionModels.get(cueType);
		SortedSet<Pair<Integer, Integer>> instancesToCue = sanitizeCueSet(selectionModel.getSelection());
		
		clearCue(cueType);
		
		switch (cueType) {
		case CUE_ACCENTUATE:
		{
			// accentuation: show point in top layer with outline
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
			// emphasis: show enlarged point in top layer
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
	
	// TODO: convenience methods to create listeners for cues (e.g. hovering does highlighting)
    
    
}