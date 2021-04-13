package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.RenderableDetails;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.GenericRenderer;
import hageldave.jplotter.util.PickingRegistry;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * The CoordSysMouseOver class
 */
public abstract class CoordSysMouseOver extends MouseAdapter {

    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;

    // TODO points need to be passed in the constructor
    protected LinkedList<double[][]> points;
    protected int extModifierMask = InputEvent.SHIFT_DOWN_MASK;
    protected final LinkedList<Integer> extModifierMaskExcludes = new LinkedList<Integer>();

    public CoordSysMouseOver (JPlotterCanvas canvas, CoordSysRenderer coordsys, LinkedList<double[][]> allDataPoints) {
        this.canvas = canvas;
        this.coordsys = coordsys;
        this.points = allDataPoints;
    }

    @Override
    public void mousePressed (MouseEvent e) {
        findPoints(e);
    }

    @Override
    public void mouseDragged (MouseEvent e) {

    }

    /**
     * TODO might find points later in datasets
     */
    @SuppressWarnings("rawtypes")
    protected boolean findPoints (final MouseEvent e) {
        PickingRegistry registry = GenericRenderer.getPickingRegistry();
        RenderableDetails details = (RenderableDetails) registry.lookup(this.canvas.getPixel(e.getX(), e.getY(), true, 8));
        if (details != null) {
            Point2D location = details.retrieveLocation();
            double[][] correctList = findAppropriateList(location);
            if (correctList != null) {
                int index = findIndex(correctList, location);
                mouseOverPoint(e.getPoint(), location, correctList, index);
                return true;
            }
        }
        return false;
    }

    protected double[][] findAppropriateList (final Point2D location) {
        double[][] tempList;
        if (points != null) {
            for (final double[][] pointList : points) {
                tempList = pointList;
                for (double[] entry : pointList) {
                    double x = entry[0], y = entry[1];
                    if (x == location.getX() && y == location.getY()) {
                        return tempList;
                    }
                }
            }
        }
        return null;
    }

    protected int findIndex (final double[][] list, final Point2D location) {
        int index = 0;
        for (double[] entry : list) {
            double x = entry[0], y = entry[1];
            if (x == location.getX() && y == location.getY()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Override
    public void mouseReleased (MouseEvent e) {

    }

    protected boolean isTriggerMouseEvent (MouseEvent e, int method) {
        return false;
    }

    /**
     * Adds this {@link CoordSysViewSelector} as {@link MouseListener} and
     * {@link MouseMotionListener} to the associated canvas.
     *
     * @return this for chaining
     */
    public CoordSysMouseOver register () {
        if (!Arrays.asList(canvas.asComponent().getMouseListeners()).contains(this))
            canvas.asComponent().addMouseListener(this);
        if (!Arrays.asList(canvas.asComponent().getMouseMotionListeners()).contains(this))
            canvas.asComponent().addMouseMotionListener(this);
        return this;
    }

    /**
     * Removes this {@link CoordSysViewSelector} from the associated canvas'
     * mouse and mouse motion listeners.
     *
     * @return this for chaining
     */
    public CoordSysMouseOver deRegister () {
        canvas.asComponent().removeMouseListener(this);
        canvas.asComponent().removeMouseMotionListener(this);
        return this;
    }

    public abstract void mouseOverPoint (Point mouselocation, Point2D pointlocation, double[][] data, int dataIndex);

}
