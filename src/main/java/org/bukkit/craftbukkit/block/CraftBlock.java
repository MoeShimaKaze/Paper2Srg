package org.bukkit.craftbukkit.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;


import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

public class CraftBlock implements Block {
    private final CraftChunk chunk;
    private final int x;
    private final int y;
    private final int z;

    public CraftBlock(CraftChunk chunk, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.chunk = chunk;
    }

    private net.minecraft.block.Block getNMSBlock() {
        return CraftMagicNumbers.getBlock(this); // TODO: UPDATE THIS
    }

    private static net.minecraft.block.Block getNMSBlock(int type) {
        return CraftMagicNumbers.getBlock(type);
    }

    @Override
    public World getWorld() {
        return chunk.getWorld();
    }

    @Override
    public Location getLocation() {
        return new Location(getWorld(), x, y, z);
    }

    @Override
    public Location getLocation(Location loc) {
        if (loc != null) {
            loc.setWorld(getWorld());
            loc.setX(x);
            loc.setY(y);
            loc.setZ(z);
            loc.setYaw(0);
            loc.setPitch(0);
        }

        return loc;
    }

    public BlockVector getVector() {
        return new BlockVector(x, y, z);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public Chunk getChunk() {
        return chunk;
    }

    @Override
    public void setData(final byte data) {
        setData(data, 3);
    }

    @Override
    public void setData(final byte data, boolean applyPhysics) {
        if (applyPhysics) {
            setData(data, 3);
        } else {
            setData(data, 2);
        }
    }

    private void setData(final byte data, int flag) {
        net.minecraft.world.World world = chunk.getHandle().func_177412_p();
        BlockPos position = new BlockPos(x, y, z);
        IBlockState blockData = world.func_180495_p(position);
        world.func_180501_a(position, blockData.func_177230_c().func_176203_a(data), flag);
    }

    private IBlockState getData0() {
        return chunk.getHandle().func_177435_g(new BlockPos(x, y, z));
    }

    @Override
    public byte getData() {
        IBlockState blockData = chunk.getHandle().func_177435_g(new BlockPos(x, y, z));
        return (byte) blockData.func_177230_c().func_176201_c(blockData);
    }

    @Override
    public void setType(final Material type) {
        setType(type, true);
    }

    @Override
    public void setType(Material type, boolean applyPhysics) {
        setTypeId(type.getId(), applyPhysics);
    }

    @Override
    public boolean setTypeId(final int type) {
        return setTypeId(type, true);
    }

    @Override
    public boolean setTypeId(final int type, final boolean applyPhysics) {
        net.minecraft.block.Block block = getNMSBlock(type);
        return setTypeIdAndData(type, (byte) block.func_176201_c(block.func_176223_P()), applyPhysics);
    }

    @Override
    public boolean setTypeIdAndData(final int type, final byte data, final boolean applyPhysics) {
        IBlockState blockData = getNMSBlock(type).func_176203_a(data);
        BlockPos position = new BlockPos(x, y, z);

        // SPIGOT-611: need to do this to prevent glitchiness. Easier to handle this here (like /setblock) than to fix weirdness in tile entity cleanup
        if (type != 0 && blockData.func_177230_c() instanceof BlockContainer && type != getTypeId()) {
            chunk.getHandle().func_177412_p().func_180501_a(position, Blocks.field_150350_a.func_176223_P(), 0);
        }

        if (applyPhysics) {
            return chunk.getHandle().func_177412_p().func_180501_a(position, blockData, 3);
        } else {
            IBlockState old = chunk.getHandle().func_177435_g(position);
            boolean success = chunk.getHandle().func_177412_p().func_180501_a(position, blockData, 18); // NOTIFY | NO_OBSERVER
            if (success) {
                chunk.getHandle().func_177412_p().func_184138_a(
                        position,
                        old,
                        blockData,
                        3
                );
            }
            return success;
        }
    }

    @Override
    public Material getType() {
        return Material.getMaterial(getTypeId());
    }

    @Deprecated
    @Override
    public int getTypeId() {
        return CraftMagicNumbers.getId(chunk.getHandle().func_177435_g(new BlockPos(this.x, this.y, this.z)).func_177230_c());
    }

    @Override
    public byte getLightLevel() {
        return (byte) chunk.getHandle().func_177412_p().func_175671_l(new BlockPos(this.x, this.y, this.z));
    }

    @Override
    public byte getLightFromSky() {
        return (byte) chunk.getHandle().func_177412_p().func_175642_b(EnumSkyBlock.SKY, new BlockPos(this.x, this.y, this.z));
    }

    @Override
    public byte getLightFromBlocks() {
        return (byte) chunk.getHandle().func_177412_p().func_175642_b(EnumSkyBlock.BLOCK, new BlockPos(this.x, this.y, this.z));
    }


    public Block getFace(final BlockFace face) {
        return getRelative(face, 1);
    }

    public Block getFace(final BlockFace face, final int distance) {
        return getRelative(face, distance);
    }

    @Override
    public Block getRelative(final int modX, final int modY, final int modZ) {
        return getWorld().getBlockAt(getX() + modX, getY() + modY, getZ() + modZ);
    }

    @Override
    public Block getRelative(BlockFace face) {
        return getRelative(face, 1);
    }

    @Override
    public Block getRelative(BlockFace face, int distance) {
        return getRelative(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    @Override
    public BlockFace getFace(final Block block) {
        BlockFace[] values = BlockFace.values();

        for (BlockFace face : values) {
            if ((this.getX() + face.getModX() == block.getX()) &&
                (this.getY() + face.getModY() == block.getY()) &&
                (this.getZ() + face.getModZ() == block.getZ())
            ) {
                return face;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "CraftBlock{" + "chunk=" + chunk + ",x=" + x + ",y=" + y + ",z=" + z + ",type=" + getType() + ",data=" + getData() + '}';
    }

    public static BlockFace notchToBlockFace(EnumFacing notch) {
        if (notch == null) return BlockFace.SELF;
        switch (notch) {
        case DOWN:
            return BlockFace.DOWN;
        case UP:
            return BlockFace.UP;
        case NORTH:
            return BlockFace.NORTH;
        case SOUTH:
            return BlockFace.SOUTH;
        case WEST:
            return BlockFace.WEST;
        case EAST:
            return BlockFace.EAST;
        default:
            return BlockFace.SELF;
        }
    }

    public static EnumFacing blockFaceToNotch(BlockFace face) {
        switch (face) {
        case DOWN:
            return EnumFacing.DOWN;
        case UP:
            return EnumFacing.UP;
        case NORTH:
            return EnumFacing.NORTH;
        case SOUTH:
            return EnumFacing.SOUTH;
        case WEST:
            return EnumFacing.WEST;
        case EAST:
            return EnumFacing.EAST;
        default:
            return null;
        }
    }


    @Override
    public BlockState getState() {
        // Paper start - allow disabling the use of snapshots
        return getState(true);
    }
    @Override
    public BlockState getState(boolean useSnapshot) {
        boolean prev = CraftBlockEntityState.DISABLE_SNAPSHOT;
        CraftBlockEntityState.DISABLE_SNAPSHOT = !useSnapshot;
        try {
            return getState0();
        } finally {
            CraftBlockEntityState.DISABLE_SNAPSHOT = prev;
        }
    }
    public BlockState getState0() {
        // Paper end
        Material material = getType();

        switch (material) {
        case SIGN:
        case SIGN_POST:
        case WALL_SIGN:
            return new CraftSign(this);
        case CHEST:
        case TRAPPED_CHEST:
            return new CraftChest(this);
        case BURNING_FURNACE:
        case FURNACE:
            return new CraftFurnace(this);
        case DISPENSER:
            return new CraftDispenser(this);
        case DROPPER:
            return new CraftDropper(this);
        case END_GATEWAY:
            return new CraftEndGateway(this);
        case HOPPER:
            return new CraftHopper(this);
        case MOB_SPAWNER:
            return new CraftCreatureSpawner(this);
        case NOTE_BLOCK:
            return new CraftNoteBlock(this);
        case JUKEBOX:
            return new CraftJukebox(this);
        case BREWING_STAND:
            return new CraftBrewingStand(this);
        case SKULL:
            return new CraftSkull(this);
        case COMMAND:
        case COMMAND_CHAIN:
        case COMMAND_REPEATING:
            return new CraftCommandBlock(this);
        case BEACON:
            return new CraftBeacon(this);
        case BANNER:
        case WALL_BANNER:
        case STANDING_BANNER:
            return new CraftBanner(this);
        case FLOWER_POT:
            return new CraftFlowerPot(this);
        case STRUCTURE_BLOCK:
            return new CraftStructureBlock(this);
        case WHITE_SHULKER_BOX:
        case ORANGE_SHULKER_BOX:
        case MAGENTA_SHULKER_BOX:
        case LIGHT_BLUE_SHULKER_BOX:
        case YELLOW_SHULKER_BOX:
        case LIME_SHULKER_BOX:
        case PINK_SHULKER_BOX:
        case GRAY_SHULKER_BOX:
        case SILVER_SHULKER_BOX:
        case CYAN_SHULKER_BOX:
        case PURPLE_SHULKER_BOX:
        case BLUE_SHULKER_BOX:
        case BROWN_SHULKER_BOX:
        case GREEN_SHULKER_BOX:
        case RED_SHULKER_BOX:
        case BLACK_SHULKER_BOX:
            return new CraftShulkerBox(this);
        case ENCHANTMENT_TABLE:
            return new CraftEnchantingTable(this);
        case ENDER_CHEST:
            return new CraftEnderChest(this);
        case DAYLIGHT_DETECTOR:
        case DAYLIGHT_DETECTOR_INVERTED:
            return new CraftDaylightDetector(this);
        case REDSTONE_COMPARATOR_OFF:
        case REDSTONE_COMPARATOR_ON:
            return new CraftComparator(this);
        case BED_BLOCK:
            return new CraftBed(this);
        default:
            TileEntity tileEntity = chunk.getCraftWorld().getTileEntityAt(x, y, z);
            if (tileEntity != null) {
                // block with unhandled TileEntity:
                return new CraftBlockEntityState<TileEntity>(this, (Class<TileEntity>) tileEntity.getClass());
            } else {
                // Block without TileEntity:
                return new CraftBlockState(this);
            }
        }
    }

    @Override
    public Biome getBiome() {
        return getWorld().getBiome(x, z);
    }

    @Override
    public void setBiome(Biome bio) {
        getWorld().setBiome(x, z, bio);
    }

    public static Biome biomeBaseToBiome(net.minecraft.world.biome.Biome base) {
        if (base == null) {
            return null;
        }

        return Biome.valueOf(net.minecraft.world.biome.Biome.field_185377_q.func_177774_c(base).func_110623_a().toUpperCase(java.util.Locale.ENGLISH));
    }

    public static net.minecraft.world.biome.Biome biomeToBiomeBase(Biome bio) {
        if (bio == null) {
            return null;
        }

        return net.minecraft.world.biome.Biome.field_185377_q.func_82594_a(new ResourceLocation(bio.name().toLowerCase(java.util.Locale.ENGLISH)));
    }

    @Override
    public double getTemperature() {
        return getWorld().getTemperature(x, z);
    }

    @Override
    public double getHumidity() {
        return getWorld().getHumidity(x, z);
    }

    @Override
    public boolean isBlockPowered() {
        return chunk.getHandle().func_177412_p().func_175676_y(new BlockPos(x, y, z)) > 0;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return chunk.getHandle().func_177412_p().func_175640_z(new BlockPos(x, y, z));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CraftBlock)) return false;
        CraftBlock other = (CraftBlock) o;

        return this.x == other.x && this.y == other.y && this.z == other.z && this.getWorld().equals(other.getWorld());
    }

    @Override
    public int hashCode() {
        return this.y << 24 ^ this.x ^ this.z ^ this.getWorld().hashCode();
    }

    @Override
    public boolean isBlockFacePowered(BlockFace face) {
        return chunk.getHandle().func_177412_p().func_175709_b(new BlockPos(x, y, z), blockFaceToNotch(face));
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        int power = chunk.getHandle().func_177412_p().func_175651_c(new BlockPos(x, y, z), blockFaceToNotch(face));

        Block relative = getRelative(face);
        if (relative.getType() == Material.REDSTONE_WIRE) {
            return Math.max(power, relative.getData()) > 0;
        }

        return power > 0;
    }

    @Override
    public int getBlockPower(BlockFace face) {
        int power = 0;
        BlockRedstoneWire wire = Blocks.field_150488_af;
        net.minecraft.world.World world = chunk.getHandle().func_177412_p();
        if ((face == BlockFace.DOWN || face == BlockFace.SELF) && world.func_175709_b(new BlockPos(x, y - 1, z), EnumFacing.DOWN)) power = wire.func_176342_a(world, new BlockPos(x, y - 1, z), power);
        if ((face == BlockFace.UP || face == BlockFace.SELF) && world.func_175709_b(new BlockPos(x, y + 1, z), EnumFacing.UP)) power = wire.func_176342_a(world, new BlockPos(x, y + 1, z), power);
        if ((face == BlockFace.EAST || face == BlockFace.SELF) && world.func_175709_b(new BlockPos(x + 1, y, z), EnumFacing.EAST)) power = wire.func_176342_a(world, new BlockPos(x + 1, y, z), power);
        if ((face == BlockFace.WEST || face == BlockFace.SELF) && world.func_175709_b(new BlockPos(x - 1, y, z), EnumFacing.WEST)) power = wire.func_176342_a(world, new BlockPos(x - 1, y, z), power);
        if ((face == BlockFace.NORTH || face == BlockFace.SELF) && world.func_175709_b(new BlockPos(x, y, z - 1), EnumFacing.NORTH)) power = wire.func_176342_a(world, new BlockPos(x, y, z - 1), power);
        if ((face == BlockFace.SOUTH || face == BlockFace.SELF) && world.func_175709_b(new BlockPos(x, y, z + 1), EnumFacing.SOUTH)) power = wire.func_176342_a(world, new BlockPos(x, y, z - 1), power);
        return power > 0 ? power : (face == BlockFace.SELF ? isBlockIndirectlyPowered() : isBlockFaceIndirectlyPowered(face)) ? 15 : 0;
    }

    @Override
    public int getBlockPower() {
        return getBlockPower(BlockFace.SELF);
    }

    @Override
    public boolean isEmpty() {
        return getType() == Material.AIR;
    }

    @Override
    public boolean isLiquid() {
        return (getType() == Material.WATER) || (getType() == Material.STATIONARY_WATER) || (getType() == Material.LAVA) || (getType() == Material.STATIONARY_LAVA);
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return PistonMoveReaction.getById(getNMSBlock().func_149656_h(getNMSBlock().func_176203_a(getData())).ordinal());
    }

    private boolean itemCausesDrops(ItemStack item) {
        net.minecraft.block.Block block = this.getNMSBlock();
        net.minecraft.item.Item itemType = item != null ? net.minecraft.item.Item.func_150899_d(item.getTypeId()) : null;
        return block != null && (block.func_176223_P().func_185904_a().func_76229_l() || (itemType != null && itemType.func_150897_b(block.func_176223_P())));
    }

    @Override
    public boolean breakNaturally() {
        // Order matters here, need to drop before setting to air so skulls can get their data
        net.minecraft.block.Block block = this.getNMSBlock();
        byte data = getData();
        boolean result = false;

        if (block != null && block != Blocks.field_150350_a) {
            block.func_180653_a(chunk.getHandle().func_177412_p(), new BlockPos(x, y, z), block.func_176203_a(data), 1.0F, 0);
            result = true;
        }

        setTypeId(Material.AIR.getId());
        return result;
    }

    @Override
    public boolean breakNaturally(ItemStack item) {
        if (itemCausesDrops(item)) {
            return breakNaturally();
        } else {
            return setTypeId(Material.AIR.getId());
        }
    }

    @Override
    public Collection<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList<ItemStack>();

        net.minecraft.block.Block block = this.getNMSBlock();
        if (block != Blocks.field_150350_a) {
            IBlockState data = getData0();
            // based on nms.Block.dropNaturally
            int count = block.func_149679_a(0, chunk.getHandle().func_177412_p().field_73012_v);
            for (int i = 0; i < count; ++i) {
                Item item = block.func_180660_a(data, chunk.getHandle().func_177412_p().field_73012_v, 0);
                if (item != Items.field_190931_a) {
                    // Skulls are special, their data is based on the tile entity
                    if (Blocks.field_150465_bP == block) {
                        net.minecraft.item.ItemStack nmsStack = new net.minecraft.item.ItemStack(item, 1, block.func_180651_a(data));
                        TileEntitySkull tileentityskull = (TileEntitySkull) chunk.getHandle().func_177412_p().func_175625_s(new BlockPos(x, y, z));

                        if (tileentityskull.func_145904_a() == 3 && tileentityskull.func_152108_a() != null) {
                            nmsStack.func_77982_d(new NBTTagCompound());
                            NBTTagCompound nbttagcompound = new NBTTagCompound();

                            NBTUtil.func_180708_a(nbttagcompound, tileentityskull.func_152108_a());
                            nmsStack.func_77978_p().func_74782_a("SkullOwner", nbttagcompound);
                        }

                        drops.add(CraftItemStack.asBukkitCopy(nmsStack));
                        // We don't want to drop cocoa blocks, we want to drop cocoa beans.
                    } else if (Blocks.field_150375_by == block) {
                        int age = data.func_177229_b(BlockCocoa.field_176501_a);
                        int dropAmount = (age >= 2 ? 3 : 1);
                        for (int j = 0; j < dropAmount; ++j) {
                            drops.add(new ItemStack(Material.INK_SACK, 1, (short) 3));
                        }
                    } else {
                        drops.add(new ItemStack(org.bukkit.craftbukkit.util.CraftMagicNumbers.getMaterial(item), 1, (short) block.func_180651_a(data)));
                    }
                }
            }
        }
        return drops;
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack item) {
        if (itemCausesDrops(item)) {
            return getDrops();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        chunk.getCraftWorld().getBlockMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return chunk.getCraftWorld().getBlockMetadata().getMetadata(this, metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return chunk.getCraftWorld().getBlockMetadata().hasMetadata(this, metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        chunk.getCraftWorld().getBlockMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }
}
