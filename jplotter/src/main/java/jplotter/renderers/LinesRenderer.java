package jplotter.renderers;

import java.util.Objects;

import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

import jplotter.globjects.Lines;
import jplotter.globjects.Shader;
import jplotter.util.GLUtils;

public class LinesRenderer extends GenericRenderer<Lines> {

	private static final char NL = '\n';
	static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in vec4 in_color;"
			+ NL + "uniform mat4 viewMX;"
			+ NL + "uniform mat3 modelMX;"
			+ NL + "out vec4 vcolor;"
			+ NL + "void main() {"
			+ NL + "   gl_Position = viewMX*vec4(modelMX*vec3(in_position,1),1);"
			+ NL + "   vcolor = in_color;"
			+ NL + "}"
			+ NL
			;
	static final String geometryShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(lines) in;"
			+ NL + "layout(triangle_strip,max_vertices=4) out;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform float linewidth;"
			+ NL + "in vec4 vcolor[];"
			+ NL + "out vec4 gcolor;"
			+ NL + "void main() {"
			+ NL + "   vec2 p1 = gl_in[0].gl_Position.xy;"
			+ NL + "   vec2 p2 = gl_in[1].gl_Position.xy;"
			+ NL + "   vec2 dir = p1-p2;"
			+ NL + "   vec2 miterDir = normalize(vec2(dir.y, -dir.x))*0.5*linewidth;"
			+ NL + "   vec2 p;"
			
			+ NL + "   p = p1+miterDir;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[0];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   p = p1-miterDir;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[0];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   p = p2+miterDir;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[1];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   p = p2-miterDir;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[1];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   EndPrimitive();"
			+ NL + "}"
			+ NL
			;
	static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "uniform vec4 pickColorToUse;"
			+ NL + "in vec4 gcolor;"
			+ NL + "in vec2 param;"
			+ NL + "void main() {"
			+ NL + "   frag_color = gcolor;"
			+ NL + "   pick_color = pickColorToUse;"
			+ NL + "}"
			+ NL
			;
	
	float[] orthoMX = GLUtils.orthoMX(0, 1, 0, 1);
	Matrix3f modelMX;
	Matrix4f viewMX;
	
	float[] viewmxarray = new float[16];
	float[] modelmxarray = new float[9];
	
	
	@Override
	public void glInit() {
		shader = new Shader(vertexShaderSrc, geometryShaderSrc, fragmentShaderSrc);
		modelMX = new Matrix3f();
		viewMX = new Matrix4f();
		itemsToRender.forEach(Lines::initGL);
	}
	
	@Override
	protected void renderStart(int w, int h) {
		orthoMX = GLUtils.orthoMX(0, w, 0, h);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL12.GL_BLEND);
		GL12.glBlendFunc(GL12.GL_SRC_ALPHA, GL12.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	protected void renderItem(Lines lines) {
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "linewidth");
		GL20.glUniform1f(loc, lines.getThickness());
		// set projection matrix in shader
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
		GL20.glUniformMatrix4fv(loc, false, viewMX.get(viewmxarray));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
		GL20.glUniformMatrix3fv(loc, false, modelMX.get(modelmxarray));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "pickColorToUse");
		GL20.glUniform4f(loc, lines.getPickColorR(), lines.getPickColorG(), lines.getPickColorB(), 1f);
		// draw things
		lines.bindVertexArray();
		GL11.glDrawArrays(GL11.GL_LINES, 0, lines.numSegments()*2);
		lines.releaseVertexArray();
	}

	@Override
	protected void renderEnd() {
		GL11.glLineWidth(1f);
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
	
	public void setViewMX(Matrix3fc m){
		this.viewMX.set(m);
	}
	
}
