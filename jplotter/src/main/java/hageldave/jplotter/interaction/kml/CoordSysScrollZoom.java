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
 * When registering this with an {@link JPlotterCanvas} and corresponding {@link CoordSysRenderer} turning the scroll wheel while holding down the ALT key zooms into or out of
 * the current coordinate system view.
 * The zoom factor can be set and is by default 2.0.
 * <p>
 * There are two different zoom modes: One where the center of the zoom is always the center of the current viewport (default) and
 * another where the center of the zoom is the current position of the mouse.
 * The zoom mode can be switched via {@link CoordSysScrollZoom#mouseFocusedZoom}.
 * <p>
 * Intended use: {@code CoordSysScrollZoom zoom = new CoordSysScrollZoom(canvas, coordsys).register(); }
 * <p>
 * Per default the key for a dragging mouse event to trigger
 * zooming is {@link KeyEvent#VK_ALT}.
 * If this is undesired a {@link KeyMaskListener} has to be passed in the constructor.<br>
 * For example to not need to press any key:
 * <pre>new CoordSysPanning(canvas, coordsys, new KeyMaskListener()).register();</pre>
 *
 * @author hageldave
 *
 */
public class CoordSysScrollZoom implements MouseWheelListener, InteractionConstants {
    protected Component canvas;
    protected CoordSysRenderer coordsys;
    protected double zoomFactor = 1.7;
    protected int axes = X_AXIS | Y_AXIS;
    protected KeyMaskListener keyMaskListener;
    protected boolean mouseFocusedZoom;

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener keyMaskListener, boolean mouseFocusedZoom) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.keyMaskListener = keyMaskListener;
        this.mouseFocusedZoom = mouseFocusedZoom;
    }

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, boolean mouseFocusedZoom) {
        this(canvas, coordsys, new KeyMaskListener(KeyEvent.VK_ALT), mouseFocusedZoom);
    }

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        this(canvas, coordsys, new KeyMaskListener(KeyEvent.VK_ALT), false);
    }

    public CoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener keyMaskListener) {
        this(canvas, coordsys, keyMaskListener, false);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (keyMaskListener.areKeysPressed()) {
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
        if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
            canvas.addKeyListener(this.keyMaskListener);
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
     * @return if mouse-focused zoom is enabled
     */
    public boolean isMouseFocusedZoom() {
        return mouseFocusedZoom;
    }

    /**
     * Changes the zooming mode. With the mouse-focused zoom mode enabled the CoordSysScrollZoom always zooms "into" the mouse position,
     * whereas when disabled it zooms into the center of the current viewport.
     *
     * @param mouseFocusedZoom toggles mouse focused zoom
     * @return this for chaining
     */
    public CoordSysScrollZoom setMouseFocusedZoom(boolean mouseFocusedZoom) {
        this.mouseFocusedZoom = mouseFocusedZoom;
        return this;
    }

    /**
     * Sets a new {@link KeyMaskListener}, removes the old KeyMaskListener from the canvas
     * and registers the new one.
     *
     * @param keyMaskListener defines the set of keys that have to pressed during the panning
     */
    public CoordSysScrollZoom setKeyMaskListener(KeyMaskListener keyMaskListener) {
        canvas.removeKeyListener(this.keyMaskListener);
        this.keyMaskListener = keyMaskListener;
        if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
            canvas.addKeyListener(this.keyMaskListener);
        return this;
    }

    /**
     * Removes this {@link CoordSysScrollZoom} from the associated canvas'
     * mouse wheel listeners.
     * @return this for chaining
     */
    public CoordSysScrollZoom deRegister(){
        canvas.removeMouseWheelListener(this);
        canvas.removeKeyListener(this.keyMaskListener);
        return this;
    }

}
