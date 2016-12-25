package de.ab.util;

public interface Configurable<Type extends Configuration> {
	Type getConfiguration();
}
