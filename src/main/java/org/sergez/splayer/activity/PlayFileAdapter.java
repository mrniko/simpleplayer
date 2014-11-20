package org.sergez.splayer.activity;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import org.sergez.splayer.R;
import org.sergez.splayer.util.DurationAlbumID;
import org.sergez.splayer.util.FileFormat;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Sergii Zhuk
 *         Date: 15.03.14
 *         Time: 23:53
 */
public class PlayFileAdapter extends ArrayAdapter<String> {
	private Map<String, DurationAlbumID> itemsMap;
	private SimplePlayerActivity simplePlayerActivity;

	public PlayFileAdapter(SimplePlayerActivity simplePlayerActivity, int textViewResourceId, Map<String, DurationAlbumID> item) {
		super(simplePlayerActivity, textViewResourceId, new ArrayList<String>(item.keySet()));
		this.simplePlayerActivity = simplePlayerActivity;
		this.itemsMap = item;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		ViewHolder holder;
		if (rowView == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = vi.inflate(R.layout.row, null);
			holder = new ViewHolder();
			holder.imageIcon = (ImageView) rowView.findViewById(R.id.icon);
			holder.imagePlaying = (ImageView) rowView.findViewById(R.id.image_playing);
			holder.textDuration = (TextView) rowView.findViewById(R.id.text_track_duration);
			holder.textTop = (TextView) rowView.findViewById(R.id.text_toptext);
			holder.relativeLayoutRow = (RelativeLayout) rowView.findViewById(R.id.layout_rows);
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		String fileItem = getItem(position);// itemsKeyList.get(position);
		if (fileItem != null) {

			holder.textTop.setText(fileItem);
			if (simplePlayerActivity.uiStateController.isBigFont()) {
				holder.textTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26); // TODO move font to const
			}
			if (FileFormat.acceptableFormat(fileItem)) { // playable file
				DurationAlbumID durationAlbumID = itemsMap.get(fileItem);
				String duration = durationAlbumID.duration;
				String albumArtID = String.valueOf(durationAlbumID.albumID);
				holder.textDuration.setText(duration);
				if (!simplePlayerActivity.uiStateController.isShowFileExtension()) {
					holder.textTop.setText(fileItem.substring(0, fileItem.lastIndexOf(".")));
				}
				if ((simplePlayerActivity.playerService != null) &&
						(simplePlayerActivity.playerService.getCurrentlyPlayingFilePath().equals(simplePlayerActivity.listData.getCurrentPath() + "/" + fileItem))) { //TODO const
					//current file is playing . Show playing icon on the right side of the row
					holder.imagePlaying.setVisibility(View.VISIBLE);
					holder.textDuration.setVisibility(View.GONE);
					//setBackgroundDrawable - otherwise not working on Android 2.*
					holder.relativeLayoutRow.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.listview_bg_playing));
				} else {
					holder.imagePlaying.setVisibility(View.GONE);
					holder.textDuration.setVisibility(View.VISIBLE);
					//setBackgroundDrawable - otherwise not working on Android 2.*
					holder.relativeLayoutRow.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.listview_bg_selector));
				}
                Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                Uri uri = ContentUris.withAppendedId(sArtworkUri, Long.parseLong(albumArtID));
                Picasso.with(getContext()).load(uri).placeholder(R.drawable.ic_launcher)
                        .error(R.drawable.ic_launcher).into(holder.imageIcon);
				holder.textTop.setTypeface(null, Typeface.NORMAL);
			} else { // folder
				// check if this folder is folder where now file
				// being playing
				String filePath = "";
				if (simplePlayerActivity.playerService != null) {
					filePath = simplePlayerActivity.playerService.getCurrentlyPlayingFilePath();
				}
				if (filePath.startsWith(simplePlayerActivity.listData.getCurrentPathItemsFullpath(position))
						&& ((position > 0) || (simplePlayerActivity.listData.getCurrentPath().equals(simplePlayerActivity.listData.getRoot())))) {
					holder.imagePlaying.setVisibility(View.VISIBLE);
				} else {
					holder.imagePlaying.setVisibility(View.GONE);
				}
				holder.textDuration.setText("");
				holder.imageIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_menu_archive));
				holder.textTop.setTypeface(null, Typeface.BOLD);
			}
		}
		return rowView;
	}

	static class ViewHolder {
		ImageView imageIcon;
		ImageView imagePlaying;
		TextView textTop;
		TextView textDuration;
		RelativeLayout relativeLayoutRow;
	}
}
