package Tenzinn.Core.Cases;

public class CaseSkin {

    /**
     * CS:GO-style rarities. Weights sum to 10 000 (= 100 %).
     * Each rarity is also the border color shown on the roulette slot.
     */
    public enum Rarity {
        MIL_SPEC   ("#2BB0B5", "Mil-Spec",    7990),
        RESTRICTED ("#5D00FF", "Restricted",  1595),
        CLASSIFIED ("#FF00D9", "Classified",   325),
        COVERT     ("#FF0000", "Covert",        65),
        SPECIAL    ("#FFE100", "Special Item",  25);

        public final String color;
        public final String label;
        /** Weight out of 10 000 — used to roll the rarity tier. */
        public final int weight;

        Rarity(String color, String label, int weight) {
            this.color  = color;
            this.label  = label;
            this.weight = weight;
        }
    }

    public final String id;
    public final String displayName;
    /** Folder name inside Shop/ (e.g. "Ak47"). Used to build the Weapons.ui image ref. */
    public final String weapon;
    public final Rarity rarity;

    public CaseSkin(String id, String displayName, String weapon, Rarity rarity) {
        this.id          = id;
        this.displayName = displayName;
        this.weapon      = weapon;
        this.rarity      = rarity;
    }
}
