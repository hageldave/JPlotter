package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.SimpleSelectionModel;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected Legend legend = new Legend();
    protected ParallelCoordsVisualMapping visualMapping = new ParallelCoordsVisualMapping(){};
    private int legendRightWidth = 100;
    private int legendBottomHeight = 60;
    final protected LinkedList<ParallelCoordsMouseEventListener> mouseEventListeners = new LinkedList<>();

    protected static final String CUE_HIGHLIGHT = "HIGHLIGHT";
    protected static final String CUE_ACCENTUATE = "ACCENTUATE";
    protected static final String CUE_EMPHASIZE = "EMPHASIZE";

    protected HashMap<String, HashMap<Pair<Color, Integer>, Lines>> cp2linesMaps = new HashMap<>();
    protected HashMap<String, SimpleSelectionModel<Pair<Integer, Integer>>> cueSelectionModels = new HashMap<>();

    public ParallelCoords(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback());
    }

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

        for(String cueType : Arrays.asList(CUE_EMPHASIZE, CUE_ACCENTUATE, CUE_HIGHLIGHT)){
            this.cp2linesMaps.put(cueType, new HashMap<>());
            this.cueSelectionModels.put(cueType, new SimpleSelectionModel<>());
            this.cueSelectionModels.get(cueType).addSelectionListener(selection->createCue(cueType));
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

    public ParallelCoordsVisualMapping getVisualMapping() {
        return visualMapping;
    }

    public void setVisualMapping(ParallelCoordsVisualMapping visualMapping) {
        this.visualMapping = visualMapping;
        for(Lines p: linesPerDataChunk)
            p.setDirty();
        this.canvas.scheduleRepaint();
    }

    public ParallelCoordsDataModel getDataModel() {
        return dataModel;
    }

    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public ParallelCoordsRenderer getCoordsys() {
        return parallelCoordsys;
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

        Lines lines = new Lines();
        linesPerDataChunk.add(lines);
        getContent().addItemToRender(lines);

        for (int i=0; i<dataChunk.length; i++) {
            double[] datapoint = dataChunk[i];
            int pickColor = registerInPickingRegistry(new int[]{chunkIdx, i});

            int numberAxes = getDataModel().axesMap.size()-1;
            for (int j = 0; j < numberAxes; j++) {
                int firstIndex = getDataModel().axesMap.get(j);
                int secondIndex = getDataModel().axesMap.get(j+1);

                Lines.SegmentDetails segmentDetails =
                        lines.addSegment(new Point2D.Double(
                                (double) j / numberAxes, normalizeValue(datapoint[firstIndex], getDataModel().feature2dataIndex.get(firstIndex))),
                                new Point2D.Double((double) (j+1) / numberAxes, normalizeValue(datapoint[secondIndex], getDataModel().feature2dataIndex.get(secondIndex))));
                segmentDetails.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                segmentDetails.setPickColor(pickColor);
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

        Lines lines = linesPerDataChunk.get(chunkIdx);
        for(Lines.SegmentDetails pd:lines.getSegments()) {
            int pickId = pd.pickColor;
            if(pickId != 0) {
                deregisterFromPickingRegistry(pickId);
            }
        }
        lines.removeAllSegments();

        for(int i=0; i<dataChunk.length; i++) {
            double[] datapoint = dataChunk[i];
            int pickColor = registerInPickingRegistry(new int[]{chunkIdx, i});
            int numberAxes = getDataModel().axesMap.size()-1;

            for (int j = 0; j < numberAxes; j++) {
                int firstIndex = getDataModel().axesMap.get(j);
                int secondIndex = getDataModel().axesMap.get(j+1);

                if (firstIndex >= datapoint.length || secondIndex >= datapoint.length) {
                    throw new RuntimeException("Data index is larger than dataset.");
                }

                Lines.SegmentDetails segmentDetails =
                        lines.addSegment(new Point2D.Double(
                                        (double) j / numberAxes, normalizeValue(datapoint[firstIndex], getDataModel().feature2dataIndex.get(firstIndex))),
                                new Point2D.Double((double) (j+1) / numberAxes, normalizeValue(datapoint[secondIndex], getDataModel().feature2dataIndex.get(secondIndex))));
                segmentDetails.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                segmentDetails.setPickColor(pickColor);
            }
        }
        this.canvas.scheduleRepaint();
    }

    protected static double normalizeValue(double value, ParallelCoordsRenderer.Feature feature) {
        return (value - feature.min)/(feature.max - feature.min);
    }

    protected static double denormalizeValue(double value, ParallelCoordsRenderer.Feature feature) {
        double diff = feature.max - feature.min;
        return diff * value + feature.min;
    }

    public static class ParallelCoordsDataModel {
        protected ArrayList<double[][]> dataChunks = new ArrayList<>();
        protected ArrayList<ParallelCoordsRenderer.Feature> features = new ArrayList<>();
        protected HashMap<Integer, ParallelCoordsRenderer.Feature> feature2dataIndex = new HashMap<>();

        protected ArrayList<String> descriptionPerChunk = new ArrayList<>();
        protected LinkedList<ParallelCoordsDataModelListener> listeners = new LinkedList<>();

        protected List<Integer> axesMap = new ArrayList<>();

        public static interface ParallelCoordsDataModelListener {
            public void featureOrderChanged();

            public void featureAdded(ParallelCoordsRenderer.Feature feature);

            public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription);

            public void dataChanged(int chunkIdx, double[][] chunkData);
        }

        public synchronized ParallelCoordsDataModel addFeature(int dataIndex, ParallelCoordsRenderer.Feature feature) {
            features.add(feature);
            axesMap.add(dataIndex);
            feature2dataIndex.put(dataIndex, feature);
            notifyFeatureAdded(feature);
            return this;
        }

        public synchronized ParallelCoordsDataModel addFeature(int[] dataIndices, ParallelCoordsRenderer.Feature[] features) {
            if (dataIndices.length != features.length) {
                throw new IllegalArgumentException("Arrays have to be of equal length");
            }
            for (int i = 0; i < dataIndices.length; i++) {
                addFeature(dataIndices[i], features[i]);
            }
            return this;
        }

        public synchronized void addData(double[][] dataChunk, String chunkDescription) {
            int chunkIdx = this.dataChunks.size();
            this.dataChunks.add(dataChunk);
            this.descriptionPerChunk.add(chunkDescription);
            notifyDataAdded(chunkIdx);
        }

        public synchronized void setDataIndicesOrder(int[] indicesOrder) {
            axesMap.clear();
            for (int i = 0; i < indicesOrder.length; i++) {
                axesMap.add(indicesOrder[i]);
            }
            notifyFeatureOrderChanged();
        }

        public List<Integer> getAxisOrder() {
            return axesMap;
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

        public int getFeatureCount() {
            return features.size();
        }

        public ArrayList<ParallelCoordsRenderer.Feature> getFeatures() {
            return this.features;
        }

        public ParallelCoordsRenderer.Feature getFeature(int index) {
            return this.features.get(index);
        }

        public String getChunkDescription(int chunkIdx) {
            return descriptionPerChunk.get(chunkIdx);
        }

        // return data indices in between min/max
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

        public synchronized ParallelCoordsDataModelListener addListener(ParallelCoordsDataModelListener l) {
            listeners.add(l);
            return l;
        }

        public synchronized void removeListener(ParallelCoordsDataModelListener l) {
            listeners.remove(l);
        }

        public synchronized void notifyFeatureOrderChanged() {
            for(ParallelCoordsDataModelListener l:listeners)
                l.featureOrderChanged();
        }

        public synchronized void notifyFeatureAdded(ParallelCoordsRenderer.Feature feature) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.featureAdded(feature);
        }

        public synchronized void notifyDataAdded(int chunkIdx) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx));
        }

        public synchronized void notifyDataChanged(int chunkIdx) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.dataChanged(chunkIdx, getDataChunk(chunkIdx));
        }
    }

    public static interface ParallelCoordsVisualMapping {
        public default int getColorForChunk(int chunkIdx) {
            DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
            return colorMap.getColor(chunkIdx%colorMap.numColors());
        }

        public default void createLegendElementForChunk(Legend legend, int chunkIdx, String chunkDescr, int pickColor) {
            int color = getColorForChunk(chunkIdx);
            legend.addLineLabel(1, color, chunkDescr, pickColor);
        }

        public default void createGeneralLegendElements(Legend legend) {};
    }

    public void placeLegendOnRight() {
        if(parallelCoordsys.getLegendBottom() == legend) {
            parallelCoordsys.setLegendBottom(null);
            parallelCoordsys.setLegendBottomHeight(0);
        }
        parallelCoordsys.setLegendRight(legend);
        parallelCoordsys.setLegendRightWidth(this.legendRightWidth);
    }

    public void placeLegendOnBottom() {
        if(parallelCoordsys.getLegendRight() == legend) {
            parallelCoordsys.setLegendRight(null);
            parallelCoordsys.setLegendRightWidth(0);
        }
        parallelCoordsys.setLegendBottom(legend);
        parallelCoordsys.setLegendBottomHeight(this.legendBottomHeight);
    }

    public void placeLegendNowhere() {
        if(parallelCoordsys.getLegendRight() == legend) {
            parallelCoordsys.setLegendRight(null);
            parallelCoordsys.setLegendRightWidth(0);
        }
        if(parallelCoordsys.getLegendBottom() == legend) {
            parallelCoordsys.setLegendBottom(null);
            parallelCoordsys.setLegendBottomHeight(0);
        }
    }

    protected void createMouseEventHandler() {
        MouseAdapter mouseEventHandler = new MouseAdapter() {
            final AtomicReference<Double> startPos = new AtomicReference<>(0.0);
            final AtomicBoolean mouseDragged = new AtomicBoolean(false);

            @Override
            public void mouseMoved(MouseEvent e) { mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_MOVED, e); }

            @Override
            public void mouseClicked(MouseEvent e) { mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_CLICKED, e); }

            @Override
            public void mousePressed(MouseEvent e) { mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_PRESSED, e); }

            @Override
            public void mouseReleased(MouseEvent e) { mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_RELEASED, e); }

            @Override
            public void mouseDragged(MouseEvent e) { mouseAction(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_DRAGGED, e); }


            private void mouseAction(String eventType, MouseEvent e) {
                /* TODO: check key mask listeners of panning, zooming, and rectangular point selection
                 * to figure out if the mouse event is being handled by them. If not handled by any of them
                 * then go on with the following.
                 */

                if (Utils.swapYAxis(parallelCoordsys.getCoordSysArea(), canvas.asComponent().getHeight()).contains(e.getPoint())) {
                    Point2D coordsysPoint = parallelCoordsys.transformAWT2CoordSys(e.getPoint(), canvas.asComponent().getHeight());
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);

                    // START Axis highlighting //
                    // TODO: possible performance improvement: call listener only when either new values are added or mouse press is released
                    boolean isEventTypeDragged = eventType.equals(ParallelCoordsMouseEventListener.MOUSE_EVENT_TYPE_DRAGGED);
                    if ((coordsysPoint.getX() % (1.0 / (2 * (dataModel.getFeatureCount() - 1)))) < 0.10 && isEventTypeDragged) {
                        int featureIndex = (int) Math.round(coordsysPoint.getX() / (1.0 / (dataModel.getFeatureCount() - 1)));
                        double currentValue = denormalizeValue(coordsysPoint.getY(), dataModel.getFeature(featureIndex));

                        if (!mouseDragged.get()) {
                            startPos.set(currentValue);
                        } else {
                            double min = Math.min(startPos.get(), currentValue);
                            double max = Math.max(startPos.get(), currentValue);

                            if (min != max)
                                notifyFeatureAxisDragged(eventType, e, featureIndex, min, max);
                        }
                        mouseDragged.set(true);

                    } else if (!isEventTypeDragged) {
                        mouseDragged.set(false);
                        notifyFeatureAxisNone(eventType, e);
                    }
                    // END Axis highlighting //

                    if ((pixel & 0x00ffffff) == 0) {
                        notifyInsideMouseEventNone(eventType, e, coordsysPoint);
                    } else {
                        Object segmentLocalizer = pickingRegistry.lookup(pixel);

                        if(segmentLocalizer instanceof int[]) {
                            int chunkIdx = ((int[])segmentLocalizer)[0];
                            int segmentIdx = ((int[])segmentLocalizer)[1];
                            notifyInsideMouseEventPoint(eventType, e, coordsysPoint, chunkIdx, segmentIdx);
                        }
                    }
                } else {
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
        for(ParallelCoordsMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
    }

    protected synchronized void notifyInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int segmentIdx) {
        for(ParallelCoordsMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, segmentIdx);
    }

    protected synchronized void notifyOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        for(ParallelCoordsMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventNone(mouseEventType, e);
    }

    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        for(ParallelCoordsMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
    }


    protected synchronized void notifyFeatureAxisDragged(String mouseEventType, MouseEvent e, int featureIndex, double min, double max) {
        for (ParallelCoordsMouseEventListener l : mouseEventListeners) {
            l.notifyInsideMouseEventFeature(mouseEventType, e, featureIndex, min, max);
        }
        highlightFeatureAxis(featureIndex, min, max);
    }

    protected synchronized void notifyFeatureAxisNone(String mouseEventType, MouseEvent e) {
        highlightFeatureAxis();
    }

    public static interface ParallelCoordsMouseEventListener {
        static final String MOUSE_EVENT_TYPE_MOVED="moved";
        static final String MOUSE_EVENT_TYPE_CLICKED="clicked";
        static final String MOUSE_EVENT_TYPE_PRESSED="pressed";
        static final String MOUSE_EVENT_TYPE_RELEASED="released";
        static final String MOUSE_EVENT_TYPE_DRAGGED="dragged";

        public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {}

        public default void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int segmentIdx) {}

        public default void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {}

        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {}

        public default void notifyInsideMouseEventFeature(String mouseEventType, MouseEvent e, int featureIndex, double min, double max) {}
    }

    public synchronized ParallelCoordsMouseEventListener addParallelCoordsMouseEventListener(ParallelCoordsMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    public synchronized boolean removeParallelCoordsMouseEventListener(ParallelCoordsMouseEventListener l) {
        return this.mouseEventListeners.remove(l);
    }

    public Lines getLinesForChunk(int chunkIdx) {
        return this.linesPerDataChunk.get(chunkIdx);
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
                    Lines lines = getLinesForChunk(instance.first);
                    Lines.SegmentDetails[] segments = new Lines.SegmentDetails[dataModel.axesMap.size()-1];

                    for (int i = 0; i < segments.length; i++) {
                        segments[i] = lines.getSegments().get(i + instance.second*segments.length);
                    }

                    for (Lines.SegmentDetails l : segments) {
                        Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                        Color c = new Color(this.parallelCoordsys.getColorScheme().getColor1());
                        front.addSegment(l.p0, l.p1).setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.75f))
                                .setThickness(l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier() + 0.2 * lines.getGlobalThicknessMultiplier() + 1.5,
                                        l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier() + 0.2 * lines.getGlobalThicknessMultiplier() + 1.5);
                        front.setStrokePattern(lines.getStrokePattern());
                        front.addSegment(l.p0, l.p1).setColor0(l.color0).setColor1(l.color1)
                                .setThickness(l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier() * 1.05, l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier() * 1.05);
                    }
                }
            }
            break;
            case CUE_EMPHASIZE:
            {
                // accentuation: show enlarged point in top layer
                for(Pair<Integer, Integer> instance : instancesToCue) {
                    Lines lines = getLinesForChunk(instance.first);
                    Lines.SegmentDetails[] segments = new Lines.SegmentDetails[dataModel.axesMap.size()-1];

                    for (int i = 0; i < segments.length; i++) {
                        segments[i] = lines.getSegments().get(i + instance.second*segments.length);
                    }

                    for (Lines.SegmentDetails l : segments) {
                        Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                        front.setStrokePattern(lines.getStrokePattern());
                        front.addSegment(l.p0, l.p1).setColor0(l.color0).setColor1(l.color1)
                                .setThickness(( l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier() ) + 1, ( l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier() ) + 1);
                    }
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
                        Lines lines = getLinesForChunk(instance.first);
                        Lines.SegmentDetails[] segments = new Lines.SegmentDetails[dataModel.axesMap.size()-1];

                        for (int i = 0; i < segments.length; i++) {
                            segments[i] = lines.getSegments().get(i + instance.second*segments.length);
                        }

                        for (Lines.SegmentDetails l : segments) {
                            Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                            front.setStrokePattern(lines.getStrokePattern());

                            front.addSegment(l.p0, l.p1).setColor0(l.color0).setColor1(l.color1)
                                    .setThickness(l.thickness0.getAsDouble()*lines.getGlobalThicknessMultiplier(), l.thickness1.getAsDouble()*lines.getGlobalThicknessMultiplier());
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
    }

    private Lines getOrCreateCueLinesForGlyph(String cue, Color c, int strokePattern) {
        HashMap<Pair<Color, Integer>, Lines> cp2lines = this.cp2linesMaps.get(cue);
        if(!cp2lines.containsKey(new Pair<>(c, strokePattern))) {
            Lines lines = new Lines();
            cp2lines.put(new Pair<>(c, strokePattern), lines);
            switch (cue) {
                case CUE_ACCENTUATE: // fallthrough
                case CUE_EMPHASIZE:
                {
                    lines.setGlobalThicknessMultiplier(1.2);
                    getContentLayer2().addItemToRender(lines);
                }
                break;
                case CUE_HIGHLIGHT:
                {
                    getContentLayer1().addItemToRender(lines);
                }
                break;
                default:
                    throw new IllegalStateException("unhandled cue case " + cue);
            }
        }
        return cp2lines.get(new Pair<>(c, strokePattern));
    }

    private void clearCue(String cue) {
        HashMap<Pair<Color, Integer>, Lines> cp2lines = this.cp2linesMaps.get(cue);
        cp2lines.values().forEach(Lines::removeAllSegments);
    }

    protected void highlightFeatureAxis() {
        this.parallelCoordsys.setHighlightedFeature(null);
    }

    protected void highlightFeatureAxis(int featureIndex, double min, double max) {
        this.parallelCoordsys.setHighlightedFeature(new Pair<>(featureIndex, new ParallelCoordsRenderer.Feature(min, max, null)));
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

        SwingUtilities.invokeLater( ()->{
            frame.pack();
            frame.setVisible(true);
        });

        return frame;
    }
}
