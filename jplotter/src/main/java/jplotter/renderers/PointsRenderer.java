package jplotter.renderers;

import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import jplotter.globjects.Points;
import jplotter.globjects.Shader;

public class PointsRenderer extends GenericRenderer<Points> {
	
	private static final char NL = '\n';
	static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in vec2 in_pointpos;"
			+ NL + "layout(location = 2) in vec2 in_rotAndScale;"
			+ NL + "layout(location = 3) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform mat4 viewMX;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"
			
			+ NL + "mat2 rotationMatrix(float angle){"
			+ NL + "   float s = sin(angle), c = cos(angle);"
			+ NL + "   return mat2(c,s,-s,c);"
			+ NL + "}"
			
			+ NL + "mat2 scalingMatrix(float s){"
			+ NL + "   return mat2(s,0,0,s);"
			+ NL + "}"
			
			+ NL + "void main() {"
			+ NL + "   mat2 rotMX = rotationMatrix(in_rotAndScale.x);"
			+ NL + "   mat2 scaleMX = scalingMatrix(in_rotAndScale.y);"
			+ NL + "   gl_Position = projMX*viewMX*vec4((scaleMX*rotMX*in_position)+in_pointpos, 1,1);"
			+ NL + "   vColor = unpackARGB(in_colors.x);"
			+ NL + "   vPickColor = unpackARGB(in_colors.y);"
			+ NL + "}"
			+ NL
			;
	static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "in vec4 vColor;"
			+ NL + "in vec4 vPickColor;"
			+ NL + "void main() {"
			+ NL + "   frag_color = vColor;"
			+ NL + "   pick_color = vPickColor;"
			+ NL + "}"
			+ NL
			;
	
	float[] viewmxarray = new float[16];

	@Override
	public void glInit() {
		this.shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
	}

	@Override
	public void close() {
		if(Objects.nonNull(shader)){
			shader.close();
		}
		deleteAllItems();
	}

	@Override
	protected void renderStart(int w, int h) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL12.GL_BLEND);
		GL12.glBlendFunc(GL12.GL_SRC_ALPHA, GL12.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	protected void renderItem(Points item) {
		if(item.numPoints() < 1){
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
		GL20.glUniformMatrix4fv(loc, false, viewMX.get(viewmxarray));
		// draw things
		item.bindVertexArray();
		GL31.glDrawElementsInstanced(item.getGlyph().primitiveType(), item.getGlyph().numVertices(), GL11.GL_UNSIGNED_INT, 0, item.numPoints());
		item.releaseVertexArray();
	}

	@Override
	protected void renderEnd() {
		GL11.glDisable(GL12.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

}
