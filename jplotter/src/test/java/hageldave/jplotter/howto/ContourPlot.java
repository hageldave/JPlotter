package hageldave.jplotter.howto;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.misc.Contours;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderables.Triangles.TriangleDetails;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;

public class ContourPlot {

	public static void main(String[] args) {
		// formulate bivariate function that defines the 2D surface
		DoubleBinaryOperator bivariateFn = (x,y)->(x*y + x*x - y*y -.01);
		// sample the function
		int hsamples=100, vsamples=100;
		double[][] X = new double[vsamples][hsamples];
		double[][] Y = new double[vsamples][hsamples];
		double[][] Z = new double[vsamples][hsamples];
		for(int i=0; i<vsamples; i++){
			for(int j=0; j<hsamples; j++){
				double x=j*1.0/(hsamples-1), y=i*1.0/(vsamples-1);
				x=x*2-1; y=y*2-1; /* range[-1,1] */
				X[i][j]=x; 
				Y[i][j]=y;
				Z[i][j]=bivariateFn.applyAsDouble(x,y);
			}
		}
		// define colors for isoline levels (3 levels)
		ColorMap colormap = DefaultColorMap.D_COOL_WARM;
		int[] colors = colormap.resample(3, 0, 1).getColors();
		// calculate contour lines from samples for iso values -0.5, 0.0 and 0.5
		List<SegmentDetails> contourA = Contours.computeContourLines(
				X,Y,Z, -0.5, colors[0]);
		List<SegmentDetails> contourB = Contours.computeContourLines(
				X,Y,Z,  0.0, colors[1]);
		List<SegmentDetails> contourC = Contours.computeContourLines(
				X,Y,Z,  0.5, colors[2]);
		// put the contour segments into a Lines object
		Lines contourlines = new Lines();
		contourlines.getSegments().addAll(contourA);
		contourlines.getSegments().addAll(contourB);
		contourlines.getSegments().addAll(contourC);
		// calculate contour bands in between lines
		List<TriangleDetails> bandAB = Contours.computeContourBands(
				X,Y,Z, -0.5,0.0, colors[0],colors[1]);
		List<TriangleDetails> bandBC = Contours.computeContourBands(
				X,Y,Z,  0.0,0.5, colors[1],colors[2]);
		// put the band triangles into a Triangles object
		Triangles contourbands = new Triangles();
		contourbands.setGlobalAlphaMultiplier(0.3);
		contourbands.getTriangleDetails().addAll(bandAB);
		contourbands.getTriangleDetails().addAll(bandBC);
		// use a coordinate system for display
		BlankCanvas canvas = new BlankCanvas();
		CoordSysRenderer coordsys = new CoordSysRenderer();
		canvas.setRenderer(coordsys);
		canvas.setPreferredSize(new Dimension(400, 400));
		canvas.setBackground(Color.WHITE);
		coordsys.setCoordinateView(-1,-1,1,1);
		// set the content renderer of the coordinate system 
		// we want to render Lines and Triangles objects
		CompleteRenderer content = new CompleteRenderer();
		content.addItemToRender(contourbands).addItemToRender(contourlines);
		coordsys.setContent(content);
		
		// display within a JFrame
		JFrame frame = new JFrame();
		frame.getContentPane().add(canvas);
		frame.setTitle("contourplot");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// code to clean up opengl resources
				canvas.runInContext(()->canvas.close());
			}
		});
		// make visible on AWT event dispatch thread
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		
		/* we cheated a bit before when creating the contour bands,
		 * we assumed the colormap to be linear and just used the
		 * 3 colors we extracted. Since the color map is not linear
		 * we need to calculate bands for each step of the map so that
		 * its color scheme is preserved.
		 */
		double loIso=-0.5, hiIso=0.5;
		List<TriangleDetails> tris = new LinkedList<>();
		for(int i=0; i<colormap.numColors()-1; i++){
			double m1 = colormap.getLocation(i);
			double m2 = colormap.getLocation(i+1);
			// calc iso values corresponding to locations in color map
			double iso1 = loIso+m1*(hiIso-loIso);
			double iso2 = loIso+m2*(hiIso-loIso);
			int color1 = colormap.getColor(i);
			int color2 = colormap.getColor(i+1);
			tris.addAll(Contours.computeContourBands(X,Y,Z, iso1, iso2, color1, color2));
		}
		// replace triangles
		contourbands.removeAllTriangles();
		contourbands.getTriangleDetails().addAll(tris);
		canvas.scheduleRepaint();
		
		
		long t=System.currentTimeMillis()+2000;
		while(t>System.currentTimeMillis());
		if("false".equals("true"))
			SwingUtilities.invokeLater(()->{
				Img img = new Img(frame.getSize());
				img.paint(g2d->frame.paintAll(g2d));
				ImageSaver.saveImage(img.getRemoteBufferedImage(), "howto_contourplot.png");
			});

	}

}
