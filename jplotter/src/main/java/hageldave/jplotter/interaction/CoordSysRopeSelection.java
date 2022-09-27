package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.Renderer;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class CoordSysRopeSelection extends MouseAdapter {
    protected Component canvas;
    protected CoordSysRenderer coordsys;
    protected CompleteRenderer overlay;
    protected Points points = new Points();
    protected Lines lines = new Lines();
    protected List<Point2D.Double> coordinates = new LinkedList<>();
    protected boolean isDone = false;
    protected int radius = 25;

    public CoordSysRopeSelection(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        this.canvas = canvas.asComponent();
        this.coordsys = coordsys;
        Renderer presentRenderer;
        if((presentRenderer = coordsys.getOverlay()) == null){
            coordsys.setOverlay(this.overlay = new CompleteRenderer());
        } else if(presentRenderer instanceof CompleteRenderer){
            this.overlay = (CompleteRenderer) presentRenderer;
        } else {
            throw new IllegalStateException(
                    "The canvas' current overlay renderer is not an instance of CompleteRenderer but "
                            + presentRenderer.getClass().getName() + " which cannot be used with CoordSysAreaSelector.");
        }

        this.overlay
                .addItemToRender(lines).addItemToRender(points);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (coordsys.getCoordSysArea().contains(e.getPoint()) && !isDone) {
            Point2D.Double newPoint = new Point2D.Double(e.getX(), e.getY());
            coordinates.add(newPoint);

            if (coordinates.size() > 1) {
                Point2D.Double firstPoint = coordinates.get(0);
                Point2D.Double lastCoord = coordinates.get(coordinates.size() - 2);
                this.lines.addSegment(new Point2D.Double(lastCoord.getX(), canvas.getHeight() - lastCoord.getY()),
                        new Point2D.Double(e.getX(), canvas.getHeight() - e.getY()));

                if (e.getPoint().distanceSq(firstPoint) < radius) {
                    isDone = true;
                    determineSelection();
                } else {
                    this.points.addPoint(new Point2D.Double(e.getX(), canvas.getHeight()-e.getY())).setColor(Color.BLACK);
                }
            } else {
                this.points.addPoint(new Point2D.Double(e.getX(), canvas.getHeight()-e.getY())).setColor(Color.BLACK);
            }

            canvas.repaint();
        } else if (isDone) {
            isDone = false;
            coordinates.clear();
            points.getPointDetails().clear();
            lines.getSegments().clear();
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!coordinates.isEmpty()) {
            Point2D.Double firstPoint = coordinates.get(0);
            if (e.getPoint().distanceSq(firstPoint) < radius) {
                this.points.getPointDetails().get(0).setScaling(1.3).setColor(Color.RED);
            } else {
                this.points.getPointDetails().get(0).setScaling(1.0).setColor(Color.BLACK);
            }
            canvas.repaint();
        }
    }

    protected void determineSelection() {
        Polygon p = new Polygon();
        for (Point2D.Double coord: coordinates) {
            p.addPoint((int) coord.getX(), (int) coord.getY());
        }
        areaSelected(p);
    }

    protected abstract void areaSelected(Polygon selectedArea);

    /**
     * Adds this {@link CoordSysPanning} as {@link MouseListener} and
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
     * Removes this {@link CoordSysPanning} from the associated canvas'
     * mouse and mouse motion listeners.
     * @return this for chaining
     */
    public CoordSysRopeSelection deRegister(){
        canvas.removeMouseListener(this);
        canvas.removeMouseMotionListener(this);
        return this;
    }
}
