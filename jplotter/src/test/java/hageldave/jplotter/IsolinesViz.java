package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.misc.Contours;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderables.Triangles.TriangleDetails;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.svg.SVGUtils;

public class IsolinesViz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Iso Lines");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(500, 450));
		BlankCanvasFallback canvas = new BlankCanvasFallback();
//		BlankCanvas canvas = new BlankCanvas();
		CoordSysRenderer coordsys = new CoordSysRenderer();
		canvas.setRenderer(coordsys);
		CompleteRenderer content = new CompleteRenderer();
		coordsys.setContent(content);
		Legend legend = new Legend();
		coordsys.setLegendRight(legend);
		coordsys.setLegendRightWidth(50);
		canvas.setBackground(Color.WHITE);

		// setup content
		DoubleBinaryOperator f1 = (x,y)->Math.exp(-(x*x+y*y));
		DoubleBinaryOperator f2 = (x,y)->(x*y)-(y+1)*y;
		DoubleBinaryOperator f = (x,y)->f1.applyAsDouble(x, y)-f2.applyAsDouble(x, y);
		final int resolution = 200;
		double[][] X = new double[resolution][resolution];
		double[][] Y = new double[resolution][resolution];
		double[][] Z = new double[resolution][resolution];
		for(int j=0; j<X.length;j++) {
			for(int i=0; i<X[0].length; i++) {
				double x = i*8.0/(resolution-1) -4.0;
				double y = j*8.0/(resolution-1) -4.0;
				double z = f.applyAsDouble(x, y);
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
		ColorMap isoColors = DefaultColorMap.S_COPPER;
		for(int i = isoValues.length-1; i >= 0; i--) {
			List<SegmentDetails> contours = Contours.computeContourLines(X, Y, Z, isoValues[i], isoColors.getColor(i));
			contourlines.getSegments().addAll(contours);
			legend.addLineLabel(1, isoColors.getColor(i), isoValues[i] < 0 ? ""+isoValues[i]:" "+isoValues[i]);
		}
		for(int i = 0; i < isoValues.length-1; i++) {
			List<TriangleDetails> contours = Contours.computeContourBands(X, Y, Z, isoValues[i], isoValues[i+1], isoColors.getColor(i), isoColors.getColor(i+1));
			contourbands.getTriangleDetails().addAll(contours);
		}
		content.addItemToRender(contourlines).addItemToRender(contourbands);
		contourlines.setGlobalThicknessMultiplier(1);
		contourbands.setGlobalAlphaMultiplier(0.3);
		new CoordSysScrollZoom(canvas, coordsys).register();
		new CoordSysPanning(canvas, coordsys).register();
		coordsys.setCoordinateView(-2.5, -1.5, 0.5, 1.5);
		
		Lines userContour = new Lines();
		Text userIsoLabel = new Text("", 10, Font.ITALIC);
		coordsys.setContent(content);
		content.addItemToRender(userContour);
		content.addItemToRender(userIsoLabel);
		MouseAdapter contourPlacer = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if(e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK)
					calcContour(e.getPoint());
			};
			
			public void mouseDragged(MouseEvent e) {
				if(e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK)
					calcContour(e.getPoint());
			};
			
			void calcContour(Point mp){
				Point2D p = coordsys.transformAWT2CoordSys(mp, canvas.getHeight());
				double isoValue = f.applyAsDouble(p.getX(), p.getY());
				userIsoLabel
					.setTextString(String.format("%.3f", isoValue))
					.setColor(0xff8844bb)
					.setOrigin(p)
					.setBackground(0xaaffffff);
				List<SegmentDetails> contourSegments = Contours.computeContourLines(X, Y, Z, isoValue, 0xff8844bb);
				userContour.removeAllSegments().getSegments().addAll(contourSegments);
				canvas.repaint();
			}
		};
		canvas.addMouseListener(contourPlacer);
		canvas.addMouseMotionListener(contourPlacer);
		
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e)){
					Document doc = canvas.paintSVG();
					SVGUtils.documentToXMLFile(doc, new File("svgtest.svg"));
					System.out.println("svg exported.");
				}
			}
		});

		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				Object obj = canvas;
				if(obj instanceof FBOCanvas)
					((FBOCanvas)obj).runInContext(()->((FBOCanvas)obj).close());
			}
		});
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}
