package uk.org.ngo.squeezer.itemlist.action;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.MusicFolderItem;
import uk.org.ngo.squeezer.model.Song;

public class AskAction extends PlayableItemAction {

    public AskAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            return;
        }

        if (item instanceof Album) {
            AskForActionDialog.showDialog(activity, item, R.string.settings_album_selectaction, ALBUM_ACTIONS);
        } else if (item instanceof Song || item instanceof MusicFolderItem) {
            AskForActionDialog.showDialog(activity, item, R.string.settings_song_selectaction, SONG_ACTIONS);
        }

    }

    public static class AskForActionDialog extends DialogFragment {
        ItemListActivity activity;
        int selection = 0;

        @TargetApi(Build.VERSION_CODES.FROYO)
        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (ItemListActivity) getActivity();
            final PlaylistItem item = getArguments().getParcelable("item");
            final int titleId = getArguments().getInt("titleId");
            final String[] typeNames = getArguments().getStringArray("typeNames");
            final Type[]types = new Type[typeNames.length];
            final String[] typeStrings = new String[typeNames.length];
            for (int i = 0; i < typeNames.length; i++) {
                types[i] = Type.valueOf(typeNames[i]);
                typeStrings[i] = types[i].getText(activity);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(titleId);
            builder.setSingleChoiceItems(typeStrings, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selection = which;
                    ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
                    ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                }
            });
            builder.setNegativeButton(R.string.execute_just_once, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    executeSelection(types[selection], item);
                }
            });
            builder.setPositiveButton(R.string.execute_always, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Type type = types[selection];
                    executeSelection(type, item);
                    new Preferences(activity).setOnSelectItemAction(item.getClass(), type);
                }
            });

            final AlertDialog dialog = builder.create();

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                    ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                }
            });

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            getDialog().show();
        }

        private void executeSelection(Type type, PlaylistItem item) {
            if (type != Type.NONE) {
                createAction(activity, type).execute(item);
            }
        }

        public static void showDialog(ItemListActivity activity, PlaylistItem item, int titleId, Type[] types) {
            final List<String> typeNames = new ArrayList<>();
            for (Type type : types) {
                if (type != Type.NONE) typeNames.add(type.name());
            }

            Bundle args = new Bundle();
            args.putParcelable("item", item);
            args.putInt("titleId", titleId);
            args.putStringArray("typeNames", typeNames.toArray(new String[typeNames.size()]));
            final AskForActionDialog dialog = new AskForActionDialog();
            dialog.setArguments(args);
            dialog.show(activity.getSupportFragmentManager(), AskForActionDialog.class.getSimpleName());
        }
    }
}
