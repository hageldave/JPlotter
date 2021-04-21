package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.charts.ScatterPlot;
import hageldave.jplotter.misc.DefaultGlyph;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

public class ReadyScatterPlot {

    private static double[][] randomData(int n){
        double[][] d = new double[n][3];
        for(int i=0; i<n; i++){
            d[i][0]=Math.random()*2-1;
            d[i][1]=Math.random()*2-1;
            d[i][2]=(d[i][1]+1)/2;
        }
        return d;
    }

    public static void main(String[] args) {
        // generate or get data
        double[][] dataA = randomData(50);
        double[][] dataB = randomData(100);

        ScatterPlot plot = new ScatterPlot(false);
        plot.addPoints(1, dataA, DefaultGlyph.CIRCLE, Color.BLUE);
        plot.addPoints(2, dataB, DefaultGlyph.ARROW, Color.RED);
        plot.new PointClickedInterface() {
            @Override
            public void pointClicked(Point mouseLocation, Point2D pointLocation, double[][] data, int dataIndex) {
                System.out.println("a point was clicked");
            }
        }.register();

        // display within a JFrame
        JFrame frame = new JFrame();
        frame.getContentPane().add(plot.getCanvas().asComponent());
        frame.setTitle("scatterplot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        plot.getCanvas().addCleanupOnWindowClosingListener(frame);
        // make visible on AWT event dispatch thread
        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
        });

        long t=System.currentTimeMillis()+2000;
        while(t>System.currentTimeMillis());
        if("false".equals("true"))
            SwingUtilities.invokeLater(()->{
                Img img = new Img(frame.getSize());
                img.paint(g2d->frame.paintAll(g2d));
                ImageSaver.saveImage(img.getRemoteBufferedImage(), "scatterplot.png");
            });
    }

}
