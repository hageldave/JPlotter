package hageldave.jplotter.interaction;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import hageldave.jplotter.CoordSysCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Utils;

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
		if(SwingUtilities.isLeftMouseButton(e) && (e.getModifiersEx()&extModifierMask) == extModifierMask){
			start = e.getPoint();
			overlay.addItemToRender(areaBorder);
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(start == null || (e.getModifiersEx()&extModifierMask) != extModifierMask)
			return;
		
		end = e.getPoint();
		Point start_ = Utils.swapYAxis(start, canvas.getHeight());
		Point2D end_ = Utils.swapYAxis(end, canvas.getHeight());
		areaBorder.removeAllSegments()
		.addSegment(start_.getX(), start_.getY(), start_.getX(), end_.getY(), 0xff222222)
		.addSegment(end_.getX(), start_.getY(), end_.getX(), end_.getY(), 0xff222222)
		.addSegment(start_.getX(), start_.getY(), end_.getX(), start_.getY(), 0xff222222)
		.addSegment(start_.getX(), end_.getY(), end_.getX(), end_.getY(), 0xff222222)
		;
		canvas.repaint();
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		areaBorder.removeAllSegments();
		overlay.lines.removeItemToRender(areaBorder);
		if(start != null && end != null){
			Point2D p1 = canvas.transformMouseToCoordSys(start);
			Point2D p2 = canvas.transformMouseToCoordSys(end);
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
	 * @return
	 */
	public CoordSysViewSelector deRegister(){
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		return this;
	}
	
	
	public abstract void areaSelected(double minX, double minY, double maxX, double maxY);
	
}
