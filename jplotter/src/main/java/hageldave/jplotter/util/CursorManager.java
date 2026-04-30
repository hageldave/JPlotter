package hageldave.jplotter.util;

import java.awt.Component;
import java.awt.Cursor;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

public class CursorManager {
	Component component;
	Cursor requestedCursor = null;
	Object requester = null;
	AtomicBoolean isCursorRequested = new AtomicBoolean(false);
	
	private CursorManager(Component component) {
		this.component = component;
	}
	
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

//		if(isCursorRequested.compareAndSet(false, true)) {
//			SwingUtilities.invokeLater(()->{
//				isCursorRequested.set(false);
//				component.setCursor(requestedCursor);
//				requestedCursor = null;
//			});
//		}
	}
	
	//////////////////////////////////////////////
	
	private static HashMap<Component, CursorManager> cursorManagerMap = new HashMap<>();
	
	public static CursorManager get(Component component) {
		CursorManager cm = cursorManagerMap.get(component);
		if(cm == null) {
			cm = new CursorManager(component);
			cursorManagerMap.put(component, cm);
		}
		return cm;
	}
	
}
