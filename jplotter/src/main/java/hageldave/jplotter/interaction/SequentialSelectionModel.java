package hageldave.jplotter.interaction;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple selection model for lists of elements using the listener pattern.
 * The model uses a LinkedList to remember a list of items (e.g. selected items).
 * When the selection (the item list) is changed, it will notify its registered
 * {@link SequentialSelectionListener}s of the change.
 * <p>
 * This can be used to synchronize across different UI elements
 * and can also be used for single valued components such as sliders.
 *
 * @param <T> item type
 */
public class SequentialSelectionModel<T> {

    /**
     * Listener interface for {@link SequentialSelectionModel}.
     *
     * @param <T> item type
     * @author hageldave
     */
    public static interface SequentialSelectionListener<T> {
        /**
         * is called when model's selection changed.
         *
         * @param selection the current (new) list of selected items.
         */
        public void selectionChanged(List<T> selection);
    }

    protected LinkedList<T> selection;
    protected ActionListener selectionListener;

    /**
     * @param def default value to return when selection is empty
     * @return first element of the selection or the argument when selection is empty.
     */
    public T getFirstOrDefault(T def) {
        return selection.isEmpty() ? def : selection.getFirst();
    }

    /**
     * @param def default value to return when selection is empty
     * @return last element of the selection or the argument when selection is empty.
     */
    public T getLastOrDefault(T def) {
        return selection.isEmpty() ? def : selection.getLast();
    }

    /**
     * Creates a new SimpleSelectionModel.
     */
    public SequentialSelectionModel() {
        this.selection = new LinkedList<>();
    }

    /**
     * @return the current selection
     */
    public LinkedList<T> getSelection() {
        return selection;
    }

    /**
     * Sets the selection of this model to the specified elements.
     *
     * @param selection iterable containing the elements (may be empty)
     */
    public void setSelection(Iterable<T> selection) {
        List<T> old = new LinkedList<>(this.selection);
        this.selection.clear();
        for (T e : selection)
            this.selection.add(e);
        if (!old.equals(this.selection))
            notifySelectionChange();
    }

    /**
     * Sets the selection of this model to the specified elements.
     *
     * @param elements to set as selection (may be empty)
     */
    @SafeVarargs
    public final void setSelection(T... elements) {
        setSelection(Arrays.asList(elements));
    }

    /**
     * Adds a {@link hageldave.jplotter.interaction.SimpleSelectionModel.SimpleSelectionListener} to this model that will be notified on changes.
     *
     * @param l the listener to be called on change
     * @return the wrapping {@link ActionListener} object for later removal of the listener.
     */
    public synchronized ActionListener addSelectionListener(SequentialSelectionListener<T> l) {
        if (l == null)
            return null;
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                l.selectionChanged(getSelection());
            }
        };
        selectionListener = AWTEventMulticaster.add(selectionListener, al);
        return al;
    }

    public synchronized void removeSelectionListener(ActionListener wrapper) {
        AWTEventMulticaster.remove(selectionListener, wrapper);
    }

    /**
     * notifies the listeners of a change to the selection
     */
    public synchronized void notifySelectionChange() {
        if (selectionListener != null) {
            selectionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, "selectionchanged"));
        }
    }
}

