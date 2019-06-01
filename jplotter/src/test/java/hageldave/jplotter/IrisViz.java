package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import hageldave.jplotter.renderables.DefaultGlyph;
import hageldave.jplotter.renderables.Glyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;

public class IrisViz {

	public static void main(String[] args) throws IOException {
		// setup content
		ArrayList<double[]> dataset = new ArrayList<>();
		//URL irissrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data");
		try (	InputStream stream = IrisViz.class.getResourceAsStream("/iris.data");
				Scanner  sc = new Scanner(stream))
		{
			while(sc.hasNextLine()){
				String nextLine = sc.nextLine();
				if(nextLine.isEmpty()){
					continue;
				}
				String[] fields = nextLine.split(",");
				double[] values = new double[5];
				values[0] = Double.parseDouble(fields[0]);
				values[1] = Double.parseDouble(fields[1]);
				values[2] = Double.parseDouble(fields[2]);
				values[3] = Double.parseDouble(fields[3]);
				if(fields[4].contains("setosa")){
					values[4] = 0; // setosa class
				} else if(fields[4].contains("versicolor")) {
					values[4] = 1; // versicolor class
				} else {
					values[4] = 2; // virginica class
				}
				dataset.add(values);
			}
		}
		
		// done reading data, lets make the viz
		JFrame frame = new JFrame("Iris Dataset");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		JPanel gridPane = new JPanel(new GridLayout(4, 4));
		gridPane.setBackground(Color.WHITE);
		frame.getContentPane().add(gridPane, BorderLayout.CENTER);
		JPanel header = new JPanel();
		header.setBackground(Color.WHITE);
		header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
		frame.getContentPane().add(header, BorderLayout.NORTH);
		
		LinkedList<FBOCanvas> canvasCollection = new LinkedList<>();
		String[] dimNames = new String[]{"sepal length","sepal width","petal length","petal width"};
		String[] perClassNames = new String[]{"Setosa", "Versicolor", "Virginica"};
		int[] perClassColors = new int[]{0xff66c2a5,0xfffc8d62,0xff8da0cb};
		Glyph[] perClassGlyphs = new Glyph[]{DefaultGlyph.CIRCLE_F, DefaultGlyph.SQUARE_F, DefaultGlyph.TRIANGLE_F};
		
		// add legend on top
		BlankCanvas legendCanvas = new BlankCanvas();
		canvasCollection.add(legendCanvas);
		legendCanvas.setPreferredSize(new Dimension(300, 16));
		Legend legend = new Legend();
		for(int c=0; c<3; c++){
			legend.addGlyphLabel(perClassGlyphs[c], new Color(perClassColors[c]), perClassNames[c]);
		}
		legendCanvas.setRenderer(legend);
		header.add(Box.createHorizontalStrut(30));
		header.add(legendCanvas);
		
		// make scatter plot matrix
		for(int j = 0; j < 4; j++){
			for(int i = 0; i < 4; i++){
				CoordSysCanvas canvas = new CoordSysCanvas();
				canvas.setPreferredSize(new Dimension(250, 250));
				gridPane.add(canvas);
				canvas.setxAxisLabel(j==0 ? dimNames[i] : "");
				canvas.setyAxisLabel(i==3 ? dimNames[j] : "");
				CompleteRenderer content = new CompleteRenderer();
				canvas.setContent(content);

				double maxX,minX,maxY,minY;
				maxX = maxY = Double.NEGATIVE_INFINITY;
				minX = minY = Double.POSITIVE_INFINITY;
				if(i==j){
					// make histo when same dimension on x and y axis
					int numBuckets = 20;
					double[][] histo = mkHistogram(dataset, i, numBuckets);
					Lines lines = new Lines();
					lines.setThickness(1.5f);
					lines.addLineStrip(perClassColors[0], histo[3], histo[0]);
					lines.addLineStrip(perClassColors[1], histo[3], histo[1]);
					lines.addLineStrip(perClassColors[2], histo[3], histo[2]);
					content.addItemToRender(lines);
					Triangles tris = new Triangles();
					for(int k = 0; k < numBuckets-1; k++){
						for(int c = 0; c < 3; c++){
							tris.addQuad(histo[3][k], 0, histo[3][k], histo[c][k], histo[3][k+1], histo[c][k+1], histo[3][k+1], 0, new Color(perClassColors[c]));
						}
					}
					tris.setGlobalAlphaMultiplier(0.1f);
					content.addItemToRender(tris);
					minX = Arrays.stream(histo[3]).min().getAsDouble();
					maxX = Arrays.stream(histo[3]).max().getAsDouble();
					maxY = Math.max(maxY, Arrays.stream(histo[0]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[1]).max().getAsDouble());
					maxY = Math.max(maxY, Arrays.stream(histo[2]).max().getAsDouble());
					minY = 0;
				} else {
					// make scatter
					Points[] perClassPoints = new Points[]{
							new Points(perClassGlyphs[0]),
							new Points(perClassGlyphs[1]),
							new Points(perClassGlyphs[2])
					};
					content
					.addItemToRender(perClassPoints[0].setGlobalAlphaMultiplier(0.6f))
					.addItemToRender(perClassPoints[1].setGlobalAlphaMultiplier(0.6f))
					.addItemToRender(perClassPoints[2].setGlobalAlphaMultiplier(0.6f));
					for(int k = 0; k < dataset.size(); k++){
						double[] instance = dataset.get(k);
						int clazz = (int)instance[4];
						double x =instance[i];
						double y = instance[j];
						perClassPoints[clazz].addPoint(
								x,
								y,
								0, 
								1, 
								perClassColors[clazz], 
								k+1
								);
						maxX = Math.max(maxX, x);
						maxY = Math.max(maxY, y);
						minX = Math.min(minX, x);
						minY = Math.min(minY, y);
					}
				}
				canvas.setCoordinateView(minX, minY, maxX, maxY);
			}
		}
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvasCollection.forEach(c->c.runInContext(()->c.close()));
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
		double[][] counts = new double[3][numbuckets];
		for(int i = 0; i < dataset.size(); i++){
			double[] instance = dataset.get(i);
				double v = instance[dim];
				int bucket = (int)((v-min)/range*numbuckets);
				counts[(int)instance[4]][bucket<numbuckets ? bucket : numbuckets-1]++;
		}
		return new double[][]{counts[0],counts[1],counts[2],bucketVals};
	}

}
