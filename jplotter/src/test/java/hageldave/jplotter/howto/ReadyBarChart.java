package hageldave.jplotter.howto;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import hageldave.jplotter.IrisViz;
import hageldave.jplotter.charts.BarChart;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.svg.SVGUtils;

public class ReadyBarChart {
	public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
		BarChart meanChart = new BarChart(true, 2);
		BarChart histogramChart = new BarChart(true, 1);
		histogramChart.getCanvas().asComponent().setPreferredSize(new Dimension(900, 400));
		ColorMap classcolors = DefaultColorMap.Q_8_SET2;
		ColorMap featurecolors = DefaultColorMap.Q_12_PAIRED;

		// prepare dataset
		String[] plantLabels = new String[]{"Iris Setosa","Iris Versicolor","Iris Virginica"};
		String[] propertyLabels = new String[]{"sl","sw","pl","pw"};
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

		// set up groups
		BarGroup groupSetosa = new BarGroup(plantLabels[0]);
		BarGroup groupVersicolor = new BarGroup(plantLabels[1]);
		BarGroup groupVirginica = new BarGroup(plantLabels[2]);


		LinkedList<BarGroup> allGroups = new LinkedList<>();
		allGroups.add(groupSetosa);
		allGroups.add(groupVersicolor);
		allGroups.add(groupVirginica);

		// now calculate mean for all values and save it in an array
		double[][] means = new double[plantLabels.length][propertyLabels.length];
		for(int p=0; p<3; p++) {
			for(int f=0; f<4; f++) {
				double sum=0; int count=0;
				for(int i=0; i<dataset.size(); i++) {
					if((int)dataset.get(i)[4]==p) {
						sum += dataset.get(i)[f];
						count++;
					}
				}
				means[p][f] = sum/count;
			}
		}
		// put means as bars in group
		for(int p=0; p<3; p++) {
			for(int f=0; f<4; f++) {
				allGroups.get(p).addBarStack(f, means[p][f], new Color(featurecolors.getColor(f)), propertyLabels[f]);
			}
		}

		// add all groups to the chart
		for (BarGroup group : allGroups)
			meanChart.addData(group);

		// set up histogram
		BarGroup[] bargroups = {
				new BarGroup("sepal length"),
				new BarGroup("sepal width"),
				new BarGroup("petal length"),
				new BarGroup("petal width")
		};

		// for each feature (property: sepal width, petal length, ...) make a histrogram
		for(int f=0; f<4; f++) {
			int f_=f;
			// separate and sort values for each plant
			double[][] valuesPerPlant = new double[3][];
			valuesPerPlant[0] = dataset.stream().filter(row->(int)row[4]==0).mapToDouble(row->row[f_]).sorted().toArray();
			valuesPerPlant[1] = dataset.stream().filter(row->(int)row[4]==1).mapToDouble(row->row[f_]).sorted().toArray();
			valuesPerPlant[2] = dataset.stream().filter(row->(int)row[4]==2).mapToDouble(row->row[f_]).sorted().toArray();
			// make bins
			int nbins = 10;
			int[][] bins = new int[nbins][3];
			double min = Math.min(Math.min(valuesPerPlant[0][0], valuesPerPlant[1][0]), valuesPerPlant[2][0]);
			double max = Math.max(Math.max(valuesPerPlant[0][49], valuesPerPlant[1][49]), valuesPerPlant[2][49]);
			double binWidth = (max-min)/nbins;
			// fill bins
			for(int p=0; p<3; p++) {
				for(double v : valuesPerPlant[p]) {
					v = v-min;
					int idx = Math.min((int)(v/binWidth), nbins-1);
					bins[idx][p]++;
				}	
			}
			// populate the barchart group
			for(int b=0; b<nbins; b++) {
				for(int p=0; p<3; p++) {
					bargroups[f].addBarStack(
							b, 
							bins[b][p], 
							new Color(classcolors.getColor(p)), 
							String.format("%.1f", min+b*binWidth)
							);
				}
			}
		}

		histogramChart.addData(bargroups[0]);
		histogramChart.addData(bargroups[1]);
		histogramChart.addData(bargroups[2]);
		histogramChart.addData(bargroups[3]);

		meanChart.placeLegendBottom()
		.addBarLabel(featurecolors.getColor(3), "petal width", 3)
		.addBarLabel(featurecolors.getColor(2), "petal length", 2)
		.addBarLabel(featurecolors.getColor(1), "sepal width", 1)
		.addBarLabel(featurecolors.getColor(0), "sepal length", 0);

		histogramChart.placeLegendBottom()
		.addBarLabel(classcolors.getColor(0), plantLabels[0], 3)
		.addBarLabel(classcolors.getColor(1), plantLabels[1], 2)
		.addBarLabel(classcolors.getColor(2), plantLabels[2], 1);


		meanChart.getBarRenderer().setxAxisLabel("mean (in cm)");
		meanChart.getBarRenderer().setyAxisLabel("mean (in cm)");

		histogramChart.getBarRenderer().setxAxisLabel("number of entries");
		histogramChart.getBarRenderer().setyAxisLabel("number of entries");

		// set up gui stuff
		Container buttonWrapper = new Container();
		JButton meanView = new JButton("Mean View");
		JButton histogramView = new JButton("Histogram View");
		buttonWrapper.add(meanView);
		buttonWrapper.add(histogramView);
		buttonWrapper.setLayout(new FlowLayout());

		Container contentWrapper = new Container();
		contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
		contentWrapper.add(meanChart.getCanvas().asComponent());
		contentWrapper.add(buttonWrapper);

		JFrame frame = new JFrame();
		frame.getContentPane().add(contentWrapper);
		frame.setTitle("Comparison chart of iris plants");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		meanChart.getCanvas().addCleanupOnWindowClosingListener(frame);
		// make visible on AWT event dispatch thread
		SwingUtilities.invokeAndWait(()->{
			frame.pack();
			frame.setVisible(true);
		});

		// set maximum size of the button wrapper, to force canvas to scale matching the resized window
		buttonWrapper.setMaximumSize(new Dimension(buttonWrapper.getWidth(), buttonWrapper.getHeight()));

		// add eventlisteners to buttons
		meanView.addActionListener(e -> {
			contentWrapper.removeAll();
			contentWrapper.add(meanChart.getCanvas().asComponent());
			contentWrapper.add(buttonWrapper);
			meanChart.alignBarRenderer();
			meanChart.getBarRenderer().setDirty();
			meanChart.getCanvas().scheduleRepaint();
			frame.repaint();
			frame.pack();
		});
		histogramView.addActionListener(e -> {
			contentWrapper.removeAll();
			contentWrapper.add(histogramChart.getCanvas().asComponent());
			contentWrapper.add(buttonWrapper);
			histogramChart.alignBarRenderer();
			histogramChart.getBarRenderer().setDirty();
			histogramChart.getCanvas().scheduleRepaint();
			frame.repaint();
			frame.pack();
		});

		// set up interaction stuff
		histogramChart.addBarChartMouseEventListener(new BarChart.BarChartMouseEventListener() {
			BarGroup.BarStruct selectedBarStruct;
			final JPopupMenu popUp = new JPopupMenu("Hovered Plant");
			@Override
			public void onInsideMouseEventNone(String mouseEventType, MouseEvent e, Point2D coordsysPoint) {
				selectedBarStruct = null;
				popUp.setVisible(false);
			}
			@Override
			public void onInsideMouseEventStruct(String mouseEventType, MouseEvent e, Point2D coordsysPoint, BarGroup.BarStruct barStruct) {
				if (barStruct != selectedBarStruct) {
					selectedBarStruct = barStruct;
					popUp.setFocusable(false);
					popUp.setVisible(false);
					popUp.removeAll();
					int plant = IntStream.of(classcolors.getColors()).boxed().collect(Collectors.toList())
							.indexOf(barStruct.stackColor.getRGB());
					JLabel label = new JLabel("Plant: " + plantLabels[plant] + ", Frequency in interval: " + barStruct.length);
					label.setBorder(new EmptyBorder(3, 12, 3, 12));
					popUp.add(label);
					popUp.show(histogramChart.getCanvas().asComponent(), 50, 20);
					popUp.setVisible(true);
				}
			}
			@Override
			public void onOutsideMouseEventNone(String mouseEventType, MouseEvent e) {}
			@Override
			public void onOutsideMouseEventElement(String mouseEventType, MouseEvent e, Legend.BarLabel legendElement) {}
		});

		meanChart.alignBarRenderer();
		meanChart.getBarRenderer().setDirty();
		meanChart.getCanvas().scheduleRepaint();

		// add a pop up menu (on right click) for exporting to SVG
		PopupMenu menu = new PopupMenu();
		meanChart.getCanvas().asComponent().add(menu);
		MenuItem svgExport = new MenuItem("SVG export");
		menu.add(svgExport);
		svgExport.addActionListener(e->{
			Document doc2 = meanChart.getCanvas().paintSVG();
			SVGUtils.documentToXMLFile(doc2, new File("barchart_demo.svg"));
			System.out.println("exported barchart_demo.svg");
		});
		MenuItem pdfExport = new MenuItem("PDF export");
		menu.add(pdfExport);
		pdfExport.addActionListener(e->{
			try {
				PDDocument doc = meanChart.getCanvas().paintPDF();
				doc.save("barchart_demo.pdf");
				doc.close();
				System.out.println("exported barchart_demo.pdf");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

		meanChart.getCanvas().asComponent().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e))
					menu.show(meanChart.getCanvas().asComponent(), e.getX(), e.getY());
			}
		});

		// add a pop up menu (on right click) for exporting to SVG
		PopupMenu combinedMenu = new PopupMenu();
		histogramChart.getCanvas().asComponent().add(combinedMenu);
		MenuItem combinedSvgExport = new MenuItem("SVG export");
		combinedMenu.add(combinedSvgExport);
		combinedSvgExport.addActionListener(e->{
			Document doc2 = histogramChart.getCanvas().paintSVG();
			SVGUtils.documentToXMLFile(doc2, new File("barchart_demo.svg"));
			System.out.println("exported barchart_demo.svg");
		});
		MenuItem combinedPdfExport = new MenuItem("PDF export");
		combinedMenu.add(combinedPdfExport);
		combinedPdfExport.addActionListener(e->{
			try {
				PDDocument doc = histogramChart.getCanvas().paintPDF();
				doc.save("barchart_demo.pdf");
				doc.close();
				System.out.println("exported barchart_demo.pdf");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

		histogramChart.getCanvas().asComponent().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e))
					combinedMenu.show(histogramChart.getCanvas().asComponent(), e.getX(), e.getY());
			}
		});
	}

}
