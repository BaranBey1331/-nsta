import sys, json
data = json.load(sys.stdin)
for j in data.get("jobs", []):
    print(f"Job: {j['name']} - {j['conclusion']}")
    for s in j.get("steps", []):
        print(f"  Step: {s['name']} - {s['conclusion']}")
