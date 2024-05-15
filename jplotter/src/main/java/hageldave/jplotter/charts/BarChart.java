package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderers.BarRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.renderers.TrianglesRenderer;
import hageldave.jplotter.util.PickingRegistry;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * The BarChart class is a convenience class to quickly create barcharts.
 * Therefore, a new class of renderable and a new renderer are introduced: {@link BarGroup} &amp; {@link BarRenderer},
 * which are connected through this class.
 *
 * Optionally a {@link Renderer} for drawing a legend (such as the {@link Legend} class)
 * can be set to either the bottom or right hand side of the coordinate system.
 * Use {@link #placeLegendBottom()}} or {@link #placeLegendOnRight()} to do so.
 * The legend area size can be partially controlled by {@link #setLegendBottomHeight(int)}
 * and {@link #setLegendRightWidth(int)} if this is needed.
 *
 * The class also implements some simple interaction interfaces
 * (see {@link #notifyInsideMouseEventStruct} for example),
 * where the click on the canvas is registered and the corresponding interface
 * is then called based on what was clicked.
 *
 */
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

    public BarChart(final boolean useFallback, final boolean horizontal) {
        this(useFallback ? new BlankCanvasFallback() : new BlankCanvas(), "X", "Y", horizontal);
    }

    public BarChart(final JPlotterCanvas canvas, final String xLabel, final String yLabel, final boolean horizontal) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.barRenderer = new BarRenderer(horizontal, DefaultColorScheme.LIGHT.get());
        this.content = this.barRenderer.getContent();
        this.barRenderer.setCoordinateView(-1, -1, 1, 1);
        this.canvas.setRenderer(barRenderer);
        this.barRenderer.setxAxisLabel(xLabel);
        this.barRenderer.setyAxisLabel(yLabel);

        createMouseEventHandler();
    }

    /**
     * Adds a BarGroup to the BarChart, which will be rendered by a BarRenderer.
     *
     * @param group will be added to the BarChart
     * @return this for chaining
     */
    public BarChart addData(BarGroup group) {
        for (BarGroup.BarStack barStack : group.getGroupedBars().values()) {
            for (BarGroup.BarStruct barStruct : barStack.barStructs) {
                barStruct.setPickColor(registerInPickingRegistry(barStruct));
            }
        }
        this.barRenderer.addBarGroup(group);
        return this;
    }

    /**
     * Removes the BarGroup from the BarChart.
     *
     * @param group will be removed from the BarChart
     * @return this for chaining
     */
    public BarChart removeData(BarGroup group) {
        if (this.barRenderer.getBarGroups().contains(group)) {
            for (BarGroup.BarStack barStack : group.getGroupedBars().values()) {
                for (BarGroup.BarStruct barStruct : barStack.barStructs) {
                    deregisterFromPickingRegistry(barStruct.pickColor);
                }
            }
            this.barRenderer.removeBarGroup(group);
        }
        return this;
    }

    /**
     * Places a legend on the right, next to the content.
     * The width can be modified by the {@link BarRenderer#setLegendRightWidth(int)} method.
     * @return Legend for chaining
     */
    public Legend placeLegendOnRight() {
        if(this.barRenderer.getLegendBottom() == legend) {
            this.barRenderer.setLegendBottom(null);
            this.barRenderer.setLegendBottomHeight(0);
        }
        this.barRenderer.setLegendRight(legend);
        this.barRenderer.setLegendRightWidth(this.legendRightWidth);
        return legend;
    }

    /**
     * Places a legend on the bottom, under the content.
     * The height can be modified by the {@link BarRenderer#setLegendBottomHeight(int)} method.
     * @return Legend for chaining
     */
    public Legend placeLegendBottom() {
        if(this.barRenderer.getLegendRight() == legend) {
            this.barRenderer.setLegendRight(null);
            this.barRenderer.setLegendRightWidth(0);
        }
        this.barRenderer.setLegendBottom(legend);
        this.barRenderer.setLegendBottomHeight(this.legendBottomHeight);
        return legend;
    }

    /**
     * Removes all legends from the BarChart.
     * @return this for chaining
     */
    public BarChart placeLegendNowhere() {
        if(this.barRenderer.getLegendRight() == legend) {
            this.barRenderer.setLegendRight(null);
            this.barRenderer.setLegendRightWidth(0);
        }
        if(this.barRenderer.getLegendBottom() == legend) {
            this.barRenderer.setLegendBottom(null);
            this.barRenderer.setLegendBottomHeight(0);
        }
        return this;
    }

    /**
     * Registers an object in the picking registry
     * (see {@link PickingRegistry} for more information about the functionality of the picking registry).
     * @param obj will be registered in the picking registry
     * @return the objects' id in the picking registry
     */
    protected synchronized int registerInPickingRegistry(Object obj) {
        int id = freedPickIds.isEmpty() ? pickingRegistry.getNewID() : freedPickIds.pollFirst();
        pickingRegistry.register(obj, id);
        return id;
    }

    /**
     * Deregisters an object from the picking registry
     * (see {@link PickingRegistry} for more information about the functionality of the picking registry).
     * @param id of the object to deregister
     * @return the object deregistered
     */
    protected synchronized Object deregisterFromPickingRegistry(int id) {
        Object old = pickingRegistry.lookup(id);
        pickingRegistry.register(null, id);
        freedPickIds.add(id);
        return old;
    }

    /**
     * @return {@link TrianglesRenderer} rendering the individual bars.
     */
    public TrianglesRenderer getContent() {
        return content;
    }

    /**
     * @return corresponding canvas, where everything is rendered in (See {@link hageldave.jplotter.canvas.FBOCanvas}).
     */
    public JPlotterCanvas getCanvas() {
        return this.canvas;
    }

    /**
     * @return {@link BarRenderer} that basically renders everything (coordinate system, legends, bars, ...).
     */
    public BarRenderer getBarRenderer() {
        return barRenderer;
    }

    /**
     * @return width of the right hand side legend area.
     */
    public int getLegendRightWidth() {
        return legendRightWidth;
    }

    /**
     * Sets the width of the legend area right to the coordinate system.
     * (height is determined by the space available until the bottom of the renderer's viewport)
     * @param legendRightWidth width of the right legend area.
     * (default is 100 px)
     * @return this for chaining
     */
    public BarChart setLegendRightWidth(int legendRightWidth) {
        this.legendRightWidth = legendRightWidth;
        return this;
    }

    /**
     * @return height of the bottom side legend area.
     */
    public int getLegendBottomHeight() {
        return legendBottomHeight;
    }

    /**
     * Sets the height of the legend area below the coordinate system.
     * (width is determined by x-axis width)
     * @param legendBottomHeight height of the bottom legend area.
     * (default is 60px)
     * @return this for chaining
     */
    public BarChart setLegendBottomHeight(int legendBottomHeight) {
        this.legendBottomHeight = legendBottomHeight;
        return this;
    }

    /**
     * @return current alignment of the BarChart.
     */
    public boolean isHorizontal() {
        return this.barRenderer.isHorizontal();
    }

    /**
     * Changes the orientation of the BarChart.
     * @param horizontal orientation of the BarChart
     * @return this for chaining
     */
    public BarChart setAlignment(boolean horizontal) {
        this.barRenderer.setHorizontal(horizontal);
        return this;
    }

    /**
     * Sets the coordinate view (see {@link BarRenderer#setCoordinateView(Rectangle2D)})
     * accordingly to the bounds of all bars in the bar chart.
     *
     * @return this for chaining
     */
    public BarChart alignBarRenderer() {
        getBarRenderer().setCoordinateView(getBarRenderer().getBounds());
        return this;
    }

    /**
     * Creates a mouse event handler.
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
                        if (pointLocalizer instanceof BarGroup.BarStruct) {
                            notifyInsideMouseEventStruct(eventType, e, coordsysPoint, (BarGroup.BarStruct) pointLocalizer);
                        }
                    }
                } else {
                    /* mouse outside coordinate area */
                    // get pick color under cursor
                    int pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
                    if((pixel & 0x00ffffff) == 0) {
                        notifyOutsideMouseEventNone(eventType, e);
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

    protected synchronized void notifyInsideMouseEventStruct(String mouseEventType, MouseEvent e, Point2D coordsysPoint, BarGroup.BarStruct barStruct) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onInsideMouseEventStruct(mouseEventType, e, coordsysPoint, barStruct);
    }

    protected synchronized void notifyOutsideMouseEventNone(String mouseEventType, MouseEvent e) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventNone(mouseEventType, e);
    }

    protected synchronized void notifyOutsideMouseEventElement(String mouseEventType, MouseEvent e, Legend.BarLabel legendElement) {
        for(BarChartMouseEventListener l:mouseEventListeners)
            l.onOutsideMouseEventElement(mouseEventType, e, legendElement);
    }

    /**
     * The BarChartMouseEventListener interface contains multiple methods,
     * notifying if an element has been hit or not (inside and outside the coordsys).
     */
    public static interface BarChartMouseEventListener {
        static final String MOUSE_EVENT_TYPE_MOVED="moved";
        static final String MOUSE_EVENT_TYPE_CLICKED="clicked";
        static final String MOUSE_EVENT_TYPE_PRESSED="pressed";
        static final String MOUSE_EVENT_TYPE_RELEASED="released";
        static final String MOUSE_EVENT_TYPE_DRAGGED="dragged";

        /**
         * Called whenever the mouse pointer doesn't hit a {@link BarGroup.BarStruct} of the BarChart while being inside the coordsys.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         * @param coordsysPoint coordinates of the mouse event inside the coordinate system
         */
        public default void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {}

        /**
         * Called when the mouse pointer does hit a {@link BarGroup.BarStruct} of the BarChart.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         * @param coordsysPoint coordinates of the mouse event inside the coordinate system
         * @param barStruct that has been hit
         */
        public default void onInsideMouseEventStruct(String mouseEventType, MouseEvent e, Point2D coordsysPoint, BarGroup.BarStruct barStruct) {}

        /**
         * Called when the mouse pointer doesn't hit an element (e.g. legend elements) of the BarChart while being outside the coordsys.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         */
        public default void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {}

        /**
         * Called when the mouse pointer hits an element (e.g. legend elements) of the BarChart while being outside the coordsys.
         *
         * @param mouseEventType type of the mouse event
         * @param e passed on mouse event of the mouse adapter registering the mouse movements
         * @param legendElement clicked legendElement
         */
        public default void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, Legend.BarLabel legendElement) {}
    }

    /**
     * Adds a {@link BarChartMouseEventListener} to the BarChart.
     *
     * @param l {@link BarChartMouseEventListener} that implements the interface methods which is called whenever one of the defined mouse events happens
     * @return {@link BarChartMouseEventListener} for chaining
     */
    public synchronized BarChartMouseEventListener addBarChartMouseEventListener(BarChartMouseEventListener l) {
        this.mouseEventListeners.add(l);
        return l;
    }

    /**
     * Removes the specified {@link BarChartMouseEventListener} from the BarChart.
     *
     * @param l {@link BarChartMouseEventListener} that should be removed
     * @return true if the {@link BarChartMouseEventListener} was added to the BarChart before
     */
    public synchronized boolean removeBarChartMouseEventListener(BarChartMouseEventListener l) {
        return this.mouseEventListeners.remove(l);
    }

}
