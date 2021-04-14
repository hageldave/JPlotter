package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.RenderableDetails;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.GenericRenderer;
import hageldave.jplotter.util.PickingRegistry;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * The CoordSysMouseOver class realizes a functionality that enables
 * recognizing the clicked point in the given datasets.
 * <p>
 * It contains an abstract method mouseOverPoint
 * which is called when a point in the dataSet is clicked on.
 * It's behavior has to be implemented by the developer.
 */
public abstract class CoordSysMouseOver extends MouseAdapter {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected LinkedList<double[][]> points;
    protected int index;
    protected double[][] dataSet;

    // TODO might be removed
    // protected int extModifierMask = InputEvent.SHIFT_DOWN_MASK;
    // protected final LinkedList<Integer> extModifierMaskExcludes = new LinkedList<Integer>();

    /**
     * @param canvas        the canvas the CoordSysMouseOver will used with
     * @param coordsys      the coordinate system the CoordSysMouseOver will used with
     * @param allDataPoints will be searched for a data point similar to the clicked location
     */
    public CoordSysMouseOver(JPlotterCanvas canvas, CoordSysRenderer coordsys,
                             LinkedList<double[][]> allDataPoints) {
        this.canvas = canvas;
        this.coordsys = coordsys;
        this.points = allDataPoints;
        this.index = 0;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!findPoints(e)) {
            System.out.println("No data point found in your dataset");
        }
        this.index = 0;
    }

    /**
     * Searches for a data point similar to the location the developer clicked on.
     *
     * @param e MouseEvent when clicking
     * @return true if a point was found, false if no point was found in the dataSet
     */
    protected boolean findPoints(final MouseEvent e) {
        PickingRegistry<RenderableDetails> registry = GenericRenderer.getPickingRegistry();
        RenderableDetails details = registry.lookup(this.canvas.getPixel(e.getX(), e.getY(), true, 5));
        if (details != null) {
            Point2D location = details.retrieveLocation();

            // TODO remove later
            // System.out.println(details.getClass());
            this.dataSet = getListAndSetIndex(location);
            if (this.dataSet != null) {
                mouseOverPoint(e.getPoint(), location, this.dataSet, this.index);
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the data array which contains the clicked-on data point.
     *
     * @param location clicked in the coordinate system
     * @return list where the data point was found
     */
    protected double[][] getListAndSetIndex(final Point2D location) {
        double[][] tempList;
        if (points != null) {
            for (final double[][] pointList : points) {
                tempList = pointList;
                for (double[] entry : pointList) {
                    double x = entry[0], y = entry[1];
                    if (x == location.getX() && y == location.getY()) {
                        return tempList;
                    }
                    this.index++;
                }
                this.index = 0;
            }
        }
        return null;
    }

    /**
     * Adds this {@link CoordSysViewSelector} as {@link MouseListener} and
     * {@link MouseMotionListener} to the associated canvas.
     *
     * @return this for chaining
     */
    public CoordSysMouseOver register() {
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
    public CoordSysMouseOver deRegister() {
        canvas.asComponent().removeMouseListener(this);
        canvas.asComponent().removeMouseMotionListener(this);
        return this;
    }

    /**
     * Will be called, when a data point is clicked on.
     *
     * @param mouseLocation location that was clicked
     * @param pointLocation location of the clicked point in the coordinate system
     * @param data          the data array where the data point was found
     * @param dataIndex     the index of the data point in the returned array
     */
    public abstract void mouseOverPoint(final Point mouseLocation, final Point2D pointLocation,
                                        final double[][] data, final int dataIndex);
}
