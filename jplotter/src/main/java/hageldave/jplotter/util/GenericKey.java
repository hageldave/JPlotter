package hageldave.jplotter.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import hageldave.jplotter.font.CharacterAtlas;

/**
 * The GenericKey class can be used as key in {@link HashMap}s when
 * multiple objects should be combined to a single key.
 * The hash key is then generated using {@link Arrays#hashCode(Object[])}
 * on the objects combined in this key.
 * <br>
 * This is for example used by the atlas collection in {@link CharacterAtlas}
 * where the attributes fontSize, style and antialiasing are used to
 * lookup the correct atlas.
 * 
 * @author hageldave
 */
public class GenericKey {

	private Object[] keycontents;
	
	/**
	 * Creates a new key from the specified objects
	 * @param objects the objects to form a key from (ordering changes hash)
	 */
	public GenericKey(Object ...objects) {
		this.keycontents = objects;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(Objects.isNull(obj) || ! (obj instanceof GenericKey))
			return false;
		GenericKey other = (GenericKey) obj;
		return Arrays.equals(keycontents, other.keycontents);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(keycontents);
	}
	
	/**
	 * @return the number of components this key consists of
	 */
	public int numComponents(){
		return keycontents.length;
	}
	
	/**
	 * Returns the component at specified index
	 * @param i index
	 * @return key component
	 */
	public Object getComponent(int i){
		return keycontents[i];
	}
	
	/**
	 * Returns the component at specified index
	 * casted to the specified type
	 * @param i index
	 * @param clazz type to cast to
	 * @return key component
	 * @param <T> type of the component
	 */
	public <T> T getComponent(int i, Class<T> clazz){
		return clazz.cast(keycontents[i]);
	}
	
}
