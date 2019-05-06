package jplotter.renderers;

import java.util.LinkedList;
import java.util.Objects;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import jplotter.globjects.Lines;
import jplotter.globjects.Shader;
import jplotter.util.GLUtils;

public class LinesRenderer implements Renderer {

	private static final char NL = '\n';
	static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in vec4 in_color;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform mat4 viewMX;"
			+ NL + "uniform mat3 modelMX;"
			+ NL + "out vec4 color;"
			+ NL + "void main() {"
			+ NL + "   gl_Position = projMX*viewMX*vec4(modelMX*vec3(in_position,1),1);"
			+ NL + "   color = in_color;"
			+ NL + "}"
			+ NL
			;
	static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "uniform vec4 pickColorToUse;"
			+ NL + "in vec4 color;"
			+ NL + "void main() {"
			+ NL + "   frag_color = color;"
			+ NL + "   pick_color = pickColorToUse;"
			+ NL + "}"
			;
	
	
	Shader shader;
	float[] orthoMX = GLUtils.orthoMX(0, 1, 0, 1);
	Matrix3f modelMX;
	Matrix4f viewMX;
	LinkedList<Lines> linesToRender = new LinkedList<>();
	
	float[] viewmxarray = new float[16];
	float[] modelmxarray = new float[9];
	
	
	@Override
	public void glInit() {
		shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
		modelMX = new Matrix3f();
		viewMX = new Matrix4f();
		linesToRender.forEach(Lines::initGL);
	}

	@Override
	public void render(int w, int h) {
		orthoMX = GLUtils.orthoMX(0, w, 0, h);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL12.GL_BLEND);
		GL12.glBlendFunc(GL12.GL_SRC_ALPHA, GL12.GL_ONE_MINUS_SRC_ALPHA);
		
		
		if(!linesToRender.isEmpty()){
			shader.bind();
			int loc;
			// set texture in shader
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			for(Lines lines: linesToRender){
				lines.initGL();
				if(lines.isDirty()){
					lines.updateVA();
				}
				GL11.glLineWidth(lines.getThickness());
				// set projection matrix in shader
				loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
				GL20.glUniformMatrix4fv(loc, false, orthoMX);
				loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
				GL20.glUniformMatrix4fv(loc, false, viewMX.get(viewmxarray));
				loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
				modelMX.setColumn(2, 0, 0, 0);
				GL20.glUniformMatrix3fv(loc, false, modelMX.get(modelmxarray));
				loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "pickColorToUse");
				GL20.glUniform4f(loc, lines.getPickColorR(), lines.getPickColorG(), lines.getPickColorB(), 1f);
				// draw things
				lines.bindVertexArray();
				GL11.glDrawArrays(GL11.GL_LINES, 0, lines.numSegments()*2);
				lines.releaseVertexArray();
			}
			// done
			GL11.glLineWidth(1f);
			shader.unbind();
		}
		
		GL11.glDisable(GL12.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	@Override
	public void close() {
		if(Objects.nonNull(shader))
			shader.close();
		deleteAllLines();
	}

	
	public LinkedList<Lines> getLinesToRender() {
		return linesToRender;
	}
	
	public void addLines(Lines lines){
		linesToRender.add(lines);
	}
	
	public boolean removeLines(Lines lines){
		return linesToRender.remove(lines);
	}
	
	public void deleteAllLines(){
		for(Lines lines:linesToRender)
			lines.close();
		linesToRender.clear();
	}
	
	@Override
	public boolean drawsPicking() {
		return true;
	}
	
}
