package hageldave.jplotter.howto;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.IntStream;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageLoader;

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
			double len=0;
			for(int i=0; i<v.length; i++) 
				len += v[i]*v[i];
			len = Math.sqrt(len);
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
		
		static double[][] project2D(double[][] x, double[] v1, double[] v2){
			int nrows = x.length;
			double[][] p = new double[nrows][2];
			for(int r=0; r<nrows; r++){
				p[r][0] = dot(x[r], v1);
				p[r][1] = dot(x[r], v2);
			}
			return p;
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
	
	static double[][] reduceDimensionality(double[][] data){
		double[][] x = LinAlg.center(data);
		double[][] xtx = LinAlg.XTX(x);
		// PCA - compute 1st and 2nd eigenvector
		double[] v1 = LinAlg.eigenV1(xtx);
		double[] v2 = LinAlg.eigenV2(xtx, v1);
		// project
		return LinAlg.project2D(x, v1, v2);
	}

	public static void main(String[] args) {
		double[][] data = loadDataset();
		double[][] projection = reduceDimensionality(data);
		
	}
}
