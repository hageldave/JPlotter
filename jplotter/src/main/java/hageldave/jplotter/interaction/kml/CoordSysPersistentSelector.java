package hageldave.jplotter.interaction.kml;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * The CoordSysPersistentSelector extends {@link CoordSysViewSelector} to provide
 * a persistent rectangular selection that survives mouse release and can be
 * moved and resized by subsequent drag interactions.
 * <p>
 * The selection rectangle is stored in coordinate space (not pixel space), so it
 * remains correct when the user pans or zooms.
 * <p>
 * Interaction model:
 * <ul>
 *   <li>SHIFT+drag draws a new selection rectangle.</li>
 *   <li>Once a selection exists, SHIFT+drag on the interior moves it.</li>
 *   <li>SHIFT+drag on an edge or corner resizes the rectangle.</li>
 *   <li>SHIFT+click or SHIFT+drag outside the selection clears it and starts a new one.</li>
 *   <li>Pressing {@code ESC} clears the selection at any time.</li>
 * </ul>
 * <p>
 * The callback contract is identical to the parent:
 * <ul>
 *   <li>{@link #areaSelectedOnGoing(double, double, double, double)} fires continuously
 *       during any drag (drawing, moving, or resizing).</li>
 *   <li>{@link #areaSelected(double, double, double, double)} fires once on mouse release.</li>
 * </ul>
 * <p>
 * Intended use:
 * <pre>
 * new CoordSysPersistentSelector(canvas, coordsys) {
 *     public void areaSelected(double minX, double minY, double maxX, double maxY) {
 *         // called on mouse release after drawing, moving, or resizing
 *     }
 * }.register();
 * </pre>
 * <p>
 * Hit test zones use a band of {@code 6px} outward and {@code 1px} inward around each
 * border edge. Corners take priority over edges in the hit test, and the interior of
 * the {@code 1px} inset activates the MOVE cursor.
 *
 * @author hageldave
 */
public abstract class CoordSysPersistentSelector extends CoordSysViewSelector {

    /**
     * Drag interaction modes for the persistent selector.
     * {@code NONE} means no active drag. {@code DRAWING} means a new rectangle is being
     * drawn. {@code MOVING} and the {@code RESIZE_*} variants are entered when an existing
     * selection is dragged from the interior or from an edge/corner, respectively.
     */
    protected enum DragMode {
        NONE, DRAWING, MOVING,
        RESIZE_N, RESIZE_S, RESIZE_E, RESIZE_W,
        RESIZE_NE, RESIZE_NW, RESIZE_SE, RESIZE_SW
    }

    /** Current drag interaction mode. */
    protected DragMode dragMode = DragMode.NONE;

    /** Stored selection left boundary in coordinate space. */
    protected double selMinX;
    /** Stored selection bottom boundary in coordinate space. */
    protected double selMinY;
    /** Stored selection right boundary in coordinate space. */
    protected double selMaxX;
    /** Stored selection top boundary in coordinate space. */
    protected double selMaxY;

    /** Whether a persistent selection currently exists. */
    protected boolean hasSelection = false;

    /** Whether {@link #areaBorder} has been added to the overlay renderer. */
    protected boolean isBorderInOverlay = false;

    /** X component of the coord-space drag anchor (set on mousePressed for MOVING/RESIZING). */
    protected double anchorCoordX;
    /** Y component of the coord-space drag anchor (set on mousePressed for MOVING/RESIZING). */
    protected double anchorCoordY;

    /** Selection left boundary at the start of a MOVE/RESIZE drag. */
    protected double anchorSelMinX;
    /** Selection bottom boundary at the start of a MOVE/RESIZE drag. */
    protected double anchorSelMinY;
    /** Selection right boundary at the start of a MOVE/RESIZE drag. */
    protected double anchorSelMaxX;
    /** Selection top boundary at the start of a MOVE/RESIZE drag. */
    protected double anchorSelMaxY;

    /**
     * Key listener that calls {@link #clearSelection()} when ESC is pressed.
     */
    protected final KeyAdapter escListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                clearSelection();
            }
        }
    };

    /**
     * Creates a new {@link CoordSysPersistentSelector} for the specified canvas
     * and corresponding coordinate system.
     *
     * @param canvas          the canvas displaying the coordsys
     * @param coordsys        the coordinate system to apply the selection in
     * @param keyMaskListener defines the set of keys required to activate selection
     */
    public CoordSysPersistentSelector(JPlotterCanvas canvas, CoordSysRenderer coordsys,
            KeyMaskListener keyMaskListener) {
        super(canvas, coordsys, keyMaskListener);
    }

    /**
     * Creates a new {@link CoordSysPersistentSelector} using the default SHIFT key mask.
     *
     * @param canvas   the canvas displaying the coordsys
     * @param coordsys the coordinate system to apply the selection in
     */
    public CoordSysPersistentSelector(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
        super(canvas, coordsys);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If keys are not pressed, returns immediately.
     * When a selection exists, performs a hit test to decide between MOVING, RESIZING,
     * or clearing (when clicked outside the selection) then starting a new DRAWING.
     * When no selection exists, starts a new DRAWING.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (!keyMaskListener.areKeysPressed()) {
            return;
        }
        if (hasSelection) {
            DragMode hitMode = hitTest(e.getPoint());
            if (hitMode == DragMode.NONE) {
                // Clicked outside current selection: clear it and start drawing a new one
                clearSelection();
                // fall through to DRAWING init below
            } else {
                // Clicked on interior or border: start MOVING or RESIZING
                dragMode = hitMode;
                Point2D anchor = coordsys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight());
                anchorCoordX = anchor.getX();
                anchorCoordY = anchor.getY();
                anchorSelMinX = selMinX;
                anchorSelMinY = selMinY;
                anchorSelMaxX = selMaxX;
                anchorSelMaxY = selMaxY;
                return;
            }
        }
        // Start a new DRAWING interaction
        dragMode = DragMode.DRAWING;
        start = e.getPoint();
        end = null;
        if (!isBorderInOverlay) {
            overlay.addItemToRender(areaBorder);
            isBorderInOverlay = true;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * DRAWING: clamps the end point to the coordsys area, redraws the border, and fires
     * {@link #areaSelectedOnGoing}.
     * MOVING/RESIZING: computes the coord-space delta from the anchor, updates the
     * relevant selection bounds (always from anchor to avoid floating-point drift),
     * redraws the border, and fires {@link #areaSelectedOnGoing}.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragMode == DragMode.NONE) {
            return;
        }
        if (dragMode == DragMode.DRAWING) {
            if (start == null) {
                return;
            }
            // Clamp end point to the visible coordsys area (same logic as parent)
            Rectangle2D coordSysArea = Utils.swapYAxis(coordsys.getCoordSysArea(), canvas.getHeight());
            Point p = e.getPoint();
            double endX = Utils.clamp(coordSysArea.getMinX(), p.getX(), coordSysArea.getMaxX());
            double endY = Utils.clamp(coordSysArea.getMinY(), p.getY(), coordSysArea.getMaxY());
            end = new Point((int) endX, (int) endY);
            createSelectionAreaBorder();
            // Compute coord-space bounds and fire ongoing callback
            Point2D p1 = coordsys.transformAWT2CoordSys(start, canvas.getHeight());
            Point2D p2 = coordsys.transformAWT2CoordSys(end, canvas.getHeight());
            areaSelectedOnGoing(
                    Math.min(p1.getX(), p2.getX()),
                    Math.min(p1.getY(), p2.getY()),
                    Math.max(p1.getX(), p2.getX()),
                    Math.max(p1.getY(), p2.getY())
            );
            canvas.repaint();
        } else {
            // MOVING or RESIZING: compute coord-space delta from anchor
            Point2D currentCoord = coordsys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight());
            double deltaX = currentCoord.getX() - anchorCoordX;
            double deltaY = currentCoord.getY() - anchorCoordY;
            // Reset all bounds from anchor values to avoid accumulated drift
            selMinX = anchorSelMinX;
            selMaxX = anchorSelMaxX;
            selMinY = anchorSelMinY;
            selMaxY = anchorSelMaxY;
            switch (dragMode) {
                case MOVING:
                    selMinX += deltaX;
                    selMaxX += deltaX;
                    selMinY += deltaY;
                    selMaxY += deltaY;
                    break;
                case RESIZE_N:
                    selMaxY += deltaY;
                    break;
                case RESIZE_S:
                    selMinY += deltaY;
                    break;
                case RESIZE_E:
                    selMaxX += deltaX;
                    break;
                case RESIZE_W:
                    selMinX += deltaX;
                    break;
                case RESIZE_NE:
                    selMaxY += deltaY;
                    selMaxX += deltaX;
                    break;
                case RESIZE_NW:
                    selMaxY += deltaY;
                    selMinX += deltaX;
                    break;
                case RESIZE_SE:
                    selMinY += deltaY;
                    selMaxX += deltaX;
                    break;
                case RESIZE_SW:
                    selMinY += deltaY;
                    selMinX += deltaX;
                    break;
                default:
                    break;
            }
            createSelectionAreaBorder();
            areaSelectedOnGoing(selMinX, selMinY, selMaxX, selMaxY);
            canvas.repaint();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * DRAWING: if the resulting area is zero, calls {@link #clearSelection()};
     * otherwise persists the selection bounds and fires {@link #areaSelected}.
     * MOVING/RESIZING: normalizes the bounds (in case a resize inverted min/max),
     * persists the result, and fires {@link #areaSelected}.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragMode == DragMode.DRAWING) {
            if (start == null || end == null) {
                dragMode = DragMode.NONE;
                start = null;
                end = null;
                canvas.repaint();
                return;
            }
            Point2D p1 = coordsys.transformAWT2CoordSys(start, canvas.getHeight());
            Point2D p2 = coordsys.transformAWT2CoordSys(end, canvas.getHeight());
            double minX = Math.min(p1.getX(), p2.getX());
            double maxX = Math.max(p1.getX(), p2.getX());
            double minY = Math.min(p1.getY(), p2.getY());
            double maxY = Math.max(p1.getY(), p2.getY());
            if (minX == maxX || minY == maxY) {
                // Zero-area selection: clear it
                clearSelection();
                return;
            }
            selMinX = minX;
            selMaxX = maxX;
            selMinY = minY;
            selMaxY = maxY;
            hasSelection = true;
            start = null;
            end = null;
            areaSelected(selMinX, selMinY, selMaxX, selMaxY);
        } else if (dragMode != DragMode.NONE) {
            // MOVING or RESIZING: normalize bounds in case resize inverted min/max
            if (selMinX > selMaxX) {
                double tmp = selMinX;
                selMinX = selMaxX;
                selMaxX = tmp;
            }
            if (selMinY > selMaxY) {
                double tmp = selMinY;
                selMinY = selMaxY;
                selMaxY = tmp;
            }
            hasSelection = true;
            areaSelected(selMinX, selMinY, selMaxX, selMaxY);
        }
        dragMode = DragMode.NONE;
        canvas.repaint();
    }

    /**
     * Updates the canvas cursor based on the current mouse position relative to
     * the persistent selection. Only activates when the key mask is pressed and a
     * selection exists; otherwise the default cursor is shown.
     *
     * @param e the mouse event
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!keyMaskListener.areKeysPressed() || !hasSelection) {
            canvas.setCursor(Cursor.getDefaultCursor());
            return;
        }
        DragMode zone = hitTest(e.getPoint());
        switch (zone) {
            case RESIZE_NW: canvas.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)); break;
            case RESIZE_NE: canvas.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)); break;
            case RESIZE_SW: canvas.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)); break;
            case RESIZE_SE: canvas.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)); break;
            case RESIZE_N:  canvas.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));  break;
            case RESIZE_S:  canvas.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));  break;
            case RESIZE_W:  canvas.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));  break;
            case RESIZE_E:  canvas.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));  break;
            case MOVING:    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));       break;
            default:        canvas.setCursor(Cursor.getDefaultCursor());                            break;
        }
    }

    /**
     * Performs a hit test of a given AWT pixel coordinate against the current selection
     * rectangle, returning the {@link DragMode} corresponding to the hit zone.
     * <p>
     * Hit zones use a band of {@code 6px} outward and {@code 1px} inward from each border
     * edge. Corners (intersections of two edge bands) take priority over plain edges, and
     * the interior (strictly inside the {@code 1px} inset on all sides) maps to
     * {@link DragMode#MOVING}. Points outside the {@code 6px} expanded bounding box map to
     * {@link DragMode#NONE}.
     *
     * @param mouseAWT the mouse position in AWT pixel coordinates
     * @return the hit zone as a {@link DragMode}, or {@link DragMode#NONE} if outside
     */
    protected DragMode hitTest(Point mouseAWT) {
        // Convert selection bounds to AWT pixel space
        // (selMinX, selMaxY) is the top-left in coord space → smaller AWT y
        // (selMaxX, selMinY) is the bottom-right in coord space → larger AWT y
        Point2D tlAWT = coordsys.transformCoordSys2AWT(
                new Point2D.Double(selMinX, selMaxY), canvas.getHeight());
        Point2D brAWT = coordsys.transformCoordSys2AWT(
                new Point2D.Double(selMaxX, selMinY), canvas.getHeight());

        double leftX   = Math.min(tlAWT.getX(), brAWT.getX());
        double rightX  = Math.max(tlAWT.getX(), brAWT.getX());
        double topY    = Math.min(tlAWT.getY(), brAWT.getY()); // smaller AWT y = screen top
        double bottomY = Math.max(tlAWT.getY(), brAWT.getY()); // larger  AWT y = screen bottom

        double mx = mouseAWT.getX();
        double my = mouseAWT.getY();

        // Quick reject: outside the 6px expanded bounding box
        if (mx < leftX - 6 || mx > rightX + 6 || my < topY - 6 || my > bottomY + 6) {
            return DragMode.NONE;
        }

        // Edge hit bands: [border - 6px outward, border + 1px inward]
        boolean inNBand = my >= topY    - 6 && my <= topY    + 1;
        boolean inSBand = my >= bottomY - 1 && my <= bottomY + 6;
        boolean inWBand = mx >= leftX   - 6 && mx <= leftX   + 1;
        boolean inEBand = mx >= rightX  - 1 && mx <= rightX  + 6;

        // Check corners first (intersection of two edge bands)
        if (inNBand && inWBand) return DragMode.RESIZE_NW;
        if (inNBand && inEBand) return DragMode.RESIZE_NE;
        if (inSBand && inWBand) return DragMode.RESIZE_SW;
        if (inSBand && inEBand) return DragMode.RESIZE_SE;

        // Then individual edges
        if (inNBand) return DragMode.RESIZE_N;
        if (inSBand) return DragMode.RESIZE_S;
        if (inWBand) return DragMode.RESIZE_W;
        if (inEBand) return DragMode.RESIZE_E;

        // Interior: strictly inside the 1px inset on all four sides
        if (mx > leftX + 1 && mx < rightX - 1 && my > topY + 1 && my < bottomY - 1) {
            return DragMode.MOVING;
        }

        return DragMode.NONE;
    }

    /**
     * Overrides the parent implementation to fix the aliasing mutation bug and to
     * write border segments directly from coordinate-space bounds.
     * <p>
     * During {@link DragMode#DRAWING} the bounds are derived from the current
     * {@link #start} and {@link #end} pixel points (via
     * {@link CoordSysRenderer#transformAWT2CoordSys}). For all other modes the
     * stored {@link #selMinX}/{@link #selMinY}/{@link #selMaxX}/{@link #selMaxY} are
     * used directly, since the overlay is an {@code AdaptableView} whose segments are
     * interpreted in coordinate space.
     */
    @Override
    protected void createSelectionAreaBorder() {
        double minX, maxX, minY, maxY;
        if (dragMode == DragMode.DRAWING) {
            // Compute coord-space bounds from the current AWT start/end points.
            // Use fresh Point2D instances to avoid mutating the original start/end fields.
            Point2D p1 = coordsys.transformAWT2CoordSys(
                    new Point2D.Double(start.getX(), start.getY()), canvas.getHeight());
            Point2D p2 = coordsys.transformAWT2CoordSys(
                    new Point2D.Double(end.getX(), end.getY()), canvas.getHeight());
            minX = Math.min(p1.getX(), p2.getX());
            maxX = Math.max(p1.getX(), p2.getX());
            minY = Math.min(p1.getY(), p2.getY());
            maxY = Math.max(p1.getY(), p2.getY());
        } else {
            minX = selMinX;
            maxX = selMaxX;
            minY = selMinY;
            maxY = selMaxY;
        }
        areaBorder.removeAllSegments();
        areaBorder.addSegment(minX, minY, minX, maxY).setColor(0xff222222);
        areaBorder.addSegment(maxX, minY, maxX, maxY).setColor(0xff222222);
        areaBorder.addSegment(minX, minY, maxX, minY).setColor(0xff222222);
        areaBorder.addSegment(minX, maxY, maxX, maxY).setColor(0xff222222);
    }

    /**
     * Clears the current selection: removes the border from the overlay, resets all
     * selection state and drag mode, and restores the default cursor.
     */
    public void clearSelection() {
        areaBorder.removeAllSegments();
        if (isBorderInOverlay) {
            overlay.lines.removeItemToRender(areaBorder);
            isBorderInOverlay = false;
        }
        hasSelection = false;
        dragMode = DragMode.NONE;
        start = null;
        end = null;
        canvas.setCursor(Cursor.getDefaultCursor());
        canvas.repaint();
    }

    /**
     * {@inheritDoc}
     * Also registers the ESC key listener so pressing ESC calls {@link #clearSelection()}.
     */
    @Override
    public CoordSysPersistentSelector register() {
        super.register();
        if (!Arrays.asList(canvas.getKeyListeners()).contains(escListener)) {
            canvas.addKeyListener(escListener);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * Also removes the ESC key listener.
     */
    @Override
    public CoordSysPersistentSelector deRegister() {
        super.deRegister();
        canvas.removeKeyListener(escListener);
        return this;
    }
}
