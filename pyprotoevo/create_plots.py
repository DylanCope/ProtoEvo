import argparse
from datetime import datetime

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
sns.set()

from pyprotoevo.utils.file_utils import SimulationFiles


def parse_args() -> dict:
    parser = argparse.ArgumentParser()
    parser.add_argument('--simulation', type=str, required=True)
    return vars(parser.parse_args())


def create_generation_plot(sim_files: SimulationFiles):
    generation_df = sim_files.generations_df
    generation_df = generation_df.sort_values('Time Stamp')
    records = generation_df.to_records()

    def are_all_future_records_of_higher_generation(record):
        return all(
            record['Generation'] < records[i]['Generation']
            for i in range(record['index'] + 1, len(records))
        )

    cleaned_stats = {
        (time_stamp := record['Time Stamp']) : sim_files.stats[time_stamp]
        for record in records
        if are_all_future_records_of_higher_generation(record)
    }

    def get_datetime(time_stamp: str) -> datetime:
        return datetime(*map(int, time_stamp.split('-')))

    generation_df_cleaned = pd.DataFrame([
        {
            'Time': get_datetime(time_stamp),
            'Time Stamp': time_stamp,
            'Generation': sim_files.get_stat_value(time_stamp, 'Max Protozoa Generation'),
        }
        for time_stamp in cleaned_stats
    ]);
    generation_df_cleaned['Wall Time'] = generation_df_cleaned['Time'] - generation_df_cleaned['Time'].min()

    plt.title('Cleaned Statistics Generation Plot')
    sns.lineplot(data=generation_df_cleaned, x='Wall Time', y='Generation')

    plt.savefig(sim_files.save_dir / 'plots' / 'generation_plot.png')


def create_population_plot(sim_files: SimulationFiles):
    sns.lineplot(data=sim_files.stats_df, x='Max Protozoa Generation', y='Protozoa')
    sns.lineplot(data=sim_files.stats_df, x='Max Protozoa Generation', y='Plants')
    sns.lineplot(data=sim_files.stats_df, x='Max Protozoa Generation', y='Meat Pellets')
    plt.savefig(sim_files.save_dir / 'plots' / 'population_plot.png')


def main():
    args = parse_args()
    sim_name = args['simulation']
    sim_files = SimulationFiles(sim_name)

    (sim_files.save_dir / 'plots').mkdir(exist_ok=True)
    print(f'Creating plots for simulation {sim_name} in {sim_files.save_dir}/plots')

    create_generation_plot(sim_files)
    create_population_plot(sim_files)


if __name__ == '__main__':
    main()
