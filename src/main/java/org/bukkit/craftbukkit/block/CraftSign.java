package org.bukkit.craftbukkit.block;

import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.tileentity.TileEntitySign;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.util.CraftChatMessage;

public class CraftSign extends CraftBlockEntityState<TileEntitySign> implements Sign {

    private String[] lines;

    public CraftSign(final Block block) {
        super(block, TileEntitySign.class);
    }

    public CraftSign(final Material material, final TileEntitySign te) {
        super(material, te);
    }

    @Override
    public void load(TileEntitySign sign) {
        super.load(sign);

        lines = new String[sign.field_145915_a.length];
        System.arraycopy(revertComponents(sign.field_145915_a), 0, lines, 0, lines.length);
    }

    @Override
    public String[] getLines() {
        return lines;
    }

    @Override
    public String getLine(int index) throws IndexOutOfBoundsException {
        return lines[index];
    }

    @Override
    public void setLine(int index, String line) throws IndexOutOfBoundsException {
        lines[index] = line;
    }

    @Override
    public void applyTo(TileEntitySign sign) {
        super.applyTo(sign);

        ITextComponent[] newLines = sanitizeLines(lines);
        System.arraycopy(newLines, 0, sign.field_145915_a, 0, 4);
    }

    public static ITextComponent[] sanitizeLines(String[] lines) {
        ITextComponent[] components = new ITextComponent[4];

        for (int i = 0; i < 4; i++) {
            if (i < lines.length && lines[i] != null) {
                components[i] = CraftChatMessage.fromString(lines[i])[0];
            } else {
                components[i] = new TextComponentString("");
            }
        }

        return components;
    }

    public static String[] revertComponents(ITextComponent[] components) {
        String[] lines = new String[components.length];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = revertComponent(components[i]);
        }
        return lines;
    }

    private static String revertComponent(ITextComponent component) {
        return CraftChatMessage.fromComponent(component);
    }
}
