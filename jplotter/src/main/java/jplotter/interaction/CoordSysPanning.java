package jplotter.interaction;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import jplotter.CoordSysCanvas;

public class CoordSysPanning implements MouseListener, MouseMotionListener {
	
	Point startPoint;
	CoordSysCanvas canvas;
	
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

	public CoordSysPanning register(){
		if( ! Arrays.asList(canvas.getMouseListeners()).contains(this))
			canvas.addMouseListener(this);
		if( ! Arrays.asList(canvas.getMouseMotionListeners()).contains(this))
			canvas.addMouseMotionListener(this);
		return this;
	}
	
	public CoordSysPanning deRegister(){
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		return this;
	}
	
	
}
