import csv
from collections import OrderedDict

rows = [r for r in csv.DictReader(open('catalogs/tree-features.csv', encoding='utf-8'))
        if r['jar'] == 'RegionsUnexploredForge-0.5.6+1.20.1.jar']
seen = OrderedDict()
for r in rows:
    for f in r['families'].split(';'):
        if f and f not in seen:
            seen[f] = r['placed_feature']
for f, v in seen.items():
    print('        feature("%s", "regions_unexplored:%s", CompatibilityLevel.UNRESOLVED);' % (v, f))
