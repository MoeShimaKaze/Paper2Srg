package org.bukkit.craftbukkit.entity;

import com.destroystokyo.paper.Title;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.chat.BaseComponent;

import net.minecraft.network.play.server.SPacketTitle.Type;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Achievement;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ManuallyAbandonedConversationCanceller;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.conversations.ConversationTracker;
import org.bukkit.craftbukkit.CraftEffect;
import org.bukkit.craftbukkit.CraftOfflinePlayer;
import org.bukkit.craftbukkit.CraftParticle;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.CraftStatistic;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.advancement.CraftAdvancement;
import org.bukkit.craftbukkit.advancement.CraftAdvancementProgress;
import org.bukkit.craftbukkit.map.CraftMapView;
import org.bukkit.craftbukkit.map.RenderData;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboard;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.bukkit.scoreboard.Scoreboard;

import javax.annotation.Nullable;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.network.play.server.SPacketEffect;
import net.minecraft.network.play.server.SPacketEntityProperties;
import net.minecraft.network.play.server.SPacketMaps;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.network.play.server.SPacketPlayerListHeaderFooter;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.network.play.server.SPacketSpawnPosition;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.network.play.server.SPacketUpdateHealth;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapDecoration;

@DelegateDeserialization(CraftOfflinePlayer.class)
public class CraftPlayer extends CraftHumanEntity implements Player {
    private long firstPlayed = 0;
    private long lastPlayed = 0;
    private boolean hasPlayedBefore = false;
    private final ConversationTracker conversationTracker = new ConversationTracker();
    private final Set<String> channels = new HashSet<String>();
    private final Map<UUID, Set<WeakReference<Plugin>>> hiddenPlayers = new HashMap<>();
    private static final WeakHashMap<Plugin, WeakReference<Plugin>> pluginWeakReferences = new WeakHashMap<>();
    private int hash = 0;
    private double health = 20;
    private boolean scaledHealth = false;
    private double healthScale = 20;
    // Paper start
    private org.bukkit.event.player.PlayerResourcePackStatusEvent.Status resourcePackStatus;
    private String resourcePackHash;
    private static final boolean DISABLE_CHANNEL_LIMIT = System.getProperty("paper.disableChannelLimit") != null; // Paper - add a flag to disable the channel limit
    // Paper end

    public CraftPlayer(CraftServer server, EntityPlayerMP entity) {
        super(server, entity);

        firstPlayed = System.currentTimeMillis();
    }

    public GameProfile getProfile() {
        return getHandle().func_146103_bH();
    }

    @Override
    public boolean isOp() {
        return server.getHandle().func_152596_g(getProfile());
    }

    @Override
    public void setOp(boolean value) {
        if (value == isOp()) return;

        if (value) {
            server.getHandle().func_152605_a(getProfile());
        } else {
            server.getHandle().func_152610_b(getProfile());
        }

        perm.recalculatePermissions();
    }

    @Override
    public boolean isOnline() {
        return server.getPlayer(getUniqueId()) != null;
    }

    @Override
    public InetSocketAddress getAddress() {
        if (getHandle().field_71135_a == null) return null;

        SocketAddress addr = getHandle().field_71135_a.field_147371_a.func_74430_c();
        if (addr instanceof InetSocketAddress) {
            return (InetSocketAddress) addr;
        } else {
            return null;
        }
    }

    // Paper start - Implement NetworkClient
    @Override
    public int getProtocolVersion() {
        if (getHandle().field_71135_a == null) return -1;
        return getHandle().field_71135_a.field_147371_a.protocolVersion;
    }

    @Override
    public InetSocketAddress getVirtualHost() {
        if (getHandle().field_71135_a == null) return null;
        return getHandle().field_71135_a.field_147371_a.virtualHost;
    }
    // Paper end

    @Override
    public double getEyeHeight(boolean ignorePose) {
        if (ignorePose) {
            return 1.62D;
        } else {
            return getEyeHeight();
        }
    }

    @Override
    public void sendRawMessage(String message) {
        if (getHandle().field_71135_a == null) return;

        for (ITextComponent component : CraftChatMessage.fromString(message)) {
            getHandle().field_71135_a.func_147359_a(new SPacketChat(component));
        }
    }

    @Override
    public void sendMessage(String message) {
        if (!conversationTracker.isConversingModaly()) {
            this.sendRawMessage(message);
        }
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String message : messages) {
            sendMessage(message);
        }
    }

    // Paper start
    @Override
    public void sendActionBar(String message) {
        if (getHandle().field_71135_a == null || message == null || message.isEmpty()) return;
        getHandle().field_71135_a.func_147359_a(new SPacketChat(new TextComponentString(message), ChatType.GAME_INFO));
    }

    @Override
    public void sendActionBar(char alternateChar, String message) {
        if (message == null || message.isEmpty()) return;
        sendActionBar(org.bukkit.ChatColor.translateAlternateColorCodes(alternateChar, message));
    }

    @Override
    public void setPlayerListHeaderFooter(BaseComponent[] header, BaseComponent[] footer) {
        SPacketPlayerListHeaderFooter packet = new SPacketPlayerListHeaderFooter();
        packet.header = header;
        packet.footer = footer;
        getHandle().field_71135_a.func_147359_a(packet);
    }

    @Override
    public void setPlayerListHeaderFooter(BaseComponent header, BaseComponent footer) {
        this.setPlayerListHeaderFooter(header == null ? null : new BaseComponent[]{header},
                footer == null ? null : new BaseComponent[]{footer});
    }


    @Override
    public void setTitleTimes(int fadeInTicks, int stayTicks, int fadeOutTicks) {
        getHandle().field_71135_a.func_147359_a(new SPacketTitle(SPacketTitle.Type.TIMES, (BaseComponent[]) null, fadeInTicks, stayTicks, fadeOutTicks));
    }

    @Override
    public void setSubtitle(BaseComponent[] subtitle) {
        getHandle().field_71135_a.func_147359_a(new SPacketTitle(SPacketTitle.Type.SUBTITLE, subtitle, 0, 0, 0));
    }

    @Override
    public void setSubtitle(BaseComponent subtitle) {
        setSubtitle(new BaseComponent[]{subtitle});
    }

    @Override
    public void showTitle(BaseComponent[] title) {
        getHandle().field_71135_a.func_147359_a(new SPacketTitle(SPacketTitle.Type.TITLE, title, 0, 0, 0));
    }

    @Override
    public void showTitle(BaseComponent title) {
        showTitle(new BaseComponent[]{title});
    }

    @Override
    public void showTitle(BaseComponent[] title, BaseComponent[] subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        setTitleTimes(fadeInTicks, stayTicks, fadeOutTicks);
        setSubtitle(subtitle);
        showTitle(title);
    }

    @Override
    public void showTitle(BaseComponent title, BaseComponent subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        setTitleTimes(fadeInTicks, stayTicks, fadeOutTicks);
        setSubtitle(subtitle);
        showTitle(title);
    }

    @Override
    public void sendTitle(Title title) {
        Preconditions.checkNotNull(title, "Title is null");
        setTitleTimes(title.getFadeIn(), title.getStay(), title.getFadeOut());
        setSubtitle(title.getSubtitle() == null ? new BaseComponent[0] : title.getSubtitle());
        showTitle(title.getTitle());
    }

    @Override
    public void updateTitle(Title title) {
        Preconditions.checkNotNull(title, "Title is null");
        setTitleTimes(title.getFadeIn(), title.getStay(), title.getFadeOut());
        if (title.getSubtitle() != null) {
            setSubtitle(title.getSubtitle());
        }
        showTitle(title.getTitle());
    }

    @Override
    public void hideTitle() {
        getHandle().field_71135_a.func_147359_a(new SPacketTitle(SPacketTitle.Type.CLEAR, (BaseComponent[]) null, 0, 0, 0));
    }
    // Paper end

    @Override
    public String getDisplayName() {
        return getHandle().displayName;
    }

    @Override
    public void setDisplayName(final String name) {
        getHandle().displayName = name == null ? getName() : name;
    }

    @Override
    public String getPlayerListName() {
        return getHandle().listName == null ? getName() : CraftChatMessage.fromComponent(getHandle().listName, TextFormatting.WHITE);
    }

    @Override
    public void setPlayerListName(String name) {
        if (name == null) {
            name = getName();
        }
        getHandle().listName = name.equals(getName()) ? null : CraftChatMessage.fromString(name)[0];
        for (EntityPlayerMP player : server.getHandle().field_72404_b) {
            if (player.getBukkitEntity().canSee(this)) {
                player.field_71135_a.func_147359_a(new SPacketPlayerListItem(SPacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, getHandle()));
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OfflinePlayer)) {
            return false;
        }
        OfflinePlayer other = (OfflinePlayer) obj;
        if ((this.getUniqueId() == null) || (other.getUniqueId() == null)) {
            return false;
        }

        boolean uuidEquals = this.getUniqueId().equals(other.getUniqueId());
        boolean idEquals = true;

        if (other instanceof CraftPlayer) {
            idEquals = this.getEntityId() == ((CraftPlayer) other).getEntityId();
        }

        return uuidEquals && idEquals;
    }

    @Override
    public void kickPlayer(String message) {
        org.spigotmc.AsyncCatcher.catchOp( "player kick"); // Spigot
        if (getHandle().field_71135_a == null) return;

        getHandle().field_71135_a.disconnect(message == null ? "" : message);
    }

    @Override
    public void setCompassTarget(Location loc) {
        if (getHandle().field_71135_a == null) return;

        // Do not directly assign here, from the packethandler we'll assign it.
        getHandle().field_71135_a.func_147359_a(new SPacketSpawnPosition(new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())));
    }

    @Override
    public Location getCompassTarget() {
        return getHandle().compassTarget;
    }

    @Override
    public void chat(String msg) {
        if (getHandle().field_71135_a == null) return;

        getHandle().field_71135_a.chat(msg, false);
    }

    @Override
    public boolean performCommand(String command) {
        return server.dispatchCommand(this, command);
    }

    @Override
    public void playNote(Location loc, byte instrument, byte note) {
        if (getHandle().field_71135_a == null) return;

        String instrumentName = null;
        switch (instrument) {
        case 0:
            instrumentName = "harp";
            break;
        case 1:
            instrumentName = "basedrum";
            break;
        case 2:
            instrumentName = "snare";
            break;
        case 3:
            instrumentName = "hat";
            break;
        case 4:
            instrumentName = "bass";
            break;
        case 5:
            instrumentName = "flute";
            break;
        case 6:
            instrumentName = "bell";
            break;
        case 7:
            instrumentName = "guitar";
            break;
        case 8:
            instrumentName = "chime";
            break;
        case 9:
            instrumentName = "xylophone";
            break;
        }

        float f = (float) Math.pow(2.0D, (note - 12.0D) / 12.0D);
        getHandle().field_71135_a.func_147359_a(new SPacketSoundEffect(CraftSound.getSoundEffect("block.note." + instrumentName), net.minecraft.util.SoundCategory.RECORDS, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 3.0f, f));
    }

    @Override
    public void playNote(Location loc, Instrument instrument, Note note) {
        if (getHandle().field_71135_a == null) return;

        String instrumentName = null;
        switch (instrument.ordinal()) {
            case 0:
                instrumentName = "harp";
                break;
            case 1:
                instrumentName = "basedrum";
                break;
            case 2:
                instrumentName = "snare";
                break;
            case 3:
                instrumentName = "hat";
                break;
            case 4:
                instrumentName = "bass";
                break;
            case 5:
                instrumentName = "flute";
                break;
            case 6:
                instrumentName = "bell";
                break;
            case 7:
                instrumentName = "guitar";
                break;
            case 8:
                instrumentName = "chime";
                break;
            case 9:
                instrumentName = "xylophone";
                break;
        }
        float f = (float) Math.pow(2.0D, (note.getId() - 12.0D) / 12.0D);
        getHandle().field_71135_a.func_147359_a(new SPacketSoundEffect(CraftSound.getSoundEffect("block.note." + instrumentName), net.minecraft.util.SoundCategory.RECORDS, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 3.0f, f));
    }

    @Override
    public void playSound(Location loc, Sound sound, float volume, float pitch) {
        playSound(loc, sound, org.bukkit.SoundCategory.MASTER, volume, pitch);
    }

    @Override
    public void playSound(Location loc, String sound, float volume, float pitch) {
        playSound(loc, sound, org.bukkit.SoundCategory.MASTER, volume, pitch);
    }

    @Override
    public void playSound(Location loc, Sound sound, org.bukkit.SoundCategory category, float volume, float pitch) {
        if (loc == null || sound == null || category == null || getHandle().field_71135_a == null) return;

        SPacketSoundEffect packet = new SPacketSoundEffect(CraftSound.getSoundEffect(CraftSound.getSound(sound)), net.minecraft.util.SoundCategory.valueOf(category.name()), loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
        getHandle().field_71135_a.func_147359_a(packet);
    }

    @Override
    public void playSound(Location loc, String sound, org.bukkit.SoundCategory category, float volume, float pitch) {
        if (loc == null || sound == null || category == null || getHandle().field_71135_a == null) return;

        SPacketCustomSound packet = new SPacketCustomSound(sound, net.minecraft.util.SoundCategory.valueOf(category.name()), loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
        getHandle().field_71135_a.func_147359_a(packet);
    }

    @Override
    public void stopSound(Sound sound) {
        stopSound(sound, null);
    }

    @Override
    public void stopSound(String sound) {
        stopSound(sound, null);
    }

    @Override
    public void stopSound(Sound sound, org.bukkit.SoundCategory category) {
        stopSound(CraftSound.getSound(sound), category);
    }

    @Override
    public void stopSound(String sound, org.bukkit.SoundCategory category) {
        if (getHandle().field_71135_a == null) return;
        PacketBuffer packetdataserializer = new PacketBuffer(Unpooled.buffer());

        packetdataserializer.func_180714_a(category == null ? "" : net.minecraft.util.SoundCategory.valueOf(category.name()).func_187948_a());
        packetdataserializer.func_180714_a(sound);
        getHandle().field_71135_a.func_147359_a(new SPacketCustomPayload("MC|StopSound", packetdataserializer));
    }

    @Override
    public void playEffect(Location loc, Effect effect, int data) {
        if (getHandle().field_71135_a == null) return;

        spigot().playEffect(loc, effect, data, 0, 0, 0, 0, 1, 1, 64); // Spigot
    }

    @Override
    public <T> void playEffect(Location loc, Effect effect, T data) {
        if (data != null) {
            Validate.isTrue(effect.getData() != null && effect.getData().isAssignableFrom(data.getClass()), "Wrong kind of data for this effect!");
        } else {
            Validate.isTrue(effect.getData() == null, "Wrong kind of data for this effect!");
        }

        int datavalue = data == null ? 0 : CraftEffect.getDataValue(effect, data);
        playEffect(loc, effect, datavalue);
    }

    @Override
    public void sendBlockChange(Location loc, Material material, byte data) {
        sendBlockChange(loc, material.getId(), data);
    }

    @Override
    public void sendBlockChange(Location loc, int material, byte data) {
        if (getHandle().field_71135_a == null) return;

        SPacketBlockChange packet = new SPacketBlockChange(((CraftWorld) loc.getWorld()).getHandle(), new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

        packet.field_148883_d = CraftMagicNumbers.getBlock(material).func_176203_a(data);
        getHandle().field_71135_a.func_147359_a(packet);
    }

    @Override
    public void sendSignChange(Location loc, String[] lines) {
        if (getHandle().field_71135_a == null) {
            return;
        }

        if (lines == null) {
            lines = new String[4];
        }

        Validate.notNull(loc, "Location can not be null");
        if (lines.length < 4) {
            throw new IllegalArgumentException("Must have at least 4 lines");
        }

        ITextComponent[] components = CraftSign.sanitizeLines(lines);
        TileEntitySign sign = new TileEntitySign();
        sign.func_174878_a(new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        System.arraycopy(components, 0, sign.field_145915_a, 0, sign.field_145915_a.length);

        getHandle().field_71135_a.func_147359_a(sign.func_189518_D_());
    }

    @Override
    public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
        if (getHandle().field_71135_a == null) return false;

        /*
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        int cx = x >> 4;
        int cz = z >> 4;

        if (sx <= 0 || sy <= 0 || sz <= 0) {
            return false;
        }

        if ((x + sx - 1) >> 4 != cx || (z + sz - 1) >> 4 != cz || y < 0 || y + sy > 128) {
            return false;
        }

        if (data.length != (sx * sy * sz * 5) / 2) {
            return false;
        }

        Packet51MapChunk packet = new Packet51MapChunk(x, y, z, sx, sy, sz, data);

        getHandle().playerConnection.sendPacket(packet);

        return true;
        */

        throw new NotImplementedException("Chunk changes do not yet work"); // TODO: Chunk changes.
    }

    @Override
    public void sendMap(MapView map) {
        if (getHandle().field_71135_a == null) return;

        RenderData data = ((CraftMapView) map).render(this);
        Collection<MapDecoration> icons = new ArrayList<MapDecoration>();
        for (MapCursor cursor : data.cursors) {
            if (cursor.isVisible()) {
                icons.add(new MapDecoration(MapDecoration.Type.func_191159_a(cursor.getRawType()), cursor.getX(), cursor.getY(), cursor.getDirection()));
            }
        }

        SPacketMaps packet = new SPacketMaps(map.getId(), map.getScale().getValue(), true, icons, data.buffer, 0, 0, 128, 128);
        getHandle().field_71135_a.func_147359_a(packet);
    }

    @Override
    public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
        EntityPlayerMP entity = getHandle();

        if (getHealth() == 0 || entity.field_70128_L) {
            return false;
        }

        if (entity.field_71135_a == null) {
           return false;
        }

        if (entity.func_184207_aI()) {
            return false;
        }

        // From = Players current Location
        Location from = this.getLocation();
        // To = Players new Location if Teleport is Successful
        Location to = location;
        // Create & Call the Teleport Event.
        PlayerTeleportEvent event = new PlayerTeleportEvent(this, from, to, cause);
        server.getPluginManager().callEvent(event);

        // Return False to inform the Plugin that the Teleport was unsuccessful/cancelled.
        if (event.isCancelled()) {
            return false;
        }

        // If this player is riding another entity, we must dismount before teleporting.
        entity.func_184210_p();

        // Update the From Location
        from = event.getFrom();
        // Grab the new To Location dependent on whether the event was cancelled.
        to = event.getTo();
        // Grab the To and From World Handles.
        WorldServer fromWorld = ((CraftWorld) from.getWorld()).getHandle();
        WorldServer toWorld = ((CraftWorld) to.getWorld()).getHandle();

        // Close any foreign inventory
        if (getHandle().field_71070_bA != getHandle().field_71069_bz) {
            getHandle().func_71053_j();
        }

        // Check if the fromWorld and toWorld are the same.
        if (fromWorld == toWorld) {
            entity.field_71135_a.teleport(to);
        } else {
            // Paper - Configurable suffocation check
            server.getHandle().moveToWorld(entity, toWorld.dimension, true, to, !toWorld.paperConfig.disableTeleportationSuffocationCheck);
        }
        return true;
    }

    // Paper start - Ugly workaround for SPIGOT-1915 & GH-114
    @Override
    public boolean setPassenger(org.bukkit.entity.Entity passenger) {
        boolean wasSet = super.setPassenger(passenger);
        if (wasSet) {
            this.getHandle().field_71135_a.func_147359_a(new net.minecraft.network.play.server.SPacketSetPassengers(this.getHandle()));
        }
        return wasSet;
    }
    // Paper end

    @Override
    public void setSneaking(boolean sneak) {
        getHandle().func_70095_a(sneak);
    }

    @Override
    public boolean isSneaking() {
        return getHandle().func_70093_af();
    }

    @Override
    public boolean isSprinting() {
        return getHandle().func_70051_ag();
    }

    @Override
    public void setSprinting(boolean sprinting) {
        getHandle().func_70031_b(sprinting);
    }

    @Override
    public void loadData() {
        server.getHandle().field_72412_k.func_75752_b(getHandle());
    }

    @Override
    public void saveData() {
        server.getHandle().field_72412_k.func_75753_a(getHandle());
    }

    @Deprecated
    @Override
    public void updateInventory() {
        getHandle().func_71120_a(getHandle().field_71070_bA);
    }

    @Override
    public void setSleepingIgnored(boolean isSleeping) {
        getHandle().fauxSleeping = isSleeping;
        ((CraftWorld) getWorld()).getHandle().checkSleepStatus();
    }

    @Override
    public boolean isSleepingIgnored() {
        return getHandle().fauxSleeping;
    }

    @Override
    public void awardAchievement(Achievement achievement) {
        throw new UnsupportedOperationException("Not supported in this Minecraft version.");
    }

    @Override
    public void removeAchievement(Achievement achievement) {
        throw new UnsupportedOperationException("Not supported in this Minecraft version.");
    }

    @Override
    public boolean hasAchievement(Achievement achievement) {
        throw new UnsupportedOperationException("Not supported in this Minecraft version.");
    }

    @Override
    public void incrementStatistic(Statistic statistic) {
        incrementStatistic(statistic, 1);
    }

    @Override
    public void decrementStatistic(Statistic statistic) {
        decrementStatistic(statistic, 1);
    }

    @Override
    public int getStatistic(Statistic statistic) {
        Validate.notNull(statistic, "Statistic cannot be null");
        Validate.isTrue(statistic.getType() == org.bukkit.Statistic.Type.UNTYPED, "Must supply additional paramater for this statistic");
        return getHandle().func_147099_x().func_77444_a(CraftStatistic.getNMSStatistic(statistic));
    }

    @Override
    public void incrementStatistic(Statistic statistic, int amount) {
        Validate.isTrue(amount > 0, "Amount must be greater than 0");
        setStatistic(statistic, getStatistic(statistic) + amount);
    }

    @Override
    public void decrementStatistic(Statistic statistic, int amount) {
        Validate.isTrue(amount > 0, "Amount must be greater than 0");
        setStatistic(statistic, getStatistic(statistic) - amount);
    }

    @Override
    public void setStatistic(Statistic statistic, int newValue) {
        Validate.notNull(statistic, "Statistic cannot be null");
        Validate.isTrue(statistic.getType() == org.bukkit.Statistic.Type.UNTYPED, "Must supply additional paramater for this statistic");
        Validate.isTrue(newValue >= 0, "Value must be greater than or equal to 0");
        net.minecraft.stats.StatBase nmsStatistic = CraftStatistic.getNMSStatistic(statistic);
        getHandle().func_147099_x().func_150873_a(getHandle(), nmsStatistic, newValue);
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) {
        incrementStatistic(statistic, material, 1);
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) {
        decrementStatistic(statistic, material, 1);
    }

    @Override
    public int getStatistic(Statistic statistic, Material material) {
        Validate.notNull(statistic, "Statistic cannot be null");
        Validate.notNull(material, "Material cannot be null");
        Validate.isTrue(statistic.getType() == org.bukkit.Statistic.Type.BLOCK || statistic.getType() == org.bukkit.Statistic.Type.ITEM, "This statistic does not take a Material parameter");
        net.minecraft.stats.StatBase nmsStatistic = CraftStatistic.getMaterialStatistic(statistic, material);
        Validate.notNull(nmsStatistic, "The supplied Material does not have a corresponding statistic");
        return getHandle().func_147099_x().func_77444_a(nmsStatistic);
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int amount) {
        Validate.isTrue(amount > 0, "Amount must be greater than 0");
        setStatistic(statistic, material, getStatistic(statistic, material) + amount);
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int amount) {
        Validate.isTrue(amount > 0, "Amount must be greater than 0");
        setStatistic(statistic, material, getStatistic(statistic, material) - amount);
    }

    @Override
    public void setStatistic(Statistic statistic, Material material, int newValue) {
        Validate.notNull(statistic, "Statistic cannot be null");
        Validate.notNull(material, "Material cannot be null");
        Validate.isTrue(newValue >= 0, "Value must be greater than or equal to 0");
        Validate.isTrue(statistic.getType() == org.bukkit.Statistic.Type.BLOCK || statistic.getType() == org.bukkit.Statistic.Type.ITEM, "This statistic does not take a Material parameter");
        net.minecraft.stats.StatBase nmsStatistic = CraftStatistic.getMaterialStatistic(statistic, material);
        Validate.notNull(nmsStatistic, "The supplied Material does not have a corresponding statistic");
        getHandle().func_147099_x().func_150873_a(getHandle(), nmsStatistic, newValue);
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) {
        incrementStatistic(statistic, entityType, 1);
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) {
        decrementStatistic(statistic, entityType, 1);
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) {
        Validate.notNull(statistic, "Statistic cannot be null");
        Validate.notNull(entityType, "EntityType cannot be null");
        Validate.isTrue(statistic.getType() == org.bukkit.Statistic.Type.ENTITY, "This statistic does not take an EntityType parameter");
        net.minecraft.stats.StatBase nmsStatistic = CraftStatistic.getEntityStatistic(statistic, entityType);
        Validate.notNull(nmsStatistic, "The supplied EntityType does not have a corresponding statistic");
        return getHandle().func_147099_x().func_77444_a(nmsStatistic);
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        Validate.isTrue(amount > 0, "Amount must be greater than 0");
        setStatistic(statistic, entityType, getStatistic(statistic, entityType) + amount);
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        Validate.isTrue(amount > 0, "Amount must be greater than 0");
        setStatistic(statistic, entityType, getStatistic(statistic, entityType) - amount);
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
        Validate.notNull(statistic, "Statistic cannot be null");
        Validate.notNull(entityType, "EntityType cannot be null");
        Validate.isTrue(newValue >= 0, "Value must be greater than or equal to 0");
        Validate.isTrue(statistic.getType() == org.bukkit.Statistic.Type.ENTITY, "This statistic does not take an EntityType parameter");
        net.minecraft.stats.StatBase nmsStatistic = CraftStatistic.getEntityStatistic(statistic, entityType);
        Validate.notNull(nmsStatistic, "The supplied EntityType does not have a corresponding statistic");
        getHandle().func_147099_x().func_150873_a(getHandle(), nmsStatistic, newValue);
    }

    @Override
    public void setPlayerTime(long time, boolean relative) {
        getHandle().timeOffset = time;
        getHandle().relativeTime = relative;
    }

    @Override
    public long getPlayerTimeOffset() {
        return getHandle().timeOffset;
    }

    @Override
    public long getPlayerTime() {
        return getHandle().getPlayerTime();
    }

    @Override
    public boolean isPlayerTimeRelative() {
        return getHandle().relativeTime;
    }

    @Override
    public void resetPlayerTime() {
        setPlayerTime(0, true);
    }

    @Override
    public void setPlayerWeather(WeatherType type) {
        getHandle().setPlayerWeather(type, true);
    }

    @Override
    public WeatherType getPlayerWeather() {
        return getHandle().getPlayerWeather();
    }

    @Override
    public void resetPlayerWeather() {
        getHandle().resetPlayerWeather();
    }

    @Override
    public boolean isBanned() {
        return server.getBanList(org.bukkit.BanList.Type.NAME).isBanned(getName());
    }

    @Override
    public boolean isWhitelisted() {
        return server.getHandle().func_152599_k().func_152705_a(getProfile());
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            server.getHandle().func_152601_d(getProfile());
        } else {
            server.getHandle().func_152597_c(getProfile());
        }
    }

    @Override
    public void setGameMode(GameMode mode) {
        if (getHandle().field_71135_a == null) return;

        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        getHandle().func_71033_a(GameType.func_77146_a(mode.getValue()));
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.getByValue(getHandle().field_71134_c.func_73081_b().func_77148_a());
    }

    // Paper start
    @Override
    public int applyMending(int amount) {
        EntityPlayerMP handle = getHandle();
        // Logic copied from EntityExperienceOrb and remapped to unobfuscated methods/properties
        ItemStack itemstack = EnchantmentHelper.getRandomEquippedItemWithEnchant(Enchantments.MENDING, handle);
        if (!itemstack.func_190926_b() && itemstack.hasDamage()) {

            EntityXPOrb orb = new EntityXPOrb(handle.field_70170_p);
            orb.field_70530_e = amount;
            orb.spawnReason = org.bukkit.entity.ExperienceOrb.SpawnReason.CUSTOM;
            orb.field_70165_t = handle.field_70165_t;
            orb.field_70163_u = handle.field_70163_u;
            orb.field_70161_v = handle.field_70161_v;

            int i = Math.min(orb.xpToDur(amount), itemstack.getDamage());
            org.bukkit.event.player.PlayerItemMendEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerItemMendEvent(handle, orb, itemstack, i);
            i = event.getRepairAmount();
            orb.field_70128_L = true;
            if (!event.isCancelled()) {
                amount -= orb.durToXp(i);
                itemstack.setDamage(itemstack.getDamage() - i);
            }
        }
        return amount;
    }

    @Override
    public void giveExp(int exp, boolean applyMending) {
        if (applyMending) {
            exp = this.applyMending(exp);
        }
        // Paper end
        getHandle().func_71023_q(exp);
    }

    @Override
    public void giveExpLevels(int levels) {
        getHandle().func_82242_a(levels);
    }

    @Override
    public float getExp() {
        return getHandle().field_71106_cc;
    }

    @Override
    public void setExp(float exp) {
        Preconditions.checkArgument(exp >= 0.0 && exp <= 1.0, "Experience progress must be between 0.0 and 1.0 (%s)", exp);
        getHandle().field_71106_cc = exp;
        getHandle().field_71144_ck = -1;
    }

    @Override
    public int getLevel() {
        return getHandle().field_71068_ca;
    }

    @Override
    public void setLevel(int level) {
        getHandle().field_71068_ca = level;
        getHandle().field_71144_ck = -1;
    }

    @Override
    public int getTotalExperience() {
        return getHandle().field_71067_cb;
    }

    @Override
    public void setTotalExperience(int exp) {
        getHandle().field_71067_cb = exp;
    }

    @Override
    public float getExhaustion() {
        return getHandle().func_71024_bL().field_75126_c;
    }

    @Override
    public void setExhaustion(float value) {
        getHandle().func_71024_bL().field_75126_c = value;
    }

    @Override
    public float getSaturation() {
        return getHandle().func_71024_bL().field_75125_b;
    }

    @Override
    public void setSaturation(float value) {
        getHandle().func_71024_bL().field_75125_b = value;
    }

    @Override
    public int getFoodLevel() {
        return getHandle().func_71024_bL().field_75127_a;
    }

    @Override
    public void setFoodLevel(int value) {
        getHandle().func_71024_bL().field_75127_a = value;
    }

    @Override
    public Location getBedSpawnLocation() {
        World world = getServer().getWorld(getHandle().spawnWorld);
        BlockPos bed = getHandle().func_180470_cg();

        if (world != null && bed != null) {
            bed = EntityPlayer.func_180467_a(((CraftWorld) world).getHandle(), bed, getHandle().func_82245_bX());
            if (bed != null) {
                return new Location(world, bed.func_177958_n(), bed.func_177956_o(), bed.func_177952_p());
            }
        }
        return null;
    }

    @Override
    public void setBedSpawnLocation(Location location) {
        setBedSpawnLocation(location, false);
    }

    @Override
    public void setBedSpawnLocation(Location location, boolean override) {
        if (location == null) {
            getHandle().func_180473_a(null, override);
        } else {
            getHandle().func_180473_a(new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()), override);
            getHandle().spawnWorld = location.getWorld().getName();
        }
    }

    @Nullable
    private static WeakReference<Plugin> getPluginWeakReference(@Nullable Plugin plugin) {
        return (plugin == null) ? null : pluginWeakReferences.computeIfAbsent(plugin, WeakReference::new);
    }

    @Override
    @Deprecated
    public void hidePlayer(Player player) {
        hidePlayer0(null, player);
    }

    @Override
    public void hidePlayer(Plugin plugin, Player player) {
        Validate.notNull(plugin, "Plugin cannot be null");
        Validate.isTrue(plugin.isEnabled(), "Plugin attempted to hide player while disabled");

        hidePlayer0(plugin, player);
    }

    private void hidePlayer0(@Nullable Plugin plugin, Player player) {
        Validate.notNull(player, "hidden player cannot be null");
        if (getHandle().field_71135_a == null) return;
        if (equals(player)) return;

        Set<WeakReference<Plugin>> hidingPlugins = hiddenPlayers.get(player.getUniqueId());
        if (hidingPlugins != null) {
            // Some plugins are already hiding the player. Just mark that this
            // plugin wants the player hidden too and end.
            hidingPlugins.add(getPluginWeakReference(plugin));
            return;
        }
        hidingPlugins = new HashSet<>();
        hidingPlugins.add(getPluginWeakReference(plugin));
        hiddenPlayers.put(player.getUniqueId(), hidingPlugins);

        // Remove this player from the hidden player's EntityTrackerEntry
        EntityPlayerMP other = ((CraftPlayer) player).getHandle();
        // Paper start
        unregisterPlayer(other);
    }
    private void unregisterPlayer(EntityPlayerMP other) {
        EntityTracker tracker = ((WorldServer) entity.field_70170_p).field_73062_L;
        // Paper end

        EntityTrackerEntry entry = tracker.field_72794_c.func_76041_a(other.func_145782_y());
        if (entry != null) {
            entry.func_73123_c(getHandle());
        }

        // Remove the hidden player from this player user list, if they're on it
        if (other.sentListPacket) {
            getHandle().field_71135_a.func_147359_a(new SPacketPlayerListItem(SPacketPlayerListItem.Action.REMOVE_PLAYER, other));
        }
    }

    @Override
    @Deprecated
    public void showPlayer(Player player) {
        showPlayer0(null, player);
    }

    @Override
    public void showPlayer(Plugin plugin, Player player) {
        Validate.notNull(plugin, "Plugin cannot be null");
        // Don't require that plugin be enabled. A plugin must be allowed to call
        // showPlayer during its onDisable() method.
        showPlayer0(plugin, player);
    }

    private void showPlayer0(@Nullable Plugin plugin, Player player) {
        Validate.notNull(player, "shown player cannot be null");
        if (getHandle().field_71135_a == null) return;
        if (equals(player)) return;

        Set<WeakReference<Plugin>> hidingPlugins = hiddenPlayers.get(player.getUniqueId());
        if (hidingPlugins == null) {
            return; // Player isn't hidden
        }
        hidingPlugins.remove(getPluginWeakReference(plugin));
        if (!hidingPlugins.isEmpty()) {
            return; // Some other plugins still want the player hidden
        }
        hiddenPlayers.remove(player.getUniqueId());

        // Paper start
        EntityPlayerMP other = ((CraftPlayer) player).getHandle();
        registerPlayer(other);
    }
    private void registerPlayer(EntityPlayerMP other) {
        EntityTracker tracker = ((WorldServer) entity.field_70170_p).field_73062_L;
        // Paper end

        getHandle().field_71135_a.func_147359_a(new SPacketPlayerListItem(SPacketPlayerListItem.Action.ADD_PLAYER, other));

        EntityTrackerEntry entry = tracker.field_72794_c.func_76041_a(other.func_145782_y());
        if (entry != null && !entry.field_73134_o.contains(getHandle())) {
            entry.func_73117_b(getHandle());
        }
    }
    // Paper start
    private void reregisterPlayer(EntityPlayerMP player) {
        if (!hiddenPlayers.containsKey(player.func_110124_au())) {
            unregisterPlayer(player);
            registerPlayer(player);
        }
    }
    @Override
    public void setPlayerProfile(PlayerProfile profile) {
        EntityPlayerMP self = getHandle();
        self.setProfile(CraftPlayerProfile.asAuthlibCopy(profile));
        List<EntityPlayerMP> players = server.getServer().func_184103_al().field_72404_b;
        for (EntityPlayerMP player : players) {
            player.getBukkitEntity().reregisterPlayer(self);
        }
    }
    @Override
    public PlayerProfile getPlayerProfile() {
        return new CraftPlayerProfile(this).clone();
    }
    // Paper end

    public void removeDisconnectingPlayer(Player player) {
        hiddenPlayers.remove(player.getUniqueId());
    }

    @Override
    public boolean canSee(Player player) {
        return !hiddenPlayers.containsKey(player.getUniqueId());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        result.put("name", getName());

        return result;
    }

    @Override
    public Player getPlayer() {
        return this;
    }

    @Override
    public EntityPlayerMP getHandle() {
        return (EntityPlayerMP) entity;
    }

    public void setHandle(final EntityPlayerMP entity) {
        super.setHandle(entity);
    }

    @Override
    public String toString() {
        return "CraftPlayer{" + "name=" + getName() + '}';
    }

    @Override
    public int hashCode() {
        if (hash == 0 || hash == 485) {
            hash = 97 * 5 + (this.getUniqueId() != null ? this.getUniqueId().hashCode() : 0);
        }
        return hash;
    }

    @Override
    public long getFirstPlayed() {
        return firstPlayed;
    }

    @Override
    public long getLastPlayed() {
        return lastPlayed;
    }

    @Override
    public boolean hasPlayedBefore() {
        return hasPlayedBefore;
    }

    public void setFirstPlayed(long firstPlayed) {
        this.firstPlayed = firstPlayed;
    }

    public void readExtraData(NBTTagCompound nbttagcompound) {
        hasPlayedBefore = true;
        if (nbttagcompound.func_74764_b("bukkit")) {
            NBTTagCompound data = nbttagcompound.func_74775_l("bukkit");

            if (data.func_74764_b("firstPlayed")) {
                firstPlayed = data.func_74763_f("firstPlayed");
                lastPlayed = data.func_74763_f("lastPlayed");
            }

            if (data.func_74764_b("newExp")) {
                EntityPlayerMP handle = getHandle();
                handle.newExp = data.func_74762_e("newExp");
                handle.newTotalExp = data.func_74762_e("newTotalExp");
                handle.newLevel = data.func_74762_e("newLevel");
                handle.expToDrop = data.func_74762_e("expToDrop");
                handle.keepLevel = data.func_74767_n("keepLevel");
            }
        }
    }

    public void setExtraData(NBTTagCompound nbttagcompound) {
        if (!nbttagcompound.func_74764_b("bukkit")) {
            nbttagcompound.func_74782_a("bukkit", new NBTTagCompound());
        }

        NBTTagCompound data = nbttagcompound.func_74775_l("bukkit");
        EntityPlayerMP handle = getHandle();
        data.func_74768_a("newExp", handle.newExp);
        data.func_74768_a("newTotalExp", handle.newTotalExp);
        data.func_74768_a("newLevel", handle.newLevel);
        data.func_74768_a("expToDrop", handle.expToDrop);
        data.func_74757_a("keepLevel", handle.keepLevel);
        data.func_74772_a("firstPlayed", getFirstPlayed());
        data.func_74772_a("lastPlayed", System.currentTimeMillis());
        data.func_74778_a("lastKnownName", handle.func_70005_c_());
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return conversationTracker.beginConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        conversationTracker.abandonConversation(conversation, new ConversationAbandonedEvent(conversation, new ManuallyAbandonedConversationCanceller()));
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        conversationTracker.abandonConversation(conversation, details);
    }

    @Override
    public void acceptConversationInput(String input) {
        conversationTracker.acceptConversationInput(input);
    }

    @Override
    public boolean isConversing() {
        return conversationTracker.isConversing();
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        StandardMessenger.validatePluginMessage(server.getMessenger(), source, channel, message);
        if (getHandle().field_71135_a == null) return;

        if (channels.contains(channel)) {
            SPacketCustomPayload packet = new SPacketCustomPayload(channel, new PacketBuffer(Unpooled.wrappedBuffer(message)));
            getHandle().field_71135_a.func_147359_a(packet);
        }
    }

    @Override
    public void setTexturePack(String url) {
        setResourcePack(url);
    }

    @Override
    public void setResourcePack(String url) {
        Validate.notNull(url, "Resource pack URL cannot be null");

        getHandle().func_175397_a(url, "null");
    }

    @Override
    public void setResourcePack(String url, byte[] hash) {
        Validate.notNull(url, "Resource pack URL cannot be null");
        Validate.notNull(hash, "Resource pack hash cannot be null");
        Validate.isTrue(hash.length == 20, "Resource pack hash should be 20 bytes long but was " + hash.length);

        getHandle().func_175397_a(url, BaseEncoding.base16().lowerCase().encode(hash));
    }

    public void addChannel(String channel) {
       com.google.common.base.Preconditions.checkState( DISABLE_CHANNEL_LIMIT || channels.size() < 128, "Too many channels registered" ); // Spigot // Paper - flag to disable channel limit
        if (channels.add(channel)) {
            server.getPluginManager().callEvent(new PlayerRegisterChannelEvent(this, channel));
        }
    }

    public void removeChannel(String channel) {
        if (channels.remove(channel)) {
            server.getPluginManager().callEvent(new PlayerUnregisterChannelEvent(this, channel));
        }
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        return ImmutableSet.copyOf(channels);
    }

    public void sendSupportedChannels() {
        if (getHandle().field_71135_a == null) return;
        Set<String> listening = server.getMessenger().getIncomingChannels();

        if (!listening.isEmpty()) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            for (String channel : listening) {
                try {
                    stream.write(channel.getBytes("UTF8"));
                    stream.write((byte) 0);
                } catch (IOException ex) {
                    Logger.getLogger(CraftPlayer.class.getName()).log(Level.SEVERE, "Could not send Plugin Channel REGISTER to " + getName(), ex);
                }
            }

            getHandle().field_71135_a.func_147359_a(new SPacketCustomPayload("REGISTER", new PacketBuffer(Unpooled.wrappedBuffer(stream.toByteArray()))));
        }
    }

    @Override
    public EntityType getType() {
        return EntityType.PLAYER;
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        server.getPlayerMetadata().setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return server.getPlayerMetadata().getMetadata(this, metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return server.getPlayerMetadata().hasMetadata(this, metadataKey);
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {
        server.getPlayerMetadata().removeMetadata(this, metadataKey, owningPlugin);
    }

    @Override
    public boolean setWindowProperty(Property prop, int value) {
        Container container = getHandle().field_71070_bA;
        if (container.getBukkitView().getType() != prop.getType()) {
            return false;
        }
        // Paper start
        if (prop == Property.REPAIR_COST && container instanceof net.minecraft.inventory.ContainerRepair) {
            ((ContainerRepair) container).field_82854_e = value;
        }
        // Paper end
        getHandle().func_71112_a(container, prop.getId(), value);
        return true;
    }

    public void disconnect(String reason) {
        conversationTracker.abandonAllConversations();
        perm.clearPermissions();
    }

    @Override
    public boolean isFlying() {
        return getHandle().field_71075_bZ.field_75100_b;
    }

    @Override
    public void setFlying(boolean value) {
        boolean needsUpdate = getHandle().field_71075_bZ.field_75100_b != value; // Paper - Only refresh abilities if needed
        if (!getAllowFlight() && value) {
            throw new IllegalArgumentException("Cannot make player fly if getAllowFlight() is false");
        }

        getHandle().field_71075_bZ.field_75100_b = value;
        if (needsUpdate) getHandle().func_71016_p(); // Paper - Only refresh abilities if needed
    }

    @Override
    public boolean getAllowFlight() {
        return getHandle().field_71075_bZ.field_75101_c;
    }

    @Override
    public void setAllowFlight(boolean value) {
        if (isFlying() && !value) {
            getHandle().field_71075_bZ.field_75100_b = false;
        }

        getHandle().field_71075_bZ.field_75101_c = value;
        getHandle().func_71016_p();
    }

    @Override
    public int getNoDamageTicks() {
        if (getHandle().field_147101_bU > 0) {
            return Math.max(getHandle().field_147101_bU, getHandle().field_70172_ad);
        } else {
            return getHandle().field_70172_ad;
        }
    }

    @Override
    public void setFlySpeed(float value) {
        validateSpeed(value);
        EntityPlayerMP player = getHandle();
        player.field_71075_bZ.field_75096_f = value / 2f;
        player.func_71016_p();

    }

    @Override
    public void setWalkSpeed(float value) {
        validateSpeed(value);
        EntityPlayerMP player = getHandle();
        player.field_71075_bZ.field_75097_g = value / 2f;
        player.func_71016_p();
    }

    @Override
    public float getFlySpeed() {
        return getHandle().field_71075_bZ.field_75096_f * 2f;
    }

    @Override
    public float getWalkSpeed() {
        return getHandle().field_71075_bZ.field_75097_g * 2f;
    }

    private void validateSpeed(float value) {
        if (value < 0) {
            if (value < -1f) {
                throw new IllegalArgumentException(value + " is too low");
            }
        } else {
            if (value > 1f) {
                throw new IllegalArgumentException(value + " is too high");
            }
        }
    }

    @Override
    public void setMaxHealth(double amount) {
        super.setMaxHealth(amount);
        this.health = Math.min(this.health, health);
        getHandle().func_71118_n();
    }

    @Override
    public void resetMaxHealth() {
        super.resetMaxHealth();
        getHandle().func_71118_n();
    }

    @Override
    public CraftScoreboard getScoreboard() {
        return this.server.getScoreboardManager().getPlayerBoard(this);
    }

    @Override
    public void setScoreboard(Scoreboard scoreboard) {
        Validate.notNull(scoreboard, "Scoreboard cannot be null");
        NetHandlerPlayServer playerConnection = getHandle().field_71135_a;
        if (playerConnection == null) {
            throw new IllegalStateException("Cannot set scoreboard yet");
        }
        if (playerConnection.isDisconnected()) {
            // throw new IllegalStateException("Cannot set scoreboard for invalid CraftPlayer"); // Spigot - remove this as Mojang's semi asynchronous Netty implementation can lead to races
        }

        this.server.getScoreboardManager().setPlayerBoard(this, scoreboard);
    }

    @Override
    public void setHealthScale(double value) {
        Validate.isTrue((float) value > 0F, "Must be greater than 0");
        healthScale = value;
        scaledHealth = true;
        updateScaledHealth();
    }

    @Override
    public double getHealthScale() {
        return healthScale;
    }

    @Override
    public void setHealthScaled(boolean scale) {
        if (scaledHealth != (scaledHealth = scale)) {
            updateScaledHealth();
        }
    }

    @Override
    public boolean isHealthScaled() {
        return scaledHealth;
    }

    public float getScaledHealth() {
        return (float) (isHealthScaled() ? getHealth() * getHealthScale() / getMaxHealth() : getHealth());
    }

    @Override
    public double getHealth() {
        return health;
    }

    public void setRealHealth(double health) {
        if (Double.isNaN(health)) {return;} // Paper
        this.health = health;
    }

    public void updateScaledHealth() {
        AttributeMap attributemapserver = (AttributeMap) getHandle().func_110140_aT();
        Collection<IAttributeInstance> set = attributemapserver.func_111160_c(); // PAIL: Rename

        injectScaledMaxHealth(set, true);

        // SPIGOT-3813: Attributes before health
        if (getHandle().field_71135_a != null) {
            getHandle().field_71135_a.func_147359_a(new SPacketEntityProperties(getHandle().func_145782_y(), set));
            sendHealthUpdate();
        }
        getHandle().func_184212_Q().func_187227_b(EntityLivingBase.field_184632_c, getScaledHealth());

        getHandle().maxHealthCache = getMaxHealth();
    }

    public void sendHealthUpdate() {
        getHandle().field_71135_a.func_147359_a(new SPacketUpdateHealth(getScaledHealth(), getHandle().func_71024_bL().func_75116_a(), getHandle().func_71024_bL().func_75115_e()));
    }

    public void injectScaledMaxHealth(Collection<IAttributeInstance> collection, boolean force) {
        if (!scaledHealth && !force) {
            return;
        }
        for (IAttributeInstance genericInstance : collection) {
            if (genericInstance.func_111123_a().func_111108_a().equals("generic.maxHealth")) {
                collection.remove(genericInstance);
                break;
            }
        }
        // Spigot start
        double healthMod = scaledHealth ? healthScale : getMaxHealth();
        if ( healthMod >= Float.MAX_VALUE || healthMod <= 0 )
        {
            healthMod = 20; // Reset health
            getServer().getLogger().warning( getName() + " tried to crash the server with a large health attribute" );
        }
        collection.add(new ModifiableAttributeInstance(getHandle().func_110140_aT(), (new RangedAttribute(null, "generic.maxHealth", healthMod, 0.0D, Float.MAX_VALUE)).func_111117_a("Max Health").func_111112_a(true)));
        // Spigot end
    }

    @Override
    public org.bukkit.entity.Entity getSpectatorTarget() {
        Entity followed = getHandle().func_175398_C();
        return followed == getHandle() ? null : followed.getBukkitEntity();
    }

    @Override
    public void setSpectatorTarget(org.bukkit.entity.Entity entity) {
        Preconditions.checkArgument(getGameMode() == GameMode.SPECTATOR, "Player must be in spectator mode");
        getHandle().func_175399_e((entity == null) ? null : ((CraftEntity) entity).getHandle());
    }

    @Override
    public void sendTitle(String title, String subtitle) {
        sendTitle(title, subtitle, 10, 70, 20);
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        SPacketTitle times = new SPacketTitle(fadeIn, stay, fadeOut);
        getHandle().field_71135_a.func_147359_a(times);

        if (title != null) {
            SPacketTitle packetTitle = new SPacketTitle(Type.TITLE, CraftChatMessage.fromString(title)[0]);
            getHandle().field_71135_a.func_147359_a(packetTitle);
        }

        if (subtitle != null) {
            SPacketTitle packetSubtitle = new SPacketTitle(Type.SUBTITLE, CraftChatMessage.fromString(subtitle)[0]);
            getHandle().field_71135_a.func_147359_a(packetSubtitle);
        }
    }

    @Override
    public void resetTitle() {
        SPacketTitle packetReset = new SPacketTitle(Type.RESET, null);
        getHandle().field_71135_a.func_147359_a(packetReset);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count) {
        spawnParticle(particle, x, y, z, count, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
        spawnParticle(particle, x, y, z, count, 0, 0, 0, data);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, T data) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, 1, data);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        if (data != null && !particle.getDataType().isInstance(data)) {
            throw new IllegalArgumentException("data should be " + particle.getDataType() + " got " + data.getClass());
        }
        SPacketParticles packetplayoutworldparticles = new SPacketParticles(CraftParticle.toNMS(particle), true, (float) x, (float) y, (float) z, (float) offsetX, (float) offsetY, (float) offsetZ, (float) extra, count, CraftParticle.toData(particle, data));
        getHandle().field_71135_a.func_147359_a(packetplayoutworldparticles);

    }

    @Override
    public org.bukkit.advancement.AdvancementProgress getAdvancementProgress(org.bukkit.advancement.Advancement advancement) {
        Preconditions.checkArgument(advancement != null, "advancement");

        CraftAdvancement craft = (CraftAdvancement) advancement;
        PlayerAdvancements data = getHandle().func_192039_O();
        AdvancementProgress progress = data.func_192747_a(craft.getHandle());

        return new CraftAdvancementProgress(craft, data, progress);
    }

    @Override
    public String getLocale() {
        // Paper start - Locale change event
        final String locale = getHandle().field_71148_cg;
        return locale != null ? locale : "en_us";
        // Paper end
    }

    @Override
    public void setAffectsSpawning(boolean affects) {
        this.getHandle().affectsSpawning = affects;
    }

    @Override
    public boolean getAffectsSpawning() {
        return this.getHandle().affectsSpawning;
    }

    @Override
    public int getViewDistance() {
        return getHandle().getViewDistance();
    }

    @Override
    public void setViewDistance(int viewDistance) {
        ((WorldServer) getHandle().field_70170_p).func_184164_w().updateViewDistance(getHandle(), viewDistance);
    }

    @Override
    public void setResourcePack(String url, String hash) {
        Validate.notNull(url, "Resource pack URL cannot be null");
        Validate.notNull(hash, "Hash cannot be null");
        this.getHandle().func_175397_a(url, hash);
    }

    @Override
    public org.bukkit.event.player.PlayerResourcePackStatusEvent.Status getResourcePackStatus() {
        return this.resourcePackStatus;
    }

    @Override
    public String getResourcePackHash() {
        return this.resourcePackHash;
    }

    @Override
    public boolean hasResourcePack() {
        return this.resourcePackStatus == org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
    }

    public void setResourcePackStatus(org.bukkit.event.player.PlayerResourcePackStatusEvent.Status status) {
        this.resourcePackStatus = status;
    }

    // Spigot start
    private final Player.Spigot spigot = new Player.Spigot()
    {

        @Override
        public InetSocketAddress getRawAddress()
        {
            return (InetSocketAddress) getHandle().field_71135_a.field_147371_a.getRawAddress();
        }

        @Override
        public boolean getCollidesWithEntities() {
            return CraftPlayer.this.isCollidable();
        }

        @Override
        public void setCollidesWithEntities(boolean collides) {
            CraftPlayer.this.setCollidable(collides);
        }

        @Override
        public void respawn()
        {
            if ( getHealth() <= 0 && isOnline() )
            {
                server.getServer().func_184103_al().func_72368_a( getHandle(), 0, false );
            }
        }

        @Override
        public void playEffect( Location location, Effect effect, int id, int data, float offsetX, float offsetY, float offsetZ, float speed, int particleCount, int radius )
        {
            Validate.notNull( location, "Location cannot be null" );
            Validate.notNull( effect, "Effect cannot be null" );
            Validate.notNull( location.getWorld(), "World cannot be null" );
            Packet packet;
            if ( effect.getType() != Effect.Type.PARTICLE )
            {
                int packetData = effect.getId();
                packet = new SPacketEffect( packetData, new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ() ), id, false );
            } else
            {
                net.minecraft.util.EnumParticleTypes particle = null;
                int[] extra = null;
                for ( net.minecraft.util.EnumParticleTypes p : net.minecraft.util.EnumParticleTypes.values() )
                {
                    if ( effect.getName().startsWith( p.func_179346_b().replace("_", "") ) )
                    {
                        particle = p;
                        if ( effect.getData() != null ) 
                        {
                            if ( effect.getData().equals( org.bukkit.Material.class ) )
                            {
                                extra = new int[]{ id };
                            } else 
                            {
                                extra = new int[]{ (data << 12) | (id & 0xFFF) };
                            }
                        }
                        break;
                    }
                }
                if ( extra == null )
                {
                    extra = new int[0];
                }
                packet = new SPacketParticles( particle, true, (float) location.getX(), (float) location.getY(), (float) location.getZ(), offsetX, offsetY, offsetZ, speed, particleCount, extra );
            }
            int distance;
            radius *= radius;
            if ( getHandle().field_71135_a == null )
            {
                return;
            }
            if ( !location.getWorld().equals( getWorld() ) )
            {
                return;
            }

            distance = (int) getLocation().distanceSquared( location );
            if ( distance <= radius )
            {
                getHandle().field_71135_a.func_147359_a( packet );
            }
        }

        @Override
        public String getLocale()
        {
            return CraftPlayer.this.getLocale(); // Paper
        }

        @Override
        public Set<Player> getHiddenPlayers()
        {
            Set<Player> ret = new HashSet<Player>();
            for ( UUID u : hiddenPlayers.keySet() )
            {
                ret.add( getServer().getPlayer( u ) );
            }

            return java.util.Collections.unmodifiableSet( ret );
        }

        @Override
        public void sendMessage(BaseComponent component) {
          sendMessage( new BaseComponent[] { component } );
        }

        @Override
        public void sendMessage(BaseComponent... components) {
           if ( getHandle().field_71135_a == null ) return;

            SPacketChat packet = new SPacketChat(null, ChatType.CHAT);
            packet.components = components;
            getHandle().field_71135_a.func_147359_a(packet);
        }

        @Override
        public void sendMessage(net.md_5.bungee.api.ChatMessageType position, BaseComponent component) {
            sendMessage( position, new BaseComponent[] { component } );
        }

        @Override
        public void sendMessage(net.md_5.bungee.api.ChatMessageType position, BaseComponent... components) {
            if ( getHandle().field_71135_a == null ) return;

            SPacketChat packet = new SPacketChat(null, ChatType.func_192582_a((byte) position.ordinal()));
            // Action bar doesn't render colours, replace colours with legacy section symbols
            if (position == net.md_5.bungee.api.ChatMessageType.ACTION_BAR) {
                components = new BaseComponent[]{new net.md_5.bungee.api.chat.TextComponent(BaseComponent.toLegacyText(components))};
            }
            packet.components = components;
            getHandle().field_71135_a.func_147359_a(packet);
        }

        @Override
        public int getPing()
        {
            return getHandle().field_71138_i;
        }
    };

    @Override
    public Player.Spigot spigot()
    {
        return spigot;
    }
    // Spigot end
}
