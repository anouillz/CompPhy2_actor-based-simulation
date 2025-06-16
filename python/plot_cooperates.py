import glob, re, pandas as pd
import matplotlib.pyplot as plt

paths = glob.glob("../output/baseline-*.csv")

inits, finals = [], []

# this regex will grab an integer part and an optional ,decimal part
rx = re.compile(r"coopRatio\s*=\s*([0-9]+(?:,[0-9]+)?)")

for p in paths:
    # read the very first line
    with open(p, "r", encoding="utf-8") as f:
        header = f.readline()
    m = rx.search(header)
    if not m:
        print(f"[WARN] couldn't parse coopRatio in {p!r}")
        continue

    # replace comma-decimal by dot
    init_str = m.group(1).replace(",", ".")
    init_ratio = float(init_str) * 100

    # load the rest of the CSV (skipping the #-header)
    df = pd.read_csv(p, comment="#", dtype={"step": int, "strategy": str})

    # get the last timestep
    last = df["step"].max()
    final_df = df[df["step"] == last]
    final_pct = final_df["strategy"].eq("C").mean() * 100

    inits.append(init_ratio)
    finals.append(final_pct)

# now plot
plt.figure(figsize=(6,6))
plt.scatter(inits, finals, s=80, edgecolor="k", alpha=0.8)
plt.plot([0,100], [0,100], "--", color="gray", lw=1)
plt.xlabel("Coopération initiale (%)")
plt.ylabel("Coopération finale (%)")
plt.title("Init vs Final Cooperation")
plt.xlim(0,100)
plt.ylim(0,100)
plt.grid(True, ls=":")
plt.tight_layout()
plt.show()
