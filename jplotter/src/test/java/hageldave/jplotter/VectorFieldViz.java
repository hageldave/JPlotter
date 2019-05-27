package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.DoubleBinaryOperator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.renderables.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;

public class VectorFieldViz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Vector Field");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		canvas.setContent(content);

		// setup content
		{
			DoubleBinaryOperator fu = (x,y)->x*y;
			DoubleBinaryOperator fv = (x,y)->x-(y+1)*y;
			Points quiver = new Points(DefaultGlyph.ARROW);
			Color color = new Color(0xff377eb8);
			final int resolution = 21;
			for(int j = 0; j < resolution; j++){
				double y = j*2.0/(resolution-1) -1.0;
				for(int i = 0; i < resolution; i++){
					double x = i*2.0/(resolution-1) -1.0;
					double u = fu.applyAsDouble(x, y);
					double v = fv.applyAsDouble(x, y);
					double magnitude = Math.sqrt(u*u+v*v);
					quiver.addPoint(
							x,y, 
							Math.atan2(u, v), 
							magnitude != 0 ? (1.5+magnitude)*0.3 : 0, 
							color);
				}
			}
			content.points.addItemToRender(quiver);
		}
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.runInContext(()->canvas.close());
			}
		});
		new CoordSysPanning(canvas).register();
		new CoordSysScrollZoom(canvas).register();
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}
