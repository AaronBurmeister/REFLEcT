package de.ab.reflect.sftp;

import android.content.Context;
import android.support.annotation.Nullable;

import de.ab.dialog.VisualTask;
import de.ab.reflect.R;
import de.ab.reflect.ssh.SSHSession;

public class SftpTask extends VisualTask<SftpCommand> {

	public static final
	@SftpDirection
	int DIRECTION_PUSH = 0, DIRECTION_PULL = 1;
	private SSHSession session;

	public SftpTask(Context context, SSHSession session) {
		super(context);
		this.session = session;
	}

	public static SftpCommand transferFiles(@SftpDirection int direction, String origin, String destination, Notifier<? extends SftpCommand> notifier) {
		class SftpCommandImpl implements SftpCommand {
			private
			@SftpDirection
			int direction;
			private String origin, destination;
			private Notifier<? extends SftpCommand> notifier;

			public SftpCommandImpl(@SftpDirection int direction, String origin, String destination, Notifier<? extends SftpCommand> notifier) {
				this.direction = direction;
				this.origin = origin;
				this.destination = destination;
				this.notifier = notifier;
			}

			@Override
			public void execute(VisualTask<? extends Command> task) throws Throwable {
				SSHSession session = ((SftpTask) task).session;
				switch (direction) {
					case DIRECTION_PUSH:
						session.pushFile(origin, destination);
						break;
					case DIRECTION_PULL:
						session.pullFile(origin, destination);
						break;
					default:
						throw new IllegalArgumentException("Invalid direction " + direction);
				}
			}

			@Nullable
			@Override
			public Notifier<? extends SftpCommand> getNotifier() {
				return notifier;
			}
		}

		return new SftpCommandImpl(direction, origin, destination, notifier);
	}

	public static SftpCommand mkdir(String path, Notifier<? extends SftpCommand> notifier) {
		class SftpCommandImpl implements SftpCommand {
			private String path;
			private Notifier<? extends SftpCommand> notifier;

			public SftpCommandImpl(String path, Notifier<? extends SftpCommand> notifier) {
				this.path = path;
				this.notifier = notifier;
			}

			@Override
			public void execute(VisualTask<? extends Command> task) throws Throwable {
				((SftpTask) task).session.mkdir(path);
			}

			@Nullable
			@Override
			public Notifier<? extends SftpCommand> getNotifier() {
				return notifier;
			}
		}

		return new SftpCommandImpl(path, notifier);
	}
	
	@Override
	protected CharSequence getDefaultDialogMessage(Context context) {
		return context.getString(R.string.talking_with_server);
	}

}
