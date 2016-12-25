package de.ab.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Configuration extends HashMap<String, Object> {

	private Pattern pattern;

	public Configuration() {
	}

	public Configuration(Map<? extends String, ? extends Object> other) {
		putAll(other);
	}

	protected Pattern onCreatePattern() {
		return new BlacklistPattern();
	}

	protected Pattern getPattern() {
		if (pattern == null)
			synchronized (getClass()) {
				if (pattern == null)
					pattern = onCreatePattern();
			}
		return pattern;
	}

	public boolean compare(Map<String, Object> other, String... keys) {
		for (String key : keys)
			if (!Objects.equals(get(key), other.get(key)))
				return false;
		return true;
	}

	public boolean deepCompare(Map<String, Object> other, String... keys) {
		for (String key : keys)
			if (!Objects.deepEquals(get(key), other.get(key)))
				return false;
		return true;
	}

	public boolean compare(Map<String, Object> other, Collection<String> keys) {
		for (String key : keys)
			if (!Objects.equals(get(key), other.get(key)))
				return false;
		return true;
	}

	public boolean deepCompare(Map<String, Object> other, Collection<String> keys) {
		for (String key : keys)
			if (!Objects.deepEquals(get(key), other.get(key)))
				return false;
		return true;
	}

	public Object get(Object key, Object defVal) {
		Object o = get(key);
		return o == null ? defVal : o;
	}

	private <A extends String, B> Map<A, B> select(Map<A, B> m) {
		Map<A, B> map = new HashMap<>();
		for (Entry<A, B> e : m.entrySet())
			if (getPattern().match(e.getKey()))
				map.put(e.getKey(), e.getValue());
		return map;
	}

	public interface Pattern {
		public boolean match(String key);
	}

	public static class WhitelistPattern implements Pattern {

		protected String[] keys;

		public WhitelistPattern(String... keys) {
			this.keys = keys;
		}

		@Override
		public boolean match(String key) {
			for (String key_ : keys)
				if (key_.equals(key))
					return true;
			return false;
		}

	}

	public static class BlacklistPattern implements Pattern {

		protected String[] keys;

		public BlacklistPattern(String... keys) {
			this.keys = keys;
		}

		@Override
		public boolean match(String key) {
			for (String key_ : keys)
				if (key_.equals(key))
					return false;
			return true;
		}

	}

}
