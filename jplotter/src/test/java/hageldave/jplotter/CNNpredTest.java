package hageldave.jplotter;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.TimePassedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.TimeUnit;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.LinesRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import static hageldave.jplotter.interaction.InteractionConstants.X_AXIS;

public class CNNpredTest {

    static JPlotterCanvas mkCanvas(boolean fallback, JPlotterCanvas contextShareParent) {
        return fallback ? new BlankCanvasFallback() : new BlankCanvas((FBOCanvas)contextShareParent);
    }

    static boolean useFallback(String[] args) {
        return Arrays.stream(args).filter(arg->"jplotter_fallback=true".equals(arg)).findAny().isPresent();
    }

    static boolean fallbackModeEnabled;

    public static void main(String[] args) throws IOException, ParseException {
        fallbackModeEnabled = useFallback(args);
        ColorScheme colorScheme = DefaultColorScheme.LIGHT.get();

        // setup content
        ArrayList<String[]> dataset = new ArrayList<>();
        //URL cnnsrc = new URL("https://archive.ics.uci.edu/ml/datasets/CNNpred%3A+CNN-based+stock+market+prediction+using+a+diverse+set+of+variables");
        try (InputStream stream = CNNpredTest.class.getResourceAsStream("/processed_nasdaq.csv");
             Scanner sc = new Scanner(stream))
        {
            while(sc.hasNextLine()){
                String nextLine = sc.nextLine();
                if(nextLine.isEmpty()){
                    continue;
                }
                String[] fields = nextLine.split(",");

                if (fields.length == 7) {
                    String[] values = new String[7];
                    values[0] = String.valueOf(fields[0]);
                    values[1] = String.valueOf(fields[1]);
                    values[2] = String.valueOf(fields[2]);
                    values[3] = String.valueOf(fields[3]);
                    values[4] = String.valueOf(fields[4]);
                    values[5] = String.valueOf(fields[5]);
                    values[6] = String.valueOf(fields[6]);
                    dataset.add(values);
                }
            }
        }

        CoordSysRenderer coordsys = new CoordSysRenderer(colorScheme);

        LinesRenderer lr = new LinesRenderer();
        Lines l = new Lines();
        lr.addItemToRender(l);
        coordsys.setContent(lr);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date date = sdf.parse(dataset.get(2)[0]);
        LocalDateTime startDate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        long daysFromStartToEnd = 0;

        int colorIndex = 0;
        for (int j = 2; j < 6; j++) {
            double intermediateValue = Double.parseDouble(dataset.get(1)[j]);
            for (int i = 2; i < dataset.size()-1; i++) {
                String[] currentLine = dataset.get(i);
                String[] nextLine = dataset.get(i+1);

                Date currentDate = sdf.parse(currentLine[0]);
                LocalDateTime currentDateTime = LocalDateTime.ofInstant(currentDate.toInstant(), ZoneId.systemDefault());

                Date nextDate = sdf.parse(nextLine[0]);
                LocalDateTime nextDateTime = LocalDateTime.ofInstant(nextDate.toInstant(), ZoneId.systemDefault());

                long durationInDays = Duration.between(startDate, currentDateTime).toDays();
                long nextDurationInDays = Duration.between(startDate, nextDateTime).toDays();

                daysFromStartToEnd = nextDurationInDays;

                if (!currentLine[j].equals("") && !nextLine[j].equals("")) {
                    l.addSegment(
                            new Point2D.Double(durationInDays, intermediateValue),
                            new Point2D.Double(nextDurationInDays, intermediateValue + intermediateValue * Double.parseDouble(nextLine[j]))
                            ).setColor(DefaultColorMap.Q_8_SET2.getColor(colorIndex)).setThickness(1.3);
                    intermediateValue = intermediateValue + intermediateValue * Double.parseDouble(nextLine[j]);
                }
            }
            colorIndex++;
        }

        coordsys.setLegendBottom(new Legend()
                .addLineLabel(1, DefaultColorMap.Q_8_SET2.getColor(0), "AAPL")
                .addLineLabel(1, DefaultColorMap.Q_8_SET2.getColor(1), "AMZN")
                .addLineLabel(1, DefaultColorMap.Q_8_SET2.getColor(2), "JNJ")
                .addLineLabel(1, DefaultColorMap.Q_8_SET2.getColor(3), "JPM")
                .addLineLabel(1, DefaultColorMap.Q_8_SET2.getColor(4), "MSFT")
        );

        //coordsys.setTickMarkGenerator(new DateTimeWilkinson(TimeUnit.Day, 1, startDate), new ExtendedWilkinson());
        //coordsys.setTickMarkGenerator(new DateTimeWilkinson(TimeUnit.Hour, 1, startDate), new ExtendedWilkinson());
        coordsys.setTickMarkGenerator(new TimePassedWilkinson(TimeUnit.Day), new ExtendedWilkinson());
        coordsys.setCoordinateView(0, -10, daysFromStartToEnd, 180);

        coordsys.setxAxisLabel("Development over time");
        coordsys.setyAxisLabel("Stock price in $ after close");

        JFrame frame = new JFrame("CNNpred");
        JPlotterCanvas canvas = fallbackModeEnabled ? new BlankCanvas() : new BlankCanvasFallback();
        canvas.setRenderer(coordsys);
        canvas.asComponent().setPreferredSize(new Dimension(500, 300));
        canvas.asComponent().setBackground(Color.WHITE);
        frame.getContentPane().add(canvas.asComponent());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.addCleanupOnWindowClosingListener(frame);

        new CoordSysScrollZoom(canvas, coordsys).register().setZoomedAxes(X_AXIS);
        new CoordSysPanning(canvas, coordsys).register();

        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
            frame.transferFocus();
        });
    }
}
