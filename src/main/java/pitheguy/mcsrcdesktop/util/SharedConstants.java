package pitheguy.mcsrcdesktop.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mcsrc.ClassData;
import mcsrc.MemberData;

import java.time.Instant;

public class SharedConstants {
    public static final int PROTOCOL_VERSION = 1;
    public static final String APP_VERSION = "1.0.2";
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, ExtraTypeAdapters.INSTANT)
            .registerTypeAdapter(ClassData.class, ExtraTypeAdapters.CLASS_DATA)
            .registerTypeAdapter(MemberData.class, ExtraTypeAdapters.MEMBER_DATA)
            .create();
}
