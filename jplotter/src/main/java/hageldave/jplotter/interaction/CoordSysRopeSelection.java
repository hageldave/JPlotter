package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The CoordSysRopeSelection class implements a {@link MouseListener}
 * and {@link MouseMotionListener} that realize rope selection functionality
 * for the coordinate view of the {@link CoordSysRenderer}.
 * So it provides a way to select an area in a coordsys by selecting multiple points.
 * The action to be performed with the selected region is up to
 * the implementation of the methods {@link #areaSelected(Path2D)}
 * and {@link #areaSelectedOnGoing(Path2D)}.
 * <p>
 * Intended use, (for example checking if point is contained):
 * <pre>
 * new CoordSysRopeSelection(canvas, coordsys) {
 *    public void areaSelected(Path2D selectedArea) {
 *       selectedArea.contains(pointToCheck);
 *    }
 * }.register();
 * </pre>
 * <p>

 */
public abstract class CoordSysRopeSelection extends MouseAdapter {
    protected Component canvas;
    protected CoordSysRenderer coordSys;
    protected final CompleteRenderer overlay = new CompleteRenderer();
    protected final Points points = new Points();
    protected final Lines lines = new Lines();
    protected final List<Point2D.Double> coordinates = new LinkedList<>();
    protected boolean isDone = false;
    protected int radius = 25;

    /**
     * Creates an CoordSysRopeSelection instance for the specified canvas and corresponding coordinate system.
     *
     * @param canvas displaying the coordsys
     * @param coordSys the coordinate system to apply the panning in
     */
    public CoordSysRopeSelection(JPlotterCanvas canvas, CoordSysRenderer coordSys) {
        this.canvas = canvas.asComponent();
        this.coordSys = coordSys;
        this.overlay.addItemToRender(lines).addItemToRender(points);
        this.coordSys.setContent(this.coordSys.getContent().withAppended(this.overlay));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (coordSys.getCoordSysArea().contains(e.getPoint())) {
            if (!isDone) {
                Point2D pointInCoordsys = coordSys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight());

                coordinates.add((Point2D.Double) pointInCoordsys);
                if (coordinates.size() > 1) {
                    Point2D.Double firstPoint = coordinates.get(0);
                    Point2D.Double lastCoord = coordinates.get(coordinates.size() - 2);
                    this.lines.addSegment(lastCoord, pointInCoordsys);

                    Point2D pointinAWT = coordSys.transformCoordSys2AWT(firstPoint, canvas.getHeight());
                    if (e.getPoint().distanceSq(pointinAWT) < radius) {
                        isDone = true;
                        canvas.repaint();

                        // call selected interface
                        areaSelected(calculateSelectedArea());
                    } else {
                        this.points.addPoint(pointInCoordsys).setColor(Color.BLACK);

                        // call ongoing interface
                        areaSelectedOnGoing(calculateSelectedArea());
                    }
                } else {
                    this.points.addPoint(pointInCoordsys).setColor(Color.BLACK);
                }
            } else {
                isDone = false;
                coordinates.clear();
                points.getPointDetails().clear();
                lines.getSegments().clear();
            }
        }
        canvas.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!coordinates.isEmpty()) {
            Point2D.Double firstPoint = coordinates.get(0);
            Point2D pointInAWT = coordSys.transformCoordSys2AWT(firstPoint, canvas.getHeight());
            if (e.getPoint().distanceSq(pointInAWT) < radius) {
                this.points.getPointDetails().get(0).setScaling(1.3).setColor(Color.RED);
            } else {
                this.points.getPointDetails().get(0).setScaling(1.0).setColor(Color.BLACK);
            }
            canvas.repaint();
        }
    }

    protected Path2D calculateSelectedArea() {
        Path2D selectedArea = new Path2D.Double();
        selectedArea.moveTo(coordinates.get(0).getX(), coordinates.get(0).getY());
        for (int i = 1; i < coordinates.size(); i++) {
            selectedArea.lineTo(coordinates.get(i).getX(), coordinates.get(i).getY());
        }
        selectedArea.closePath();
        return selectedArea;
    }

    /**
     * Will be called when selection is done (first point connected to the last point).
     * @param selectedArea represents the selected points in the coordsys
     *                      ({@link Path2D#contains(double, double)} method can be used to check if points are inside the selected area)
     */
    public abstract void areaSelected(Path2D selectedArea);

    /**
     * Reports on the area currently selected when start and end points haven't been connected.
     * When selection is finished (mouse button released),
     * {@link #areaSelected(Path2D)}
     * will be called.
     *
     * @param selectedArea represents the selected points in the coordsys
     *                      ({@link Path2D#contains(double, double)} method can be used to check if points are inside the selected area)
     */
    public void areaSelectedOnGoing(Path2D selectedArea) {

    }

    /**
     * Adds this {@link CoordSysRopeSelection} as {@link MouseListener} and
     * {@link MouseMotionListener} to the associated canvas.
     * @return this for chaining
     */
    public CoordSysRopeSelection register(){
        if( ! Arrays.asList(canvas.getMouseListeners()).contains(this))
            canvas.addMouseListener(this);
        if( ! Arrays.asList(canvas.getMouseMotionListeners()).contains(this))
            canvas.addMouseMotionListener(this);
        return this;
    }

    /**
     * Removes this {@link CoordSysRopeSelection} from the associated canvas'
     * mouse and mouse motion listeners.
     * @return this for chaining
     */
    public CoordSysRopeSelection deRegister(){
        canvas.removeMouseListener(this);
        canvas.removeMouseMotionListener(this);
        return this;
    }
}