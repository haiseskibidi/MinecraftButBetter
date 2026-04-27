package com.za.zenith.world.blocks;

import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.utils.Identifier;

public class BlockTextureMapper {
    // face order: 0:+Z(front), 1:-Z(back), 2:+X(right), 3:-X(left), 4:+Y(top), 5:-Y(bottom)
    public static float[] uvFor(Block block, int face, DynamicTextureAtlas atlas) {
        String key = keyFor(block, face);
        float[] uv = atlas.uvFor(key);
        
        BlockDefinition def = BlockRegistry.getBlock(block.getType());
        if (def != null && def.hasTag("zenith:logs")) {
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
        // uv is 12 floats: (U,V,W) x 4 vertices
        // Vertices: 0:LB, 1:RB, 2:RT, 3:LT
        float u0 = uv[0], v0 = uv[1], w0 = uv[2];
        float u1 = uv[3], v1 = uv[4], w1 = uv[5];
        float u2 = uv[6], v2 = uv[7], w2 = uv[8];
        float u3 = uv[9], v3 = uv[10], w3 = uv[11];

        if (clockwise) {
            // Rotate 90 CW: 0->1, 1->2, 2->3, 3->0
            // BUT we want to keep the same vertex positions (LB, RB, RT, LT) 
            // and rotate the UV mapping ON them.
            // Old mapping: 0,0 1,0 1,1 0,1
            // New mapping: 0,1 0,0 1,0 1,1
            return new float[]{
                u3, v3, w3,
                u0, v0, w0,
                u1, v1, w1,
                u2, v2, w2
            };
        } else {
            // Rotate 90 CCW: 1->0, 2->1, 3->2, 0->3
            return new float[]{
                u1, v1, w1,
                u2, v2, w2,
                u3, v3, w3,
                u0, v0, w0
            };
        }
    }
    
    private static String keyFor(Block block, int face) {
        int type = block.getType();
        BlockDefinition def = BlockRegistry.getBlock(type);
        
        // --- Логика универсальных стадий срубания ---
        if (def instanceof FellingLogBlockDefinition) {
            int metadata = block.getMetadata();
            int woodIndex = metadata & 0x7F; // Игнорируем функциональные флаги (BIT_NATURAL)
            Identifier logId = WoodTypeRegistry.getLogId(woodIndex);
            
            // Ищем stripped версию блока в реестре
            Identifier strippedId = Identifier.of(logId.getNamespace(), "stripped_" + logId.getPath());
            BlockDefinition strippedDef = BlockRegistry.getBlock(strippedId);
            BlockDefinition targetDef = (strippedDef != null) ? strippedDef : BlockRegistry.getBlock(logId);
            
            if (targetDef != null) {
                BlockTextures targetTextures = BlockRegistry.getTextures(targetDef.getId());
                if (targetTextures != null) {
                    byte meta = (byte)(metadata & 0x07);
                    String cap = targetTextures.getTop();
                    String side = targetTextures.getNorth();
                    
                    if (meta == Block.DIR_UP || meta == Block.DIR_DOWN) return (face == 4 || face == 5) ? cap : side;
                    if (meta == Block.DIR_EAST || meta == Block.DIR_WEST) return (face == 2 || face == 3) ? cap : side;
                    if (meta == Block.DIR_NORTH || meta == Block.DIR_SOUTH) return (face == 0 || face == 1) ? cap : side;
                    return side;
                }
            }
        }

        BlockTextures textures = BlockRegistry.getTextures(type);
        if (textures == null) {
            return "zenith/textures/default.png";
        }

        // --- DOUBLE_PLANT logic ---
        if (def != null && def.getPlacementType() == PlacementType.DOUBLE_PLANT) {
            if (block.getMetadata() == 1 && def.getUpperTexture() != null) {
                return def.getUpperTexture();
            }
        }

        // Orientation-sensitive mapping for logs (WOOD)
        if (def != null && def.hasTag("zenith:logs")) {
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




