package io.github.linkfgfgui.simple_airport;

import immersive_aircraft.entity.AircraftEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import java.lang.SuppressWarnings;

import java.util.ArrayList;
import java.util.List;

import static io.github.linkfgfgui.simple_airport.SimpleAirport.CORE_BLOCK_ENTITY;

public class CoreBlockEntity extends BlockEntity {
    public static final int RADIUS = 3;
    public final int SLEEP_TICK = 20;
    public BlockPos runwayPos = null;
    public List<BlockPos> aprons = new ArrayList<>();
    int alreadySleptTicks = 0;


    public CoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(CORE_BLOCK_ENTITY.get(), pos, blockState);
    }

    static AABB getAABBfromPos(BlockPos pos) {
        return AABB.ofSize( pos.getCenter(),RADIUS,RADIUS,RADIUS);
    }

    static AircraftEntity getAircraftOn(BlockPos pos, Level level) {
//        List<AircraftEntity> lel = level.getEntitiesOfClass(AircraftEntity.class, new AABB(pos.above()));
        List<AircraftEntity> lel = level.getEntitiesOfClass(AircraftEntity.class, getAABBfromPos(pos.above()));
        if (lel.isEmpty()) return null;
        AircraftEntity aircraft = lel.getFirst();
//        SimpleAirport.LOGGER.debug("{} ES:{} PS:{}", aircraft.toString(), aircraft.getProperties().get(VehicleStat.ENGINE_SPEED), aircraft.getProperties().get(VehicleStat.PUSH_SPEED));
        return aircraft;
    }

    static float oppositeRot(float r) {
        r += 180;
        if (r > 180) r -= 360;
        if (r == -180) r = 180;
        return r;
    }


    public static void tick(Level level, BlockPos pos, BlockState state, CoreBlockEntity be) {
//        if (level.isClientSide) return;
        if (be.runwayPos == null || be.aprons.isEmpty()) return;
//        if (be.alreadySleptTicks < be.SLEEP_TICK) {
//            be.alreadySleptTicks++;
//            return;
//        }
        AircraftEntity aircraft;
        aircraft = getAircraftOn(be.runwayPos, level);
        if (aircraft != null) {
            if (aircraft.enginePower.getValue() == 0) {
                for (BlockPos apronPos : be.aprons) {
                    if (getAircraftOn(apronPos, level) == null) {
                        BlockPos targetApronPos = apronPos.above();
                        aircraft.setPos(targetApronPos.getCenter());
                        aircraft.setYRot(oppositeRot(aircraft.getYRot()));
//                        aircraft.teleportTo(targetApronPos.getX(),targetApronPos.getY(),targetApronPos.getZ());
//                        be.alreadySleptTicks = 0;
                        return;
                    }
                }
            }
        }

        for (BlockPos apronPos : be.aprons) {
            aircraft = getAircraftOn(apronPos, level);
            if (aircraft != null) {
                if (aircraft.enginePower.getValue() > 0) {
                    BlockPos targetRwyPos = be.runwayPos.above();
                    aircraft.setPos(targetRwyPos.getCenter());
//                    aircraft.teleportTo(targetRwyPos.getX(),targetRwyPos.getY(),targetRwyPos.getZ());
//                    be.alreadySleptTicks = 0;
                    return;
                }
            }
        }


    }

    public void setRWY(BlockPos pos) {
        runwayPos = pos;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void addApron(BlockPos pos) {
        if (aprons.contains(pos)) return;
        aprons.add(pos);
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void removeApron(BlockPos pos) {
        if (!aprons.contains(pos)) return;
        aprons.remove(pos);
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("runway")) {
            CompoundTag rwyTag = tag.getCompound("runway");
            this.runwayPos = new BlockPos(rwyTag.getInt("x"), rwyTag.getInt("y"), rwyTag.getInt("z"));
        }
        if (tag.contains("aprons")) {
            ListTag apronsTag = tag.getList("aprons", Tag.TAG_INT_ARRAY);
            aprons.clear();
            for (int i = 0; i < apronsTag.size(); i++) {
                int[] apronTag = apronsTag.getIntArray(i);
                aprons.add(new BlockPos(apronTag[0], apronTag[1], apronTag[2]));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        CompoundTag rwyTag = new CompoundTag();
        if (runwayPos != null) {
            rwyTag.putInt("x", runwayPos.getX());
            rwyTag.putInt("y", runwayPos.getY());
            rwyTag.putInt("z", runwayPos.getZ());
        }
        tag.put("runway", rwyTag);

        ListTag lt = new ListTag();
        for (BlockPos pos : aprons) {
            lt.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("aprons", lt);

    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
