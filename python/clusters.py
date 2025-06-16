import pandas as pd
import matplotlib.pyplot as plt

# 1) Load the CSV you exported:
df = pd.read_csv("../output/clust-nA1000.csv", comment="#")

# 2) Compute percent cooperation and cluster count per step:
#    (Assuming you have columns 'step', 'cooperatorClusters', and 'strategy'.)
grouped = df.groupby("step").agg(
    coop_pct = ("strategy", lambda s: 100*(s.eq("C").sum()/len(s))),
    clusters = ("cooperatorClusters", "first")
).reset_index()

# 3) Smooth the cluster count with a rolling window (optional):
grouped["clusters_smooth"] = grouped["clusters"].rolling(window=5, center=True, min_periods=1).mean()

# 4) Plot
fig, ax1 = plt.subplots(figsize=(9,4))

# Cooperation line
ax1.plot(grouped.step, grouped.coop_pct,
         color="tab:blue", lw=2, label="Cooperators (%)")
ax1.set_xlabel("Step")
ax1.set_ylabel("Cooperation (%)", color="tab:blue")
ax1.tick_params(axis="y", labelcolor="tab:blue")
ax1.set_ylim(0,100)

# Twin axis for clusters
ax2 = ax1.twinx()
ax2.plot(grouped.step, grouped.clusters_smooth,
         color="tab:orange", lw=1.5, alpha=0.4, label="Cooperator Clusters (smoothed)")
ax2.set_ylabel("Cluster Count", color="tab:orange")
ax2.tick_params(axis="y", labelcolor="tab:orange")

# Legends
lines, labels = ax1.get_legend_handles_labels()
lines2, labels2 = ax2.get_legend_handles_labels()
ax1.legend(lines + lines2, labels + labels2, loc="upper right")

ax1.set_title("Evolution of Cooperation & Cluster Count")
ax1.grid(True, which="both", axis="x", linestyle="--", alpha=0.5)
fig.tight_layout()
plt.show()
