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


public interface CoordSysViewController {
    public default void setDesiredView(double minX, double minY, double maxX, double maxY) {
        this.setDesiredView(new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY));
    }

    public void setDesiredView(Rectangle2D desiredCoordinateView);
}