package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.ParallelCoordsRenderer;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.PickingRegistry;
import hageldave.jplotter.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;

/**
 * The ParallelCoords class provides an easy way to create a parallel coordinates chart.
 * Therefore, it abstracts some concepts that JPlotter provides (JPlotterCanvas, Renderers, ...) and sets them up automatically.
 * To edit those, they can be returned with their respective getter methods.
 * <p>
 * It consists of a {@link ParallelCoordsRenderer} and multiple content layers.
 * It also has a data model (see {@link ParallelCoordsDataModel}) and a listener (see {@link ParallelCoordsDataModel.ParallelCoordsDataModelListener})
 * linked to it, listening for data changes.
 * There is also a mouse event handler created in the constructor,
 * which can be used to listen the events of the {@link ParallelCoordsMouseEventListener}.
 * <p>
 * There are 2 line connection modes (see {@link ParallelCoords#connectionMode}), which define how the chart is drawn.
 */
public class ParallelCoords {
    protected JPlotterCanvas canvas;
    protected ParallelCoordsRenderer parallelCoordsys;
    protected CompleteRenderer contentLayer0;
    protected CompleteRenderer contentLayer1;
    protected CompleteRenderer contentLayer2;
    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();
    final protected ParallelCoordsDataModel dataModel = new ParallelCoordsDataModel();
    final protected ArrayList<Lines> linesPerDataChunk = new ArrayList<>();
    final protected ArrayList<Curves> curvesPerDataChunk = new ArrayList<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected Legend legend = new Legend();
    protected ParallelCoordsVisualMapping visualMapping = new ParallelCoordsVisualMapping() {
    };
    private int legendRightWidth = 100;
    private int legendBottomHeight = 60;
    final protected LinkedList<ParallelCoordsMouseEventListener> mouseEventListeners = new LinkedList<>();

    protected static final String CUE_HIGHLIGHT = "HIGHLIGHT";
    protected static final String CUE_ACCENTUATE = "ACCENTUATE";
    protected static final String CUE_EMPHASIZE = "EMPHASIZE";

    protected HashMap<String, HashMap<Pair<Color, Integer>, Lines>> cp2linesMaps = new HashMap<>();
    protected HashMap<String, HashMap<Pair<Color, Integer>, Curves>> cp2curvesMaps = new HashMap<>();
    protected HashMap<String, SimpleSelectionModel<Pair<Integer, Integer>>> cueSelectionModels = new HashMap<>();

    protected boolean axisHighlighting = false;

    public final static int LINES=1;
    public final static int CURVES=2;
    protected int connectionMode = LINES | CURVES;
    protected double bezierCPdist = 0.35;

    public ParallelCoords(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback());
    }

    public ParallelCoords(final boolean useOpenGL, final int connectionMode) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback());
        this.setConnectionMode(connectionMode);
    }

    /**
     * Creates a standard instance of a ParallelCoords chart.
     * The constructor sets up the content layers, data model and more.
     * It uses the {@link #LINES} connection mode by default.
     *
     * @param canvas the content of the ParallelCoords chart will be drawn here
     */
    public ParallelCoords(final JPlotterCanvas canvas) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.parallelCoordsys = new ParallelCoordsRenderer();
        this.contentLayer0 = new CompleteRenderer();
        this.contentLayer1 = new CompleteRenderer();
        this.contentLayer2 = new CompleteRenderer();
        this.parallelCoordsys.setContent(contentLayer0.withAppended(contentLayer1).withAppended(contentLayer2));
        this.canvas.setRenderer(parallelCoordsys);
        this.setConnectionMode(LINES);

        this.dataModel.addListener(new ParallelCoordsDataModel.ParallelCoordsDataModelListener() {
            @Override
            public void featureOrderChanged() {
                onFeatureOrderChanged();
            }

            @Override
            public void featureAdded(ParallelCoordsRenderer.Feature feature) {
                onFeatureAdded(feature);
            }

            @Override
            public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription) {
                onDataAdded(chunkIdx, chunkData, chunkDescription);
            }

            @Override
            public void dataChanged(int chunkIdx, double[][] chunkData) {
                onDataChanged(chunkIdx, chunkData);
            }
        });

        createMouseEventHandler();

        for (String cueType : Arrays.asList(CUE_EMPHASIZE, CUE_ACCENTUATE, CUE_HIGHLIGHT)) {
            this.cp2linesMaps.put(cueType, new HashMap<>());
            this.cp2curvesMaps.put(cueType, new HashMap<>());
            this.cueSelectionModels.put(cueType, new SimpleSelectionModel<>());
            this.cueSelectionModels.get(cueType).addSelectionListener(selection -> createCue(cueType));
        }
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

    /**
     * @return the visual mapping {@link ParallelCoordsVisualMapping} of the ParallelCoords chart
     */
    public ParallelCoordsVisualMapping getVisualMapping() {
        return visualMapping;
    }

    /**
     * Sets a new {@link ParallelCoordsVisualMapping}.
     *
     * @param visualMapping to be set
     */
    public void setVisualMapping(ParallelCoordsVisualMapping visualMapping) {
        this.visualMapping = visualMapping;
        for (Lines p : linesPerDataChunk)
            p.setDirty();
        this.canvas.scheduleRepaint();
    }

    /**
     * @return the data model (see {@link ParallelCoordsVisualMapping}) linked to the ParallelCoords chart
     */
    public ParallelCoordsDataModel getDataModel() {
        return dataModel;
    }

    /**
     * @return the underlying canvas on which the ParallelCoords chart will be rendered on
     */
    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    /**
     * @return the underlying (see {@link ParallelCoordsRenderer}) on which the ParallelCoords chart items will be placed on
     */
    public ParallelCoordsRenderer getCoordsys() {
        return parallelCoordsys;
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

    protected synchronized void onFeatureAdded(ParallelCoordsRenderer.Feature feature) {
        this.parallelCoordsys.addFeature(feature);

        // repaint all the line stuff
        for (int i = 0; i < getDataModel().dataChunks.size(); i++) {
            getDataModel().notifyDataChanged(i);
        }

        this.canvas.scheduleRepaint();
    }

    protected synchronized void onFeatureOrderChanged() {
        this.parallelCoordsys.getFeatures().clear();

        for (int i = 0; i < getDataModel().axesMap.size(); i++) {
            this.parallelCoordsys.addFeature(getDataModel().getFeature(getDataModel().axesMap.get(i)));
        }

        // repaint all the line stuff
        for (int i = 0; i < getDataModel().dataChunks.size(); i++) {
            getDataModel().notifyDataChanged(i);
        }
        this.canvas.scheduleRepaint();
    }

    protected synchronized void onDataAdded(int chunkIdx, double[][] dataChunk, String chunkDescription) {
        this.parallelCoordsys.getFeatures().clear();

        for (Integer value : getDataModel().axesMap)
            this.parallelCoordsys.addFeature(getDataModel().feature2dataIndex.get(value));

        // create line object
        Lines lines = new Lines();
        linesPerDataChunk.add(lines);
        getContent().addItemToRender(lines);

        // create curve object
        Curves curves = new Curves();
        curvesPerDataChunk.add(curves);
        getContent().addItemToRender(curves);

        for (int i = 0; i < dataChunk.length; i++) {
            double[] datapoint = dataChunk[i];
            int pickColor = registerInPickingRegistry(new int[]{chunkIdx, i});
            int numberAxes = getDataModel().axesMap.size() - 1;

            for (int j = 0; j < numberAxes; j++) {
                int firstIndex = getDataModel().axesMap.get(j);
                int secondIndex = getDataModel().axesMap.get(j + 1);

                Point2D.Double firstCoord = new Point2D.Double((double) j / numberAxes, normalizeValue(datapoint[firstIndex], getDataModel().feature2dataIndex.get(firstIndex)));
                Point2D.Double secCoord = new Point2D.Double((double) (j + 1) / numberAxes, normalizeValue(datapoint[secondIndex], getDataModel().feature2dataIndex.get(secondIndex)));

                // set lines
                Lines.SegmentDetails segmentDetails = lines.addSegment(firstCoord, secCoord);
                segmentDetails.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                segmentDetails.setPickColor(pickColor);

                // set curves
                double diff = secCoord.x - firstCoord.x;
                ArrayList<Curves.CurveDetails> curveDetails = curves.addCurveStrip(firstCoord, new Point2D.Double(firstCoord.x + diff*bezierCPdist, firstCoord.y), new Point2D.Double(secCoord.x - diff*bezierCPdist, secCoord.y), secCoord);
                for (Curves.CurveDetails det : curveDetails) {
                    det.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                    det.setPickColor(pickColor);
                }
            }
        }
        // create a picking ID for use in legend for this data chunk
        this.legendElementPickIds.add(registerInPickingRegistry(chunkIdx));
        visualMapping.createLegendElementForChunk(legend, chunkIdx, chunkDescription, legendElementPickIds.get(chunkIdx));
        this.canvas.scheduleRepaint();
    }

    protected synchronized void onDataChanged(int chunkIdx, double[][] dataChunk) {
        this.parallelCoordsys.getFeatures().clear();

        for (Integer value : getDataModel().axesMap)
            this.parallelCoordsys.addFeature(getDataModel().feature2dataIndex.get(value));

        // handle lines
        Lines lines = linesPerDataChunk.get(chunkIdx);
        for (Lines.SegmentDetails ld : lines.getSegments()) {
            int pickId = ld.pickColor;
            if (pickId != 0) {
                deregisterFromPickingRegistry(pickId);
            }
        }
        lines.removeAllSegments();

        // handle curves
        Curves curves = curvesPerDataChunk.get(chunkIdx);
        for (Curves.CurveDetails cd : curves.getCurveDetails()) {
            int pickId = cd.pickColor;
            if (pickId != 0) {
                deregisterFromPickingRegistry(pickId);
            }
        }
        curves.removeAllCurves();

        for (int i = 0; i < dataChunk.length; i++) {
            double[] datapoint = dataChunk[i];
            int pickColor = registerInPickingRegistry(new int[]{chunkIdx, i});
            int numberAxes = getDataModel().axesMap.size() - 1;

            for (int j = 0; j < numberAxes; j++) {
                int firstIndex = getDataModel().axesMap.get(j);
                int secondIndex = getDataModel().axesMap.get(j + 1);

                if (firstIndex >= datapoint.length || secondIndex >= datapoint.length) {
                    throw new RuntimeException("Data index is larger than dataset.");
                }

                Point2D.Double firstCoord = new Point2D.Double((double) j / numberAxes, normalizeValue(datapoint[firstIndex], getDataModel().feature2dataIndex.get(firstIndex)));
                Point2D.Double secCoord = new Point2D.Double((double) (j + 1) / numberAxes, normalizeValue(datapoint[secondIndex], getDataModel().feature2dataIndex.get(secondIndex)));

                // set lines
                Lines.SegmentDetails segmentDetails = lines.addSegment(firstCoord, secCoord);
                segmentDetails.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                segmentDetails.setPickColor(pickColor);

                // set curves
                double diff = secCoord.x - firstCoord.x;
                ArrayList<Curves.CurveDetails> curveDetails = curves.addCurveStrip(firstCoord, new Point2D.Double(firstCoord.x + diff*bezierCPdist, firstCoord.y), new Point2D.Double(secCoord.x - diff*bezierCPdist, secCoord.y), secCoord);
                for (Curves.CurveDetails det : curveDetails) {
                    det.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                    det.setPickColor(pickColor);
                }
            }
        }
        this.canvas.scheduleRepaint();
    }

    protected static double normalizeValue(double value, ParallelCoordsRenderer.Feature feature) {
        return (value - feature.bottom) / (feature.top - feature.bottom);
    }

    protected static double denormalizeValue(double value, ParallelCoordsRenderer.Feature feature) {
        double diff = feature.top - feature.bottom;
        return diff * value + feature.bottom;
    }

    /**
     * The ParallelCoordsDataModel is the inner data model of the ParallelCoords chart.
     *
     * It consists of multiple so-called dataChunks,
     * which are 2D double arrays holding the data that is used to render the lines/curves in the ParallelCoords chart.
     *
     * To access any data point later again (to highlight it for example [see {@link ParallelCoords#highlight(java.lang.Iterable)}]), we differentiate the term chunkIdx and segmentIdx.
     * The chunkIdx is the index of the data chunk in the array, where all the added data chunks are saved (so the first added data chunk has chunkIdx 0, the second then chunkIdx 1, ...).
     * The segmentIdx then is the index inside the data chunk, as expected.
     *
     * The data model consists also of listeners (see {@link ParallelCoordsDataModelListener}),
     * which methods are called if data is added or changed. The listeners methods then cause a repaint of the ParallelCoords chart (including legend, etc.),
     * with the new or changed data.
     *
     */
    public static class ParallelCoordsDataModel {
        protected ArrayList<double[][]> dataChunks = new ArrayList<>();
        protected ArrayList<ParallelCoordsRenderer.Feature> features = new ArrayList<>();
        protected HashMap<Integer, ParallelCoordsRenderer.Feature> feature2dataIndex = new HashMap<>();

        protected ArrayList<String> descriptionPerChunk = new ArrayList<>();
        protected LinkedList<ParallelCoordsDataModelListener> listeners = new LinkedList<>();

        protected List<Integer> axesMap = new ArrayList<>();

        /**
         * The ParallelCoordsDataModelListener consists of multiple listener interfaces, which are called when the data model is manipulated.
         */
        public static interface ParallelCoordsDataModelListener {
            /**
             * Whenever the feature axis order is changed, this interface will be called.
             */
            public void featureOrderChanged();

            /**
             * Whenever a new feature axis is added to the ParallelCoords chart, this interface will be called.
             *
             * @param feature the feature axis to be added to the chart
             */
            public void featureAdded(ParallelCoordsRenderer.Feature feature);

            /**
             * Whenever data is added to the ParallelCoords chart, this interface will be called.
             *
             * @param chunkIdx index of the dataChunk in the array where all dataChunks are stored
             * @param chunkData 2D array containing the data that will be rendered in the ParallelCoords chart
             * @param chunkDescription label of the data chunk which will also be shown in the legend
             */
            public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription);

            /**
             * Whenever data is updated in the ParallelCoords chart, this interface will be called.
             *
             * @param chunkIdx index of the data chunk that should be updated
             * @param chunkData 2D array containing the updated data
             */
            public void dataChanged(int chunkIdx, double[][] chunkData);
        }

        /**
         * Adds a feature axis (see {@link ParallelCoordsRenderer.Feature}) to the ParallelCoordinates chart.
         *
         * The feature axis is mapped to an index which enables the possibility to reorder
         * the axes later with the {@link #setDataIndicesOrder} method.
         *
         * @param dataIndex index of the feature
         * @param feature the feature to be added
         * @return this for chaining
         */
        public synchronized ParallelCoordsDataModel addFeature(int dataIndex, ParallelCoordsRenderer.Feature feature) {
            features.add(feature);
            axesMap.add(dataIndex);
            feature2dataIndex.put(dataIndex, feature);
            notifyFeatureAdded(feature);
            return this;
        }

        /**
         * Adds multiple feature axes (see {@link ParallelCoordsRenderer.Feature}) to the ParallelCoordinates chart.
         *
         * Each feature axis is mapped to an index which enables the possibility to reorder
         * the axes later with the {@link #setDataIndicesOrder} method.
         *
         * @param dataIndices indices of the features
         * @param features the features to be added
         * @return this for chaining
         */
        public synchronized ParallelCoordsDataModel addFeature(int[] dataIndices, ParallelCoordsRenderer.Feature[] features) {
            if (dataIndices.length != features.length) {
                throw new IllegalArgumentException("Arrays have to be of equal length");
            }
            for (int i = 0; i < dataIndices.length; i++) {
                addFeature(dataIndices[i], features[i]);
            }
            return this;
        }

        /**
         * Adds data to the data model of the ParallelCoords chart.
         *
         * @param dataChunk 2D array containing the data that will be rendered in the ParallelCoords chart
         * @param chunkDescription description of the chunk which will also be shown in the legend
         */
        public synchronized void addData(double[][] dataChunk, String chunkDescription) {
            int chunkIdx = this.dataChunks.size();
            this.dataChunks.add(dataChunk);
            this.descriptionPerChunk.add(chunkDescription);
            notifyDataAdded(chunkIdx);
        }

        /**
         * Sets the order of the feature axes.
         *
         * Each axis will be displayed at the position specified in the array.
         * E.g. [0, 2, 1] means, that the feature added 1st -> displayed at the 1st position
         *                                              2nd -> displayed at the 3rd position
         *                                              3rd -> displayed at the 2nd position
         *
         * @param indicesOrder array containing the indices of the feature axes
         */
        public synchronized void setDataIndicesOrder(int[] indicesOrder) {
            axesMap.clear();
            for (int i = 0; i < indicesOrder.length; i++) {
                axesMap.add(indicesOrder[i]);
            }
            notifyFeatureOrderChanged();
        }

        /**
         * @return a list containing the indices of the axes which are displayed in their list order
         */
        public List<Integer> getAxisOrder() {
            return axesMap;
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
        public double[][] getDataChunk(int chunkIdx) {
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
         * Updates the dataChunk with the given chunkIdx.
         * This will trigger the {@link ParallelCoordsDataModelListener#dataChanged(int, double[][])} method of the {@link ParallelCoordsDataModelListener}.
         *
         * @param chunkIdx id of the dataChunk to update
         * @param dataChunk the new dataChunk
         */
        public synchronized void setDataChunk(int chunkIdx, double[][] dataChunk) {
            if (chunkIdx >= numChunks())
                throw new ArrayIndexOutOfBoundsException("specified chunkIdx out of bounds: " + chunkIdx);
            this.dataChunks.set(chunkIdx, dataChunk);
            this.notifyDataChanged(chunkIdx);
        }

        /**
         * @return the number of feature axes in the chart
         */
        public int getFeatureCount() {
            return features.size();
        }

        /**
         * @return all {@link ParallelCoordsRenderer.Feature} objects stored in the data model
         */
        public ArrayList<ParallelCoordsRenderer.Feature> getFeatures() {
            return this.features;
        }

        /**
         * Returns the {@link ParallelCoordsRenderer.Feature} at the given index.
         *
         * @param index of the feature
         * @return feature at the given index
         */
        public ParallelCoordsRenderer.Feature getFeature(int index) {
            return this.features.get(index);
        }

        /**
         * Returns the chunk description of the corresponding chunkIdx.
         *
         * @param chunkIdx specifies which chunk's description should be returned
         * @return chunk description
         */
        public String getChunkDescription(int chunkIdx) {
            return descriptionPerChunk.get(chunkIdx);
        }

        /**
         * Returns all data indices of the specified data chunk (chunkIdx) that contain values
         * between a min/max on a feature axis.
         *
         * @param chunkIdx elements of this chunk will be searched
         * @param featureIndex defines which values will be searched
         * @param min lower bound of values to be returned
         * @param max upper bound of values to be returned
         * @return all data indices with values between min/max on the specified feature axis
         */
        public TreeSet<Integer> getIndicesOfSegmentsInRange(int chunkIdx, int featureIndex, double min, double max) {
            // naive search for contained points
            // TODO: quadtree supported search (quadtrees per chunk have to be kept up to date)
            double[][] data = getDataChunk(chunkIdx);
            TreeSet<Integer> containedPointIndices = new TreeSet<>();
            for (int i = 0; i < data.length; i++) {
                if (min <= data[i][axesMap.get(featureIndex)] && data[i][axesMap.get(featureIndex)] <= max)
                    containedPointIndices.add(i);
            }
            return containedPointIndices;
        }

        /**
         * Calculates the global index of idx if all values of the chunks
         * are viewed as one sequence.
         *
         * @param chunkIdx marks the starting point of the globalIndex before idx
         * @param idx will be added to the globalIndex after the sizes of all chunks before chunkIdx
         * @return the global index of idx
         */
        public int getGlobalIndex(int chunkIdx, int idx) {
            int globalIdx = 0;
            for (int i = 0; i < chunkIdx; i++) {
                globalIdx += chunkSize(i);
            }
            return globalIdx + idx;
        }

        /**
         * Calculate the chunkIdx of the chunk which contains the globalIdx
         * if all values of the chunks are viewed as one sequence.
         *
         * @param globalIdx data index that's chunk should be retrieved
         * @return Pair of chunkIdx and globalIdx
         */
        public Pair<Integer, Integer> locateGlobalIndex(int globalIdx) {
            int chunkIdx = 0;
            while (globalIdx >= chunkSize(chunkIdx)) {
                globalIdx -= chunkSize(chunkIdx);
                chunkIdx++;
            }
            return Pair.of(chunkIdx, globalIdx);
        }

        /**
         * @return how many values are stored in the data model
         */
        public int numDataPoints() {
            int n = 0;
            for (int i = 0; i < numChunks(); i++)
                n += chunkSize(i);
            return n;
        }

        /**
         * Adds a {@link ParallelCoordsDataModelListener} to the data model.
         *
         * @param l listener to be added
         * @return this for chaining
         */
        public synchronized ParallelCoordsDataModelListener addListener(ParallelCoordsDataModelListener l) {
            listeners.add(l);
            return l;
        }

        /**
         * Removes a {@link ParallelCoordsDataModelListener} from the data model.
         *
         * @param l listener to be removed
         */
        public synchronized void removeListener(ParallelCoordsDataModelListener l) {
            listeners.remove(l);
        }

        /**
         * Calls the {@link ParallelCoordsDataModelListener#featureOrderChanged()} interface of all registered {@link ParallelCoordsDataModelListener}.
         */
        public synchronized void notifyFeatureOrderChanged() {
            for (ParallelCoordsDataModelListener l : listeners)
                l.featureOrderChanged();
        }

        /**
         * Calls the {@link ParallelCoordsDataModelListener#featureAdded(ParallelCoordsRenderer.Feature)} interface of all registered {@link ParallelCoordsDataModelListener}.
         *
         * @param feature which has been added
         */
        public synchronized void notifyFeatureAdded(ParallelCoordsRenderer.Feature feature) {
            for (ParallelCoordsDataModelListener l : listeners)
                l.featureAdded(feature);
        }

        /**
         * Calls the {@link ParallelCoordsDataModelListener#dataAdded(int, double[][], String)} interface of all registered {@link ParallelCoordsDataModelListener}.
         *
         * @param chunkIdx data chunk id of the added data
         */
        public synchronized void notifyDataAdded(int chunkIdx) {
            for (ParallelCoordsDataModelListener l : listeners)
                l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx));
        }

        /**
         * Calls the {@link ParallelCoordsDataModelListener#dataChanged(int, double[][])} interface of all registered {@link ParallelCoordsDataModelListener}.
         *
         * @param chunkIdx data chunk id of the changed data
         */
        public synchronized void notifyDataChanged(int chunkIdx) {
            for (ParallelCoordsDataModelListener l : listeners)
                l.dataChanged(chunkIdx, getDataChunk(chunkIdx));
        }
    }

    /**
     * The ParallelCoordsVisualMapping is responsible for mapping the chunks to a color.
     *
     */
    public static interface ParallelCoordsVisualMapping {
        /**
         * This method returns a color to the given chunkIdx.
         *
         * As there is only a limited number of colors in the color map (see {@link DefaultColorMap}),
         * they are repeated in the same order, if all of them have been used.
         *
         * @param chunkIdx id of the data chunk
         * @return color in an integer packed ARGB format
         */
        public default int getColorForChunk(int chunkIdx) {
            DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
            return colorMap.getColor(chunkIdx % colorMap.numColors());
        }

        /**
         * Adds a line label element to the legend of the ParallelCoords for a data chunk, using the {@link ParallelCoordsVisualMapping#getColorForChunk(int)} method.
         *
         * @param legend where the new element will be added
         * @param chunkIdx id of the data chunk
         * @param chunkDescr description of the data chunk
         * @param pickColor pick color of the legend element
         */
        public default void createLegendElementForChunk(Legend legend, int chunkIdx, String chunkDescr, int pickColor) {
            int color = getColorForChunk(chunkIdx);
            legend.addLineLabel(1, color, chunkDescr, pickColor);
        }

        public default void createGeneralLegendElements(Legend legend) {
        }

        ;
    }

    /**
     * Sets the legend on the right of the ParallelCoords graph.
     * This replaces the legend on the bottom, if it was set before.
     */
    public void placeLegendOnRight() {
        if (parallelCoordsys.getLegendBottom() == legend) {
            parallelCoordsys.setLegendBottom(null);
            parallelCoordsys.setLegendBottomHeight(0);
        }
        parallelCoordsys.setLegendRight(legend);
        parallelCoordsys.setLegendRightWidth(this.legendRightWidth);
    }

    /**
     * Sets the legend on the bottom of the ParallelCoords graph.
     * This replaces the legend on the right, if it was set before.
     */
    public void placeLegendOnBottom() {
        if (parallelCoordsys.getLegendRight() == legend) {
            parallelCoordsys.setLegendRight(null);
            parallelCoordsys.setLegendRightWidth(0);
        }
        parallelCoordsys.setLegendBottom(legend);
        parallelCoordsys.setLegendBottomHeight(this.legendBottomHeight);
    }

    /**
     * Removes all legends of the ParallelCoords graph.
     */
    public void placeLegendNowhere() {
        if (parallelCoordsys.getLegendRight() == legend) {
            parallelCoordsys.setLegendRight(null);
            parallelCoordsys.setLegendRightWidth(0);
        }
        if (parallelCoordsys.getLegendBottom() == legend) {
            parallelCoordsys.setLegendBottom(null);
            parallelCoordsys.setLegendBottomHeight(0);
        }
    }

    /**
     * @return the currently active connection mode,
     * which defines if the datapoints are connected by lines or curves.
     */
    public int getConnectionMode() {
        return connectionMode;
    }

    /**
     * Defines if the datapoints are connected by lines (=1) or curves (=2)
     * (see {@link #LINES} and {@link #CURVES}).
     *
     * To switch between connection modes, the corresponding renderers are enabled/disabled.
     *
     * @param connectionMode defines how datapoints should be connected
     */
    public void setConnectionMode(int connectionMode) {
        this.connectionMode = connectionMode;
        if (connectionMode == LINES) {
            this.getContentLayer0().curves.setEnabled(false);
            this.getContentLayer0().lines.setEnabled(true);
            this.getContentLayer1().curves.setEnabled(false);
            this.getContentLayer1().lines.setEnabled(true);
            this.getContentLayer2().curves.setEnabled(false);
            this.getContentLayer2().lines.setEnabled(true);
        } else if (connectionMode == CURVES) {
            this.getContentLayer0().lines.setEnabled(false);
            this.getContentLayer0().curves.setEnabled(true);
            this.getContentLayer1().lines.setEnabled(false);
            this.getContentLayer1().curves.setEnabled(true);
            this.getContentLayer2().lines.setEnabled(false);
            this.getContentLayer2().curves.setEnabled(true);
        }
    }

    /**
     * Enables/Disables the axis highlighting feature of the ParallelCoords graph.
     * The ParallelCoords class contains multiple interaction interfaces that react to user input.
     * One of these interfaces reacts to the event when the mouse is dragged on/over an axis.
     * There's also a visual indicator which area is currently selected.
     * If this behaviour is undesired, it can be deactivated.
     *
     * @param toHighlight controls if the feature should be enabled or disabled
     */
    public void setAxisHighlighting(boolean toHighlight) {
        this.axisHighlighting = toHighlight;
    }

    protected void createMouseEventHandler() {
        MouseAdapter mouseEventHandler = new MouseAdapter() {
            double yPos = 0.0;
            double currentValue = 0.0;
            boolean isMouseDragged = false;
            boolean isDragRegistered = false;
            int featureIndex = -1;

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_MOVED, e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_CLICKED, e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_PRESSED, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_RELEASED, e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_DRAGGED, e);
            }


            private void mouseAction(String eventType, MouseEvent e) {
                /* TODO: check key mask listeners of panning, zooming, and rectangular point selection
                 * to figure out if the mouse event is being handled by them. If not handled by any of them
                 * then go on with the following.
                 */
                Point2D coordsysPoint = parallelCoordsys.transformAWT2CoordSys(e.getPoint(), canvas.asComponent().getHeight());

                if (Utils.swapYAxis(parallelCoordsys.getCoordSysArea(), canvas.asComponent().getHeight()).contains(e.getPoint())) {
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);

                    if ((pixel & 0x00ffffff) == 0) {
                        notifyInsideMouseEventNone(eventType, e, coordsysPoint);
                    } else {
                        Object segmentLocalizer = pickingRegistry.lookup(pixel);

                        if (segmentLocalizer instanceof int[]) {
                            int chunkIdx = ((int[]) segmentLocalizer)[0];
                            int segmentIdx = ((int[]) segmentLocalizer)[1];
                            notifyInsideMouseEventSegment(eventType, e, coordsysPoint, chunkIdx, segmentIdx);
                        }
                    }
                } else {
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
                    if ((pixel & 0x00ffffff) == 0) {
                        notifyOutsideMouseEventeNone(eventType, e);
                    } else {
                        Object miscLocalizer = pickingRegistry.lookup(pixel);
                        if (miscLocalizer instanceof Integer) {
                            int chunkIdx = (int) miscLocalizer;
                            notifyOutsideMouseEventElement(eventType, e, chunkIdx);
                        }
                    }
                }

                // START Axis highlighting //
                if (axisHighlighting) {
                    boolean isEventTypeKeyPressed = eventType.equals(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_PRESSED);
                    boolean isEventTypeDragged = eventType.equals(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_DRAGGED);
                    boolean isEventTypeMoved = eventType.equals(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_MOVED);
                    boolean isEventTypeReleased = eventType.equals(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_RELEASED);

                    double featureProportion = 1.0 / ((dataModel.getFeatureCount() - 1));
                    double modFP = coordsysPoint.getX() % featureProportion;

                    double distToAxis = Math.min(featureProportion - modFP, modFP);
                    double allowedAxisDist = 0.03;

                    if (!isDragRegistered)
                        isDragRegistered = distToAxis < allowedAxisDist && isEventTypeKeyPressed;

                    if (coordsysPoint.getX() % distToAxis < allowedAxisDist && isEventTypeDragged && isDragRegistered) {
                        if (featureIndex == -1)
                            featureIndex = (int) Math.round(coordsysPoint.getX() / (1.0 / (dataModel.getFeatureCount() - 1)));
                        currentValue = denormalizeValue(coordsysPoint.getY(), dataModel.getFeature(featureIndex));

                        if (!isMouseDragged) {
                            yPos = currentValue;
                        } else {
                            double featureMax = Math.max(dataModel.getFeature(featureIndex).bottom, dataModel.getFeature(featureIndex).top);
                            double featureMin = Math.min(dataModel.getFeature(featureIndex).bottom, dataModel.getFeature(featureIndex).top);
                            double selectedMin = Math.max(Math.min(yPos, currentValue), featureMin);
                            double selectedMax = Math.min(Math.max(yPos, currentValue), featureMax);

                            if (selectedMin != selectedMax) {
                                notifyMouseEventOnFeatureAxis(eventType, e, featureIndex, selectedMin, selectedMax);
                            }
                        }
                        isMouseDragged = true;
                    } else if (isEventTypeReleased) {
                        if (featureIndex != -1) {
                            double min = Math.min(yPos, currentValue);
                            double max = Math.max(yPos, currentValue);
                            notifyMouseEventOnFeatureAxis(eventType, e, featureIndex, min, max);
                            highlightFeatureAxis();
                        } else {
                            notifyMouseEventOffFeatureAxis(eventType, e);
                        }
                    } else if (isEventTypeMoved || !isDragRegistered) {
                        notifyMouseEventOffFeatureAxis(eventType, e);
                    }

                    if (isEventTypeMoved || isEventTypeReleased || !isDragRegistered) {
                        isMouseDragged = false;
                        isDragRegistered = false;
                        featureIndex = -1;
                        currentValue = 0.0;
                    }
                }
                // END Axis highlighting //
            }
        };
        this.canvas.asComponent().addMouseListener(mouseEventHandler);
        this.canvas.asComponent().addMouseMotionListener(mouseEventHandler);
    }

    protected synchronized void notifyInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
        for (ParallelCoordsMouseEventListener l : mouseEventListeners)
            l.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
    }

    protected synchronized void notifyInsideMouseEventSegment(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int segmentIdx) {
        for (ParallelCoordsMouseEventListener l : mouseEventListeners)
            l.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, segmentIdx);
    }

    protected synchronized void notifyOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        for (ParallelCoordsMouseEventListener l : mouseEventListeners)
            l.onOutsideMouseEventNone(mouseEventType, e);
    }

    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        for (ParallelCoordsMouseEventListener l : mouseEventListeners)
            l.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
    }

    protected synchronized void notifyMouseEventOnFeatureAxis(String mouseEventType, MouseEvent e, int featureIndex, double min, double max) {
        for (ParallelCoordsMouseEventListener l : mouseEventListeners) {
            l.notifyMouseEventOnFeature(mouseEventType, e, featureIndex, min, max);
        }
        highlightFeatureAxis(featureIndex, min, max);
    }

    protected synchronized void notifyMouseEventOffFeatureAxis(String mouseEventType, MouseEvent e) {
        for (ParallelCoordsMouseEventListener l : mouseEventListeners) {
            l.notifyMouseEventOffFeature(mouseEventType, e);
        }
        highlightFeatureAxis();
    }

    /**
     * The ParallelCoordsMouseEventListener interface contains multiple methods,
     * notifying if an element has been hit or not (inside and outside the graph).
     */
    public static interface ParallelCoordsMouseEventListener {
        static final String MOUSE_EVENT_TYPE_MOVED = "moved";
        static final String MOUSE_EVENT_TYPE_CLICKED = "clicked";
        static final String MOUSE_EVENT_TYPE_PRESSED = "pressed";
        static final String MOUSE_EVENT_TYPE_RELEASED = "released";
        static final String MOUSE_EVENT_TYPE_DRAGGED = "dragged";

        /**
         * Called whenever the mouse pointer doesn't hit a line/curve of the ParallelCoord while being inside the graph.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         * @param coordsysPoint coordinates of the mouse event inside the coordinate system
         */
        public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
        }

        /**
         * Called when the mouse pointer does hit a line/curve of the ScatterPlot.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         * @param coordsysPoint coordinates of the mouse event inside the coordinate system
         * @param chunkIdx id of the data chunk
         * @param segmentIdx id of the data point inside the data chunk
         */
        public default void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int segmentIdx) {
        }

        /**
         * Called when the mouse pointer doesn't hit an element (e.g. legend elements) of the ParallelCoords while being outside the graph.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         */
        public default void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {
        }

        /**
         * Called when the mouse pointer hits an element (e.g. legend elements) of the ParallelCoords while being outside the graph.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         * @param chunkIdx id of the data chunk
         */
        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        }

        /**
         * Called when the mouse pointer is being dragged over a feature axis.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         * @param featureIndex index of the axis that the mouse is dragged over
         * @param min lower bound of the selection
         * @param max upper bound of the selection
         */
        public default void notifyMouseEventOnFeature(String mouseEventType, MouseEvent e, int featureIndex, double min, double max) {
        }

        /**
         * Called when the mouse pointer is not being dragged over a feature axis.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         */
        public default void notifyMouseEventOffFeature(String mouseEventType, MouseEvent e) {
        }
    }

    /**
     * Adds a {@link ParallelCoordsMouseEventListener} to the ParallelCoords chart.
     *
     * @param l {@link ParallelCoordsMouseEventListener} that implements the interface methods which is called whenever one of the defined mouse events happens
     * @return {@link ParallelCoordsMouseEventListener} for chaining
     */
    public synchronized ParallelCoordsMouseEventListener addParallelCoordsMouseEventListener(ParallelCoordsMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    /**
     * Removes the given {@link ParallelCoordsMouseEventListener} from the ScatterPlot.
     *
     * @param l {@link ParallelCoordsMouseEventListener} that should be removed
     * @return true if the {@link ParallelCoordsMouseEventListener} was added to the ParallelCoords chart before
     */
    public synchronized boolean removeParallelCoordsMouseEventListener(ParallelCoordsMouseEventListener l) {
        return this.mouseEventListeners.remove(l);
    }

    /**
     * @param chunkIdx chunkIdx specifies which {@link Lines} object should be returned
     * @return {@link Lines} object connected to the chunkIdx
     */
    public Lines getLinesForChunk(int chunkIdx) {
        return this.linesPerDataChunk.get(chunkIdx);
    }

    /**
     * @param chunkIdx chunkIdx specifies which {@link Curves} object should be returned
     * @return {@link Curves} object connected to the chunkIdx
     */
    public Curves getCurvesForChunk(int chunkIdx) {
        return this.curvesPerDataChunk.get(chunkIdx);
    }

    @SafeVarargs
    public final void accentuate(Pair<Integer, Integer>... toAccentuate) {
        accentuate(Arrays.asList(toAccentuate));
    }

    /**
     * "Accentuates" all the Lines/Curves which match the input parameters.
     * The accentuation effect adds an outline to the specified line(s)/curve(s).
     *
     * @param toAccentuate pair of chunkIdx and pointIdx defining the lines/curves to accentuate
     */
    public void accentuate(Iterable<Pair<Integer, Integer>> toAccentuate) {
        SimpleSelectionModel<Pair<Integer, Integer>> selectionModel = this.cueSelectionModels.get(CUE_ACCENTUATE);
        selectionModel.setSelection(toAccentuate);
    }

    @SafeVarargs
    public final void emphasize(Pair<Integer, Integer>... toEmphasize) {
        emphasize(Arrays.asList(toEmphasize));
    }

    /**
     * "Emphasizes" all the Lines/Curves which match the input parameters.
     * The emphasizing effect enlarges the specified line(s)/curve(s).
     *
     * @param toEmphasize pair of chunkIdx and pointIdx defining the lines/curves to emphasize
     */
    public void emphasize(Iterable<Pair<Integer, Integer>> toEmphasize) {
        SimpleSelectionModel<Pair<Integer, Integer>> selectionModel = this.cueSelectionModels.get(CUE_EMPHASIZE);
        selectionModel.setSelection(toEmphasize);
    }

    @SafeVarargs
    public final void highlight(Pair<Integer, Integer>... toHighlight) {
        highlight(Arrays.asList(toHighlight));
    }

    /**
     * "Highlights" all the Lines/Curves which match the input parameters.
     * The highlighting effect greys out all other points other than the specified line(s)/curve(s).
     *
     * @param toHighlight pair of chunkIdx and pointIdx defining the lines/curves to highlight
     */
    public void highlight(Iterable<Pair<Integer, Integer>> toHighlight) {
        SimpleSelectionModel<Pair<Integer, Integer>> selectionModel = this.cueSelectionModels.get(CUE_HIGHLIGHT);
        selectionModel.setSelection(toHighlight);
    }

    protected void createCue(final String cueType) {
        SimpleSelectionModel<Pair<Integer, Integer>> selectionModel = this.cueSelectionModels.get(cueType);
        SortedSet<Pair<Integer, Integer>> instancesToCue = selectionModel.getSelection();

        clearCue(cueType);
        switch (cueType) {
            case CUE_ACCENTUATE: {
                // emphasis: show point in top layer with outline
                for (Pair<Integer, Integer> instance : instancesToCue) {
                    if (connectionMode == LINES) {
                        // lines here
                        Lines lines = getLinesForChunk(instance.first);
                        Lines.SegmentDetails[] segments = new Lines.SegmentDetails[dataModel.axesMap.size() - 1];

                        for (int i = 0; i < segments.length; i++) {
                            segments[i] = lines.getSegments().get(i + instance.second * segments.length);
                        }

                        for (Lines.SegmentDetails l : segments) {
                            Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                            Color c = new Color(this.parallelCoordsys.getColorScheme().getColor1());
                            front.addSegment(l.p0, l.p1).setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.3f))
                                    .setThickness(l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier() + 0.2 * lines.getGlobalThicknessMultiplier() + 1.5,
                                            l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier() + 0.2 * lines.getGlobalThicknessMultiplier() + 1.5);
                            front.setStrokePattern(lines.getStrokePattern());
                            front.addSegment(l.p0, l.p1).setColor0(l.color0).setColor1(l.color1)
                                    .setThickness(l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier() * 1.05, l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier() * 1.05);
                        }
                    } else {
                        // curves here
                        Curves curves = getCurvesForChunk(instance.first);
                        Curves.CurveDetails[] curveDetails = new Curves.CurveDetails[dataModel.axesMap.size() - 1];

                        for (int i = 0; i < curveDetails.length; i++) {
                            curveDetails[i] = curves.getCurveDetails().get(i + instance.second * curveDetails.length);
                        }

                        Color col = new Color(this.parallelCoordsys.getColorScheme().getColor1());
                        Curves front = getOrCreateCueCurvesForGlyph(cueType, new Color(curves.getCurveDetails().get(0).color.getAsInt()), curves.getStrokePattern());
                        front.setStrokePattern(curves.getStrokePattern());
                        for (Curves.CurveDetails c : curveDetails) {
                            front.addCurve(c.copy())
                                    .setColor(new Color(col.getRed()/255.f, col.getGreen()/255.f, col.getBlue()/255.f, 0.3f))
                                    .setThickness(c.thickness.getAsDouble() * curves.getGlobalThicknessMultiplier() + 0.2 * curves.getGlobalThicknessMultiplier() + 1.5);
                            front.addCurve(c.copy())
                                    .setColor(new Color(curves.getCurveDetails().get(0).color.getAsInt()))
                                    .setThickness(c.thickness.getAsDouble() * curves.getGlobalThicknessMultiplier() * 1.05);
                        }
                    }
                }
            }
            break;
            case CUE_EMPHASIZE: {
                // accentuation: show enlarged point in top layer
                for (Pair<Integer, Integer> instance : instancesToCue) {
                    if (connectionMode == LINES) {
                        Lines lines = getLinesForChunk(instance.first);
                        Lines.SegmentDetails[] segments = new Lines.SegmentDetails[dataModel.axesMap.size() - 1];

                        for (int i = 0; i < segments.length; i++) {
                            segments[i] = lines.getSegments().get(i + instance.second * segments.length);
                        }

                        for (Lines.SegmentDetails l : segments) {
                            Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                            front.setStrokePattern(lines.getStrokePattern());
                            front.addSegment(l.p0, l.p1).setColor0(l.color0).setColor1(l.color1)
                                    .setThickness((l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier()) + 1, (l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier()) + 1);
                        }
                    } else {
                        Curves curves = getCurvesForChunk(instance.first);
                        Curves.CurveDetails[] curveDetails = new Curves.CurveDetails[dataModel.axesMap.size() - 1];

                        for (int i = 0; i < curveDetails.length; i++) {
                            curveDetails[i] = curves.getCurveDetails().get(i + instance.second * curveDetails.length);
                        }

                        Curves front = getOrCreateCueCurvesForGlyph(cueType, new Color(curves.getCurveDetails().get(0).color.getAsInt()), curves.getStrokePattern());
                        front.setStrokePattern(curves.getStrokePattern());
                        for (Curves.CurveDetails c : curveDetails) {
                            front.addCurve(c.copy())
                                    .setColor(new Color(curves.getCurveDetails().get(0).color.getAsInt()))
                                    .setThickness((c.thickness.getAsDouble() * curves.getGlobalThicknessMultiplier()) + 1);
                        }
                    }
                }
            }
            break;
            case CUE_HIGHLIGHT: {
                if (instancesToCue.isEmpty()) {
                    for (int chunk = 0; chunk < getDataModel().numChunks(); chunk++)
                        greyOutChunk(chunk, false);
                } else {
                    for (int chunk = 0; chunk < getDataModel().numChunks(); chunk++)
                        greyOutChunk(chunk, true);

                    for (Pair<Integer, Integer> instance : instancesToCue) {
                        if (connectionMode == LINES) {
                            Lines lines = getLinesForChunk(instance.first);
                            Lines.SegmentDetails[] segments = new Lines.SegmentDetails[dataModel.axesMap.size() - 1];

                            for (int i = 0; i < segments.length; i++) {
                                segments[i] = lines.getSegments().get(i + instance.second * segments.length);
                            }

                            for (Lines.SegmentDetails l : segments) {
                                Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                                front.setStrokePattern(lines.getStrokePattern());

                                front.addSegment(l.p0, l.p1).setColor0(l.color0).setColor1(l.color1)
                                        .setThickness(l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier(), l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier());
                            }
                        } else {
                            // curves stuff
                            Curves curves = getCurvesForChunk(instance.first);
                            Curves.CurveDetails[] curveDetails = new Curves.CurveDetails[dataModel.axesMap.size() - 1];
                            for (int i = 0; i < curveDetails.length; i++) {
                                curveDetails[i] = curves.getCurveDetails().get(i + instance.second * curveDetails.length);
                            }

                            Curves front = getOrCreateCueCurvesForGlyph(cueType, new Color(curves.getCurveDetails().get(0).color.getAsInt()), curves.getStrokePattern());
                            front.setStrokePattern(curves.getStrokePattern());
                            for (Curves.CurveDetails c : curveDetails) {
                                front.addCurve(c.copy())
                                        .setColor(new Color(curves.getCurveDetails().get(0).color.getAsInt()))
                                        .setThickness(c.thickness.getAsDouble() * curves.getGlobalThicknessMultiplier());
                            }
                        }
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
        getLinesForChunk(chunkIdx).setGlobalSaturationMultiplier(factor).setGlobalAlphaMultiplier(factor);
        getCurvesForChunk(chunkIdx).setGlobalSaturationMultiplier(factor).setGlobalAlphaMultiplier(factor);
    }

    private Lines getOrCreateCueLinesForGlyph(String cue, Color c, int strokePattern) {
        HashMap<Pair<Color, Integer>, Lines> cp2lines = this.cp2linesMaps.get(cue);
        if (!cp2lines.containsKey(new Pair<>(c, strokePattern))) {
            Lines lines = new Lines();
            cp2lines.put(new Pair<>(c, strokePattern), lines);

            switch (cue) {
                case CUE_ACCENTUATE: // fallthrough
                case CUE_EMPHASIZE: {
                    lines.setGlobalThicknessMultiplier(1.2);
                    getContentLayer2().addItemToRender(lines);
                }
                break;
                case CUE_HIGHLIGHT: {
                    getContentLayer1().addItemToRender(lines);
                }
                break;
                default:
                    throw new IllegalStateException("unhandled cue case " + cue);
            }
        }
        return cp2lines.get(new Pair<>(c, strokePattern));
    }

    private Curves getOrCreateCueCurvesForGlyph(String cue, Color c, int strokePattern) {
        HashMap<Pair<Color, Integer>, Curves> cp2curves = this.cp2curvesMaps.get(cue);
        if (!cp2curves.containsKey(new Pair<>(c, strokePattern))) {
            Curves curves = new Curves();
            cp2curves.put(new Pair<>(c, strokePattern), curves);

            switch (cue) {
                case CUE_ACCENTUATE: // fallthrough
                case CUE_EMPHASIZE: {
                    curves.setGlobalThicknessMultiplier(1.2);
                    getContentLayer2().addItemToRender(curves);
                }
                break;
                case CUE_HIGHLIGHT: {
                    getContentLayer1().addItemToRender(curves);
                }
                break;
                default:
                    throw new IllegalStateException("unhandled cue case " + cue);
            }
        }
        return cp2curves.get(new Pair<>(c, strokePattern));
    }

    private void clearCue(String cue) {
        HashMap<Pair<Color, Integer>, Lines> cp2lines = this.cp2linesMaps.get(cue);
        cp2lines.values().forEach(Lines::removeAllSegments);

        HashMap<Pair<Color, Integer>, Curves> cp2curves = this.cp2curvesMaps.get(cue);
        cp2curves.values().forEach(Curves::removeAllCurves);
    }

    protected void highlightFeatureAxis() {
        this.parallelCoordsys.setHighlightedFeature(null);
        this.getCanvas().scheduleRepaint();
    }

    protected void highlightFeatureAxis(int featureIndex, double min, double max) {
        this.parallelCoordsys.setHighlightedFeature(new Pair<>(featureIndex, new ParallelCoordsRenderer.Feature(min, max, null)));
        this.getCanvas().scheduleRepaint();
    }

    /**
     * Sets up JFrame boilerplate, puts this plot into it, and sets the
     * frame visible on the AWT event dispatch thread.
     *
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

        SwingUtilities.invokeLater(() -> {
            frame.pack();
            frame.setVisible(true);
        });

        return frame;
    }
}
