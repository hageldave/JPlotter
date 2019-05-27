package jplotter.interaction;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;

import jplotter.CoordSysCanvas;

public class CoordSysScrollZoom implements MouseWheelListener {

	CoordSysCanvas canvas;
	
	public CoordSysScrollZoom(CoordSysCanvas canvas) {
		this.canvas = canvas;
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int wheelRotation = e.getWheelRotation();
		double zoom = Math.pow(2, wheelRotation);
		double centerX = canvas.getCoordinateView().getCenterX();
		double centerY = canvas.getCoordinateView().getCenterY();
		double width = canvas.getCoordinateView().getWidth();
		double height = canvas.getCoordinateView().getHeight();
		width *= zoom;
		height *= zoom;
		canvas.setCoordinateView(
				centerX-width/2,
				centerY-height/2,
				centerX+width/2,
				centerY+height/2
		);
		canvas.repaint();
	}
	
	public CoordSysScrollZoom register(){
		if( ! Arrays.asList(canvas.getMouseWheelListeners()).contains(this))
			canvas.addMouseWheelListener(this);
		return this;
	}
	
	public CoordSysScrollZoom deRegister(){
		canvas.removeMouseWheelListener(this);
		return this;
	}

}
