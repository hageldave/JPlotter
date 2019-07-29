package hageldave.jplotter.interaction;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.CoordSysCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Utils;

/**
 * The CoordSysViewSelector class realizes a rectangular 
 * selection tool for {@link CoordSysCanvas}.
 * This enables to drag a selection region within the
 * coordinate area of a {@link CoordSysCanvas} using SHIFT+LMB.
 * The action to be performed with the selected region is up to
 * the implementation of the methods {@link #areaSelected(double, double, double, double)}
 * and {@link #areaSelectedOnGoing(double, double, double, double)}.
 * <p>
 * Intended use, (for example rectangular zooming):
 * <pre>
 * new CoordSysViewSelector(canvas) {
 *    public void areaSelected(double minX, double minY, double maxX, double maxY) {
 *       canvas.setCoordinateView(minX, minY, maxX, maxY);
 *    }
 * }.register();
 * </pre>
 * <p>
 * Per default the extended modifier mask for a dragging mouse event to trigger
 * selection is {@link InputEvent#SHIFT_DOWN_MASK}. 
 * If this is undesired the {@link #extModifierMask} has to be overridden.<br>
 * For example to not need to press any key:
 * <pre>
 * new CoordSysViewSelector(canvas) {
 *    {extModifierMask=0;}
 *    
 *    public void areaSelected(double minX, double minY, double maxX, double maxY) {
 *       canvas.setCoordinateView(minX, minY, maxX, maxY);
 *    }
 * }.register();
 * </pre>
 * 
 * @author hageldave
 */
public abstract class CoordSysViewSelector extends MouseAdapter {
	
	protected CoordSysCanvas canvas;
	protected CompleteRenderer overlay;
	protected Lines areaBorder = new Lines().setVertexRoundingEnabled(true);
	protected Point start,end;
	protected int extModifierMask = InputEvent.SHIFT_DOWN_MASK;
	
	
	public CoordSysViewSelector(CoordSysCanvas canvas) {
		this.canvas = canvas;
		Renderer presentRenderer;
		if((presentRenderer = canvas.getOverlay()) == null){
			canvas.setOverlay(this.overlay = new CompleteRenderer());
		} else if(presentRenderer instanceof CompleteRenderer){
			this.overlay = (CompleteRenderer) presentRenderer;
		} else {
			throw new IllegalStateException(
					"The canvas' current overlay renderer is not an instance of CompleteRenderer but "
					+ presentRenderer.getClass().getName() + " which cannot be used with CoordSysAreaSelector.");
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if(isTriggerMouseEvent(e, MouseEvent.MOUSE_PRESSED)){
			start = e.getPoint();
			overlay.addItemToRender(areaBorder);
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(start == null || !isTriggerMouseEvent(e, MouseEvent.MOUSE_DRAGGED)){	
			return;
		}
		{
			Rectangle2D coordSysArea = Utils.swapYAxis(canvas.getCoordSysArea(),canvas.getHeight());
			Point end = e.getPoint();
			// clamp end point to area
			double endX = Utils.clamp(coordSysArea.getMinX(), end.getX(), coordSysArea.getMaxX());
			double endY = Utils.clamp(coordSysArea.getMinY(), end.getY(), coordSysArea.getMaxY());
			this.end = new Point((int)endX, (int)endY);
		}
		Point start_ = Utils.swapYAxis(start, canvas.getHeight());
		Point2D end_ = Utils.swapYAxis(end, canvas.getHeight());
		areaBorder.removeAllSegments();
		areaBorder.addSegment(start_.getX(), start_.getY(), start_.getX(), end_.getY()).setColor(0xff222222);
		areaBorder.addSegment(end_.getX(), start_.getY(), end_.getX(), end_.getY()).setColor(0xff222222);
		areaBorder.addSegment(start_.getX(), start_.getY(), end_.getX(), start_.getY()).setColor(0xff222222);
		areaBorder.addSegment(start_.getX(), end_.getY(), end_.getX(), end_.getY()).setColor(0xff222222);
		;
		
		Point2D p1 = canvas.transformAWT2CoordSys(start);
		Point2D p2 = canvas.transformAWT2CoordSys(end);
		this.areaSelectedOnGoing(
				Math.min(p1.getX(), p2.getX()),
				Math.min(p1.getY(), p2.getY()),
				Math.max(p1.getX(), p2.getX()),
				Math.max(p1.getY(), p2.getY())
		);
		
		canvas.repaint();
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		areaBorder.removeAllSegments();
		overlay.lines.removeItemToRender(areaBorder);
		if(start != null && end != null){
			Point2D p1 = canvas.transformAWT2CoordSys(start);
			Point2D p2 = canvas.transformAWT2CoordSys(end);
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
	
	protected boolean isTriggerMouseEvent(MouseEvent e, int method){
		if(!SwingUtilities.isLeftMouseButton(e))
			return false;
		if((e.getModifiersEx()&extModifierMask) != extModifierMask)
			return false;
		if(method == MouseEvent.MOUSE_PRESSED){
			return canvas.getCoordSysArea().contains( Utils.swapYAxis(e.getPoint(), canvas.getHeight()) );
		}
		return true;
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
