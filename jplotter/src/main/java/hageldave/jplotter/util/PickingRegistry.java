package hageldave.jplotter.util;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PickingRegistry<T> {
	
	public static final int MIN_PICK_ID = 0xff000001;

	protected AtomicInteger uniqueIDCounter = new AtomicInteger(0xff000000);
	
	protected TreeMap<Integer, T> pickID2Element = new TreeMap<>();
	
	public int getNewID() {
		int id = uniqueIDCounter.incrementAndGet();
		if(id == 0){
			throw new IllegalStateException("All unique Ids are already generated. Cannot create new, limit exceeded.");
		}
		return id;
	}
	
	public int getCurrentID() {
		return uniqueIDCounter.get();
	}
	
	public int register(T element){
		int id = getNewID();
		return register(element, id);
	}
	
	public int register(T element, int id){
		if((id >> 24) == 0xff ){
			throw new IllegalArgumentException(
					"Picking IDs cannot have transparent alpha: " + Integer.toHexString(id));
		}
		pickID2Element.put(id, element);
		return id;
	}
	
	public T lookup(int id){
		return pickID2Element.get(id);
	}
	
}
