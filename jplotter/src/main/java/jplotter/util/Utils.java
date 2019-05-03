package jplotter.util;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public class Utils {

	public static void execOnAWTEventDispatch(Runnable runnable){
		if(SwingUtilities.isEventDispatchThread()){
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (InvocationTargetException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
}
