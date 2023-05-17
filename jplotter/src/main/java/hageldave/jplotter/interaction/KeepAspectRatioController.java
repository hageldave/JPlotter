package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
public class KeepAspectRatioController extends ComponentAdapter implements CoordSysViewController {
    protected final Component canvas;
    protected final CoordSysRenderer coordsys;
    protected Rectangle2D desiredCoordinateView;

    /**
     * Creates a new {@link KeepAspectRatioController} for the specified canvas and corresponding coordinate system.
     *
     * @param canvas displaying the coordsys
     * @param coordsys the coordinate system to control
     * @param desiredCoordinateView the area of the coordinate system that should always be visible
     */
    public KeepAspectRatioController(JPlotterCanvas canvas, CoordSysRenderer coordsys, Rectangle2D desiredCoordinateView) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.desiredCoordinateView = desiredCoordinateView;
    }

    /**
     * Creates a new {@link KeepAspectRatioController} for the specified canvas and corresponding coordinate system.
     * It uses the current coordinate view of the {@link CoordSysRenderer} as the default {@link #desiredCoordinateView}.
     *
     * @param canvas displaying the coordsys
     * @param coordsys the coordinate system to control
     */
    public KeepAspectRatioController(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        Rectangle2D coordView = coordsys.getCoordinateView();
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.desiredCoordinateView = new Rectangle2D.Double(coordView.getMinX(), coordView.getMinY(), coordView.getMaxX(), coordView.getMaxY());
    }

    protected void calculateViewport(Rectangle2D desiredCoordinateView) {
        double canvasWidth = this.canvas.getWidth();
        double canvasHeight = this.canvas.getHeight();
        double contentAspect = desiredCoordinateView.getWidth() / desiredCoordinateView.getHeight();
        double viewportAspect = canvasWidth / canvasHeight;
        double w,h,x,y;

        if(viewportAspect < contentAspect) { // taller viewport
            w = desiredCoordinateView.getWidth();
            h = w/viewportAspect;
        } else { // wider viewport
            h = desiredCoordinateView.getHeight();
            w = h*viewportAspect;
        }
        x = desiredCoordinateView.getMinX()-(w-desiredCoordinateView.getWidth())/2;
        y = desiredCoordinateView.getMinY()-(h-desiredCoordinateView.getHeight())/2;
        this.coordsys.setCoordinateViewObject(new Rectangle2D.Double(x, y, w, h));
    }

    @Override
    public void componentResized(ComponentEvent e) {
        super.componentResized(e);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                calculateViewport(desiredCoordinateView);
            }
        });
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

    /**
     * Sets a new desiredCoordinateView and updates the {@link CoordSysRenderer#coordinateView} of the given CoordSysRenderer.
     *
     * @param minX minimal x-coordinate of the viewport
     * @param minY minimal y-coordinate of the viewport
     * @param maxX maximal x-coordinate of the viewport
     * @param maxY maximal y-coordinate of the viewport
     */
    @Override
    public void setDesiredView(double minX, double minY, double maxX, double maxY) {
        Rectangle2D desiredView = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
        this.desiredCoordinateView = desiredView;
        this.calculateViewport(desiredView);
    }
}
