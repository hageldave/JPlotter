package hageldave.jplotter.util;

import hageldave.jplotter.CoordSysCanvas;

/**
 * The TickMarkGenerator interface provides the
 * {@link #genTicksAndLabels(double, double, int, boolean)} method.
 * It is used by the {@link CoordSysCanvas} to obtain tick marks and
 * labels for its current view of coordinates.
 * 
 * @author hageldave
 */
public interface TickMarkGenerator {

	/**
	 * Generates a number of tick marks and corresponding labels.
	 * The first entry of the returned pair are the tick mark values,
	 * the second part are the corresponding labels.
	 * 
	 * @param min minimum of value range for which ticks marks are to be generated
	 * @param max maximum of value range for which ticks marks are to be generated
	 * @param desiredNumTicks the desired number of generated tick marks, not obligatory,
	 * can also create less or more tick marks if that leads to better tick values.
	 * @param verticalAxis true if marks are for vertical axis, false when for horizontal axis
	 * @return pair of a tick mark value array and corresponding label array for these values
	 */
	public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks, boolean verticalAxis);
	
}
