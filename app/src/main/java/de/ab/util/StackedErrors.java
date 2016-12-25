package de.ab.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public interface StackedErrors {

	int unique();

	Throwable popNextError(int type);

	Throwable popLatestError(int type);

	Throwable peekNextError(int type);

	Throwable peekLatestError(int type);

	void clearErrorStack(int type);

	OverrideErrorType startOverrideErrorType(int type);

	void endOverrideErrorType();

	public interface OverrideErrorType extends Closeable {
	}

	public static class ImplPr implements StackedErrors {

		private int unique = 0;
		private Deque<Entry> stack = new LinkedList<>();
		private Integer overrideType;

		@Override
		public int unique() {
			return unique++;
		}

		protected synchronized void onError(int type, Throwable t) {
			stack.addLast(new Entry(overrideType == null ? type : overrideType, t));
		}

		@Override
		public synchronized Throwable popNextError(int type) {
			for (Entry e : stack)
				if (e.getKey() == type) {
					stack.remove(e);
					return e.getValue();
				}
			return null;
		}

		@Override
		public synchronized Throwable popLatestError(int type) {
			for (Iterator<Entry> iterator = stack.descendingIterator(); iterator.hasNext(); ) {
				Entry e = iterator.next();
				if (e.getKey() == type) {
					stack.remove(e);
					return e.getValue();
				}
			}
			return null;
		}

		@Override
		public synchronized Throwable peekNextError(int type) {
			for (Entry e : stack)
				if (e.getKey() == type)
					return e.getValue();
			return null;
		}

		@Override
		public synchronized Throwable peekLatestError(int type) {
			for (Iterator<Entry> iterator = stack.descendingIterator(); iterator.hasNext(); ) {
				Entry e = iterator.next();
				if (e.getKey() == type)
					return e.getValue();
			}
			return null;
		}

		@Override
		public synchronized void clearErrorStack(int type) {
			for (Entry e : stack)
				if (e.getKey() == type)
					stack.remove(e);
		}

		@Override
		public synchronized OverrideErrorType startOverrideErrorType(int type) {
			overrideType = type;
			return new OverrideErrorType() {
				@Override
				public void close() throws IOException {
					endOverrideErrorType();
				}
			};
		}

		@Override
		public synchronized void endOverrideErrorType() {
			overrideType = null;
		}

		private static class Entry implements Map.Entry<Integer, Throwable> {

			private int key;
			private Throwable value;

			public Entry(int key, Throwable value) {
				this.key = key;
				this.value = value;
			}

			@Override
			public Integer getKey() {
				return key;
			}

			@Override
			public Throwable getValue() {
				return value;
			}

			@Override
			public Throwable setValue(Throwable value) {
				Throwable prev = this.value;
				this.value = value;
				return prev;
			}

		}

	}

	public static class Impl extends ImplPr {

		@Override
		public void onError(int type, Throwable t) {
			super.onError(type, t);
		}

	}

}
