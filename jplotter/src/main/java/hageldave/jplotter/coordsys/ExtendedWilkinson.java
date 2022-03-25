package hageldave.jplotter.coordsys;

import static java.lang.Math.*;

import java.util.Arrays;

import hageldave.jplotter.util.Pair;

/**
 * Implementation of the extended Wilkinson algorithm for tick label positioning.
 * See <a href="http://vis.stanford.edu/papers/tick-labels">vis.stanford.edu/papers/tick-labels</a> for details.
 * <p>
 * The algorithm uses set of 'nice' value increments which are order by
 * 'niceness', i.e. earlier increment values are to be preferred over the later ones.
 * The algorithm tries to figure out which increments to use at what scale in order to
 * fill a desired value range with ticks.
 * It utilizes a scoring function to decide which tick marks to use.
 * <p>
 * By default the ordered set of nice increments is {1, 5, 2, 2.5, 4, 3, 1.5, 6, 8}.
 * This can be changed by sub classing this class and setting the {@link #Q} array
 * accordingly.
 * <p>
 * There is also a set of weights used to calculate the score of a tick mark sequence 
 * based on the qualities: simplicity, coverage, density and legibility (legibility 
 * calculation is not implemented).
 * The influence of these qualities on the score is determined by the weights in the
 * attribute {@link #w}. 
 * 
 * @author hageldave
 */
public class ExtendedWilkinson implements TickMarkGenerator {


	/** preference-decreasing ordered set of nice increments */
	protected double[] Q = new double[]{1, 5, 2, 2.5, 4, 3, 1.5, 6, 8};
	/** weights for simplicity, coverage, density and legibility */
	protected double[] w = new double[]{0.2, 0.25, 0.5, 0.05};

	/**
	 * Uses either decimal or scientific notation for the labels and strips unnecessary
	 * trailing zeros.
	 * If one of the specified tick values is formatted in scientific notation by the
	 * {@link String#format(String, Object...)} '%g' option, all values will use scientific
	 * notation, otherwise decimal is used.
	 * 
	 * @return String[] of labels corresponding to specified tick values
	 */



	@Override
	public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks,
			boolean verticalAxis) {
		double[] ticks = LabelsForTicks.getTicks(min, max, desiredNumTicks, this.Q, this.w);
		String[] labelsForTicks = LabelsForTicks.labelsForTicks(ticks);
		return new Pair<double[], String[]>(ticks, labelsForTicks);
	}


	/* STATIC DOWN HERE */


	static double coverage(double dmin, double dmax, double lmin, double lmax){
		return 1 - 0.5 * (pow(dmax - lmax,2) + pow(dmin - lmin,2)) / pow(0.1 * (dmax - dmin), 2);
	}

	static double coverage_max(double dmin, double dmax, double span){
		double drange = dmax - dmin;
		if(span > drange){
			return 1 - pow(0.5 * (span - drange),2) / pow(0.1 * drange,2);
		}
		return 1;
	}

	static double density(double k, double m, double dmin, double dmax, double lmin, double lmax){
		double r = (k - 1) / (lmax - lmin);
		double rt = (m - 1) / (max(lmax, dmax) - min(lmin, dmin));
		return 2 - max(r / rt, rt / r);
	}

	static double density_max(double k, double m){
		if(k >= m){
			return 2 - (k - 1.0) / (m - 1.0);
		}
		return 1;
	}

	static double simplicity(double q, double[] Q, double j, double lmin, double lmax, double lstep){
		double eps = 1e-10;
		int n = Q.length;
		int i = Arrays.binarySearch(Q, q) + 1;
		int v = 0;
		if( ((lmin % lstep) < eps) || ((((lstep - lmin) % lstep) < eps) && (lmin <= 0) && (lmax >= 0)) ){
			v = 1;
		} else {
			v = 0;
		}
		return (n - i) / (n - 1.0) + v - j;
	}

	static double simplicity_max(double q, double[] Q, int j){
		int n = Q.length;
		int i = Arrays.binarySearch(Q, q) + 1;
		int v = 1;
		return (n - i) / (n - 1.0) + v - j;
	}

	static double legibility(double lmin, double lmax, double lstep){
		return 1;
	}

	static double score(double[] weights, double simplicity, double coverage, double density, double legibility){
		return weights[0] * simplicity + weights[1] * coverage + weights[2] * density + weights[3] * legibility;
	}

	static double[] ext_wilk(double dmin, double dmax, int m, int onlyInside, double[] Q, double[] w){
		if(dmin >= dmax || m < 1){
			return new double[]{dmin, dmax, dmax-dmin,1,0,2,0};
		}
		double best_score = -1.0;
		double[] result = null;

		int j = 1;
		while(j < 5){
			for(double q:Q){
				double sm = simplicity_max(q, Q, j);
				if(score(w, sm,  1, 1, 1) < best_score){
					j = Integer.MAX_VALUE-1;
					break;
				}
				int k = 2;
				while(k <= m+2){
					double dm = density_max(k, m);
					if(score(w, sm, 1, dm, 1) < best_score){
						break;
					}
					double delta = (dmax - dmin) / (k + 1.) / j / q;
					int z = (int)ceil(log10(delta));
					int zmax = z+3;
					while(z < zmax){
						double step = j*q*pow(10,z);
						double cm = coverage_max(dmin, dmax, step*(k-1));
						if(score(w, sm, cm, dm, 1) < best_score){
							break;
						}

						double min_start = floor(dmax / step) * j - (k - 1) * j;
						double max_start = ceil(dmin / step) * j;

						if(min_start > max_start || /*precision insanity check*/(min_start+1)==min_start){
							z++;
							break;
						}
						for(double start=min_start; start<max_start+1; start++){
							double lmin = start * (step/j);
							double lmax = lmin + step * (k-1);
							double lstep = step;

							double s = simplicity(q, Q, j, lmin, lmax, lstep);
							double c = coverage(dmin, dmax, lmin, lmax);
							double d = density(k, m, dmin, dmax, lmin, lmax);
							double l = legibility(lmin, lmax, lstep);
							double scr = score(w, s,c,d,l);
							if( 	scr > best_score 
									&& 
									(onlyInside <= 0 || (lmin >= dmin && lmax <= dmax))
									&&
									(onlyInside >= 0 || (lmin <= dmin && lmax >= dmax))
									){
								best_score = scr;
								result = new double[]{lmin, lmax, lstep, j, q, k, scr};
							}
						}
						z++;
					}
					k++;
				}
			}
			j++;
		}
		return result;
	}

}
