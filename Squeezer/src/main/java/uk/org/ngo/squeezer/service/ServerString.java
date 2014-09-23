package uk.org.ngo.squeezer.service;

public enum ServerString {
    ALBUM,
    BROWSE_NEW_MUSIC,
    SORT_ARTISTYEARALBUM,
    SORT_ARTISTALBUM,
    SORT_YEARALBUM,
    SORT_YEARARTISTALBUM,
    REPEAT_OFF,
    REPEAT_ONE,
    REPEAT_ALL,
    SHUFFLE_OFF,
    SHUFFLE_ON_SONGS,
    SHUFFLE_ON_ALBUMS,
    SWITCH_TO_EXTENDED_LIST,
    SWITCH_TO_GALLERY,
    ALBUM_DISPLAY_OPTIONS,
    SLEEP,
    SLEEP_CANCEL,
    X_MINUTES,
    SLEEPING_IN,
    SLEEP_AT_END_OF_SONG,
    VOLUME,
    ALARM,
    ALARM_ALARM_REPEAT,
    ALARM_ALARM_REPEAT_DESC,
    ALARM_SHORT_DAY_0,
    ALARM_SHORT_DAY_1,
    ALARM_SHORT_DAY_2,
    ALARM_SHORT_DAY_3,
    ALARM_SHORT_DAY_4,
    ALARM_SHORT_DAY_5,
    ALARM_SHORT_DAY_6,
    ALARM_VOLUME,
    ALARM_VOLUME_DESC,
    ALARM_ADD,
    ALARM_ADD_DESC,
    ALARM_SAVING,
    ALARM_DELETING,
    ALARM_ALL_ALARMS,
    ALARM_ALARMS_ENABLED_DESC,
    ALARM_FADE,
    SETUP_ALARM_TIMEOUT,
    SETUP_ALARM_TIMEOUT_DESC,
    SETUP_SNOOZE_MINUTES,
    SETUP_SNOOZE_MINUTES_DESC,
    ;

    private String localizedString;

    /**
     * @return The localized string or just the name of the token, if not yet fetched (unlikely).
     */
    public String getLocalizedString() {
        return localizedString != null ? localizedString : name();
    }

    /**
     * Set the localized string for this token, as fetched from the server.
     *
     * @param localizedString
     */
    public void setLocalizedString(String localizedString) {
        this.localizedString = localizedString;
    }

    public static String getAlarmShortDayText(int day) {
        return ServerString.values()[(ALARM_SHORT_DAY_0.ordinal() + day)].getLocalizedString();
    }
}
