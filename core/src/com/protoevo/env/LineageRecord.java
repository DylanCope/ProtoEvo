package com.protoevo.env;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Phylogeny entry for a single cell that ever lived. Kept on the
 * Environment so the lineage tree view can walk parent→child even after
 * the parent has died. Records are culled once the entire descendant
 * subtree has died out.
 */
public class LineageRecord implements Serializable {
    public static final long serialVersionUID = 1L;

    public long id;
    public long parentId;       // 0 if a founder (initial spawn)
    public int generation;
    public float birthTime;
    public float deathTime = -1f; // -1 ≡ still alive
    public int aliveDescendants = 1; // self + living descendants
    // Per-cell-class enum: 0=Protozoan, 1=Plant, 2=Meat, -1=other.
    public byte cellType = -1;
    // Children list — populated lazily by the renderer when it walks the
    // tree, to avoid maintaining parent→children invariants on every
    // birth/death (which is hot path).
    public transient List<LineageRecord> childrenScratch;

    public boolean isAlive() { return deathTime < 0f; }
    public List<LineageRecord> children() {
        if (childrenScratch == null)
            childrenScratch = new ArrayList<>(0);
        return childrenScratch;
    }
}
