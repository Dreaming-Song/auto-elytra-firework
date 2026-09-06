package net.metrodata.autoelytra.config;

public final class ConfigData {
    /** 模组总开关。 */
    public boolean enabled = true;

    /** 是否也把游戏自带的跳跃键(通常是空格)当作触发键。 */
    public boolean respondToVanillaJump = false;

    /**
     * 跳过“启动滑翔”那一次按键的加速:
     * 按下第二下空格只是进入鞘翅滑翔,需要再按第三下才使用烟花。
     */
    public boolean skipBoostOnGlideStart = true;

    /** 两次自动使用的最小间隔(tick,1秒=20tick)。 */
    public int minTicksBetweenUses = 10;

    /** 允许使用带爆炸效果的烟花(飞行时会在身边炸开)。默认不使用。 */
    public boolean useExplosiveRockets = false;

    /** 允许使用不带爆炸效果、仅用于推进的普通烟花。 */
    public boolean usePlainRockets = true;

    /** 按“飞行时间”细分:1个火药(飞行时间1)。 */
    public boolean useFlightOne = true;

    /** 2个火药(飞行时间2)。 */
    public boolean useFlightTwo = true;

    /** 3个火药(飞行时间3)。 */
    public boolean useFlightThree = true;

    /** 副手中的烟花也可以被自动取用(用完仍放回副手)。 */
    public boolean useOffhand = true;

    /** 副手中有可用烟花时优先使用副手(更符合实际操作习惯)。 */
    public boolean prioritizeOffhand = true;

    /**
     * 保留数量:自动搜索背包时,若可用烟花总数不超过该值则不动它们,
     * 避免把最后的保命/备用烟花用完。
     */
    public int reserveCount = 1;

    /** 候选烟花都满足条件时,优先使用哪一叠。 */
    public UsePriority priority = UsePriority.FLIGHT_LONG_FIRST;
}
