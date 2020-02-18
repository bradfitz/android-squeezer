package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import java.util.Calendar;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.Item;

public class InputTimeDialog extends TimePickerDialog implements TimePickerDialog.OnTimeSetListener {
    private BaseListActivity activity;
    private Item item;
    private int alreadyPopped;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseListActivity) getActivity();
        item = getArguments().getParcelable(Item.class.getName());
        alreadyPopped = getArguments().getInt("alreadyPopped", 0);
        setOnTimeSetListener(this);
        return super.onCreateDialog(savedInstanceState);
    }

    public static void show(BaseActivity activity, Item item, int alreadyPopped) {
        int hour;
        int minute;
        try {
            int tod = Integer.parseInt(item.input.initialText);
            hour = tod / 3600;
            minute = (tod / 60) % 60;
        } catch (NumberFormatException nfe) {
            // Fall back to current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }

        InputTimeDialog dialog = new InputTimeDialog();

        Bundle args = new Bundle();
        args.putParcelable(Item.class.getName(), item);
        args.putInt("alreadyPopped", alreadyPopped);
        dialog.setArguments(args);

        dialog.initialize(dialog, hour, minute, DateFormat.is24HourFormat(activity));
        dialog.setThemeDark(activity.getThemeId() == R.style.AppTheme);
        dialog.show(activity.getSupportFragmentManager(), InputTimeDialog.class.getSimpleName());
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        item.inputValue = String.valueOf((hourOfDay * 60 + minute) * 60);
        activity.action(item, item.goAction, alreadyPopped);
    }
}
