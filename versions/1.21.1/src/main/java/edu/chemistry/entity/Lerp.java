package edu.chemistry.entity;

import net.minecraft.world.entity.Entity;

/**
 * Smooth client-side movement for the mod's plain entities (balloons, NPCs, tripod cameras): when the server moves one,
 * it slides there over a few ticks instead of jumping. (Later Minecraft versions have this built in.)
 */
public final class Lerp {
    private final Entity entity;
    private final int defaultSteps;
    private int steps;
    private double x;
    private double y;
    private double z;
    private float yRot;
    private float xRot;

    public Lerp(Entity entity, int steps) {
        this.entity = entity;
        this.defaultSteps = steps;
    }

    /** where the server says the entity is now (call from Entity.lerpTo) */
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
        this.steps = this.defaultSteps;
    }

    public boolean active() {
        return this.steps > 0;
    }

    public double targetX() {
        return this.steps > 0 ? this.x : this.entity.getX();
    }

    public double targetY() {
        return this.steps > 0 ? this.y : this.entity.getY();
    }

    public double targetZ() {
        return this.steps > 0 ? this.z : this.entity.getZ();
    }

    /** one tick of the slide (call each client tick) */
    public void interpolate() {
        if (this.steps <= 0) return;
        double d = 1.0 / this.steps;
        Entity e = this.entity;
        e.setPos(e.getX() + (this.x - e.getX()) * d, e.getY() + (this.y - e.getY()) * d, e.getZ() + (this.z - e.getZ()) * d);
        e.setYRot(e.getYRot() + net.minecraft.util.Mth.wrapDegrees(this.yRot - e.getYRot()) * (float) d);
        e.setXRot(e.getXRot() + (this.xRot - e.getXRot()) * (float) d);
        this.steps--;
    }
}
