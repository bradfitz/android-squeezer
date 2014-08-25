/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.itemlist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.DisabledItemListAdapter;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Alarm;
import uk.org.ngo.squeezer.model.AlarmPlaylist;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerPref;
import uk.org.ngo.squeezer.service.IServicePlayerPrefCallback;
import uk.org.ngo.squeezer.service.IServicePlayersCallback;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.util.CompoundButtonWrapper;

public class AlarmsActivity extends BaseListActivity<Alarm> {
    private Player player;
    private AlarmView alarmView;
    private CompoundButtonWrapper alarmsEnabledButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        alarmsEnabledButton = new CompoundButtonWrapper((CompoundButton) findViewById(R.id.alarms_enabled));
        findViewById(R.id.all_alarms_desc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(AlarmsActivity.this, ServerString.ALARM_ALARMS_ENABLED_DESC.getLocalizedString(), Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.add_alarm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment.show(getSupportFragmentManager());
            }
        });
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlarmsSettingsActivity.show(AlarmsActivity.this);
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        alarmsEnabledButton.setOncheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getService().playerPref(PlayerPref.alarmsEnabled, isChecked ? "1" : "0");
            }
        });
    }

    public static void show(Activity context) {
        final Intent intent = new Intent(context, AlarmsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getContentView() {
        return R.layout.item_list_player_alarms;
    }

    @Override
    public ItemView<Alarm> createItemView() {
        alarmView = new AlarmView(this);
        return alarmView;
    }

    @Override
    protected ItemAdapter<Alarm> createItemListAdapter(ItemView<Alarm> itemView) {
        return new DisabledItemListAdapter<Alarm>(itemView, getImageFetcher());
    }

    @Override
    protected void registerCallback() {
        super.registerCallback();
        player = getService().getActivePlayer();
        getService().registerPlayerPrefCallback(playerPrefCallback);
        getService().registerPlayersCallback(playersCallback);
        getService().alarmPlaylists(-1, alarmPlaylistsCallback);
    }

    @Override
    protected void orderPage(int start) {
        getService().alarms(start, this);
        if (start == 0) getService().playerPref(PlayerPref.alarmsEnabled);
    }

    private List<AlarmPlaylist> alarmPlaylists = new ArrayList<AlarmPlaylist>();
    private final IServiceItemListCallback<AlarmPlaylist> alarmPlaylistsCallback = new IServiceItemListCallback<AlarmPlaylist>() {
        @Override
        public void onItemsReceived(final int count, final int start, Map<String, String> parameters, final List<AlarmPlaylist> items, Class<AlarmPlaylist> dataType) {
            alarmPlaylists.addAll(items);
            if (start + items.size() >= count) {
                getUIThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        alarmView.setAlarmPlaylists(alarmPlaylists);
                        getItemAdapter().notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };

    private final IServicePlayerPrefCallback playerPrefCallback = new IServicePlayerPrefCallback() {
        @Override
        public void onPlayerPrefReceived(final PlayerPref playerPref, final String value) {
            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (playerPref == PlayerPref.alarmsEnabled) {
                        alarmsEnabledButton.setChecked(Integer.valueOf(value) > 0);
                    }
                }
            });
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };

    private final IServicePlayersCallback playersCallback = new IServicePlayersCallback() {
        @Override
        public void onPlayersChanged(final List<Player> players, final Player activePlayer) {
            if (activePlayer != null && !activePlayer.equals(player)) {
                getUIThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        player = activePlayer;
                        clearAndReOrderItems();
                    }
                });
            }
        }

        @Override
        public Object getClient() {
            return AlarmsActivity.this;
        }
    };

    public static class TimePickerFragment extends DialogFragment {
        BaseListActivity activity;

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (BaseListActivity) getActivity();

            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final TimePicker timePicker = new TimePicker(getActivity());
            timePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
            builder.setView(timePicker);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.getService().alarmAdd((timePicker.getCurrentHour() * 60 + timePicker.getCurrentMinute()) * 60);
                    activity.clearAndReOrderItems();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            return builder.create();
        }

        public static void show(FragmentManager manager) {
            TimePickerFragment fragment = new TimePickerFragment();
            fragment.show(manager, TimePickerFragment.class.getSimpleName());
        }
    }

}
