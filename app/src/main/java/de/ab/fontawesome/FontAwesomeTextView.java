package de.ab.fontawesome;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.TextView;

public class FontAwesomeTextView extends TextView {

	public FontAwesomeTextView(Context context) {
		super(context);
		init();
	}

	public FontAwesomeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public FontAwesomeTextView(Context context, AttributeSet attrs, int styleRes) {
		super(context, attrs, styleRes);
		init();
	}

	private void init() {
		FontAwesome.apply(this);
	}


	public void setHtmlText(int stringResourceIdentifier) {
		setHtmlText(getResources().getString(stringResourceIdentifier));
	}

	public void setHtmlText(String html) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			setText(Html.fromHtml(html, 0));
		else
			setText(Html.fromHtml(html));
	}
}
