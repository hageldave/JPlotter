package jplotter.util;

import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import hageldave.imagingkit.core.Img;

public class GLUtils {

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
	
	public static float[] orthoMX(float[] buffer, float left, float right, float bottom, float top) {
//		detail::tmat4x4<valType> Result(1);
//		Result[0][0] = valType(2) / (right - left);
//		Result[1][1] = valType(2) / (top - bottom);
//		Result[2][2] = - valType(1);
//		Result[3][0] = - (right + left) / (right - left);
//		Result[3][1] = - (top + bottom) / (top - bottom);
//		return Result;
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
	
	public static float[] mx3fromRowMajor(
			float m00, float m01, float m02,
			float m10, float m11, float m12,
			float m20, float m21, float m22) 
	{
		return new float[]{m00,m10,m20, m01,m11,m21, m02,m12,m22};
	}
	
	/**
	 * performs glReadPixels for pixel at specified position
	 * @param attachment one of GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_DEPTH_ATTACHMENT
	 * @return color in integer packed 8bit per channel ARGB format (blue on least significant 8 bytes)
	 */
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
	
}
