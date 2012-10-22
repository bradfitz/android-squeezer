package uk.org.ngo.squeezer.service;

public enum SqueezerServerString {
    ALBUM,
    BROWSE_NEW_MUSIC,
    SORT_ARTISTYEARALBUM,
    SORT_ARTISTALBUM,
    SORT_YEARALBUM,
    SORT_YEARARTISTALBUM;

    private String localizedString;

    /**
     * @return The localized string or just the name of the token, if not yet fetched (unlikely).
     */ 
    public String getLocalizedString() {
        return localizedString != null ? localizedString : name();
    }

    /**
     * Set the localized string for this token, as fetched from the server.
     * @param localizedString
     */
    public void setLocalizedString(String localizedString) {
        this.localizedString = localizedString;
    }

}
