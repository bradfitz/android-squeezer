package uk.org.ngo.squeezer.service;

public enum IRButton {
    playPreset_1("playPreset_1"),
    playPreset_2("playPreset_2"),
    playPreset_3("playPreset_3"),
    playPreset_4("playPreset_4"),
    playPreset_5("playPreset_5"),
    playPreset_6("playPreset_6");

    private String function;

    IRButton(String function) {
        this.function = function;
    }

    public String getFunction() {
        return function;
    }
}
