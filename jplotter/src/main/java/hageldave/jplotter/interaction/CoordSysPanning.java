package hageldave.jplotter.interaction;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.CoordSysCanvas;

/**
 * The CoordSysPanning class implements a {@link MouseListener}
 * and {@link MouseMotionListener} that realize panning functionality
 * for the coordinate view of the {@link CoordSysCanvas}.
 * When registering this with a CoordSysCanvas dragging with the left mouse
 * button over the Canvas while holding down CTRL will set the coordinate view accordingly.
 * <p>
 * Intended use: {@code CoordSysPanning pan = new CoordSysPanning(canvas).register(); }
 * <p>
 * Per default the extended modifier mask for a dragging mouse event to trigger
 * panning is {@link InputEvent#CTRL_DOWN_MASK}. 
 * If this is undesired the {@link #extModifierMask} has to be overridden.<br>
 * For example to not need to press any key:
 * <pre>new CoordSysPanning(canvas){{extModifierMask=0;}}.register();</pre>
 * 
 * @author hageldave
 */
public class CoordSysPanning implements MouseListener, MouseMotionListener {
	
	protected Point startPoint;
	protected CoordSysCanvas canvas;
	protected int extModifierMask = InputEvent.CTRL_DOWN_MASK;
	
	/**
	 * Creates a new {@link CoordSysPanning} for the specified canvas.
	 * @param canvas to control coordinate view of
	 */
	public CoordSysPanning(CoordSysCanvas canvas) {
		this.canvas = canvas;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(SwingUtilities.isLeftMouseButton(e))
			this.startPoint = e.getPoint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(SwingUtilities.isLeftMouseButton(e) && (e.getModifiersEx()&extModifierMask) == extModifierMask){
			Point dragPoint = e.getPoint();
			double mouseTx = dragPoint.getX()-startPoint.getX();
			double mouseTy = dragPoint.getY()-startPoint.getY();
			startPoint = dragPoint;
			Rectangle2D coordSysFrame = canvas.getCoordSysArea();
			Rectangle2D coordinateArea = canvas.getCoordinateView();
			double relativeTx = mouseTx/coordSysFrame.getWidth();
			double relativeTy = mouseTy/coordSysFrame.getHeight();
			double areaTx = relativeTx*coordinateArea.getWidth();
			double areaTy = relativeTy*coordinateArea.getHeight();
			canvas.setCoordinateView(
					coordinateArea.getMinX()-areaTx, 
					coordinateArea.getMinY()+areaTy,  
					coordinateArea.getMaxX()-areaTx, 
					coordinateArea.getMaxY()+areaTy
			);
			canvas.repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(SwingUtilities.isLeftMouseButton(e))
			startPoint = null;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// NOOP
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// NOOP
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// NOOP
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// NOOP
	}

	/**
	 * Adds this {@link CoordSysPanning} as {@link MouseListener} and
	 * {@link MouseMotionListener} to the associated canvas.
	 * @return this for chaining
	 */
	public CoordSysPanning register(){
		if( ! Arrays.asList(canvas.getMouseListeners()).contains(this))
			canvas.addMouseListener(this);
		if( ! Arrays.asList(canvas.getMouseMotionListeners()).contains(this))
			canvas.addMouseMotionListener(this);
		return this;
	}
	
	/**
	 * Removes this {@link CoordSysPanning} from the associated canvas'
	 * mouse and mouse motion listeners.
	 * @return
	 */
	public CoordSysPanning deRegister(){
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		return this;
	}
	
	
}
