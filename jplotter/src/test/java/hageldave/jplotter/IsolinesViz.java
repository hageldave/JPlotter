package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderables.Triangles.TriangleDetails;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.util.Contours;

public class IsolinesViz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Iso Lines");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		canvas.setContent(content);

		// setup content
		DoubleBinaryOperator f1 = (x,y)->Math.exp(-(x*x+y*y));
		DoubleBinaryOperator f2 = (x,y)->(x*y)-(y+1)*y;
		final int resolution = 100;
		double[][] X = new double[resolution][resolution];
		double[][] Y = new double[resolution][resolution];
		double[][] Z = new double[resolution][resolution];
		for(int j=0; j<X.length;j++) {
			for(int i=0; i<X[0].length; i++) {
				double x = i*8.0/(resolution-1) -4.0;
				double y = j*8.0/(resolution-1) -4.0;
				double z = f1.applyAsDouble(x, y) - f2.applyAsDouble(x, y);
				X[j][i] = x;
				Y[j][i] = y;
				Z[j][i] = z;
			}
		}
		// make contour plot
		Lines contourlines = new Lines();
		Triangles contourbands = new Triangles();
		double[] isoValues = new double[] {
			-2,
			-1,
			-.5,
			0,
			.5,
			1,
			2,
		};
		int[] isoColors = new int[] {
				0xff000044,
				0xff220044,
				0xff441144,
				0xff661144,
				0xff882244,
				0xffaa2244,
				0xffcc3344,
		};
		for(int i = 0; i < isoValues.length; i++) {
			List<SegmentDetails> contours = Contours.computeContourLines(X, Y, Z, isoValues[i], isoColors[i]);
			contourlines.getSegments().addAll(contours);
		}
		for(int i = 0; i < isoValues.length-1; i++) {
			List<TriangleDetails> contours = Contours.computeContourBands(X, Y, Z, isoValues[i], isoValues[i+1], isoColors[i], isoColors[i+1]);
			contourbands.getTriangleDetails().addAll(contours);
		}
		content.addItemToRender(contourlines).addItemToRender(contourbands);
		contourlines.setThickness(1);
		contourbands.setGlobalAlphaMultiplier(0.3);
		new CoordSysScrollZoom(canvas).register();
		new CoordSysPanning(canvas).register();
//		// make trajectory (interactive)
//		Lines trajectorySegments = new Lines();
//		content.lines.addItemToRender(trajectorySegments);
//		MouseAdapter trajectoryInteraction = new MouseAdapter() {
//			public void mousePressed(java.awt.event.MouseEvent e) {
//				calcTrajectory(e.getPoint());
//			}
//
//			public void mouseDragged(java.awt.event.MouseEvent e) {
//				calcTrajectory(e.getPoint());
//			}
//
//			void calcTrajectory(Point mousePoint){
//				Point2D point = canvas.transformMouseToCoordSys(mousePoint);
//				double h = 0.02;
//				LinkedList<Point2D> trajectory = new LinkedList<>();
//				trajectory.add(point);
//				for(int i = 0; i < 1000; i++){
//					double x = point.getX();
//					double y = point.getY();
//					// runge kutta 4
//					double u0 = fu.applyAsDouble(x, y);
//					double v0 = fv.applyAsDouble(x, y);
//					double u1 = fu.applyAsDouble(x+u0*h/2, y+v0*h/2);
//					double v1 = fv.applyAsDouble(x+u0*h/2, y+v0*h/2);
//					double u2 = fu.applyAsDouble(x+u1*h/2, y+v1*h/2);
//					double v2 = fv.applyAsDouble(x+u1*h/2, y+v1*h/2);
//					double u3 = fu.applyAsDouble(x+u2*h, y+v2*h);
//					double v3 = fv.applyAsDouble(x+u2*h, y+v2*h);
//					x = x + h*(u0 + 2*u1 + 2*u2 + u3)/6;
//					y = y + h*(v0 + 2*v1 + 2*v2 + v3)/6;
//					point = new Point2D.Double(x,y);
//					trajectory.add(point);
//				}
//				Utils.execOnAWTEventDispatch(()->{
//					trajectorySegments.removeAllSegments();
//					trajectorySegments.addLineStrip(new Color(0xffe41a1c), trajectory.toArray(new Point2D[0]));
//					canvas.repaint();
//				});
//			}
//		};
//		canvas.addMouseListener(trajectoryInteraction);
//		canvas.addMouseMotionListener(trajectoryInteraction);
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
