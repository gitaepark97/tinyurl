#!/usr/bin/env bash
set -euo pipefail

api="$1"       # create | redirect
scenario="$2"  # smoke | load | stress

script="k6/${api}-${scenario}.js"
out_dir="k6/reports/${api}/${scenario}"
mkdir -p "$out_dir"

timestamp=$(date +%Y%m%d-%H%M%S)
k6 run \
  --summary-trend-stats="avg,min,med,p(90),p(95),p(99),max" \
  --summary-export="${out_dir}/${timestamp}.json" \
  "$script"
