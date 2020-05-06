package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.PointsRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.renderers.TrianglesRenderer;

public class SwapContext {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		BlankCanvas canvas1 = new BlankCanvas();
		BlankCanvas canvas2 = new BlankCanvas();
		
		CoordSysRenderer coordsys = new CoordSysRenderer();
		
		canvas1.setRenderer(coordsys);
		Points points = new Points(DefaultGlyph.SQUARE_F);
		points.addPoint(0.1, 0.3);
		Triangles tris = new Triangles();
		tris.addTriangle(0, 0, 1, 1, 0, 1);
		coordsys.setContent(
				new TrianglesRenderer()
				.addItemToRender(tris)
				.withAppended(
						new PointsRenderer()
						.addItemToRender(points)
				)
		);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(canvas1,BorderLayout.NORTH);
		frame.getContentPane().add(canvas2,BorderLayout.SOUTH);
		
		JButton b = new JButton("swap");
		b.addActionListener(e->{
			
			if(canvas1.getRenderer() !=null) {
				Renderer renderer = canvas1.getRenderer();
				canvas1.runInContext(()->renderer.close());
				canvas1.setRenderer(null);
				canvas2.setRenderer(renderer);
			} else if(canvas2.getRenderer() !=null) {
				Renderer renderer = canvas2.getRenderer();
				canvas2.runInContext(()->renderer.close());
				canvas2.setRenderer(null);
				canvas1.setRenderer(renderer);
			}
			canvas1.scheduleRepaint();
			canvas2.scheduleRepaint();
		});
		
		frame.getContentPane().add(b, BorderLayout.CENTER);
		
		canvas1.setPreferredSize(new Dimension(300, 300));
		canvas2.setPreferredSize(new Dimension(300, 500));
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
	}

}
