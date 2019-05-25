package jplotter.util;

import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import hageldave.imagingkit.core.Img;
import jplotter.Annotations.GLContextRequired;

/**
 * Utility class containing methods for Open GL specific tasks.
 * @author hageldave
 */
public class GLUtils {

	/**
	 * Creates an empty 2D texture. The texture has mipmap level 0.
	 * @param width
	 * @param height
	 * @param internalformat
	 * @param format
	 * @param filter the filter for GL_TEXTURE_MIN_FILTER and GL_TEXTURE_MAG_FILTER e.g. GL11.GL_LINEAR
	 * @param wrap the wrapping for GL_TEXTURE_WRAP_S and GL_TEXTURE_WRAP_T e.g. GL12.GL_CLAMP_TO_EDGE
	 * @return GL object name of the created texture (texture id)
	 */
	@GLContextRequired
	public static int create2DTexture(int width, int height, int internalformat, int format, int filter, int wrap) {
		int texid = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texid);
		{
			GL11.glTexImage2D(
					GL11.GL_TEXTURE_2D, // target
					0, // level,
					internalformat, 
					width, 
					height, 
					0, // border, 
					format, 
					GL11.GL_BYTE, // type 
					0 // data pointer
			);
			// default parameter setting
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
		}
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		return texid;
	}
	
	/**
	 * Creates a multisampled 2D texture with fixed sample locations. 
	 * @param width
	 * @param height
	 * @param internalformat
	 * @param numSamples
	 * @return GL object name of the created texture (texture id)
	 */
	@GLContextRequired
	public static int create2DTextureMultisample(int width, int height, int internalformat, int numSamples) {
		int texid = GL11.glGenTextures();
		GL11.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texid);
		{
			GL32.glTexImage2DMultisample(
					GL32.GL_TEXTURE_2D_MULTISAMPLE, 
					numSamples, 
					internalformat, 
					width, 
					height, 
					true
			);
		}
		GL11.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0);
		return texid;
	}
	
	/**
	 * Creates a 2D texture from the specified image with internal format
	 * GL_RGBA8.
	 * @param img from which texture is created
	 * @param filter the filter for GL_TEXTURE_MIN_FILTER and GL_TEXTURE_MAG_FILTER e.g. GL11.GL_LINEAR
	 * @param wrap the wrapping for GL_TEXTURE_WRAP_S and GL_TEXTURE_WRAP_T e.g. GL12.GL_CLAMP_TO_EDGE
	 * @return GL object name of the created texture (texture id)
	 */
	@GLContextRequired
	public static int create2DTexture(Img img, int filter, int wrap) {
		int format=GL11.GL_RGBA, internalformat=GL11.GL_RGBA8;
		int texid = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texid);
		{
			GL11.glTexImage2D(
					GL11.GL_TEXTURE_2D, // target
					0, // level,
					internalformat, 
					img.getWidth(), 
					img.getHeight(), 
					0, // border, 
					format, 
					GL11.GL_UNSIGNED_BYTE, // type 
					img.getData() // data pointer
			);
			// default parameter setting
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
		}
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		return texid;
	}
	
	/**
	 * Calls {@link GL30#glCheckFramebufferStatus(int)} and translates
	 * the returned status code into the corresponding constant's name.
	 * @return empty string if {@code GL_FRAMEBUFFER_COMPLETE}, else the 
	 * name of the status e.g. "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT".
	 */
	@GLContextRequired
	public static String checkFBOstatus() {
		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		switch (status) {
		case GL30.GL_FRAMEBUFFER_COMPLETE: {
			return "";
		}
		case GL30.GL_FRAMEBUFFER_UNDEFINED: {
			return "GL_FRAMEBUFFER_UNDEFINED";
		}
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT: {
			return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
		}
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: {
			return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
		}
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER: {
			return "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
		}
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER: {
			return "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
		}
		case GL30.GL_FRAMEBUFFER_UNSUPPORTED: {
			return "GL_FRAMEBUFFER_UNSUPPORTED";
		}
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE: {
			return "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
		}
		case GL32.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS: {
			return "GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS";
		}
		case 0: {
			return "ERROR";
		}
		default:
			return "STATUS_CODE:"+status;
		}
	}
	
	/**
	 * Creates (or fills) an array that contains an orthographic
	 * projection matrix in column major order.
	 * @param buffer null or array of length 16
	 * @param left 
	 * @param right
	 * @param bottom
	 * @param top
	 * @return the specified array or a new array if null was specified.
	 */
	public static float[] orthoMX(float[] buffer, float left, float right, float bottom, float top) {
		if(Objects.isNull(buffer)){
			buffer = new float[16];
		}
		int i=0;
		
		buffer[i++]= 2.0f / (right - left);
		buffer[i++]= 0;
		buffer[i++]= 0;
		buffer[i++]= 0;
			
		buffer[i++]= 0;
		buffer[i++]= 2.0f / (top - bottom);
		buffer[i++]= 0;
		buffer[i++]= 0;
			
		buffer[i++]= 0;
		buffer[i++]= 0;
		buffer[i++]= -1;
		buffer[i++]= 0;
			
		buffer[i++]= -(right + left) / (right - left);
		buffer[i++]= -(top + bottom) / (top - bottom);
		buffer[i++]= 0;
		buffer[i++]= 1;
		return buffer;
	}
	
	/**
	 * Performs glReadPixels for pixel at specified position
	 * @param attachment one of GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_DEPTH_ATTACHMENT
	 * @return color in integer packed 8bit per channel ARGB format (blue on least significant 8 bytes)
	 */
	@GLContextRequired
	public static int fetchPixel(int fboID, int attachment, int x, int y){
		int pixel = 0;
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fboID);
		{
			GL11.glReadBuffer(attachment);
			int[] rgba_bytes = new int[1];
			GL11.glReadPixels(x, y, 1, 1, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, rgba_bytes);
			pixel = rgba_bytes[0];
		}
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
		return pixel;
	}
	
	
	public static class GLRuntimeException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public GLRuntimeException() {
			super();
		}

		public GLRuntimeException(String message, Throwable cause) {
			super(message, cause);
		}

		public GLRuntimeException(String message) {
			super(message);
		}

		public GLRuntimeException(Throwable cause) {
			super(cause);
		}
		
	}
	
	@GLContextRequired
	public static boolean canMultisample2X(){
		return 
				GL11.glGetInteger(GL32.GL_MAX_COLOR_TEXTURE_SAMPLES) >= 2
				&&
				GL11.glGetInteger(GL32.GL_MAX_COLOR_TEXTURE_SAMPLES) >= 2;
	}
	
	@GLContextRequired
	public static boolean canMultisample4X(){
		return 
				GL11.glGetInteger(GL32.GL_MAX_COLOR_TEXTURE_SAMPLES) >= 4
				&&
				GL11.glGetInteger(GL32.GL_MAX_COLOR_TEXTURE_SAMPLES) >= 4;
	}
	
}
