package hageldave.jplotter.util;

import java.util.Objects;

/**
 * Pair class. 
 * Access directly through {@link #first} and {@link #second}.
 * 
 * @author hageldave
 *
 * @param <T1> type 1
 * @param <T2> type 2
 */
public class Pair<T1,T2> {

	public final T1 first;
	
	public final T2 second;
	
	/**
	 * Creates a pair
	 * @param first 
	 * @param second
	 */
	public Pair(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}
	
	/**
	 * syntactic sugar
	 * @param first
	 * @param second
	 * @return a new Pair
	 */
	public static <T1,T2> Pair<T1, T2> of(T1 first, T2 second){
		return new Pair<>(first, second);
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj instanceof Pair){
			Pair<?,?> other = (Pair<?,?>)obj;
			return Objects.equals(first, other.first) && Objects.equals(second, other.second);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(first,second);
	}
	
	@Override
	public String toString() {
		return String.format("{%s, %s}", first,second);
	}
	
}
