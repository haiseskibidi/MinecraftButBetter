package com.za.zenith.world.items.component;

/**
 * Marker component for items that provide magnetic attraction for entities.
 */
public class MagneticComponent implements ItemComponent {
    public final float attractionRadius;
    public final float pickupRadius;
    public final float attractionForce;

    public MagneticComponent(float attractionRadius, float pickupRadius, float attractionForce) {
        this.attractionRadius = attractionRadius;
        this.pickupRadius = pickupRadius;
        this.attractionForce = attractionForce;
    }
}
