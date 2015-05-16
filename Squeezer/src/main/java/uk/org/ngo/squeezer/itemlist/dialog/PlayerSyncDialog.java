/*
 * Copyright (c) 2014 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.model.Player;

/**
 * A dialog that shows sync group options -- either joining an existing group, or
 * removing a player from a sync group. The hosting activity must implement
 * {@link PlayerSyncDialogHost} to provide the information that the dialog needs.
 */
public class PlayerSyncDialog extends DialogFragment {
    /**
     * Activities that host this dialog must implement this interface.
     */
    public interface PlayerSyncDialogHost {
        Multimap<String, Player> getPlayerSyncGroups();
        Player getCurrentPlayer();
        void syncPlayerToPlayer(@NonNull Player slave, @NonNull String masterId);
        void unsyncPlayer(@NonNull Player player);
    }

    private PlayerSyncDialogHost mHost;

    /** The sync group the user selected. */
    private int mSelectedGroup = 0;

    // Override the Fragment.onAttach() method to instantiate the PlayerSyncDialogHost.
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the PlayerSyncDialogHost.
            mHost = (PlayerSyncDialogHost) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PlayerSyncDialogHost");
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Multimap<String, Player> playerSyncGroups = mHost.getPlayerSyncGroups();
        final Player currentPlayer = mHost.getCurrentPlayer();

        /** The names of each sync group. */
        List<String> playerSyncGroupNames = new ArrayList<String>();

        /**
         * The master ID of the player for each sync group. Indices in this correspond
         * 1:1 with {@link playerSyncGroupNames}.
         */
        final List<String> playerSyncGroupMasterIds = new ArrayList<String>();

        // Build the list of sync groups to show the user.

        // Collect and sort the master IDs.
        List<String> masterIds = new ArrayList<String>(playerSyncGroups.keySet());
        Collections.sort(masterIds);

        // Generate descriptive text for each sync group.
        for (String masterId : masterIds) {
            // Do not show an entry for the group the current player is in.  That might be...
            // 1. Because it's the master of this group, or...
            if (masterId.equals(currentPlayer.getId()))
                continue;

            // 2. Because it's a member of this group.
            if (masterId.equals(currentPlayer.getPlayerState().getSyncMaster()))
                continue;

            // Collect the player names and master ID for this sync group.
            List<String> playerNames = new ArrayList<String>();
            List<Player> slaves = new ArrayList<Player>(playerSyncGroups.get(masterId));
            Collections.sort(slaves, Player.compareById);

            for (Player slave : slaves) {
                playerNames.add(slave.getName());
            }
            playerSyncGroupNames.add(Joiner.on(", ").join(playerNames));
            playerSyncGroupMasterIds.add(masterId);
        }

        // Add an additional entry for the "No synchronisation" option.
        playerSyncGroupNames.add(getString(R.string.menu_item_player_unsync));

        ArrayAdapter<String> playerSyncGroupAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_single_choice, playerSyncGroupNames);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.sync_title, currentPlayer.getName()))
                .setSingleChoiceItems(playerSyncGroupAdapter, mSelectedGroup,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSelectedGroup = which;
                            }
                        })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The "No synchronisation" option is always last.
                        if (mSelectedGroup == playerSyncGroupMasterIds.size()) {
                            mHost.unsyncPlayer(currentPlayer);
                        } else {
                            mHost.syncPlayerToPlayer(currentPlayer,
                                    playerSyncGroupMasterIds.get(mSelectedGroup));
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog, nothing to do.
                    }
                });
        return builder.create();
    }
}
