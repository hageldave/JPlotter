package jplotter.renderers;

import java.awt.Font;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import jplotter.globjects.CharacterAtlas;
import jplotter.globjects.Shader;
import jplotter.globjects.VertexArray;
import jplotter.util.GLUtils;

public class TextRenderer implements Renderer {

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
			+ NL + "   gl_Position = projMX*viewMX*vec4(modelMX*vec3(in_position,0),1);"
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
			+ NL + "uniform vec3 pickColorToUse;"
			+ NL + "in vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   vec4 texColor = texture(tex, tex_Coords);"
			+ NL + "   frag_color = fragColorToUse*texColor;"
			+ NL + "   pick_color = vec4(pickColorToUse,1);"
			+ NL + "}"
			;
	
	
	CharacterAtlas atlas;
	VertexArray va;
	VertexArray vertexArray;
	Shader shader;
	float[] orthoMX = GLUtils.orthoMX(0, 1, 0, 1);
	Matrix3f modelMX;
	Matrix4f viewMX;
	Vector4f color;
	
	
	@Override
	public void glInit() {
		atlas = new CharacterAtlas(12, Font.PLAIN, true);
		String s = "sweet";
		va = atlas.createVAforString(s);
		shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
		modelMX = new Matrix3f(new Vector3f(1, 0, 0),new Vector3f(0,1,0), new Vector3f(0, 0, 1));
		viewMX = new Matrix4f();
		color = new Vector4f();
	}

	@Override
	public void render(int w, int h) {
		orthoMX = GLUtils.orthoMX(0, w, 0, h);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL12.GL_BLEND);
		GL12.glBlendFunc(GL12.GL_SRC_ALPHA, GL12.GL_ONE_MINUS_SRC_ALPHA);
		
		shader.bind();
		{
			va.bindAndEnableAttributes(0,1);
			int loc;
			// set texture in shader
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL13.glBindTexture(GL11.GL_TEXTURE_2D, atlas.getTexID());
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "tex");
			GL20.glUniform1i(loc, 0);
			// set projection matrix in shader
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
			GL20.glUniformMatrix4fv(loc, false, orthoMX);
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
			GL20.glUniformMatrix4fv(loc, false, viewMX.get(new float[16]));
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
			GL20.glUniformMatrix3fv(loc, false, modelMX.get(new float[9]));
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "fragColorToUse");
			GL20.glUniform4f(loc, color.x, color.y, color.z, color.w);
			// draw things
			GL11.glDrawElements(GL11.GL_TRIANGLES, va.getNumIndices(), GL11.GL_UNSIGNED_INT, 0);
			// done
			va.unbindAndDisableAttributes(0,1);
		}
		shader.unbind();
		
		GL11.glDisable(GL12.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	@Override
	public void close() {
		atlas.close();
		va.close();
		shader.close();
	}

}
