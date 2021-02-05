package hageldave.jplotter.howto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageLoader;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.CurvesRenderer;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.renderers.PointsRenderer;

public class BezierDemo {

	private static class LinAlg {

		static double[][] XTX(double[][] x){
			int nrows = x.length; int ncols = x[0].length;
			double[][] xtx = new double[ncols][ncols];
			for(int r=0; r<ncols; r++){
				for(int c=r; c<ncols; c++){
					double sum = 0;
					for(int k = 0; k<nrows; k++)
						sum += x[k][c] * x[k][r]; 
					xtx[r][c] = xtx[c][r] = sum;
				}
			}
			return xtx;
		}
		
		static double[][] XXT(double[][] x, BiFunction<double[], double[], Double> kernel){
			int nrows = x.length; int ncols = x[0].length;
			double[][] xxt = new double[nrows][nrows];
			for(int r=0; r<nrows; r++){
				for(int c=r; c<nrows; c++){
					double sum = kernel.apply(x[c], x[r]);
					xxt[r][c] = xxt[c][r] = sum;
				}
			}
			return xxt;
		}
		
		static double SIGMA = 5;
		static double gaussian(double[] a, double[] b){
			double[] diff = add(a.clone(), b, -1);
			double dot = dot(diff,diff);
			double sig2 = SIGMA*SIGMA;
			return Math.exp(-dot/sig2);
		}

		static double[][] center(double[][] x){
			int nrows = x.length; int ncols = x[0].length;
			x = Arrays.stream(x).map(row->row.clone()).toArray(double[][]::new);
			double[] colmeans = new double[ncols];
			double toMean = 1.0/nrows;
			for(int c=0; c<ncols; c++){
				for(int r=0; r<nrows; r++)
					colmeans[c] += x[r][c];
				colmeans[c] *= toMean;
			}
			for(int c=0; c<ncols; c++){
				for(int r=0; r<nrows; r++)
					x[r][c] -= colmeans[c];
			}
			return x;
		}

		static double[] normalize(double[] v){
			double len = Math.sqrt(dot(v, v));
			double normalization = 1.0/len;
			for(int i=0; i<v.length; i++)
				v[i] *= normalization;
			return v;
		}

		static double[] Xv(double[][] x, double[] v, double a){
			int nrows = x.length; int ncols = x[0].length;
			double[] q = new double[nrows];
			for(int r=0; r<nrows; r++){
				for(int c=0; c<ncols; c++)
					q[r] += x[r][c]*v[c]*a;
			}
			return q;
		}

		static double[] add(double[] a, double[] b, double c){
			for(int i=0; i<a.length; i++){
				a[i] += b[i]*c;
			}
			return a;
		}

		static double[] eigenV1(double[][] x){
			int nrows = x.length;
			Random r = new Random(0xff00ff00);
			double[] v = IntStream.range(0, nrows).mapToDouble(i->r.nextBoolean() ? 1.0:-1.0).toArray();
			v = normalize(v);
			// power method
			for(int i=0; i<16; i++){
				v = Xv(x,v,-1);
				v = normalize(v);
			}
			return v;
		}

		static double[] eigenV2(double[][] x, double[] v1){
			int nrows = x.length;
			Random r = new Random(0x00ff00ff);
			double[] v = IntStream.range(0, nrows).mapToDouble(i->r.nextBoolean() ? 1.0:-1.0).toArray();
			v = add(v,v1,-dot(v,v1));
			v = normalize(v);
			// power method
			for(int i=0; i<16; i++){
				v = Xv(x,v,-1);
				v = add(v,v1,-dot(v,v1));
				v = normalize(v);
			}
			return v;
		}

		static double dot(double[] a, double[] b){
			double sum = 0;
			for(int i=0; i<a.length; i++){
				sum += a[i]*b[i];
			}
			return sum;
		}
		
		static double[][] project2D(double[][] x, double[] v1, double[] v2){
			int nrows = x.length;
			double[][] p = new double[nrows][2];
			for(int r=0; r<nrows; r++){
				p[r][0] = dot(x[r], v1);
				p[r][1] = dot(x[r], v2);
			}
			return p;
		}
		
		static double[] project_2D_KPCA(double[][] x, double[] v1, double[] v2, BiFunction<double[], double[], Double> kernel, double[] toProject){
			int nrows = x.length;
			double[] p = new double[nrows];
			for(int r=0; r<nrows; r++){
				p[r] = kernel.apply(x[r], toProject);
			}
			return new double[]{dot(v1,p),dot(v2,p)};
		}
	}
	
	static double[][] loadDataset() {
		Class<?> loader = BezierDemo.class;
		try(InputStream is = loader.getResourceAsStream("/stopmotionframes64x64.png")){
			BufferedImage image = ImageLoader.loadImage(is, BufferedImage.TYPE_INT_ARGB);
			Img img = Img.createRemoteImg(image);
			double[][] data = new double[img.getHeight()][img.getWidth()];
			double toUnit = 1/255.0;
			img.forEach(true, px -> data[px.getY()][px.getX()]=px.getLuminance()*toUnit);
			return data;
		} catch (IOException e) {
			throw new RuntimeException("sorry something went wrong on loading resource", e);
		}
	}
	
	static double[][] reduceDimensionality(double[][] data, BiFunction<double[], double[], Double> kernel){
		double[][] x = LinAlg.center(data);
		double[][] xxt = LinAlg.XXT(x, kernel);
		// PCA - compute 1st and 2nd eigenvector
		double[] v1 = LinAlg.eigenV1(xxt);
		double[] v2 = LinAlg.eigenV2(xxt, v1);
		// projection
		double[][] p = new double[x.length][];
		IntStream.range(0, x.length).parallel().forEach(i->
//		for(int i=0;i<x.length;i++)
		{
			p[i] = LinAlg.project_2D_KPCA(x, v1, v2, kernel, x[i]);
		}
		);
			
		return p;
	}
	
	

	public static void main(String[] args) {
		double[][] data = loadDataset();
		Point2D[] pointset = Arrays.stream(data).map(p->new Point2D.Double(p[0], p[1])).toArray(Point2D[]::new);
		
		Runnable runDimRed = () -> {
			double[][] projection = reduceDimensionality(data, LinAlg::gaussian);
			double maxx = Arrays.stream(projection).mapToDouble(v->v[0]).max().getAsDouble();
			double minx = Arrays.stream(projection).mapToDouble(v->v[0]).min().getAsDouble();
			double maxy = Arrays.stream(projection).mapToDouble(v->v[1]).max().getAsDouble();
			double miny = Arrays.stream(projection).mapToDouble(v->v[1]).min().getAsDouble();
			double rx = maxx-minx, ry=maxy-miny;
			for(int i=0; i<pointset.length; i++)
				pointset[i].setLocation((projection[i][0]-minx)/rx, (projection[i][1]-miny)/ry);
		};
		
		runDimRed.run();
		
		// visual elements
		ColorMap cmap = DefaultColorMap.S_VIRIDIS.resample(pointset.length, 0, 0.8);
		Points points = new Points(DefaultGlyph.CIRCLE_F);
		for(int i=0; i<pointset.length; i++)
			points.addPoint(pointset[i]).setColor(cmap.getColor(i));
		Curves curves = new Curves();
		curves.addCurvesThrough(pointset);
		Lines lines = new Lines().setStrokePattern(0xf0f0);
		lines.addLineStrip(
				curves.getCurveDetails().stream()
				.flatMap(c->Arrays.asList(c.p0,c.pc0,c.p1)
				.stream()).toArray(Point2D[]::new))
		.forEach(l->l.setColor(0xffff0000));
		
		// UI
		BlankCanvas timeCurveCanvas = new BlankCanvas();
		timeCurveCanvas.setPreferredSize(new Dimension(400, 400));
		CoordSysRenderer timecurvesCoordsys = new CoordSysRenderer();
		timeCurveCanvas.setRenderer(timecurvesCoordsys);
		timecurvesCoordsys.setContent(
								new PointsRenderer().addItemToRender(points)
				.withAppended(  new LinesRenderer().addItemToRender(lines))
				.withAppended(	new CurvesRenderer().addItemToRender(curves)));
		timecurvesCoordsys.setCoordinateView(points.getBounds());
		new CoordSysPanning(timeCurveCanvas,timecurvesCoordsys).register();
		new CoordSysScrollZoom(timeCurveCanvas, timecurvesCoordsys).register();
		new CoordSysViewSelector(timeCurveCanvas,timecurvesCoordsys) {
			@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
				timecurvesCoordsys.setCoordinateView(minX, minY, maxX, maxY);
			}
		}.register();
		
		Runnable recalcbezier = ()->{
			curves.getCurveDetails().clear();
			curves.addCurvesThrough(pointset);
			lines.removeAllSegments();
			lines.addLineStrip(
					curves.getCurveDetails().stream()
					.flatMap(c->Arrays.asList(c.p0,c.pc0,c.p1)
					.stream()).toArray(Point2D[]::new))
			.forEach(l->l.setColor(0xffff0000));
		};
		timecurvesCoordsys.addCoordinateViewListener((src,view)->{
			curves.getCurveDetails().clear();
			curves.addCurvesThrough(pointset);
			lines.removeAllSegments();
			lines.addLineStrip(
					curves.getCurveDetails().stream()
					.flatMap(c->Arrays.asList(c.p0,c.pc0,c.pc1)
					.stream()).toArray(Point2D[]::new))
			.forEach(l->l.setColor(0xffff0000));
		});
		
		JSlider slider = new JSlider(0,100,50);
		slider.addChangeListener(e->{
			double v = slider.getValue();
			v -= 50;
			v /= 10;
			v = 5+v;
			LinAlg.SIGMA = v;
			runDimRed.run();
			points.setDirty();
			recalcbezier.run();
			timeCurveCanvas.scheduleRepaint();
		});
		
		// boiler plate JFrame
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(timeCurveCanvas, BorderLayout.CENTER);
		frame.getContentPane().add(slider, BorderLayout.NORTH);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				timeCurveCanvas.runInContext(()->timeCurveCanvas.close());
			}
		});
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// launch
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
	}
	
	
	
	
	
	
	
	
	
	
}
