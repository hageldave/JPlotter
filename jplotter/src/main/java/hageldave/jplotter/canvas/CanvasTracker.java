package hageldave.jplotter.canvas;

import java.util.HashMap;

/**
 * This class is a singleton that is used to keep track of the currently active (rendering) {@link FBOCanvas}.
 * Usually renderers do not need to know which GL context or canvas they are working with, and will simply
 * call static GL routines.
 * However, in some cases it may be required to know what GL context is currently active or which FBOCanvas
 * is rendering, e.g. for managing GL resources like textures, or setting viewport according to the FBO scaling.
 * <p>
 * The {@link FBOCanvas} will register with the CanvasTracker when starting rendering, and signoff when rendering
 * terminates.
 * 
 * @author hageldave
 */
public class CanvasTracker {
	
	public static final CanvasTracker INSTANCE = new CanvasTracker();
	
	/**
	 * Get the CanvasTracker.
	 * @return the CanvasTracker
	 */
	public static CanvasTracker getInstance() {return INSTANCE;}

	private HashMap<Thread, FBOCanvas> thread2currentlyRendering = new HashMap<>();
	
	private CanvasTracker() {}
	
	/**
	 * Tell the tracker that the specified canvas is now rendering.
	 * @param canvas that currently renders.
	 */
	public void registerCurrentlyRenderingCanvas(FBOCanvas canvas) {
		synchronized (thread2currentlyRendering) {
			Thread thread = Thread.currentThread();
			FBOCanvas cnvs = thread2currentlyRendering.get(thread);
			if(cnvs != null && cnvs != canvas)
				System.err.printf("%s: Another canvas is currently registered as rendering on this thread (%d). It must have forgotten to report that it is done rendering.%n", 
						this.getClass().getCanonicalName(), 
						thread.getId()
				);
			thread2currentlyRendering.put(thread, canvas);
		}
	}
	
	/**
	 * Get the {@link FBOCanvas} that is rendering on the current thread.
	 * @return currently rendering FBOCanvas or null if none is rendering at the moment.  
	 */
	public FBOCanvas getCurrentlyRenderingCanvas() {
		synchronized (thread2currentlyRendering) {
			return thread2currentlyRendering.get(Thread.currentThread());
		}
	}
	
	/**
	 * Tell the tracker that the specified canvas has finished rendering.
	 * @param canvas that finished rendering.
	 */
	public void signoffCurrentlyRenderingCanvas(FBOCanvas canvas) {
		synchronized (thread2currentlyRendering) {
			Thread thread = Thread.currentThread();
			FBOCanvas cnvs = thread2currentlyRendering.get(thread);
			if(cnvs != canvas)
				System.err.printf("%s: Another (or no) canvas is currently registered as rendering on this thread (%d). Maybe the provided canvas did forget to register in the first place.%n", 
						this.getClass().getCanonicalName(), 
						thread.getId()
				);
			thread2currentlyRendering.remove(thread);
		}
	}
	
}
