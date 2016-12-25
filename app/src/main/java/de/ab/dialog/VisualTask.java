package de.ab.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import de.ab.reflect.R;

public class VisualTask<CommandType extends VisualTask.Command> extends AsyncTask<CommandType, Integer, VisualTask.Result[]> {

	private final Object handle = new Object();
	private Context context;
	private ProgressDialog progress;
	private CharSequence message;

	public VisualTask(Context context) {
		this.context = context;
	}

	public static Command newCommand(Runnable action) {
		return newCommand(action, null);
	}

	public static Command newCommand(Runnable action, Notifier<Command> notifier) {
		class CommandImpl implements Command {
			private Runnable action;
			private Notifier<Command> notifier;

			public CommandImpl(Runnable action, Notifier<Command> notifier) {
				this.action = action;
				this.notifier = notifier;
			}

			@Override
			public void execute(VisualTask<? extends Command> task) throws Throwable {
				action.run();
			}

			@Override
			public Notifier<Command> getNotifier() {
				return notifier;
			}
		}
		return new CommandImpl(action, notifier);
	}

	protected static Result newResult(Command command) {
		return newResult(command, true, null);
	}

	protected static Result newResult(Command command, Throwable failureCause) {
		return newResult(command, false, failureCause);
	}

	protected static Result newResult(Command command, boolean wasSuccessful, Throwable failureCause) {
		class ResultImpl implements Result {
			private Command command;
			private boolean wasSuccessful;
			private Throwable failureCause;

			public ResultImpl(Command command, boolean wasSuccessful, Throwable failureCause) {
				this.command = command;
				this.wasSuccessful = wasSuccessful;
				this.failureCause = failureCause;
			}

			@Override
			public Command getCommand() {
				return command;
			}

			@Override
			public boolean wasSuccessful() {
				return wasSuccessful;
			}

			@Override
			public Throwable getFailureCause() {
				return failureCause;
			}
		}
		return new ResultImpl(command, wasSuccessful, failureCause);
	}

	public Context getContext() {
		return context;
	}

	protected CharSequence getDefaultDialogMessage(Context context) {
		return context.getString(R.string.progressing);
	}

	public CharSequence getDialogMessage() {
		return message == null ? getDefaultDialogMessage(getContext()) : message;
	}

	public void setDialogMessage(CharSequence message) {
		this.message = message;
		if (progress != null)
			synchronized (handle) {
				if (progress != null)
					progress.setMessage(getDialogMessage());
			}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (progress == null || !progress.isShowing())
			synchronized (handle) {
				if (progress == null || !progress.isShowing()) {
					progress = new ProgressDialog(getContext());
					progress.setMessage(getDialogMessage());
					progress.setIndeterminate(true);
					progress.setCancelable(false);
					progress.setCanceledOnTouchOutside(false);
					progress.show();
				}
			}
	}

	@Override
	protected Result[] doInBackground(Command[] cmds) {
		int progress = 0;
		Result results[] = new Result[cmds.length];
		publishProgress(progress++, cmds.length);

		for (int i = 0; i < cmds.length; i++) {
			Command cmd = cmds[i];
			try {
				cmd.execute(this);
				results[i] = newResult(cmd);
			} catch (Throwable t) {
				results[i] = newResult(cmd, t);
			} finally {
				publishProgress(progress++, cmds.length);
			}
		}
		return results;
	}

	@Override
	protected void onProgressUpdate(Integer[] values) {
		super.onProgressUpdate(values);
		if (progress != null && progress.isShowing())
			synchronized (handle) {
				if (progress != null && progress.isShowing()) {
					progress.setMax(values[1]);
					progress.setProgress(values[0]);
				}
			}
	}

	@Override
	protected void onPostExecute(Result[] results) {
		super.onPostExecute(results);
		if (progress != null && progress.isShowing())
			synchronized (handle) {
				if (progress != null && progress.isShowing())
					progress.hide();
			}
		for (Result result : results) {
			CommandType cmd = (CommandType) result.getCommand();
			Notifier<CommandType> notifier = (VisualTask.Notifier<CommandType>) cmd.getNotifier();
			if (notifier != null)
				if (result.wasSuccessful())
					notifier.onSuccess(this, cmd);
				else
					notifier.onFailure(this, cmd, result.getFailureCause());
		}
	}

	public interface Command {
		void execute(VisualTask<? extends Command> task) throws Throwable;

		@Nullable
		Notifier<? extends Command> getNotifier();
	}

	public interface Notifier<CommandType extends Command> {
		void onSuccess(VisualTask<CommandType> task, CommandType command);

		void onFailure(VisualTask<CommandType> task, CommandType command, Throwable cause);
	}

	public interface Result {
		Command getCommand();

		boolean wasSuccessful();

		Throwable getFailureCause();
	}

}
