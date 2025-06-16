import glob
import os
import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

# ─── CONFIG ────────────────────────────────────────────────────────────

# Pattern pour tous les fichiers ratio*.csv
FILES = glob.glob(os.path.join("../output", "ratio*.csv"))

# Extrait le ratio initial (en %) du nom de fichier, ex "ratio20.csv" → 20.0
def extract_ratio(fname):
    m = re.search(r"ratio(\d+)", os.path.basename(fname))
    return float(m.group(1)) if m else None

# ─── COLLECTE DES DONNÉES ───────────────────────────────────────────────

records = []
for fn in FILES:
    r0 = extract_ratio(fn)
    if r0 is None:
        print(f"[WARN] impossible d’extraire le ratio depuis {fn}")
        continue

    df = pd.read_csv(fn, comment="#")
    # dernière étape
    last = df["step"].max()
    final = df[df["step"] == last]

    # 1) taux final de coopérateurs
    pct_coop = 100.0 * (final["strategy"] == "C").mean()

    # 2) nombre de clusters de coopérateurs
    # on suppose que vous avez une colonne 'cooperatorClusters' dans vos CSV
    if "cooperatorClusters" in final.columns:
        clusters = final["cooperatorClusters"].iloc[0]
    else:
        clusters = np.nan

    records.append((r0, pct_coop, clusters))

# on construit un DataFrame trié
df = pd.DataFrame(records, columns=["init_ratio", "final_coop_pct", "coop_clusters"])
df = df.sort_values("init_ratio")

# ─── TRACÉ ──────────────────────────────────────────────────────────────

fig, ax1 = plt.subplots(figsize=(7,4))

# Scatter + trait pour coopérateurs
ax1.scatter(df.init_ratio,
            df.final_coop_pct,
            s=80,
            facecolors="white",
            edgecolors="tab:blue",
            label="Final Coop (%)")
ax1.plot(  df.init_ratio,
           df.final_coop_pct,
           color="tab:blue",
           linewidth=2,
           alpha=0.6)

ax1.set_xlabel("Initial Cooperation (%)")
ax1.set_ylabel("Final Cooperation (%)", color="tab:blue")
ax1.tick_params(axis="y", labelcolor="tab:blue")
ax1.set_ylim(0, 105)
ax1.grid(True, linestyle="--", alpha=0.3)

# Deuxième axe pour les clusters
ax2 = ax1.twinx()
ax2.scatter(df.init_ratio,
            df.coop_clusters,
            s=80,
            facecolors="white",
            edgecolors="tab:red",
            label="Coop Clusters")
ax2.plot(  df.init_ratio,
           df.coop_clusters,
           color="tab:red",
           linestyle="--",
           linewidth=2,
           alpha=0.6)

ax2.set_ylabel("Cooperator Clusters", color="tab:red")
ax2.tick_params(axis="y", labelcolor="tab:red")

# Légende combinée
h1, l1 = ax1.get_legend_handles_labels()
h2, l2 = ax2.get_legend_handles_labels()
ax1.legend(h1+h2, l1+l2, loc="upper left")

plt.title("Final Cooperation & Clusters vs Initial Cooperation Ratio")
fig.tight_layout()
plt.show()
