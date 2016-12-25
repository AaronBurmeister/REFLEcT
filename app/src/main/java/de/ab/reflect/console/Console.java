package de.ab.reflect.console;

import java.io.Closeable;
import java.io.IOException;

public interface Console extends Closeable {

	void write(int _byte) throws IOException;

	void write(byte... bytes) throws IOException;

}
