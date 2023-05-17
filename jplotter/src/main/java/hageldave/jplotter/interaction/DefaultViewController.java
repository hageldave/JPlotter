package hageldave.jplotter.interaction;

import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.geom.Rectangle2D;

/**
 * The DefaultViewController is the default implementation of the {@link CoordSysViewController}.
 * It is used by the {@link CoordSysRenderer} as the default CoordSysViewController.
 * Therefore, the DefaultViewController just passes the coordinate view update calls to the given coordinate system,
 * without any modifications.
 */
public class DefaultViewController implements CoordSysViewController {
    protected final CoordSysRenderer coordsys;

    /**
     * Creates a new {@link KeepAspectRatioController} for the given coordinate system.
     *
     * @param coordsys the coordinate system to control
     */
    public DefaultViewController(CoordSysRenderer coordsys) {
        this.coordsys = coordsys;
    }

    /**
     * Passes the view coordinates to the {@link CoordSysRenderer}.
     *
     * @param minX minimal x-coordinate of the viewport
     * @param minY minimal y-coordinate of the viewport
     * @param maxX maximal x-coordinate of the viewport
     * @param maxY maximal y-coordinate of the viewport
     */
    @Override
    public void setDesiredView(double minX, double minY, double maxX, double maxY) {
        this.coordsys.setCoordinateViewObject(new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY));
    }
}
