package net.metrodata.autoelytra.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.metrodata.autoelytra.config.ConfigData;
import net.metrodata.autoelytra.config.ModConfig;
import net.metrodata.autoelytra.config.UsePriority;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoElytraConfigScreen {
    private AutoElytraConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigData config = ModConfig.INSTANCE;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(text("text.autoelytra.title"))
                .setSavingRunnable(ModConfig::save);
        ConfigEntryBuilder entry = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(text("text.autoelytra.general"));
        general.addEntry(entry.startBooleanToggle(text("text.autoelytra.enabled"), config.enabled)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.enabled.tooltip"))
                .setSaveConsumer(v -> config.enabled = v)
                .build());

        general.addEntry(entry.startBooleanToggle(text("text.autoelytra.jump"), config.respondToVanillaJump)
                .setDefaultValue(false)
                .setTooltip(text("text.autoelytra.jump.tooltip"))
                .setSaveConsumer(v -> config.respondToVanillaJump = v)
                .build());

        general.addEntry(entry.startBooleanToggle(text("text.autoelytra.skipGlideStart"), config.skipBoostOnGlideStart)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.skipGlideStart.tooltip"))
                .setSaveConsumer(v -> config.skipBoostOnGlideStart = v)
                .build());

        general.addEntry(entry.startIntSlider(text("text.autoelytra.cooldown"), config.minTicksBetweenUses, 1, 60)
                .setDefaultValue(10)
                .setTextGetter(value -> text("text.autoelytra.ticks", value))
                .setTooltip(text("text.autoelytra.cooldown.tooltip"))
                .setSaveConsumer(v -> config.minTicksBetweenUses = v)
                .build());

        ConfigCategory selection = builder.getOrCreateCategory(text("text.autoelytra.selection"));

        selection.addEntry(entry.startBooleanToggle(text("text.autoelytra.plain"), config.usePlainRockets)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.plain.tooltip"))
                .setSaveConsumer(v -> config.usePlainRockets = v)
                .build());

        selection.addEntry(entry.startBooleanToggle(text("text.autoelytra.explosive"), config.useExplosiveRockets)
                .setDefaultValue(false)
                .setTooltip(text("text.autoelytra.explosive.tooltip"))
                .setSaveConsumer(v -> config.useExplosiveRockets = v)
                .build());

        selection.addEntry(entry.startBooleanToggle(text("text.autoelytra.flight1"), config.useFlightOne)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.flight.tooltip"))
                .setSaveConsumer(v -> config.useFlightOne = v)
                .build());

        selection.addEntry(entry.startBooleanToggle(text("text.autoelytra.flight2"), config.useFlightTwo)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.flight.tooltip"))
                .setSaveConsumer(v -> config.useFlightTwo = v)
                .build());

        selection.addEntry(entry.startBooleanToggle(text("text.autoelytra.flight3"), config.useFlightThree)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.flight.tooltip"))
                .setSaveConsumer(v -> config.useFlightThree = v)
                .build());

        selection.addEntry(entry.startBooleanToggle(text("text.autoelytra.offhand"), config.useOffhand)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.offhand.tooltip"))
                .setSaveConsumer(v -> config.useOffhand = v)
                .build());

        selection.addEntry(entry.startBooleanToggle(text("text.autoelytra.offhandFirst"), config.prioritizeOffhand)
                .setDefaultValue(true)
                .setTooltip(text("text.autoelytra.offhandFirst.tooltip"))
                .setSaveConsumer(v -> config.prioritizeOffhand = v)
                .build());

        selection.addEntry(entry.startIntSlider(text("text.autoelytra.reserve"), config.reserveCount, 0, 64)
                .setDefaultValue(1)
                .setTooltip(text("text.autoelytra.reserve.tooltip"))
                .setSaveConsumer(v -> config.reserveCount = v)
                .build());

        selection.addEntry(entry.startEnumSelector(text("text.autoelytra.priority"), UsePriority.class, config.priority)
                .setDefaultValue(UsePriority.FLIGHT_LONG_FIRST)
                .setEnumNameProvider(value -> text(((UsePriority) value).labelKey))
                .setTooltip(text("text.autoelytra.priority.tooltip"))
                .setSaveConsumer(v -> config.priority = v)
                .build());

        return builder.build();
    }

    private static Component text(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
