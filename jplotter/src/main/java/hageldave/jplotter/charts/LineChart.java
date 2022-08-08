package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.interaction.kml.CoordSysPanning;
import hageldave.jplotter.interaction.kml.CoordSysScrollZoom;
import hageldave.jplotter.interaction.kml.CoordSysViewSelector;
import hageldave.jplotter.interaction.kml.KeyMaskListener;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
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

public class LineChart {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;

    protected ScatterPlot scatterPlot;

    protected CompleteRenderer contentLayer0;
    protected CompleteRenderer contentLayer1;
    protected CompleteRenderer contentLayer2;

    public PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();

    final protected LineChartDataModel dataModel = new LineChartDataModel();
    final protected ArrayList<Lines> linesPerDataChunk = new ArrayList<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();
    final protected Legend legend = new Legend();
    protected LineChartVisualMapping visualMapping = new LineChartVisualMapping(){};
    final protected LinkedList<LineChartMouseEventListener> mouseEventListeners = new LinkedList<>();
    final protected LinkedList<LineSetSelectionListener> lineSetSelectionListeners = new LinkedList<>();
    final protected LinkedList<LineSetSelectionListener> lineSetSelectionOngoingListeners = new LinkedList<>();
    private int legendRightWidth = 100;
    private int legendBottomHeight = 60;

    protected static final String CUE_HIGHLIGHT = "HIGHLIGHT";
    protected static final String CUE_ACCENTUATE = "ACCENTUATE";
    protected static final String CUE_EMPHASIZE = "EMPHASIZE";

    protected HashMap<String, HashMap<Pair<Color, Integer>, Lines>> cp2linesMaps = new HashMap<>();
    protected HashMap<String, SimpleSelectionModel<Pair<Integer, Integer>>> cueSelectionModels = new HashMap<>();

    public LineChart(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public LineChart(final boolean useOpenGL, final String xLabel, final String yLabel) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), xLabel, yLabel);
    }

    public LineChart(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
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

        this.dataModel.addListener(new LineChartDataModel.LineChartDataModelListener() {
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
            this.cp2linesMaps.put(cueType, new HashMap<>());
            this.cueSelectionModels.put(cueType, new SimpleSelectionModel<>());
            this.cueSelectionModels.get(cueType).addSelectionListener(selection->createCue(cueType));
        }
    }

    public LineChartVisualMapping getVisualMapping() {
        return visualMapping;
    }

    public void setVisualMapping(LineChartVisualMapping visualMapping) {
        this.visualMapping = visualMapping;
        for(Lines p:linesPerDataChunk)
            p.setDirty();
        this.canvas.scheduleRepaint();
    }

    public LineChartDataModel getDataModel() {
        return dataModel;
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
        Lines lines = new Lines();
        linesPerDataChunk.add(lines);
        contentLayer0.addItemToRender(lines);
        lines.setStrokePattern(getVisualMapping().getStrokePatternForChunk(chunkIdx, chunkDescription));
        for(int i=0; i<dataChunk.length-1; i++) {
            int i_=i;
            double[] datapoint = dataChunk[i];
            double[] nextDatapoint = dataChunk[i+1];
            Lines.SegmentDetails linesDetails = lines.addSegment(
                    new Point2D.Double(datapoint[xIdx], datapoint[yIdx]), new Point2D.Double(nextDatapoint[xIdx], nextDatapoint[yIdx]));
            linesDetails.setColor(()->getVisualMapping().getColorForDataPoint(chunkIdx, chunkDescription, dataChunk, i_));
            linesDetails.setPickColor(registerInPickingRegistry(new LineDataChunk(chunkIdx, i)));
        }
        // create a picking ID for use in legend for this data chunk
        this.legendElementPickIds.add(registerInPickingRegistry(chunkIdx));
        visualMapping.createLegendElementForChunk(legend, chunkIdx, chunkDescription, legendElementPickIds.get(chunkIdx));

        this.canvas.scheduleRepaint();
    }

    protected synchronized void onDataChanged(int chunkIdx, double[][] dataChunk) {
        Lines lines = linesPerDataChunk.get(chunkIdx);

        // collect pick id's from current points for later reuse
        for(Lines.SegmentDetails sd:lines.getSegments()) {
            int pickId = sd.pickColor;
            if(pickId != 0) {
                deregisterFromPickingRegistry(pickId);
            }
        }
        lines.removeAllSegments();

        // add changed data
        for(int i=0; i<dataChunk.length-1; i++) {
            int i_=i;
            double[] datapoint = dataChunk[i];
            double[] nextDatapoint = dataChunk[i+1];
            Lines.SegmentDetails segmentDetails = lines.addSegment(
                    new Point2D.Double(datapoint[dataModel.getXIdx(chunkIdx)], datapoint[dataModel.getYIdx(chunkIdx)]),
                    new Point2D.Double(nextDatapoint[dataModel.getXIdx(chunkIdx)], nextDatapoint[dataModel.getYIdx(chunkIdx)]));
            segmentDetails.setColor(()->getVisualMapping().getColorForDataPoint(chunkIdx, dataModel.getChunkDescription(chunkIdx), dataChunk, i_));
            segmentDetails.setPickColor(registerInPickingRegistry(new LineDataChunk(chunkIdx, i)));
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

    public static class LineChartDataModel extends DataModel {
        protected ArrayList<Pair<Integer, Integer>> xyIndicesPerChunk = new ArrayList<>();
        protected ArrayList<String> descriptionPerChunk = new ArrayList<>();

        protected LinkedList<LineChartDataModelListener> listeners = new LinkedList<>();

        public static interface LineChartDataModelListener {
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

        public synchronized void addListener(LineChartDataModelListener l) {
            listeners.add(l);
        }

        protected synchronized void notifyDataAdded(int chunkIdx) {
            for(LineChartDataModelListener l:listeners)
                l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx), getXIdx(chunkIdx), getYIdx(chunkIdx));
        }

        public synchronized void notifyDataChanged(int chunkIdx) {
            for(LineChartDataModelListener l:listeners)
                l.dataChanged(chunkIdx, getDataChunk(chunkIdx));
        }
    }

    final public static class LineDataChunk extends DataChunk {
        public LineDataChunk(int chunkID, int pointID) {
            super(chunkID, pointID);
        }
    }

    public static interface LineChartVisualMapping {
        DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
        int[] usualLineChartStrokePatterns = {0xffff, 0xff00, 0x0f0f, 0xaaaa};

        public default int getStrokePatternForChunk(int chunkIdx, String chunkDescr) {
            if (chunkIdx > colorMap.numColors()) {
                return usualLineChartStrokePatterns[chunkIdx%usualLineChartStrokePatterns.length];
            }
            return usualLineChartStrokePatterns[0];
        }

        public default int getColorForDataPoint(int chunkIdx, String chunkDescr, double[][] dataChunk, int pointIdx) {
            return colorMap.getColor(chunkIdx%colorMap.numColors());
        }

        public default void createLegendElementForChunk(Legend legend, int chunkIdx, String chunkDescr, int pickColor) {
            int strokepattern = getStrokePatternForChunk(chunkIdx, chunkDescr);
            int color = getColorForDataPoint(chunkIdx, chunkDescr, null, -1);
            legend.addLineLabel(1, color, strokepattern, chunkDescr, pickColor);
        }

        public default void createGeneralLegendElements(Legend legend) {};
    }


    public LineChart alignCoordsys(final double scaling) {
        Rectangle2D union = null;
        for (Lines lines: linesPerDataChunk) {
            if(union==null)
                union = lines.getBounds();
            else
                Rectangle2D.union(lines.getBounds(), union, union);
        }
        if(union != null)
            this.coordsys.setCoordinateView(Utils.scaleRect(union, scaling));
        this.canvas.scheduleRepaint();
        return this;
    }

    public LineChart alignCoordsys() {
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
     * TODO
     *
     * @return
     */
    public LineChart fillLineArea(double threshold, double alpha, int... chunkIds) {
        for (int chunkId: chunkIds) {
            Triangles lineArea = new Triangles();
            lineArea.setGlobalAlphaMultiplier(alpha);

            double[][] dataChunk = dataModel.getDataChunk(chunkId);
            Lines lines = linesPerDataChunk.get(chunkId);
            int color = visualMapping.getColorForDataPoint(chunkId, dataModel.getChunkDescription(chunkId), dataChunk, 0);

            for(Lines.SegmentDetails segment: lines.getSegments()){
                Point2D pL=segment.p0, pR=segment.p1;

                if(Math.signum(pL.getY()-threshold) == Math.signum(pR.getY()-threshold)){
                    // points are on same side of x-axis
                    ArrayList<Triangles.TriangleDetails> quad = lineArea.addQuad(
                            pL.getX(), threshold, pL.getX(), pL.getY(),
                            pR.getX(), pR.getY(), pR.getX(), threshold);
                    quad.forEach(tri->tri.setColor(color));
                } else {
                    // segment intersects x-axis, need to find intersection
                    double x0=pL.getX(),y0=pL.getY(), x2=pR.getX(), y2=pR.getY();
                    double m = ((y2-threshold)-(y0-threshold))/(x2-x0);
                    // solving for x1 in (x1-x0)*m+y0 = 0 --> 1x = x0-y0/m;
                    double x1 = x0-(y0-threshold)/m;
                    lineArea.addTriangle(x0, y0, x1, threshold, x0, threshold).setColor(color);
                    lineArea.addTriangle(x2, y2, x1, threshold, x2, threshold).setColor(color);
                }
            }
            contentLayer0.addItemToRender(lineArea);
        }
        return this;
    }

    public LineChart fillLineArea(double threshold, int... chunkIds) {
        return fillLineArea(threshold, 0.2, chunkIds);
    }

    public LineChart fillLineArea(int... chunkIds) {
        return fillLineArea(0, 0.2, chunkIds);
    }

    /**
     * Adds a scroll zoom to the Scatterplot
     *
     * @return the {@link CoordSysScrollZoom} so that it can be further customized
     */
    public CoordSysScrollZoom addScrollZoom() {
        return new CoordSysScrollZoom(this.canvas, this.coordsys).register();
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
     * Adds panning functionality to the Scatterplot
     *
     * @return the {@link CoordSysPanning} so that it can be further customized
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
     * Adds a zoom functionality by selecting a rectangle.
     *
     * @return the {@link CoordSysViewSelector} so that it can be further customized
     */
    public CoordSysViewSelector addZoomViewSelector() {
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

    public ScatterPlot getScatterPlot() {
        return scatterPlot;
    }

    public void setScatterPlot(ScatterPlot scatterPlot) {
        this.scatterPlot = scatterPlot;
        this.scatterPlot.canvas = this.canvas;
        this.scatterPlot.coordsys = this.coordsys;
        this.scatterPlot.pickingRegistry = this.pickingRegistry;
        this.scatterPlot.contentLayer0 = this.contentLayer0;
        this.scatterPlot.contentLayer1 = this.contentLayer1;
        this.scatterPlot.contentLayer2 = this.contentLayer2;
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

    protected void createMouseEventHandler() {
        MouseAdapter mouseEventHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { mouseAction(LineChartMouseEventListener.MOUSE_EVENT_TYPE_MOVED, e); }

            @Override
            public void mouseClicked(MouseEvent e) { mouseAction(LineChartMouseEventListener.MOUSE_EVENT_TYPE_CLICKED, e); }

            @Override
            public void mousePressed(MouseEvent e) { mouseAction(LineChartMouseEventListener.MOUSE_EVENT_TYPE_PRESSED, e); }

            @Override
            public void mouseReleased(MouseEvent e) { mouseAction(LineChartMouseEventListener.MOUSE_EVENT_TYPE_RELEASED, e); }

            @Override
            public void mouseDragged(MouseEvent e) { mouseAction(LineChartMouseEventListener.MOUSE_EVENT_TYPE_DRAGGED, e); }


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
                        if (scatterPlot != null)
                            scatterPlot.notifyInsideMouseEventNone(eventType, e, coordsysPoint);
                    } else {
                        Object pointLocalizer = pickingRegistry.lookup(pixel);
                        if (pointLocalizer instanceof LineDataChunk) {
                            int chunkIdx = ( (LineDataChunk) pointLocalizer ).getChunkID();
                            int pointIdx = ( (LineDataChunk) pointLocalizer ).getPointID();
                            notifyInsideMouseEventLine(eventType, e, coordsysPoint, chunkIdx, pointIdx);
                        } else if (pointLocalizer instanceof ScatterPlot.PointDataChunk) {
                            int chunkIdx = ( (ScatterPlot.PointDataChunk) pointLocalizer ).getChunkID();
                            int pointIdx = ( (ScatterPlot.PointDataChunk) pointLocalizer ).getPointID();
                            if (scatterPlot != null)
                                scatterPlot.notifyInsideMouseEventPoint(eventType, e, coordsysPoint, chunkIdx, pointIdx);
                        }
                    }
                } else {
                    /* mouse outside coordinate area */
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
                    if((pixel & 0x00ffffff) == 0) {
                        notifyOutsideMouseEventeNone(eventType, e);
                        if (scatterPlot != null)
                            scatterPlot.notifyOutsideMouseEventeNone(eventType, e);
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
        for(LineChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
    }

    protected synchronized void notifyInsideMouseEventLine(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int lineIdx) {
        for(LineChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventLine(mouseEventType, e, coordsysPoint, chunkIdx, lineIdx);
    }

    protected synchronized void notifyOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        for(LineChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventeNone(mouseEventType, e);
    }

    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        for(LineChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
    }

    public static interface LineChartMouseEventListener {
        static final String MOUSE_EVENT_TYPE_MOVED="moved";
        static final String MOUSE_EVENT_TYPE_CLICKED="clicked";
        static final String MOUSE_EVENT_TYPE_PRESSED="pressed";
        static final String MOUSE_EVENT_TYPE_RELEASED="released";
        static final String MOUSE_EVENT_TYPE_DRAGGED="dragged";

        public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {}

        public default void onInsideMouseEventLine(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {}

        public default void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {}

        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {}
    }

    public synchronized LineChartMouseEventListener addLineChartMouseEventListener(LineChartMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    public synchronized boolean removeLineChartMouseEventListener(LineChartMouseEventListener l) {
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

        //KeyMaskListener keyMask = new KeyMaskListener(KeyEvent.VK_S);
        CoordSysViewSelector selector = new CoordSysViewSelector(this.canvas, this.coordsys) {
            @Override
            public void areaSelectedOnGoing(double minX, double minY, double maxX, double maxY) {
                if(lineSetSelectionOngoingListeners.isEmpty())
                    return;
                Rectangle2D area = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
                selectionRectMemory[0] = area;
                selectedPointsOngoing.setSelection(getIndicesOfPointsInArea(area));
            }

            @Override
            public void areaSelected(double minX, double minY, double maxX, double maxY) {
                if(lineSetSelectionListeners.isEmpty())
                    return;
                Rectangle2D area = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
                selectionRectMemory[1] = area;
                selectedPoints.setSelection(getIndicesOfPointsInArea(area));
            }
        };
        selector.register();

        selectedPointsOngoing.addSelectionListener(s->{
            ArrayList<Pair<Integer, TreeSet<Integer>>> list = new ArrayList<>(s);
            notifyLineSetSelectionChangeOngoing(list, selectionRectMemory[0]);
        });
        selectedPoints.addSelectionListener(s->{
            ArrayList<Pair<Integer, TreeSet<Integer>>> list = new ArrayList<>(s);
            notifyLineSetSelectionChange(list, selectionRectMemory[1]);
        });
    }

    public static interface LineSetSelectionListener {
        public void onLineSetSelectionChanged(ArrayList<Pair<Integer, TreeSet<Integer>>> selectedPoints, Rectangle2D selectionArea);
    }

    protected synchronized void notifyLineSetSelectionChangeOngoing(ArrayList<Pair<Integer, TreeSet<Integer>>> list, Rectangle2D rect) {
        for(LineSetSelectionListener l: lineSetSelectionOngoingListeners) {
            l.onLineSetSelectionChanged(list, rect);
        }
    }

    protected synchronized void notifyLineSetSelectionChange(ArrayList<Pair<Integer, TreeSet<Integer>>> list, Rectangle2D rect) {
        for(LineSetSelectionListener l: lineSetSelectionListeners) {
            l.onLineSetSelectionChanged(list, rect);
        }
    }

    public synchronized void addLineSetSelectionListener(LineSetSelectionListener l) {
        this.lineSetSelectionListeners.add(l);
    }

    public synchronized void addLineSetSelectionOngoingListener(LineSetSelectionListener l) {
        this.lineSetSelectionOngoingListeners.add(l);
    }

    public Lines getLinesForChunk(int chunkIdx) {
        return this.linesPerDataChunk.get(chunkIdx);
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
                    Lines lines = getLinesForChunk(instance.first);
                    if (instance.second <= lines.getSegments().size()-1) {
                        Lines.SegmentDetails l = lines.getSegments().get(instance.second);
                        Lines front = getOrCreateCueLinesForGlyph(cueType, new Color(lines.getSegments().get(0).color0.getAsInt()), lines.getStrokePattern());
                        Color c = new Color(this.coordsys.getColorScheme().getColor1());
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

    // TODO: convenience methods to create listeners for cues (e.g. hovering does highlighting)
}
