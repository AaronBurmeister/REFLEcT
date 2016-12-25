package de.ab.util;

public interface Mappable<K, T> {

	boolean map(K key, T value);

}
