package de.ab.reflect.console;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import android.widget.AdapterView.*;
import android.widget.*;
import android.content.*;
import de.ab.reflect.*;

public class ConsoleView extends ListView implements Console, OnItemLongClickListener {

	private ConsoleAdapter adapter;
	private OnCloseListener onCloseListener;

	public ConsoleView(Context context) {
		super(context);
		init();
	}

	public ConsoleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ConsoleView(Context context, AttributeSet attrs, int styleRes) {
		super(context, attrs, styleRes);
		init();
	}

	private void init() {
		setDivider(null);
		setDividerHeight(0);
		setAdapter(adapter = new ConsoleAdapter(getContext()));
		setOnItemLongClickListener(this);
	}

	@Override
	public void write(final int _byte) {
		post(new Runnable() {
			@Override
			public void run() {
				try {
					char _char = (char) _byte;
					adapter.write(_char);
					if (_char == '\n')
						setSelection(adapter.getCount() - 1);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}

	@Override
	public void write(final byte... bytes) {
		post(new Runnable() {
			@Override
			public void run() {
				for (byte _byte : bytes) {
					try {
						char _char = (char) _byte;
						adapter.write(_char);
						if (_char == '\n')
							setSelection(adapter.getCount() - 1);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void close() {
		write("Connection closed.\n\n".getBytes());
		post(new Runnable() {
			@Override
			public void run() {
				if (onCloseListener != null)
					onCloseListener.onClose(ConsoleView.this);
			}
		});
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> p1, View p2, int index, long p4)
	{
		ClipData clip = ClipData.newPlainText(getContext().getPackageName(), (String) getAdapter().getItem(index));
		((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
		Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
		return true;
	}

	public int getOverflowLimit() {
		return adapter.getOverflowLimit();
	}

	public void setOverflowLimit(int overflowLimit) {
		adapter.setOverflowLimit(overflowLimit);
	}

	public void setOnCloseListener(OnCloseListener listener) {
		onCloseListener = listener;
	}

	public interface OnCloseListener {
		void onClose(ConsoleView view);
	}

	public static class ConsoleAdapter extends BaseAdapter {

		private Context context;
		private List<String> data;
		private int overflowLimit = 0;

		ConsoleAdapter(Context context) {
			this.context = context;
			data = new ArrayList<String>();
		}

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public String getItem(int p1) {
			return data.get(p1);
		}

		@Override
		public long getItemId(int p1) {
			return p1;
		}

		@Override
		public View getView(int p1, View p2, ViewGroup p3) {
			TextView view = (TextView) p2;
			if (view == null)
				view = new TextView(context);
			view.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
			view.setText(getItem(p1));
			return view;
		}

		public void write(char _char) {
			if (_char == '\n')
				data.add("");
			else {
				int index = data.size() - 1;
				if (index < 0)
					data.add("" + _char);
				else
					data.set(index, data.get(index) + _char);
			}
			handleOverflow();
			notifyDataSetChanged();
		}

		public int getOverflowLimit() {
			return overflowLimit;
		}

		public void setOverflowLimit(int overflowLimit) {
			this.overflowLimit = overflowLimit;
		}

		private void handleOverflow() {
			if (overflowLimit > 0)
				while (data.size() > overflowLimit)
					data.remove(0);
		}

	}

}
