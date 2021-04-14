package hageldave.jplotter.renderables;

import hageldave.jplotter.util.PickingRegistry;

import java.awt.geom.Point2D;

/**
 * TODO documenation needs to be added
 *
 * @author lucareichmann
 */
public interface RenderableDetails {

    /**
     * Sets the pick color in the {@link PickingRegistry}
     * @param pickID picking color of the RenderableDetails (see {@link RenderableDetails} for details)
     * @return this for chaining
     */
    public RenderableDetails setPickColor(int pickID);

    // TODO maybe needs to be rewritten, as a single point is not applicable for lines, curves,...
    public Point2D retrieveLocation();
}
