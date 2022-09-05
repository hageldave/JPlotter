package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.renderers.PointsRenderer;
import hageldave.jplotter.svg.SVGUtils;

public class TestExportRendering {

	public static void main(String[] args) {
		JFrame frame = Boilerplate.createJFrameWithBoilerPlate("layout test");
		Container components = mkComponents();
		frame.setContentPane(components);
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		// idle a bit so system has time to display on screen and render
		long time = System.currentTimeMillis();
		while(time + 500 > System.currentTimeMillis()) {
			Thread.yield();
		}
		SwingUtilities.invokeLater(()->{
			// SVG
			{
				Document svg = SVGUtils.containerToSVG(components);
				SVGUtils.documentToXMLFile(svg, new File("testexport.svg"));
			}
			// PDF
			try {
				PDDocument pdf = PDFUtils.containerToPDF(components);
				pdf.save("testexport.pdf");
			} catch (IOException e) {
				e.printStackTrace();
			}
			// PNG
			{
				Img img = new Img(components.getSize());
				img.paint(components::paintAll);
				ImageSaver.saveImage(img.getRemoteBufferedImage(), "testexport.png");
			}
			System.out.println("exported");
		});
	}
	
	static Container mkComponents() {
		Container all = new Container();
		all.setLayout(new BorderLayout());
		Container center = new Container();
		center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
		all.add(center, BorderLayout.CENTER);
		for(int i=0; i<3; i++) {
			Container column = new Container();
			column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
			column.add(mkCanvasWithCoordSys(new Dimension(200,200)).asComponent());
			column.add(mkCanvasWithCoordSys(new Dimension(200,100)).asComponent());
			center.add(column);
		}
		all.add(mkSeparator(new Dimension(2, 200)), BorderLayout.WEST);
		all.add(mkSeparator(new Dimension(2, 200)), BorderLayout.EAST);
		return all;
	}
	
	static JPlotterCanvas mkCanvasWithCoordSys(Dimension prefSize) {
		JPlotterCanvas canvas = new BlankCanvasFallback();
		CoordSysRenderer coordsys = new CoordSysRenderer();
		canvas.setRenderer(coordsys);
		Points p = new Points();
		for(int i=0; i<100; i++)
			p.addPoint(Math.random(), Math.random());
		PointsRenderer pr = new PointsRenderer();
		pr.addItemToRender(p);
		coordsys.setContent(pr);
		canvas.asComponent().setPreferredSize(prefSize);
		return canvas;
	}
	
	public static Container mkSeparator(Dimension prefSize) {
		JPlotterCanvas canvas = new BlankCanvasFallback();
		LinesRenderer renderer = new LinesRenderer();
		Lines lines = new Lines();
		lines.setGlobalThicknessMultiplier(2);
		lines.addSegment(0, 1, 0, 0);
		lines.setStrokePattern(0xf0f0);
		renderer.addItemToRender(lines);
		renderer.setView(new Rectangle2D.Double(-.5, 0, 1, 1));
		canvas.setRenderer(renderer);
		Container c = new Container();
		c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
		c.add(canvas.asComponent());
		c.setPreferredSize(prefSize);
		return c;
	}
	
}
