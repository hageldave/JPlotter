package hageldave.jplotter.util;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import hageldave.jplotter.canvas.FBOCanvas;

/**
 * The PickingRegistry class is a utility class for obtaining unique
 * IDs for usage with picking color (see {@link FBOCanvas}).
 * <p>
 * When using {@link #register(Object)} a new ID is obtained while memorizing
 * the specified element that will be associated with that ID and can later be
 * looked up using {@link #lookup(int)}.
 * This makes picking color specification and identification of objects by 
 * picking color an easy task.
 * <p>
 * It is recommended to only use a single registry per canvas since multiple
 * would create duplicate IDs within the same draw space.
 * 
 * @author hageldave
 * @param <T> Element type of the registry
 */
public class PickingRegistry<T> {

	protected AtomicInteger uniqueIDCounter = new AtomicInteger(0xff000000);
	
	protected TreeMap<Integer, T> pickID2Element = new TreeMap<>();
	
	/**
	 * Generates a new ID. IDs range from 0xff000001 to 0xffffffff.
	 * @return a new ID by incrementing the ID counter
	 * @throws IllegalStateException when all unique IDs have been generated already. 
	 * The total number of possibles ID's is {@code 0xffffff-1 = 16.777.214}.
	 */
	public int getNewID() {
		int id = uniqueIDCounter.incrementAndGet();
		if(id == 0){
			throw new IllegalStateException("All unique Ids are already generated. Cannot create new, limit exceeded.");
		}
		return id;
	}
	
	/**
	 * @return the current ID, i.e. the counters current value
	 */
	public int getCurrentID() {
		return uniqueIDCounter.get();
	}
	
	/**
	 * Generates a new ID and associates it with the specified element.
	 * @param element to associate with generated ID
	 * @return the generated ID
	 * @see #lookup(int)
	 */
	public int register(T element){
		int id = getNewID();
		return register(element, id);
	}
	
	/**
	 * Associates the specified element with the specified ID.
	 * @param element to register
	 * @param id that maps to element
	 * @return the specified ID
	 * @throws IllegalArgumentException if the most significant 8 bits are
	 * not 0xff, i.e. the ID has a translucent alpha channel if used as a color.
	 */
	public int register(T element, int id){
		if((id >>> 24) != 0xff ){
			throw new IllegalArgumentException(
					"Picking IDs cannot have transparent alpha: " + Integer.toHexString(id));
		}
		pickID2Element.put(id, element);
		return id;
	}
	
	public static void main(String[] args) {
		new PickingRegistry<>().register(null, 0xfe004544);
	}
	
	/**
	 * Looks up the element value associated with the specified ID.
	 * @param id to look up
	 * @return the associated element or null if the ID is not associated
	 * with an element.
	 * @see #register(Object)
	 */
	public T lookup(int id){
		return pickID2Element.get(id);
	}
	
}
