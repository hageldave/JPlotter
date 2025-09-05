package hageldave.jplotter.util;

import java.util.ArrayList;

/**
 * BilloProfiler is a low fidelity manual profiling utility.
 * Billo = Billig (German slang) = cheap/tacky.
 * <br>
 * You need to manually put {@link #start()} and {@link #stop()} at the piece of code you like to profile,
 * and sprinkle {@link #split(String)} inbetween to get timings for different sections.
 * Then you can simply print the profiler to get the total time and fractions of the time for the sections.
 * You can use the global {@link #instance} to facilitate profiling over multiple classes.
 */
public class BilloProfiler {
	
	public static final BilloProfiler instance = new BilloProfiler();

	ArrayList<Pair<String, Long>> timings = new ArrayList<>(100);
	
	public BilloProfiler clear() {
		timings.clear();
		return this;
	}
	
	public BilloProfiler start() {
		this.clear();
		timings.add(Pair.of("start", System.nanoTime()));
		return this;
	}
	
	public BilloProfiler stop() {
		timings.add(Pair.of("stop", System.nanoTime()));
		return this;
	}
	
	public BilloProfiler split(String identifier) {
		timings.add(Pair.of(identifier, System.nanoTime()));
		return this;
	}
	
	public long time(int i) {
		return timings.get(i).second.longValue();
	}
	
	public String identifier(int i) {
		return timings.get(i).first;
	}
	
	public long total() {
		if(timings.size() < 2)
			return 0;
		else return time(timings.size()-1) - time(0);
	}
	
	public long[] deltas() {
		long[] deltas = new long[timings.size()-1];
		for(int i=0; i<deltas.length; i++) {
			deltas[i] = time(i+1)-time(i);
		}
		return deltas;
	}
	
	@Override
	public String toString() {
		long total = total();
		if(total == 0) {
			return "[]";
		}
		double divByTotal = 1.0/total;
		StringBuilder sb = new StringBuilder();
		sb.append("[total ms ");
		sb.append(String.format("%.3f ", total*0.000001));
		long[] deltas = deltas();
		for(int i=0; i<deltas.length; i++) {
			sb.append(identifier(i));
			sb.append(String.format(" %.2f ", deltas[i]*divByTotal*100));
		}
		sb.append(identifier(timings.size()-1));
		sb.append("]");
		return sb.toString();
	}
	
}