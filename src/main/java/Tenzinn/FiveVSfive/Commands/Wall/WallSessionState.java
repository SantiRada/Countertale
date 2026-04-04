package Tenzinn.FiveVSfive.Commands.Wall;

public final class WallSessionState {

    private static String activeMap = null;

    private WallSessionState() { }

    public static String currentMap()              { return activeMap; }
    public static void   setCurrentMap(String map) { activeMap = map; }
    public static void   clear()                   { activeMap = null; }
}