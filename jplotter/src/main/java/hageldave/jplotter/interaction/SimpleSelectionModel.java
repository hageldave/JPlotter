package hageldave.jplotter.interaction;

import java.awt.AWTEventMulticaster;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class SimpleSelectionModel<T> {
	
	public static interface SimpleSelectionListener<T> {
		public void selectionChanged(SortedSet<T> selection);
	}

	protected SortedSet<T> selection;
	protected ActionListener selectionListener;
	
	public T getFirstOrDefault(T def){
		return selection.isEmpty() ? def:selection.first();
	}
	
	public T getLastOrDefault(T def){
		return selection.isEmpty() ? def:selection.last();
	}
	
	public SimpleSelectionModel(Comparator<? super T> comp) {
		this.selection = new TreeSet<>(comp);
	}
	
	public SimpleSelectionModel() {
		this(null);
	}
	
	public SortedSet<T> getSelection() {
		return selection;
	}
	
	public void setSelection(Iterable<T> selection){
		TreeSet<T> old = new TreeSet<>(this.selection);
		this.selection.clear();
		for(T e: selection)
			this.selection.add(e);
		if(!old.equals(this.selection))
			notifySelectionChange();
	}
	
	@SafeVarargs
	public final void setSelection(T ... elements){
		setSelection(Arrays.asList(elements));
	}
	
	public synchronized void addSelectionListener(SimpleSelectionListener<T> l){
		if(l==null)
			return;
		selectionListener = AWTEventMulticaster.add(selectionListener, e -> l.selectionChanged(getSelection()));
	}
	
	public synchronized void notifySelectionChange(){
		if(selectionListener != null){
			selectionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, "selectionchanged"));
		}
	}
}
