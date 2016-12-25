package de.ab.reflect.sftp;

import android.content.Context;

import com.jcraft.jsch.SftpException;

import de.ab.reflect.R;

public class SftpErrors {

	public static String getSftpErrorMessage(Context context, Throwable cause) {
		if (cause instanceof SftpException) {
			String errorCode = String.valueOf(((SftpException) cause).id);
			int descriptionIdentifier = context.getResources().getIdentifier("sftp_error_code_" + errorCode, "string", context.getPackageName());
			return context.getString(R.string.sftp_error, errorCode, descriptionIdentifier == 0 ? "internal error" : context.getString(descriptionIdentifier));
		} else
			return null;
	}

}
