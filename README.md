# ProtoEvo 2.0

## Overview

The aim of this project is to create an environment where protozoa-like entities can evolve their behaviours
and morphologies in order to survive and reproduce.
The simulation takes place in a 2D environment with Newtonian physics implemented with Verlet integration.
The following screenshot shows a zoomed-out view of the entire environment.
In the screenshot below, can see procedurally generated rocks shown as brown-grey triangles that form rigid
boundaries for cells moving around the tank fluids. The bright green cells are plants that serve as a sources
of energy and mass for protozoa.
These plants emit chemical pheromones that spread through the environment,
and gradients of which can be detected by the protozoa.
These pheromones are visualised in the screenshot and can be seen as the glowing green trails dispersed
around and behind plant cells.

The primary research objective of this project is to investigate the emergence of multicellular structures,
i.e. the development of coordinated groups of attached cells that incur a survival benefit by being attached.
So far, by implementing cell-adhesion and allowing protozoa to share resources I have seen the
emergence of some quite cool multi-cell behaviour. However, the next step is to achieve cell differentiation
via the evolution of gene-regulatory networks.

![png](/screenshots/full_env_view.png "Full view of the environment")

In the next screenshot we see a close-up of tracking a protozoa in the environment.
The tracked cell is fixed at the centre of the screen as it moves around, and the neural network that controls
its actions is illustrated on the right-hand side of the screen.
This network evolved using a variation of the NEAT algorithm.
The protozoa have a variety of other evolvable traits, including (but not limited to) their size, growth rate, colour,
speed, herbivore factor, and the growth of offensive spikes for harming and killing other protozoa.

![png](/screenshots/tank.png)

Zooming in more on a protozoan, we can see one of their key evolvable traits: vision by light-sensitive "retinas".
These retinas can have variable fields-of-view and acuity, mediated by a ray-casting procedure that feeds into their
control circuits. However, developing such capabilities' comes with a cost. Retinas require a complex molecule call
_retinal_ that is sensitive to light, which itself requires mass and energy to produce from raw material extracted
from feeding on plants. The introduction a prerequisite material for developing such a useful trait that has a cost
to produce opens up the interesting possibility for predation as an alternative strategy for meeting the requirement.

![png](/screenshots/retina_example.png)

This final screenshot shots an example of the kinds of multi-cell structures that can evolve in this simulator.
This is facilitated as the cells have the ability to evolve _Cell-adhesion molecules (CAMs)_
that allow them to bind to other cells and transmit mass, energy, signals, and complex molecules.

![png](/screenshots/evolved_multicells2.png)

## Next Steps

* Evolvable gene regulation to promote cell differentiation.
* Temporal control of gene expression ([regulation of transcription](https://en.wikipedia.org/wiki/Transcriptional_regulation)).
* Environmental and internal temperature to add ecological variety and new cell interaction dynamics.
* Signal relaying channels for cells bound together.
* Improved visualisations of protozoa genes.
* Lineage tracking UI tools.


## Running the Simulation

The simulation is written in Java and uses the built-in Java Swing library for the UI.
I developed this project using the [IntelliJ IDEA](https://www.jetbrains.com/idea/) IDE,
and I recommend using this IDE to run the simulation. To do so, simply clone the repository,
open the project in IntelliJ, and run the `protoevo.core.Application` class build adding an
"Application" build configuration. Be sure to include `-Xmx16G -Dsun.java2d.opengl=true`
as program arguments.

![png](/screenshots/build_config.png)

The dependencies should be handled by Maven. You can check that they are properly configured
by looking at the Modules tab in Project Structure window in IntelliJ.

![png](/screenshots/project_structure.png)