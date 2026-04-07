package hageldave.jplotter.util;

import java.util.ArrayList;
import java.util.function.ToDoubleFunction;

/**
 * QuadTree for fast rectangular (AABB) query of 2D points.
 * @param <T> type of entries stored in the tree, must be able to extract x and y coordinates 
 * via provided extractors.
 * This design is intended to be flexible and allow for lookups, e.g., tree storing index into
 * an external array holding the coordinates.
 */
public class QTree<T> {
	
	ToDoubleFunction<T> xExtr;
	ToDoubleFunction<T> yExtr;
	
	int capacity;
	ArrayList<T> entries;
	QTree<T> LL;
	QTree<T> LR;
	QTree<T> UL;
	QTree<T> UR;
	double xMin, xMax, yMin, yMax;
	boolean onlyDuplicatesContained = true;
	
	/**
	 * Creates a new QTree with the given parameters. 
	 * The tree will be initialized with the given bounds and capacity.
	 * @param xExtractor function to extract x coordinate from an entry
	 * @param yExtractor function to extract y coordinate from an entry
	 * @param xMin minimum x coordinate of the tree bounds
	 * @param xMax maximum x coordinate of the tree bounds
	 * @param yMin minimum y coordinate of the tree bounds
	 * @param yMax maximum y coordinate of the tree bounds
	 * @param capacity maximum number of entries in a leaf node before it is split into subnodes.
	 * Exception: if only duplicates are contained, the node will be allowed to exceed capacity until a non-duplicate entry is added.
	 */
	public QTree(
			ToDoubleFunction<T> xExtractor, 
			ToDoubleFunction<T> yExtractor, 
			double xMin, 
			double xMax, 
			double yMin, 
			double yMax,
			int capacity
	){
		this.xExtr = xExtractor;
		this.yExtr = yExtractor;
		this.entries = new ArrayList<>(capacity);
		// check that bounds are valid
		if(xMin > xMax || yMin > yMax) {
			throw new IllegalArgumentException("Invalid bounds: xMin must be <= xMax and yMin must be <= yMax");
		}
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		// check that capacity is valid
		if(capacity < 1) {
			throw new IllegalArgumentException("Capacity must be at least 1");
		}
		this.capacity = capacity;
	}
	
	/**
	 * Creates a new QTree with the given parameters. 
	 * The tree will be initialized with the given bounds and capacity=8.
	 * @param xExtractor function to extract x coordinate from an entry
	 * @param yExtractor function to extract y coordinate from an entry
	 * @param xMin minimum x coordinate of the tree bounds
	 * @param xMax maximum x coordinate of the tree bounds
	 * @param yMin minimum y coordinate of the tree bounds
	 * @param yMax maximum y coordinate of the tree bounds
	 * @see #QTree(ToDoubleFunction, ToDoubleFunction, double, double, double, double, int)
	 */
	public QTree(
			ToDoubleFunction<T> xExtractor, 
			ToDoubleFunction<T> yExtractor, 
			double xMin, 
			double xMax, 
			double yMin, 
			double yMax
	){
		this(xExtractor, yExtractor, xMin, xMax, yMin, yMax, 8);
	}
	
	/**
	 * Inserts the given element into the tree. 
	 * The x and y coordinates of the element are extracted using the provided extractors.
	 * @param element to insert
	 * @throws IllegalArgumentException if the element's coordinates are out of bounds of the tree
	 */
	public void insert(T element) {
		double x = getX(element);
		double y = getY(element);
		if (x < xMin || x > xMax || y < yMin || y > yMax) {
			throw new IllegalArgumentException(
					"Element coordinates out of bounds: "
					+ "(" + x + ", " + y + ") not in [" + xMin + ", " + xMax + "] x [" + yMin + ", " + yMax + "]"
					+ " for element: " + element);
		}
		insert(element, x, y);
	}
	
	private void insert(T element, double x, double y) {
		// if leaf node, try add
		if(entries != null) { 
			if(entries.size() < capacity) {
				entries.add(element);
				// if only duplicates contained, check if new entry is duplicate as well
				if(onlyDuplicatesContained) {
					onlyDuplicatesContained = (x == getX(entries.get(0)) && y == getY(entries.get(0)));
				}
				return;
			} else if(onlyDuplicatesContained && x == getX(entries.get(0)) && y == getY(entries.get(0))) {
				// if only duplicates contained, add anyway
				entries.add(element);
				return;
			} else {
				// split node into 4 subnodes
				double midx = xMin + (xMax-xMin) / 2;
				double midy = yMin + (yMax-yMin) / 2;
				LL = new QTree<>(xExtr, yExtr, xMin, midx, yMin, midy);
				LR = new QTree<>(xExtr, yExtr, midx, xMax, yMin, midy);
				UL = new QTree<>(xExtr, yExtr, xMin, midx, midy, yMax);
				UR = new QTree<>(xExtr, yExtr, midx, xMax, midy, yMax);
				ArrayList<T> oldEntries = entries;
				entries = null;
				for(int i = 0; i < oldEntries.size(); i++) {
					T oldEntry = oldEntries.get(i);
					double oldX = getX(oldEntry);
					double oldY = getY(oldEntry);
					insert(oldEntry, oldX, oldY);
				}
				// no longer leaf node, fallthrough to insert into subnode
			}
		}
		// if not leaf node, insert into correct subnode
		boolean left = x < xMin + (xMax-xMin) / 2;
		boolean lower = y < yMin + (yMax-yMin) / 2;
		if(left && lower) {
			LL.insert(element, x, y);
		} else if(left && !lower) {
			UL.insert(element, x, y);
		} else if(!left && lower) {
			LR.insert(element, x, y);
		} else {
			UR.insert(element, x, y);
		}
	}
	
	/**
	 * Returns a list of all entries in the tree that are within the given rectangular bounds.
	 * @param xMin minimum x coordinate of the bounds
	 * @param xMax maximum x coordinate of the bounds
	 * @param yMin minimum y coordinate of the bounds
	 * @param yMax maximum y coordinate of the bounds
	 * @return list of entries within the bounds
	 */
	public ArrayList<T> getEntriesInBounds(double xMin, double xMax, double yMin, double yMax){
		if(xMin > xMax || yMin > yMax) {
			throw new IllegalArgumentException("Invalid bounds: xMin must be <= xMax and yMin must be <= yMax");
		}
		return getEntriesInBounds(xMin, xMax, yMin, yMax, new ArrayList<>());
	}
	
	private ArrayList<T> getEntriesInBounds(double xMin, double xMax, double yMin, double yMax, ArrayList<T> result) {
		// no intersection
		if(this.xMax < xMin || this.xMin > xMax || this.yMax < yMin || this.yMin > yMax) {
			return result;
		}
		if(entries != null) { // leaf node
			// fully contained
			if(this.xMin >= xMin && this.xMax <= xMax && this.yMin >= yMin && this.yMax <= yMax) {
				result.addAll(this.entries);
			} else { // partially contained, check each entry
				for(int i = 0; i < entries.size(); i++) {
					T entry = entries.get(i);
					double x = getX(entry);
					double y = getY(entry);
					if(x >= xMin && x <= xMax && y >= yMin && y <= yMax) {
						result.add(entry);
					}
				}
			}
		} else { // not leaf node, get from subnodes
			LL.getEntriesInBounds(xMin, xMax, yMin, yMax, result);
			LR.getEntriesInBounds(xMin, xMax, yMin, yMax, result);
			UL.getEntriesInBounds(xMin, xMax, yMin, yMax, result);
			UR.getEntriesInBounds(xMin, xMax, yMin, yMax, result);
		}
		return result;
	}
	
	private final double getX(T element) {
		return xExtr.applyAsDouble(element);
	}
	
	private final double getY(T element) {
		return yExtr.applyAsDouble(element);
	}
	
}
