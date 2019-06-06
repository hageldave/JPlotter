package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.function.DoubleBinaryOperator;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import hageldave.jplotter.renderables.DefaultGlyph;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.util.Utils;

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
		DoubleBinaryOperator fu = (x,y)->(x+y)*(y+1);
		DoubleBinaryOperator fv = (x,y)->(x*y)-(y+1)*y;
		// make quiver plot
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
						Math.atan2(v, u), 
						magnitude != 0 ? (2+magnitude)*0.3 : 0, 
						color
				);
			}
		}
		content.points.addItemToRender(quiver);
		// make trajectory (interactive)
		Lines trajectorySegments = new Lines();
		content.lines.addItemToRender(trajectorySegments);
		MouseAdapter trajectoryInteraction = new MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				calcTrajectory(e.getPoint());
			}
			
			public void mouseDragged(java.awt.event.MouseEvent e) {
				calcTrajectory(e.getPoint());
			}
			
			void calcTrajectory(Point mousePoint){
				Point2D point = canvas.transformAWT2CoordSys(mousePoint);
				double h = 0.02;
				LinkedList<Point2D> trajectory = new LinkedList<>();
				trajectory.add(point);
				for(int i = 0; i < 1000; i++){
					double x = point.getX();
					double y = point.getY();
					// runge kutta 4
					double u0 = fu.applyAsDouble(x, y);
					double v0 = fv.applyAsDouble(x, y);
					double u1 = fu.applyAsDouble(x+u0*h/2, y+v0*h/2);
					double v1 = fv.applyAsDouble(x+u0*h/2, y+v0*h/2);
					double u2 = fu.applyAsDouble(x+u1*h/2, y+v1*h/2);
					double v2 = fv.applyAsDouble(x+u1*h/2, y+v1*h/2);
					double u3 = fu.applyAsDouble(x+u2*h, y+v2*h);
					double v3 = fv.applyAsDouble(x+u2*h, y+v2*h);
					x = x + h*(u0 + 2*u1 + 2*u2 + u3)/6;
					y = y + h*(v0 + 2*v1 + 2*v2 + v3)/6;
					point = new Point2D.Double(x,y);
					trajectory.add(point);
				}
				Utils.execOnAWTEventDispatch(()->{
					trajectorySegments.removeAllSegments();
					trajectorySegments.addLineStrip(new Color(0xffe41a1c), trajectory.toArray(new Point2D[0]));
					canvas.repaint();
				});
			}
		};
		canvas.addMouseListener(trajectoryInteraction);
		canvas.addMouseMotionListener(trajectoryInteraction);
		
		JSlider slider = new JSlider(0, 100, 20);
		slider.addChangeListener((e)->{
			int value = slider.getValue();
			quiver.setGlobalScaling(value*5/100.0);
			canvas.repaint();
		});
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(new JLabel("Arrow Size:"), BorderLayout.WEST);
		bottomPanel.add(slider, BorderLayout.CENTER);
		frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.runInContext(()->canvas.close());
			}
		});
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}
