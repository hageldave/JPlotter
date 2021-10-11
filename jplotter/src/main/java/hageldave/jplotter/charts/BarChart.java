package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderers.BarRenderer;
import hageldave.jplotter.renderers.TrianglesRenderer;
import hageldave.jplotter.util.PickingRegistry;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;


public class BarChart {
    protected TrianglesRenderer content;
    protected JPlotterCanvas canvas;
    protected BarRenderer barRenderer;
    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();

    final protected Legend legend = new Legend();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected LinkedList<BarChartMouseEventListener> mouseEventListeners = new LinkedList<>();

    private int legendRightWidth = 100;
    private int legendBottomHeight = 60;

    public BarChart(final boolean useFallback, final int alignment) {
        this(useFallback ? new BlankCanvasFallback() : new BlankCanvas(), "X", "Y", alignment);
    }

    public BarChart(final JPlotterCanvas canvas, final String xLabel, final String yLabel, final int alignment) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.barRenderer = new BarRenderer(alignment, DefaultColorScheme.LIGHT.get());
        this.content = new TrianglesRenderer();
        this.barRenderer.setCoordinateView(-1, -1, 1, 1);
        this.barRenderer.setContent(content);
        this.canvas.setRenderer(barRenderer);
        this.barRenderer.setxAxisLabel(xLabel);
        this.barRenderer.setyAxisLabel(yLabel);

        createMouseEventHandler();
    }

    public BarChart addData(BarGroup group) {
        for (BarGroup.BarStruct struct : group.getGroupedBars().values()) {
            for (BarGroup.Stack stack : struct.stacks) {
                stack.setPickColor(registerInPickingRegistry(stack));
            }
        }
        this.barRenderer.addBarGroup(group);
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

    public BarRenderer getBarRenderer() {
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

    public int getAlignment() {
        return this.barRenderer.getAlignment();
    }

    public void setAlignment(int alignment) {
        this.barRenderer.setAlignment(alignment);
    }

    /**
     * IDEA: return group, struct and stack - manipulate this data and refresh
     *
     */
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
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
                    if((pixel & 0x00ffffff) == 0) {
                        notifyInsideMouseEventNone(eventType, e, coordsysPoint);
                    } else {
                        Object pointLocalizer = pickingRegistry.lookup(pixel);
                        if (pointLocalizer instanceof BarGroup.Stack) {
                            notifyInsideMouseEventStack(eventType, e, coordsysPoint, (BarGroup.Stack) pointLocalizer);
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
                        if(miscLocalizer instanceof Legend.BarLabel) {
                            notifyOutsideMouseEventElement(eventType, e, (Legend.BarLabel) miscLocalizer);
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

    protected synchronized void notifyInsideMouseEventStack(String mouseEventType, MouseEvent e, Point2D coordsysPoint, BarGroup.Stack stack) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventPoint(mouseEventType, e, coordsysPoint, stack);
    }

    protected synchronized void notifyOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventeNone(mouseEventType, e);
    }

    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, Legend.BarLabel legendElement) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventElement(mouseEventType, e, legendElement);
    }

    public static interface BarChartMouseEventListener {
        static final String MOUSE_EVENT_TYPE_MOVED="moved";
        static final String MOUSE_EVENT_TYPE_CLICKED="clicked";
        static final String MOUSE_EVENT_TYPE_PRESSED="pressed";
        static final String MOUSE_EVENT_TYPE_RELEASED="released";
        static final String MOUSE_EVENT_TYPE_DRAGGED="dragged";

        public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {}

        public default void onInsideMouseEventPoint(String mouseEventType, MouseEvent e, Point2D coordsysPoint, BarGroup.Stack stack) {}

        public default void onOutsideMouseEventeNone(String mouseEventType, MouseEvent e) {}

        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, Legend.BarLabel legendElement) {}
    }

    public synchronized BarChartMouseEventListener addBarChartMouseEventListener(BarChartMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    public synchronized boolean removeBarChartMouseEventListener(BarChartMouseEventListener l) {
        return this.mouseEventListeners.remove(l);
    }

}
