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
import java.awt.geom.Rectangle2D;
import java.util.*;

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
            public void featureAdded(ParallelCoordsRenderer.Feature feature, int featureXPos) {
                onFeatureAdded(feature, featureXPos);
            }

            @Override
            public void dataAdded(int chunkIdx, Pair<double[][], int[]> chunkData, String chunkDescription, int xIdx) {
                onDataAdded(chunkIdx, chunkData, chunkDescription, xIdx);
            }

            @Override
            public void dataChanged(int chunkIdx, Pair<double[][], int[]> chunkData, int xIdx) {
                onDataChanged(chunkIdx, chunkData, xIdx);
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

    protected synchronized void onFeatureAdded(ParallelCoordsRenderer.Feature feature, int featureXPos) {
        this.parallelCoordsys.addFeature(featureXPos, feature);

        // repaint all the line stuff
        for (int i = 0; i < getDataModel().dataChunks.size(); i++) {
            onDataChanged(i, getDataModel().dataChunks.get(i), dataModel.getFeatureCount());
        }

        this.canvas.scheduleRepaint();
    }

    protected synchronized void onDataAdded(int chunkIdx, Pair<double[][], int[]> dataChunk, String chunkDescription, int featureCount) {
        Lines lines = new Lines();
        linesPerDataChunk.add(lines);
        getContent().addItemToRender(lines);
        for(int i=0; i<dataChunk.first.length; i++) {
            //int i_=i;
            double[] datapoint = dataChunk.first[i];
            int[] indices = dataChunk.second;

            for (int j = 0; j < datapoint.length-1; j++) {
                Lines.SegmentDetails segmentDetails =
                        lines.addSegment(new Point2D.Double(
                                (double) indices[j] / (featureCount-1), normalizeValue(datapoint[j], getDataModel().getFeature(j))),
                                new Point2D.Double((double) indices[j+1] / (featureCount-1), normalizeValue(datapoint[j+1], getDataModel().getFeature(j+1))));
                segmentDetails.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                segmentDetails.setPickColor(registerInPickingRegistry(new int[]{chunkIdx, j}));
            }
        }
        // create a picking ID for use in legend for this data chunk
        this.legendElementPickIds.add(registerInPickingRegistry(chunkIdx));
        visualMapping.createLegendElementForChunk(legend, chunkIdx, chunkDescription, legendElementPickIds.get(chunkIdx));
        this.canvas.scheduleRepaint();
    }

    protected synchronized void onDataChanged(int chunkIdx, Pair<double[][], int[]> dataChunk, int featureCount) {
        Lines lines = linesPerDataChunk.get(chunkIdx);
        for(Lines.SegmentDetails pd:lines.getSegments()) {
            int pickId = pd.pickColor;
            if(pickId != 0) {
                deregisterFromPickingRegistry(pickId);
            }
        }
        lines.removeAllSegments();

        for(int i=0; i<dataChunk.first.length; i++) {
            //int i_=i;
            double[] datapoint = dataChunk.first[i];
            int[] indices = dataChunk.second;

            for (int j = 0; j < datapoint.length-1; j++) {
                Lines.SegmentDetails segmentDetails =
                        lines.addSegment(new Point2D.Double(
                                        (double) indices[j] / (featureCount-1), normalizeValue(datapoint[j], getDataModel().getFeature(indices[j]))),
                                new Point2D.Double((double) indices[j+1] / (featureCount-1), normalizeValue(datapoint[j+1], getDataModel().getFeature(indices[j+1]))));
                segmentDetails.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                segmentDetails.setPickColor(registerInPickingRegistry(new int[]{chunkIdx, j}));
            }
        }

        this.canvas.scheduleRepaint();
    }

    protected static double normalizeValue(double value, ParallelCoordsRenderer.Feature feature) {
        return (value - feature.min)/(feature.max - feature.min);
    }

    public static class ParallelCoordsDataModel {
        protected ArrayList<Pair<double[][], int[]>> dataChunks = new ArrayList<>();
        protected ArrayList<ParallelCoordsRenderer.Feature> features = new ArrayList<>();

        protected ArrayList<String> descriptionPerChunk = new ArrayList<>();

        protected LinkedList<ParallelCoordsDataModelListener> listeners = new LinkedList<>();

        public static interface ParallelCoordsDataModelListener {
            public void featureAdded(ParallelCoordsRenderer.Feature feature, int featureXPos);

            public void dataAdded(int chunkIdx, Pair<double[][], int[]> chunkData, String chunkDescription, int xIdx);

            public void dataChanged(int chunkIdx, Pair<double[][], int[]> chunkData, int xIdx);
        }

        public synchronized ParallelCoordsDataModel addFeature(double min, double max, String label) {
            ParallelCoordsRenderer.Feature feature = new ParallelCoordsRenderer.Feature(min, max, label);
            features.add(feature);
            notifyFeatureAdded(feature, features.size()-1);
            return this;
        }

        public synchronized ParallelCoordsDataModel addFeature(double min, double max, String label, int xMapping) {
            ParallelCoordsRenderer.Feature feature = new ParallelCoordsRenderer.Feature(min, max, label);

            for (int i = features.size()-1; i < xMapping-1; i++) {
                features.add(new ParallelCoordsRenderer.Feature(0, 1, ""));
            }

            features.add(xMapping, feature);
            notifyFeatureAdded(feature, xMapping);
            return this;
        }

        public synchronized ParallelCoordsDataModel addFeature(ParallelCoordsRenderer.Feature... features) {
            this.features.addAll(Arrays.asList(features));
            return this;
        }

        public synchronized ParallelCoordsDataModel addFeature(int[] xMappings, ParallelCoordsRenderer.Feature... features) {
            if (xMappings.length != features.length) {
                throw new IllegalArgumentException("Both arrays have to be of equal length");
            }
            for (int i = 0; i < xMappings.length-1; i++) {
                this.features.set(xMappings[i], features[i]);
            }
            return this;
        }

        public synchronized void addData(double[][] dataChunk, String chunkDescription) {
            // TODO: first check if there are enough features already set
            int[] defaultXMapping = new int[dataChunk[0].length];

            for (int i = 0; i < defaultXMapping.length; i++)
                defaultXMapping[i] = i;

            int chunkIdx = this.dataChunks.size();
            this.dataChunks.add(new Pair<>(dataChunk, defaultXMapping));
            this.descriptionPerChunk.add(chunkDescription);

            notifyDataAdded(chunkIdx);
        }

        public synchronized void addData(double[][] dataChunk, int[] xMapping, String chunkDescription) {
            int chunkIdx = this.dataChunks.size();
            this.dataChunks.add(new Pair<>(dataChunk, xMapping));
            this.descriptionPerChunk.add(chunkDescription);
            notifyDataAdded(chunkIdx);
        }

        public int numChunks() {
            return dataChunks.size();
        }

        public Pair<double[][], int[]> getDataChunk(int chunkIdx){
            return dataChunks.get(chunkIdx);
        }

        public int chunkSize(int chunkIdx) {
            return getDataChunk(chunkIdx).first.length;
        }

        public synchronized void setDataChunk(int chunkIdx, double[][] dataChunk, int[] xMapping){
            if(chunkIdx >= numChunks())
                throw new ArrayIndexOutOfBoundsException("specified chunkIdx out of bounds: " + chunkIdx);
            this.dataChunks.set(chunkIdx, new Pair<>(dataChunk, xMapping));
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

        public TreeSet<Integer> getIndicesOfPointsInArea(int chunkIdx, Rectangle2D area){
            // naive search for contained points
            // TODO: quadtree supported search (quadtrees per chunk have to be kept up to date)
            /*int xIdx = getXIdx();
            int yIdx = getYIdx(chunkIdx);
            double[][] data = getDataChunk(chunkIdx);
            TreeSet<Integer> containedPointIndices = new TreeSet<>();
            for(int i=0; i<data.length; i++) {
                if(area.contains(data[i][xIdx], data[i][yIdx]))
                    containedPointIndices.add(i);
            }
            return containedPointIndices;*/
            return null;
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

        public synchronized void notifyFeatureAdded(ParallelCoordsRenderer.Feature feature, int featureXPos) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.featureAdded(feature, featureXPos);
        }

        public synchronized void notifyDataAdded(int chunkIdx) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx), getFeatureCount());
        }

        public synchronized void notifyDataChanged(int chunkIdx) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.dataChanged(chunkIdx, getDataChunk(chunkIdx), getFeatureCount());
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
                if(Utils.swapYAxis(parallelCoordsys.getCoordSysArea(),canvas.asComponent().getHeight()).contains(e.getPoint())) {

                    Point2D coordsysPoint = parallelCoordsys.transformAWT2CoordSys(e.getPoint(), canvas.asComponent().getHeight());
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
                    if((pixel & 0x00ffffff) == 0) {
                        notifyInsideMouseEventNone(eventType, e, coordsysPoint);
                    } else {
                        Object pointLocalizer = pickingRegistry.lookup(pixel);

                        if(pointLocalizer instanceof int[]) {
                            int chunkIdx = ((int[])pointLocalizer)[0];
                            int lineIdx = ((int[])pointLocalizer)[1];
                            notifyInsideMouseEventPoint(eventType, e, coordsysPoint, chunkIdx, lineIdx);
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

    protected synchronized void notifyInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
        for(ParallelCoordsMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, pointIdx);
    }

    protected synchronized void notifyOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        for(ParallelCoordsMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventNone(mouseEventType, e);
    }

    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        for(ParallelCoordsMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
    }

    public static interface ParallelCoordsMouseEventListener {
        static final String MOUSE_EVENT_TYPE_MOVED="moved";
        static final String MOUSE_EVENT_TYPE_CLICKED="clicked";
        static final String MOUSE_EVENT_TYPE_PRESSED="pressed";
        static final String MOUSE_EVENT_TYPE_RELEASED="released";
        static final String MOUSE_EVENT_TYPE_DRAGGED="dragged";

        public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {}

        public default void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {}

        public default void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {}

        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {}
    }

    public synchronized ParallelCoordsMouseEventListener addScatterPlotMouseEventListener(ParallelCoordsMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    public synchronized boolean removeScatterPlotMouseEventListener(ParallelCoordsMouseEventListener l) {
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

    //@SafeVarargs
    // TODO: Pair is propably not enough
    public final void highlight(int chunkIdx, int lineIdx, int segmentIdx) {
        //highlight(Arrays.asList(toHighlight));
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
                    if (instance.second <= lines.getSegments().size()-1) {
                        Lines.SegmentDetails l = lines.getSegments().get(instance.second);
                        Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                        Color c = new Color(this.parallelCoordsys.getColorScheme().getColor1());
                        front.addSegment(l.p0, l.p1).setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.75f))
                                .setThickness(l.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier() + 0.2*lines.getGlobalThicknessMultiplier() + 1.5,
                                        l.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier() + 0.2*lines.getGlobalThicknessMultiplier() + 1.5);
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
                    if (instance.second <= lines.getSegments().size()-1) {
                        Lines.SegmentDetails l = lines.getSegments().get(instance.second);
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

                        if (instance.second <= lines.getSegments().size()-1) {
                            Lines.SegmentDetails l = lines.getSegments().get(instance.second);
                            Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                            front.setStrokePattern(lines.getStrokePattern());

                            System.out.println(l.p0);
                            System.out.println(l.p1);
                            System.out.println(l);


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
            //Points points = new Points(g);
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
}
