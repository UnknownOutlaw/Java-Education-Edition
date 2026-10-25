package edu.chemistry.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.chemistry.EduPermissions;
import edu.chemistry.registry.EduItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Education Edition's NPC: an immobile character with a name, a skin, a line of dialog and buttons that run commands
 * or open web links. Anyone can talk to it; only World Builders can place, edit or remove it.
 */
public class NpcEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_SKIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_DIALOG = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ACTIONS = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    /** where the head is looking (the body's direction is the entity's own rotation) */
    private static final EntityDataAccessor<Float> DATA_HEAD_YAW = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEAD_PITCH = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.FLOAT);

    /** Bedrock's skin packs, in the order the skin picker shows them */
    public static final String[][] SKIN_PACKS = {
        {"npc", "10"}, {"scientist", "10"}, {"teacher", "5"}, {"agriculture", "10"}, {"apiary", "5"}, {"businessmobs", "5"},
        {"construction", "5"}, {"everydaybusiness", "5"}, {"kiosk", "5"}};
    public static final String DEFAULT_SKIN = "npc_1";

    /** set by the client: opens the dialog (or the editor, for World Builders) */
    public static Consumer<NpcEntity> openScreen = npc -> {
    };


    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
    }

    public static List<String> skinsInPack(int pack) {
        List<String> out = new ArrayList<>();
        String[] p = SKIN_PACKS[pack];
        int count = Integer.parseInt(p[1]);
        for (int i = 1; i <= count; i++) out.add(p[0].equals("npc") ? "npc_" + i : "npc_" + p[0] + "_" + i);
        return out;
    }

    public static boolean isSkin(String skin) {
        for (int i = 0; i < SKIN_PACKS.length; i++) if (skinsInPack(i).contains(skin)) return true;
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SKIN, DEFAULT_SKIN);
        builder.define(DATA_DIALOG, "");
        builder.define(DATA_ACTIONS, "[]");
        builder.define(DATA_HEAD_YAW, Float.NaN);
        builder.define(DATA_HEAD_PITCH, 0.0F);
    }

    public String getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkin(String skin) {
        this.entityData.set(DATA_SKIN, isSkin(skin) ? skin : DEFAULT_SKIN);
    }

    public String getDialog() {
        return this.entityData.get(DATA_DIALOG);
    }

    public void setDialog(String dialog) {
        this.entityData.set(DATA_DIALOG, dialog);
    }

    public List<NpcAction> getActions() {
        return NpcAction.listFromJson(this.entityData.get(DATA_ACTIONS));
    }

    public void setActions(List<NpcAction> actions) {
        this.entityData.set(DATA_ACTIONS, NpcAction.listToJson(actions).toString());
    }

    public String getNpcName() {
        Component name = this.getCustomName();
        return name == null ? "NPC" : name.getString();
    }

    /** smooth movement on the client between position updates (the level calls it each tick) */
    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return net.minecraft.world.entity.LinearInterpolationHandler.create(this, 3);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.smoothHead();
            return;
        }
        if (this.isRemoved()) return;
        this.lookAround();
        // NPCs don't walk, but they do fall
        Vec3 v = this.getDeltaMovement();
        if (!this.onGround()) v = v.add(0.0, -0.08, 0.0);
        this.setDeltaMovement(v);
        this.move(MoverType.SELF, v);
        edu.chemistry.util.BlockTriggers.touch(this);
        this.setDeltaMovement(this.onGround() ? Vec3.ZERO : this.getDeltaMovement().multiply(0.0, 0.98, 0.0));
    }

    // ---------------------------------------------------------------- looking around
    // It watches the nearest player within 6 blocks (and keeps watching them out to 8). With nobody close it stays
    // facing wherever it last looked. The head turns first and the body slowly follows, like a villager.
    private static final float HEAD_SPEED = 10.0F;
    private static final float BODY_SPEED = 3.0F;
    private static final float NECK = 75.0F;
    private static final double LOOK_START = 6.0;
    private static final double LOOK_KEEP = 8.0;
    private @Nullable Player lookAt;
    private float lookYaw = Float.NaN;
    private float lookPitch = Float.NaN;

    private void lookAround() {
        float head = this.entityData.get(DATA_HEAD_YAW);
        if (Float.isNaN(head)) head = this.getYRot();
        if (Float.isNaN(this.lookYaw)) this.lookYaw = head;
        if (Float.isNaN(this.lookPitch)) this.lookPitch = this.entityData.get(DATA_HEAD_PITCH);
        if (this.lookAt != null && (this.lookAt.isRemoved() || this.lookAt.isSpectator()
            || this.lookAt.distanceToSqr(this) > LOOK_KEEP * LOOK_KEEP)) {
            this.lookAt = null;
        }
        if (this.lookAt == null || this.tickCount % 20 == 0) {
            Player near = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), LOOK_START, p -> !p.isSpectator());
            if (near != null) this.lookAt = near;
        }
        if (this.lookAt != null) {
            Vec3 d = this.lookAt.getEyePosition().subtract(this.getEyePosition());
            this.lookYaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.lookPitch = Mth.clamp((float) -(Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG), -40.0F, 40.0F);
        }
        head = Mth.approachDegrees(head, this.lookYaw, HEAD_SPEED);
        float body = Mth.approachDegrees(this.getYRot(), head, BODY_SPEED);
        float off = Mth.wrapDegrees(head - body);
        if (off > NECK) body = head - NECK;
        else if (off < -NECK) body = head + NECK;
        this.setYRot(Mth.wrapDegrees(body));
        this.entityData.set(DATA_HEAD_YAW, Mth.wrapDegrees(head));
        float pitch = this.entityData.get(DATA_HEAD_PITCH);
        this.entityData.set(DATA_HEAD_PITCH, pitch + Mth.clamp(this.lookPitch - pitch, -HEAD_SPEED, HEAD_SPEED));
    }

    /** this client's smoothed copy of where the head is looking */
    public float headYaw = Float.NaN;
    public float headYawO = Float.NaN;
    public float headPitch;
    public float headPitchO;

    private void smoothHead() {
        float target = this.entityData.get(DATA_HEAD_YAW);
        if (Float.isNaN(target)) target = this.getYRot();
        if (Float.isNaN(this.headYaw)) this.headYaw = target;
        this.headYawO = this.headYaw;
        this.headPitchO = this.headPitch;
        this.headYaw = Mth.approachDegrees(this.headYaw, target, HEAD_SPEED * 1.5F);
        float p = this.entityData.get(DATA_HEAD_PITCH);
        this.headPitch += Mth.clamp(p - this.headPitch, -HEAD_SPEED * 1.5F, HEAD_SPEED * 1.5F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // only World Builders can remove an NPC, by hitting it
        if (source.getEntity() instanceof Player player && EduPermissions.isWorldBuilder(player)) {
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        // anyone can talk to an NPC and use its buttons; only World Builders get the Edit button
        if (this.level().isClientSide() && edu.chemistry.EduRules.clientEnabled) openScreen.accept(this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(EduItems.NPC_SPAWN_EGG);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.08;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setSkin(input.getStringOr("Skin", DEFAULT_SKIN));
        this.setDialog(input.getStringOr("Dialog", ""));
        this.entityData.set(DATA_ACTIONS, input.getStringOr("Actions", "[]"));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("Skin", this.getSkin());
        output.putString("Dialog", this.getDialog());
        output.putString("Actions", this.entityData.get(DATA_ACTIONS));
    }

    /** A button (or an on-open / on-close trigger) on an NPC. */
    public record NpcAction(boolean url, String name, String mode, String text) {
        public static final String BUTTON = "button";
        public static final String ON_ENTER = "on_enter";
        public static final String ON_EXIT = "on_exit";

        public JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("type", this.url ? "url" : "command");
            o.addProperty("name", this.name);
            o.addProperty("mode", this.mode);
            o.addProperty("text", this.text);
            return o;
        }

        public static NpcAction fromJson(JsonObject o) {
            boolean url = "url".equals(str(o, "type"));
            String mode = str(o, "mode");
            if (url || !(mode.equals(ON_ENTER) || mode.equals(ON_EXIT))) mode = BUTTON;
            return new NpcAction(url, str(o, "name"), mode, str(o, "text"));
        }

        private static String str(JsonObject o, String key) {
            JsonElement e = o.get(key);
            return e == null || !e.isJsonPrimitive() ? "" : e.getAsString();
        }

        public static JsonArray listToJson(List<NpcAction> actions) {
            JsonArray a = new JsonArray();
            for (NpcAction action : actions) a.add(action.toJson());
            return a;
        }

        public static List<NpcAction> listFromJson(String json) {
            List<NpcAction> out = new ArrayList<>();
            try {
                for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
                    if (e.isJsonObject()) out.add(fromJson(e.getAsJsonObject()));
                }
            } catch (RuntimeException ignored) {
                // bad data: no actions
            }
            return out;
        }
    }
}
