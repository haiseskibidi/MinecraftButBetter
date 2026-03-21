package com.za.minecraft.world.items;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.items.component.FoodComponent;

public class FoodItem extends Item {

    public FoodItem(int id, String name, String texturePath, float nutrition, float saturationBonus) {
        super(id, name, texturePath);
        addComponent(FoodComponent.class, new FoodComponent(nutrition, saturationBonus));
    }

    public FoodItem(int id, Identifier identifier, String translationKey, String texturePath, float nutrition, float saturationBonus) {
        super(id, identifier, translationKey, texturePath);
        addComponent(FoodComponent.class, new FoodComponent(nutrition, saturationBonus));
    }

    public float getNutrition() {
        FoodComponent c = getComponent(FoodComponent.class);
        return c != null ? c.nutrition() : 0;
    }

    public float getSaturationBonus() {
        FoodComponent c = getComponent(FoodComponent.class);
        return c != null ? c.saturationBonus() : 0;
    }
}
