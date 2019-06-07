package hageldave.jplotter.misc;

import static hageldave.jplotter.util.Utils.interpolateColor;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Triangles.TriangleDetails;

/**
 * The Contours class provides methods to compute contour lines and contour bands
 * from a 2D regular grid of scalar values.
 * See {@link #computeContourLines(double[][], double, int)}
 * and {@link #computeContourBands(double[][], double, double, int, int)}
 * 
 * @author hageldave
 */
public class Contours {
	
	/**
	 * Computes the contour lines from the grid samples of a bivariate function z(x,y).
	 * The resulting line segments are solutions to the equation { (x,y) | z(x,y)=iso }
	 * within the grid.
	 * @param X x-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (X<sub>ij</sub>,Y<sub>ij</sub>,Z<sub>ij</sub>) )
	 * @param Y y-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (X<sub>ij</sub>,Y<sub>ij</sub>,Z<sub>ij</sub>) )
	 * @param Z z-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (X<sub>ij</sub>,Y<sub>ij</sub>,Z<sub>ij</sub>) )
	 * @param isoValue the iso value for which the contour (iso) lines should be computed
	 * @param color integer packed ARGB color value the returned line segments should have, e.g. 0xff00ff00 for opaque green.
	 * @return list of line segments that form the contour lines. There is no particular order so subsequent segments are not 
	 * necessarily adjacent.
	 */
	public static List<SegmentDetails> computeContourLines(double[][] X, double[][] Y, double[][] Z, double isoValue, int color){
		List<SegmentDetails> contourLines = computeContourLines(Z, isoValue, color);
		for(SegmentDetails segment:contourLines){
			for(Point2D p : Arrays.asList(segment.p0,segment.p1)){
				int i = (int)p.getX();
				int j = (int)p.getY();
				double mi = p.getX()-i;
				double mj = p.getY()-j;
				double xcoord = X[j][i];
				if(mi > 1e-6){
					xcoord = X[j][i]+mi*(X[j][i+1]-X[j][i]);
				}
				double ycoord = Y[j][i];
				if(mj > 1e-6){
					ycoord = Y[j][i]+mj*(Y[j+1][i]-Y[j][i]);
				}
				p.setLocation(xcoord, ycoord);
			}
		}
		return contourLines;
	}
	
	/**
	 * Computes the contour bands from the grid samples of a bivariate function z(x,y).
	 * The resulting triangles are solutions to the equation { (x,y) | iso1 &lt; z(x,y) &lt; iso2 }
	 * within the grid.
	 * @param X x-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (X<sub>ij</sub>,Y<sub>ij</sub>,Z<sub>ij</sub>) )
	 * @param Y y-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (X<sub>ij</sub>,Y<sub>ij</sub>,Z<sub>ij</sub>) )
	 * @param Z z-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (X<sub>ij</sub>,Y<sub>ij</sub>,Z<sub>ij</sub>) )
	 * @param isoValue1 the lower bound for values of the iso bands
	 * @param isoValue2 the upper bound for values of the iso bands
	 * @param c1 color for the isoValue1
	 * @param c2 color for the isoValue2, values in between iso1 and iso2 will have their color linearly interpolated
	 * @return list of triangles that form the iso bands. The order of triangles does NOT imply any adjacency between them.
	 */
	public static List<TriangleDetails> computeContourBands(double[][] X, double[][] Y, double[][] Z, double isoValue1, double isoValue2, int c1, int c2){
		List<TriangleDetails> contourBands = computeContourBands(Z, isoValue1, isoValue2, c1, c2);
		double[] xCoords = new double[3];
		double[] yCoords = new double[3];
		for(TriangleDetails tri:contourBands){
			xCoords[0]=tri.x0; xCoords[1]=tri.x1; xCoords[2]=tri.x2;
			yCoords[0]=tri.y0; yCoords[1]=tri.y1; yCoords[2]=tri.y2;
			for(int t=0; t<3; t++){
				int i = (int)xCoords[t];
				int j = (int)yCoords[t];
				double mi = xCoords[t]-i;
				double mj = yCoords[t]-j;
				double xcoord = X[j][i];
				if(mi > 1e-6){
					xcoord = X[j][i]+mi*(X[j][i+1]-X[j][i]);
				}
				double ycoord = Y[j][i];
				if(mj > 1e-6){
					ycoord = Y[j][i]+mj*(Y[j+1][i]-Y[j][i]);
				}
				xCoords[t] = xcoord;
				yCoords[t] = ycoord;
			}
			tri.x0=(float)xCoords[0]; tri.x1=(float)xCoords[1]; tri.x2=(float)xCoords[2];
			tri.y0=(float)yCoords[0]; tri.y1=(float)yCoords[1]; tri.y2=(float)yCoords[2];
		}
		return contourBands;
	}

	/**
	 * Computes the contour lines from the grid samples of a bivariate function z(x,y)<br>
	 * with implicit integer valued (x,y) = (i,j).<br>
	 * The resulting line segments are solutions to the equation { (x,y) | z(x,y)=iso }
	 * within the grid.
	 * <p>
	 * The lines are computed using the Meandering Triangles algorithm which divides each
	 * square cell of the grid into 2 triangle cells first before computing isovalue intersections
	 * on the triangle sides.
	 * The more well known Marching Squares algorithm has significantly more cell cases to check, 
	 * which is why Meandering Triangles was preferred here.
	 * 
	 * @param uniformGridSamples z-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (i,j,Z<sub>ij</sub>) )
	 * @param isoValue the iso value for which the contour (iso) lines should be computed
	 * @param color integer packed ARGB color value the returned line segments should have, e.g. 0xff00ff00 for opaque green.
	 * @return list of line segments that form the contour lines. There is no particular order so subsequent segments are not 
	 * necessarily adjacent.
	 */
	public static List<SegmentDetails> computeContourLines(double[][] uniformGridSamples, double isoValue, int color){
		int height = uniformGridSamples.length;
		int width = uniformGridSamples[0].length;
		double[][] f = uniformGridSamples; // shorthand
		// mark nodes that have a greater value than the iso value
		boolean[][] greaterThanIso = new boolean[height][width];
		for(int i=0; i<height; i++){
			for(int j=0; j<width; j++){
				greaterThanIso[i][j] = f[i][j] > isoValue;
			}
		}
		LinkedList<SegmentDetails> cntrLineSegments = new LinkedList<>();
		/* 
		 * go through all cells, determine cell type and add corresponding line segments to list
		 */
		for(int i=0; i<height-1; i++){
			for(int j=0; j<width-1; j++){
				for(int t=0; t<2;t++){
					int celltype;
					double tx0,ty0,tx1,ty1,tx2,ty2;
					double v0, v1, v2;
					if(t == 0){
						// lt, rt, lb
						tx0=j+0; ty0=i+0;
						tx1=j+1; ty1=i+0;
						tx2=j+0; ty2=i+1;
						celltype = celltype(
								greaterThanIso[i][j], 
								greaterThanIso[i][j+1], 
								greaterThanIso[i+1][j]);
						v0 = f[i][j];
						v1 = f[i][j+1];
						v2 = f[i+1][j];
					} else {
						// rb, lb, rt
						tx0=j+1; ty0=i+1;
						tx1=j+0; ty1=i+1;
						tx2=j+1; ty2=i+0;
						celltype = celltype(
								greaterThanIso[i+1][j+1], 
								greaterThanIso[i+1][j], 
								greaterThanIso[i][j+1]);
						v0 = f[i+1][j+1];
						v1 = f[i+1][j];
						v2 = f[i][j+1];
					}
					switch (celltype) {
					// non intersecting celltypes
					case 0b000: // fall through
					case 0b111: // no intersection of isoline in this cell
						break;
					case 0b100:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v1, v0, isoValue);
						m1 = 1-interpolateToValue(v2, v0, isoValue);
						x0 = tx0+m0*(tx1-tx0);
						y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx2-tx0);
						y1 = ty0+m1*(ty2-ty0);
						cntrLineSegments.add(new SegmentDetails(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), color,color,0));
						break;
					}
					case 0b010:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v0, v1, isoValue);
						m1 = 1-interpolateToValue(v2, v1, isoValue);
						x0 = tx1+m0*(tx0-tx1);
						y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx2-tx1);
						y1 = ty1+m1*(ty2-ty1);
						cntrLineSegments.add(new SegmentDetails(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), color,color,0));
						break;
					}
					case 0b001:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v0, v2, isoValue);
						m1 = 1-interpolateToValue(v1, v2, isoValue);
						x0 = tx2+m0*(tx0-tx2);
						y0 = ty2+m0*(ty0-ty2);
						x1 = tx2+m1*(tx1-tx2);
						y1 = ty2+m1*(ty1-ty2);
						cntrLineSegments.add(new SegmentDetails(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), color,color,0));
						break;
					}
					case 0b011:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v0, v1, isoValue);
						m1 = interpolateToValue(v0, v2, isoValue);
						x0 = tx0+m0*(tx1-tx0);
						y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx2-tx0);
						y1 = ty0+m1*(ty2-ty0);
						cntrLineSegments.add(new SegmentDetails(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), color,color,0));
						break;
					}
					case 0b101:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v1, v0, isoValue);
						m1 = interpolateToValue(v1, v2, isoValue);
						x0 = tx1+m0*(tx0-tx1);
						y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx2-tx1);
						y1 = ty1+m1*(ty2-ty1);
						cntrLineSegments.add(new SegmentDetails(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), color,color,0));
						break;
					}
					case 0b110:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v2, v0, isoValue);
						m1 = interpolateToValue(v2, v1, isoValue);
						x0 = tx2+m0*(tx0-tx2);
						y0 = ty2+m0*(ty0-ty2);
						x1 = tx2+m1*(tx1-tx2);
						y1 = ty2+m1*(ty1-ty2);
						cntrLineSegments.add(new SegmentDetails(new Point2D.Double(x0, y0), new Point2D.Double(x1, y1), color,color,0));
						break;
					}
					default:
						break;
					}
				}
			}
		}
		return cntrLineSegments;
	}
	
	/**
	 * Computes the contour bands from the grid samples of a bivariate function z(x,y)<br>
	 * with implicit integer valued (x,y) = (i,j).<br>
	 * The resulting triangles are solutions to the equation { (x,y) | iso1 &lt; z(x,y) &lt; iso2 }
	 * within the grid.
	 * <p>
	 * The triangles are computed using the Meandering Triangles algorithm which divides each
	 * square cell of the grid into 2 triangle cells first before computing isovalue intersections
	 * on the triangle sides.
	 * The more well known Marching Squares algorithm has significantly more cell cases to check, 
	 * which is why Meandering Triangles was preferred here.
	 * 
	 * @param uniformGridSamples z-coordinates of the grid points ( (x,y,z)<sub>ij</sub> = (i,j,Z<sub>ij</sub>) )
	 * @param isoValue1 the lower bound for values of the iso bands
	 * @param isoValue2 the upper bound for values of the iso bands
	 * @param c1 color for the isoValue1
	 * @param c2 color for the isoValue2, values in between iso1 and iso2 will have their color linearly interpolated
	 * @return list of triangles that form the iso bands. The order of triangles does NOT imply any adjacency between them.
	 */
	public static List<TriangleDetails> computeContourBands(double[][] uniformGridSamples, double isoValue1, double isoValue2, int c1, int c2){
		if(isoValue1 > isoValue2){
			// swap
			double temp = isoValue2;
			isoValue2 = isoValue1;
			isoValue1 = temp;
			int tempc = c2;
			c2 = c1;
			c1 = tempc;
		}
		int height = uniformGridSamples.length;
		int width = uniformGridSamples[0].length;
		double[][] f = uniformGridSamples; // shorthand
		// mark nodes that have a greater value than the iso value
		boolean[][] greaterThanIso1 = new boolean[height][width];
		boolean[][] greaterThanIso2 = new boolean[height][width];
		for(int i=0; i<height; i++){
			for(int j=0; j<width; j++){
				greaterThanIso1[i][j] = f[i][j] > isoValue1;
				greaterThanIso2[i][j] = f[i][j] > isoValue2;
			}
		}
		LinkedList<TriangleDetails> tris = new LinkedList<>();
		/* 
		 * go through all cells, determine cell type and add corresponding line segments to list
		 */
		for(int i=0; i<height-1; i++){
			for(int j=0; j<width-1; j++){
				for(int t=0; t<2;t++){
					int celltype;
					double tx0,ty0,tx1,ty1,tx2,ty2;
					double v0, v1, v2;
					if(t == 0){
						// lt, rt, lb
						tx0=j+0; ty0=i+0;
						tx1=j+1; ty1=i+0;
						tx2=j+0; ty2=i+1;
						celltype = celltype(
								greaterThanIso1[i][j], 
								greaterThanIso1[i][j+1], 
								greaterThanIso1[i+1][j],
								greaterThanIso2[i][j], 
								greaterThanIso2[i][j+1], 
								greaterThanIso2[i+1][j]);
						v0 = f[i][j];
						v1 = f[i][j+1];
						v2 = f[i+1][j];
					} else {
						// rb, lb, rt
						tx0=j+1; ty0=i+1;
						tx1=j+0; ty1=i+1;
						tx2=j+1; ty2=i+0;
						celltype = celltype(
								greaterThanIso1[i+1][j+1], 
								greaterThanIso1[i+1][j], 
								greaterThanIso1[i][j+1],
								greaterThanIso2[i+1][j+1], 
								greaterThanIso2[i+1][j], 
								greaterThanIso2[i][j+1]);
						v0 = f[i+1][j+1];
						v1 = f[i+1][j];
						v2 = f[i][j+1];
					}
					switch (celltype) {
					// non intersecting celltypes
					case 0x000: // fall through
					case 0x222: // no intersection of isoline in this cell
						break;
					case 0x111:{
						tris.add(new TriangleDetails(
								tx0, ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)), 
								tx1, ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)), 
								tx2, ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								0)
						);
						break;
					}
					// one corner cases
					case 0x100:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v1, v0, isoValue1);
						m1 = 1-interpolateToValue(v2, v0, isoValue1);
						x0 = tx0+m0*(tx1-tx0);
						y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx2-tx0);
						y1 = ty0+m1*(ty2-ty0);
						tris.add(new TriangleDetails(
								tx0, ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)), 
								x0, y0, c1, 
								x1, y1, c1,0));
						break;
					}
					case 0x122:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v0, v1, isoValue2);
						m1 = interpolateToValue(v0, v2, isoValue2);
						x0 = tx0+m0*(tx1-tx0);
						y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx2-tx0);
						y1 = ty0+m1*(ty2-ty0);
						tris.add(new TriangleDetails(
								tx0, ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)), 
								x0, y0, c2, 
								x1, y1, c2,0));
						break;
					}
					case 0x010:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v0, v1, isoValue1);
						m1 = 1-interpolateToValue(v2, v1, isoValue1);
						x0 = tx1+m0*(tx0-tx1);
						y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx2-tx1);
						y1 = ty1+m1*(ty2-ty1);
						tris.add(new TriangleDetails(
								x0, y0, c1, 
								tx1, ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)), 
								x1, y1, c1,0));
						break;
					}
					case 0x212:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v1, v0, isoValue2);
						m1 = interpolateToValue(v1, v2, isoValue2);
						x0 = tx1+m0*(tx0-tx1);
						y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx2-tx1);
						y1 = ty1+m1*(ty2-ty1);
						tris.add(new TriangleDetails(
								x0, y0, c2, 
								tx1, ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)), 
								x1, y1, c2,0));
						break;
					}
					case 0x001:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v0, v2, isoValue1);
						m1 = 1-interpolateToValue(v1, v2, isoValue1);
						x0 = tx2+m0*(tx0-tx2);
						y0 = ty2+m0*(ty0-ty2);
						x1 = tx2+m1*(tx1-tx2);
						y1 = ty2+m1*(ty1-ty2);
						tris.add(new TriangleDetails(
								x0, y0, c1,  
								x1, y1, c1,
								tx2, ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),0));
						break;
					}
					case 0x221:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v2, v0, isoValue2);
						m1 = interpolateToValue(v2, v1, isoValue2);
						x0 = tx2+m0*(tx0-tx2);
						y0 = ty2+m0*(ty0-ty2);
						x1 = tx2+m1*(tx1-tx2);
						y1 = ty2+m1*(ty1-ty2);
						tris.add(new TriangleDetails(
								x0, y0, c2,  
								x1, y1, c2,
								tx2, ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),0));
						break;
					}
					
					
					// two corner cases
					case 0x011:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v0, v1, isoValue1);
						m1 = interpolateToValue(v0, v2, isoValue1);
						x0 = tx0+m0*(tx1-tx0);
						y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx2-tx0);
						y1 = ty0+m1*(ty2-ty0);
						tris.add(new TriangleDetails(
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)),
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c1
								,0));
						tris.add(new TriangleDetails(
								x1,y1, c1,
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c1
								,0));
						break;
					}
					case 0x211:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v1, v0, isoValue2);
						m1 = 1-interpolateToValue(v2, v0, isoValue2);
						x0 = tx0+m0*(tx1-tx0);
						y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx2-tx0);
						y1 = ty0+m1*(ty2-ty0);
						tris.add(new TriangleDetails(
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)),
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c2
								,0));
						tris.add(new TriangleDetails(
								x1,y1, c2,
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c2
								,0));
						break;
					}
					case 0x101:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v1, v0, isoValue1);
						m1 = interpolateToValue(v1, v2, isoValue1);
						x0 = tx1+m0*(tx0-tx1);
						y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx2-tx1);
						y1 = ty1+m1*(ty2-ty1);
						tris.add(new TriangleDetails(
								tx0,ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)),
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c1
								,0));
						tris.add(new TriangleDetails(
								x1,y1, c1,
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c1
								,0));
						break;
					}
					case 0x121:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v0, v1, isoValue2);
						m1 = 1-interpolateToValue(v2, v1, isoValue2);
						x0 = tx1+m0*(tx0-tx1);
						y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx2-tx1);
						y1 = ty1+m1*(ty2-ty1);
						tris.add(new TriangleDetails(
								tx0,ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)),
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c2
								,0));
						tris.add(new TriangleDetails(
								x1,y1, c2,
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)),
								x0,y0, c2
								,0));
						break;
					}
					case 0x110:{
						double x0,y0,x1,y1, m0, m1;
						m0 = interpolateToValue(v2, v0, isoValue1);
						m1 = interpolateToValue(v2, v1, isoValue1);
						x0 = tx2+m0*(tx0-tx2);
						y0 = ty2+m0*(ty0-ty2);
						x1 = tx2+m1*(tx1-tx2);
						y1 = ty2+m1*(ty1-ty2);
						tris.add(new TriangleDetails(
								tx0,ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)),
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)),
								x0,y0, c1
								,0));
						tris.add(new TriangleDetails(
								x1,y1, c1,
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)),
								x0,y0, c1
								,0));
						break;
					}
					case 0x112:{
						double x0,y0,x1,y1, m0, m1;
						m0 = 1-interpolateToValue(v0, v2, isoValue2);
						m1 = 1-interpolateToValue(v1, v2, isoValue2);
						x0 = tx2+m0*(tx0-tx2);
						y0 = ty2+m0*(ty0-ty2);
						x1 = tx2+m1*(tx1-tx2);
						y1 = ty2+m1*(ty1-ty2);
						tris.add(new TriangleDetails(
								tx0,ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)),
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)),
								x0,y0, c2
								,0));
						tris.add(new TriangleDetails(
								x1,y1, c2,
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)),
								x0,y0, c2
								,0));
						break;
					}
					// entirely in between vertices
					case 0x200:{
						double x0,y0,x1,y1,x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v1, v0, isoValue1);
						m1 = interpolateToValue(v1, v0, isoValue2);
						m2 = interpolateToValue(v2, v0, isoValue1);
						m3 = interpolateToValue(v2, v0, isoValue2);
						x0 = tx1+m0*(tx0-tx1); y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx0-tx1); y1 = ty1+m1*(ty0-ty1);
						x2 = tx2+m2*(tx0-tx2); y2 = ty2+m2*(ty0-ty2);
						x3 = tx2+m3*(tx0-tx2); y3 = ty2+m3*(ty0-ty2);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						break;
					}
					case 0x020:{
						double x0,y0,x1,y1,x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v0, v1, isoValue1);
						m1 = interpolateToValue(v0, v1, isoValue2);
						m2 = interpolateToValue(v2, v1, isoValue1);
						m3 = interpolateToValue(v2, v1, isoValue2);
						x0 = tx0+m0*(tx1-tx0); y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx1-tx0); y1 = ty0+m1*(ty1-ty0);
						x2 = tx2+m2*(tx1-tx2); y2 = ty2+m2*(ty1-ty2);
						x3 = tx2+m3*(tx1-tx2); y3 = ty2+m3*(ty1-ty2);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						break;
					}
					case 0x002:{
						double x0,y0,x1,y1,x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v0, v2, isoValue1);
						m1 = interpolateToValue(v0, v2, isoValue2);
						m2 = interpolateToValue(v1, v2, isoValue1);
						m3 = interpolateToValue(v1, v2, isoValue2);
						x0 = tx0+m0*(tx2-tx0); y0 = ty0+m0*(ty2-ty0);
						x1 = tx0+m1*(tx2-tx0); y1 = ty0+m1*(ty2-ty0);
						x2 = tx1+m2*(tx2-tx1); y2 = ty1+m2*(ty2-ty1);
						x3 = tx1+m3*(tx2-tx1); y3 = ty1+m3*(ty2-ty1);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						break;
					}
					case 0x220:{
						double x0,y0,x1,y1,x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v2, v0, isoValue1);
						m1 = interpolateToValue(v2, v0, isoValue2);
						m2 = interpolateToValue(v2, v1, isoValue1);
						m3 = interpolateToValue(v2, v1, isoValue2);
						x0 = tx2-m0*(tx2-tx0); y0 = ty2-m0*(ty2-ty0);
						x1 = tx2-m1*(tx2-tx0); y1 = ty2-m1*(ty2-ty0);
						x2 = tx2-m2*(tx2-tx1); y2 = ty2-m2*(ty2-ty1);
						x3 = tx2-m3*(tx2-tx1); y3 = ty2-m3*(ty2-ty1);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						break;
					}
					case 0x202:{
						double x0,y0,x1,y1,x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v1, v0, isoValue1);
						m1 = interpolateToValue(v1, v0, isoValue2);
						m2 = interpolateToValue(v1, v2, isoValue1);
						m3 = interpolateToValue(v1, v2, isoValue2);
						x0 = tx1-m0*(tx1-tx0); y0 = ty1-m0*(ty1-ty0);
						x1 = tx1-m1*(tx1-tx0); y1 = ty1-m1*(ty1-ty0);
						x2 = tx1-m2*(tx1-tx2); y2 = ty1-m2*(ty1-ty2);
						x3 = tx1-m3*(tx1-tx2); y3 = ty1-m3*(ty1-ty2);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						break;
					}
					case 0x022:{
						double x0,y0,x1,y1,x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v0, v1, isoValue1);
						m1 = interpolateToValue(v0, v1, isoValue2);
						m2 = interpolateToValue(v0, v2, isoValue1);
						m3 = interpolateToValue(v0, v2, isoValue2);
						x0 = tx0-m0*(tx0-tx1); y0 = ty0-m0*(ty0-ty1);
						x1 = tx0-m1*(tx0-tx1); y1 = ty0-m1*(ty0-ty1);
						x2 = tx0-m2*(tx0-tx2); y2 = ty0-m2*(ty0-ty2);
						x3 = tx0-m3*(tx0-tx2); y3 = ty0-m3*(ty0-ty2);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						break;
					}
					// mixed cases (pentagons)
					case 0x012:{
						double x0,y0,x1,y1, x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v0, v2, isoValue1);
						m1 = interpolateToValue(v0, v2, isoValue2);
						m2 = interpolateToValue(v0, v1, isoValue1);
						m3 = interpolateToValue(v1, v2, isoValue2);
						x0 = tx0+m0*(tx2-tx0); y0 = ty0+m0*(ty2-ty0);
						x1 = tx0+m1*(tx2-tx0); y1 = ty0+m1*(ty2-ty0);
						x2 = tx0+m2*(tx1-tx0); y2 = ty0+m2*(ty1-ty0);
						x3 = tx1+m3*(tx2-tx1); y3 = ty1+m3*(ty2-ty1);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(
								x3, y3, c2, 
								x2, y2, c1, 
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)), 
								0));
						break;
					}
					case 0x102:{
						double x0,y0,x1,y1, x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v1, v2, isoValue1);
						m1 = interpolateToValue(v1, v2, isoValue2);
						m2 = interpolateToValue(v1, v0, isoValue1);
						m3 = interpolateToValue(v0, v2, isoValue2);
						x0 = tx1+m0*(tx2-tx1); y0 = ty1+m0*(ty2-ty1);
						x1 = tx1+m1*(tx2-tx1); y1 = ty1+m1*(ty2-ty1);
						x2 = tx1+m2*(tx0-tx1); y2 = ty1+m2*(ty0-ty1);
						x3 = tx0+m3*(tx2-tx0); y3 = ty0+m3*(ty2-ty0);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(
								x3, y3, c2, 
								x2, y2, c1, 
								tx0,ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)), 
								0));
						break;
					}
					case 0x120:{
						double x0,y0,x1,y1, x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v2, v1, isoValue1);
						m1 = interpolateToValue(v2, v1, isoValue2);
						m2 = interpolateToValue(v2, v0, isoValue1);
						m3 = interpolateToValue(v0, v1, isoValue2);
						x0 = tx2+m0*(tx1-tx2); y0 = ty2+m0*(ty1-ty2);
						x1 = tx2+m1*(tx1-tx2); y1 = ty2+m1*(ty1-ty2);
						x2 = tx2+m2*(tx0-tx2); y2 = ty2+m2*(ty0-ty2);
						x3 = tx0+m3*(tx1-tx0); y3 = ty0+m3*(ty1-ty0);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(
								x3, y3, c2, 
								x2, y2, c1, 
								tx0,ty0, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v0)), 
								0));
						break;
					}
					case 0x210:{
						double x0,y0,x1,y1, x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v2, v0, isoValue1);
						m1 = interpolateToValue(v2, v0, isoValue2);
						m2 = interpolateToValue(v2, v1, isoValue1);
						m3 = interpolateToValue(v1, v0, isoValue2);
						x0 = tx2+m0*(tx0-tx2); y0 = ty2+m0*(ty0-ty2);
						x1 = tx2+m1*(tx0-tx2); y1 = ty2+m1*(ty0-ty2);
						x2 = tx2+m2*(tx1-tx2); y2 = ty2+m2*(ty1-ty2);
						x3 = tx1+m3*(tx0-tx1); y3 = ty1+m3*(ty0-ty1);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(
								x3, y3, c2, 
								x2, y2, c1, 
								tx1,ty1, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v1)), 
								0));
						break;
					}
					case 0x201:{
						double x0,y0,x1,y1, x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v1, v0, isoValue1);
						m1 = interpolateToValue(v1, v0, isoValue2);
						m2 = interpolateToValue(v1, v2, isoValue1);
						m3 = interpolateToValue(v2, v0, isoValue2);
						x0 = tx1+m0*(tx0-tx1); y0 = ty1+m0*(ty0-ty1);
						x1 = tx1+m1*(tx0-tx1); y1 = ty1+m1*(ty0-ty1);
						x2 = tx1+m2*(tx2-tx1); y2 = ty1+m2*(ty2-ty1);
						x3 = tx2+m3*(tx0-tx2); y3 = ty2+m3*(ty0-ty2);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(
								x3, y3, c2, 
								x2, y2, c1, 
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)), 
								0));
						break;
					}
					case 0x021:{
						double x0,y0,x1,y1, x2,y2,x3,y3, m0,m1,m2,m3;
						m0 = interpolateToValue(v0, v1, isoValue1);
						m1 = interpolateToValue(v0, v1, isoValue2);
						m2 = interpolateToValue(v0, v2, isoValue1);
						m3 = interpolateToValue(v2, v1, isoValue2);
						x0 = tx0+m0*(tx1-tx0); y0 = ty0+m0*(ty1-ty0);
						x1 = tx0+m1*(tx1-tx0); y1 = ty0+m1*(ty1-ty0);
						x2 = tx0+m2*(tx2-tx0); y2 = ty0+m2*(ty2-ty0);
						x3 = tx2+m3*(tx1-tx2); y3 = ty2+m3*(ty1-ty2);
						tris.add(new TriangleDetails(x0, y0, c1, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(x3, y3, c2, x1, y1, c2, x2, y2, c1, 0));
						tris.add(new TriangleDetails(
								x3, y3, c2, 
								x2, y2, c1, 
								tx2,ty2, interpolateColor(c1, c2, interpolateToValue(isoValue1, isoValue2, v2)), 
								0));
						break;
					}
					
					// missing pentagon cases: ,021
					default:
//						throw new RuntimeException(Integer.toHexString(celltype));
//						break;
					}
				}
			}
		}
		return tris;
	}

	static int celltype(boolean v1, boolean v2, boolean v3){
		int type = 0;
		type = (type<<1) | (v1 ? 1:0);
		type = (type<<1) | (v2 ? 1:0);
		type = (type<<1) | (v3 ? 1:0);
		return type;
	}
	
	static int celltype(
			boolean v11, boolean v21, boolean v31,
			boolean v12, boolean v22, boolean v32
	){
		int type = 0;
		type = (type<<4) | (v11  ? (v12 ? 2:1):0);
		type = (type<<4) | (v21  ? (v22 ? 2:1):0);
		type = (type<<4) | (v31  ? (v32 ? 2:1):0);
		return type;
	}

	/**
	 * Returns m of the equation: iso = lower*(1-m) + upper*m <br>
	 * which is: m = (iso-lower)/(upper-lower)
	 * @param lower value
	 * @param upper value
	 * @param iso value in between lower and upper
	 * @return m
	 */
	static double interpolateToValue(double lower, double upper, double iso){
		return (iso-lower)/(upper-lower);
	}

}
