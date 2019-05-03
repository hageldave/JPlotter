package jplotter.util;

import java.util.Arrays;
import java.util.Objects;

public class GenericKey {

	Object[] keycontents;
	
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
	
}
