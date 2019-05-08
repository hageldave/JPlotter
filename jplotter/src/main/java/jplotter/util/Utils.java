package jplotter.util;

import java.awt.geom.Point2D;
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
	
	@SuppressWarnings("unchecked")
	public static <T extends Point2D> T copy(T p){
		return (T) p.clone();
	}
	
}
