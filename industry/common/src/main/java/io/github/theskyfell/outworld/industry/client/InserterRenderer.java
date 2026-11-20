package io.github.theskyfell.outworld.industry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import io.github.theskyfell.outworld.industry.logistics.InserterBlock;
import io.github.theskyfell.outworld.industry.logistics.InserterBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the item an inserter carries, swinging in an arc over the arm from the back to the front. */
public final class InserterRenderer implements BlockEntityRenderer<InserterBlockEntity, InserterRenderer.State> {
    private static boolean failureLogged;

    private final ItemModelResolver itemModels;

    public InserterRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InserterBlockEntity inserter, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(inserter, state, partialTicks, cameraPosition, breakProgress);
        state.item.clear();
        try {
            if (inserter.held().isEmpty() || !(inserter.getBlockState().getBlock() instanceof InserterBlock)) {
                return;
            }
            itemModels.updateForTopItem(state.item, inserter.held(), ItemDisplayContext.GROUND, inserter.getLevel(), null,
                    (int) inserter.getBlockPos().asLong());
            Direction facing = inserter.facing();
            double angle = inserter.swingProgress(partialTicks) * Math.PI;
            float along = (float) (-0.45 * Math.cos(angle));
            state.x = 0.5F + along * facing.getStepX();
            state.z = 0.5F + along * facing.getStepZ();
            state.y = (float) (0.55 + 0.35 * Math.sin(angle));
            state.yaw = -facing.toYRot();
        } catch (RuntimeException e) {
            logOnce(e);
            state.item.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        try {
            poseStack.pushPose();
            poseStack.translate(state.x, state.y, state.z);
            poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
            poseStack.scale(0.6F, 0.6F, 0.6F);
            state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        } catch (RuntimeException e) {
            logOnce(e);
        }
    }

    private static void logOnce(RuntimeException e) {
        if (!failureLogged) {
            failureLogged = true;
            OutworldIndustry.LOG.error("Couldn't draw an inserter's item; held items won't be drawn", e);
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float x;
        float y;
        float z;
        float yaw;
    }
}
