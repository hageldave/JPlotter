package hageldave.jplotter;

import static hageldave.jplotter.renderers.CompleteRenderer.LIN;
import static hageldave.jplotter.renderers.CompleteRenderer.PNT;
import static hageldave.jplotter.renderers.CompleteRenderer.TRI;
import static hageldave.jplotter.renderers.CompleteRenderer.TXT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import hageldave.jplotter.canvas.CoordSysCanvas;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Utils;

public class Viz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setContentPane(new JPanel());
		frame.getContentPane().setBackground(Color.white);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		canvas.setyAxisLabel("Y-Axis");
		CompleteRenderer content = new CompleteRenderer();
		content.setRenderOrder(PNT, LIN, TRI, TXT);
		canvas.setContent(content);
		
		// setup content
		{
			Triangles tris = new Triangles();
			Lines lines = new Lines();
			double scaling = 0.1;
			Color triColor = new Color(0xff6633aa,true);
			for(int i = 0; i < 100; i++){
				double x1 = i*scaling;
				double x2 = (i+1)*scaling;
				double y1 = Math.sin(x1);
				double y2 = Math.sin(x2);
				lines.addSegment(x1, y1, x2, y2, 0xffff00ff,0xffff00ff, 0xbabe01);
				lines.addSegment(i, i, i+1, i+1, Utils.interpolateColor(0xff00ff00, 0xff00ffff, i/99.0));
				tris.addQuad(x1,0, x1, y1, x2, y2, x2, 0, triColor);
			}
			tris.setGlobalAlphaMultiplier(0.2);
			lines.setThickness(2);
			lines.setGlobalAlphaMultiplier(0.8);
			content.lines.addItemToRender(lines);
			content.triangles.addItemToRender(tris);
			
			Points trianglepoints = new Points(DefaultGlyph.TRIANGLE_F);
			Points quiver = new Points(DefaultGlyph.ARROW);
			Color color1 = new Color(0xffe41a1c);
			Color color2 = new Color(0xff377eb8);
			for(int i = 0; i < 100; i++){
				trianglepoints.addPoint(Math.random(), Math.random(), color1);
				double x = Math.random();
				double y = Math.random();
				quiver.addPoint(x,y, Math.atan2(-y, -x), Math.sqrt(x*x+y*y), color2);
			}
			content.points.addItemToRender(trianglepoints).addItemToRender(quiver);
		}
		Legend legend = new Legend();
		legend.addGlyphLabel(DefaultGlyph.TRIANGLE_F, new Color(0xffe41a1c), "rand pnts");
		legend.addGlyphLabel(DefaultGlyph.ARROW, new Color(0xff377eb8), "-(x,y)");
		legend.addLineLabel(2, new Color(0xffff00ff), "sin(x)");
		legend.addLineLabel(2, new Color(0xff00ff00), "x=y");
		canvas.setLegendRight(legend);
		canvas.setLegendRightWidth(80);
		
		CompleteRenderer overlay = new CompleteRenderer();
		canvas.setOverlay(overlay);
		new CoordSysViewSelector(canvas) {
			@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
				canvas.setCoordinateView(minX, minY, maxX, maxY);
			}
		}.register();
		
		canvas.setCoordinateView(0, 0, 2, 1);
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.runInContext(()->canvas.close());
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
//		new CoordSysPanning(canvas).register();
		new CoordSysScrollZoom(canvas).register();
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
		
//		{
//			BlankCanvas bc = new BlankCanvas();
//			bc.setPreferredSize(new Dimension(100,200));
//			JFrame f2 = new JFrame("f2");
//			f2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//			f2.getContentPane().add(bc);
//			bc.setRenderer(legend);
//			SwingUtilities.invokeLater(()->{
//				f2.pack();
//				f2.setVisible(true);
//				f2.transferFocus();
//			});
//		}
	}

}
