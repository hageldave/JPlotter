package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.geom.Rectangle2D;

public class DefaultViewController implements CoordSysViewController {
    protected final CoordSysRenderer coordsys;

    /**
     *
     * @param coordsys
     */
    public DefaultViewController(CoordSysRenderer coordsys) {
        this.coordsys = coordsys;
    }

    /**
     *
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     */
    @Override
    public void setDesiredView(double minX, double minY, double maxX, double maxY) {
        this.coordsys.setCoordinateViewObject(new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY));
    }
}
