package Tenzinn.Core.Shop;

public class EconomySystem {

    public enum Revenue { KILL, ROUND, BOMB, DEATH }

    public static int getRevenue(Revenue type) { return RevenuesConfig.getValue(type.toString()); }
}
