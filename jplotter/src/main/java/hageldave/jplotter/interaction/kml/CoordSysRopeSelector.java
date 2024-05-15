package hageldave.jplotter.interaction.kml;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.interaction.SimpleSelectionModel;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;

/**
 * The CoordSysRopeSelector class implements a {@link MouseListener}
 * and {@link MouseMotionListener} that realize rope selection functionality
 * for the coordinate view of the {@link CoordSysRenderer}.
 * So it provides a way to select an area in a coordsys by selecting multiple points.
 * A selection can always be aborted by performing a right-click.
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
 */
public abstract class CoordSysRopeSelector extends MouseAdapter implements KeyListener {
    protected Component canvas;
    protected CoordSysRenderer coordSys;
    protected ColorScheme colorScheme;
    protected KeyMaskListener keyMaskListener;
    protected final CompleteRenderer overlay = new CompleteRenderer();
    protected final Points points = new Points();
    protected final Lines lines = new Lines();
    protected boolean isDone = false;
    protected int radius = 25;
    protected Lines.SegmentDetails hoverIndicator = null;
    protected SimpleSelectionModel<List<Point2D.Double>> selectionModel = new SimpleSelectionModel<>(Comparator.comparingInt(List::hashCode));

    /**
     * Creates an CoordSysRopeSelection instance for the specified canvas and corresponding coordinate system.
     *
     * @param canvas          displaying the coordsys
     * @param coordSys        the coordinate system to apply the panning in
     * @param keyMaskListener defines the set of keys that have to pressed during the selection
     */
    public CoordSysRopeSelector(JPlotterCanvas canvas, CoordSysRenderer coordSys, KeyMaskListener keyMaskListener) {
        this.canvas = canvas.asComponent();
        this.coordSys = coordSys;
        this.colorScheme = coordSys.getColorScheme();
        this.keyMaskListener = keyMaskListener;
        this.overlay.addItemToRender(lines).addItemToRender(points);
        this.coordSys.setContent(this.coordSys.getContent().withAppended(this.overlay));

        List<List<Point2D.Double>> initialSelection = new ArrayList<>();
        initialSelection.add(new ArrayList<>());
        selectionModel.setSelection(initialSelection);

        selectionModel.addSelectionListener(e -> {
            if (!e.isEmpty()) drawSelection();
        });
    }

    public CoordSysRopeSelector(JPlotterCanvas canvas, CoordSysRenderer coordSys) {
        this(canvas, coordSys, new KeyMaskListener(KeyEvent.VK_R));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (keyMaskListener.areKeysPressed() && coordSys.getCoordSysArea().contains(e.getPoint())) {
            if (isDone || SwingUtilities.isRightMouseButton(e)) {
                clearSelectionResources();
            } else if (!isDone) {
                Point2D pointInCoordsys = coordSys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight());
                List<Point2D.Double> currentSelection = new LinkedList<>(selectionModel.getSelection().first());
                currentSelection.add((Point2D.Double) pointInCoordsys);
                selectionModel.setSelection(currentSelection);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!selectionModel.getSelection().isEmpty() && !selectionModel.getSelection().first().isEmpty() && keyMaskListener.areKeysPressed() && !isDone) {
            Point2D.Double firstPoint = selectionModel.getSelection().first().get(0);
            Point2D pointInAWT = coordSys.transformCoordSys2AWT(firstPoint, canvas.getHeight());
            if (e.getPoint().distanceSq(pointInAWT) < radius) {
                this.points.getPointDetails().get(0).setColor(Color.RED);
            } else {
                this.points.getPointDetails().get(0).setColor(colorScheme.getColor1());
            }

            if (Objects.nonNull(hoverIndicator)) {
                this.lines.getSegments().remove(hoverIndicator);
            }
            Point2D.Double lastPoint = selectionModel.getSelection().first().get(selectionModel.getSelection().first().size()-1);
            hoverIndicator = this.lines.addSegment(lastPoint, coordSys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight())).setColor(colorScheme.getColor2());
            canvas.repaint();
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else if (!selectionModel.getSelection().isEmpty() && !isDone) {
            this.lines.getSegments().remove(hoverIndicator);
            hoverIndicator = null;
            canvas.repaint();
        }

        changeCursor();
    }


    protected Path2D calculateSelectedArea() {
        Path2D selectedArea = new Path2D.Double();
        selectedArea.moveTo(selectionModel.getFirstOrDefault(null).get(0).getX(), selectionModel.getSelection().first().get(0).getY());
        for (int i = 1; i < selectionModel.getFirstOrDefault(null).size(); i++) {
            selectedArea.lineTo(selectionModel.getSelection().first().get(i).getX(), selectionModel.getSelection().first().get(i).getY());
        }
        selectedArea.closePath();
        return selectedArea;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        changeCursor();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        changeCursor();
    }

    private void changeCursor() {
        if (keyMaskListener.areKeysPressed() && isDone) {
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        canvas.repaint();
    }

    /**
     * Sets a rope selection with the given points.
     * If the first and another point are on the same position, the selection is done (the following points will be ignored, if there are any)
     * and the {@link #areaSelected(Path2D)} interface is called.
     * If this isn't the case, the {@link #areaSelectedOnGoing(Path2D)} interface will be called and the selection can be continued by mouse selection.
     *
     * @param pointsToSet the coordinates to set in the coordsys (have to be given in coordsys coordinates)
     */
    public void setSelection(List<Point2D.Double> pointsToSet) {
        selectionModel.getSelection().first().clear();
        selectionModel.getSelection().first().addAll(pointsToSet);
    }

    protected void drawSelection() {
        clearSelectionResources();

        List<Point2D.Double> coordinates = new LinkedList<>();
        for (Point2D.Double pointInCoordsys: selectionModel.getSelection().first()) {
            coordinates.add(pointInCoordsys);
            this.points.addPoint(pointInCoordsys).setColor(colorScheme.getColor1());

            if (coordinates.size() > 1) {
                Point2D.Double firstPoint = coordinates.get(0);
                Point2D.Double lastCoord = coordinates.get(coordinates.size() - 2);
                this.lines.addSegment(lastCoord, pointInCoordsys).setColor(colorScheme.getColor2());

                if (coordSys.transformCoordSys2AWT(firstPoint, canvas.getHeight()).distanceSq(coordSys.transformCoordSys2AWT(pointInCoordsys, canvas.getHeight())) < radius) {
                    isDone = true;
                    for (Points.PointDetails pd: this.points.getPointDetails()) {
                        pd.setColor(colorScheme.getColorText());
                    }
                    this.lines.setGlobalAlphaMultiplier(0.5);
                    canvas.repaint();

                    // call selected interface
                    areaSelected(calculateSelectedArea());
                    selectionModel.getSelection().first().clear();
                    break;
                } else {
                    // call ongoing interface
                    areaSelectedOnGoing(calculateSelectedArea());
                }
            }
        }
        canvas.repaint();
    }

    /**
     * Simply removes the current (possibly not finished) selection.
     */
    public void removeSelection() {
        setSelection(new ArrayList<>());
    }

    private void clearSelectionResources() {
        isDone = false;
        this.points.getPointDetails().clear();
        this.lines.getSegments().clear();
        this.lines.setGlobalAlphaMultiplier(1);
    }

    /**
     * Will be called when selection is done (first point connected to the last point).
     *
     * @param selectedArea represents the selected points in the coordsys
     *                     ({@link Path2D#contains(double, double)} method can be used to check if points are inside the selected area)
     */
    public abstract void areaSelected(Path2D selectedArea);

    /**
     * Reports on the area currently selected when start and end points haven't been connected.
     * When selection is finished (mouse button released),
     * {@link #areaSelected(Path2D)}
     * will be called.
     *
     * @param selectedArea represents the selected points in the coordsys
     *                     ({@link Path2D#contains(double, double)} method can be used to check if points are inside the selected area)
     */
    public void areaSelectedOnGoing(Path2D selectedArea) {}

    /**
     * Sets a new {@link KeyMaskListener}, removes the old KeyMaskListener from the canvas
     * and registers the new one.
     *
     * @param keyMaskListener defines the set of keys that have to pressed during the panning
     */
    public CoordSysRopeSelector setKeyMaskListener(KeyMaskListener keyMaskListener) {
        canvas.removeKeyListener(this.keyMaskListener);
        this.keyMaskListener = keyMaskListener;
        if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
            canvas.addKeyListener(this.keyMaskListener);
        return this;
    }

    /**
     * Adds this {@link CoordSysRopeSelector} as {@link MouseListener} and
     * {@link MouseMotionListener} to the associated canvas.
     *
     * @return this for chaining
     */
    public CoordSysRopeSelector register() {
        if (!Arrays.asList(canvas.getMouseListeners()).contains(this))
            canvas.addMouseListener(this);
        if (!Arrays.asList(canvas.getMouseMotionListeners()).contains(this))
            canvas.addMouseMotionListener(this);
        if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
            canvas.addKeyListener(this.keyMaskListener);
        if (!Arrays.asList(canvas.getKeyListeners()).contains(this))
            canvas.addKeyListener(this);
        return this;
    }

    /**
     * Removes this {@link CoordSysRopeSelector} from the associated canvas'
     * mouse and mouse motion listeners.
     *
     * @return this for chaining
     */
    public CoordSysRopeSelector deRegister() {
        canvas.removeMouseListener(this);
        canvas.removeMouseMotionListener(this);
        canvas.removeKeyListener(this.keyMaskListener);
        canvas.removeKeyListener(this);
        return this;
    }
}