import sys, json
data = json.load(sys.stdin)
for j in data.get("jobs", []):
    print(f"Job ID: {j['id']}, Name: {j['name']}, Conclusion: {j['conclusion']}")
