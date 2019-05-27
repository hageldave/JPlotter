package jplotter.interaction;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;

import jplotter.CoordSysCanvas;

/**
 * The CoordSysScrollZoom class implements a {@link MouseWheelListener}
 * that realize zooming functionality for the coordinate view of the {@link CoordSysCanvas}.
 * When registering this with a CoordSysCanvas turning the scroll wheel zooms into or out of
 * the current coordinate system view.
 * The zoom factor can be set and is by default 2.0.
 * <p>
 * Intended use: {@code CoordSysScrollZoom zoom = new CoordSysScrollZoom(canvas).register(); }
 * 
 * @author hageldave
 */
public class CoordSysScrollZoom implements MouseWheelListener {

	protected CoordSysCanvas canvas;
	protected double zoomFactor = 2;
	
	public CoordSysScrollZoom(CoordSysCanvas canvas) {
		this.canvas = canvas;
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int wheelRotation = e.getWheelRotation();
		double zoom = Math.pow(zoomFactor, wheelRotation);
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
	
	/**
	 * Sets the zoom factor of this {@link CoordSysScrollZoom}.
	 * The default value is 2.0.
	 * Using a value in ]0,1[ will reverse the zoom direction.
	 * @param zoomFactor to be set
	 * @return this for chaining
	 */
	public CoordSysScrollZoom setZoomFactor(double zoomFactor) {
		this.zoomFactor = zoomFactor;
		return this;
	}
	
	public double getZoomFactor() {
		return zoomFactor;
	}
	
	/**
	 * Adds this {@link CoordSysScrollZoom} as {@link MouseWheelListener} to the associated canvas.
	 * @return this for chaining
	 */
	public CoordSysScrollZoom register(){
		if( ! Arrays.asList(canvas.getMouseWheelListeners()).contains(this))
			canvas.addMouseWheelListener(this);
		return this;
	}
	
	/**
	 * Removes this {@link CoordSysScrollZoom} from the associated canvas'
	 * mouse wheel listeners.
	 * @return
	 */
	public CoordSysScrollZoom deRegister(){
		canvas.removeMouseWheelListener(this);
		return this;
	}

}
