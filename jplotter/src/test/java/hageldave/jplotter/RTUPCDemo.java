package hageldave.jplotter;

import hageldave.jplotter.charts.ParallelCoords;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class RTUPCDemo {
    public static void main(String[] args) {
        ParallelCoords coords = new ParallelCoords(false);

        coords.getDataModel().addFeature(4.3,7.9, "sepal length");
        coords.getDataModel().addFeature(2.0,4.4, "sepal width");
        coords.getDataModel().addFeature(1.0,6.9, "petal length");
        coords.getDataModel().addFeature(0.1,2.5, "petal width");

        ArrayList<double[]> setosaList = new ArrayList<>();
        ArrayList<double[]> versicolorList = new ArrayList<>();
        ArrayList<double[]> virginicaList = new ArrayList<>();

        try (Scanner sc = new Scanner(new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data").openStream())) {
            while(sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                if (nextLine.isEmpty()) {
                    continue;
                }
                String[] fields = nextLine.split(",");
                double[] values = new double[4];
                values[0] = Double.parseDouble(fields[0]);
                values[1] = Double.parseDouble(fields[1]);
                values[2] = Double.parseDouble(fields[2]);
                values[3] = Double.parseDouble(fields[3]);
                if(fields[4].contains("setosa")){
                    setosaList.add(values);
                } else if(fields[4].contains("versicolor")) {
                    versicolorList.add(values);
                } else {
                    virginicaList.add(values);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[][] setosa = new double[][]{};
        double[][] versicolor = new double[][]{};
        double[][] virginica = new double[][]{};

        setosa = setosaList.toArray(setosa);
        versicolor = versicolorList.toArray(versicolor);
        virginica = virginicaList.toArray(virginica);
        coords.getDataModel().addData(setosa,  "setosa");
        coords.getDataModel().addData(versicolor,  "setosa");
        coords.getDataModel().addData(virginica,  "setosa");

        coords.display("Iris");
    }
}
