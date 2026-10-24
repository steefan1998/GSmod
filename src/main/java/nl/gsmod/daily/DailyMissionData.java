package nl.gsmod.daily;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/** The missions a player got for {@code day} and how far along they are. Stored as a player attachment. */
public record DailyMissionData(long day, List<Entry> missions) {
    public record Entry(Identifier mission, long progress, boolean completed) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("mission").forGetter(Entry::mission),
                Codec.LONG.optionalFieldOf("progress", 0L).forGetter(Entry::progress),
                Codec.BOOL.optionalFieldOf("completed", false).forGetter(Entry::completed)
        ).apply(i, Entry::new));
    }

    public static final DailyMissionData EMPTY = new DailyMissionData(DailyRewardData.NEVER, List.of());

    public static final MapCodec<DailyMissionData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("day", DailyRewardData.NEVER).forGetter(DailyMissionData::day),
            Entry.CODEC.listOf().optionalFieldOf("missions", List.of()).forGetter(DailyMissionData::missions)
    ).apply(i, DailyMissionData::new));

    public DailyMissionData with(int index, Entry entry) {
        List<Entry> copy = new ArrayList<>(missions);
        copy.set(index, entry);
        return new DailyMissionData(day, List.copyOf(copy));
    }
}
