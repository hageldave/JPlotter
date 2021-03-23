package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.font.FontProvider;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.svg.SVGUtils;

public class StatLogSPLOMViz {

	static JPlotterCanvas mkCanvas(boolean fallback, JPlotterCanvas contextShareParent) {
		return fallback ? new BlankCanvasFallback() : new BlankCanvas((FBOCanvas)contextShareParent);
	}
	
	static boolean useFallback(String[] args) {
		return Arrays.stream(args).filter(arg->"jplotter_fallback=true".equals(arg)).findAny().isPresent()||true;
	}

	static boolean fallbackModeEnabled;
	
	public static void main(String[] args) throws IOException {
		fallbackModeEnabled = useFallback(args);
		// setup content
		ArrayList<double[]> dataset = new ArrayList<>();
		URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/shuttle/shuttle.tst");
		try (	InputStream stream = statlogsrc.openStream();
				Scanner  sc = new Scanner(stream))
		{
			while(sc.hasNextLine()){
				String nextLine = sc.nextLine();
				if(nextLine.isEmpty()){
					continue;
				}
				String[] fields = nextLine.split(" ");
				double[] values = new double[10];
				int i = 0;
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++]);
				values[i] = Integer.parseInt(fields[i++])-1;
				dataset.add(values);
			}
		}
		
		// done reading data, lets make the viz
		JFrame frame = new JFrame("Iris Dataset");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		JPanel gridPane = new JPanel(new GridLayout(9, 9));
		gridPane.setBackground(Color.WHITE);
		frame.getContentPane().add(gridPane, BorderLayout.CENTER);
		JPanel header = new JPanel();
		header.setBackground(Color.WHITE);
		header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
		frame.getContentPane().add(header, BorderLayout.NORTH);
		
		LinkedList<JPlotterCanvas> canvasCollection = new LinkedList<>();
		String[] dimNames = IntStream.of(1,2,3,4,5,6,7,8,9).mapToObj(i->"dim " + i).toArray(String[]::new);
		String[] perClassNames = new String[]{
				 "Rad Flow",
				 "Fpv Close",
				 "Fpv Open",
				 "High",
				 "Bypass",
				 "Bpv Close",
				 "Bpv Open",
		};
		int[] perClassColors = new int[]{
				0xff1b9e77,
				0xffd95f02,
				0xff7570b3,
				0xffe7298a,
				0xff66a61e,
				0xffe6ab02,
				0xffa6761d
		};
		Glyph[] perClassGlyphs = new Glyph[]{
				DefaultGlyph.CROSS,
				DefaultGlyph.CIRCLE,
				DefaultGlyph.CIRCLE_F,
				DefaultGlyph.SQUARE,
				DefaultGlyph.SQUARE_F,
				DefaultGlyph.TRIANGLE,
				DefaultGlyph.TRIANGLE_F
		};
		
		// add legend on top
		JPlotterCanvas legendCanvas = mkCanvas(fallbackModeEnabled, null);
		canvasCollection.add(legendCanvas);
		legendCanvas.asComponent().setPreferredSize(new Dimension(400, 16));
		Legend legend = new Legend();
		for(int c=0; c<7; c++){
			legend.addGlyphLabel(perClassGlyphs[c], perClassColors[c], perClassNames[c]);
		}
		legendCanvas.setRenderer(legend);
		header.add(Box.createHorizontalStrut(30));
		header.add(legendCanvas.asComponent());
		JLabel pointInfo = new JLabel("");
		pointInfo.setFont(FontProvider.getUbuntuMono(10, Font.PLAIN));
		pointInfo.setPreferredSize(new Dimension(300, pointInfo.getPreferredSize().height));
		header.add(pointInfo);
		
		ArrayList<Points[]> allPoints = new ArrayList<>();
		ArrayList<Triangles[]> allTris = new ArrayList<>();
		ArrayList<Lines[]> allLines = new ArrayList<>();
 		
		// make scatter plot matrix
		for(int j = 0; j < 9; j++){
			for(int i = 0; i < 9; i++){
				CoordSysRenderer coordsys = new CoordSysRenderer(){
					{
						paddingLeft=paddingRight=paddingTop=paddingBot=2;
					}
				};
				JPlotterCanvas canvas = mkCanvas(fallbackModeEnabled, legendCanvas);
				canvas.setRenderer(coordsys);
				canvasCollection.add(canvas);
				canvas.asComponent().setPreferredSize(new Dimension(250, 250));
				gridPane.add(canvas.asComponent());
				coordsys.setxAxisLabel(j==0 ? dimNames[i] : "");
				coordsys.setyAxisLabel(i==8 ? dimNames[j] : "");
				CompleteRenderer content = new CompleteRenderer();

				double maxX,minX,maxY,minY;
				maxX = maxY = Double.NEGATIVE_INFINITY;
				minX = minY = Double.POSITIVE_INFINITY;
				if(i==j){
					// make histo when same dimension on x and y axis
					int numBuckets = 20;
					double[][] histo = mkHistogram(dataset, i, numBuckets);
					Lines[] lines = IntStream.range(0, 7).mapToObj(n->new Lines()).toArray(Lines[]::new);
					allLines.add(lines);
					for(int c = 0; c < 7; c++){
						int color = perClassColors[c];
						lines[c].setGlobalThicknessMultiplier(1.5f).addLineStrip(histo[7], histo[c])
							.forEach(seg->seg.setColor(color));
						content.addItemToRender(lines[c]);
					}
					Triangles[] perClassTris = IntStream.range(0, 7).mapToObj(n->new Triangles()).toArray(Triangles[]::new);
					allTris.add(perClassTris);
					for(int k = 0; k < numBuckets-1; k++){
						for(int c = 0; c < 7; c++){
							int color = perClassColors[c];
							perClassTris[c].addQuad(histo[7][k], 0, histo[7][k], histo[c][k], histo[7][k+1], histo[c][k+1], histo[7][k+1], 0)
								.forEach(t->t.setColor(color));
						}
					}
					for(int c = 0; c < 7; c++){
						content.addItemToRender(perClassTris[c].setGlobalAlphaMultiplier(0.1f));
					}
					minX = Arrays.stream(histo[7]).min().getAsDouble();
					maxX = Arrays.stream(histo[7]).max().getAsDouble();
					maxY = Math.max(maxY, Arrays.stream(histo[0]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[1]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[2]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[3]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[4]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[5]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[6]).max().getAsDouble());
					minY = 0;
				} else {
					// make scatter
					Points[] perClassPoints = new Points[]{
							new Points(perClassGlyphs[0]),
							new Points(perClassGlyphs[1]),
							new Points(perClassGlyphs[2]),
							new Points(perClassGlyphs[3]),
							new Points(perClassGlyphs[4]),
							new Points(perClassGlyphs[5]),
							new Points(perClassGlyphs[6]),

					};
					allPoints.add(perClassPoints);
					for(int c = 0; c < 7; c++){
						content.addItemToRender(perClassPoints[c].setGlobalAlphaMultiplier(0.4f));
					}
					for(int k = 0; k < dataset.size(); k++){
						double[] instance = dataset.get(k);
						int clazz = (int)instance[9];
						double x =instance[i];
						double y = instance[j];
						perClassPoints[clazz].addPoint(x,y)
								.setColor(perClassColors[clazz])
								.setPickColor(k+1);
						maxX = Math.max(maxX, x);
						maxY = Math.max(maxY, y);
						minX = Math.min(minX, x);
						minY = Math.min(minY, y);
					}
					// remove duplicates
					for(int c=0; c<7; c++){
						ArrayList<PointDetails> pointDetails = perClassPoints[c].getPointDetails();
						Comparator<PointDetails> comparator = (p1,p2)->{
							int comp = Double.compare(p1.location.getX(), p2.location.getX());
							if(comp != 0)
								return comp;
							return Double.compare(p1.location.getY(), p2.location.getY());
						};
						pointDetails.sort(comparator);
						for(int k=1; k<pointDetails.size(); k++){
							if(comparator.compare(pointDetails.get(k-1), pointDetails.get(k)) == 0){
								pointDetails.remove(k--);
							}
						}
						perClassPoints[c].setDirty();
					}
					
					// hovering over point
					canvas.asComponent().addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							if(SwingUtilities.isRightMouseButton(e)){
								return;
							}
							Point2D location = coordsys.transformAWT2CoordSys(e.getPoint(), canvas.asComponent().getHeight());
							if(!coordsys.getCoordinateView().contains(location)){
								pointInfo.setText("");
								recolorAll();
								return;
							}
							int pixel = canvas.getPixel(e.getX(), e.getY(), true, 1);
							if((pixel&0x00ffffff)==0){
								pixel = canvas.getPixel(e.getX(), e.getY(), true, 3);
							}
							if((pixel&0x00ffffff)==0){
								pointInfo.setText("");
								recolorAll();
								return;
							}
							int dataSetinstanceIDX = (pixel & 0x00ffffff)-1;
							double[] instance = dataset.get(dataSetinstanceIDX);
							pointInfo.setForeground(new Color(perClassColors[(int)instance[9]]));
							pointInfo.setText(""
									+ perClassNames[(int)instance[9]] 
									+ Arrays.toString(Arrays.stream(instance, 0, 9).mapToInt(d->(int)d).toArray())
							);
							desaturateExcept(pixel|0xff000000);
						}
						
						void recolorAll() {
							for(Points[] points:allPoints){
								for(int c=0; c<7; c++){
									int color = perClassColors[c];
									points[c].getPointDetails().forEach(p->p
										.setColor(color)
										.setScaling(1));
									points[c].setGlobalAlphaMultiplier(0.4).setDirty();
								}
							}
							for(Triangles[] tris:allTris){
								for(int c=0; c<7; c++){
									int color = perClassColors[c];
									tris[c].getTriangleDetails().forEach(t->t.setColor(color));
									tris[c].setDirty();
								}
							}
							for(Lines[] lines:allLines){
								for(int c=0; c<7; c++){
									int color = perClassColors[c];
									lines[c].getSegments().forEach(s->s.setColor(color));
									lines[c].setDirty();
								}
							}
							canvasCollection.forEach(cnvs->cnvs.scheduleRepaint());
						}
						
						void desaturateExcept(int pick){
							int clazz = (int)dataset.get((pick&0x00ffffff)-1)[9];
							for(Points[] points:allPoints){
								for(int c=0; c<7; c++){
									int color = perClassColors[c];
									int desat = 0x33aaaaaa;
									points[c].getPointDetails().forEach(p->p
										.setColor( p.pickColor==pick ? color:desat)
										.setScaling(p.pickColor==pick ? 1.2f:1)
									);
									// bring picked point to front by sorting
									points[c].getPointDetails().sort((p1,p2)->{
										if(p1.pickColor==p2.pickColor) return 0;
										return p1.pickColor==pick ? 1:-1;
									});
									points[c].setGlobalAlphaMultiplier(1).setDirty();
								}
							}
							for(Triangles[] tris:allTris){
								for(int c=0; c<7; c++){
									int color = c==clazz ? perClassColors[c]:0xff777777;
									tris[c].setDirty().getTriangleDetails().forEach(t->t.setColor(color));
								}
							}
							for(Lines[] lines:allLines){
								for(int c=0; c<7; c++){
									int color = c==clazz ? perClassColors[c]:0xff777777;
									lines[c].setDirty().getSegments().forEach(s->s.setColor(color));
								}
							}
							canvasCollection.forEach(cnvs->cnvs.scheduleRepaint());
						}
					});
					// selecting points (brush & link)
					new CoordSysViewSelector(canvas,coordsys) {
						{extModifierMask=0;/* no shift needed */}
						public void areaSelectedOnGoing(double minX, double minY, double maxX, double maxY) {
							pointInfo.setText("");
							desaturateExcept(minX, minY, maxX, maxY);
						}
						public void areaSelected(double minX, double minY, double maxX, double maxY) {
							pointInfo.setText("");
							desaturateExcept(minX, minY, maxX, maxY);
						}
						void desaturateExcept(double minX, double minY, double maxX, double maxY){
							Rectangle2D r = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
							Predicate<Point2D> isinselection = r::contains;
							TreeSet<Integer> pickIDs = Arrays.stream(perClassPoints)
									.flatMap(points->points.getPointDetails().stream())
									.filter(p->isinselection.test(p.location))
									.map(p->p.pickColor)
									.collect(Collectors.toCollection(TreeSet::new));
							Set<Integer> clazzes = pickIDs.stream()
									.map(id->(id&0x00ffffff)-1)
									.map(dataset::get)
									.map(inst->(int)inst[9])
									.collect(Collectors.toSet());
							for(Points[] points:allPoints){
								for(int c=0; c<7; c++){
									int color = perClassColors[c];
									int desat = 0x33aaaaaa;
									points[c].getPointDetails().forEach(p->p.setColor(pickIDs.contains(p.pickColor) ? color:desat));
									// bring picked point to front by sorting
									points[c].getPointDetails().sort((p1,p2)->{
										if(pickIDs.contains(p1.pickColor)==pickIDs.contains(p2.pickColor)) return 0;
										return pickIDs.contains(p1.pickColor) ? 1:-1;
									});
									points[c].setGlobalAlphaMultiplier(1).setDirty();
								}
							}
							for(Triangles[] tris:allTris){
								for(int c=0; c<7; c++){
									int color = clazzes.contains(c) ? perClassColors[c]:0xff777777;
									tris[c].setDirty().getTriangleDetails().forEach(t->t.setColor(color));
								}
							}
							for(Lines[] lines:allLines){
								for(int c=0; c<7; c++){
									int color = clazzes.contains(c) ? perClassColors[c]:0xff777777;
									lines[c].setDirty().getSegments().forEach(s->s.setColor(color));
								}
							}
							canvasCollection.forEach(cnvs->cnvs.scheduleRepaint());
						}
					}.register();
				}
				coordsys.setContent(content);
				coordsys.setCoordinateView(minX, minY, maxX, maxY);
			}
		}
		
		for(JPlotterCanvas cnvs:canvasCollection){
			// add a pop up menu (on right click) for exporting to SVG
			PopupMenu menu = new PopupMenu();
			MenuItem svgExport = new MenuItem("SVG export");
			menu.add(svgExport);
			svgExport.addActionListener(e->{
				Document svg = SVGUtils.containerToSVG(frame.getContentPane());
				SVGUtils.documentToXMLFile(svg, new File("iris_export.svg"));
				System.out.println("exported iris_export.svg");
			});
			cnvs.asComponent().add(menu);
			cnvs.asComponent().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(SwingUtilities.isRightMouseButton(e))
						menu.show(cnvs.asComponent(), e.getX(), e.getY());
				}
			});
		}
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvasCollection.forEach(c->{
					if(c instanceof FBOCanvas) {
						((FBOCanvas)c).runInContext(()->((FBOCanvas)c).close());
					}
				});
			}
		});
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}
	
	static double[][] mkHistogram(ArrayList<double[]> dataset, int dim, int numbuckets){
		double min = dataset.stream().mapToDouble(instance->instance[dim]).min().getAsDouble();
		double max = dataset.stream().mapToDouble(instance->instance[dim]).max().getAsDouble();
		double range = max-min;
		double[] bucketVals = new double[numbuckets];
		for(int i = 0; i < numbuckets; i++){
			bucketVals[i] = i*range/(numbuckets-1) + min;
		}
		double[][] counts = new double[7][numbuckets];
		for(int i = 0; i < dataset.size(); i++){
			double[] instance = dataset.get(i);
				double v = instance[dim];
				int bucket = (int)((v-min)/range*numbuckets);
				counts[(int)instance[9]][bucket<numbuckets ? bucket : numbuckets-1]++;
		}
		return new double[][]{counts[0],counts[1],counts[2],counts[3],counts[4],counts[5],counts[6],bucketVals};
	}

}
