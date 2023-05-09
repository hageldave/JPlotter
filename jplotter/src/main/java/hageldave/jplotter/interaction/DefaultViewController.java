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

    @Override
    public void setDesiredView(Rectangle2D desiredCoordinateView) {
        this.coordsys.setCoordinateViewObject(desiredCoordinateView);
    }
}
