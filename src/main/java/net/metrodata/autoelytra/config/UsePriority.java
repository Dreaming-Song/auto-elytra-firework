package net.metrodata.autoelytra.config;

public enum UsePriority {
    FLIGHT_LONG_FIRST("text.autoelytra.priority.flightLong"),
    FLIGHT_SHORT_FIRST("text.autoelytra.priority.flightShort"),
    STACK_LARGE_FIRST("text.autoelytra.priority.stackLarge"),
    STACK_SMALL_FIRST("text.autoelytra.priority.stackSmall"),
    HOTBAR_FIRST("text.autoelytra.priority.hotbar");

    public final String labelKey;

    UsePriority(String labelKey) {
        this.labelKey = labelKey;
    }
}
