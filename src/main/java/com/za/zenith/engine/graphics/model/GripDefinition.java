package com.za.zenith.engine.graphics.model;

import com.za.zenith.utils.Identifier;

/**
 * Определяет целевые углы поворота пальцев для конкретного хвата.
 * Поддерживает процедурный расчет (Auto-Grip) на основе размеров модели.
 */
public class GripDefinition implements com.za.zenith.utils.LiveReloadable {
    private final float[] thumb;
    private final float[] thumb_tip;
    private final float[] index;
    private final float[] index_tip;
    private final float[] fingers;
    private final float[] fingers_tip;
    private String sourcePath;

    public GripDefinition(float[] thumb, float[] thumb_tip, float[] index, float[] index_tip, float[] fingers, float[] fingers_tip) {
        this.thumb = thumb != null ? thumb : new float[]{0, 0, 0};
        this.thumb_tip = thumb_tip != null ? thumb_tip : this.thumb;
        this.index = index != null ? index : new float[]{0, 0, 0};
        this.index_tip = index_tip != null ? index_tip : this.index;
        this.fingers = fingers != null ? fingers : new float[]{0, 0, 0};
        this.fingers_tip = fingers_tip != null ? fingers_tip : this.fingers;
    }

    public GripDefinition() {
        this(null, null, null, null, null, null);
    }

    public float[] thumb() { return thumb; }
    public float[] thumb_tip() { return thumb_tip; }
    public float[] index() { return index; }
    public float[] index_tip() { return index_tip; }
    public float[] fingers() { return fingers; }
    public float[] fingers_tip() { return fingers_tip; }

    public static GripDefinition createRelaxed() {
        // Естественное расслабленное состояние: пальцы слегка согнуты
        return new GripDefinition(
            new float[]{-10, 0, 0}, new float[]{-10, 0, 0}, 
            new float[]{0, 15, 0}, new float[]{0, 5, 0}, 
            new float[]{0, 20, 0}, new float[]{0, 10, 0}
        );
    }

    public static GripDefinition createAuto(com.za.zenith.world.items.Item item) {
        if (item == null) return createRelaxed();
        
        // 1. Пытаемся найти специфичный хват в реестре по типу инструмента
        String path = item.getIdentifier().getPath();
        GripDefinition def = null;
        if (path.contains("pickaxe")) def = GripRegistry.get("zenith:pickaxe");
        else if (path.contains("axe")) def = GripRegistry.get("zenith:axe");
        else if (path.contains("shovel")) def = GripRegistry.get("zenith:shovel");
        
        if (def != null) return def;

        // 2. ПРОЦЕДУРНЫЙ РАСЧЕТ (fallback)
        // Если специфичного хвата нет, рассчитываем сгиб на основе размеров предмета
        float scale = item.getViewmodelScale();
        if (item.isBlock()) scale *= 0.4f;
        else scale *= 0.85f;
        
        com.za.zenith.world.items.component.ViewmodelComponent vm = item.getComponent(com.za.zenith.world.items.component.ViewmodelComponent.class);
        if (vm != null) scale *= vm.scale();

        org.joml.Vector3f min = item.getVisualMin();
        org.joml.Vector3f max = item.getVisualMax();
        
        // Ширина захвата (X-axis в локальных координатах предмета)
        float width = item.getGripWidth() * scale;
        float height = (max.y - min.y) * scale;

        // Чем шире предмет, тем меньше угол сгиба пальцев (yaw в системе координат костей)
        float fingerYaw = Math.clamp(85.0f - (width * 120.0f), 5.0f, 85.0f);
        float tipYaw = fingerYaw * 0.9f;

        // Сгиб большого пальца зависит от высоты предмета
        float thumbPitch = Math.clamp(-40.0f + (height * 30.0f), -40.0f, 0.0f);

        return new GripDefinition(
            new float[]{thumbPitch, 30, 0}, new float[]{thumbPitch * 0.5f, 20, 0}, 
            new float[]{0, fingerYaw, 0}, new float[]{0, tipYaw, 0}, 
            new float[]{0, fingerYaw + 5, 0}, new float[]{0, tipYaw + 5, 0}
        );
    }

    @Override
    public String getSourcePath() { return sourcePath; }

    @Override
    public void setSourcePath(String path) { this.sourcePath = path; }
}
