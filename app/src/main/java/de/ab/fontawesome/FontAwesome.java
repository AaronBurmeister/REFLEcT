package de.ab.fontawesome;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.widget.TextView;

public class FontAwesome {

	private static final String FILE_NAME = "fontawesome.ttf";

	private static volatile Typeface instance;

	public static Typeface getTypeface(AssetManager assets) {
		if (instance == null)
			synchronized (FontAwesome.class) {
				if (instance == null)
					instance = Typeface.createFromAsset(assets, FILE_NAME);
			}
		return instance;
	}

	public static void apply(TextView view) {
		view.setTypeface(getTypeface(view.getContext().getAssets()));
	}

}
