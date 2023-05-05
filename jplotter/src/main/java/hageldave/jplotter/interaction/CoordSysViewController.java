package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * The CoordSysViewController extends the {@link ComponentAdapter} class
 * that realizes a window resize behavior, so that the coordinate view doesn't get compressed.
 * Rather than that the proportions should stay the same, so parts of the coordinate view
 * will be hidden when decreasing the window size.
 * A so-called desiredCoordinateView can be set, which means that it will always be visible,
 * independent of the window size. If it's not set manually, it will be the currently active coordinate view by default.
 *
 */
public class CoordSysViewController extends ComponentAdapter {
    protected Component canvas;
    protected CoordSysRenderer coordsys;
    // TODO: this should also be able to be null
    protected Rectangle2D desiredCoordinateView;

    /**
     * Creates a new {@link CoordSysViewController} for the specified canvas and corresponding coordinate system.
     *
     * @param canvas displaying the coordsys
     * @param coordsys the coordinate system to control
     * @param desiredCoordinateView the area of the coordinate system that should always be visible
     */
    public CoordSysViewController(JPlotterCanvas canvas, CoordSysRenderer coordsys, Rectangle2D desiredCoordinateView) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.desiredCoordinateView = desiredCoordinateView;
    }

    public CoordSysViewController(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        Rectangle2D coordView = coordsys.getCoordinateView();
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.desiredCoordinateView = new Rectangle2D.Double(coordView.getX(), coordView.getY(), coordView.getMaxX(), coordView.getMaxY());
    }

    protected void calculateViewport() {
        double canvasWidth = this.canvas.getWidth();
        double canvasHeight = this.canvas.getHeight();

        Rectangle2D contentBounds = this.desiredCoordinateView;
        double contentAspect = contentBounds.getWidth() / contentBounds.getHeight();
        double viewportAspect = canvasWidth / canvasHeight;
        double w,h,x,y;
        if(viewportAspect < contentAspect) { // taller viewport
            w = contentBounds.getWidth();
            h = w/viewportAspect;
        } else { // wider viewport
            h = contentBounds.getHeight();
            w = h*viewportAspect;
        }
        x = contentBounds.getMinX()-(w-contentBounds.getWidth())/2;
        y = contentBounds.getMinY()-(h-contentBounds.getHeight())/2;
        this.coordsys.setCoordinateView(x, y, w, h);
    }

    @Override
    public void componentResized(ComponentEvent e) {
        super.componentResized(e);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                calculateViewport();
            }
        });
    }

    /**
     * @return the desiredCoordinateView, which is the minimal always visible area of the coordinate view
     * When resizing the window that part won't be hidden, but downscaled
     */
    public Rectangle2D getDesiredCoordinateView() {
        return desiredCoordinateView;
    }

    /**
     * Sets a new desiredCoordinateView
     *
     * @param desiredCoordinateView the minimal always visible area of the coordinate view
     */
    public void setDesiredCoordinateView(Rectangle2D desiredCoordinateView) {
        this.desiredCoordinateView = desiredCoordinateView;
    }

    /**
     * Adds this {@link CoordSysViewController} as {@link ComponentAdapter} to the associated canvas.
     * @return this for chaining
     */
    public CoordSysViewController register() {
        if(!Arrays.asList(canvas.getComponentListeners()).contains(this))
            canvas.addComponentListener(this);
        return this;
    }

    /**
     * Removes this {@link CoordSysViewController} from the associated canvas'
     * component adapter listeners.
     * @return this for chaining
     */
    public CoordSysViewController deregister() {
        canvas.removeComponentListener(this);
        return this;
    }
}