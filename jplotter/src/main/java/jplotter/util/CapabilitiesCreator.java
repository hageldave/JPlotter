package jplotter.util;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

/** makes sure capabilities are created on AWT event dispatch thread */
public class CapabilitiesCreator {

	static boolean created = false;
	static GLCapabilities glcapabilities = null;
	
	public static void create() {
		if(!created){
			synchronized(CapabilitiesCreator.class) {
				if(!created){
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							GLCapabilities glcap = GL.createCapabilities();
							created = true;
							glcapabilities = glcap;
						}
					};
					Utils.execOnAWTEventDispatch(runnable);
				}
			}
		}
	}
	
	
	
}
