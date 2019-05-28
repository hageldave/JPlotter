package hageldave.jplotter.renderers;

import java.util.Objects;

import org.joml.Matrix3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.globjects.Shader;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Renderable;

/**
 * The LinesRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Lines}.
 * This renderer uses a geometry shader that extends a line primitive
 * into a quad of width that corresponds to the line width of the Lines
 * object.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class LinesRenderer extends GenericRenderer<Lines> {

	protected static final char NL = '\n';
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in uint in_color;"
			+ NL + "uniform mat4 viewMX;"
			+ NL + "uniform mat3 modelMX;"
			+ NL + "out vec4 vcolor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"
			
			+ NL + "void main() {"
			+ NL + "   gl_Position = viewMX*vec4(modelMX*vec3(in_position,1),1);"
			+ NL + "   vcolor = unpackARGB(in_color);"
			+ NL + "}"
			+ NL
			;
	protected static final String geometryShaderSrc = ""
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
	protected static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "uniform vec4 pickColorToUse;"
			+ NL + "in vec4 gcolor;"
			+ NL + "void main() {"
			+ NL + "   frag_color = gcolor;"
			+ NL + "   pick_color = pickColorToUse;"
			+ NL + "}"
			+ NL
			;
	
	
	protected Matrix3f modelMX = new Matrix3f();
	protected float[] viewmxarray = new float[16];
	protected float[] modelmxarray = new float[9];
	
	
	/**
	 * Creates the shader if not already created and 
	 * calls {@link Renderable#initGL()} for all items 
	 * already contained in this renderer.
	 * Items that are added later on will be initialized during rendering.
	 */
	@Override
	@GLContextRequired
	public void glInit() {
		if(Objects.isNull(shader)){
			shader = new Shader(vertexShaderSrc, geometryShaderSrc, fragmentShaderSrc);
			itemsToRender.forEach(Renderable::initGL);
		}
	}
	
	/**
	 * Disables {@link GL11#GL_DEPTH_TEST},
	 * enables {@link GL11#GL_BLEND}
	 * and sets {@link GL11#GL_SRC_ALPHA}, {@link GL11#GL_ONE_MINUS_SRC_ALPHA}
	 * as blend function.
	 */
	@Override
	@GLContextRequired
	protected void renderStart(int w, int h) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	@GLContextRequired
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

	/**
	 * disables {@link GL11#GL_BLEND},
	 * enables {@link GL11#GL_DEPTH_TEST}
	 */
	@Override
	@GLContextRequired
	protected void renderEnd() {
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	/**
	 * Disposes of GL resources, i.e. closes the shader.
	 * It also deletes (closes) all {@link Lines} contained in this
	 * renderer.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(shader))
			shader.close();
		deleteAllItems();
	}
	
}
