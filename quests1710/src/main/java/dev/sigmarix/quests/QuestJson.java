package dev.sigmarix.quests;
import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.IOException;
import java.math.BigDecimal;
/** Integer fields must never silently truncate fractions or wrap overflowing quantities. */
public final class QuestJson {
    private static BigDecimal integer(JsonReader reader,int digits) throws IOException {
        if(reader.peek()!=JsonToken.NUMBER) throw new JsonParseException("Expected an integer JSON number");
        String text=reader.nextString();
        if(text.length()>64) throw new JsonParseException("Integer literal exceeds supported range");
        try {
            BigDecimal number=new BigDecimal(text).stripTrailingZeros();
            if(number.scale()>0 || (long)number.precision()-number.scale()>digits) throw new ArithmeticException();
            return number;
        } catch(NumberFormatException | ArithmeticException e) { throw new JsonParseException("Expected an exact integer within supported range",e); }
    }
    public static Gson create() {
        TypeAdapter<Integer> ints=new TypeAdapter<Integer>() {
            public void write(JsonWriter out,Integer value) throws IOException { out.value(value); }
            public Integer read(JsonReader in) throws IOException {
                try { return integer(in,10).intValueExact(); } catch(ArithmeticException e) { throw new JsonParseException("Integer outside 32-bit range",e); }
            }
        };
        TypeAdapter<Long> longs=new TypeAdapter<Long>() {
            public void write(JsonWriter out,Long value) throws IOException { out.value(value); }
            public Long read(JsonReader in) throws IOException {
                try { return integer(in,19).longValueExact(); } catch(ArithmeticException e) { throw new JsonParseException("Integer outside 64-bit range",e); }
            }
        };
        return new GsonBuilder().setPrettyPrinting().registerTypeAdapter(int.class,ints).registerTypeAdapter(Integer.class,ints).registerTypeAdapter(long.class,longs).registerTypeAdapter(Long.class,longs).create();
    }
}
