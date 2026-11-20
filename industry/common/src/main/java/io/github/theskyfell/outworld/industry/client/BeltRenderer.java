package io.github.theskyfell.outworld.industry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import io.github.theskyfell.outworld.industry.logistics.BeltBlock;
import io.github.theskyfell.outworld.industry.logistics.BeltBlockEntity;
import io.github.theskyfell.outworld.industry.logistics.BeltLane;
import io.github.theskyfell.outworld.industry.logistics.BeltShape;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the items riding a belt, lying flat on its two lanes. Positions come from
 * {@link BeltBlockEntity#projectLane}, so items glide between server updates. On a curve, items follow
 * a quarter circle around the inside corner.
 */
public final class BeltRenderer implements BlockEntityRenderer<BeltBlockEntity, BeltRenderer.State> {
    private static final float ITEM_SCALE = 0.35F;
    private static boolean failureLogged;

    private final ItemModelResolver itemModels;
    private final float[] positions = new float[BeltLane.CAPACITY];

    public BeltRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BeltBlockEntity belt, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(belt, state, partialTicks, cameraPosition, breakProgress);
        state.items.clear();
        try {
            BlockState blockState = belt.getBlockState();
            if (!(blockState.getBlock() instanceof BeltBlock)) {
                return;
            }
            Direction facing = blockState.getValue(BeltBlock.FACING);
            BeltShape shape = blockState.getValue(BeltBlock.SHAPE);
            int seed = (int) belt.getBlockPos().asLong();
            for (int lane = 0; lane < 2; lane++) {
                int count = belt.projectLane(lane, partialTicks, positions);
                for (int i = 0; i < count; i++) {
                    ItemStackRenderState item = new ItemStackRenderState();
                    itemModels.updateForTopItem(item, belt.lane(lane).item(i), ItemDisplayContext.FIXED, belt.getLevel(), null, seed + lane * 8 + i);
                    state.items.add(place(item, facing, shape, lane, positions[i] / BeltLane.LENGTH));
                }
            }
        } catch (RuntimeException e) {
            logOnce(e);
            state.items.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        try {
            for (PlacedItem item : state.items) {
                if (item.item().isEmpty()) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(item.x(), BeltBlock.HEIGHT + 0.02F, item.z());
                poseStack.rotate(Axis.YP.rotationDegrees(item.yaw()));
                poseStack.rotate(Axis.XP.rotationDegrees(90));
                poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                item.item().submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        } catch (RuntimeException e) {
            logOnce(e);
        }
    }

    /**
     * Where an item {@code progress} (0 to 1) along a lane sits in the block, and which way it faces.
     * Lanes run a quarter block either side of the middle; on a curve they are quarter circles around
     * the inside corner, the inner lane a quarter block from it and the outer three quarters.
     */
    private static PlacedItem place(ItemStackRenderState item, Direction facing, BeltShape shape, int lane, float progress) {
        float fx = facing.getStepX();
        float fz = facing.getStepZ();
        Direction leftDirection = facing.getCounterClockWise();
        float lx = leftDirection.getStepX();
        float lz = leftDirection.getStepZ();
        float yaw = -facing.toYRot();
        if (shape == BeltShape.STRAIGHT) {
            float along = progress - 0.5F;
            float side = lane == BeltBlockEntity.LEFT ? 0.25F : -0.25F;
            return new PlacedItem(item, 0.5F + along * fx + side * lx, 0.5F + along * fz + side * lz, yaw);
        }
        // Fed from the left, the curve turns around the front-left corner; from the right, the front-right.
        float sign = shape == BeltShape.LEFT ? 1 : -1;
        boolean inner = (lane == BeltBlockEntity.LEFT) == (shape == BeltShape.LEFT);
        float radius = inner ? 0.25F : 0.75F;
        double angle = progress * Math.PI / 2;
        float back = (float) (radius * Math.cos(angle));
        float in = (float) (radius * Math.sin(angle));
        float pivotX = 0.5F + 0.5F * fx + sign * 0.5F * lx;
        float pivotZ = 0.5F + 0.5F * fz + sign * 0.5F * lz;
        float x = pivotX - back * fx - sign * in * lx;
        float z = pivotZ - back * fz - sign * in * lz;
        return new PlacedItem(item, x, z, yaw - sign * 90 * (1 - progress));
    }

    private static void logOnce(RuntimeException e) {
        if (!failureLogged) {
            failureLogged = true;
            OutworldIndustry.LOG.error("Couldn't draw items on a belt; belt items won't be drawn", e);
        }
    }

    private record PlacedItem(ItemStackRenderState item, float x, float z, float yaw) {
    }

    public static final class State extends BlockEntityRenderState {
        final List<PlacedItem> items = new ArrayList<>();
    }
}
