package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.PickingRegistry;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class LineChart {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected CompleteRenderer content;
    final protected ArrayList<double[][]> dataAdded = new ArrayList<>();
    final protected HashMap<Integer, Lines> linesAdded = new HashMap<>();

    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();

    final protected LineChartDataModel dataModel = new LineChartDataModel();
    final protected ArrayList<Lines> linesPerDataChunk = new ArrayList<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();
    final protected Legend legend = new Legend();
    protected LineChartVisualMapping visualMapping = new LineChartVisualMapping(){};
    final protected LinkedList<LineChartMouseEventListener> mouseEventListeners = new LinkedList<>();
    final protected LinkedList<PointSetSelectionListener> pointSetSelectionListeners = new LinkedList<>();
    final protected LinkedList<PointSetSelectionListener> pointSetSelectionOngoingListeners = new LinkedList<>();
    private int legendRightWidth = 100;
    private int legendBottomHeight = 60;


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
        this.content = new CompleteRenderer();
        this.coordsys.setCoordinateView(-1, -1, 1, 1);
        this.coordsys.setContent(content);
        this.canvas.setRenderer(coordsys);
        this.coordsys.setxAxisLabel(xLabel);
        this.coordsys.setyAxisLabel(yLabel);

        this.dataModel.addListener(new LineChartDataModel.LineChartDataModelListener() {
            @Override
            public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx, Integer startEndInterval) {
                onDataAdded(chunkIdx, chunkData, chunkDescription, xIdx, yIdx, startEndInterval);
            }

            @Override
            public void dataChanged(int chunkIdx, double[][] chunkData) {
                onDataChanged(chunkIdx, chunkData);
            }
        });

        createMouseEventHandler();
        createRectangularPointSetSelectionCapabilities();
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

    protected synchronized void onDataAdded(int chunkIdx, double[][] dataChunk, String chunkDescription, int xIdx, int yIdx, int startEndInterval) {
        Lines lines = new Lines();
        linesPerDataChunk.add(lines);
        content.addItemToRender(lines);
        lines.setStrokePattern(getVisualMapping().getStrokePatternForChunk(chunkIdx, chunkDescription));
        for(int i=0; i<dataChunk.length-startEndInterval; i++) {
            int i_=i;
            double[] datapoint = dataChunk[i];
            double[] scndDatapoint = dataChunk[i+startEndInterval];
            Lines.SegmentDetails linesDetails = lines.addSegment(
                    new Point2D.Double(datapoint[xIdx], datapoint[yIdx]), new Point2D.Double(scndDatapoint[xIdx], scndDatapoint[yIdx]));
            linesDetails.setColor(()->getVisualMapping().getColorForDataPoint(chunkIdx, chunkDescription, dataChunk, i_));
            linesDetails.setPickColor(registerInPickingRegistry(new int[]{chunkIdx,i}));
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

        int interval = this.getDataModel().startEndInterval.get(chunkIdx);

        // add changed data
        for(int i=0; i<dataChunk.length-interval; i++) {
            int i_=i;
            double[] datapoint = dataChunk[i];
            double[] scndDatapoint = dataChunk[i+interval];
            Lines.SegmentDetails segmentDetails = lines.addSegment(
                    new Point2D.Double(datapoint[dataModel.getXIdx(chunkIdx)], datapoint[dataModel.getYIdx(chunkIdx)]),
                    new Point2D.Double(scndDatapoint[dataModel.getXIdx(chunkIdx)], scndDatapoint[dataModel.getYIdx(chunkIdx)]));
            segmentDetails.setColor(()->getVisualMapping().getColorForDataPoint(chunkIdx, dataModel.getChunkDescription(chunkIdx), dataChunk, i_));
            segmentDetails.setPickColor(registerInPickingRegistry(new int[]{chunkIdx,i}));
        }

        this.canvas.scheduleRepaint();
    }


    public static class LineChartDataModel {
        protected ArrayList<double[][]> dataChunks = new ArrayList<>();
        protected ArrayList<Pair<Integer, Integer>> xyIndicesPerChunk = new ArrayList<>();
        protected ArrayList<Integer> startEndInterval = new ArrayList<>();
        protected ArrayList<String> descriptionPerChunk = new ArrayList<>();

        protected LinkedList<LineChartDataModelListener> listeners = new LinkedList<>();

        public static interface LineChartDataModelListener {
            public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx, int yIdx, Integer startEndInterval);

            public void dataChanged(int chunkIdx, double[][] chunkData);
        }

        public synchronized void addData(double[][] dataChunk, int xIdx, int yIdx, int startEndInterval, String chunkDescription) {
            int chunkIdx = this.dataChunks.size();
            this.dataChunks.add(dataChunk);
            this.xyIndicesPerChunk.add(Pair.of(xIdx, yIdx));
            this.startEndInterval.add(startEndInterval);
            this.descriptionPerChunk.add(chunkDescription);
            notifyDataAdded(chunkIdx);
        }

        public int numChunks() {
            return dataChunks.size();
        }

        public double[][] getDataChunk(int chunkIdx){
            return dataChunks.get(chunkIdx);
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

        public Integer getStartEndInterval(int chunkIdx) {
            return startEndInterval.get(chunkIdx);
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
                l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx), getXIdx(chunkIdx), getYIdx(chunkIdx), getStartEndInterval(chunkIdx));
        }

        public synchronized void notifyDataChanged(int chunkIdx) {
            for(LineChartDataModelListener l:listeners)
                l.dataChanged(chunkIdx, getDataChunk(chunkIdx));
        }

    }

    public static interface LineChartVisualMapping {
        DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
        int[] usualLineChartStrokePatterns = {0xffff, 0xff00, 0x0f0f, 0xaaaa};

        public default int getStrokePatternForChunk(int chunkIdx, String chunkDescr) {
            //int[] usualLineChartStrokePatterns = {0xffff, 0xff00, 0x0f0f, 0xaaaa};
            if (chunkIdx > colorMap.numColors()) {
                return usualLineChartStrokePatterns[chunkIdx%usualLineChartStrokePatterns.length];
            }
            return usualLineChartStrokePatterns[0];
        }

        public default int getColorForDataPoint(int chunkIdx, String chunkDescr, double[][] dataChunk, int pointIdx) {
            //DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
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
     * Adds a scroll zoom to the Scatterplot
     *
     * @return the {@link CoordSysScrollZoom} so that it can be further customized
     */
    public CoordSysScrollZoom addScrollZoom() {
        return new CoordSysScrollZoom(this.canvas, this.coordsys).register();
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
     * Adds a zoom functionality by selecting a rectangle.
     *
     * @return the {@link CoordSysViewSelector} so that it can be further customized
     */
    public CoordSysViewSelector addZoomViewSelector() {
        return new CoordSysViewSelector(this.canvas, this.coordsys) {
            { extModifierMask = 0;/* no need for shift to be pressed */ }

            @Override
            public void areaSelected(double minX, double minY, double maxX, double maxY) {
                coordsys.setCoordinateView(minX, minY, maxX, maxY);
            }
        }.register();
    }

    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public CoordSysRenderer getCoordsys() {
        return coordsys;
    }

    public CompleteRenderer getContent() {
        return content;
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
        for(LineChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
    }

    protected synchronized void notifyInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
        for(LineChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, pointIdx);
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

        public default void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {}

        public default void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {}

        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {}
    }

    public synchronized LineChartMouseEventListener addLineChartMouseEventListener(LineChartMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    public synchronized boolean removeScatterPlotMouseEventListener(LineChartMouseEventListener l) {
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

    public Lines getPointsForChunk(int chunkIdx) {
        return this.linesPerDataChunk.get(chunkIdx);
    }
}
