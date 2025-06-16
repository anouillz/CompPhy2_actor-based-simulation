import glob
import os
import re
import pandas as pd
import matplotlib.pyplot as plt

CSV_DIR = "../output"
pattern = os.path.join(CSV_DIR, "Temptation*.csv")
paths = glob.glob(pattern)

# regex to pull out the “1-2” or “1” part
rx = re.compile(r"Temptation(\d+(?:-\d+)*)\.csv")

Ts = []
final_coop = []

for p in paths:
    fn = os.path.basename(p)
    m = rx.match(fn)
    if not m:
        print(f"⚠skipping unexpected filename: {fn}")
        continue

    # turn “1-2” → “1.2”, “1” → “1.0”, etc.
    t_str = m.group(1).replace("-", ".")
    T = float(t_str)

    # skip commented header
    df = pd.read_csv(p, comment="#")

    # pick out the last timestep
    last = df["step"].max()
    final = df[df["step"] == last]

    # compute % cooperators
    pct = (final["strategy"] == "C").mean() * 100

    Ts.append(T)
    final_coop.append(pct)

# sort by temptation value
pairs = sorted(zip(Ts, final_coop))
Ts, final_coop = zip(*pairs)

# plot
plt.figure(figsize=(6, 4))
plt.plot(Ts, final_coop, "o-", lw=2, ms=6)
plt.xlabel("Temptation (T)")
plt.ylabel("Final Cooperation (%)")
plt.title("How Temptation Affects Final Cooperation")
plt.grid(True, linestyle=":", alpha=0.6)
plt.tight_layout()
plt.show()
