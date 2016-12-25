package de.ab.reflect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class ProjectAdapter extends BaseAdapter {

	private Context context;
	private String host;
	private int port;

	private String[][] projects;

	public ProjectAdapter(Context context, String host, int port) {
		this.context = context;
		this.host = host;
		this.port = port;

		update();
	}

	public void update() {
		String[] projectIdentifiers = LocalFiles.listProjectIdentifiers(context, host, port);
		projects = new String[projectIdentifiers.length][3];
		for (int i = 0; i < projectIdentifiers.length; i++) {
			projects[i][0] = projectIdentifiers[i];
			try {
				File projectRoot = LocalFiles.getProjectRoot(context, host, port, projectIdentifiers[i]);
				projects[i][1] = LocalFiles.getProjectTitle(projectRoot);
				projects[i][2] = LocalFiles.getProjectRemoteDir(projectRoot).getPath();
			} catch (IOException e) {
				projects[i][1] = context.getResources().getString(R.string.unknown_project_name);
			}
		}
		notifyDataSetChanged();
	}

	@Override
	public String[] getItem(int i) {
		return projects[i];
	}

	@Override
	public int getCount() {
		return projects.length;
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View convertView, ViewGroup viewGroup) {
		View view = convertView;
		if (view == null)
			view = LayoutInflater.from(context).inflate(R.layout.project_adapter_item, viewGroup, false);
		String[] data = getItem(i);
		view.setTag(data[0]);
		((TextView) view.findViewById(R.id.project_name)).setText(data[1]);
		((TextView) view.findViewById(R.id.dir_path)).setText(data[2]);
		return view;
	}

}
