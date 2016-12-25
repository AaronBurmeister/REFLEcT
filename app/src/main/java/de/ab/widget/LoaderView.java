package de.ab.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.ab.reflect.R;

public class LoaderView extends StateView {

	private int stateLoader, stateContent;

	public LoaderView(Context context) {
		super(context);
		init();
	}

	public LoaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public LoaderView(Context context, AttributeSet attrs, int styleRes) {
		super(context, attrs, styleRes);
		init();
	}

	private void init() {
		stateLoader = addState();
		stateContent = addState();
		setContentView(stateLoader, onCreateLoader(LayoutInflater.from(getContext()), getParent(stateLoader)));
	}

	protected View onCreateLoader(LayoutInflater inflater, ViewGroup parent) {
		return inflater.inflate(R.layout.loader_view, parent, false);
	}

	public void setContentView(View view) {
		setContentView(stateContent, view);
	}

	public View getContentView() {
		return getContent(stateContent);
	}

	public void setContentView(@LayoutRes int layoutRes) {
		setContentView(stateContent, layoutRes);
	}

	public void setLoading(boolean loading) {
		setActiveState(loading ? stateLoader : stateContent);
	}

	public <Param, Data> void load(Process<Param, Data> process, Param... params) {
		try {
			setLoading(true);
			new Thread(new ProcessRunnable<Param, Data>(process, params)).start();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public interface Process<Param, Data> {

		Data load(View contentView, Param... params) throws Throwable;

		void finished(View contentView, Data data);

		void failed(View contentView, Throwable cause);

	}

	public class ProcessRunnable<Param, Data> implements Runnable {

		private Process<Param, Data> process;
		private Param[] params;

		ProcessRunnable(Process<Param, Data> process, Param[] params) {
			this.process = process;
			this.params = params;
		}

		@Override
		public void run() {
			try {
				while (!isAttachedToWindow())
					Thread.sleep(10);
				final Data data = process.load(getContentView(), params);
				getHandler().post(new Runnable() {
					@Override
					public void run() {
						try {
							process.finished(getContentView(), data);
							setLoading(false);
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				});
			} catch (final Throwable t) {
				try {
					getHandler().post(new Runnable() {
						@Override
						public void run() {
							try {
								process.failed(getContentView(), t);
								setLoading(false);
							} catch (Throwable t) {
								t.printStackTrace();
							}
						}
					});
				} catch (Throwable t2) {
					t2.printStackTrace();
				}
			}
		}

	}

}
