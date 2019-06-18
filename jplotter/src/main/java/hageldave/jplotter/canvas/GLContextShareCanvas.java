package hageldave.jplotter.canvas;

import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

public final class GLContextShareCanvas {
	
	private static AWTGLCanvas INSTANCE;
	
	private static GLData SHARED_GL_DATA;
	
	public static boolean isSet(){
		return INSTANCE != null;
	}
	
	public static void set(AWTGLCanvas canvas){
		INSTANCE = canvas;
		SHARED_GL_DATA = new GLData();
		SHARED_GL_DATA.shareContext = INSTANCE;
	}
	
	public static AWTGLCanvas getInstance() {
		return INSTANCE;
	}
	
	public static GLData getGLData(){
		return SHARED_GL_DATA;
	}
	
}
