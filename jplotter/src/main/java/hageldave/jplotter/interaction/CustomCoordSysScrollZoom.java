package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.KeyEvent;
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
 *
 * TODO seems better, needs discussion
 */
public class CustomCoordSysScrollZoom implements MouseWheelListener, InteractionConstants {

    protected Component canvas;
    protected CoordSysRenderer coordsys;
    protected double zoomFactor = 2;
    protected int axes = X_AXIS | Y_AXIS;
    protected KeyListenerMask keyListenerMask;

    public CustomCoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyListenerMask keyListenerMask) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        this.keyListenerMask = keyListenerMask;
    }

    public CustomCoordSysScrollZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        this(canvas, coordsys, new KeyListenerMask(KeyEvent.VK_ALT));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (keyListenerMask.isKeyTyped()) {
            if (!coordsys.getCoordSysArea().contains(Utils.swapYAxis(e.getPoint(), canvas.getHeight())))
                return;

            // Warning: this is not pretty
            double coordSysAreaLength = coordsys.getCoordSysArea().getWidth();
            double coordViewLength = coordsys.getCoordinateView().getWidth();

            double coordSysAreaHeight = coordsys.getCoordSysArea().getHeight();
            double coordViewHeight = coordsys.getCoordinateView().getHeight();

            double factorX = coordViewLength/coordSysAreaLength;
            double factorY = coordViewHeight/coordSysAreaHeight;


            // TODO this one does it
            double posInCoordSysX = coordsys.getCoordinateView().getMinX() + ((e.getX() - coordsys.getCoordSysArea().getMinX())*factorX);
            double posInCoordSysY = coordsys.getCoordinateView().getMaxY() - ((e.getY() - (coordsys.getCoordSysArea().getMinY() - 50))*factorY);

            // also vielleicht doch mauspos bei screen capturen?!
            /*System.out.println(canvas.getLocationOnScreen());
            System.out.println("-------------------------");
            System.out.println(canvas.getBounds());
            System.out.println(coordsys.getCurrentViewPort());
            //System.out.println(coordsys());
            System.out.println("offsetUpper: " + coordsys.getCoordSysArea().getBounds());
            System.out.println("offsetUpper: " + coordsys.getCoordSysArea().getMinY());
            System.out.println("mousePosRaw: " + e.getY());
            System.out.println("test: " + (e.getY() - coordsys.getCoordSysArea().getMinY())*factorY);
            System.out.println("posY: " + posInCoordSysY);

            // wenn zu nah rangezoomt gehts nicht mehr*/

            int wheelRotation = e.getWheelRotation();
            double zoom = Math.pow(zoomFactor, wheelRotation);
            double centerX = ((coordsys.getCoordinateView().getCenterX() * 0.45) + (posInCoordSysX * 0.55));
            double centerY = ((coordsys.getCoordinateView().getCenterY() * 0.45) + (posInCoordSysY * 0.55));


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
    public CustomCoordSysScrollZoom setZoomFactor(double zoomFactor) {
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
    public CustomCoordSysScrollZoom register(){
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
    public CustomCoordSysScrollZoom setZoomedAxes(int axes){
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

    public void setKeyListenerMask(KeyListenerMask keyListenerMask) {
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
    public CustomCoordSysScrollZoom deRegister(){
        canvas.removeMouseWheelListener(this);
        canvas.removeKeyListener(this.keyListenerMask);
        return this;
    }

}
