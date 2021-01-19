package hageldave.jplotter.howto;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import javax.swing.JFrame;
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
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.CurvesRenderer;
import hageldave.jplotter.renderers.PointsRenderer;

public class BezierDemo {

	private static class LinAlg {

		static double[][] XTX(double[][] x, BiFunction<double[], double[], Double> kernel){
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
		
		static double SIGMA = 4;
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

		static double[] add(double[] a, double[] b, double m){
			for(int i=0; i<a.length; i++){
				a[i] += b[i]*m;
			}
			return a;
		}
		
		static double[] add(double[] a, double[] b, double m, double c){
			for(int i=0; i<a.length; i++){
				a[i] += b[i]*m + c;
			}
			return a;
		}

		static double[] eigenV1(double[][] x){
			int nrows = x.length;
			double[] v = IntStream.range(0, nrows).mapToDouble(i->Math.random() < .5 ? 1.0:-1.0).toArray();
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
			double[] v = IntStream.range(0, nrows).mapToDouble(i->Math.random() < .5 ? 1.0:-1.0).toArray();
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
		
		static double[][] project2D(double[][] x, double[] v1, double[] v2, BiFunction<double[], double[], Double> kernel){
			int nrows = x.length;
			double[][] p = new double[nrows][2];
			for(int r=0; r<nrows; r++){
				p[r][0] = kernel.apply(x[r], v1);
				p[r][1] = kernel.apply(x[r], v2);
			}
			return p;
		}
	}
	
	static double[][] loadDataset() {
//		Class<?> loader = BezierDemo.class;
//		try(InputStream is = loader.getResourceAsStream("/stopmotionframes64x64.png")){
//			BufferedImage image = ImageLoader.loadImage(is, BufferedImage.TYPE_INT_ARGB);
//			int w = image.getWidth()/16;
//			int h = image.getHeight();
//			Img img = new Img(w,h);
//			img.paint(g->g.drawImage(image,0,0,w,h,null));
//			double[][] data = new double[img.getHeight()][img.getWidth()];
//			double toUnit = 1/255.0;
//			img.forEach(true, px -> data[px.getY()][px.getX()]=px.getLuminance()*toUnit);
//			return data;
//		} catch (IOException e) {
//			throw new RuntimeException("sorry something went wrong on loading resource", e);
//		}
		int d = 56;
		int n = d*5;
		double[] init = IntStream.range(0, d).mapToDouble(i->Math.random()).toArray();
		double[][] data = new double[n][];
		data[0] = averageNeighbors(averageNeighbors(init));
		for(int i=1; i<n; i++) {
			data[i] = shift(data[i-1]);
			if(i%(d/3)==0) {
//				data[i] = averageNeighbors(data[i]);
				LinAlg.add(data[i], LinAlg.add(new double[d], init, (i/(d/3))%2 == 0 ? 1:-1, -.5), 0.01);
			}
		}
		return data;
	}
	
	static double[] shift(double[] a) {
		double[] b = new double[a.length];
		for(int i=0; i<a.length; i++)
			b[(i+1)%a.length] = a[i];
		return b;
	}
	
	static double[] averageNeighbors(double[] a) {
		double[] b = new double[a.length];
		for(int i=0; i<a.length; i++) {
			b[i] = a[i]+a[(i+1)%a.length]+a[(i+a.length-1)%a.length];
			b[i]/= 3;
		}
		return b;
	}
	
	static double[][] reduceDimensionality(double[][] data, BiFunction<double[], double[], Double> kernel){
		double[][] x = LinAlg.center(data);
		double[][] xtx = LinAlg.XTX(x, kernel);
		// PCA - compute 1st and 2nd eigenvector
		double[] v1 = LinAlg.eigenV1(xtx);
		double[] v2 = LinAlg.eigenV2(xtx, v1);
		// projection
		return LinAlg.project2D(x, v1, v2, kernel);
	}

	public static void main(String[] args) {
		double[][] data = loadDataset();
		double[][] projection = reduceDimensionality(data, LinAlg::gaussian);
		Point2D[] pointset = Arrays.stream(projection).map(p->new Point2D.Double(p[0], p[1])).toArray(Point2D[]::new);
		// visual elements
		ColorMap cmap = DefaultColorMap.S_VIRIDIS.resample(pointset.length, 0, 0.8);
		Points points = new Points(DefaultGlyph.CIRCLE_F);
		for(int i=0; i<pointset.length; i++)
			points.addPoint(pointset[i]).setColor(cmap.getColor(i));
		Curves curves = new Curves();
		curves.addCurvesThrough(pointset);
		
		// UI
		BlankCanvas timeCurveCanvas = new BlankCanvas();
		timeCurveCanvas.setPreferredSize(new Dimension(400, 400));
		CoordSysRenderer timecurvesCoordsys = new CoordSysRenderer();
		timeCurveCanvas.setRenderer(timecurvesCoordsys);
		timecurvesCoordsys.setContent(
								new PointsRenderer().addItemToRender(points)
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
		
		// boiler plate JFrame
		JFrame frame = new JFrame();
		frame.getContentPane().add(timeCurveCanvas);
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
