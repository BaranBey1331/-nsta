import sys, json, urllib.request

try:
    req = urllib.request.Request("https://api.github.com/repos/BaranBey1331/-nsta/issues")
    with urllib.request.urlopen(req) as response:
        issues = json.loads(response.read().decode('utf-8'))
        if issues:
            print("Latest Issue Title:", issues[0]['title'])
            print("Issue Body:\n")
            print(issues[0]['body'])
        else:
            print("No issues found.")
except Exception as e:
    print("Error:", e)
