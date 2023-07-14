package de.markusfisch.android.shadereditor.util;

import java.util.Arrays;

public class IntList {
	public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

	private static final int DEFAULT_CAPACITY = 10;
	private transient int[] elementData;
	private int size;


	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the list
	 * @throws IllegalArgumentException if the specified initial capacity
	 *                                  is negative
	 */
	public IntList(int initialCapacity) {
		if (initialCapacity > 0) {
			this.elementData = new int[initialCapacity];
		} else if (initialCapacity == 0) {
			this.elementData = new int[DEFAULT_CAPACITY];
		} else {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
	}

	/**
	 * Constructs an empty list with an initial capacity of ten.
	 */
	public IntList() {
		this.elementData = new int[DEFAULT_CAPACITY];
	}

	private static int newLength(int oldLength, int minGrowth, int prefGrowth) {

		int prefLength = oldLength + Math.max(minGrowth, prefGrowth); // might overflow
		if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
			return prefLength;
		} else {
			// put code cold in a separate method
			return hugeLength(oldLength, minGrowth);
		}
	}

	private static int hugeLength(int oldLength, int minGrowth) {
		int minLength = oldLength + minGrowth;
		if (minLength < 0) { // overflow
			throw new OutOfMemoryError(
					"Required array length " + oldLength + " + " + minGrowth + " is too large");
		} else if (minLength <= SOFT_MAX_ARRAY_LENGTH) {
			return SOFT_MAX_ARRAY_LENGTH;
		} else {
			return minLength;
		}
	}

	/**
	 * Increases the capacity to ensure that it can hold at least the
	 * number of elements specified by the minimum capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity
	 * @throws OutOfMemoryError if minCapacity is less than zero
	 */
	private int[] grow(int minCapacity) {
		int oldCapacity = elementData.length;
		if (oldCapacity > 0) {
			int newCapacity = newLength(oldCapacity,
					minCapacity - oldCapacity, /* minimum growth */
					oldCapacity >> 1           /* preferred growth */);
			return elementData = Arrays.copyOf(elementData, newCapacity);
		} else {
			return elementData = new int[Math.max(DEFAULT_CAPACITY, minCapacity)];
		}
	}

	private int[] grow() {
		return grow(size + 1);
	}

	/**
	 * Returns the number of elements in this list.
	 *
	 * @return the number of elements in this list
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns {@code true} if this list contains no elements.
	 *
	 * @return {@code true} if this list contains no elements
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	public int[] getRaw() {
		return elementData;
	}

	public void trimToSize() {
		if (size < elementData.length) {
			elementData = Arrays.copyOf(elementData, size);
		}
	}

	int elementData(int index) {
		return elementData[index];
	}

	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this list
	 * @throws IndexOutOfBoundsException if index not in the range 0..size
	 */
	public int get(int index) {
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		return elementData(index);
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e element to be appended to this list
	 * @return {@code true} (as specified by {@link java.util.Collection#add})
	 */
	public boolean add(int e) {
		if (size == elementData.length)
			elementData = grow();
		elementData[size] = e;
		++size;
		return true;
	}

	public int hashCode() {
		final int[] es = elementData;
		int hashCode = 1;
		for (int i = 0; i < size; i++) {
			int e = es[i];
			hashCode = 31 * hashCode + e;
		}
		return hashCode;
	}

	/**
	 * Removes all of the elements from this list.  The list will
	 * be empty after this call returns.
	 */
	public void clear() {
		size = 0;
	}
}