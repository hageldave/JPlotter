package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderers.CombinedBarRenderer;
import hageldave.jplotter.renderers.TrianglesRenderer;
import hageldave.jplotter.util.AlignmentConstants;
import hageldave.jplotter.util.PickingRegistry;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

public class CombinedBarChart {
    protected TrianglesRenderer content;
    protected JPlotterCanvas canvas;
    protected CombinedBarRenderer barRenderer;
    protected LinkedList<BarGroup> barsInRenderer = new LinkedList<>();
    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();

    final protected Legend legend = new Legend();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected LinkedList<BarChartMouseEventListener> mouseEventListeners = new LinkedList<>();

    private int legendRightWidth = 100;
    private int legendBottomHeight = 60;

    public CombinedBarChart(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public CombinedBarChart(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setBounds(new Rectangle(400, 400));
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.black);
        this.barRenderer = new CombinedBarRenderer(AlignmentConstants.HORIZONTAL, DefaultColorScheme.DARK.get());
        this.content = new TrianglesRenderer();
        this.barRenderer.setCoordinateView(-1, -1, 1, 1);
        this.barRenderer.setContent(content);
        this.canvas.setRenderer(barRenderer);
        this.barRenderer.setxAxisLabel(xLabel);
        this.barRenderer.setyAxisLabel(yLabel);

        createMouseEventHandler();
    }

    public CombinedBarChart addData(BarGroup group) {
        this.barsInRenderer.add(group);
        this.barRenderer.addBarGroup(group);
        for (BarGroup.BarStruct struct : group.getGroupedBars().values()) {
            for (BarGroup.Stack stack : struct.stacks) {
                stack.setPickColor(registerInPickingRegistry(stack));
            }
        }
        return this;
    }

    public Legend placeLegendOnRight() {
        if(this.barRenderer.getLegendBottom() == legend) {
            this.barRenderer.setLegendBottom(null);
            this.barRenderer.setLegendBottomHeight(0);
        }
        this.barRenderer.setLegendRight(legend);
        this.barRenderer.setLegendRightWidth(this.legendRightWidth);
        return legend;
    }

    public Legend placeLegendBottom() {
        if(this.barRenderer.getLegendRight() == legend) {
            this.barRenderer.setLegendRight(null);
            this.barRenderer.setLegendRightWidth(0);
        }
        this.barRenderer.setLegendBottom(legend);
        this.barRenderer.setLegendBottomHeight(this.legendBottomHeight);
        return legend;
    }

    public void placeLegendNowhere() {
        if(this.barRenderer.getLegendRight() == legend) {
            this.barRenderer.setLegendRight(null);
            this.barRenderer.setLegendRightWidth(0);
        }
        if(this.barRenderer.getLegendBottom() == legend) {
            this.barRenderer.setLegendBottom(null);
            this.barRenderer.setLegendBottomHeight(0);
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

    public TrianglesRenderer getContent() {
        return content;
    }

    public JPlotterCanvas getCanvas() {
        return this.canvas;
    }

    public CombinedBarRenderer getBarRenderer() {
        return barRenderer;
    }

    public int getLegendRightWidth() {
        return legendRightWidth;
    }

    public void setLegendRightWidth(int legendRightWidth) {
        this.legendRightWidth = legendRightWidth;
    }

    public int getLegendBottomHeight() {
        return legendBottomHeight;
    }

    public void setLegendBottomHeight(int legendBottomHeight) {
        this.legendBottomHeight = legendBottomHeight;
    }

    protected void createMouseEventHandler() {
        MouseAdapter mouseEventHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { mouseAction(BarChartMouseEventListener.MOUSE_EVENT_TYPE_MOVED, e); }

            @Override
            public void mouseClicked(MouseEvent e) { mouseAction(BarChartMouseEventListener.MOUSE_EVENT_TYPE_CLICKED, e); }

            @Override
            public void mousePressed(MouseEvent e) { mouseAction(BarChartMouseEventListener.MOUSE_EVENT_TYPE_PRESSED, e); }

            @Override
            public void mouseReleased(MouseEvent e) { mouseAction(BarChartMouseEventListener.MOUSE_EVENT_TYPE_RELEASED, e); }

            @Override
            public void mouseDragged(MouseEvent e) { mouseAction(BarChartMouseEventListener.MOUSE_EVENT_TYPE_DRAGGED, e); }


            private void mouseAction(String eventType, MouseEvent e) {
                /* TODO: check key mask listeners of panning, zooming, and rectangular point selection
                 * to figure out if the mouse event is being handled by them. If not handled by any of them
                 * then go on with the following.
                 */
                if(Utils.swapYAxis(barRenderer.getCoordSysArea(),canvas.asComponent().getHeight()).contains(e.getPoint())) {
                    /* mouse inside coordinate area */
                    Point2D coordsysPoint = barRenderer.transformAWT2CoordSys(e.getPoint(), canvas.asComponent().getHeight());
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 30);
                    System.out.println(pixel);
                    if((pixel & 0x00ffffff) == 0) {
                        notifyInsideMouseEventNone(eventType, e, coordsysPoint);
                    } else {
                        Object pointLocalizer = pickingRegistry.lookup(pixel);
                        /*if(pointLocalizer instanceof int[]) {
                            int chunkIdx = ((int[])pointLocalizer)[0];
                            int pointIdx = ((int[])pointLocalizer)[1];*/
                            //notifyInsideMouseEventPoint(eventType, e, coordsysPoint, chunkIdx, pointIdx);
                        //}
                        notifyInsideMouseEventPoint(eventType, e, coordsysPoint, 0, 1);
                    }
                } else {
                    /* mouse outside coordinate area */
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
                    System.out.println(pixel);
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
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventNone(mouseEventType, e, coordsysPoint);
    }

    protected synchronized void notifyInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, int chunkIdx, int pointIdx) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, chunkIdx, pointIdx);
    }

    protected synchronized void notifyOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventeNone(mouseEventType, e);
    }

    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, int chunkIdx) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventElement(mouseEventType, e, chunkIdx);
    }

    public static interface BarChartMouseEventListener {
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

    public synchronized BarChartMouseEventListener addBarChartMouseEventListener(BarChartMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    public synchronized boolean removeBarChartMouseEventListener(BarChartMouseEventListener l) {
        return this.mouseEventListeners.remove(l);
    }

}
