package hageldave.jplotter.util;

import java.util.Objects;

public class Quadruple<T1,T2, T3, T4> {

    public final T1 first;

    public final T2 second;

    public final T3 third;

    public final T4 fourth;

    /**
     * Creates a pair
     * @param first part of pair
     * @param second part of pair
     */
    public Quadruple(T1 first, T2 second, T3 third, T4 fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    /**
     * syntactic sugar
     * @param first part of pair
     * @param second part of pair
     * @return a new Pair
     *
     * @param <T1> type 1
     * @param <T2> type 2
     */
    public static <T1,T2,T3,T4> Quadruple<T1, T2, T3, T4> of(T1 first, T2 second, T3 third, T4 fourth){
        return new Quadruple<>(first, second, third, fourth);
    }


    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Quadruple){
            Quadruple<?,?,?,?> other = (Quadruple<?,?,?,?>)obj;
            return Objects.equals(first, other.first) && Objects.equals(second, other.second)
                    && Objects.equals(third, other.third) && Objects.equals(fourth, other.fourth);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first,second,third,fourth);
    }

    @Override
    public String toString() {
        return String.format("{%s, %s, %s, %s}", first,second,third,fourth);
    }

}
