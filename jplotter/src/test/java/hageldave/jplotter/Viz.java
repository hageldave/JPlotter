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
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.TextRenderer;
import hageldave.jplotter.svg.SVGUtils;

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
		content.setRenderOrder(PNT, LIN, TRI, TXT);
		coordsys.setContent(content);
		canvas.setPreferredSize(new Dimension(300, 300));
		
		// setup content
		{
			Triangles tris = new Triangles();
			Lines lines = new Lines();
			lines.setStrokePattern(0xf790);
			double scaling = 0.1;
			Color triColor = new Color(0xff6633aa,true);
			for(int i = 0; i < 100; i++){
				double x1 = i*scaling;
				double x2 = (i+1)*scaling;
				double y1 = Math.sin(x1);
				double y2 = Math.sin(x2);
				lines.addSegment(x1, y1, x2, y2)
					.setColor0(0xffff00ff)
					.setColor1(0xffff00ff)
					.setPickColor(0xbabe01);
				tris.addQuad(x1,0, x1, y1, x2, y2, x2, 0)
					.forEach(tri->tri.setColor(triColor));
			}
			lines.addSegment(0, 0, 100, 100)
				.setColor0(0xff00ff00).setColor1(0xff00ffff);
			tris.setGlobalAlphaMultiplier(0.2);
			lines.setGlobalThicknessMultiplier(2);
			lines.setGlobalAlphaMultiplier(0.8);
			content.lines.addItemToRender(lines);
			content.triangles.addItemToRender(tris);
			
			Points trianglepoints = new Points(DefaultGlyph.TRIANGLE_F);
			Points quiver = new Points(DefaultGlyph.ARROW);
			Color color1 = new Color(0xffe41a1c);
			Color color2 = new Color(0xff377eb8);
			for(int i = 0; i < 100; i++){
				trianglepoints.addPoint(Math.random(), Math.random()).setColor(color1);
				double x = Math.random();
				double y = Math.random();
				quiver.addPoint(x,y)
					.setRotation(Math.atan2(-y, -x))
					.setScaling(Math.sqrt(x*x+y*y))
					.setColor(color2);
			}
			content.points.addItemToRender(trianglepoints).addItemToRender(quiver);
		}
		Legend legend = new Legend();
		legend.addGlyphLabel(DefaultGlyph.TRIANGLE_F, 0xffe41a1c, "rand pnts");
		legend.addGlyphLabel(DefaultGlyph.ARROW, 0xff377eb8, "-(x,y)");
		legend.addLineLabel(2, 0xffff00ff, 0xf790, "sin(x)", 0);
		legend.addLineLabel(2, 0xff00ff00, "x=y");
		coordsys.setLegendRight(legend);
		coordsys.setLegendRightWidth(80);
		
		CompleteRenderer overlay = new CompleteRenderer();
		coordsys.setOverlay(overlay);
		new CoordSysViewSelector(canvas,coordsys) {
			@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
				coordsys.setCoordinateView(minX, minY, maxX, maxY);
			}
		}.register();
		
		coordsys.setCoordinateView(0, 0, 2, 1);
//		frame.getContentPane().add(canvas, BorderLayout.CENTER);
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
//		new CoordSysPanning(canvas,coordsys).register();
		new CoordSysScrollZoom(canvas,coordsys).register();
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
		
		
		BlankCanvas canvas2 = new BlankCanvas();
		canvas2.setDisposeOnRemove(false);
		canvas2.setPreferredSize(new Dimension(100, 100));
		TextRenderer textRenderer = new TextRenderer();
		canvas2.setRenderer(textRenderer);
		textRenderer.addItemToRender(new Text("blank", 15, Font.PLAIN).setOrigin(40, 40));
		
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,canvas,canvas2);
		canvas.setMinimumSize(new Dimension(1, 1));
		canvas2.setMinimumSize(new Dimension(1, 1));
		pane.setDividerLocation(150);
		
		frame.setContentPane(pane);
	}

}
