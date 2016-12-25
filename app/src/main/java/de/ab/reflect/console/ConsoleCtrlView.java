package de.ab.reflect.console;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.ab.fontawesome.FontAwesomeButton;
import de.ab.reflect.R;

public class ConsoleCtrlView extends RelativeLayout implements OnEditorActionListener, OnClickListener {

	private AutoCompleteTextView inputField;
	private Button sendButton;
	private Console console;
	private SuggestionsAdapter<String> suggestions;

	public ConsoleCtrlView(Context context) {
		super(context);
		init();
	}

	public ConsoleCtrlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ConsoleCtrlView(Context context, AttributeSet attrs, int styleRes) {
		super(context, attrs, styleRes);
		init();
	}

	private void init() {
		LayoutParams params;

		params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(CENTER_VERTICAL);
		params.addRule(LEFT_OF, R.id.send);
		inputField = new AutoCompleteTextView(getContext());
		inputField.setSingleLine();
		inputField.setMaxLines(1);
		try {
			inputField.setAdapter(suggestions = new SuggestionsAdapter<>(getContext()));
		} catch (Throwable t) {
			Toast.makeText(getContext(), t.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
		}
		inputField.setLayoutParams(params);
		inputField.setOnEditorActionListener(this);
		inputField.setThreshold(0);

		params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(CENTER_VERTICAL);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
			params.addRule(ALIGN_PARENT_END);
		else
			params.addRule(ALIGN_PARENT_RIGHT);
		sendButton = new FontAwesomeButton(getContext(), null, android.R.attr.buttonBarButtonStyle);
		sendButton.setId(R.id.send);
		((FontAwesomeButton) sendButton).setHtmlText(R.string.fa_paper_plane);
		sendButton.setLayoutParams(params);
		sendButton.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
		sendButton.setOnClickListener(this);

		addView(sendButton);
		addView(inputField);
	}

	public EditText getInputField() {
		return inputField;
	}

	public Button getSendButton() {
		return sendButton;
	}

	public void setConsole(Console console) {
		this.console = console;
	}

	public String getCommand() {
		return getInputField().getText().toString();
	}

	public void setCommand(String command) {
		getInputField().setText(command);
	}

	@Override
	public void onClick(View v) {
		try {
			exec();
		} catch (IOException e) {
			Toast.makeText(getContext(), R.string.error_exec_command, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onEditorAction(TextView p1, int p2, KeyEvent p3) {
		try {
			exec();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void exec() throws IOException {
		String cmd = getCommand();
		if (console != null && cmd.trim().length() > 0) {
			suggestions.push(cmd);
			console.write((cmd + "\n").getBytes());
		}
		getInputField().setText(null);
	}

	public static class SuggestionsAdapter<Data> extends ArrayAdapter<Data> implements Filterable {

		private List<Data> elem = new ArrayList<>(), data = new ArrayList<>();

		SuggestionsAdapter(Context context) {
			super(context, R.layout.text);
		}

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Data getItem(int p1) {
			return data.get(p1);
		}

		@Override
		public long getItemId(int p1) {
			return p1;
		}

		@NonNull
		@Override
		public Filter getFilter() {
			return new Filter() {

				@Override
				protected Filter.FilterResults performFiltering(CharSequence constraint) {
					FilterResults r = new FilterResults();
					if (constraint != null) {
						List<Data> filtered = new ArrayList<>();
						for (Data d : elem)
							if (d.toString().toLowerCase().contains(constraint.toString().toLowerCase()))
								filtered.add(d);
						r.values = filtered;
						r.count = filtered.size();
					}
					return r;
				}

				@Override
				protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
					data.clear();
					if (results != null && results.count > 0)
						data.addAll((Collection<Data>) results.values);
					notifyDataSetChanged();
				}
			};
		}

		public void push(Data entry) {
			if (!elem.contains(entry)) {
				elem.add(entry);
				notifyDataSetChanged();
			}
		}

	}

}
