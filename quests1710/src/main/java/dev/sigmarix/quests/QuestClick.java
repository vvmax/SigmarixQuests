package dev.sigmarix.quests;
/** Portable click protocol. Commands always execute as the actual player. */
public final class QuestClick {
    public final String type,value;
    public QuestClick(String action) {
        if(action==null || action.length()>8192 || action.indexOf('\0')>=0 || action.indexOf('\n')>=0 || action.indexOf('\r')>=0) throw new IllegalArgumentException("Invalid quest click action");
        int colon=action.indexOf(':'); type=colon<0?"":action.substring(0,colon); value=colon<0?action:action.substring(colon+1);
        if(!action.isEmpty() && (value.isEmpty() && !type.equals("guide") || !type.isEmpty() && !type.matches("[a-z][a-z0-9_.-]{0,63}"))) throw new IllegalArgumentException("Invalid quest click protocol");
        if((type.equals("http") || type.equals("https")) && !QuestPresentation.link(action)) throw new IllegalArgumentException("Invalid click URL");
        if(type.equals("guide") && value.length()>512 || type.equals("custom") && (!QuestPresentation.resource(value) || value.isEmpty())) throw new IllegalArgumentException("Invalid guide or custom click ID");
        if(type.equals("curseforgepages") && !value.matches("[a-zA-Z0-9_-]+[:/][a-zA-Z0-9_/-]+")) throw new IllegalArgumentException("Invalid CurseForge page");
    }
}
