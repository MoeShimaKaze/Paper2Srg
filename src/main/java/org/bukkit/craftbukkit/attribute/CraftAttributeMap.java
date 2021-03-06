package org.bukkit.craftbukkit.attribute;

import com.google.common.base.Preconditions;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;



public class CraftAttributeMap implements Attributable {

    private final AbstractAttributeMap handle;

    public CraftAttributeMap(AbstractAttributeMap handle) {
        this.handle = handle;
    }

    @Override
    public AttributeInstance getAttribute(Attribute attribute) {
        Preconditions.checkArgument(attribute != null, "attribute");
        net.minecraft.entity.ai.attributes.IAttributeInstance nms = handle.func_111152_a(toMinecraft(attribute.name()));

        return (nms == null) ? null : new CraftAttributeInstance(nms, attribute);
    }

    static String toMinecraft(String bukkit) {
        int first = bukkit.indexOf('_');
        int second = bukkit.indexOf('_', first + 1);

        StringBuilder sb = new StringBuilder(bukkit.toLowerCase(java.util.Locale.ENGLISH));

        sb.setCharAt(first, '.');
        if (second != -1) {
            sb.deleteCharAt(second);
            sb.setCharAt(second, bukkit.charAt(second + 1));
        }

        return sb.toString();
    }
}
