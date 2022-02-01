package hageldave.jplotter.interaction.kml;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.InteractionConstants;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
 *
 */
public class CoordSysScrollZoom implements MouseWheelListener, InteractionConstants {
    protected Component canvas;
    protected CoordSysRenderer coordsys;
    protected double zoomFactor = 1.7;
    protected int axes = X_AXIS | Y_AXIS;
    protected KeyMaskListener keyListenerMask;
    protected boolean mouseFocusedZoom;

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener keyListenerMask, boolean mouseFocusedZoom) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.keyListenerMask = keyListenerMask;
        this.mouseFocusedZoom = mouseFocusedZoom;
    }

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, boolean mouseFocusedZoom) {
        this(canvas, coordsys, new KeyMaskListener(KeyEvent.VK_ALT), mouseFocusedZoom);
    }

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        this(canvas, coordsys, new KeyMaskListener(KeyEvent.VK_ALT), false);
    }

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener keyListenerMask) {
        this(canvas, coordsys, keyListenerMask, false);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (keyListenerMask.isKeysPressed()) {
            if (!coordsys.getCoordSysArea().contains(Utils.swapYAxis(e.getPoint(), canvas.getHeight())))
                return;

            double coordsysPosX = coordsys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight()).getX();
            double coordsysPosY = coordsys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight()).getY();
            int wheelRotation = e.getWheelRotation();
            double zoom = Math.pow(zoomFactor, wheelRotation*0.7);

            // zoom relative to mouse position
            if (mouseFocusedZoom) {
                AffineTransform at = new AffineTransform();
                at.translate(coordsysPosX, coordsysPosY);
                at.scale(zoom, zoom);
                at.translate(-coordsysPosX, -coordsysPosY);

                Rectangle2D.Double r2d = new Rectangle2D.Double(
                        coordsys.getCoordinateView().getX(),
                        coordsys.getCoordinateView().getY(),
                        coordsys.getCoordinateView().getMaxX() - coordsys.getCoordinateView().getX(),
                        coordsys.getCoordinateView().getMaxY() - coordsys.getCoordinateView().getY());
                Shape transformedR2D = at.createTransformedShape(r2d);

                coordsys.setCoordinateView(
                        transformedR2D.getBounds2D().getX(),
                        transformedR2D.getBounds2D().getY(),
                        transformedR2D.getBounds2D().getMaxX(),
                        transformedR2D.getBounds2D().getMaxY()
                );
            // zoom into the center of the coordsys
            } else {
                double centerX = coordsys.getCoordinateView().getCenterX();
                double centerY = coordsys.getCoordinateView().getCenterY();
                double width = coordsys.getCoordinateView().getWidth();
                double height = coordsys.getCoordinateView().getHeight();
                if (( axes & X_AXIS ) != 0)
                    width *= zoom;
                if (( axes & Y_AXIS ) != 0)
                    height *= zoom;
                coordsys.setCoordinateView(
                        centerX - width / 2,
                        centerY - height / 2,
                        centerX + width / 2,
                        centerY + height / 2
                );
            }
            canvas.repaint();
        }
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
        if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyListenerMask))
            canvas.addKeyListener(this.keyListenerMask);
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

    public boolean isMouseFocusedZoom() {
        return mouseFocusedZoom;
    }

    public CoordSysScrollZoom setMouseFocusedZoom(boolean mouseFocusedZoom) {
        this.mouseFocusedZoom = mouseFocusedZoom;
        return this;
    }

    public void setKeyListenerMask(KeyMaskListener keyListenerMask) {
        canvas.removeKeyListener(this.keyListenerMask);
        this.keyListenerMask = keyListenerMask;
        if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyListenerMask))
            canvas.addKeyListener(this.keyListenerMask);
    }

    /**
     * Removes this {@link CoordSysScrollZoom} from the associated canvas'
     * mouse wheel listeners.
     * @return this for chaining
     */
    public CoordSysScrollZoom deRegister(){
        canvas.removeMouseWheelListener(this);
        canvas.removeKeyListener(this.keyListenerMask);
        return this;
    }

}
