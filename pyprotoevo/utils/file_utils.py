from pathlib import Path
import json
from typing import List
from functools import cached_property

import pandas as pd
import numpy as np
from datetime import datetime


class SimulationFiles:

    def __init__(self, name: str):
        self.name = name
        self.genomes = dict()
        self.stats = dict()
        self.save_dir = Path(f'./assets/saves/{self.name}')
        self.load()

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

    @cached_property
    def stats_df(self) -> pd.DataFrame:
        if not self.stats:
            return None

        stats_df = pd.DataFrame([
                {'Time Stamp': self.time_from_string(time_stamp), **{
                    stat: self.get_stat_value(time_stamp, stat)
                    for stat in self.basic_stat_names
                    if self.has_stat(time_stamp, stat)
                }}
                for time_stamp in self.stats
            ] + [
                {'Time Stamp': self.time_from_string(time_stamp), **{
                    f'{stat} Error': self.get_stat_error(time_stamp, stat)
                    for stat in self.basic_stat_names
                    if self.has_stat(time_stamp, stat)
                }}
                for time_stamp in self.stats
            ])

        stats_df.fillna(0, inplace=True)
        stats_df.replace(to_replace='NaN', value=np.nan, inplace=True)
        stats_df.head()

        stats_df['Wall Time'] = stats_df['Time Stamp'] - stats_df['Time Stamp'].min()

        return stats_df

    @cached_property
    def stat_names(self) -> List[str]:
        return list(set(
            stat
            for time_stamp in self.stats
            for stat in self.stats[time_stamp]['stats']
        ))

    @cached_property
    def basic_stat_names(self) -> List[str]:
        black_list = ['Node ', 'Organelle', 'Log', 'Molecule']
        return [
            stat for stat in self.stat_names
            if all(x not in stat for x in black_list)
        ]

    @cached_property
    def stat_types(self) -> dict:
        return {
            stat: record['unit']['units']
            for time_stamp in self.stats.keys()
            for stat, record in self.stats[time_stamp]['stats'].items()
            if record['unit'] is not None
        }

    def get_stats_at_time(self, time_stamp) -> dict:
        return self.stats[time_stamp]['stats']

    def get_stat_value(self, time_stamp, stat_name) -> float:
        return self.stats[time_stamp]['stats'][stat_name]['value']

    def get_stat_error(self, time_stamp, stat_name) -> float:
        return self.stats[time_stamp]['stats'][stat_name]['error']

    def has_stat(self, time_stamp, stat_name) -> bool:
        return stat_name in self.stats[time_stamp]['stats']

    def time_from_string(self, time_stamp: str) -> datetime:
        return datetime(*map(int, time_stamp.split('-')))

    @cached_property
    def generations_df(self) -> pd.DataFrame:
        generation_df = pd.DataFrame([
            {
                'Time': self.time_from_string(time_stamp),
                'Time Stamp': time_stamp,
                'Generation': self.get_stat_value(time_stamp, 'Max Protozoa Generation'),
            }
            for time_stamp in self.stats
        ])

        if generation_df.empty:
            raise ValueError('No generations data found')

        generation_df['Wall Time'] = generation_df['Time'] - generation_df['Time'].min()
        generation_df['Next Time Stamp'] = generation_df['Time Stamp'].shift(-1)
        return generation_df
