package net.minecraft.util;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.Explosion;

public class DamageSource {

    public static final DamageSource field_76372_a = (new DamageSource("inFire")).func_76361_j();
    public static final DamageSource field_180137_b = new DamageSource("lightningBolt");
    public static final DamageSource field_76370_b = (new DamageSource("onFire")).func_76348_h().func_76361_j();
    public static final DamageSource field_76371_c = (new DamageSource("lava")).func_76361_j();
    public static final DamageSource field_190095_e = (new DamageSource("hotFloor")).func_76361_j();
    public static final DamageSource field_76368_d = (new DamageSource("inWall")).func_76348_h();
    public static final DamageSource field_191291_g = (new DamageSource("cramming")).func_76348_h();
    public static final DamageSource field_76369_e = (new DamageSource("drown")).func_76348_h();
    public static final DamageSource field_76366_f = (new DamageSource("starve")).func_76348_h().func_151518_m();
    public static final DamageSource field_76367_g = new DamageSource("cactus");
    public static final DamageSource field_76379_h = (new DamageSource("fall")).func_76348_h();
    public static final DamageSource field_188406_j = (new DamageSource("flyIntoWall")).func_76348_h();
    public static final DamageSource field_76380_i = (new DamageSource("outOfWorld")).func_76348_h().func_76359_i();
    public static final DamageSource field_76377_j = (new DamageSource("generic")).func_76348_h();
    public static final DamageSource field_76376_m = (new DamageSource("magic")).func_76348_h().func_82726_p();
    public static final DamageSource field_82727_n = (new DamageSource("wither")).func_76348_h();
    public static final DamageSource field_82728_o = new DamageSource("anvil");
    public static final DamageSource field_82729_p = new DamageSource("fallingBlock");
    public static final DamageSource field_188407_q = (new DamageSource("dragonBreath")).func_76348_h();
    public static final DamageSource field_191552_t = (new DamageSource("fireworks")).func_94540_d();
    private boolean field_76374_o;
    private boolean field_76385_p;
    private boolean field_151520_r;
    private float field_76384_q = 0.1F;
    private boolean field_76383_r;
    private boolean field_76382_s;
    private boolean field_76381_t;
    private boolean field_82730_x;
    private boolean field_76378_k;
    public String field_76373_n;
    // CraftBukkit start
    private boolean sweep;

    public boolean isSweep() {
        return sweep;
    }

    public DamageSource sweep() {
        this.sweep = true;
        return this;
    }
    // CraftBukkit end

    public static DamageSource func_76358_a(EntityLivingBase entityliving) {
        return new EntityDamageSource("mob", entityliving);
    }

    public static DamageSource func_188403_a(Entity entity, EntityLivingBase entityliving) {
        return new EntityDamageSourceIndirect("mob", entity, entityliving);
    }

    public static DamageSource func_76365_a(EntityPlayer entityhuman) {
        return new EntityDamageSource("player", entityhuman);
    }

    public static DamageSource func_76353_a(EntityArrow entityarrow, @Nullable Entity entity) {
        return (new EntityDamageSourceIndirect("arrow", entityarrow, entity)).func_76349_b();
    }

    public static DamageSource func_76362_a(EntityFireball entityfireball, @Nullable Entity entity) {
        return entity == null ? (new EntityDamageSourceIndirect("onFire", entityfireball, entityfireball)).func_76361_j().func_76349_b() : (new EntityDamageSourceIndirect("fireball", entityfireball, entity)).func_76361_j().func_76349_b();
    }

    public static DamageSource func_76356_a(Entity entity, @Nullable Entity entity1) {
        return (new EntityDamageSourceIndirect("thrown", entity, entity1)).func_76349_b();
    }

    public static DamageSource func_76354_b(Entity entity, @Nullable Entity entity1) {
        return (new EntityDamageSourceIndirect("indirectMagic", entity, entity1)).func_76348_h().func_82726_p();
    }

    public static DamageSource func_92087_a(Entity entity) {
        return (new EntityDamageSource("thorns", entity)).func_180138_v().func_82726_p();
    }

    public static DamageSource func_94539_a(@Nullable Explosion explosion) {
        return explosion != null && explosion.func_94613_c() != null ? (new EntityDamageSource("explosion.player", explosion.func_94613_c())).func_76351_m().func_94540_d() : (new DamageSource("explosion")).func_76351_m().func_94540_d();
    }

    public static DamageSource func_188405_b(@Nullable EntityLivingBase entityliving) {
        return entityliving != null ? (new EntityDamageSource("explosion.player", entityliving)).func_76351_m().func_94540_d() : (new DamageSource("explosion")).func_76351_m().func_94540_d();
    }

    public boolean func_76352_a() {
        return this.field_76382_s;
    }

    public DamageSource func_76349_b() {
        this.field_76382_s = true;
        return this;
    }

    public boolean func_94541_c() {
        return this.field_76378_k;
    }

    public DamageSource func_94540_d() {
        this.field_76378_k = true;
        return this;
    }

    public boolean func_76363_c() {
        return this.field_76374_o;
    }

    public float func_76345_d() {
        return this.field_76384_q;
    }

    public boolean func_76357_e() {
        return this.field_76385_p;
    }

    public boolean func_151517_h() {
        return this.field_151520_r;
    }

    protected DamageSource(String s) {
        this.field_76373_n = s;
    }

    @Nullable
    public Entity func_76364_f() {
        return this.func_76346_g();
    }

    @Nullable
    public Entity func_76346_g() {
        return null;
    }

    protected DamageSource func_76348_h() {
        this.field_76374_o = true;
        this.field_76384_q = 0.0F;
        return this;
    }

    protected DamageSource func_76359_i() {
        this.field_76385_p = true;
        return this;
    }

    protected DamageSource func_151518_m() {
        this.field_151520_r = true;
        this.field_76384_q = 0.0F;
        return this;
    }

    protected DamageSource func_76361_j() {
        this.field_76383_r = true;
        return this;
    }

    public ITextComponent func_151519_b(EntityLivingBase entityliving) {
        EntityLivingBase entityliving1 = entityliving.func_94060_bK();
        String s = "death.attack." + this.field_76373_n;
        String s1 = s + ".player";

        return entityliving1 != null && I18n.func_94522_b(s1) ? new TextComponentTranslation(s1, new Object[] { entityliving.func_145748_c_(), entityliving1.func_145748_c_()}) : new TextComponentTranslation(s, new Object[] { entityliving.func_145748_c_()});
    }

    public boolean func_76347_k() {
        return this.field_76383_r;
    }

    public String func_76355_l() {
        return this.field_76373_n;
    }

    public DamageSource func_76351_m() {
        this.field_76381_t = true;
        return this;
    }

    public boolean func_76350_n() {
        return this.field_76381_t;
    }

    public boolean func_82725_o() {
        return this.field_82730_x;
    }

    public DamageSource func_82726_p() {
        this.field_82730_x = true;
        return this;
    }

    public boolean func_180136_u() {
        Entity entity = this.func_76346_g();

        return entity instanceof EntityPlayer && ((EntityPlayer) entity).field_71075_bZ.field_75098_d;
    }

    @Nullable
    public Vec3d func_188404_v() {
        return null;
    }
}
