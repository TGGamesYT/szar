package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.BlueprintBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public class BlueprintWrappedModel implements BakedModel {

    private final BakedModel blueprint;
    private final BakedModel stored;
    private final BlockState blueprintState;
    private final BlockState storedState;
    private final BlueprintBlockEntity entity;

    public BlueprintWrappedModel(BakedModel blueprint, BakedModel stored,
                                 BlockState blueprintState, BlockState storedState, BlueprintBlockEntity entity) {
        this.blueprint = blueprint;
        this.stored = stored;
        this.blueprintState = blueprintState;
        this.storedState = storedState;
        this.entity = entity;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        List<BakedQuad> original = blueprint.getQuads(blueprintState, face, random);
        List<BakedQuad> result = new ArrayList<>(original.size());

        for (BakedQuad quad : original) {
            int[] data = quad.getVertexData().clone();

            Direction quadFace = quad.getFace();
            Sprite target = getSpriteForFace(stored, storedState, quadFace);

            remapUVs(data, quad.getSprite(), target);

            BlockPos pos = entity.getPos();
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            Vec3d camPos = camera.getPos();
            double dx = pos.getX() + 0.5 - camPos.x;
            double dy = pos.getY() + 0.5 - camPos.y;
            double dz = pos.getZ() + 0.5 - camPos.z;
            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
            float offsetAmount = 0.0001f + (float)distance * 1e-5f;
            offsetAmount = Math.min(offsetAmount, 0.001f); // clamp max
            offsetVertsAlongNormal(data, quad.getFace(), offsetAmount);

            result.add(new BakedQuad(
                    data,
                    quad.getColorIndex(),
                    quadFace,
                    target,
                    quad.hasShade()
            ));
        }

        return result;
    }

    // --- IMPORTANT: delegate everything else properly ---

    @Override
    public boolean useAmbientOcclusion() {
        return stored.useAmbientOcclusion(); // important for lighting
    }

    @Override
    public boolean hasDepth() {
        return stored.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return stored.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return stored.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return stored.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return stored.getOverrides();
    }

    // --- helpers ---

    private Sprite getSpriteForFace(BakedModel model, BlockState state, Direction face) {
        Random rand = Random.create(42L);

        List<BakedQuad> quads = model.getQuads(state, face, rand);
        if (!quads.isEmpty()) return quads.get(0).getSprite();

        quads = model.getQuads(state, null, rand);
        for (BakedQuad q : quads) {
            if (q.getFace() == face) return q.getSprite();
        }

        return model.getParticleSprite();
    }

    private void remapUVs(int[] vertexData, Sprite from, Sprite to) {
        int stride = 8;

        for (int i = 0; i < 4; i++) {
            int uvIndex = i * stride + 4;

            float u = Float.intBitsToFloat(vertexData[uvIndex]);
            float v = Float.intBitsToFloat(vertexData[uvIndex + 1]);

            float nu = (u - from.getMinU()) / (from.getMaxU() - from.getMinU());
            float nv = (v - from.getMinV()) / (from.getMaxV() - from.getMinV());

            float newU = to.getMinU() + nu * (to.getMaxU() - to.getMinU());
            float newV = to.getMinV() + nv * (to.getMaxV() - to.getMinV());

            vertexData[uvIndex]     = Float.floatToRawIntBits(newU);
            vertexData[uvIndex + 1] = Float.floatToRawIntBits(newV);
        }
    }

    private void offsetVertsAlongNormal(int[] vertexData, Direction face, float amount) {
        if (face == null) return; // skip general quads

        float dx = face.getOffsetX() * amount;
        float dy = face.getOffsetY() * amount;
        float dz = face.getOffsetZ() * amount;

        int vertexSize = 8; // X,Y,Z,COLOR,U,V,UV2,NORMAL
        for (int i = 0; i < 4; i++) {
            int base = i * vertexSize;
            float x = Float.intBitsToFloat(vertexData[base]);
            float y = Float.intBitsToFloat(vertexData[base + 1]);
            float z = Float.intBitsToFloat(vertexData[base + 2]);

            vertexData[base]     = Float.floatToRawIntBits(x + dx);
            vertexData[base + 1] = Float.floatToRawIntBits(y + dy);
            vertexData[base + 2] = Float.floatToRawIntBits(z + dz);
        }
    }
}