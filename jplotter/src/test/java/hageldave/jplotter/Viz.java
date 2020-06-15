package hageldave.jplotter;

import static hageldave.jplotter.renderers.CompleteRenderer.LIN;
import static hageldave.jplotter.renderers.CompleteRenderer.PNT;
import static hageldave.jplotter.renderers.CompleteRenderer.TRI;
import static hageldave.jplotter.renderers.CompleteRenderer.TXT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Curves.CurveDetails;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.CurveRenderer;
import hageldave.jplotter.renderers.SplitScreenRenderer;
import hageldave.jplotter.renderers.TextRenderer;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Utils;

public class Viz {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new JPanel(new BorderLayout()));
		frame.getContentPane().setBackground(Color.white);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));
		BlankCanvas canvas = new BlankCanvas();
		CoordSysRenderer coordsys = new CoordSysRenderer();
		canvas.setRenderer(coordsys);
		canvas.setDisposeOnRemove(false);
		coordsys.setyAxisLabel("Y-Axis");
		CompleteRenderer content = new CompleteRenderer();
		content.setRenderOrder(TRI, PNT, LIN, TXT);
		coordsys.setContent(content);
		canvas.setPreferredSize(new Dimension(300, 300));
		
		Legend legend = new Legend();
		Legend legendB = new Legend();
		legend.addGlyphLabel(DefaultGlyph.TRIANGLE_F, 0xffe41a1c, "rand pnts");
		legend.addGlyphLabel(DefaultGlyph.ARROW, 0xff377eb8, "-(x,y)");
		legend.addLineLabel(2, 0xffff00ff, 0xf790, "sin(x)", 0);
		legend.addLineLabel(2, 0xff00ff00, "x=y");
		legendB.addColormapLabel("M", DefaultColorMap.S_RISING_DEEP_PURPLE, false, new double[]{0,0.5,1,0.75,0.25}, new String[]{"lo","mid","hi", "", ""});
		legendB.addColormapLabel("sads", DefaultColorMap.S_PLASMA, false, new double[]{0,0.5,0.4,0.2,1}, new String[]{"Lo","mig","", "", "hiN"});
		coordsys.setLegendRight(legend);
		coordsys.setLegendRightWidth(80);
		coordsys.setLegendBottom(legendB);
		coordsys.setLegendBottomHeight(60);
		
		new CoordSysPanning(canvas, coordsys).register();
		new CoordSysScrollZoom(canvas, coordsys).register();
		new CoordSysViewSelector(canvas,coordsys) {
			@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
				coordsys.setCoordinateView(minX, minY, maxX, maxY);
			}
		}.register();
		
		coordsys.setCoordinateView(0, 0, 2, 1);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.runInContext(()->canvas.close());
				canvas.disposePlatformCanvas();
			}
		});
		
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int pixel = canvas.getPixel(
						e.getX(), 
						e.getY(), 
						SwingUtilities.isRightMouseButton(e), 
						5);
				System.out.println(Integer.toHexString(pixel));
			}
		});
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e)){
					Document doc = canvas.paintSVG();
					SVGUtils.documentToXMLFile(doc, new File("svgtest.svg"));
					System.out.println("svg exported:");
					System.out.println(SVGUtils.documentToXMLString(doc));
				}
			}
		});
		CurveRenderer curverenderer = new CurveRenderer();
		Curves curves = new Curves().setGlobalThicknessMultiplier(2).setGlobalAlphaMultiplier(0.5).setStrokePattern(0xffff);
//		curves.addCurveStrip(0,0, 1,0, 1,1, .5,1, 0,1, .5,.5, 1,1);
		Point2D[] points = new Point2D[] {p(0,0), p(1,0), p(1,.8), p(.5,1), p(1,1),p(.5,1), p(1,1),p(.5,1),p(1,1),p(1.5,1),p(.5,1)};
		Points maPoints = new Points(DefaultGlyph.CIRCLE_F);
		for(Point2D x:points)
			maPoints.addPoint(x);
		LinkedList<Point2D> samples = new LinkedList<>();
		for(int i = 0; i<100; i++){
			double x = i/20.0;
			double y = Math.sin(x)*(x+1)/x;
			Point2D p = p(x,y);
			maPoints.addPoint(p).setColor(0xffff0000);
			samples.add(p);
		}
		
		
		content.addItemToRender(maPoints);
		curves.addCurvesThrough(points);
		curves.addCurvesThrough(samples.toArray(new Point2D[0])).forEach(c->c.color=()->(0xff3333)));
		curverenderer.addItemToRender(curves);
		coordsys.setContent(content.withAppended(curverenderer));
		canvas.setMinimumSize(new Dimension(1, 1));
		frame.getContentPane().add(canvas);
		
		
	}
	
	static Point2D p(double x, double y) {
		return new Point2D.Double(x, y);
	}

}
