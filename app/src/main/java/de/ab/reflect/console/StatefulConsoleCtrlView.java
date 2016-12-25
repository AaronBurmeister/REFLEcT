package de.ab.reflect.console;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.ab.fontawesome.FontAwesomeButton;
import de.ab.reflect.R;
import de.ab.widget.StateView;

public class StatefulConsoleCtrlView extends StateView implements OnClickListener {

	private int stateConn, stateLost, stateReconn;
	private ConsoleCtrlView consoleCtrlView;
	private OnRetryListener onRetryListener;

	public StatefulConsoleCtrlView(Context context) {
		super(context);
		init();
	}

	public StatefulConsoleCtrlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public StatefulConsoleCtrlView(Context context, AttributeSet attrs, int styleRes) {
		super(context, attrs, styleRes);
		init();
	}

	private void init() {
		consoleCtrlView = new ConsoleCtrlView(getContext());
		stateConn = addState(consoleCtrlView);

		RelativeLayout view = new RelativeLayout(getContext());
		view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		LayoutParams params;
		params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(CENTER_VERTICAL);
		params.addRule(LEFT_OF, R.id.retry);
		TextView connLostMsg = new TextView(getContext());
		connLostMsg.setText(R.string.no_connection);
		connLostMsg.setLayoutParams(params);
		params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(CENTER_VERTICAL);
		params.addRule(ALIGN_PARENT_RIGHT);
		Button retryButton = new FontAwesomeButton(getContext(), null, android.R.attr.buttonBarButtonStyle);
		retryButton.setId(R.id.retry);
		((FontAwesomeButton) retryButton).setHtmlText(R.string.fa_refresh);
		retryButton.setLayoutParams(params);
		retryButton.setOnClickListener(this);
		view.addView(retryButton);
		view.addView(connLostMsg);
		stateLost = addState(view);

		TextView reconnMsg = new TextView(getContext());
		reconnMsg.setText(R.string.reconnecting);
		stateReconn = addState(reconnMsg);
	}

	public void setConsole(Console console) {
		consoleCtrlView.setConsole(console);
	}

	public ConsoleCtrlView getConsoleCtrlView() {
		return consoleCtrlView;
	}

	public void setOnRetryListener(OnRetryListener listener) {
		onRetryListener = listener;
	}

	public void setIsConnected() {
		setActiveState(stateConn);
	}

	public void setIsLost() {
		setActiveState(stateLost);
	}

	public void setIsReconnecting() {
		setActiveState(stateReconn);
	}

	@Override
	public void onClick(View p1) {
		if (onRetryListener != null) {
			setIsReconnecting();
			onRetryListener.onRetry(this);
		}
	}

	public interface OnRetryListener {
		void onRetry(StatefulConsoleCtrlView view);
	}

}
