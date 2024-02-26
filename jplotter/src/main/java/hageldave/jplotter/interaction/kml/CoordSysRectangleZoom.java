package hageldave.jplotter.interaction.kml;

import java.awt.event.KeyEvent;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;

/**
 * The {@link CoordSysRectangleZoom} is an implementation of the {@link CoordSysViewSelector}
 * that zooms into the selected area by default.
 * <br/>
 * Intended use:
 * <pre>
 * new CoordSysRectangleZoom(canvas, coordsys).register();
 * </pre>
 * <p>
 * Per default the key event for a dragging mouse event to trigger
 * selection is {@link KeyEvent#VK_SHIFT}.
 * If this is undesired a {@link KeyMaskListener} has to be passed in the constructor.<br>
 * You may also want to not trigger selection when other modifiers are present. E.g.
 * when CTRL {@link KeyEvent#VK_CONTROL} is pressed, don't select because CTRL
 * is already meant for panning.
 */
public class CoordSysRectangleZoom extends CoordSysViewSelector {

	/**
	 * Creates a new {@link CoordSysRectangleZoom} for the specified canvas and corresponding coordinate system.
	 * @param canvas displaying the coordsys
	 * @param coordsys the coordinate system to apply the view selection in
	 * @param kml defines the set of keys that have to pressed during the view selection
	 */
    public CoordSysRectangleZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener kml) {
        super(canvas, coordsys, kml);
    }
    
    /**
	 * Creates a new {@link CoordSysRectangleZoom} for the specified canvas and corresponding coordinate system.
	 * @param canvas displaying the coordsys
	 * @param coordsys the coordinate system to apply the view selection in
	 */
    public CoordSysRectangleZoom(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        super(canvas, coordsys);
    }

    @Override
    public void areaSelected(double minX, double minY, double maxX, double maxY) {
        this.coordsys.setCoordinateView(minX, minY, maxX, maxY);
    }
}
