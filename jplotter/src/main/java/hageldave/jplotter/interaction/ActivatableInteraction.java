package hageldave.jplotter.interaction;

/**
 * This interface provides the method {@link #isInteractionActive()} to check if an interaction is currently active.
 * Being active means that the interaction should respond to user input (e.g. process mouse events) 
 * and perform its intended functionality.
 * This can be used to enable or disable certain interactions based on specific conditions, 
 * such as key presses or other criteria defined in the implementation.
 */
public interface ActivatableInteraction {

	boolean isInteractionActive();
	
}
