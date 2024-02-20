package hageldave.jplotter.canvas;

import java.util.HashMap;

public class CanvasTracker {
	
	public static final CanvasTracker INSTANCE = new CanvasTracker();
	
	public static CanvasTracker getInstance() {return INSTANCE;}

	private HashMap<Long, FBOCanvas> thread2currentlyRendering = new HashMap<>();
	
	private CanvasTracker() {}
	
	public void registerCurrentlyRenderingCanvas(FBOCanvas canvas) {
		synchronized (thread2currentlyRendering) {
			long threadID = Thread.currentThread().getId();
			FBOCanvas cnvs = thread2currentlyRendering.get(threadID);
			if(cnvs != null && cnvs != canvas)
				System.err.printf("%s: Another canvas is currently registered as rendering on this thread (%d). It must have forgotten to report that it is done rendering.%n", 
						this.getClass().getCanonicalName(), 
						threadID
				);
			thread2currentlyRendering.put(threadID, canvas);
		}
	}
	
	public FBOCanvas getCurrentlyRenderingCanvas() {
		synchronized (thread2currentlyRendering) {
			return thread2currentlyRendering.get(Thread.currentThread().getId());
		}
	}
	
	public void signoffCurrentlyRenderingCanvas(FBOCanvas canvas) {
		synchronized (thread2currentlyRendering) {
			long threadID = Thread.currentThread().getId();
			FBOCanvas cnvs = thread2currentlyRendering.get(threadID);
			if(cnvs != canvas)
				System.err.printf("%s: Another (or no) canvas is currently registered as rendering on this thread (%d). Maybe the provided canvas did forget to register in the first place.%n", 
						this.getClass().getCanonicalName(), 
						threadID
				);
			thread2currentlyRendering.put(threadID, null);
		}
	}
	
}
