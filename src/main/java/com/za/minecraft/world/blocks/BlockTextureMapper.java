package com.za.minecraft.world.blocks;

import com.za.minecraft.engine.graphics.DynamicTextureAtlas;

public class BlockTextureMapper {
    // face order: 0:+Z(front), 1:-Z(back), 2:+X(right), 3:-X(left), 4:+Y(top), 5:-Y(bottom)
    public static float[] uvFor(Block block, int face, DynamicTextureAtlas atlas) {
        String key = keyFor(block, face);
        float[] uv = atlas.uvFor(key);
        // For logs on horizontal axes, rotate side texture 90° so fibers align with axis
        if (block.getType() == BlockType.WOOD) {
            Block.Axis axis = block.getAxis();
            if (axis == Block.Axis.X) {
                if (face == 0 || face == 1 || face == 4 || face == 5) {
                    return rotateUv90(uv, true);
                }
            } else if (axis == Block.Axis.Z) {
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
        BlockType type = block.getType();
        BlockTextures textures = BlockRegistry.getTextures(type);
        if (textures == null) {
            return "minecraft/textures/block/dirt.png";
        }

        // Orientation-sensitive mapping for logs (WOOD)
        if (type == BlockType.WOOD) {
            Block.Axis axis = block.getAxis();
            String cap = textures.getTop();
            String side = textures.getNorth(); // any side is fine; all equal
            return switch (axis) {
                case Y -> (face == 4 || face == 5) ? cap : side;
                case X -> (face == 2 || face == 3) ? cap : side;
                case Z -> (face == 0 || face == 1) ? cap : side;
            };
        }

        return textures.getTextureForFace(face);
    }
}


