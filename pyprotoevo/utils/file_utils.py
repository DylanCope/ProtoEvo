from pathlib import Path
import json


class SimulationFiles:

    def __init__(self, name: str):
        self.name = name
        self.genomes = dict()
        self.stats = dict()
        self.save_dir = Path(f'./assets/saves/{self.name}')

    def load(self):
        stats_dir = self.save_dir  / 'stats/summaries'
        genomes_dir = self.save_dir / 'stats/protozoa-genomes'

        self.stats = {
            f.stem: json.load(open(f)) for f in stats_dir.glob('*.json')
        }

        self.genomes = {
            f.stem: json.load(open(f)) for f in genomes_dir.glob('*.json')
        }

        return self.stats, self.genomes
