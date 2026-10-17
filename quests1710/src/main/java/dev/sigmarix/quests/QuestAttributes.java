package dev.sigmarix.quests;
/** Persistent native 1.7.10 equivalents of personal stages and numeric skill levels. */
public final class QuestAttributes {
    public static void validateKey(String key) {
        if(key==null || !key.matches("[a-zA-Z0-9_.:/-]{1,128}")) throw new IllegalArgumentException("Invalid stage or skill key");
    }
    public static boolean compare(long actual,long expected,String comparison) {
        switch(comparison) { case "at_least": return actual>=expected; case "at_most": return actual<=expected; case "equals": return actual==expected; case "not_equals": return actual!=expected; case "greater": return actual>expected; case "less": return actual<expected; default: throw new IllegalArgumentException("Invalid comparison"); }
    }
    private QuestAttributes() {}
}
