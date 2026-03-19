package com.za.minecraft.world.items;

public class FoodItem extends Item {
    private final float nutrition;
    private final float saturationBonus;

    public FoodItem(byte id, String name, String texturePath, float nutrition, float saturationBonus) {
        super(id, name, texturePath);
        this.nutrition = nutrition;
        this.saturationBonus = saturationBonus;
    }

    public float getNutrition() {
        return nutrition;
    }

    public float getSaturationBonus() {
        return saturationBonus;
    }

    @Override
    public boolean isFood() {
        return true;
    }
}
