package de.ab.reflect;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.StringRes;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tooltip.Tooltip;

import de.ab.fontawesome.FontAwesomeButton;
import de.ab.fontawesome.FontAwesomeTextView;
import android.widget.*;
import android.widget.AdapterView.*;

public class ProjectView extends LinearLayout implements View.OnClickListener, PopupMenu.OnMenuItemClickListener
{

	private ProjectViewListener listener;
	private TextView title;
	private LinearLayout shortcuts;
	private PopupMenu menu;
	private String remoteDir;

	public ProjectView(Context context)
	{
		super(context);
		init();
	}

	public ProjectView(Context context, AttributeSet attr)
	{
		super(context, attr);
		init();
	}

	public ProjectView(Context context, AttributeSet attr, int styleRes)
	{
		super(context, attr, styleRes);
		init();
	}

	private void init()
	{
		setOrientation(VERTICAL);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.LEFT_OF, R.id.options);
		params.addRule(RelativeLayout.START_OF, R.id.options);
		title = new TextView(getContext());
		title.setLayoutParams(params);
		title.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
		title.setSingleLine();
		title.setMaxLines(1);
		title.setHorizontallyScrolling(true);
		title.setEllipsize(TextUtils.TruncateAt.END);

		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.ALIGN_PARENT_END);
		FontAwesomeButton options = new FontAwesomeButton(getContext(), null, android.R.attr.buttonBarButtonStyle);
		options.setLayoutParams(params);
		options.setHtmlText(R.string.fa_caret_down);
		options.setOnClickListener(this);

		RelativeLayout titleLayout = new RelativeLayout(getContext());
		titleLayout.addView(title);
		titleLayout.addView(options);

		shortcuts = new LinearLayout(getContext());
		shortcuts.setOrientation(HORIZONTAL);

		HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
		scrollView.addView(shortcuts);

		addView(titleLayout);
		addView(scrollView);

		menu = new PopupMenu(getContext(), options);
		menu.inflate(R.menu.project_view);
		menu.setOnMenuItemClickListener(this);
	}

	@Override
	public void onClick(View v)
	{
		menu.show();
	}

	@Override
	public boolean onMenuItemClick(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.close:
				if (listener != null)
					listener.onRequestClose();
				return true;
			case R.id.edit:
				listener.onRequestEdit();
				return true;
			case R.id.add_shortcut:
				shortcutConfig(null);
				return true;
			case R.id.edit_shortcut:
				selectShortcutForEditing(false);
				return true;
			case R.id.remove_shortcut:
				selectShortcutForEditing(true);
				return true;
			case R.id.remove:
				if (listener != null)
					listener.onRequestRemove();
				return true;
			default:
				return false;
		}
	}

	public void setProjectViewListener(ProjectViewListener listener)
	{
		this.listener = listener;
	}

	public void setTitle(@StringRes int title)
	{
		setTitle(getContext().getString(title));
	}

	public void setTitle(String title)
	{
		this.title.setText(title);
	}

	public String getTitle()
	{
		return this.title.getText().toString();
	}

	public void setRemoteDir(String remoteDir)
	{
		this.remoteDir = remoteDir;
	}

	public void clearShortcuts()
	{
		for (int i = 0; i < shortcuts.getChildCount(); i++)
			shortcuts.getChildAt(i).setOnClickListener(null);
		shortcuts.removeAllViews();
	}

	public void addShortcut(int itemId, @StringRes int faTag, @StringRes int description, @StringRes int command)
	{
		addShortcut(itemId, getContext().getString(faTag), getContext().getText(description), getContext().getString(command));
	}

	public void addShortcut(int itemId, String faTag, CharSequence description, String command)
	{
		FontAwesomeButton button = new FontAwesomeButton(getContext(), null, android.R.attr.buttonBarButtonStyle);
		button.setHtmlText(faTag);
		button.setContentDescription(description);
		button.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View view)
				{
					final Tooltip tooltip = new Tooltip.Builder(view)
						.setText(view.getContentDescription().toString())
						.setDismissOnClick(true)
						.setCancelable(true)
						.setTextColor(Color.DKGRAY)
						.setBackgroundColor(Color.LTGRAY)
						.show();
					getHandler().postDelayed(new Runnable() {
							@Override
							public void run()
							{
								tooltip.dismiss();
							}
						}, 2000);
					return true;
				}
			});
		button.setTag(itemId);
		button.setTag(R.id.command, command);
		button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					if (listener != null)
						listener.onShortcutClick(v, (int) v.getTag(), (String) v.getTag(R.id.command));
				}
			});
		shortcuts.addView(button);
	}

	public void updateShortcut(int itemId, @StringRes int faTag, @StringRes int description, @StringRes int command)
	{
		updateShortcut(itemId, getContext().getString(faTag), getContext().getText(description), getContext().getString(command));
	}

	public void updateShortcut(int itemId, String faTag, CharSequence description, String command)
	{
		FontAwesomeButton button = (FontAwesomeButton) shortcuts.findViewWithTag(itemId);
		if (button != null)
		{
			button.setHtmlText(faTag);
			button.setContentDescription(description);
			button.setTag(R.id.command, command);
		}
	}
	
	public void moveShortcut(int itemId, int steps) {
		FontAwesomeButton button = (FontAwesomeButton) shortcuts.findViewWithTag(itemId);
		int index = shortcuts.indexOfChild(button);
		shortcuts.removeViewAt(index);
		shortcuts.addView(button, index + steps);
	}

	public void removeShortcut(int itemId)
	{
		int i = getShortcutIndex(itemId);
		if (i > -1)
			shortcuts.removeViewAt(i);
	}

	public int getShortcutCount()
	{
		return shortcuts.getChildCount();
	}

	public int getShortcutIndex(int itemId)
	{
		return shortcuts.indexOfChild(shortcuts.findViewWithTag(itemId));
	}

	public interface ProjectViewListener
	{
		void onRequestClose();

		void onRequestEdit();

		void onRequestAddShortcut(String faTag, CharSequence description, String command);

		void onRequestEditShortcut(int itemId, String faTag, CharSequence description, String command);

		void onRequestMoveShortcut(int itemId, int steps);
		
		void onRequestRemoveShortcut(int itemId);

		void onRequestRemove();

		void onShortcutClick(View v, int itemId, String command);
	}

	private void selectShortcutForEditing(final boolean remove)
	{
		final BaseAdapter adapter = new BaseAdapter() {
			@Override
			public int getCount()
			{
				return shortcuts.getChildCount();
			}

			@Override
			public FontAwesomeButton getItem(int i)
			{
				return (FontAwesomeButton) shortcuts.getChildAt(i);
			}

			@Override
			public long getItemId(int i)
			{
				return i;
			}

			@Override
			public View getView(final int i, View convertView, ViewGroup viewGroup)
			{
				View view = convertView;
				if (view == null)
					view = LayoutInflater.from(getContext()).inflate(R.layout.project_view_item, viewGroup, false);
				FontAwesomeButton data = getItem(i);
				((FontAwesomeTextView) view.findViewById(R.id.icon)).setHtmlText(data.getText().toString());
				((TextView) view.findViewById(R.id.description)).setText(data.getContentDescription());
				view.findViewById(R.id.controls).setVisibility(remove ? View.GONE : View.VISIBLE);
				view.findViewById(R.id.down).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v)
						{
							listener.onRequestMoveShortcut((int) getItem(i).getTag(), 1);
							notifyDataSetChanged();
						}
					});
				view.findViewById(R.id.up).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v)
						{
							listener.onRequestMoveShortcut((int) getItem(i).getTag(), -1);
							notifyDataSetChanged();
						}
					});
				return view;
			}
		};

		new AlertDialog.Builder(getContext())
			.setTitle(remove ? R.string.remove_shortcut : R.string.edit_shortcut)
			.setAdapter(adapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					if (listener != null)
					{
						FontAwesomeButton data = (FontAwesomeButton) adapter.getItem(i);
						if (remove)
							listener.onRequestRemoveShortcut((int) data.getTag());
						else
							shortcutConfig((int) data.getTag());
					}
				}
			})
			.show();
	}

	private void shortcutConfig(final Integer itemId)
	{
		final ArrayAdapter<String> faAdapter = new ArrayAdapter<String>(getContext(), R.layout.fa_text, getContext().getResources().getStringArray(R.array.fa));

		final FontAwesomeButton icon = new FontAwesomeButton(getContext(), null, android.R.attr.buttonBarButtonStyle);
		icon.setText(faAdapter.getItem(0));
		icon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View p1)
				{
					GridView v = new GridView(getContext());
					v.setNumColumns(GridView.AUTO_FIT);
					v.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
					v.setColumnWidth(getContext().getResources().getDimensionPixelSize(R.dimen.fa_width));
					v.setAdapter(faAdapter);
					final AlertDialog dialog = new AlertDialog.Builder(getContext())
						.setView(v)
						.show();
					v.setOnItemClickListener(new OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> v, View p2, int position, long p4)
							{
								icon.setText(faAdapter.getItem(position));
								dialog.dismiss();
							}
						});
				}
			});

		final EditText description = new EditText(getContext());
		description.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		description.setSingleLine();
		description.setMaxLines(1);
		description.setHint(R.string.description);
		description.setHorizontallyScrolling(true);

		LinearLayout visualLayout = new LinearLayout(getContext());
		visualLayout.setOrientation(HORIZONTAL);
		visualLayout.setGravity(Gravity.CENTER);
		visualLayout.addView(icon);
		visualLayout.addView(description);

		final EditText command = new EditText(getContext());
		command.setSingleLine();
		command.setMaxLines(1);
		command.setHorizontallyScrolling(true);
		command.setHint(R.string.command);
		command.setText(remoteDir == null ? "" : remoteDir);

		LinearLayout view = new LinearLayout(getContext());
		view.setOrientation(VERTICAL);
		view.addView(visualLayout);
		view.addView(command);

		if (itemId != null)
		{
			FontAwesomeButton orig = (FontAwesomeButton) shortcuts.findViewWithTag(itemId);
			icon.setText(orig.getText());
			description.setText(orig.getContentDescription().toString());
			command.setText((String) orig.getTag(R.id.command));
		}

		new AlertDialog.Builder(getContext())
			.setTitle(itemId == null ? R.string.add_shortcut : R.string.edit_shortcut)
			.setView(view)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					String faTag = icon.getText().toString();
					String descriptionString = description.getText().toString().trim();
					String commandString = command.getText().toString().trim();

					if (description.length() == 0 || command.length() == 0)
					{
						Toast.makeText(getContext(), R.string.nothing_entered, Toast.LENGTH_SHORT).show();
						return;
					}

					if (listener != null)
						if (itemId == null)
							listener.onRequestAddShortcut(faTag, descriptionString, commandString);
						else
							listener.onRequestEditShortcut(itemId, faTag, descriptionString, commandString);
				}
			})
			.setNegativeButton(R.string.cancel, null)
			.show();
	}

}
