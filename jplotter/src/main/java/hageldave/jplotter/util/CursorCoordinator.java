package hageldave.jplotter.util;

import java.awt.Component;
import java.awt.Cursor;
import java.util.HashMap;

/**
 * Coordinates which {@link Cursor} gets set for a {@link Component}.
 * <br>
 * The main problem witch setting cursor from interaction classes that are 
 * oblivious of each other is when they set default cursor because no specific
 * cursor is needed in their current state.
 * Another interaction class may have already set a specific cursor which is
 * then overwritten by the class that has no specific preference.
 * <br>
 * This coordinator allows to request a specific cursor (which is immediately
 * set), and to signal having no specific preference (resulting in default 
 * cursor when nobody currently has a preference).
 * <br>
 * All CursorCoordinators are held in a static map and can be obtained via
 * {@link #get(Component)}.
 */
public class CursorCoordinator {
	/** component on which cursor is set */
	public Component component;
	/** currently requested cursor, null if no preference */
	public Cursor requestedCursor = null;
	/** the requester of the cursor, null if no preference */
	public Object requester = null;
	
	
	private CursorCoordinator(Component component) {
		this.component = component;
	}
	
	/**
	 * Request a specific cursor, or signal to have no preference.
	 * @param cursor to request or null if no preference.
	 * @param requester the object responsible for the cursor request
	 */
	public void requestCursor(Cursor cursor, Object requester) {
		if(cursor != null) {
			this.requestedCursor = cursor;
			this.requester = requester;
		} else {
			if (this.requester == requester) {
				this.requestedCursor = null;
				this.requester = null;
			} else {
				return; // only the requester can release the cursor
			}
		}
		
		this.component.setCursor(requestedCursor != null ? requestedCursor : Cursor.getDefaultCursor());
	}
	
	/**
	 * Request no specific cursor ("don't care").
	 * @param requester the object responsible for the cursor request
	 */
	public void requestAnyCursor(Object requester) {
		this.requestCursor(null, requester);
	}
	
	//////////////////////////////////////////////
	
	private static HashMap<Component, CursorCoordinator> cursorManagerMap = new HashMap<>();
	
	/**
	 * Return the {@link CursorCoordinator} for the specified component.
	 * The {@link CursorCoordinator} will be constructed on first demand.
	 * @param component for which a coordinator should be returned
	 * @return the component's CursorCoordinator
	 */
	public static CursorCoordinator get(Component component) {
		CursorCoordinator cm = cursorManagerMap.get(component);
		if(cm == null) {
			cm = new CursorCoordinator(component);
			cursorManagerMap.put(component, cm);
		}
		return cm;
	}
	
}
