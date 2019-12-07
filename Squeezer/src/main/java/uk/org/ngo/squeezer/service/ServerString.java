package uk.org.ngo.squeezer.service;

public enum ServerString {
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
    ALARM,
    ALARM_ALARM_REPEAT,
    ALARM_SHORT_DAY_0,
    ALARM_SHORT_DAY_1,
    ALARM_SHORT_DAY_2,
    ALARM_SHORT_DAY_3,
    ALARM_SHORT_DAY_4,
    ALARM_SHORT_DAY_5,
    ALARM_SHORT_DAY_6,
    ALARM_DELETING,
    ALARM_ALL_ALARMS,
    MORE,
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
