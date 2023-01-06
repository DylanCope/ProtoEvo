package com.protoevo.biology.protozoa;

import com.protoevo.core.settings.Settings;
import com.protoevo.core.Simulation;

import java.io.Serializable;

public interface Brain extends Serializable
{
	void tick(Protozoan p);
	float turn(Protozoan p);
	float speed(Protozoan p);
	float attack(Protozoan p);
	boolean wantToMateWith(Protozoan p);
	float energyConsumption();
	float growthControl();
	
	Brain RANDOM = new Brain()
	{
		private static final long serialVersionUID = 1648484737904226314L;

		@Override
		public void tick(Protozoan p) {}

		@Override
		public float turn(Protozoan p) {
			float x = 2f* Simulation.RANDOM.nextFloat() - 1f;
			float t = (float) Math.toRadians(35);
			return t * x;
		}

		@Override
		public float speed(Protozoan p) {
			return Simulation.RANDOM.nextFloat() * Settings.maxProtozoaSpeed;
		}

		@Override
		public float attack(Protozoan p) {
			return Simulation.RANDOM.nextFloat();
		}

		@Override
		public boolean wantToMateWith(Protozoan p) {
			return false;
		}

		@Override
		public float energyConsumption() {
			return 0;
		}

		@Override
		public float growthControl() {
			return Simulation.RANDOM.nextFloat();
		}

	};

	Brain EMPTY = new Brain() {
		@Override
		public void tick(Protozoan p) {}

		@Override
		public float turn(Protozoan p) {
			return 0;
		}

		@Override
		public float speed(Protozoan p) {
			return 0;
		}

		@Override
		public float attack(Protozoan p) {
			return 0;
		}

		@Override
		public boolean wantToMateWith(Protozoan p) {
			return false;
		}

		@Override
		public float energyConsumption() {
			return 0;
		}

		@Override
		public float growthControl() {
			return 0;
		}
	};
}
