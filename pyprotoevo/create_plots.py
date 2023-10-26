import argparse
from datetime import datetime
import itertools
import re

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
sns.set()

from pyprotoevo.utils.file_utils import SimulationFiles
from pyprotoevo.utils.stat_utils import unit_to_string


def parse_args() -> dict:
    parser = argparse.ArgumentParser()
    parser.add_argument('--simulation', type=str, required=True)
    parser.add_argument('--quiet', action='store_true', default=False)
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
    ])
    # wall_times = generation_df_cleaned['Time'] - generation_df_cleaned['Time'].min()
    # generation_df_cleaned['Wall Time'] = np.array(wall_times, dtype='timedelta64[s]')

    plt.title('Cleaned Statistics Generation Plot')
    sns.lineplot(data=generation_df_cleaned, x='Time', y='Generation')
    plt.savefig(sim_files.save_dir / 'plots' / 'generation_plot.png')
    plt.close()


def create_population_plot(sim_files: SimulationFiles):
    sns.lineplot(data=sim_files.stats_df, x='Time Elapsed', y='Protozoa')
    plt.savefig(sim_files.save_dir / 'plots/protozoa_population_plot.png')
    plt.close()

    sns.lineplot(data=sim_files.stats_df, x='Time Elapsed', y='Plants')
    plt.savefig(sim_files.save_dir / 'plots/plant_population_plot.png')
    plt.close()

    sns.lineplot(data=sim_files.stats_df, x='Time Elapsed', y='Meat Pellets')
    plt.savefig(sim_files.save_dir / 'plots/meat_population_plot.png')
    plt.close()


def get_counts(sim_files: SimulationFiles, search_term: str):
    photo_stats_df = pd.DataFrame([
        {'Generation': sim_files.get_stat_value(time_stamp, 'Max Protozoa Generation'), **{
            stat: sim_files.get_stat_value(time_stamp, stat)
            for stat in sim_files.stat_names
            if search_term in sim_files.stats
            and 'Count' in stat
            and sim_files.has_stat(time_stamp, stat)
        }}
        for time_stamp in sim_files.stats.keys()
    ])
    photo_stats_df.fillna(0, inplace=True)
    return photo_stats_df[[
        col for col in photo_stats_df.columns if search_term in col
    ]].sum(axis=1)


def stats_plot(sim_files: SimulationFiles):

    # node stats only present with the corresponding attachment types
    search_term_nodes = {
        'Red Light': 'Photoreceptor',
        'Thrust': 'Flagellum',
        'Engulfed Cells': 'Phagocytosis receptor',
        'Binding': 'Adhesion receptor',
    }

    stat_names = {
        match.group(1) for stat in sim_files.stats_df.columns 
        if (match := re.match(r'(.*) [Mean|Max|Min|Count]', stat))
        and 'Died' not in stat
    }

    for search_term in search_term_nodes:
        node_type = search_term_nodes[search_term]
        sim_files.stats_df[f'{node_type} Count'] = get_counts(sim_files, search_term)
        stat_names.add(node_type)

    stat_type_strs = {
        stat: unit_to_string(unit)
        for stat, unit in sim_files.stat_types.items()
    }

    cols = 4
    rows = int(np.ceil(len(stat_names) / cols))

    _, axes = plt.subplots(rows, cols, figsize=(20, rows * 2))
    for ax, stat in itertools.zip_longest(axes.flatten(), sorted(stat_names)):
        if stat is not None:
            variable = f'{stat} Mean'
            if variable in sim_files.stats_df.columns:
                sns_ax = sns.lineplot(data=sim_files.stats_df, x='Max Protozoa Generation', y=variable, ax=ax)
                if f'{stat} Error' in sim_files.stats_df.columns:
                    error = np.power(sim_files.stats_df[stat + ' Error'], 2)
                    lower = sim_files.stats_df[variable] - error
                    upper = sim_files.stats_df[variable] + error
                    sns_ax.fill_between(sim_files.stats_df['Max Protozoa Generation'], lower, upper, alpha=0.2)
            ax.set_title(stat)
            ax.set_xlabel('')
            if stat in stat_type_strs and stat_type_strs[stat] != '':
                ax.set_ylabel(f'${stat_type_strs[stat]}$'.replace('%', r'\%'))
            else:
                ax.set_ylabel('')
        else:
            ax.axis('off')

    plt.tight_layout()
    plt.savefig(sim_files.save_dir / 'plots/stats_plot.png')
    plt.close()

    stats_with_counts = [
        stat for stat in stat_names
        if stat + ' Count' in sim_files.stats_df.columns
    ]
    n_plots = len(stats_with_counts)
    rows = int(np.ceil(n_plots / cols))

    for stat in stat_names:
        if stat + ' Count' in sim_files.stats_df.columns:
            sim_files.stats_df[stat + ' Frequency'] = sim_files.stats_df[stat + ' Count'] / sim_files.stats_df['Protozoa']

    _, axes = plt.subplots(rows, cols, figsize=(20, rows * 2))
    for ax, stat in itertools.zip_longest(axes.flatten(), sorted(stats_with_counts)):
        if stat is not None:
            variable = f'{stat} Frequency'
            if variable in sim_files.stats_df.columns:
                sns.lineplot(data=sim_files.stats_df, x='Max Protozoa Generation', y=variable, ax=ax)
                ax.set_title(stat)
            ax.set_xlabel('')
            if stat in stat_type_strs and stat_type_strs[stat] != '':
                ax.set_ylabel(f'${stat_type_strs[stat]}$'.replace('%', r'\%'))
            else:
                ax.set_ylabel('')
        else:
            ax.axis('off')

    plt.tight_layout()
    plt.savefig(sim_files.save_dir / 'plots/nodes_stats.png')
    plt.close()

    _, ax = plt.subplots(figsize=(8, 5))

    labels_map = {
        # 'Multicell Structure Size': 'In Multicellular Structure',
        'Num Spikes': 'Spikes',
        'Flagellum': 'Flagella',
        'Photoreceptor': 'Photoreceptors',
        'Phagocytosis receptor': 'Phagocytosis Receptors',
        'Adhesion receptor': 'Adhesion Receptors',
    }

    for stat in labels_map:
        sns.lineplot(data=sim_files.stats_df,
                     x='Max Protozoa Generation', y=f'{stat} Frequency',
                     ax=ax, label=labels_map[stat])

    ax.set_ylabel('Frequency (Nodes Per Protozoa)')
    ax.set_xlabel('Max Generation')
    ax.set_title('Change in Surface Node Function Frequency')

    plt.savefig(sim_files.save_dir / 'plots/nodes_frequencies.png')
    plt.close()


def main():
    args = parse_args()
    sim_name = args['simulation']
    sim_files = SimulationFiles(sim_name)

    (sim_files.save_dir / 'plots').mkdir(exist_ok=True)

    if not args['quiet']:
        print(f'Creating plots for simulation {sim_name} in {sim_files.save_dir}/plots')

    create_generation_plot(sim_files)
    create_population_plot(sim_files)
    stats_plot(sim_files)


if __name__ == '__main__':
    main()
