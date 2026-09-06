package net.metrodata.autoelytra.core;

final class FireworkInfo {
    final int slot;
    final int count;
    final int flightDuration;
    final boolean explosive;

    FireworkInfo(int slot, int count, int flightDuration, boolean explosive) {
        this.slot = slot;
        this.count = count;
        this.flightDuration = flightDuration;
        this.explosive = explosive;
    }
}
