package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.DoubleBinaryOperator;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Utils;

public class VectorFieldViz {
	
	static JPlotterCanvas mkCanvas(boolean fallback) {
		return fallback ? new BlankCanvasFallback() : new BlankCanvas();
	}
	
	static boolean useFallback(String[] args) {
		return Arrays.stream(args).filter(arg->"jplotter_fallback=true".equals(arg)).findAny().isPresent();
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Vector Field");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(500, 500));
		JPlotterCanvas canvas = mkCanvas(useFallback(args));
		CoordSysRenderer coordsys = new CoordSysRenderer();
		canvas.setRenderer(coordsys);
		CompleteRenderer content = new CompleteRenderer();
		coordsys.setContent(content);

		// setup content
		DoubleBinaryOperator fu = (x,y)->(x+y)*(y+1);
		DoubleBinaryOperator fv = (x,y)->(x*y)-(y+1)*y;
		// make quiver plot
		Points quiver = new Points(DefaultGlyph.ARROW);
		Color color = new Color(DefaultColorMap.Q_9_SET1.getColor(1));
		final int resolution = 21;
		for(int j = 0; j < resolution; j++){
			double y = j*2.0/(resolution-1) -1.0;
			for(int i = 0; i < resolution; i++){
				double x = i*2.0/(resolution-1) -1.0;
				double u = fu.applyAsDouble(x, y);
				double v = fv.applyAsDouble(x, y);
				double magnitude = Math.sqrt(u*u+v*v);
				quiver.addPoint(x,y)
					.setRotation(Math.atan2(v, u))
					.setScaling(magnitude != 0 ? (2+magnitude)*0.3 : 0)
					.setColor(color);
			}
		}
		content.points.addItemToRender(quiver);
		// make trajectory (interactive)
		Lines trajectorySegments = new Lines();
		content.lines.addItemToRender(trajectorySegments);
		MouseAdapter trajectoryInteraction = new MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				if(SwingUtilities.isLeftMouseButton(e))
					calcTrajectory(e.getPoint());
			}
			
			public void mouseDragged(java.awt.event.MouseEvent e) {
				if(SwingUtilities.isLeftMouseButton(e))
					calcTrajectory(e.getPoint());
			}
			
			void calcTrajectory(Point mousePoint){
				Point2D point = coordsys.transformAWT2CoordSys(mousePoint, canvas.asComponent().getHeight());
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
					if(Double.isNaN(x) || Double.isNaN(y) || Math.abs(x) > 1e+3 || Math.abs(y) > 1e+3){
						break;
					}
					point = new Point2D.Double(x,y);
					trajectory.add(point);
				}
				Utils.execOnAWTEventDispatch(()->{
					trajectorySegments.removeAllSegments();
					trajectorySegments.addLineStrip(trajectory.toArray(new Point2D[0]))
						.forEach(seg->seg.setColor(DefaultColorMap.Q_9_SET1.getColor(0)));
					canvas.scheduleRepaint();
				});
			}
		};
		canvas.asComponent().addMouseListener(trajectoryInteraction);
		canvas.asComponent().addMouseMotionListener(trajectoryInteraction);
		
		JSlider slider = new JSlider(0, 100, 20);
		slider.addChangeListener((e)->{
			int value = slider.getValue();
			quiver.setGlobalScaling(value*5/100.0);
			canvas.scheduleRepaint();
		});
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(new JLabel("Arrow Size:"), BorderLayout.WEST);
		bottomPanel.add(slider, BorderLayout.CENTER);
		frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		frame.getContentPane().add(canvas.asComponent(), BorderLayout.CENTER);
		canvas.addCleanupOnWindowClosingListener(frame);
		
		// add a pop up menu (on right click) for exporting to SVG
		PopupMenu menu = new PopupMenu();
		canvas.asComponent().add(menu);
		MenuItem svgExport = new MenuItem("SVG export");
		menu.add(svgExport);
		svgExport.addActionListener(e->{
			Document svg = SVGUtils.containerToSVG(frame.getContentPane());
			SVGUtils.documentToXMLFile(svg, new File("vectorfield_export.svg"));
			System.out.println("exported vectorfield_export.svg");
		});
		canvas.asComponent().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e))
					menu.show(canvas.asComponent(), e.getX(), e.getY());
			}
		});
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}
