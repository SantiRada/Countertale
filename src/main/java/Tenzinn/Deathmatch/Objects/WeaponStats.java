package Tenzinn.Deathmatch.Objects;

public class WeaponStats {

    public enum TypeWeapon { Weapon, Shotgun, Knife }
    public enum Firemode { Melee, Single, Burst, Automatic }

    public String nameWeapon;
    public String typeWeapon;
    public String firemode;

    public WeaponStats(String nameWeapon, TypeWeapon typeWeapon, Firemode firemode) {
        this.nameWeapon = nameWeapon;
        this.typeWeapon = typeWeapon.toString();
        this.firemode = firemode.toString();
    }
}