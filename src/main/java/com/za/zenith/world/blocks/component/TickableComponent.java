package com.za.zenith.world.blocks.component;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;

/**
 * Компонент для блоков с логикой, выполняющейся каждый тик.
 */
public class TickableComponent extends BlockComponent {
    @Override
    public void onTick(World world, BlockPos pos) {
        // Логика будет переопределена в подклассах или настраиваться через параметры
    }
}
