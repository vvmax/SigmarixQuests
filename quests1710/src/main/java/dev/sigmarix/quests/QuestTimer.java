package dev.sigmarix.quests;
/** Tick durations retain the original space-separated w/d/h/m/s/t notation. */
public final class QuestTimer {
    private static final long[] UNITS={12096000,1728000,72000,1200,20,1};
    private static final String SUFFIXES="wdhmst";
    public static long parse(String text){
        String value=text.trim();if(value.startsWith("\"") && value.endsWith("\"") && value.length()>1)value=value.substring(1,value.length()-1);
        if(value.isEmpty())throw new IllegalArgumentException("Missing duration");long ticks=0;
        for(String part:value.split("\\s+")){char last=Character.toLowerCase(part.charAt(part.length()-1));int unit=SUFFIXES.indexOf(last);String number=unit<0?part:part.substring(0,part.length()-1);long amount=Long.parseLong(number);if(amount<0)throw new IllegalArgumentException("Negative duration");ticks=Math.addExact(ticks,Math.multiplyExact(amount,unit<0?1:UNITS[unit]));}return ticks;
    }
    public static String format(long ticks){if(ticks<0)throw new IllegalArgumentException("Negative duration");if(ticks==0)return "0s";StringBuilder text=new StringBuilder();for(int i=0;i<UNITS.length;i++){long amount=ticks/UNITS[i];ticks%=UNITS[i];if(amount>0){if(text.length()>0)text.append(' ');text.append(amount).append(SUFFIXES.charAt(i));}}return text.toString();}
    public static long emergency(QuestCore.Book book){return book.emergencyCooldownTicks<0?book.emergencyCooldownSeconds*20L:book.emergencyCooldownTicks;}
    private QuestTimer(){}
}
