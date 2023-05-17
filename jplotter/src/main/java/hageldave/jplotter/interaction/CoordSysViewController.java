package hageldave.jplotter.interaction;

import hageldave.jplotter.renderers.CoordSysRenderer;


/**
 * The CoordSysViewController interface provides the {@link #setDesiredView(double, double, double, double)} method,
 * which is used to define a viewport that should be visible by the coordsys.
 * A possible implementation of the CoordSysViewController, like {@link KeepAspectRatioController} controls the
 * viewport of the coordinate system of the {@link CoordSysRenderer}.
 * Depending on the implementation of the controller the behavior of the viewport changes can be different.
 */
public interface CoordSysViewController {
    /**
     * Sets the "desired" viewport of the controller.
     *
     * @param minX minimal x-coordinate of the viewport
     * @param minY minimal y-coordinate of the viewport
     * @param maxX maximal x-coordinate of the viewport
     * @param maxY maximal y-coordinate of the viewport
     */
    void setDesiredView(double minX, double minY, double maxX, double maxY);
}