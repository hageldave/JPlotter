package jplotter.globjects;

import static org.lwjgl.opengl.GL11.GL_DOUBLE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_INT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;

public class VertexArray implements AutoCloseable {

	int va; // vertex array object
	int ibo; // indices (optional element array buffer)
	int[] vbos; // vertex buffer objects
	// for debugging
	int dims[];
	int numValues[];
	int numAttributes;
	int numIndices;

	public VertexArray(int n) {
		this.numAttributes = n;
		this.va = glGenVertexArrays();
		this.vbos = new int[n];
		this.dims = new int[n];
		this.numValues = new int[n];
		glBindVertexArray(va);
		glBindVertexArray(0);
	}
	
	public VertexArray setBuffer(int i, int dim, float ... buffercontent){
		glBindVertexArray(va);
		{
			if(vbos[i] == 0){
				vbos[i] = glGenBuffers();
			}
			glBindBuffer(GL_ARRAY_BUFFER, vbos[i]);
			{
				// put vertices into vbo
				glBufferData(GL_ARRAY_BUFFER, buffercontent, GL_STATIC_DRAW);
				// put vbo into va
				glVertexAttribPointer(i, dim, GL_FLOAT, false, 0, 0);
			}
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		glBindVertexArray(0);
		dims[i] = dim;
		numValues[i] = buffercontent.length;
		return this;
	}
	
	public VertexArray setBuffer(int i, int dim, boolean signed, int ... buffercontent){
		glBindVertexArray(va);
		{
			if(vbos[i] == 0){
				vbos[i] = glGenBuffers();
			}
			glBindBuffer(GL_ARRAY_BUFFER, vbos[i]);
			{
				// put vertices into vbo
				glBufferData(GL_ARRAY_BUFFER, buffercontent, GL_STATIC_DRAW);
				// put vbo into va
				glVertexAttribIPointer(i, 1, signed ? GL_INT:GL_UNSIGNED_INT, 0, 0);
			}
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		glBindVertexArray(0);
		dims[i] = dim;
		numValues[i] = buffercontent.length;
		return this;
	}
	
	public VertexArray setBuffer(int i, int dim, double ... buffercontent){
		glBindVertexArray(va);
		{
			if(vbos[i] == 0){
				vbos[i] = glGenBuffers();
			}
			glBindBuffer(GL_ARRAY_BUFFER, vbos[i]);
			{
				// put vertices into vbo
				glBufferData(GL_ARRAY_BUFFER, buffercontent, GL_STATIC_DRAW);
				// put vbo into va
				glVertexAttribPointer(i, dim, GL_DOUBLE, false, 0, 0);
			}
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		glBindVertexArray(0);
		dims[i] = dim;
		numValues[i] = buffercontent.length;
		return this;
	}
	
	public VertexArray setIndices(int... indices){
		if(ibo == 0){
			ibo = glGenBuffers();
		}
		numIndices = indices.length;
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		return this;
	}

	public void bindAndEnableAttributes(int ... is) {
		glBindVertexArray(va);
		for(int i: is){
			glEnableVertexAttribArray(i);
		}
		if(ibo != 0){
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		}
	}
	
	public void unbindAndDisableAttributes(int ... is){
		for(int i: is){
			glDisableVertexAttribArray(i);
		}
		glBindVertexArray(0);
		if(ibo != 0){
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		}
	}
	
	public int getNumIndices() {
		return numIndices;
	}
	
	@Override
	public void close() {
		for(int i = 0; i < vbos.length; i++){
			glDeleteBuffers(vbos[i]);
			vbos[i] = 0;
		}
		glDeleteBuffers(ibo);
		glDeleteVertexArrays(va);
		ibo = va = 0;
	}
	

}
