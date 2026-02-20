package Tenzinn.Deathmatch.Objects;

import java.util.ArrayList;

public class WeaponStats {

    public String nameWeapon;
    public String crossType;
    public String typeWeapon;
    public String firemode;
    public String image;
    public int pos;
    public ArrayList<String> giveItems;

    public WeaponStats(String nameWeapon, String typeWeapon, String crosshair, String firemode, ArrayList<String> giveItems, String image, int pos) {
        this.nameWeapon = nameWeapon;
        this.typeWeapon = typeWeapon;
        this.crossType = crosshair;
        this.firemode = firemode;

        this.giveItems = new ArrayList<>();
        this.giveItems.addAll(giveItems);

        this.image = image;
        this.pos = pos;
    }
}