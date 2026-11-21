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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws an inserter's arm, swinging over the top from the block behind to the block in front with the
 * item it carries, and back empty. The base and post are the baked block model; only the arm is drawn
 * here, from its own model ({@code block/inserter_arm}), whose centre is the pivot on top of the post.
 */
public final class InserterRenderer implements BlockEntityRenderer<InserterBlockEntity, InserterRenderer.State> {
    /** The arm's model, as an item model so it can be drawn like the carried item. */
    private static final Identifier ARM_MODEL = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "inserter_arm");
    /** The pivot, on top of the post. */
    private static final float PIVOT_Y = 12 / 16F;
    /** From the pivot to the middle of the gripper, where the item hangs. */
    private static final float REACH = 5.75F / 16F;
    private static boolean failureLogged;

    private final ItemModelResolver itemModels;
    /** Made when first drawn, not while resources load (item components aren't ready then). */
    private ItemStack armStack;

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
        state.arm.clear();
        state.item.clear();
        try {
            if (!(inserter.getBlockState().getBlock() instanceof InserterBlock)) {
                return;
            }
            if (armStack == null) {
                armStack = new ItemStack(Items.STICK);
                armStack.set(DataComponents.ITEM_MODEL, ARM_MODEL);
            }
            int seed = (int) inserter.getBlockPos().asLong();
            itemModels.updateForTopItem(state.arm, armStack, ItemDisplayContext.NONE, inserter.getLevel(), null, seed);
            // Out over the top with an item (0 = pointing back, pi = pointing forward), back again empty.
            float progress = inserter.swingProgress(partialTicks);
            boolean carrying = !inserter.held().isEmpty();
            state.angle = (float) (Math.PI * (carrying ? progress : 1 - progress));
            state.yaw = -inserter.facing().toYRot();
            if (carrying) {
                itemModels.updateForTopItem(state.item, inserter.held(), ItemDisplayContext.GROUND, inserter.getLevel(), null, seed);
            }
        } catch (RuntimeException e) {
            logOnce(e);
            state.arm.clear();
            state.item.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.arm.isEmpty()) {
            return;
        }
        try {
            poseStack.pushPose();
            // Turned so +z points out of the inserter's front, then swung about the pivot.
            poseStack.translate(0.5F, PIVOT_Y, 0.5F);
            poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
            poseStack.rotate(Axis.XP.rotation(state.angle));
            state.arm.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            if (!state.item.isEmpty()) {
                // At the gripper, which the arm model puts REACH behind the pivot (-z).
                poseStack.translate(0F, 0F, -REACH);
                poseStack.rotate(Axis.XP.rotation(-state.angle)); // keep the item upright
                poseStack.translate(0F, -0.12F, 0F);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        } catch (RuntimeException e) {
            logOnce(e);
        }
    }

    private static void logOnce(RuntimeException e) {
        if (!failureLogged) {
            failureLogged = true;
            OutworldIndustry.LOG.error("Couldn't draw an inserter's arm; inserter arms won't be drawn", e);
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState arm = new ItemStackRenderState();
        final ItemStackRenderState item = new ItemStackRenderState();
        float angle;
        float yaw;
    }
}
