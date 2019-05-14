package jplotter.util;

public interface TickMarkGenerator {

	public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks, boolean verticalAxis);
	
}
