package hageldave.jplotter.interaction.kml;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Utils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * The CoordSysViewSelector class realizes a rectangular 
 * selection tool for {@link CoordSysRenderer}.
 * This enables to drag a selection region within the
 * coordinate area of a {@link CoordSysRenderer} using SHIFT+LMB.
 * The action to be performed with the selected region is up to
 * the implementation of the methods {@link #areaSelected(double, double, double, double)}
 * and {@link #areaSelectedOnGoing(double, double, double, double)}.
 * <p>
 * Intended use, (for example rectangular zooming):
 * <pre>
 * new CoordSysViewSelector(canvas, coordsys) {
 *    public void areaSelected(double minX, double minY, double maxX, double maxY) {
 *       coordsys.setCoordinateView(minX, minY, maxX, maxY);
 *    }
 * }.register();
 * </pre>
 * <p>
 * Per default the key event for a dragging mouse event to trigger
 * selection is {@link KeyEvent#VK_SHIFT}.
 * If this is undesired a {@link KeyMaskListener} has to be passed in the constructor.<br>
 * You may also want to not trigger selection when other modifiers are present. E.g.
 * when CTRL {@link KeyEvent#VK_CONTROL} is pressed, don't select because CTRL
 * is already meant for panning.
 *
 * @author hageldave
 */
public abstract class CoordSysViewSelector extends MouseAdapter {
	
	protected Component canvas;
	protected CoordSysRenderer coordsys;
	protected CompleteRenderer overlay;
	protected Lines areaBorder = new Lines().setVertexRoundingEnabled(true);
	protected Point start,end;
	protected KeyMaskListener keyMaskListener;

	/**
	 * Creates a new {@link CoordSysViewSelector} for the specified canvas and corresponding coordinate system.
	 * @param canvas displaying the coordsys
	 * @param coordsys the coordinate system to apply the view selection in
	 * @param keyMaskListener defines the set of keys that have to pressed during the view selection
	 */
	public CoordSysViewSelector(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener keyMaskListener) {
		this.canvas = canvas.asComponent();
		this.coordsys = coordsys;
		this.keyMaskListener = keyMaskListener;
		Renderer presentRenderer;
		if((presentRenderer = coordsys.getOverlay()) == null){
			coordsys.setOverlay(this.overlay = new CompleteRenderer());
		} else if (presentRenderer instanceof CompleteRenderer){
			this.overlay = (CompleteRenderer) presentRenderer;
		} else {
			throw new IllegalStateException(
					"The canvas' current overlay renderer is not an instance of CompleteRenderer but "
							+ presentRenderer.getClass().getName() + " which cannot be used with CoordSysAreaSelector.");
		}
	}

	public CoordSysViewSelector(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
		this(canvas, coordsys, new KeyMaskListener(KeyEvent.VK_SHIFT));
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if (this.keyMaskListener.areKeysPressed()) {
			start = e.getPoint();
			overlay.addItemToRender(areaBorder);
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(start == null || !this.keyMaskListener.areKeysPressed()){
			return;
		}
		{
			Rectangle2D coordSysArea = Utils.swapYAxis(coordsys.getCoordSysArea(),canvas.getHeight());
			Point end = e.getPoint();
			// clamp end point to area
			double endX = Utils.clamp(coordSysArea.getMinX(), end.getX(), coordSysArea.getMaxX());
			double endY = Utils.clamp(coordSysArea.getMinY(), end.getY(), coordSysArea.getMaxY());
			this.end = new Point((int)endX, (int)endY);
		}
		createSelectionAreaBorder();
		
		Point2D p1 = coordsys.transformAWT2CoordSys(start, canvas.getHeight());
		Point2D p2 = coordsys.transformAWT2CoordSys(end, canvas.getHeight());
		this.areaSelectedOnGoing(
				Math.min(p1.getX(), p2.getX()),
				Math.min(p1.getY(), p2.getY()),
				Math.max(p1.getX(), p2.getX()),
				Math.max(p1.getY(), p2.getY())
		);
		canvas.repaint();
	}
	
	protected void createSelectionAreaBorder() {
		Point2D start_ = start;
		Point2D end_ = end;
		Rectangle vp = coordsys.getCurrentViewPort();
		start_.setLocation(start_.getX()-vp.x, start_.getY()-vp.y);
		end_.setLocation(end_.getX()-vp.x, end_.getY()-vp.y);

		start_ = coordsys.transformAWT2CoordSys(start_, canvas.getHeight());
		end_ = coordsys.transformAWT2CoordSys(end_, canvas.getHeight());

		areaBorder.removeAllSegments();
		areaBorder.addSegment(start_.getX(), start_.getY(), start_.getX(), end_.getY()).setColor(0xff222222);
		areaBorder.addSegment(end_.getX(), start_.getY(), end_.getX(), end_.getY()).setColor(0xff222222);
		areaBorder.addSegment(start_.getX(), start_.getY(), end_.getX(), start_.getY()).setColor(0xff222222);
		areaBorder.addSegment(start_.getX(), end_.getY(), end_.getX(), end_.getY()).setColor(0xff222222);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		areaBorder.removeAllSegments();
		overlay.lines.removeItemToRender(areaBorder);
		if(start != null && end != null){
			Point2D p1 = coordsys.transformAWT2CoordSys(start, canvas.getHeight());
			Point2D p2 = coordsys.transformAWT2CoordSys(end, canvas.getHeight());
			this.areaSelected(
					Math.min(p1.getX(), p2.getX()),
					Math.min(p1.getY(), p2.getY()),
					Math.max(p1.getX(), p2.getX()),
					Math.max(p1.getY(), p2.getY())
			);
		}
		canvas.repaint();
		start = null;
		end = null;
	}

	/**
	 * Sets a new {@link KeyMaskListener}, removes the old KeyMaskListener from the canvas
	 * and registers the new one.
	 *
	 * @param keyMaskListener defines the set of keys that have to pressed during the panning
	 */
	public void setKeyMaskListener(KeyMaskListener keyMaskListener) {
		canvas.removeKeyListener(this.keyMaskListener);
		this.keyMaskListener = keyMaskListener;
		if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
			canvas.addKeyListener(this.keyMaskListener);
	}

	/**
	 * Adds this {@link CoordSysViewSelector} as {@link MouseListener} and
	 * {@link MouseMotionListener} to the associated canvas.
	 * @return this for chaining
	 */
	public CoordSysViewSelector register(){
		if( ! Arrays.asList(canvas.getMouseListeners()).contains(this))
			canvas.addMouseListener(this);
		if( ! Arrays.asList(canvas.getMouseMotionListeners()).contains(this))
			canvas.addMouseMotionListener(this);
		if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
			canvas.addKeyListener(this.keyMaskListener);
		return this;
	}
	
	/**
	 * Removes this {@link CoordSysViewSelector} from the associated canvas'
	 * mouse and mouse motion listeners.
	 * @return this for chaining
	 */
	public CoordSysViewSelector deRegister(){
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		canvas.removeKeyListener(this.keyMaskListener);
		return this;
	}
	
	/**
	 * Reports on the area currently selected during dragging.
	 * When selection is finished (mouse button released),
	 * {@link #areaSelected(double, double, double, double)}
	 * will be called.
	 * @param minX left boundary of selection
	 * @param minY bottom boundary of selection
	 * @param maxX right boundary of selection
	 * @param maxY top boundary of selection
	 */
	public void areaSelectedOnGoing(double minX, double minY, double maxX, double maxY){
		
	}
	
	/**
	 * Will be called when selection is done (mouse button released).
	 * @param minX left boundary of selection
	 * @param minY bottom boundary of selection
	 * @param maxX right boundary of selection
	 * @param maxY top boundary of selection
	 */
	public abstract void areaSelected(double minX, double minY, double maxX, double maxY);
	
}
