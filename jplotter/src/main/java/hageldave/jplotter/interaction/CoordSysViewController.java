package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;


/**
 *
 */
public interface CoordSysViewController {
    /**
     *
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     */
    public void setDesiredView(double minX, double minY, double maxX, double maxY);
}