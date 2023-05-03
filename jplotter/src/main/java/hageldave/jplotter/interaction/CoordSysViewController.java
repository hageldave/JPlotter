package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * TODO
 */
public class CoordSysViewController extends ComponentAdapter {
    protected Component canvas;
    protected CoordSysRenderer coordsys;
    Rectangle2D desiredCoordinateView;

    /**
     * TODO
     * @param canvas
     * @param coordsys
     * @param desiredCoordinateView
     */
    public CoordSysViewController(JPlotterCanvas canvas, CoordSysRenderer coordsys, Rectangle2D desiredCoordinateView) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.desiredCoordinateView = desiredCoordinateView;
    }

    protected void calculateViewport() {
        double canvasWidth = this.canvas.getWidth();
        double canvasHeight = this.canvas.getHeight();

        Rectangle2D contentBounds = this.desiredCoordinateView;
        double contentAspect = contentBounds.getWidth() / contentBounds.getHeight();
        double viewpAspect = canvasWidth * 1.0 / canvasHeight;
        double w,h,x,y;
        if(viewpAspect < contentAspect) { // taller viewport
            w = contentBounds.getWidth();
            h = w/viewpAspect;
        } else { // wider viewport
            h = contentBounds.getHeight();
            w = h*viewpAspect;
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