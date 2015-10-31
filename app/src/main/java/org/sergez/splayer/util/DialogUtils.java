package org.sergez.splayer.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.MenuItem;

import org.sergez.splayer.R;
import org.sergez.splayer.enums.RepeatState;
import org.sergez.splayer.enums.ShuffleState;
import org.sergez.splayer.service.SimplePlayerService;

import java.io.File;

import static org.sergez.splayer.util.Utils.makeToast;

/**
 * @author Sergii Zhuk
 *         Date: 16.03.14
 *         Time: 9:46
 */
public class DialogUtils {

	private static int dialogRepeatSelectedItem = -1;

	private static int dialogShuffleSelectedItem = -1;

	// TODO : do not pass service here
	public static void showDialogRepeatChoice(final Context context, final SimplePlayerService playerService,
	                                          final MenuItem menuItemRepeat) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		CharSequence[] array = {RepeatState.NO_REPEAT.getLabel(), RepeatState.REPEAT_CURRENT_TRACK.getLabel(),
				RepeatState.REPEAT_ALL_FILES.getLabel()};

		builder.setTitle("Select repeat mode") // TODO const
				.setSingleChoiceItems(array, playerService.getRepeatState().ordinal(), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialogRepeatSelectedItem = which;
					}
				})
				.setPositiveButton("OK", new DialogInterface.OnClickListener() { // TODO const
					@Override
					public void onClick(DialogInterface dialog, int id) {
						if (dialogRepeatSelectedItem > -1) {
							RepeatState selectedState = RepeatState.values()[dialogRepeatSelectedItem];
							playerService.setRepeatState(selectedState);
							menuItemRepeat.setTitle(context.getString(R.string.button_and_toast_repeat_first_part) + " " + selectedState.getLabel());
							makeToast(context, context.getString(R.string.button_and_toast_repeat_first_part) + " " + selectedState.getLabel());
							dialogRepeatSelectedItem = -1;
						}

					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  // TODO const
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// empty
					}
				});

		builder.create().show();
	}


	public static void showDialogShuffleChoice(final Context context, final SimplePlayerService playerService,
	                                           final MenuItem menuItemShuffle) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		CharSequence[] array = {ShuffleState.SHUFFLE_OFF.getLabel(), ShuffleState.SHUFFLE_ON.getLabel()};

		builder.setTitle("Select shuffle mode") // TODO const

				.setSingleChoiceItems(array, playerService.getShuffleState().ordinal(), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialogShuffleSelectedItem = which;
					}
				})
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						if (dialogShuffleSelectedItem > -1) {
							ShuffleState selectedState = ShuffleState.values()[dialogShuffleSelectedItem];
							playerService.setShuffleState(selectedState);
							menuItemShuffle.setTitle(context.getString(R.string.button_and_toast_shuffle_first_part) + " " + selectedState.getLabel());
							makeToast(context, context.getString(R.string.button_and_toast_shuffle_first_part) + " " + selectedState.getLabel());
							dialogShuffleSelectedItem = -1;
						}
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// empty
					}
				});

		builder.create().show();
	}

	public static void showFileCantBePlayed(Context context, String filePath) {
		if (TextUtils.isEmpty(filePath)) {
			new AlertDialog.Builder(context)
					.setTitle(R.string.app_name)
					.setMessage(R.string.file_cant_be_played_refresh)
					.setPositiveButton(R.string.ok, null).show();
		} else {
			new AlertDialog.Builder(context)
					.setTitle(R.string.app_name)
					.setMessage(context.getString(R.string.file_cant_be_played_refresh_part1) + filePath
							+ context.getString(R.string.file_cant_be_played_refresh_part2))
					.setPositiveButton(R.string.ok, null).show();
		}
	}

	public static void showFileCantBePlayed(Context context, File file) {
		showFileCantBePlayed(context, file.getAbsolutePath());
	}

	public static void showFolderCantBeRead(Context context, String folderName) {
		new AlertDialog.Builder(context)
				.setTitle(R.string.app_name)
				.setMessage(context.getString(R.string.folder_cant_read_part1) + folderName
						+ context.getString(R.string.folder_cant_read_part2))
				.setPositiveButton(R.string.ok, null).show();
	}

	public static void showFolderCantBeRead(Context context, File file) {
		showFolderCantBeRead(context, file.getName());
	}

}
