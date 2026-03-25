package com.za.minecraft.world.blocks;

import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.utils.Identifier;

public class BlockTextureMapper {
    // face order: 0:+Z(front), 1:-Z(back), 2:+X(right), 3:-X(left), 4:+Y(top), 5:-Y(bottom)
    public static float[] uvFor(Block block, int face, DynamicTextureAtlas atlas) {
        String key = keyFor(block, face);
        float[] uv = atlas.uvFor(key);
        
        BlockDefinition def = BlockRegistry.getBlock(block.getType());
        if (def != null && def.hasTag("minecraft:logs")) {
            byte meta = (byte)(block.getMetadata() & 0x07); // Игнорируем флаги
            if (meta == Block.DIR_EAST || meta == Block.DIR_WEST) {
                if (face == 0 || face == 1 || face == 4 || face == 5) {
                    return rotateUv90(uv, true);
                }
            } else if (meta == Block.DIR_NORTH || meta == Block.DIR_SOUTH) {
                if (face == 2 || face == 3) {
                    return rotateUv90(uv, true);
                }
            }
        }
        return uv;
    }

    private static float[] rotateUv90(float[] uv, boolean clockwise) {
        float u0 = uv[0], v0 = uv[1];
        float u1 = uv[2], v1 = uv[5];
        if (clockwise) {
            return new float[]{
                u0, v1,
                u0, v0,
                u1, v0,
                u1, v1
            };
        } else {
            return new float[]{
                u1, v0,
                u1, v1,
                u0, v1,
                u0, v0
            };
        }
    }
    
    private static String keyFor(Block block, int face) {
        int type = block.getType();
        BlockDefinition def = BlockRegistry.getBlock(type);
        
        // --- Логика универсальных стадий срубания ---
        if (def instanceof FellingLogBlockDefinition) {
            int woodIndex = block.getMetadata() & 0xFF;
            Identifier logId = WoodTypeRegistry.getLogId(woodIndex);
            
            // Ищем stripped версию блока в реестре
            Identifier strippedId = Identifier.of(logId.getNamespace(), "stripped_" + logId.getPath());
            BlockDefinition strippedDef = BlockRegistry.getBlock(strippedId);
            
            if (strippedDef != null) {
                BlockTextures strippedTextures = BlockRegistry.getTextures(strippedDef.getId());
                if (strippedTextures != null) {
                    return (face == 4 || face == 5) ? strippedTextures.getTop() : strippedTextures.getNorth();
                }
            }
            
            // Если не нашли stripped, используем оригинальное бревно (лучше чем земля или весь атлас)
            BlockTextures originalTextures = BlockRegistry.getTextures(BlockRegistry.getBlock(logId).getId());
            if (originalTextures != null) {
                return (face == 4 || face == 5) ? originalTextures.getTop() : originalTextures.getNorth();
            }
        }

        BlockTextures textures = BlockRegistry.getTextures(type);
        if (textures == null) {
            return "minecraft/textures/block/dirt.png";
        }

        // --- DOUBLE_PLANT logic ---
        if (def != null && def.getPlacementType() == PlacementType.DOUBLE_PLANT) {
            if (block.getMetadata() == 1 && def.getUpperTexture() != null) {
                return def.getUpperTexture();
            }
        }

        // Orientation-sensitive mapping for logs (WOOD)
        if (def != null && def.hasTag("minecraft:logs")) {
            byte meta = (byte)(block.getMetadata() & 0x07); // Игнорируем флаги
            String cap = textures.getTop();
            String side = textures.getNorth(); // any side is fine; all equal
            if (meta == Block.DIR_UP || meta == Block.DIR_DOWN) return (face == 4 || face == 5) ? cap : side;
            if (meta == Block.DIR_EAST || meta == Block.DIR_WEST) return (face == 2 || face == 3) ? cap : side;
            if (meta == Block.DIR_NORTH || meta == Block.DIR_SOUTH) return (face == 0 || face == 1) ? cap : side;
            return side;
        }

        return textures.getTextureForFace(face);
    }
}


