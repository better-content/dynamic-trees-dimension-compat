package com.bettercontent.dynamictreesdimensioncompat;

import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.LevelContext;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import com.ferreusveritas.dynamictrees.worldgen.GenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class DimensionForestChunkDecorator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int DELAY_TICKS = 40;
    private static final int MAX_CHUNKS_PER_TICK = 2;
    private static final int ROOT_SCAN_DEPTH = 48;
    private static final int COLUMN_SCAN_STRIDE = 4;
    private static final AtomicInteger LOGS = new AtomicInteger();

    private static final List<Target> TARGETS = List.of(
            new Target("undergarden:undergarden", "undergarden:dense_forest", "grongle", 5, 3, false),
            new Target("undergarden:undergarden", "undergarden:gronglegrowth", "grongle", 6, 3, false),
            new Target("undergarden:undergarden", "undergarden:smogstem_forest", "smogstem", 6, 3, false),
            new Target("undergarden:undergarden", "undergarden:wigglewood_forest", "wigglewood", 6, 3, false),
            new Target("the_finley_dimension_remastered:finley_dimension", "the_finley_dimension_remastered:finley_forest", "finley_wood", 5, 3, false),
            new Target("the_finley_dimension_remastered:finley_dimension", "the_finley_dimension_remastered:living_forest", "living_wood", 7, 3, false),
            new Target("callfromthedepth_:depth", "callfromthedepth_:deepforest", "silent_tree", 8, 3, true),
            new Target("callfromthedepth_:depth", "callfromthedepth_:forgottenforest", "silent_tree", 8, 3, true)
    );

    private final Map<ResourceKey<Level>, ArrayDeque<PendingChunk>> pending = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onChunkLoad(final ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk) || targetsFor(level).isEmpty()) {
            return;
        }
        final DecoratedChunks decorated = DecoratedChunks.get(level);
        final ChunkKey key = new ChunkKey(chunk.getPos().x, chunk.getPos().z);
        if (decorated.contains(key)) {
            return;
        }
        pending.computeIfAbsent(level.dimension(), ignored -> new ArrayDeque<>())
                .addLast(new PendingChunk(key, DELAY_TICKS));
    }

    @SubscribeEvent
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) {
            return;
        }

        for (final ServerLevel level : event.getServer().getAllLevels()) {
            final ArrayDeque<PendingChunk> queue = pending.get(level.dimension());
            if (queue == null || queue.isEmpty()) {
                continue;
            }
            int processed = 0;
            int remaining = queue.size();
            while (remaining-- > 0 && processed < MAX_CHUNKS_PER_TICK) {
                final PendingChunk pendingChunk = queue.removeFirst();
                final PendingChunk delayed = pendingChunk.tickDown();
                if (delayed.ticksRemaining() > 0) {
                    queue.addLast(delayed);
                    continue;
                }
                if (decorate(level, pendingChunk.key())) {
                    processed++;
                }
            }
            if (queue.isEmpty()) {
                pending.remove(level.dimension());
            }
        }
    }

    private static boolean decorate(final ServerLevel level, final ChunkKey key) {
        if (!level.getChunkSource().hasChunk(key.x(), key.z())) {
            return true;
        }

        final DecoratedChunks decorated = DecoratedChunks.get(level);
        if (decorated.contains(key)) {
            return true;
        }

        final List<Target> targets = targetsFor(level);
        if (targets.isEmpty()) {
            decorated.add(key);
            return true;
        }

        final RandomSource random = RandomSource.create(level.getSeed()
                ^ (((long) key.x()) * 341873128712L)
                ^ (((long) key.z()) * 132897987541L));
        int generated = 0;
        for (final Target target : targets) {
            generated += decorateTarget(level, key, target, random);
        }
        decorated.add(key);
        if (generated > 0 && LOGS.getAndIncrement() < 80) {
            LOGGER.info("Generated {} Better Content dynamic dimension trees in {} chunk {},{}",
                    generated, level.dimension().location(), key.x(), key.z());
        }
        return true;
    }

    private static int decorateTarget(final ServerLevel level, final ChunkKey key, final Target target, final RandomSource random) {
        final Species species = Species.REGISTRY.get(BcDimTrees.location(target.speciesPath()));
        if (!species.isValid()) {
            return 0;
        }

        int generated = 0;
        final LevelContext levelContext = LevelContext.create(level);
        for (int attempt = 0; attempt < target.attempts(); attempt++) {
            final int x = key.x() * 16 + random.nextInt(16);
            final int z = key.z() * 16 + random.nextInt(16);
            final BlockPos rootPos = target.columnScan()
                    ? findRootPosInColumn(level, x, z, target.biomeId(), species, random)
                    : findSurfaceRootPos(level, x, z, target.biomeId(), species);
            if (rootPos == null) {
                continue;
            }
            final GenerationContext generationContext = new GenerationContext(
                    levelContext,
                    species,
                    rootPos,
                    rootPos.mutable(),
                    level.getBiome(rootPos),
                    Direction.Plane.HORIZONTAL.getRandomDirection(random),
                    target.radius(),
                    SafeChunkBounds.ANY_WG
            );
            if (species.generate(generationContext)) {
                generated++;
            }
        }
        return generated;
    }

    private static BlockPos findSurfaceRootPos(final ServerLevel level, final int x, final int z, final String biomeId, final Species species) {
        final int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        final BlockPos surface = new BlockPos(x, y, z);
        if (!isTargetBiome(level, surface, biomeId)) {
            return null;
        }
        return findRootPos(level, surface, species);
    }

    private static BlockPos findRootPos(final ServerLevel level, final BlockPos origin, final Species species) {
        BlockPos pos = origin;
        for (int offset = 0; offset <= ROOT_SCAN_DEPTH; offset++) {
            final BlockState state = level.getBlockState(pos);
            if (species.isAcceptableSoilForWorldgen(level, pos, state)) {
                return pos;
            }
            pos = pos.below();
        }
        return null;
    }

    private static BlockPos findRootPosInColumn(
            final ServerLevel level,
            final int x,
            final int z,
            final String biomeId,
            final Species species,
            final RandomSource random
    ) {
        BlockPos selected = null;
        int candidates = 0;
        final int minY = level.getMinBuildHeight();
        final int maxY = level.getMaxBuildHeight() - 1;
        for (int y = maxY; y >= minY; y--) {
            final BlockPos pos = new BlockPos(x, y, z);
            final BlockState state = level.getBlockState(pos);
            if (!species.isAcceptableSoilForWorldgen(level, pos, state)) {
                continue;
            }
            final BlockPos trunkSpace = pos.above();
            if (!isTargetBiome(level, trunkSpace, biomeId)) {
                continue;
            }
            if (!level.getBlockState(trunkSpace).canBeReplaced()) {
                continue;
            }
            candidates++;
            if (random.nextInt(candidates) == 0) {
                selected = pos;
            }
            y -= COLUMN_SCAN_STRIDE - 1;
        }
        return selected;
    }

    private static boolean isTargetBiome(final ServerLevel level, final BlockPos pos, final String biomeId) {
        return level.getBiome(pos).unwrapKey()
                .map(key -> biomeId.equals(key.location().toString()))
                .orElse(false);
    }

    private static List<Target> targetsFor(final ServerLevel level) {
        final String dimension = level.dimension().location().toString();
        final List<Target> targets = new ArrayList<>();
        for (final Target target : TARGETS) {
            if (target.dimensionId().equals(dimension)) {
                targets.add(target);
            }
        }
        return targets;
    }

    private record Target(String dimensionId, String biomeId, String speciesPath, int attempts, int radius, boolean columnScan) {
    }

    private record PendingChunk(ChunkKey key, int ticksRemaining) {
        PendingChunk tickDown() {
            return new PendingChunk(key, ticksRemaining - 1);
        }
    }

    private record ChunkKey(int x, int z) {
        CompoundTag save() {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("x", x);
            tag.putInt("z", z);
            return tag;
        }

        static ChunkKey load(final CompoundTag tag) {
            return new ChunkKey(tag.getInt("x"), tag.getInt("z"));
        }
    }

    private static final class DecoratedChunks extends SavedData {
        private static final String NAME = BcDimTrees.MODID + "_decorated_dimension_tree_chunks_v2";
        private final Set<ChunkKey> chunks = new HashSet<>();

        static DecoratedChunks get(final ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(DecoratedChunks::load, DecoratedChunks::new, NAME);
        }

        private static DecoratedChunks load(final CompoundTag tag) {
            final DecoratedChunks data = new DecoratedChunks();
            final ListTag chunks = tag.getList("chunks", Tag.TAG_COMPOUND);
            for (int i = 0; i < chunks.size(); i++) {
                data.chunks.add(ChunkKey.load(chunks.getCompound(i)));
            }
            return data;
        }

        boolean contains(final ChunkKey key) {
            return chunks.contains(key);
        }

        void add(final ChunkKey key) {
            if (chunks.add(key)) {
                setDirty();
            }
        }

        @Override
        public CompoundTag save(final CompoundTag tag) {
            final ListTag savedChunks = new ListTag();
            for (final ChunkKey chunk : chunks) {
                savedChunks.add(chunk.save());
            }
            tag.put("chunks", savedChunks);
            return tag;
        }
    }
}
