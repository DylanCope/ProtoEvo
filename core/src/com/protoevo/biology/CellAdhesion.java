package com.protoevo.biology;

import com.badlogic.gdx.math.MathUtils;
import com.protoevo.biology.cells.Cell;
import com.protoevo.settings.legacy.LegacyGeneralSettings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Implementation of <a href="https://en.wikipedia.org/wiki/Cell_adhesion">cell adhesion</a> logic.
 */
public class CellAdhesion implements Serializable  {

    private static final long serialVersionUID = 1L;

    /**
     * <b>Cell-adhesion molecules</b> (CAMs) can facilitate the following kinds of cell-junctions:
     *
     * (1) <b>Anchoring junctions</b>,
     * which maintain cells together and strengthens contact between cells.
     *
     * (2) <b>Occluding junctions</b>, which seal gaps between cells through
     * cell–cell contact, making an impermeable barrier for diffusion
     *
     * (3) <b>Channel-forming junctions</b>, which links cytoplasm of adjacent
     * cells allowing transport of molecules to occur between cells
     *
     * (3) <b>Signal-relaying junctions</b>, which can be synapses in the nervous system
     */
    public enum CAMJunctionType implements Serializable {
        ANCHORING("Anchoring"),
        OCCLUDING("Occluding"),
        CHANNEL_FORMING("Channel Forming"),
        SIGNAL_RELAYING("Signal Relaying");

        private final String name;

        CAMJunctionType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static CAMJunctionType randomJunctionType() {
            ArrayList<CAMJunctionType> junctionTypes = new ArrayList<>();
            if (LegacyGeneralSettings.enableAnchoringBinding)
                junctionTypes.add(ANCHORING);
            if (LegacyGeneralSettings.enableOccludingBinding)
                junctionTypes.add(OCCLUDING);
            if (LegacyGeneralSettings.enableChannelFormingBinding)
                junctionTypes.add(CHANNEL_FORMING);
            if (LegacyGeneralSettings.enableSignalRelayBinding)
                junctionTypes.add(SIGNAL_RELAYING);
            int idx = MathUtils.random(junctionTypes.size() - 1);
            return junctionTypes.get(idx);
        }
    }

    public static class Binding implements Serializable {

        private static final long serialVersionUID = 1L;
        private final Cell srcCell, destCell;
        private final CAM cam;

        public Binding(Cell srcCell, Cell destCell, CAM cam) {
            this.srcCell = srcCell;
            this.destCell = destCell;
            this.cam = cam;
        }

        public Cell getSourceEntity() {
            return srcCell;
        }

        public Cell getDestinationEntity() {
            return destCell;
        }

        public CAM getCAM() {
            return cam;
        }

        public CAMJunctionType getJunctionType() {
            return cam.getJunctionType();
        }
    }

    /**
     * Cell-adhesion molecule (CAM)
     */
    public interface CAM extends Serializable {

        boolean bindsTo(CAM cam);
        CAMJunctionType getJunctionType();
        int getChemicalBindingSignature();

        default float getProductionCost() {
            return LegacyGeneralSettings.camProductionEnergyCost;
        }
    }

    private final static ConcurrentHashMap<Integer, CAM> existingCAMs =
            new ConcurrentHashMap<>(LegacyGeneralSettings.numPossibleCAMs);

    private static CAM newCAM(Function<Integer, CAM> camBuilder) {
        int newSignature = randomBindingSignature();
        if (existingCAMs.containsKey(newSignature))
            return existingCAMs.get(newSignature);
        CAM cam = camBuilder.apply(newSignature);
        existingCAMs.put(newSignature, cam);
        return cam;
    }

    public static CAM randomCAM() {
//        if (Simulation.RANDOM.nextBoolean())
            return newHomophilicCAM();
//        else
//            return newHeterophilicCAM();
    }

    public static CAM newHomophilicCAM() {
        return newCAM(CellAdhesion::buildHomophilicCAM);
    }

    public static CAM newHeterophilicCAM() {
        return newCAM(CellAdhesion::buildHeterophilicCAM);
    }

    private static CAM buildHomophilicCAM(int camSignature) {
        return new CAM() {
            private final int signature = camSignature;
            private final CAMJunctionType junctionType = CAMJunctionType.randomJunctionType();

            @Override
            public boolean bindsTo(CAM cam) {
                return getChemicalBindingSignature() == cam.getChemicalBindingSignature();
            }

            @Override
            public CAMJunctionType getJunctionType() {
                return junctionType;
            }

            @Override
            public int getChemicalBindingSignature() {
                return signature;
            }

            @Override
            public String toString() {
                return signature + "";
            }
        };
    }

    private static CAM buildHeterophilicCAM(int camSignature) {
        return new CAM() {
            private final int signature = camSignature;
            private final int bindingSignature = randomExistingBindingSignature();
            private final CAMJunctionType junctionType = CAMJunctionType.randomJunctionType();

            @Override
            public boolean bindsTo(CAM cam) {
                return bindingSignature == cam.getChemicalBindingSignature();
            }

            @Override
            public CAMJunctionType getJunctionType() {
                return junctionType;
            }

            @Override
            public int getChemicalBindingSignature() {
                return signature;
            }

            @Override
            public String toString() {
                return signature + "";
            }
        };
    }

    private static int randomExistingBindingSignature() {
        ConcurrentHashMap.KeySetView<Integer, CAM> keySet = existingCAMs.keySet();
        if (keySet.size() > 0) {
            int selectedIdx = MathUtils.random(keySet.size() - 1);
            int i = 0;
            for (Integer signature : keySet) {
                if (i == selectedIdx)
                    return signature;
                i++;
            }
        }
        return randomBindingSignature();
    }

    private static int randomBindingSignature() {
        return MathUtils.random(LegacyGeneralSettings.numPossibleCAMs - 1);
    }

}
