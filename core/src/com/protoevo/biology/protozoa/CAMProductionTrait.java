package com.protoevo.biology.protozoa;

import com.protoevo.biology.CellAdhesion;
import com.protoevo.biology.evolution.Trait;
import com.protoevo.core.Simulation;

import java.util.HashMap;
import java.util.Map;

public class CAMProductionTrait implements Trait<Map<CellAdhesion.CAM, Float>> {
    public static final long serialVersionUID = 1L;

    private static Map<CellAdhesion.CAM, Float> disabledValue = new HashMap<>();
    private final Map<CellAdhesion.CAM, Float> camMap;
    private final String geneName;
    private boolean disabled;

    public CAMProductionTrait(String geneName) {
        this.geneName = geneName;
        this.camMap = newRandomValue();
        disabled = false;
    }

    public CAMProductionTrait(String geneName, Map<CellAdhesion.CAM, Float> camMap, boolean disabled) {
        this.geneName = geneName;
        this.camMap = camMap;
        this.disabled = disabled;
    }

    @Override
    public Map<CellAdhesion.CAM, Float> getValue(Map<String, Object> dependencies) {
        if (dependencies.containsKey("Disable " + geneName))
            disabled = (boolean) dependencies.get("Disable " + geneName);
        if (disabled)
            return disabledValue;
        return camMap;
    }

    @Override
    public boolean canDisable() {
        return true;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public Map<CellAdhesion.CAM, Float> newRandomValue() {
        if (camMap == null)
            return new HashMap<>();
        Map<CellAdhesion.CAM, Float> newMap = new HashMap<>();
        for (CellAdhesion.CAM cam : camMap.keySet()) {
            if (Simulation.RANDOM.nextBoolean()) {
                newMap.put(cam, camMap.get(cam));
            } else {
                newMap.put(cam, Simulation.RANDOM.nextFloat());
            }
        }
        CellAdhesion.CAM newCAM = CellAdhesion.randomCAM();
        newMap.put(newCAM, Simulation.RANDOM.nextFloat());
        return newMap;
    }

    @Override
    public Trait<Map<CellAdhesion.CAM, Float>> createNew(Map<CellAdhesion.CAM, Float> value) {
        return new CAMProductionTrait(geneName, value, disabled);
    }

    @Override
    public String valueString() {
        Map<CellAdhesion.CAM, Float> map = getValue();
        StringBuilder str = new StringBuilder();
        for (CellAdhesion.CAM cam : map.keySet())
            str.append(cam.toString()).append(";")
                    .append(map.get(cam).toString()).append(";")
                    .append(cam.getJunctionType().toString().charAt(0));
        return str.toString();
    }

    @Override
    public String getTraitName() {
        return geneName;
    }
}
