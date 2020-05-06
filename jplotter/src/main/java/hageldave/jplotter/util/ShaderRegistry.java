package hageldave.jplotter.util;

import java.util.HashMap;
import java.util.function.Supplier;

import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The ShaderRegistry class is a statically accessed class for keeping track of {@link Shader}s.
 * To avoid the creation of duplicate shaders in the same GL context, this class can be used to
 * easily allocate or get shaders shared by different objects.
 * <p>
 * Shaders are identified by context (canvasID) and a label, and they are obtained through the
 * {@link #getOrCreateShader(String, Supplier)} method.
 * When the shader is no longer in use by the object it has to be handed back to this class
 * through {@link #handbackShader(Shader)} which will close it if no longer in use by any other object.
 * <p>
 * Each shader in the registry is reference counted to determine if a registered shader is in use or not.
 * {@link #getOrCreateShader(String, Supplier)} increments the reference count, {@link #handbackShader(Shader)}
 * decrements the reference count.
 * 
 * @author hageldave
 */
public final class ShaderRegistry {

	private static final HashMap<Integer,HashMap<String, Pair<Shader,int[]>>> context2label2shader = new HashMap<>();
	private static final HashMap<Shader, Pair<Integer, String>> shader2contextAndLabel = new HashMap<>();
	
	
	private ShaderRegistry(){/* statically accessed singleton */}
	
	/**
	 * Returns the desired shader.
	 * If a Shader with the provided label in the current GL context is already registered, 
	 * it will be returned and its reference count incremented.
	 * Otherwise a new shader will be allocated using the specified supplier and registered.
	 * 
	 * @param label of the shader
	 * @param shadermaker supplier (constructor/factory) of the shader in case it was not yet registered.
	 * @return shader corresponding to specified label and current context.
	 * 
	 * @throws IllegalStateException when no context is active (FBOCanvas.CURRENTLY_ACTIVE_CANVAS == 0)
	 */
	@GLContextRequired
	public static Shader getOrCreateShader(String label, Supplier<Shader> shadermaker){
		int canvasid = FBOCanvas.CURRENTLY_ACTIVE_CANVAS;
		if(canvasid == 0){
			throw new IllegalStateException(
					"No active FBOCanvas, the FBOCanvas.CURRENTLY_ACTIVE_CANVAS field was 0. " +
					"This indicates that there is likely no active GL context to execute GL methods in."
			);
		}
		
		HashMap<String, Pair<Shader,int[]>> label2shader = context2label2shader.get(canvasid);
		if(label2shader == null){
			label2shader = new HashMap<>();
			context2label2shader.put(canvasid, label2shader);
		}
		Pair<Shader, int[]> shaderref = label2shader.get(label);
		if(shaderref == null){
			shaderref = Pair.of(shadermaker.get(), new int[1]);
			label2shader.put(label, shaderref);
			shader2contextAndLabel.put(shaderref.first, Pair.of(canvasid, label));
		}
		// increment ref count
		shaderref.second[0]++;
		return shaderref.first;
	}
	
	/**
	 * Hands back the specified shader, signaling it is no longer in use by the caller.
	 * This decrements the reference count of the specified shader in the registry.
	 * When the reference count drops to 0, the shader is closed (destroyed).
	 * @param shader to be handed back.
	 */
	@GLContextRequired
	public static void handbackShader(Shader shader){
		int canvasid = FBOCanvas.CURRENTLY_ACTIVE_CANVAS;
		if(canvasid == 0){
			throw new IllegalStateException(
					"No active FBOCanvas, the FBOCanvas.CURRENTLY_ACTIVE_CANVAS field was 0. " +
					"This indicates that there is likely no active GL context to execute GL methods in."
			);
		}
		
		Pair<Integer, String> pair = shader2contextAndLabel.get(shader);
		if(pair == null)
			return;
		if(pair.first != canvasid){
			throw new IllegalStateException(
					"Canvas ID of shader and currently active canvas dont match." +
					"This means that the wrong context is active to delete the shader."
			);
		}
		HashMap<String, Pair<Shader, int[]>> label2shader = context2label2shader.get(pair.first);
		Pair<Shader, int[]> shaderref = label2shader.get(pair.second);
		if((--shaderref.second[0]) == 0){
			// destroy the shader
			shaderref.first.close();
			label2shader.remove(pair.second);
			shader2contextAndLabel.remove(shaderref.first);
		}
	}
	
	
}
