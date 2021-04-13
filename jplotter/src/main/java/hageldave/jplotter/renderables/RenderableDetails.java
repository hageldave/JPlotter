package hageldave.jplotter.renderables;

import java.awt.geom.Point2D;

/**
 *
 * @author lucareichmann
 */
public interface RenderableDetails {
    public RenderableDetails setPickColor(int pickID);

    // TODO needs to be rewritten, as a single point is not applicable for lines, curves,...
    public Point2D retrieveLocation();
}
