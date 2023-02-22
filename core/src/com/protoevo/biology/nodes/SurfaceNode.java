package com.protoevo.biology.nodes;

import com.badlogic.gdx.math.Vector2;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.protoevo.biology.cells.Cell;
import com.protoevo.biology.ComplexMolecule;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.biology.MoleculeFunctionalContext;
import com.protoevo.biology.evolution.*;
import com.protoevo.core.Statistics;
import com.protoevo.settings.SimulationSettings;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class SurfaceNode implements Evolvable.Element, Serializable {
    public int id;

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
    private final Statistics stats = new Statistics();
    private final ArrayList<NodeAttachment> candidateAttachments = new ArrayList<>();
    private GeneExpressionFunction geneExpressionFunction;
    private boolean nodeExists = false;

    private final Map<MoleculeFunctionalContext.MoleculeFunction, Float> nodeFunctionSignatures =
            new HashMap<>(NodeAttachment.possibleAttachments.length, 1);
    private final MoleculeFunctionalContext moleculeFunctionalContext = () -> nodeFunctionSignatures;
    private static final float criticalCandidateConstructionProgress = 0.1f;

    public SurfaceNode() {
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
                    candidate.getRequiredMass() / 10f);
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

        if (amount <= 0 || !project.notFinished())
            return;

        float progressionFactor = 1f;

        float availableEnergy = cell.getEnergyAvailable();
        float energyUsed = Math.min(availableEnergy, project.energyToMakeProgress(deltaTime));
        progressionFactor *= (1 + energyUsed) / (1 + project.energyToMakeProgress(deltaTime));

        float availableConstructionMass = cell.getConstructionMassAvailable();
        float massUsed = Math.min(availableConstructionMass, project.massToMakeProgress(deltaTime));
        progressionFactor *= (1 + massUsed) / (1 + project.massToMakeProgress(deltaTime));

        float moleculeUsed = 0;
        for (ComplexMolecule requiredMolecule : project.getRequiredMolecules()) {
            float thisMatch = moleculeFunctionalContext.getMatching(availableMolecule, requiredMolecule);
            float amountRequired = project.complexMoleculesToMakeProgress(deltaTime, requiredMolecule);
            float moleculeUsedHere = Math.min(cell.getComplexMoleculeAvailable(availableMolecule), amountRequired);
            progressionFactor *= (1 + thisMatch * moleculeUsedHere) / (1 + thisMatch * amountRequired);
            moleculeUsed += thisMatch * moleculeUsedHere;
        }

        if (progressionFactor == 0)
            return;

        project.progress(amount * progressionFactor);

        cell.depleteComplexMolecule(availableMolecule, progressionFactor * moleculeUsed);
        cell.depleteEnergy(progressionFactor * energyUsed);
        cell.depleteConstructionMass(progressionFactor * massUsed);
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
        Arrays.fill(outputActivation, 0);
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

    @GeneRegulator(name=outputActivationPrefix + "0", min=-1, max=1)
    public float getActivation0() {
        return outputActivation[0];
    }

    @RegulatedFloat(name=inputActivationPrefix + "1", min=-1, max=1)
    public void setActivation1(float value) {
        inputActivation[1] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "1", min=-1, max=1)
    public float getActivation1() {
        return outputActivation[1];
    }

    @RegulatedFloat(name=inputActivationPrefix + "2", min=-1, max=1)
    public void setActivation2(float value) {
        inputActivation[2] = value;
    }

    @GeneRegulator(name=outputActivationPrefix + "2", min=-1, max=1)
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

    public Statistics getStats() {
        stats.clear();
        stats.putRadian("Angle", angle);
        stats.put("Construction Signature", constructionSignature);
        stats.put("Node Exists", nodeExists);
        stats.put("Function", attachment == null ? "None" : attachment.getName());
        if (attachment != null) {
            stats.putPercentage(
                    "Construction Progress",
                    100 * attachment.getConstructionProgress());
            stats.putMass("Required Mass", attachment.getRequiredMass());
            stats.putEnergy("Required Energy", attachment.getRequiredEnergy());
            stats.putTime("Required Time", attachment.getTimeToComplete());
            for (ComplexMolecule molecule : attachment.getRequiredComplexMolecules().keySet()) {
                stats.putMass(
                        "Required Molecule %.2f".formatted(molecule.getSignature()),
                        attachment.getConstructionProject().getRequiredComplexMoleculeAmount(molecule));
            }

            for (ComplexMolecule molecule : cell.getComplexMolecules())
                if (cell.getComplexMoleculeAvailable(molecule) > 0)
                    stats.putMass("Molecule %.2f Available".formatted(molecule.getSignature()),
                            cell.getComplexMoleculeAvailable(molecule));

            Statistics attachmentStats = attachment.getStats();
            if (attachmentStats != null)
                stats.putAll(attachmentStats);
        }
        for (NodeAttachment candidate : candidateAttachments)
            if (candidate.getConstructionProgress() > 0 && candidate != attachment)
                stats.putPercentage(candidate.getName() + " Construction Progress", candidate.getConstructionProgress());
        return stats;
    }

    public String getAttachmentName() {
        return attachment == null ? "Empty" : attachment.getName();
    }

    public boolean hasAttachment() {
        return attachment != null;
    }

    public float getAttachmentConstructionProgress() {
        return attachment == null ? 0f : attachment.getConstructionProgress();
    }
}
