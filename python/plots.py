import os
import pandas as pd
import matplotlib.pyplot as plt

# charge all files
dir_out = os.path.join("..", "output")
files = {
    "Fast Movers":    "baseline-fastMovers.csv",
    "Slow Movers":    "baseline-slowMovers.csv",
    "High Temptation":"baseline-highT.csv",
    "Low Temptation": "baseline-lowT.csv",
    "Fast Adoption": "baseline-FastAdoption.csv",
    "Slow Adoption": "baseline-SlowAdoption.csv",
}
data = {}
for label, fname in files.items():
    path = os.path.join(dir_out, fname)
    df   = pd.read_csv(path, comment="#")
    # get % of cooperators per step
    frac = df.groupby("step")["strategy"].apply(lambda s: (s=="C").mean()*100)
    data[label] = frac

# plot
plt.figure(figsize=(8,5))
for label, frac in data.items():
    plt.plot(frac.index, frac.values, label=label)
plt.xlabel("Step")
plt.ylabel("Cooperation (%)")
plt.title("Evolution of cooperators depending on condition")
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()

finals = { label: frac.values[-1] for label, frac in data.items() }

plt.figure(figsize=(6,4))
plt.bar(finals.keys(), finals.values(), color=["C0","C1","C2","C3"])
plt.ylabel("Final cooperation(%)")
plt.title("Conditions comparison (final step)")
plt.xticks(rotation=15)
plt.tight_layout()
plt.show()



