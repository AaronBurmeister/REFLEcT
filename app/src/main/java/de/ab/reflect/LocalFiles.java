package de.ab.reflect;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class LocalFiles {

	public static final String NO_PROJECT = null;
	private File projectRoot, root;

	private LocalFiles(File root) {
		this(null, root);
	}

	private LocalFiles(File projectRoot, File root) {
		this.projectRoot = projectRoot;
		this.root = root;
	}

	private static File getBaseDir(Context context, String host, int port) {
		return new File(context.getExternalFilesDir(null), host + "#" + port);
	}

	private static File getProjectRoot(File baseDir, String identifier) {
		return new File(new File(baseDir, "projects"), identifier);
	}

	public static File getProjectRoot(Context context, String host, int port, String identifier) {
		return getProjectRoot(getBaseDir(context, host, port), identifier);
	}

	private static File getProjectFileDir(File projectRoot) {
		return new File(projectRoot, "files");
	}

	public static String[] listProjectIdentifiers(Context context, String host, int port) {
		File root = new File(getBaseDir(context, host, port), "projects");
		String[] identifiers = root.list();
		return identifiers == null ? new String[0] : identifiers;
	}

	public static String getProjectTitle(File projectRoot) throws IOException {
		return loadProjectMapping(projectRoot)[0];
	}

	public static File getProjectRemoteDir(File projectRoot) throws IOException {
		return new File(loadProjectMapping(projectRoot)[1]);
	}

	public static void renameProject(File projectRoot, String title) throws IOException {
		putProjectMapping(projectRoot, title, getProjectRemoteDir(projectRoot).getPath());
	}

	public static void putProjectMapping(File projectRoot, String title, String remoteDir) throws IOException {
		File config = new File(projectRoot, "config");
		BufferedWriter writer = null;
		Throwable tt = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config, false)));
			writer.write(title);
			writer.newLine();
			writer.write(remoteDir);
		} catch (Throwable t) {
			tt = t;
		}
		try {
			if (writer != null)
				writer.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (tt != null)
			if (tt instanceof IOException)
				throw (IOException) tt;
			else
				throw new IOException(tt);
	}

	private static String[] loadProjectMapping(File projectRoot) throws IOException {
		File config = new File(projectRoot, "config");
		BufferedReader reader = null;
		Throwable tt = null;
		String[] result = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
			result = new String[]{
					reader.readLine(),
					reader.readLine()
			};
		} catch (Throwable t) {
			tt = t;
		}
		try {
			if (reader != null)
				reader.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (tt != null)
			if (tt instanceof IOException)
				throw (IOException) tt;
			else
				throw new IOException(tt);
		return result;
	}

	public static boolean closeProject(File projectRoot) {
		return recursiveDelete(projectRoot);
	}

	public static LocalFiles newProject(Context context, String host, int port, String title, File remoteDir) {
		File baseDir = getBaseDir(context, host, port);
		String identifier;
		File projectRoot;
		do {
			identifier = String.valueOf(System.currentTimeMillis());
		} while ((projectRoot = getProjectRoot(baseDir, identifier)).exists());
		File fileDir = getProjectFileDir(projectRoot);
		if (initProject(projectRoot, title, remoteDir))
			return new LocalFiles(projectRoot, fileDir);
		closeProject(projectRoot);
		return null;
	}

	private static boolean initProject(File projectRoot, String title, File remoteDir) {
		try {
			if (!projectRoot.mkdirs())
				return false;
			if (!getProjectFileDir(projectRoot).mkdirs())
				return false;
			if (!new File(projectRoot, "config").createNewFile())
				return false;
			if (!new File(projectRoot, "shortcuts").createNewFile())
				return false;
			putProjectMapping(projectRoot, title, remoteDir.getPath());
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static LocalFiles get(Context context, String host, int port, String projectIdentifier) {
		File baseDir = getBaseDir(context, host, port);
		if (projectIdentifier == null || projectIdentifier.equals(NO_PROJECT))
			return new LocalFiles(new File(baseDir, "files"));
		File projectRoot = getProjectRoot(baseDir, projectIdentifier);
		return getProject(projectRoot);
	}
	
	public static LocalFiles getProject(File projectRoot) {
		return new LocalFiles(projectRoot, getProjectFileDir(projectRoot));
	}

	public static String[][] listShortcuts(File projectRoot) throws IOException {
		File shortcuts = new File(projectRoot, "shortcuts");
		BufferedReader reader = null;
		Throwable tt = null;
		List<String[]> result = new ArrayList<>();
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(shortcuts)));
			String line;
			while ((line = reader.readLine()) != null && line.length() > 0)
				result.add(
						new String[]{
								line,
								reader.readLine(),
								reader.readLine()
						});
		} catch (Throwable t) {
			tt = t;
		}
		try {
			if (reader != null)
				reader.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (tt != null)
			if (tt instanceof IOException)
				throw (IOException) tt;
			else
				throw new IOException(tt);
		return result.toArray(new String[result.size()][]);
	}

	public static void putShortcuts(File projectRoot, String[][] shortcutData) throws IOException {
		File shortcuts = new File(projectRoot, "shortcuts");
		BufferedWriter writer = null;
		Throwable tt = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(shortcuts, false)));
			for (String[] shortcut : shortcutData) {
				writer.write(shortcut[0]);
				writer.newLine();
				writer.write(shortcut[1]);
				writer.newLine();
				writer.write(shortcut[2]);
				writer.newLine();
			}
		} catch (Throwable t) {
			tt = t;
		}
		try {
			if (writer != null)
				writer.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (tt != null)
			if (tt instanceof IOException)
				throw (IOException) tt;
			else
				throw new IOException(tt);
	}

	public static void addShortcut(File projectRoot, String faTag, String description, String command) throws IOException {
		File shortcuts = new File(projectRoot, "shortcuts");
		BufferedWriter writer = null;
		Throwable tt = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(shortcuts, true)));
			writer.write(faTag);
			writer.newLine();
			writer.write(description);
			writer.newLine();
			writer.write(command);
			writer.newLine();
		} catch (Throwable t) {
			tt = t;
		}
		try {
			if (writer != null)
				writer.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (tt != null)
			if (tt instanceof IOException)
				throw (IOException) tt;
			else
				throw new IOException(tt);
	}

	public static boolean clearShortcuts(File projectRoot) {
		return new File(projectRoot, "shortcuts").delete();
	}

	public static void updateShortcut(File projectRoot, int index, String faTag, String description, String command) throws IOException {
		String[][] shortcuts = listShortcuts(projectRoot);
		if (shortcuts.length > index) {
			shortcuts[index][0] = faTag;
			shortcuts[index][1] = description;
			shortcuts[index][2] = command;
			putShortcuts(projectRoot, shortcuts);
		}
	}
	
	public static void moveShortcut(File projectRoot, int index, int steps) throws IOException {
		String[][] shortcuts = listShortcuts(projectRoot);
		String[] a = shortcuts[index];
		String[] b = shortcuts[index + steps];
		shortcuts[index] = b;
		shortcuts[index + steps] = a;
		putShortcuts(projectRoot, shortcuts);
	}

	public static void removeShortcut(File projectRoot, int index) throws IOException {
		String[][] shortcuts = listShortcuts(projectRoot);
		if (shortcuts.length > index) {
			String[][] newShortcuts = new String[shortcuts.length - 1][3];
			System.arraycopy(shortcuts, 0, newShortcuts, 0, index);
			System.arraycopy(shortcuts, index + 1, newShortcuts, index, shortcuts.length - index - 1);
			putShortcuts(projectRoot, newShortcuts);
		}
	}

	public boolean isProject() {
		return projectRoot != null;
	}

	public File getProjectRoot() {
		return projectRoot;
	}

	public File getFileDir(String identifier) {
		return new File(root, identifier);
	}

	public File getContentDir(String identifier) {
		return new File(getFileDir(identifier), "content");
	}

	public String getContentName(String identifier) throws IOException {
		return loadMapping(identifier)[0];
	}

	public File getContentFile(String identifier) throws IOException {
		return new File(getContentDir(identifier), getContentName(identifier));
	}

	public File getRemoteDir(String identifier) throws IOException {
		return new File(loadMapping(identifier)[1]);
	}

	public File getRemoteFile(String identifier) throws IOException {
		String[] mapping = loadMapping(identifier);
		return new File(mapping[1], mapping[0]);
	}

	private void putMapping(String identifier, String fileName, String remoteFilePath) throws IOException {
		File config = new File(getFileDir(identifier), "config");
		BufferedWriter writer = null;
		Throwable tt = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config, false)));
			writer.write(fileName);
			writer.newLine();
			writer.write(remoteFilePath);
		} catch (Throwable t) {
			tt = t;
		}
		try {
			if (writer != null)
				writer.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (tt != null)
			if (tt instanceof IOException)
				throw (IOException) tt;
			else
				throw new IOException(tt);
	}

	private String[] loadMapping(String identifier) throws IOException {
		File config = new File(getFileDir(identifier), "config");
		BufferedReader reader = null;
		Throwable tt = null;
		String[] result = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
			result = new String[]{
					reader.readLine(),
					reader.readLine()
			};
		} catch (Throwable t) {
			tt = t;
		}
		try {
			if (reader != null)
				reader.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (tt != null)
			if (tt instanceof IOException)
				throw (IOException) tt;
			else
				throw new IOException(tt);
		return result;
	}

	public String open(File remoteFile) {
		String identifier;
		File fileDir;
		do {
			identifier = String.valueOf(System.currentTimeMillis());
		} while ((fileDir = getFileDir(identifier)).exists());
		if (init(identifier, fileDir, remoteFile))
			return identifier;
		closeFile(identifier);
		return null;
	}

	private boolean init(String identifier, File fileDir, File remoteFile) {
		try {
			if (!fileDir.mkdirs())
				return false;
			File contentDir = new File(fileDir, "content");
			if (!contentDir.mkdir())
				return false;
			File contentFile = new File(contentDir, remoteFile.getName());
			if (!contentFile.createNewFile())
				return false;
			if (!new File(fileDir, "config").createNewFile())
				return false;
			putMapping(identifier, remoteFile.getName(), remoteFile.getParent());
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean closeFile(String identifier) {
		return recursiveDelete(getFileDir(identifier));
	}

	public FileInputStream openInputStream(String identifier) throws IOException {
		return new FileInputStream(getContentFile(identifier));
	}

	public FileOutputStream openOutputStream(String identifier) throws IOException {
		return new FileOutputStream(getContentFile(identifier));
	}

	public String[] listIdentifiers() {
		String[] identifiers = root.list();
		return identifiers == null ? new String[0] : identifiers;
	}

	private static boolean recursiveDelete(File file) {
		if (file.isDirectory())
			for (File child : file.listFiles())
				if (!recursiveDelete(child))
					return false;
		return file.delete();
	}

}
