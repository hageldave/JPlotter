package hageldave.jplotter.util;

import java.util.HashMap;
import java.util.function.Supplier;

import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.renderers.LinesRenderer;

public class ShaderRegistry {

	static final HashMap<Integer,HashMap<String, Pair<Shader,int[]>>> context2label2shader = new HashMap<>();
	static final HashMap<Shader, Pair<Integer, String>> shader2contextAndLabel = new HashMap<>();
	
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
