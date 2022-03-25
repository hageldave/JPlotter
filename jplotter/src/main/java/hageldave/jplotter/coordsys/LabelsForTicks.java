package hageldave.jplotter.coordsys;

import java.util.Locale;

public class LabelsForTicks {
    /**
     * Uses either decimal or scientific notation for the labels and strips unnecessary
     * trailing zeros.
     * If one of the specified tick values is formatted in scientific notation by the
     * {@link String#format(String, Object...)} '%g' option, all values will use scientific
     * notation, otherwise decimal is used.
     *
     * @param ticks to be labeled
     * @return String[] of labels corresponding to specified tick values
     */
    public static String[] labelsForTicks(double[] ticks)
    {
        String str1 = String.format(Locale.US, "%g", ticks[0]);
        String str2 = String.format(Locale.US, "%g", ticks[ticks.length - 1]);
        String[] labels = new String[ticks.length];
        if (str1.contains("e") || str2.contains("e")) {
            for (int i = 0; i < ticks.length; i++) {
                String l = String.format(Locale.US, "%e", ticks[i]);
                String[] Esplit = l.split("e", -2);
                String[] dotsplit = Esplit[0].split("\\.", -2);
                dotsplit[1] = ('#' + dotsplit[1])
                        .replaceAll("0", " ")
                        .trim()
                        .replaceAll(" ", "0")
                        .replaceAll("#", "");
                dotsplit[1] = dotsplit[1].isEmpty() ? "0" : dotsplit[1];
                l = dotsplit[0] + '.' + dotsplit[1] + 'e' + Esplit[1];
                labels[i] = l;
            }
        } else {
            for (int i = 0; i < ticks.length; i++) {
                String l = String.format(Locale.US, "%f", ticks[i]);
                if (l.contains(".")) {
                    String[] dotsplit = l.split("\\.", -2);
                    dotsplit[1] = ('#' + dotsplit[1])
                            .replaceAll("0", " ")
                            .trim()
                            .replaceAll(" ", "0")
                            .replaceAll("#", "");
                    if (dotsplit[1].isEmpty()) {
                        l = dotsplit[0];
                    } else {
                        l = dotsplit[0] + '.' + dotsplit[1];
                    }
                }
                labels[i] = l;
            }
        }
        return labels;
    }

    static double[] getTicks(double dmin, double dmax, int m, double[] Q, double[] w) {
        double[] l = ExtendedWilkinson.ext_wilk(dmin, dmax, m, 1, Q, w);
        double lmin = l[0];
        //double lmax  = l[1];
        double lstep = l[2];
        //int    j =(int)l[3];
        //double q     = l[4];
        int k = (int) l[5];
        //double scr   = l[6];

        double[] ticks = new double[k];
        for (int i = 0; i < k; i++) {
            ticks[i] = lmin + i * lstep;
        }
        return ticks;
    }
}