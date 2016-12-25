package de.ab.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class StateView extends RelativeLayout {

	private SparseArray<ViewGroup> states = new SparseArray<>();
	private SparseArray<View> contents = new SparseArray<>();

	private int nextState = 0;

	public StateView(Context context) {
		super(context);
	}

	public StateView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public StateView(Context context, AttributeSet attrs, int styleRes) {
		super(context, attrs, styleRes);
	}

	public int addState() {
		int state = nextState++;
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(CENTER_IN_PARENT);
		RelativeLayout parent = new RelativeLayout(getContext());
		parent.setLayoutParams(params);
		parent.setVisibility(GONE);
		addView(parent);
		states.put(state, parent);
		return state;
	}

	public int addState(View view) {
		int state = addState();
		setContentView(state, view);
		return state;
	}

	public int addState(@LayoutRes int layoutRes) {
		int state = addState();
		setContentView(state, layoutRes);
		return state;
	}

	public void removeState(int state) {
		ViewGroup parent = getParent(state);
		parent.removeAllViews();
		removeView(parent);
		states.remove(state);
	}

	public void setContentView(int state, View view) {
		ViewGroup parent = getParent(state);
		parent.removeAllViews();
		contents.put(state, view);
		if (view != null)
			parent.addView(view);
	}

	public void setContentView(int state, @LayoutRes int layoutRes) {
		setContentView(state, LayoutInflater.from(getContext()).inflate(layoutRes, getParent(state), false));
	}

	protected ViewGroup getParent(int state) {
		return states.get(state);
	}

	protected View getContent(int state) {
		return contents.get(state);
	}

	public void setActiveState(int state) {
		for (int i = 0, key = states.keyAt(i); i < states.size(); key = states.keyAt(++i))
			states.get(key).setVisibility(key == state ? VISIBLE : GONE);
	}

	public void setActiveStates(int... states) {
		for (int i = 0, key = this.states.keyAt(i); i < this.states.size(); key = this.states.keyAt(++i))
			this.states.get(key).setVisibility(contains(states, key) ? VISIBLE : GONE);
	}

	private boolean contains(int[] array, int val) {
		for (int i : array)
			if (i == val)
				return true;
		return false;
	}

}
