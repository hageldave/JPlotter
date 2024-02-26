package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.event.InputEvent;

/**
 * The {@link CoordSysRectangleZoom} is an implementation of the {@link CoordSysViewSelector}
 * that zooms into the selected area by default.
 * <br/>
 * Intended use:
 * <pre>
 * new CoordSysRectangleZoom(canvas, coordsys).register();
 * </pre>
 * <p>
 * Per default the extended modifier mask for a dragging mouse event to trigger
 * selection is {@link InputEvent#SHIFT_DOWN_MASK}.
 * If this is undesired the {@link #extModifierMask} has to be overridden.<br>
 * You may also want to not trigger selection when other modifiers are present. E.g.
 * when CTRL {@link InputEvent#CTRL_DOWN_MASK} is pressed, don't select because CTRL
 * is already meant for panning.
 * 
 * @deprecated Replaced by {@link hageldave.jplotter.interaction.kml.CoordSysRectangleZoom}
 */
@Deprecated
public class CoordSysRectangleZoom extends CoordSysViewSelector {

    public CoordSysRectangleZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        super(canvas, coordsys);
    }

    @Override
    public void areaSelected(double minX, double minY, double maxX, double maxY) {
        this.coordsys.setCoordinateView(minX, minY, maxX, maxY);
    }
}