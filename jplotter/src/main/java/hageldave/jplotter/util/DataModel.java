package hageldave.jplotter.util;

import java.util.LinkedList;
import java.util.List;


public class DataModel<T> {
    // TODO maybe change to optional
    protected T value;
    protected List<ValueListener<T>> listeners = new LinkedList<>();

    public DataModel() {
    }

    public DataModel(T val) {
        this.value = val;
    }

    public void setSelectedItem(T val) {
        if (val == null) {
            value = val;
            notifyListeners(null);
        } else if(!val.equals(value)) {
            T old = value;
            value = val;
            notifyListeners(old);
        }
    }

    /**
     * adds a new value listener to this model.
     * @param l listener to add
     */
    public void addValueListener(ValueListener<T> l) {listeners.add(l);}

    public void addValueListener(OldNewValueListener<T> l) {listeners.add(l);}

    /**
     * notifies all listeners that value has changed
     */
    public void notifyListeners(T old) {
        listeners.forEach(l->{
            if(l instanceof OldNewValueListener) {
                ((OldNewValueListener<T>)l).valueChanged(old, value);
            } else {
                l.valueChanged(value);
            }
        });
    }

    public Object getSelectedItem() {
        return value;
    }

    /**
     *
     * @param <T>
     */
    public interface ValueListener<T> {
        /**
         * is called when a value changed
         * @param valNew the new value
         */
        void valueChanged(T valNew);
    }

    /**
     *
     * @param <T>
     */
    public interface OldNewValueListener<T> extends ValueListener<T> {

        @Override
        default void valueChanged(T valNew) {
            valueChanged(valNew, valNew);
        }

        /**
         * is called when value changed
         * @param valOld the previous value
         * @param valNew the current value that was just set
         */
        public void valueChanged(T valOld, T valNew);
    }
}
