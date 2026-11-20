package io.github.theskyfell.outworld.core.multiblock;

/**
 * A block entity at the heart of a multiblock: {@link MultiblockControllerBlockEntity} for machines
 * built only to be multiblocks, or {@code MultiblockRecipeMachineBlockEntity} for recipe machines.
 * Core's block code, build preview and change tracking reach the structure through {@link #multiblock}.
 */
public interface MultiblockController {
    /** The controller's structure check: whether it is complete, what is missing, where. */
    MultiblockLink multiblock();
}
