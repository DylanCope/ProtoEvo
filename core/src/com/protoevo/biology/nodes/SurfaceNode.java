package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.protoevo.biology.Cell;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.biology.MoleculeFunctionalContext;
import com.protoevo.biology.evolution.*;
import com.protoevo.settings.SimulationSettings;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SurfaceNode implements Evolvable.Element, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String activationPrefix = "Activation/";
    public static final String inputActivationPrefix = "Input" + activationPrefix;
    public static final String outputActivationPrefix = "Output" + activationPrefix;

    private Cell cell;
    private float angle, constructionSignature, deltaTime;
    private final Vector2 relativePosition = new Vector2(), worldPosition = new Vector2();
    private NodeAttachment attachment = null;
    private final float[] inputActivation = new float[3];
    private final float[] outputActivation = new float[3];
    private int nodeIdx;
    private final Map<String, Float> stats = new HashMap<>();
    private GeneExpressionFunction geneExpressionFunction;
    private boolean nodeExists = false;

    private final Map<MoleculeFunctionalContext.MoleculeFunction, Float> nodeFunctionSignatures =
            new HashMap<>(NodeAttachment.possibleAttachments.length, 1);
    private final MoleculeFunctionalContext moleculeFunctionalContext = () -> nodeFunctionSignatures;
    private static final float criticalCandidateConstructionProgress = 0.1f;

    public SurfaceNode() {
        ArrayList<NodeAttachment> candidateAttachments = new ArrayList<>();
        for (Class<NodeAttachment> attachmentClass : NodeAttachment.possibleAttachments) {
            try {
                NodeAttachment attachment = attachmentClass.getConstructor(SurfaceNode.class)
                        .newInstance(this);
                candidateAttachments.add(attachment);
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to instantiate node attachment: " + e);
            }
        }

        for (int i = 0; i < candidateAttachments.size(); i++) {
            NodeAttachment candidate = candidateAttachments.get(i);
            float signature = i / (float) candidateAttachments.size();
            candidate.getRequiredComplexMolecules().put(
                    ComplexMolecule.fromSignature(signature),
                    candidate.getRequiredMass() / 20f);
            nodeFunctionSignatures.put(
                    (molecule, potency) -> constructCandidate(candidate, molecule, potency),
                    signature);
        }
    }

    private void constructCandidate(NodeAttachment candidate, ComplexMolecule molecule, float potency) {
        if (cell == null)
            return;

        float matching = moleculeFunctionalContext.getMatching(molecule, constructionSignature);
        if (matching > 0 && attachment != null && candidate != attachment) {
            ConstructionProject constructionProject = attachment.getConstructionProject();
            constructionProject.deconstruct(matching * potency * deltaTime);
            if (attachment.getConstructionProgress() < criticalCandidateConstructionProgress)
                attachment = null;

        } else {
            ConstructionProject constructionProject = candidate.getConstructionProject();
            progressProject(constructionProject, molecule, matching * potency);
        }

        if (candidate.getConstructionProgress() >= criticalCandidateConstructionProgress)
            attachment = candidate;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    public void progressProject(ConstructionProject project,
                                ComplexMolecule availableMolecule,
                                float moleculeEffectiveness) {
        float amount = moleculeEffectiveness * deltaTime;

        if (amount <= 0 && !project.notFinished())
            return;

        float scale = 1f;

        float availableEnergy = cell.getEnergyAvailable();
        float energyUsed = Math.min(availableEnergy, project.energyToMakeProgress(deltaTime));
        scale *= energyUsed / project.energyToMakeProgress(deltaTime);

        float availableConstructionMass = cell.getConstructionMassAvailable();
        float massUsed = Math.min(availableConstructionMass, project.massToMakeProgress(deltaTime));
        scale *= massUsed / project.massToMakeProgress(deltaTime);

        float moleculeUsed = 0;
        for (ComplexMolecule requiredMolecule : project.getRequiredMolecules()) {
            float thisMatch = moleculeFunctionalContext.getMatching(availableMolecule, requiredMolecule);
            float amountRequired = thisMatch * project.complexMoleculesToMakeProgress(deltaTime, requiredMolecule);
            float moleculeUsedHere =
                    thisMatch * Math.min(cell.getComplexMoleculeAvailable(availableMolecule), amountRequired);
            moleculeUsed += moleculeUsedHere;
            scale *= moleculeUsedHere / amountRequired;
        }

        project.progress(amount * scale);

        cell.depleteComplexMolecule(availableMolecule, scale * moleculeUsed);
        cell.depleteEnergy(scale * energyUsed);
        cell.depleteConstructionMass(scale * massUsed);
    }


    /**
     * @param signature The signature of the attachment to be added.
     */
    @EvolvableFloat(name = "Construction Signature")
    public void setConstructionSignature(float signature) {
        this.constructionSignature = signature;
    }

    @EvolvableFloat(name = "Angle", max = 2 * (float) Math.PI, regulated = false)
    public void setAngle(float angle) {
        this.angle = angle;
    }

    public Vector2 getRelativePos() {
        float t = cell.getAngle() + angle;
        relativePosition.set((float) Math.cos(t), (float) Math.sin(t)).scl(cell.getRadius());
        return relativePosition;
    }

    public Vector2 getWorldPosition() {
        worldPosition.set(getRelativePos()).add(cell.getPos());
        return worldPosition;
    }

    public void update(float delta) {
        if (nodeExists) {
            handleAttachmentConstructionProjects(delta);

            if (attachment != null) {
                attachment.update(delta, inputActivation, outputActivation);
            }
        } else {
            tryCreate();
        }
    }

    private void handleAttachmentConstructionProjects(float delta) {
        deltaTime = delta;
        for (ComplexMolecule molecule : cell.getComplexMolecules())
            moleculeFunctionalContext.accept(molecule);
    }

    public float requiredArcLength() {
        return (float) (SimulationSettings.maxParticleRadius * 2 * Math.PI / 15f);
    }

    public void tryCreate() {
        if (cell.getSurfaceNodes() == null)
            return;

        for (SurfaceNode node : cell.getSurfaceNodes()) {
            if (node == this || !node.exists())
                continue;
            float dAngle = Math.abs(node.getAngle() - angle);
            float arcLen = dAngle * cell.getRadius();
            if (arcLen < this.requiredArcLength() / 2f + node.requiredArcLength() / 2f) {
                return;
            }
        }
        nodeExists = true;
    }

    public boolean exists() {
        return nodeExists;
    }

    public void setAttachment(NodeAttachment attachment) {
        this.attachment = attachment;
    }

    public NodeAttachment getAttachment() {
        return attachment;
    }

    public float[] getInputActivation() {
        return inputActivation;
    }

    public float[] getOutputActivation() {
        return outputActivation;
    }

    @RegulatedFloat(name=inputActivationPrefix + "0", min=-1, max=1)
    public void setActivation0(float value) {
        inputActivation[0] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "0")
    public float getActivation0() {
        return outputActivation[0];
    }

    @RegulatedFloat(name=inputActivationPrefix + "1", min=-1, max=1)
    public void setActivation1(float value) {
        inputActivation[1] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "1")
    public float getActivation1() {
        return outputActivation[1];
    }

    @RegulatedFloat(name=inputActivationPrefix + "2", min=-1, max=1)
    public void setActivation2(float value) {
        inputActivation[2] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "2")
    public float getActivation2() {
        return outputActivation[2];
    }

    public float getAngle() {
        return angle;
    }

    public Cell getCell() {
        return cell;
    }

    public float getInteractionRange() {
        if (attachment == null)
            return 0f;
        else
            return attachment.getInteractionRange();
    }

    @Override
    public void setIndex(int index) {
        this.nodeIdx = index;
    }

    @Override
    public int getIndex() {
        return nodeIdx;
    }

    @Override
    public void setGeneExpressionFunction(GeneExpressionFunction fn) {
        geneExpressionFunction = fn;
    }

    @Override
    public GeneExpressionFunction getGeneExpressionFunction() {
        return geneExpressionFunction;
    }
}
