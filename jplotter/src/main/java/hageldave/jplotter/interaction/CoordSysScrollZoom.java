package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;

/**
 * The CoordSysScrollZoom class implements a {@link MouseWheelListener}
 * that realize zooming functionality for the coordinate view of the {@link CoordSysRenderer}.
 * When registering this with an {@link JPlotterCanvas} and corresponding {@link CoordSysRenderer} turning the scroll wheel zooms into or out of
 * the current coordinate system view.
 * The zoom factor can be set and is by default 2.0.
 * <p>
 * Intended use: {@code CoordSysScrollZoom zoom = new CoordSysScrollZoom(canvas, coordsys).register(); }
 *
 * @author hageldave
 */
public class CoordSysScrollZoom implements MouseWheelListener, InteractionConstants {

    protected Component canvas;
    protected CoordSysRenderer coordsys;
    protected double zoomFactor = 2;
    protected int axes = X_AXIS | Y_AXIS;

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
    }


    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(!coordsys.getCoordSysArea().contains(Utils.swapYAxis(e.getPoint(), canvas.getHeight())))
            return;

        int wheelRotation = e.getWheelRotation();
        double zoom = Math.pow(zoomFactor, wheelRotation);
        double centerX = coordsys.getCoordinateView().getCenterX();
        double centerY = coordsys.getCoordinateView().getCenterY();
        double width = coordsys.getCoordinateView().getWidth();
        double height = coordsys.getCoordinateView().getHeight();
        if((axes & X_AXIS) != 0)
            width *= zoom;
        if((axes & Y_AXIS) != 0)
            height *= zoom;
        coordsys.setCoordinateView(
                centerX-width/2,
                centerY-height/2,
                centerX+width/2,
                centerY+height/2
        );
        canvas.repaint();
    }

    /**
     * Sets the zoom factor of this {@link CoordSysScrollZoom}.
     * The default value is 2.0.
     * Using a value in ]0,1[ will reverse the zoom direction.
     * @param zoomFactor to be set
     * @return this for chaining
     */
    public CoordSysScrollZoom setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
        return this;
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    /**
     * Adds this {@link CoordSysScrollZoom} as {@link MouseWheelListener} to the associated canvas.
     * @return this for chaining
     */
    public CoordSysScrollZoom register(){
        if( ! Arrays.asList(canvas.getMouseWheelListeners()).contains(this))
            canvas.addMouseWheelListener(this);
        return this;
    }

    /**
     * Sets the axes to which this scroll zoom is applied.
     * Default are both x and y axis.
     * @param axes {@link InteractionConstants#X_AXIS}, {@link InteractionConstants#Y_AXIS} or {@code X_AXIS|Y_AXIS}
     * @return this for chaining
     */
    public CoordSysScrollZoom setZoomedAxes(int axes){
        this.axes = axes;
        return this;
    }

    /**
     * @return the axes this scroll zoom applies to, i.e.
     * {@link InteractionConstants#X_AXIS}, {@link InteractionConstants#Y_AXIS} or {@code X_AXIS|Y_AXIS}
     */
    public int getZoomedAxes() {
        return axes;
    }

    /**
     * Removes this {@link CoordSysScrollZoom} from the associated canvas'
     * mouse wheel listeners.
     * @return this for chaining
     */
    public CoordSysScrollZoom deRegister(){
        canvas.removeMouseWheelListener(this);
        return this;
    }

}