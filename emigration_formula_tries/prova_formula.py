import pandas as pd
import numpy as np
import random
import math
from math import radians, sin, cos, sqrt, atan2, ceil

def calculate_distance(a, b):
    R = 6371
    lat1 = radians(a["Latitudine"])
    lon1 = radians(a["Longitudine"])
    lat2 = radians(b["Latitudine"])
    lon2 = radians(b["Longitudine"])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    h = sin(dlat/2)**2 + cos(lat1)*cos(lat2)*sin(dlon/2)**2
    return 2 * R * atan2(sqrt(h), sqrt(1 - h))

def compute_extremes_per_category(df, base_categories):
    extremes = {}
    for category, config in base_categories.items():
        fake_agent = {
            "utility_weights": config["utility_weights"],
            "pushpull_weights": config["pushpull_weights"]
        }
        utilities = df.apply(lambda r: compute_utility(fake_agent, r), axis=1)
        gravities = []
        pushpulls = []
        for _, orig in df.iterrows():
            for _, dest in df.iterrows():
                if orig["Nome"] != dest["Nome"]:
                    distance = calculate_distance(orig, dest)
                    gravity = compute_gravity(orig["Popolazione"], dest["Popolazione"], distance)
                    pp = compute_pushpull(fake_agent, orig, dest)
                    gravities.append(gravity)
                    pushpulls.append(pp)
        extremes[category] = {
            "min_utility": utilities.min(),
            "max_utility": utilities.max(),
            "min_gravity": min(gravities),
            "max_gravity": max(gravities),
            "min_pushpull": min(pushpulls),
            "max_pushpull": max(pushpulls)
        }
    return extremes

def normalize(x, min_x, max_x, eps=1e-6):
    if abs(max_x - min_x) < eps:
        return 0.5
    return max(0, min(1, (x - min_x)/(max_x - min_x)))

def normalize_pushpull(x, min_x, max_x):
    mid = (max_x + min_x) / 2
    half_range = (max_x - min_x) / 2
    if abs(half_range) < 1e-6:
        return 0
    return (x - mid) / half_range

def compute_gravity(pop_orig, pop_dest, distance_km):
    d_scaled = distance_km / 100
    return pop_orig * pop_dest / (d_scaled**2 + 1e-6)

def compute_utility(agent, region):
    w = agent["utility_weights"]
    return (
        w["Salario"] * region["Salario"] +
        w["Occupazione"] * region["Occupazione"] +
        w["Istruzione"] * region["Istruzione"] -
        w["Affitto"] * region["Affitto"]
    )

def compute_pushpull(agent, orig, dest):
    w = agent["pushpull_weights"]
    return (
        w["Salario"] * (dest["Salario"] - orig["Salario"]) +
        w["Occupazione"] * (dest["Occupazione"] - orig["Occupazione"]) +
        w["Servizi"] * (dest["Servizi"] - orig["Servizi"])
    )

def generate_agents(df, n):
    id_counter = 0
    global base_categories
    base_categories = {
        "Disoccupato_Diploma": {
            "weights": [0.37, 0.35, 0.28],
            "utility_weights": {"Salario": 0.2, "Occupazione": 0.4, "Istruzione": 0.1, "Affitto": 0.25},
            "pushpull_weights": {"Salario": 0.3, "Occupazione": 0.5, "Servizi": 0.2},
            "soglia": 0.3, "età": (18, 22)},
        "Disoccupato_Laurea": {
            "weights": [0.25, 0.4, 0.35],
            "utility_weights": {"Salario": 0.25, "Occupazione": 0.25, "Istruzione": 0.25, "Affitto": 0.25},
            "pushpull_weights": {"Salario": 0.35, "Occupazione": 0.35, "Servizi": 0.3},
            "soglia": 0.2, "età": (23, 30)},
        "Lavoratore_Diploma": {
            "weights": [0.25, 0.35, 0.4],
            "utility_weights": {"Salario": 0.35, "Occupazione": 0.45, "Istruzione": 0.01, "Affitto": 0.19}, 
            "pushpull_weights": {"Salario": 0.3, "Occupazione": 0.35, "Servizi": 0.35},
            "soglia": 0.35, "età": (25, 60)},
        "Lavoratore_Laurea": {
            "weights": [0.2, 0.5, 0.3],
            "utility_weights": {"Salario": 0.3, "Occupazione": 0.3, "Istruzione": 0.3, "Affitto": 0.1},
            "pushpull_weights": {"Salario": 0.45, "Occupazione": 0.25, "Servizi": 0.30},
            "soglia": 0.25, "età": (25, 60)}}
    
    agents = []
    for _, row in df.iterrows():
        for cat, config in base_categories.items():
            for _ in range(n):
                agent = {
                    "id_agente": id_counter,
                    "origine": row["Nome"],
                    "categoria": cat,
                    "weights": config["weights"].copy(),
                    "utility_weights": config["utility_weights"].copy(),
                    "pushpull_weights": config["pushpull_weights"].copy(),
                    "soglia_base": config["soglia"],  # Mantieni la soglia base separata
                    "eta": random.randint(*config["età"]),
                    "famiglia": random.choice([True, False]),
                    "anni_disoccupato": random.randint(0, 5),
                    "anni_stabile": random.randint(1, 10)
                }
                agents.append(agent)
                id_counter += 1
    return agents

def noise(agent):
    if agent["eta"] < 30:
        sigma = 0.04 if "Disoccupato" in agent["categoria"] else 0.05
        if agent["famiglia"]:
            sigma = 0.03
    elif 30 <= agent["eta"] < 40:
        sigma = 0.02 if not agent["famiglia"] else 0.01
    else:
        sigma = 0.005
    if "Laurea" in agent["categoria"]:
        sigma *= 0.8
    return np.random.normal(0, sigma)

def adjust_agent(agent):
    wG, wU, wP = agent["weights"]
    if agent["famiglia"]:
        wG *= 1.5
        wU *= 0.7
        wP *= 1.2
    if agent["eta"] < 30:
        wG *= 0.7
        wU *= 0.95
        wP *= 0.95
    total = wG + wU + wP
    agent["weights"] = [wG / total, wU / total, wP / total]

    uw = agent["utility_weights"]
    if agent["famiglia"]:
        uw["Affitto"] *= 1.4
    if agent["eta"] > 40:
        uw["Istruzione"] *= 0.4
    total_u = sum(uw.values())
    for k in uw:
        uw[k] /= total_u

    pw = agent["pushpull_weights"]
    if agent["famiglia"]:
        pw["Servizi"] *= 1.2
    if agent["eta"] < 30:
        pw["Salario"] *= 1.2
    total_p = sum(pw.values())
    for k in pw:
        pw[k] /= total_p

def calculate_treshold(agent, orig_region, distance):
    # FIX: Parti sempre dalla soglia base
    soglia = agent["soglia_base"]
    
    if agent["famiglia"]:
        soglia += 0.12
    if agent["eta"] > 35:
        soglia += ceil(agent["eta"] / 10) * 0.05
    if agent["anni_stabile"] > 2:
        soglia += agent["anni_stabile"] * 0.08
    if "Disoccupato" in agent["categoria"] and agent["anni_disoccupato"] < 2:
        soglia += 0.01
    if distance > 200:
        soglia += ((distance - 200) / 100) * 0.05 * (2000 / (orig_region["Salario"] + 1e-6))
    
    return soglia

def decide(agent, df, extremes_per_category):
    category = agent["categoria"]
    extremes = extremes_per_category[category]
    orig = df[df["Nome"] == agent["origine"]].iloc[0]
    candidati = df[df["Nome"] != orig["Nome"]].sample(n=min(10, len(df)-1))
    utility_orig = normalize(compute_utility(agent, orig), extremes["min_utility"], extremes["max_utility"])
    log = []
    
    for _, dest in candidati.iterrows():
        distance = calculate_distance(orig, dest)
        soglia_calcolata = calculate_treshold(agent, orig, distance)
        gravity = compute_gravity(orig["Popolazione"], dest["Popolazione"], distance)

        gravity_norm = normalize(gravity, extremes["min_gravity"], extremes["max_gravity"])
        utility_dest = normalize(compute_utility(agent, dest), extremes["min_utility"], extremes["max_utility"])
        pushpull_norm = normalize_pushpull(compute_pushpull(agent, orig, dest), extremes["min_pushpull"], extremes["max_pushpull"])

        attr = (agent["weights"][0] * gravity_norm +
                agent["weights"][1] * (utility_dest - utility_orig) +
                agent["weights"][2] * pushpull_norm)
        emigrato = attr > soglia_calcolata + noise(agent)

        log.append({
            "id_agente": agent["id_agente"],
            "categoria": agent["categoria"],
            "origine": agent["origine"],
            "destinazione": dest["Nome"],
            "famiglia": agent["famiglia"],
            "anni_stab": agent["anni_stabile"],
            "attrattivita": round(attr, 4),
            "soglia": round(soglia_calcolata, 4),
            "eta": agent["eta"],
            "emigrato": emigrato,
            "gravity": round(gravity_norm, 4),
            "pp": round(pushpull_norm, 4),
            "uty T": round(utility_dest - utility_orig, 4)
        })
    return log

def run_simulation():
    df = pd.read_csv("regioni_istat.csv")
    agents = generate_agents(df, n=100)
    extremes_per_category = compute_extremes_per_category(df, base_categories)

    logs = []
    for agent in agents:
        adjust_agent(agent)
        logs.extend(decide(agent, df, extremes_per_category))

    df_log = pd.DataFrame(logs)
    df_log.to_csv("log.csv", index=False)
    print_summary(df_log)
    return df_log

def print_summary(df_log):
    emigrati = df_log[(df_log["emigrato"] == True) & (df_log["destinazione"] != df_log["origine"])]
    unique = emigrati.drop_duplicates(subset=["id_agente"])
    total_per_cat = df_log.drop_duplicates(subset=["id_agente"])
    total_per_cat = total_per_cat.groupby(["origine", "categoria"]).size().reset_index(name="total")
    grouped = unique.groupby(["categoria", "origine", "destinazione"]).size().reset_index(name="count")
    merged = pd.merge(grouped, total_per_cat, on=["origine", "categoria"])
    merged["percentuale"] = (merged["count"] / merged["total"]) * 100

    total_all = unique.groupby(["origine", "destinazione"]).size().reset_index(name="count")
    total_pop = df_log.drop_duplicates(subset=["id_agente"]).groupby("origine").size().reset_index(name="total")
    total_merged = pd.merge(total_all, total_pop, on="origine")
    total_merged["percentuale"] = (total_merged["count"] / total_merged["total"]) * 100
    total_merged["categoria"] = "TOTALE"

    merged = merged[["categoria", "origine", "destinazione", "count", "total", "percentuale"]]
    total_merged = total_merged[["categoria", "origine", "destinazione", "count", "total", "percentuale"]]

    final_rows = []
    current_region = None 
    full_data = pd.concat([total_merged, merged]).sort_values(by=["origine", "categoria", "destinazione"])

    for origine, blocco in full_data.groupby("origine"):
        if current_region is not None:
            final_rows.append(["", "", "", "", "", ""])
        current_region = origine

        for _, row in blocco.iterrows():
            final_rows.append([
                row["categoria"], row["origine"], row["destinazione"],
                int(row["count"]), int(row["total"]), round(row["percentuale"], 4)
            ])

        total_popolazione = total_pop[total_pop["origine"] == origine]["total"].values[0]
        total_emigrati = unique[unique["origine"] == origine].shape[0]
        perc_totale = round((total_emigrati / total_popolazione) * 100, 2)

        cat_counts = unique[unique["origine"] == origine].groupby("categoria").size()
        cat_totals = total_per_cat[total_per_cat["origine"] == origine].set_index("categoria")["total"]
        cat_perc = {cat: round((cat_counts.get(cat, 0) / cat_totals.get(cat, 1)) * 100, 2) for cat in cat_totals.index}
        cat_str = "; ".join([f"{k}={v:.2f}%" for k, v in cat_perc.items()])

        final_rows.append(["TOTALE_RIEPILOGO", origine, "", "", "", f"{perc_totale:.2f}%"])
        final_rows.append(["", "", cat_str, "", "", ""])

    df_final = pd.DataFrame(final_rows, columns=["categoria", "origine", "destinazione", "count", "total", "percentuale"])
    df_final.to_csv("results.csv", index=False)

    print("\n SALDO MIGRATORIO REGIONALE (percentuale)")
    immigrati = unique.groupby("destinazione").size().reset_index(name="immigrati")
    emigrati = unique.groupby("origine").size().reset_index(name="emigrati")
    popolazione_simulata = df_log.drop_duplicates(subset=["id_agente"]).groupby("origine").size().reset_index(name="pop")

    saldo = pd.merge(popolazione_simulata, emigrati, on="origine", how="left").fillna(0)
    saldo = saldo.rename(columns={"origine": "regione"})
    saldo = pd.merge(saldo, immigrati, left_on="regione", right_on="destinazione", how="left").fillna(0)
    saldo = saldo.drop(columns=["destinazione"])

    saldo["%_persi"] = round(saldo["emigrati"] / saldo["pop"] * 100, 2)
    saldo["%_guadagnati"] = round(saldo["immigrati"] / saldo["pop"] * 100, 2)
    saldo["saldo_%"] = saldo["%_guadagnati"] - saldo["%_persi"]
    saldo = saldo.sort_values(by="saldo_%", ascending=False)

    for _, row in saldo.iterrows():
        print(f"🟦 {row['regione']}: +{row['%_guadagnati']}% guadagnati, -{row['%_persi']}% persi → saldo {row['saldo_%']}%")

if __name__ == "__main__":
    run_simulation()