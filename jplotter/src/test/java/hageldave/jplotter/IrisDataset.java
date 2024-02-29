package hageldave.jplotter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class IrisDataset {
	
	String[] labelNames = {"Setosa", "Versicolor", "Virginica"};
	String[] featureNames = {"sepal length", "sepal width", "petal length", "petal width"};
	
	double[][] data;
	int[] labels;
	
	public IrisDataset() {
		ArrayList<double[]> raw = load();
		data = raw.stream().map(row->Arrays.copyOf(row, 4)).toArray(double[][]::new);
		labels = raw.stream().mapToInt(row->(int)row[4]).toArray();
	}
	
	public String[] getLabelNames() {
		return labelNames;
	}
	
	public String[] getFeatureNames() {
		return featureNames;
	}
	
	public double[][] getData() {
		return data;
	}
	
	public int[] getLabels() {
		return labels;
	}
	
	public int getNumPoints() {
		return data.length;
	}

	public ArrayList<double[]> load() {
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
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return dataset;
	}
	
}
