package com.za.zenith.world.items.component;

/**
 * Компонент для предметов-локаторов.
 * Разрешает отображение сущностей на миникарте.
 */
public record LocatorComponent(float range, boolean showEnemies, boolean showItems) implements ItemComponent {
}
