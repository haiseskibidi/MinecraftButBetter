package com.za.zenith.engine.graphics.model;

import com.za.zenith.utils.Identifier;

/**
 * Определяет целевые углы поворота пальцев для конкретного хвата.
 * 
 * --- ЕДИНЫЙ СТАНДАРТ ОСЕЙ ZENITH (В ГРАДУСАХ) ---
 * Формат: [X, Y, Z]
 * 
 * [X] Pitch (Тангаж) : Наклон ВВЕРХ (+) / ВНИЗ (-). 
 *                     Например: чтобы опустить большой палец к ладони, используем отрицательный X.
 * [Y] Yaw (Рысканье): Поворот ВЛЕВО (+) / ВПРАВО (-). 
 *                     Например: чтобы согнуть 4 пальца правой руки в кулак, поворачиваем их ВЛЕВО (положительный Y).
 * [Z] Roll (Крен)   : Вращение вокруг своей оси (обычно 0).
 * 
 * ПРАВИЛО КУЛАКА (Правая рука):
 * - Указательный и остальные: Сгибаются ВЛЕВО (+Y).
 * - Большой палец: Сгибается ВЛЕВО (+Y, чтобы лечь на пальцы) и слегка ВНИЗ (-X).
 */
public record GripDefinition(
    float[] thumb,
    float[] thumb_tip,
    float[] index,
    float[] index_tip,
    float[] fingers,
    float[] fingers_tip
) {
    public GripDefinition() {
        this(
            new float[]{0, 0, 0}, null,
            new float[]{0, 0, 0}, null,
            new float[]{0, 0, 0}, null
        );
    }

    public float[] getThumbTip() { return thumb_tip != null ? thumb_tip : thumb; }
    public float[] getIndexTip() { return index_tip != null ? index_tip : index; }
    public float[] getFingersTip() { return fingers_tip != null ? fingers_tip : fingers; }

    public static GripDefinition createRelaxed() {
        // Легкий изгиб пальцев внутрь (по оси Y) в расслабленном состоянии
        // Положительный Y = поворот влево (к центру ладони для правой руки).
        // Отрицательный X = наклон вниз.
        return new GripDefinition(
            new float[]{-10, 0, 0}, new float[]{-10, 0, 0}, 
            new float[]{0, 15, 0}, new float[]{0, 5, 0}, 
            new float[]{0, 20, 0}, new float[]{0, 10, 0}
        );
    }

    public static GripDefinition createAuto(com.za.zenith.world.items.Item item) {
        if (item == null) return createRelaxed();

        float scale = item.getVisualScale();
        com.za.zenith.world.items.component.ViewmodelComponent vm = item.getComponent(com.za.zenith.world.items.component.ViewmodelComponent.class);
        if (vm != null) scale *= vm.scale();

        org.joml.Vector3f min = item.getVisualMin();
        org.joml.Vector3f max = item.getVisualMax();
        
        // Берем ширину только в месте хвата (рукоять)
        float width = item.getGripWidth() * scale;
        float height = (max.y - min.y) * scale;

        // Чем шире предмет, тем меньше угол сгиба (пальцы остаются прямее)
        // Для толщины 0.0 -> сгиб 80
        // Для толщины 0.5 -> сгиб 20
        float fingerYaw = Math.clamp(85.0f - (width * 120.0f), 5.0f, 85.0f);
        float tipYaw = fingerYaw * 0.9f;

        // Большой палец опускается вниз в зависимости от высоты предмета
        float thumbPitch = Math.clamp(-40.0f + (height * 30.0f), -40.0f, 0.0f);

        return new GripDefinition(
            new float[]{thumbPitch, 30, 0}, new float[]{thumbPitch * 0.5f, 20, 0}, 
            new float[]{0, fingerYaw, 0}, new float[]{0, tipYaw, 0}, 
            new float[]{0, fingerYaw + 5, 0}, new float[]{0, tipYaw + 5, 0}
        );
    }
}
