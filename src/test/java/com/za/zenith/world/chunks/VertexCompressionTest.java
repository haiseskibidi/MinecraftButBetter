package com.za.zenith.world.chunks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VertexCompressionTest {

    @Test
    public void testTextureCoordPacking() {
        float u = 0.5f;
        float v = 0.75f;
        int u16 = (int)(u * 65535.0f) & 0xFFFF;
        int v16 = (int)(v * 65535.0f) & 0xFFFF;
        int packedTex = u16 | (v16 << 16);

        int outU16 = packedTex & 0xFFFF;
        int outV16 = (packedTex >>> 16) & 0xFFFF;
        
        float outU = outU16 / 65535.0f;
        float outV = outV16 / 65535.0f;

        assertEquals(u, outU, 0.001f);
        assertEquals(v, outV, 0.001f);
    }

    @Test
    public void testLayerAndFacePacking() {
        int texLayer = 100;
        int overLayer = 200;
        int face = 5;

        int packedLayers = (texLayer & 0xFFF) | ((overLayer & 0xFFF) << 12) | ((face & 0x7) << 24);

        int outTexLayer = packedLayers & 0xFFF;
        int outOverLayer = (packedLayers >>> 12) & 0xFFF;
        int outFace = (packedLayers >>> 24) & 0x7;

        assertEquals(texLayer, outTexLayer);
        assertEquals(overLayer, outOverLayer);
        assertEquals(face, outFace);
    }

    @Test
    public void testBlockDataPacking() {
        int bType = 45000;
        int nMask = 31;
        int wt = 1;

        int packedBlock = (bType & 0xFFFF) | ((nMask & 0x3F) << 16) | ((wt & 0x1) << 22);

        int outBType = packedBlock & 0xFFFF;
        int outNMask = (packedBlock >>> 16) & 0x3F;
        int outWt = (packedBlock >>> 22) & 0x1;

        assertEquals(bType, outBType);
        assertEquals(nMask, outNMask);
        assertEquals(wt, outWt);
    }

    @Test
    public void testLightAndAOPacking() {
        int l0 = 15;
        int l1 = 8;
        int aoi = 3;
        int pPos = 65000;

        int packedLight = (l0 & 0xF) | ((l1 & 0xF) << 4) | ((aoi & 0x3) << 8) | ((pPos & 0xFFFF) << 10);

        int outL0 = packedLight & 0xF;
        int outL1 = (packedLight >>> 4) & 0xF;
        int outAoi = (packedLight >>> 8) & 0x3;
        int outPPos = (packedLight >>> 10) & 0xFFFF;

        assertEquals(l0, outL0);
        assertEquals(l1, outL1);
        assertEquals(aoi, outAoi);
        assertEquals(pPos, outPPos);
    }
}
