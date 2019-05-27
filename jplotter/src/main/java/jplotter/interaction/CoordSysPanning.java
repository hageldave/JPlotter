package jplotter.interaction;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import jplotter.CoordSysCanvas;

/**
 * The CoordSysPanning class implements a {@link MouseListener}
 * and {@link MouseMotionListener} that realize panning functionality
 * for the coordinate view of the {@link CoordSysCanvas}.
 * When registering this with a CoordSysCanvas dragging with the left mouse
 * button over the Canvas will set the coordinate view accordingly.
 * <p>
 * Intended use: {@code CoordSysPanning pan = new CoordSysPanning(canvas).register(); }
 * 
 * @author hageldave
 */
public class CoordSysPanning implements MouseListener, MouseMotionListener {
	
	protected Point startPoint;
	protected CoordSysCanvas canvas;
	
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
		if(SwingUtilities.isLeftMouseButton(e)){
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
