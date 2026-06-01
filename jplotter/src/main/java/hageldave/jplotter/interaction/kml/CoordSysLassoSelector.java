package hageldave.jplotter.interaction.kml;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.CursorCoordinator;
import hageldave.jplotter.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The {@link CoordSysLassoSelector} extends {@link CoordSysViewSelector} to provide
 * free-form lasso selection for the coordinate view of a {@link CoordSysRenderer}.
 * <p>
 * The user presses the configured key mask and drags with the mouse to draw an
 * arbitrary path. On mouse release the path is finalized, automatically closed,
 * and can no longer be edited. When the lasso is self-intersecting,
 * {@link Path2D#WIND_EVEN_ODD} is used so inside/outside follows the lasso outline.
 * <p>
 * Interaction model:
 * <ul>
 * <li>Press key mask + left mouse button inside the coordsys area to start a new lasso.</li>
 * <li>Drag to append points to the lasso path.</li>
 * <li>Release the mouse button to finalize the lasso and call {@link #areaSelected(Path2D)}.</li>
 * <li>Right-click or press {@code ESC} to clear the lasso via {@link #clearLasso()}.</li>
 * </ul>
 * <p>
 * Intended use:
 *
 * <pre>
 * new CoordSysLassoSelector(canvas, coordsys) {
 * @Override
 * public void areaSelected(Path2D selectedArea) {
 * // selectedArea.contains(x, y)
 * }
 * }.register();
 * </pre>
 *
 * @author hageldave
 */
public abstract class CoordSysLassoSelector extends CoordSysViewSelector implements KeyListener {

protected final ColorScheme colorScheme;
protected boolean isDragging = false;
protected boolean hasSelection = false;
protected final List<Point2D.Double> lassoPoints = new ArrayList<>();
protected final Lines lassoLines = new Lines();
protected boolean isLassoInOverlay = false;

/**
 * Key listener that clears the current lasso when ESC is pressed.
 */
protected final KeyAdapter escListener = new KeyAdapter() {
@Override
public void keyPressed(KeyEvent e) {
if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
clearLasso();
}
}
};

/**
 * Creates a new {@link CoordSysLassoSelector} for the specified canvas and
 * corresponding coordinate system.
 *
 * @param canvas          the canvas displaying the coordsys
 * @param coordsys        the coordinate system to apply the lasso selection in
 * @param keyMaskListener defines the set of keys required to activate selection
 */
public CoordSysLassoSelector(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener keyMaskListener) {
super(canvas, coordsys, keyMaskListener);
this.colorScheme = coordsys.getColorScheme();
this.lassoLines.setVertexRoundingEnabled(true);
}

/**
 * Creates a new {@link CoordSysLassoSelector} using the default {@code L} key mask.
 *
 * @param canvas   the canvas displaying the coordsys
 * @param coordsys the coordinate system to apply the lasso selection in
 */
public CoordSysLassoSelector(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
this(canvas, coordsys, new KeyMaskListener(KeyEvent.VK_L));
}

@Override
public void mousePressed(MouseEvent e) {
if (!keyMaskListener.areKeysPressed() || !coordsys.getCoordSysArea().contains(e.getPoint())) {
updateCursor();
return;
}
if (SwingUtilities.isRightMouseButton(e) || hasSelection) {
clearLasso();
return;
}
isDragging = true;
lassoPoints.clear();
lassoLines.removeAllSegments();
lassoPoints.add((Point2D.Double) coordsys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight()));
if (!isLassoInOverlay) {
overlay.addItemToRender(lassoLines);
isLassoInOverlay = true;
}
updateCursor();
}

@Override
public void mouseDragged(MouseEvent e) {
if (!isDragging) {
updateCursor();
return;
}

Rectangle2D coordSysArea = Utils.swapYAxis(coordsys.getCoordSysArea(), canvas.getHeight());
Point p = e.getPoint();
double clampedX = Utils.clamp(coordSysArea.getMinX(), p.getX(), coordSysArea.getMaxX());
double clampedY = Utils.clamp(coordSysArea.getMinY(), p.getY(), coordSysArea.getMaxY());
Point clampedPoint = new Point((int) clampedX, (int) clampedY);

Point2D.Double lastPoint = lassoPoints.isEmpty() ? null : lassoPoints.get(lassoPoints.size() - 1);
if (lastPoint == null || coordsys.transformCoordSys2AWT(lastPoint, canvas.getHeight()).distance(clampedPoint) > 2.0) {
lassoPoints.add((Point2D.Double) coordsys.transformAWT2CoordSys(clampedPoint, canvas.getHeight()));
}

rebuildLassoLines(true);
if (lassoPoints.size() >= 2) {
areaSelectedOnGoing(calculateCurrentPath());
}
jPlotterCanvas.scheduleRepaint();
updateCursor();
}

@Override
public void mouseReleased(MouseEvent e) {
if (!isDragging) {
updateCursor();
return;
}
isDragging = false;
if (lassoPoints.size() < 3) {
clearLasso();
return;
}
rebuildLassoLines(false);
hasSelection = true;
areaSelected(calculateSelectedArea());
jPlotterCanvas.scheduleRepaint();
updateCursor();
}

@Override
public void mouseMoved(MouseEvent e) {
updateCursor();
}

@Override
public void keyTyped(KeyEvent e) {
}

@Override
public void keyPressed(KeyEvent e) {
updateCursor();
}

@Override
public void keyReleased(KeyEvent e) {
updateCursor();
}

protected void updateCursor() {
if (keyMaskListener.areKeysPressed() && hasSelection) {
CursorCoordinator.get(canvas).requestCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), this);
} else if (keyMaskListener.areKeysPressed()) {
CursorCoordinator.get(canvas).requestCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR), this);
} else {
CursorCoordinator.get(canvas).requestCursor(null, this);
}
}

protected void rebuildLassoLines(boolean withPreviewClose) {
lassoLines.removeAllSegments();
for (int i = 1; i < lassoPoints.size(); i++) {
Point2D.Double p0 = lassoPoints.get(i - 1);
Point2D.Double p1 = lassoPoints.get(i);
lassoLines.addSegment(p0, p1).setColor(colorScheme.getColor2());
}
if (lassoPoints.size() > 1) {
Point2D.Double first = lassoPoints.get(0);
Point2D.Double last = lassoPoints.get(lassoPoints.size() - 1);
lassoLines.addSegment(last, first).setColor(withPreviewClose ? colorScheme.getColor1() : colorScheme.getColor2());
}
}

private Path2D calculateCurrentPath() {
Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
path.moveTo(lassoPoints.get(0).x, lassoPoints.get(0).y);
for (int i = 1; i < lassoPoints.size(); i++) {
path.lineTo(lassoPoints.get(i).x, lassoPoints.get(i).y);
}
return path;
}

private Path2D calculateSelectedArea() {
Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
path.moveTo(lassoPoints.get(0).x, lassoPoints.get(0).y);
for (int i = 1; i < lassoPoints.size(); i++) {
path.lineTo(lassoPoints.get(i).x, lassoPoints.get(i).y);
}
path.closePath();
return path;
}

/**
 * Clears the current lasso selection and removes its visual representation.
 */
public void clearLasso() {
lassoLines.removeAllSegments();
if (isLassoInOverlay) {
overlay.lines.removeItemToRender(lassoLines);
isLassoInOverlay = false;
}
lassoPoints.clear();
isDragging = false;
hasSelection = false;
CursorCoordinator.get(canvas).requestCursor(null, this);
jPlotterCanvas.scheduleRepaint();
areaCleared();
}

/**
 * {@inheritDoc} Also registers ESC handling and cursor updates via
 * {@link KeyListener}.
 */
@Override
public CoordSysLassoSelector register() {
super.register();
if (!Arrays.asList(canvas.getKeyListeners()).contains(escListener)) {
canvas.addKeyListener(escListener);
}
if (!Arrays.asList(canvas.getKeyListeners()).contains(this)) {
canvas.addKeyListener(this);
}
return this;
}

/**
 * {@inheritDoc} Also removes ESC handling and cursor key listeners.
 */
@Override
public CoordSysLassoSelector deRegister() {
super.deRegister();
canvas.removeKeyListener(escListener);
canvas.removeKeyListener(this);
return this;
}

/**
 * Will be called when lasso selection is completed (mouse button released).
 * The path uses {@link Path2D#WIND_EVEN_ODD}, so
 * {@link Path2D#contains(double, double)} correctly handles self-intersections.
 *
 * @param selectedArea finalized closed lasso path in coordinate space
 */
public abstract void areaSelected(Path2D selectedArea);

/**
 * Reports the lasso path while dragging.
 *
 * @param currentPath current not-yet-finalized lasso path in coordinate space
 */
public void areaSelectedOnGoing(Path2D currentPath) {
}

/**
 * Will be called when the lasso is cleared.
 */
public void areaCleared() {
}
}
