package com.protoevo.settings;

public class CellSettings extends Settings {
    public Parameter<Float> complexMoleculeDecayRate = new Parameter<>(
            "Complex Molecule Decay Rate",
            "The rate at which complex molecules decay.",
            1e-6f
    );
    // Monotonic selection-pressure exponent ratcheted by the homeostat.
    // PhagocyticReceptor.engulf uses match^(1 + this) as the digestion
    // efficiency, so:
    //   pressure=0: efficiency = match¹ (linear, very forgiving — 60% match
    //               gives 60% digestion rate, lineages with mediocre keys
    //               can still feed).
    //   pressure=1: efficiency = match² (current "old" behavior — moderate
    //               selection).
    //   pressure=2: efficiency = match³ (strict — partial matches are
    //               heavily penalized; lineages that can't keep up with
    //               plant signature drift starve).
    //   pressure=3: match⁴ (only near-perfect matches feed at full rate).
    //
    // The ratchet in Simulation.maybeRatchetSelection only goes UP, only
    // when population is at or above target AND derivative is small (stable).
    // Starts at 0 so the world is forgiving early-game; the difficulty
    // climbs as the lineages prove they can handle it. There's a one-minute
    // sim-time delay between steps so a transient stable patch can't
    // suddenly multiply the pressure.
    public Parameter<Float> selectionPressureExponent = new Parameter<>(
            "Selection Pressure Exponent",
            "Additional exponent on receptor match for digestion efficiency. "
            + "Ratcheted up monotonically by the homeostat when population is stable.",
            0f);

    // Floor on engulf digestion efficiency. When 1.0, the receptor-match
    // system is effectively OFF — every engulf delivers full digestion
    // regardless of signature, so cells can thrive on the basic engulf
    // mechanic alone. As the homeostat ratchets this DOWN over time, the
    // floor approaches the original 0.02 hard minimum, and lineages whose
    // signatures don't track plant drift lose efficiency. This is what
    // the user means by "gradual ramp": start the world forgiving, only
    // make receptor specialization matter once the population is stable
    // enough to handle it.
    public Parameter<Float> engulfBaseEfficiency = new Parameter<>(
            "Engulf Base Efficiency",
            "Floor on engulf digestion efficiency (1.0 = receptor system effectively off). "
            + "Ratcheted DOWN by the homeostat alongside the MIN_RUN ratchet.",
            1.0f);

    // Engulf MIN_RUN — the BINARY gate, separate from the analog efficiency
    // curve above. A receptor must share a contiguous run of at least
    // MIN_RUN identical residues with the prey's signature to even attempt
    // engulfing. If MIN_RUN is too high relative to what fresh sequences
    // achieve, no engulfs happen — and then the selectionPressureExponent
    // ratchet never fires either (it only runs when population is stable
    // and ≥ target, which requires successful predation in the first place).
    //
    // So MIN_RUN itself needs to start LOW (easy clear) and ratchet UP only
    // when the population can sustain the current difficulty. Mirror of the
    // exponent ratchet: monotonic, +1 step every RATCHET_INTERVAL when
    // population is ≥ target and stable, capped well below sequence length.
    //
    // Initial values:
    //   plant=0: gate off for plants (the design intent is that cells nibble
    //     and engulf plants freely; ratchet phases in plant-receptor
    //     selection as the world stabilizes).
    //   protozoa=12: ALWAYS non-zero. Kin-cannibalism is meant to be HARD
    //     even at game start — without a floor, fresh random-receptor
    //     protozoa would mutually annihilate within seconds and the
    //     species would never get off the ground. 12 of 75 (16%) means
    //     random pairs effectively never match (chance of a 12-run by
    //     coincidence ≈ 64 × (1/20)^12 ≈ 4e-15), so only lineages that
    //     have actively co-evolved toward predator-specialization can
    //     cannibalize. The ratchet then climbs from there toward the
    //     cap, never below.
    //
    // Caps prevent the ratchet from driving evolution to impossibility:
    //   plant cap=15 (30% of length), protozoa cap=20 (~27% of length).
    public Parameter<Integer> plantEngulfMinRun = new Parameter<>(
            "Plant Engulf Min Run",
            "Minimum contiguous receptor-match length required to engulf a plant. "
            + "Starts at 0 (gate off). Ratcheted up by the homeostat when population is stable.",
            0);
    public Parameter<Integer> protozoaEngulfMinRun = new Parameter<>(
            "Protozoa Engulf Min Run",
            "Minimum contiguous receptor-match length required to engulf a protozoan. "
            + "Always non-zero — prevents free kin-cannibalism at game start. "
            + "Ratcheted up by the homeostat when population is stable.",
            12);

    public Parameter<Float> energyDecayRate = new Parameter<>(
            "Energy Decay Rate",
            "Fractional energy decay coefficient. Effective rate is "
            + "delta × this × (radius/minRadius) × energyAvailable.",
            // 0.005: at radius/minR=2.5 and full energy this gives ~1.25%/sec
            // decay, i.e. ~55s half-life for a full cell at rest. Cells must
            // feed regularly to stay topped up; sitting still drains them.
            // Previous interpretation was a flat J/sec which made decay
            // essentially negligible for any cell over a few seconds old.
            .005f
    );
    // Starvation damage: when a cell's energy falls below
    // `starvationThresholdFraction` × its energy cap, it begins to lose
    // health at `starvationDeathRate` × the deficit fraction per sim second.
    // Without this, an energy-depleted cell just stops growing and repairing
    // but never dies — so the homeostat's "decay rate" lever couldn't
    // actually reduce the population, no matter how high it pushed decay.
    // Cells could indefinitely recycle each other via meat without ever
    // hitting an energy-driven death pathway.
    //
    // Threshold is 5% of capacity — newborns spawn at 10% (startingEnergy=50,
    // cap=500) so they have a small buffer before starvation begins. Death
    // rate of 0.2/s means a fully-starved cell dies in ~5 sim seconds, fast
    // enough that the homeostat's decay-rate lever has real authority over
    // an out-of-target population.
    public final Parameter<Float> starvationThresholdFraction = new Parameter<>(
            "Starvation Threshold Fraction",
            "Energy/capacity ratio below which cells start losing health to starvation.",
            0.05f
    );
    public final Parameter<Float> starvationDeathRate = new Parameter<>(
            "Starvation Death Rate",
            "Health lost per second of fully-starved time (linearly scaled by deficit).",
            // 0.2 → 0.05. At 0.2, a fully-starved cell dies in ~5 sim-seconds —
            // not enough time for a fresh cohort to find a chemical-drip
            // halo in plant-poor regions. Repeated diagnostic runs showed
            // ~250 STARVATION deaths in the first 5 sim-seconds of every
            // launch: cells spawned, drained their initial energy reserve,
            // and were killed before diffusion had time to spread plant
            // chemicals across the gaps. 0.05 quadruples the grace period
            // (full starvation kills in ~20 sim-sec) so a cell that
            // wanders into a plant-poor pocket has a real chance to drift
            // back to chemistry before health collapses.
            0.05f
    );
    // Dropped 10× (1e-3 → 1e-4). The drip channel was sustaining cells
    // without ANY receptor match, so plants could outevolve protozoa keys
    // and the cells would just nibble chemicals indefinitely instead of
    // adapting. With drips an order of magnitude smaller, drip alone is a
    // bare survival trickle — cells must engulf to thrive, and engulf
    // rewards receptor-match² (see PhagocyticReceptor). This creates
    // genuine selection pressure for keys that track evolving plant
    // signatures.
    public final Parameter<Float> chemicalExtractionPlantConversion = new Parameter<>(
            "Chemical Extraction Plant Conversion",
            "The amount of food extracted from plant matter in the chemical solution.",
            1e-4f);
    public final Parameter<Float> chemicalExtractionMeatConversion = new Parameter<>(
            "Chemical Extraction Meat Conversion",
            "The amount of food extracted from meat matter in the chemical solution.",
            1e-5f);
    public final Parameter<Float> chemicalExtractionFactor = new Parameter<>(
            "Chemical Extraction Factor",
            "The amount to dilute the chemical solution by when extracting food.",
            100f);
    public final Parameter<Double> energyRequiredForGrowth = new Parameter<>(
            "Energy Required For Growth",
            "Factor controlling how much energy is required for growth.",
            1e3);
    public final Parameter<Float> growthFactor = new Parameter<>(
            "Cell Growth Factor",
            "Controls how quickly cells can grow.",
            3e-2f);
    public final Parameter<Float> digestionFactor = new Parameter<>(
            "Digestion Factor",
            "Controls how quickly cells can digest food.",
            20f);
    // Dropped from 0.01 → 0.003. Combined with the activity-heat generator
    // (below), the old rate killed actively-feeding cells via hyperthermia
    // faster than they could repair, even with full energy reserves.
    // Eating literally cooked them. 0.003 lets temperature mismatch still
    // matter for selection without turning every meal into a death event.
    public final Parameter<Float> temperatureDeathRate = new Parameter<>(
            "Temperature Death Rate",
            "Rate at which a cell looses health when outside its temperature tolerance range.",
            .003f);
    public final Parameter<Float> minTemperatureTolerance = new Parameter<>(
            "Min Temperature Tolerance",
            "Minimum temperature tolerance (+/- degrees before suffering adverse effects).",
            2f);
    public final Parameter<Float> maxTemperatureTolerance = new Parameter<>(
            "Max Temperature Tolerance",
            "Maximum temperature tolerance (+/- degrees before suffering adverse effects).",
            5f);
    public final Parameter<Float> temperatureToleranceEnergyCost = new Parameter<>(
            "Temperature Tolerance Energy Cost",
            "Energy cost per degree of temperature tolerance per unit time.",
            1 / 100f);
    // Dropped 0.5 → 0.1. Repeated diagnostic runs showed HYPERTHERMIA
    // becoming the dominant death cause once the population hits target
    // (~25–35 deaths/interval, vs ~5 from starvation). The 0.5 rate let
    // cells equilibrate at envTemp + 5°C from passive movement+GRN
    // activity alone, putting them above the +3°C tolerance window at
    // baseline. 0.1 brings the equilibrium to ~envTemp + 1°C — well
    // inside tolerance, so only cells that are EXTREMELY active (large
    // burst of digestion, repair, growth all at once) flirt with the
    // overheat band. Combat / hunger pressure still scale with activity,
    // just at a sane multiplier.
    public final Parameter<Float> activityHeatGeneration = new Parameter<>(
            "Cell Temperature Death Rate",
            "Heat generated per unit activity per unit time.",
            0.1f);
    public final Parameter<Float> basicParticleMassDensity = new Parameter<>(
            "Base Particle Mass Density",
            "The mass density of a basic particle.",
            1f);
    public final Parameter<Float> startingAvailableCellEnergy = new Parameter<>(
            "Starting Available Cell Energy",
            "Starting amount of energy available to cells.",
            // 50 → 250. The cap is 500 × radius / minRadius. Cells spawn at
            // radii in [0.02, 0.06], giving caps of [500, 1500] and 5%
            // starvation thresholds in [25, 75]. With starting=50, all but
            // the smallest spawns were BELOW threshold from t=0 — cells took
            // starvation damage before they could reach a plant, causing
            // ~50 deaths/sec in the first 5 sim-seconds of every run. At 250
            // even the largest spawn (cap=1500, threshold=75) starts comfortably
            // above the threshold (250 > 75), so cells have time to forage
            // before metabolism + decay drain them past the danger line.
            250f);
    public final Parameter<Float> energyCapFactor = new Parameter<>(
            "Energy Cap Factor",
            "Maximum energy a cell can have at the minimum size.",
            500f);
    public final Parameter<Float> startingAvailableConstructionMass = new Parameter<>(
            "Starting Available Construction Mass",
            "Starting amount of construction mass available to cells.",
            10e-3f);
    public final Parameter<Float> engulfExtractionWasteMultiplier = new Parameter<>(
            "Engulf Extraction Waste Multiplier",
            "Multiplier of mass removed from engulfed cell for each unit extracted.",
            1.05f
    );
    //    public final Settings.Parameter<Float> engulfRangeFactor = new Settings.Parameter<>(
//            "Engulf Range Factor",
//            "Multiplier of mass removed from engulfed cell for each unit extracted.",
//            1.05f
//    );
    public final Parameter<Float> repairRate = new Parameter<>(
            "Cell Repair Rate",
            "",
            5e-1f
    );
    public final Parameter<Float> repairMassFactor = new Parameter<>(
            "Cell Repair Mass Factor",
            "Amount of mass required to repair as a percent of the cell's mass.",
            0.05f
    );
    public final Parameter<Float> repairEnergyFactor = new Parameter<>(
            "Cell Repair Energy Factor",
            "Amount of energy per unit mass required for repair.",
            5e2f
    );
    public final Parameter<Float> bindingResourceTransport = new Parameter<>(
            "Binding Resource Transport",
            "Percentage of a cell's resources transported in one unit of time",
            2.0f
    );
    public final Parameter<Float> repairActivity = new Parameter<>(
            "Cell Repair Activity",
            "Amount of activity generated by repairing.",
            1f
    );
    public final Parameter<Float> growthActivity = new Parameter<>(
            "Cell Growth Activity",
            "Amount of activity generated by growing.",
            1f
    );
    public final Parameter<Float> digestionActivity = new Parameter<>(
            "Cell Digestion Activity",
            "Amount of activity generated per mass generated.",
            1f
    );
    public final Parameter<Float> kineticEnergyActivity = new Parameter<>(
            "Kinetic Energy Activity",
            "Amount of activity generated per kinetic energy generated.",
            .1f
    );
    public final Parameter<Float> grnHiddenNodeActivity = new Parameter<>(
            "GRN Node Activity",
            "Amount of cell activity generated per GRN node activation.",
            .1f
    );
//    public Settings.Parameter<Integer> surfaceNodeDim = new Settings.Parameter<>(
//            "Surface Node Dimension",
//            "Dimension of the surface node I/O channel. Should be 1 or 3.",
//            1,
//            false
//    );

    public CellSettings() {
        super("Cell");
    }
}
