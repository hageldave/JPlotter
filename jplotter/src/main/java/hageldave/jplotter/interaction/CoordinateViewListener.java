package hageldave.jplotter.interaction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import hageldave.jplotter.canvas.CoordSysCanvas;

public interface CoordinateViewListener extends ActionListener {

	@Override
	default void actionPerformed(ActionEvent e) {
		if(e.getSource() instanceof CoordSysCanvas){
			CoordSysCanvas canvas = (CoordSysCanvas) e.getSource();
			coordinateViewChanged(canvas,canvas.getCoordinateView());
		}
	}

	void coordinateViewChanged(CoordSysCanvas src, Rectangle2D view);

}