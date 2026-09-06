package net.metrodata.autoelytra.core;

import net.metrodata.autoelytra.AutoElytraBoost;
import net.metrodata.autoelytra.config.ConfigData;
import net.metrodata.autoelytra.config.ModConfig;
import net.metrodata.autoelytra.config.UsePriority;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 鞘翅飞行时按空格自动使用背包烟花的客户端逻辑。
 *
 * <p>为了让玩家不需要把烟花放在手上,本模组借助 26.2 的容器点击接口,
 * 先把选中的烟花与当前手持格短暂互换 -> 使用 -> 再换回原处。
 * 使用结束后原手持物品仍然保留在原位。
 */
public final class ElytraBoostController {
    /** 副手在 Inventory 中的索引(0-35 主背包,36-39 盔甲,40 副手)。 */
    private static final int OFFHAND_INDEX = 40;
    /** 手持栏第 0 格在 InventoryMenu 中的容器槽位。 */
    private static final int HOTBAR_MENU_OFFSET = 36;

    private static int tick;
    private static int nextAllowedTick;
    private static boolean wasFallFlying;
    /** 上一 tick 两个触发键是否处于按下状态,用于只认“松开后的新按下”。 */
    private static boolean boostKeyWasDown;
    private static boolean jumpKeyWasDown;

    private ElytraBoostController() {
    }

    public static void onEndTick(Minecraft client) {
        tick++;

        // 系统/游戏在按住时也会重复投递按键事件,因此不能只看 consumeClick()。
        // 必须结合上一 tick 的按下状态,只把“松开后重新按下”当成一次有效触发。
        boolean boostKeyDownNow = AutoElytraBoost.BOOST_KEY.isDown();
        boolean boostKeyPressed = AutoElytraBoost.BOOST_KEY.consumeClick() && !boostKeyWasDown;
        boostKeyWasDown = boostKeyDownNow;

        boolean jumpKeyDownNow = client.options.keyJump.isDown();
        boolean jumpKeyPressed = jumpKeyDownNow && !jumpKeyWasDown;
        jumpKeyWasDown = jumpKeyDownNow;

        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            wasFallFlying = false;
            return;
        }

        boolean fallFlying = player.isFallFlying();
        boolean glideJustStarted = fallFlying && !wasFallFlying;
        wasFallFlying = fallFlying;

        if (!ModConfig.INSTANCE.enabled) {
            return;
        }

        if (!player.isFallFlying() || player.isDeadOrDying() || player.isUsingItem()) {
            return;
        }

        if (player.containerMenu != player.inventoryMenu) {
            return;
        }

        boolean wantBoost = boostKeyPressed;

        ConfigData config = ModConfig.INSTANCE;
        if (config.respondToVanillaJump && jumpKeyPressed) {
            // 鞘翅滑翔时空格(跳跃键)本来没有其它用途,可以放心消费。
            wantBoost = true;
        }

        // 第二下空格:同一 tick 刚进入鞘翅滑翔,先只滑翔不用烟花;
        // 第三下空格(或之后)才触发加速。可在配置中关闭此行为。
        if (wantBoost && config.skipBoostOnGlideStart && glideJustStarted) {
            showMessage(client, "text.autoelytra.msg.suppressed");
            return;
        }

        if (!wantBoost || tick < nextAllowedTick) {
            return;
        }

        if (tryBoost(client, player)) {
            nextAllowedTick = tick + Math.max(1, config.minTicksBetweenUses);
        }
    }

    private static boolean tryBoost(Minecraft client, LocalPlayer player) {
        ConfigData config = ModConfig.INSTANCE;
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();

        if (isAllowed(inventory.getItem(selectedSlot), config)) {
            // 手上正好有可用烟花,直接使用。
            return useSelectedItem(client, player);
        }

        int sourceSlot = findBestSource(inventory, selectedSlot, config);
        if (sourceSlot == -1) {
            AutoElytraBoost.LOGGER.debug(
                    "[Auto Elytra Boost] No usable firework found (check 'keep this many rockets' and type filters).");
            showMessage(client, "text.autoelytra.msg.noRocket");
            return false;
        }

        if (sourceSlot < 9) {
            // 烟花在快捷栏:直接临时切换手持栏位,用完切回原槽,
            // 不移动任何物品堆,比容器点击更可靠。
            return boostFromHotbarSlot(client, player, sourceSlot);
        }

        // 主背包 / 副手:借助 26.2 容器点击把烟花换到手持格再使用。
        return boostFromOtherSlot(client, player, selectedSlot, sourceSlot);
    }

    /** 当前手持格就是可用烟花,直接走原版使用路径。 */
    private static boolean useSelectedItem(Minecraft client, LocalPlayer player) {
        if (client.gameMode == null) {
            return false;
        }
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        showMessage(client, "text.autoelytra.msg.fire");
        return true;
    }

    /** 烟花在快捷栏(0-8):临时把手持栏位切过去,用完切回来。 */
    private static boolean boostFromHotbarSlot(Minecraft client, LocalPlayer player, int fireworkSlot) {
        Inventory inventory = player.getInventory();
        int originalSlot = inventory.getSelectedSlot();
        if (originalSlot == fireworkSlot) {
            return useSelectedItem(client, player);
        }

        inventory.setSelectedSlot(fireworkSlot);
        sendCarriedItemPacket(client, fireworkSlot);
        try {
            if (!isAllowed(inventory.getItem(fireworkSlot), ModConfig.INSTANCE)) {
                return false;
            }
            return useSelectedItem(client, player);
        } finally {
            if (inventory.getSelectedSlot() != originalSlot) {
                inventory.setSelectedSlot(originalSlot);
                sendCarriedItemPacket(client, originalSlot);
            }
        }
    }

    /** 主背包 / 副手:把目标烟花与手持格互换,使用后再换回。 */
    private static boolean boostFromOtherSlot(
            Minecraft client,
            LocalPlayer player,
            int selectedSlot,
            int sourceSlot
    ) {
        Inventory inventory = player.getInventory();
        ConfigData config = ModConfig.INSTANCE;
        boolean moved = sourceSlot != selectedSlot;
        try {
            if (moved) {
                moveBetweenInventoryAndSelectedSlot(client.gameMode, player, selectedSlot, sourceSlot);
            }

            ItemStack inHand = inventory.getItem(selectedSlot);
            if (!isAllowed(inHand, config)) {
                AutoElytraBoost.LOGGER.warn(
                        "[Auto Elytra Boost] Inventory swap did not take effect; skipping use to avoid wrong item use.");
                showMessage(client, "text.autoelytra.msg.swapFail");
                return false;
            }

            // 与游戏内右键使用物品走同一条网络路径;服务器校验到鞘翅飞行即发射烟花。
            return useSelectedItem(client, player);
        } finally {
            if (moved) {
                // 无论成功与否都立刻把原手持物品换回来。
                moveBetweenInventoryAndSelectedSlot(client.gameMode, player, selectedSlot, sourceSlot);
            }
        }
    }

    private static void sendCarriedItemPacket(Minecraft client, int slot) {
        if (client.getConnection() != null) {
            client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    private static void showMessage(Minecraft client, String translationKey) {
        client.gui.hud.setOverlayMessage(Component.translatable(translationKey), false);
    }

    /**
     * 在背包中找最合适的一叠烟花。
     *
     * @return Inventory 物品索引(0-35 主背包、40 副手),找不到返回 -1。
     */
    private static int findBestSource(Inventory inventory, int selectedSlot, ConfigData config) {
        List<FireworkInfo> candidates = new ArrayList<>();
        long totalCount = 0;

        for (int index : inventoryIndexOrder()) {
            if (index == selectedSlot) {
                continue; // 当前手持格已确认不含可用烟花。
            }
            if (index == OFFHAND_INDEX && !config.useOffhand) {
                continue;
            }

            ItemStack stack = inventory.getItem(index);
            if (stack.isEmpty() || stack.getItem() != Items.FIREWORK_ROCKET) {
                continue;
            }

            Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
            int flight = fireworks == null ? 0 : Math.max(0, fireworks.flightDuration());
            boolean explosive = fireworks != null && !fireworks.explosions().isEmpty();

            if (!typeAllowed(explosive, flight, config)) {
                continue;
            }

            int count = stack.getCount();
            totalCount += count;
            candidates.add(new FireworkInfo(index, count, flight, explosive));
        }

        if (candidates.isEmpty()) {
            return -1;
        }

        if (config.prioritizeOffhand) {
            // 副手有符合过滤条件的烟花时,优先使用副手这一叠。
            // 副手与主手一样属于“主动使用位”,不受保留数量限制。
            for (FireworkInfo candidate : candidates) {
                if (candidate.slot == OFFHAND_INDEX) {
                    return candidate.slot;
                }
            }
        }

        // 保留数量:背包中可用烟花总数不足/等于保留值时,不自动取用。
        if (totalCount <= config.reserveCount) {
            return -1;
        }

        candidates.sort(priorityComparator(config.priority));
        return candidates.get(0).slot;
    }

    private static List<Integer> inventoryIndexOrder() {
        List<Integer> order = new ArrayList<>(37);
        for (int i = 0; i < 36; i++) {
            order.add(i);
        }
        order.add(OFFHAND_INDEX);
        return order;
    }

    private static Comparator<FireworkInfo> priorityComparator(UsePriority priority) {
        Comparator<FireworkInfo> comparator;
        switch (priority) {
            case FLIGHT_SHORT_FIRST -> comparator = Comparator.comparingInt((FireworkInfo f) -> f.flightDuration);
            case STACK_LARGE_FIRST -> comparator = Comparator.comparingInt((FireworkInfo f) -> -f.count);
            case STACK_SMALL_FIRST -> comparator = Comparator.comparingInt(f -> f.count);
            case HOTBAR_FIRST -> comparator = Comparator.comparingInt(f -> f.slot);
            case FLIGHT_LONG_FIRST -> comparator = Comparator.comparingInt((FireworkInfo f) -> -f.flightDuration);
            default -> comparator = Comparator.comparingInt((FireworkInfo f) -> -f.flightDuration);
        }
        // 同样的优先级下按背包顺序稳定取值。
        return comparator.thenComparingInt(f -> f.slot);
    }

    private static boolean typeAllowed(boolean explosive, int flight, ConfigData config) {
        if (explosive) {
            if (!config.useExplosiveRockets) {
                return false;
            }
        } else {
            if (!config.usePlainRockets) {
                return false;
            }
        }

        return switch (flight) {
            case 0 -> config.useFlightOne;
            case 1 -> config.useFlightTwo;
            default -> config.useFlightThree;
        };
    }

    private static boolean isAllowed(ItemStack stack, ConfigData config) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.FIREWORK_ROCKET) {
            return false;
        }
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        int flight = fireworks == null ? 0 : Math.max(0, fireworks.flightDuration());
        boolean explosive = fireworks != null && !fireworks.explosions().isEmpty();
        return typeAllowed(explosive, flight, config);
    }

    /**
     * 把 {@code sourceInventoryIndex} 这一格与当前手持格(selectedSlot)互换。
     * 换入、换出使用同一函数,天然可逆。
     *
     * <p>背包主区域 9-35 在玩家物品栏菜单中的槽位号恰巧与 Inventory 索引相同;
     * 手持栏是菜单 36-44;副手是菜单 45。</p>
     */
    private static void moveBetweenInventoryAndSelectedSlot(
            MultiPlayerGameMode gameMode,
            Player player,
            int selectedSlot,
            int sourceInventoryIndex
    ) {
        if (selectedSlot == sourceInventoryIndex) {
            return;
        }

        int menuId = player.inventoryMenu.containerId;
        int selectedMenuSlot = HOTBAR_MENU_OFFSET + selectedSlot;

        if (sourceInventoryIndex < 9) {
            // 来源在手握栏:直接 SWAP 即可(第二参数使用 Inventory 索引)。
            gameMode.handleContainerInput(menuId, selectedMenuSlot, sourceInventoryIndex, ContainerInput.SWAP, player);
        } else if (sourceInventoryIndex < 36) {
            // 来源在背包主区域(9-35),菜单槽位号等于 Inventory 索引。
            // 用三次拾取完成互换,效果等同于把两格物品对调。
            gameMode.handleContainerInput(menuId, sourceInventoryIndex, 0, ContainerInput.PICKUP, player);
            gameMode.handleContainerInput(menuId, selectedMenuSlot, 0, ContainerInput.PICKUP, player);
            gameMode.handleContainerInput(menuId, sourceInventoryIndex, 0, ContainerInput.PICKUP, player);
        } else if (sourceInventoryIndex == OFFHAND_INDEX) {
            // 来源在副手(Inventory 索引 40)。
            gameMode.handleContainerInput(menuId, selectedMenuSlot, OFFHAND_INDEX, ContainerInput.SWAP, player);
        }
    }

}
