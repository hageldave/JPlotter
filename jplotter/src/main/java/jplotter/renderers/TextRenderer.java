package jplotter.renderers;

import java.util.LinkedList;
import java.util.Objects;

import org.joml.Matrix3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import jplotter.globjects.Shader;
import jplotter.globjects.Text;

public class TextRenderer extends GenericRenderer<Text> {

	private static final char NL = '\n';
	static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in vec2 in_texcoords;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform mat4 viewMX;"
			+ NL + "uniform mat3 modelMX;"
			+ NL + "out vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   gl_Position = projMX*viewMX*vec4(modelMX*vec3(in_position,1),1);"
			+ NL + "   tex_Coords = in_texcoords;"
			+ NL + "}"
			+ NL
			;
	static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "uniform sampler2D tex;"
			+ NL + "uniform vec4 fragColorToUse;"
			+ NL + "uniform vec4 pickColorToUse;"
			+ NL + "in vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   vec4 texColor = texture(tex, tex_Coords);"
			+ NL + "   frag_color = fragColorToUse*texColor;"
			+ NL + "   pick_color = pickColorToUse;"
			+ NL + "}"
			;
	
	protected Matrix3f modelMX;
	LinkedList<Text> textsToRender = new LinkedList<>();
	
	float[] viewmxarray = new float[16];
	float[] modelmxarray = new float[9];
	
	
	@Override
	public void glInit() {
		shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
		modelMX = new Matrix3f();
		textsToRender.forEach(Text::initGL);
	}
	
	@Override
	protected void renderStart(int w, int h) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL12.GL_BLEND);
		GL12.glBlendFunc(GL12.GL_SRC_ALPHA, GL12.GL_ONE_MINUS_SRC_ALPHA);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}

	@Override
	protected void renderItem(Text txt) {
		int loc;
		txt.bindVertexArray();
		GL13.glBindTexture(GL11.GL_TEXTURE_2D, txt.getTextureID());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "tex");
		GL20.glUniform1i(loc, 0);
		// set projection matrix in shader
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
		GL20.glUniformMatrix4fv(loc, false, viewMX.get(viewmxarray));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
		modelMX.setColumn(2, txt.getOrigin().x, txt.getOrigin().y, 0);
		GL20.glUniformMatrix3fv(loc, false, modelMX.get(modelmxarray));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "fragColorToUse");
		GL20.glUniform4f(loc, txt.getColorR(), txt.getColorG(), txt.getColorB(), txt.getColorA());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "pickColorToUse");
		GL20.glUniform4f(loc, txt.getPickColorR(), txt.getPickColorG(), txt.getPickColorB(), 1f);
		// draw things
		GL11.glDrawElements(GL11.GL_TRIANGLES, txt.getVertexArray().getNumIndices(), GL11.GL_UNSIGNED_INT, 0);
		txt.releaseVertexArray();
	}
	
	@Override
	protected void renderEnd() {
		GL13.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL11.glDisable(GL12.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		
	}

	@Override
	public void close() {
		if(Objects.nonNull(shader))
			shader.close();
		deleteAllItems();
	}
	
	@Override
	public boolean drawsPicking() {
		return true;
	}


	
}
