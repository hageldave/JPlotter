package jplotter.renderers;

import java.util.Objects;

import org.joml.Matrix3fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import jplotter.globjects.Shader;
import jplotter.renderables.Points;

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
			+ NL + "uniform mat2 modelMX;"
			+ NL + "uniform float globalScaling;"
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
			+ NL + "   gl_Position = projMX*viewMX*vec4(globalScaling*(modelMX*scaleMX*rotMX*in_position)+in_pointpos, 1,1);"
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
			+ NL + "uniform float alphaMultiplier;"
			+ NL + "void main() {"
			+ NL + "   frag_color = vec4(vColor.rgb, vColor.a*alphaMultiplier);"
			+ NL + "   pick_color = vPickColor;"
			+ NL + "}"
			+ NL
			;

	float[] viewmxarray = new float[16];
	float[] modelmxarray = new float[]{1,0,0,1};
	float glyphScaling = 1f;

	@Override
	public void glInit() {
		if(Objects.isNull(shader)){
			this.shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
			itemsToRender.forEach(Points::initGL);
		}
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
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
		GL20.glUniformMatrix2fv(loc, false, modelmxarray);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "globalScaling");
		GL20.glUniform1f(loc, this.glyphScaling * item.glyph.pixelSize() * item.getGlobalScaling());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, this.glyphScaling * item.glyph.pixelSize() * item.getGlobalAlphaMultiplier());
		// draw things
		item.bindVertexArray();
		if(item.glyph.useElementsDrawCall()){
			GL31.glDrawElementsInstanced(item.glyph.primitiveType(), item.glyph.numVertices(), GL11.GL_UNSIGNED_INT, 0, item.numPoints());
		} else {
			GL31.glDrawArraysInstanced(item.glyph.primitiveType(), 0, item.glyph.numVertices(), item.numPoints());
		}
		item.releaseVertexArray();
	}

	@Override
	protected void renderEnd() {
		GL11.glDisable(GL12.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}
	
	@Override
	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx) {
		super.setViewMX(viewmx, scalemx, transmx);
		this.modelmxarray[0] = 1f/scalemx.m00();
		this.modelmxarray[1] = this.modelmxarray[2] = 0;
		this.modelmxarray[3] = 1f/scalemx.m11();
	}

}
