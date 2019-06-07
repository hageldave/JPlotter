package hageldave.jplotter.interaction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import hageldave.jplotter.canvas.CoordSysCanvas;

/**
 * The CoordinateViewListener is a listener that listens on changes to a 
 * {@link CoordSysCanvas}' coordinate view 
 * (i.e. {@link CoordSysCanvas#setCoordinateView(double, double, double, double)}).
 * 
 * @author hageldave
 */
public interface CoordinateViewListener extends ActionListener {

	@Override
	default void actionPerformed(ActionEvent e) {
		if(e.getSource() instanceof CoordSysCanvas){
			CoordSysCanvas canvas = (CoordSysCanvas) e.getSource();
			coordinateViewChanged(canvas,canvas.getCoordinateView());
		}
	}

	/**
	 * Method will be called when the source's coordinate view changed.
	 * @param src source {@link CoordSysCanvas}
	 * @param view the new coordinate view
	 */
	void coordinateViewChanged(CoordSysCanvas src, Rectangle2D view);

}