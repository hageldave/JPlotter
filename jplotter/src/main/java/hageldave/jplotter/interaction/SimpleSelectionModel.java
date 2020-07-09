package hageldave.jplotter.interaction;

import java.awt.AWTEventMulticaster;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A simple slection model for sets of elements using the listener pattern.
 * The model uses a SortedSet to remember a set of items (e.g. selected items).
 * When the selection (the item set) is changed, it will notify its registered
 * {@link SimpleSelectionListener}s of the change.
 * <p>
 * This can be used to synchronize across different UI elements 
 * and can also be used for single valued components such as sliders.
 * 
 * @author hageldave
 * @param <T> item type
 */
public class SimpleSelectionModel<T> {
	
	/**
	 * Listener interface for {@link SimpleSelectionModel}.
	 * 
	 * @author hageldave
	 * @param <T> item type
	 */
	public static interface SimpleSelectionListener<T> {
		/**
		 * is called when model's selection changed.
		 * @param selection the current (new) set of selected items.
		 */
		public void selectionChanged(SortedSet<T> selection);
	}

	protected SortedSet<T> selection;
	protected ActionListener selectionListener;
	
	/**
	 * @param def default value to return when selection is empty
	 * @return first element of the selection or the argument when selection is empty.
	 */
	public T getFirstOrDefault(T def){
		return selection.isEmpty() ? def:selection.first();
	}
	
	/**
	 * @param def default value to return when selection is empty
	 * @return last element of the selection or the argument when selection is empty.
	 */
	public T getLastOrDefault(T def){
		return selection.isEmpty() ? def:selection.last();
	}
	
	
	/**
	 * Creates a new SimpleSelectionModel using the specified comparator
	 * to sort the model's items.
	 * @param comp comparator to be used (null for natural ordering)
	 */
	public SimpleSelectionModel(Comparator<? super T> comp) {
		this.selection = new TreeSet<>(comp);
	}
	
	/**
	 * Creates a new SimpleSelectionModel.
	 */
	public SimpleSelectionModel() {
		this(null);
	}
	
	/**
	 * @return the current selection
	 */
	public SortedSet<T> getSelection() {
		return selection;
	}
	
	/**
	 * Sets the selection of this model to the specified elements.
	 * @param selection iterable containing the elements (may be empty)
	 */
	public void setSelection(Iterable<T> selection){
		TreeSet<T> old = new TreeSet<>(this.selection);
		this.selection.clear();
		for(T e: selection)
			this.selection.add(e);
		if(!old.equals(this.selection))
			notifySelectionChange();
	}
	
	/**
	 * Sets the selection of this model to the specified elements.
	 * @param elements to set as selection (may be empty)
	 */
	@SafeVarargs
	public final void setSelection(T ... elements){
		setSelection(Arrays.asList(elements));
	}
	
	/**
	 * Adds a {@link SimpleSelectionListener} to this model that will be notified on changes. 
	 * @param l the listener to be called on change
	 * @return the wrapping {@link ActionListener} object for later removal of the listener.
	 */
	public synchronized ActionListener addSelectionListener(SimpleSelectionListener<T> l){
		if(l==null)
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
	
	public synchronized void removeSelectionListener(ActionListener wrapper){
		AWTEventMulticaster.remove(selectionListener, wrapper);
	}
	
	/**
	 * notifies the listeners of a change to the selection
	 */
	public synchronized void notifySelectionChange(){
		if(selectionListener != null){
			selectionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, "selectionchanged"));
		}
	}
}
