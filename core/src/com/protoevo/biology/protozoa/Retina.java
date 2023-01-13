package com.protoevo.biology.protozoa;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.google.common.collect.Iterators;
import com.protoevo.biology.ConstructionProject;
import com.protoevo.biology.Food;
import com.protoevo.biology.evolution.Evolvable;
import com.protoevo.biology.evolution.EvolvableFloat;
import com.protoevo.biology.evolution.EvolvableInteger;
import com.protoevo.biology.evolution.GeneRegulator;
import com.protoevo.core.settings.ProtozoaSettings;
import com.protoevo.core.settings.SimulationSettings;
import com.protoevo.utils.Geometry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Retina implements Evolvable.Component, Iterable<Retina.Cell>, Serializable
{
	private static final long serialVersionUID = 5214857174841633362L;

	public static class Cell implements Serializable {
		private static final long serialVersionUID = 1L;
		private final float angle;
		private final Color[] colours;
		private final float[] weights, lengths;
		private final Vector2[] rays;
		private final RetinaConstructionProject constructionProject;

		public Cell(float angle, float cellFov, RetinaConstructionProject constructionProject) {
			this.constructionProject = constructionProject;

			int nRays = 1;
			if (cellFov > ProtozoaSettings.minRetinaRayAngle)
				nRays = (int) (cellFov / ProtozoaSettings.minRetinaRayAngle);

			colours = new Color[nRays];
			weights = new float[nRays];
			lengths = new float[nRays];
			rays = new Vector2[nRays];
			float rayAngle = cellFov / nRays;
			for (int i = 0; i < nRays; i++) {
				float t = -rayAngle / 2 + angle + (nRays - 2*i) * cellFov / (2*nRays);
				rays[i] = Geometry.fromAngle(t);
			}
			this.angle = angle;
			reset();
		}

		public void reset() {
			Arrays.fill(colours, null);
			Arrays.fill(weights, 0);
			Arrays.fill(lengths, Float.MAX_VALUE);
		}

		public void set(int idx, Color c, float sqLen) {
			colours[idx] = c;
			lengths[idx] = sqLen;
			weights[idx] = 1f;
		}

		public Vector2[] getRays() {
			return rays;
		}

		public Color getColour() {
			float r = 0;
			float g = 0;
			float b = 0;
			int nEntities = 0;
			float constructionProgress = constructionProject.getProgress();
			for (int i = 0; i < colours.length; i++) {
				if (colours[i] != null) {
					float w = constructionProgress * weights[i];
					r += w * colours[i].r;
					g += w * colours[i].g;
					b += w * colours[i].b;
					nEntities++;
				}
			}

			if (nEntities == 0)
				return new Color(0, 0, 0, 0);

			return new Color(
					(int) (r / nEntities),
					(int) (g / nEntities),
					(int) (b / nEntities),
					(int) (255 * constructionProgress)
			);
		}

		public boolean anythingVisible() {
			for (Color c : colours)
				if (c != null)
					return true;
			return false;
		}

		public float getAngle() {
			return angle;
		}

		public boolean rayIntersectedEntity(int rayIndex) {
			return colours[rayIndex] != null;
		}

		public float collisionSqLen(int rayIndex) {
			return lengths[rayIndex];
		}
	}

	private static class RetinaConstructionProject extends ConstructionProject {

		RetinaConstructionProject(float fov, int nCells) {
			super(getRequiredMass(fov, nCells),
				  getRequiredEnergy(fov, nCells),
				  getRequiredTime(fov, nCells),
				  getRequiredComplexMolecules(fov, nCells));
		}

		public static float getRequiredMass(float retinaFoV, int nCells) {
			float r = SimulationSettings.minParticleRadius;
			return (float) (Math.log(nCells + 1) * Math.log(retinaFoV + 1) * r * r * r / 20f);
		}

		public static float getRequiredEnergy(float retinaFoV, int nCells) {
			return (float) (Math.log(nCells + 1) * Math.log(retinaFoV + 1) / 20f);
		}

		public static float getRequiredTime(float retinaFoV, int nCells) {
			return (float) (Math.log(retinaFoV + 1) * Math.log(nCells + 1) / 2f);
		}

		public static Map<Food.ComplexMolecule, Float> getRequiredComplexMolecules(float retinaFoV, int nCells) {
			Map<Food.ComplexMolecule, Float> requiredMolecules = new HashMap<>();
			float r = SimulationSettings.minParticleRadius;
			requiredMolecules.put(
					Food.ComplexMolecule.Retinal,
					(float) (nCells * retinaFoV * r * r * r / (20 * Math.PI))
			);
			return requiredMolecules;
		}

		@Override
		public void progress(float delta) {
			super.progress(delta);
		}
	}
	
	private Cell[] cells;
	private float fov;
	private RetinaConstructionProject constructionProject;
	private float health;

	public Retina() {
		constructionProject = new RetinaConstructionProject(0, 0);
		cells = new Cell[0];
		health = 1f;
	}

	@EvolvableFloat(name="Retina FoV", min=(float)(Math.PI / 18), max=(float)(Math.PI * 2 / 3))
	public void setFOV(float fov) {
		this.fov = fov;
		updateState(cells.length);
	}

	@EvolvableInteger(
			name = "Retina Size",
			max = ProtozoaSettings.maxRetinaSize, randomInitialValue=false,
			initValue = ProtozoaSettings.defaultRetinaSize,
			canDisable = true, disableValue = 0
	)
	public void setRetinaSize(int retinaSize) {
		updateState(retinaSize);
	}

	private void updateState(int numCells) {
		constructionProject = new RetinaConstructionProject(fov, numCells);
		cells = new Cell[numCells];
		if (numCells > 0) {
			float cellFov = fov / numCells;
			for (int i = 0; i < numCells; i++) {
				float angle = -cellFov / 2 + fov * (numCells - 2 * i) / (2 * numCells);
				cells[i] = new Cell(angle, cellFov, constructionProject);
			}
		}
	}

	public Retina(int numCells, float fov)
	{
		health = 1f;
	}

	public void reset() {
		for (Cell cell : cells)
			cell.reset();
	}

	public Cell getCell(int cellIdx) {
		return cells[cellIdx];
	}

	@Override
	public Iterator<Cell> iterator() {
		return Iterators.forArray(cells);
	}

	public float getCellAngle() {
		return fov / (float) cells.length;
	}
	
	public float getFov() {
		return fov;
	}

	public int numberOfCells() { return cells.length; }

	public Cell[] getCells() {
		return cells;
	}

	@GeneRegulator(name="Retina Health")
	public float getHealth() {
		if (constructionProject.notFinished())
			return constructionProject.getProgress();
		return health;
	}

	public float updateHealth(float delta, float availableRetinal) {
		if (constructionProject.notFinished())
			return 0;

		float requiredRetinal =
			constructionProject.complexMoleculesToMakeProgress(delta, Food.ComplexMolecule.Retinal) / 20f;

		if (requiredRetinal > 0) {
			float usedRetinal = Math.min(availableRetinal, requiredRetinal);
			health = Math.max(0, health - 0.01f * delta * (requiredRetinal/2f - usedRetinal) / requiredRetinal);
			health = Math.min(health, 1f);
			return usedRetinal;
		}
		return 0f;
	}

	public static float computeWeight(float sqLen) {
		float dMin = 0.9f * ProtozoaSettings.protozoaInteractRange;
		float wD = 0.5f;
		float x = 1 - wD;
		float k = (float) (0.5 * Math.log((1 + x) / (1 - x))) / (dMin - ProtozoaSettings.protozoaInteractRange);
		return (float) (1 + Math.tanh(-k*(Math.sqrt(sqLen) - dMin))) / 2f;
	}

	public static String retinaCellLabel(int idx) {
		return "Retina Sensor " + idx;
	}

	public ConstructionProject getConstructionProject() {
		return constructionProject;
	}

}
