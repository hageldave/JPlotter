package hageldave.jplotter;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import hageldave.jplotter.renderables.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;

public class IrisViz {

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel gridPane = new JPanel(new GridLayout(4, 4));
		frame.setContentPane(gridPane);

		// setup content
		ArrayList<double[]> dataset = new ArrayList<>();
		//URL irissrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data");
		try (	InputStream stream = IrisViz.class.getResourceAsStream("/iris.data");
				Scanner  sc = new Scanner(stream);
				)
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
		// done reading, lets make scatter plot matrix 
		LinkedList<CoordSysCanvas> canvasCollection = new LinkedList<>();
		String[] dimNames = new String[]{"sepal length","sepal width","petal length","petal width"};
		for(int j = 0; j < 4; j++){
			for(int i = 0; i < 4; i++){
				CoordSysCanvas canvas = new CoordSysCanvas();
				canvas.setPreferredSize(new Dimension(250, 250));
				gridPane.add(canvas);
				canvas.setxAxisLabel(j==0 ? dimNames[i] : "");
				canvas.setyAxisLabel(i==3 ? dimNames[j] : "");
				CompleteRenderer content = new CompleteRenderer();
				canvas.setContent(content);
				Points[] perClassPoints = new Points[]{
						new Points(DefaultGlyph.CROSS),
						new Points(DefaultGlyph.SQUARE),
						new Points(DefaultGlyph.TRIANGLE)
				};
				content
				.addItemToRender(perClassPoints[0])
				.addItemToRender(perClassPoints[1])
				.addItemToRender(perClassPoints[2]);

				int[] perClassColors = new int[]{0xff007700,0xff990000,0xff0000bb};
				double maxX,minX,maxY,minY;
				maxX = maxY = Double.NEGATIVE_INFINITY;
				minX = minY = Double.POSITIVE_INFINITY;
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

}
