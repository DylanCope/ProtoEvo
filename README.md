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

**Note:** This project is still in development, and is not yet ready for public consumption. It also requires a
quite powerful computer to run.

The simulation is written in Java and uses the built-in Java Swing library for the UI.
I developed this project using the [IntelliJ IDEA](https://www.jetbrains.com/idea/) IDE,
and I recommend using this to run the simulation.
The project is built using LibGDX, which is a cross-platform game development library.
It relies on OpenGL for rendering, and so you will need to have the appropriate drivers installed,
as well as CUDA for GPU acceleration (if you have a CUDA-capable GPU). For the time being I have not 
implemented a CPU-only version of the simulation, so you will need a CUDA-capable GPU to run the simulation.
Finally, the project is only tested on Windows 11, but it should work on Linux and Mac OS X as well.

**Steps**
- Clone the repository, open the project in IntelliJ.
For a general guide to running LibGDX projects, see [this article](https://libgdx.com/wiki/start/import-and-running).
- Download and install the CUDA toolkit for your GPU. 
  You can find the latest version [here](https://developer.nvidia.com/cuda-downloads).
  The current version of the project is tested with CUDA 12.0.
- To test your installation try to compile the CUDA kernels in the `assets/kernels` directory using the `nvcc` compiler. 
  For example, `nvcc -m64 -ptx diffusion.cu`. You should see a `diffusion.ptx` file generated in the same directory.
  The project will automatically compile the CUDA kernels when it is run, but it is useful to test this beforehand.
- Run the Gradle task `desktop:run` to run the simulation. In IntelliJ, this can be done by opening the Gradle
  tool window and navigating to `ProtoEvo > desktop > other > run`.