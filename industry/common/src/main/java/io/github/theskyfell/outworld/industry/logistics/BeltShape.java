package io.github.theskyfell.outworld.industry.logistics;

import net.minecraft.util.StringRepresentable;

/**
 * How a belt is drawn. A belt fed only from one side, with nothing feeding it from behind, is a curve:
 * items turn the corner and keep their lanes. Otherwise it is straight, and belts feeding it from the
 * side put their items on the lane nearest them.
 */
public enum BeltShape implements StringRepresentable {
    STRAIGHT("straight"),
    /** Fed only from its left side (looking the way items leave it): items turn left onto it. */
    LEFT("left"),
    /** Fed only from its right side: items turn right onto it. */
    RIGHT("right");

    private final String name;

    BeltShape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
