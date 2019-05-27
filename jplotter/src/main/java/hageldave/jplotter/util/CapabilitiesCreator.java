package hageldave.jplotter.util;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.awt.AWTGLCanvas;

import hageldave.jplotter.FBOCanvas;

/**
 * This class provides the {@link #create()} method that makes sure that 
 * {@link GLCapabilities} are created on the AWT event dispatch thread.
 * It also makes sure that this happens mutually exclusive and only once.
 * Once the {@link #created} flag is set to true, this method returns 
 * right away.
 * This method is called by the {@link FBOCanvas#initGL()} method which
 * is automatically triggered on the first {@link AWTGLCanvas#render()}
 * pass.
 * In practice it is unlikely that this method has to be called from elsewhere.
 * The created GLCapabilities are stored in the static attribute {@link #glcapabilities}.
 * 
 * @author hageldave
 */
public class CapabilitiesCreator {

	public static boolean created = false;
	public static GLCapabilities glcapabilities = null;
	
	/**
	 * Creates the GLCapabilities mutually exclusive on the AWT event dispatch thread.
	 * This method sets the {@link #created} flasg to true, and will thus only create
	 * capabilities once.
	 */
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
