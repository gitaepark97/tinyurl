#!/usr/bin/env python3
"""Turn one or more k6 --summary-export JSON files into a plain-language Korean HTML report.

Two modes:
  --mode sections (default): one section per input, for combining different test TYPES
                              (smoke/load/stress/...) into one document.
  --mode compare: one row-per-metric comparison table across inputs, for tracking the
                  SAME test over time (e.g. before/after an improvement). The first
                  --input given is treated as the baseline; later ones are compared
                  against it with improve/regress deltas.
"""

import argparse
import html
import json
import re
import sys
from datetime import datetime
from pathlib import Path

BUILTIN_DURATION_METRICS = {"http_req_duration", "iteration_duration"}
SKIP_METRICS = {
    "http_req_blocked", "http_req_connecting", "http_req_receiving",
    "http_req_sending", "http_req_tls_handshaking", "http_req_waiting",
    "data_received", "data_sent", "vus", "vus_max",
}

STYLE = """
    body { font-family: -apple-system, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif; max-width: 900px; margin: 40px auto; padding: 0 20px; color: #222; line-height: 1.6; }
    h1 { border-bottom: 3px solid #2563eb; padding-bottom: 10px; }
    h2 { margin-top: 40px; color: #1e3a8a; border-top: 1px solid #eee; padding-top: 24px; }
    h3 { margin-top: 24px; }
    table { width: 100%; border-collapse: collapse; margin: 16px 0; }
    th, td { border: 1px solid #ddd; padding: 10px 14px; text-align: left; }
    th { background: #f3f4f6; }
    .meta { color: #555; font-size: 0.95em; }
    .badge { display: inline-block; padding: 3px 12px; border-radius: 12px; font-weight: bold; font-size: 0.9em; }
    .pass { background: #dcfce7; color: #166534; }
    .fail { background: #fee2e2; color: #991b1b; }
    .improve { color: #166534; font-weight: bold; }
    .regress { color: #991b1b; font-weight: bold; }
    .note { background: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 16px; margin: 16px 0; }
    .footer { margin-top: 50px; font-size: 0.85em; color: #888; border-top: 1px solid #eee; padding-top: 12px; }
    code { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
    a { color: #2563eb; }
"""


def fmt_ms(value):
    if value is None:
        return "-"
    if value >= 1000:
        return f"{value / 1000:.2f}s"
    return f"{value:.1f}ms"


def fmt_pct(value):
    if value is None:
        return "-"
    return f"{value * 100:.2f}%"


def slugify(label):
    return re.sub(r"[^a-zA-Z0-9\-]+", "-", label).strip("-").lower() or "section"


def threshold_badges(entry):
    thresholds = entry.get("thresholds")
    if not thresholds:
        return ""
    badges = []
    for expr, failed in thresholds.items():
        cls = "fail" if failed else "pass"
        label = "FAIL" if failed else "PASS"
        badges.append(f'<span class="badge {cls}">{label}</span> (기준: {html.escape(expr)})')
    return " ".join(badges)


def any_threshold_failed(metrics):
    for entry in metrics.values():
        for failed in entry.get("thresholds", {}).values():
            if failed:
                return True
    return False


def find_duration_breakdown(metrics):
    """Custom Trend metrics ending in _duration are treated as per-step timings."""
    rows = []
    for name, entry in metrics.items():
        if name in BUILTIN_DURATION_METRICS or name in SKIP_METRICS:
            continue
        if not name.endswith("_duration"):
            continue
        if "avg" not in entry:
            continue
        rows.append((name, entry))
    rows.sort(key=lambda r: r[1].get("avg", 0), reverse=True)
    return rows


def build_distribution_insights(duration_rows):
    """Insights about a single metric's own spread (outliers), not cross-metric comparisons."""
    insights = []
    for name, entry in duration_rows:
        avg = entry.get("avg")
        p95 = entry.get("p(95)")
        med = entry.get("med")
        if avg is not None and p95 is not None and p95 > 0 and avg > p95 * 1.2:
            insights.append(
                f"<b>{html.escape(name)}</b>는 평균({fmt_ms(avg)})이 p95({fmt_ms(p95)})보다 높습니다 — "
                f"소수의 요청이 크게 튀는 패턴입니다."
            )
        elif med is not None and p95 is not None and p95 > 0 and p95 > med * 3:
            insights.append(
                f"<b>{html.escape(name)}</b>는 p95({fmt_ms(p95)})가 중앙값({fmt_ms(med)})보다 3배 이상 커서 "
                f"일부 요청이 눈에 띄게 지연되고 있습니다."
            )
    return insights


# ---------------------------------------------------------------------------
# sections mode: one report section per input (different test types combined)
# ---------------------------------------------------------------------------

def render_section(label, data):
    anchor = slugify(label)
    metrics = data.get("metrics", {})
    http_reqs = metrics.get("http_reqs", {})
    http_req_failed = metrics.get("http_req_failed", {})
    http_req_duration = metrics.get("http_req_duration", {})
    checks = metrics.get("checks", {})
    vus_max = metrics.get("vus_max", {})
    iterations = metrics.get("iterations", {})

    overall_pass = not any_threshold_failed(metrics)
    duration_rows = find_duration_breakdown(metrics)
    insights = build_distribution_insights(duration_rows)

    checks_total = checks.get("passes", 0) + checks.get("fails", 0)
    checks_rate = (checks.get("passes", 0) / checks_total) if checks_total else 1.0

    breakdown_html = ""
    if duration_rows:
        rows_html = "\n".join(
            f"<tr><td>{html.escape(name)}</td>"
            f"<td>{fmt_ms(e.get('avg'))}</td>"
            f"<td>{fmt_ms(e.get('med'))}</td>"
            f"<td>{fmt_ms(e.get('p(95)'))}</td>"
            f"<td>{fmt_ms(e.get('max'))}</td></tr>"
            for name, e in duration_rows
        )
        breakdown_html = f"""
<h3>구간별 응답속도</h3>
<table>
<tr><th>구간</th><th>평균</th><th>중앙값</th><th>p95</th><th>최대</th></tr>
{rows_html}
</table>
"""

    insights_html = ""
    if insights:
        items = "\n".join(f"<li>{i}</li>" for i in insights)
        insights_html = f'<div class="note"><b>해석</b><ul>{items}</ul></div>'

    overall_badge = '<span class="badge pass">합격</span>' if overall_pass else '<span class="badge fail">불합격</span>'

    section_html = f"""
<section id="{anchor}">
<h2>{html.escape(label)}</h2>
<p class="meta">
    동시 사용자(VU): {vus_max.get('value', vus_max.get('max', '-'))}명 ·
    총 반복: {iterations.get('count', '-')}회 ·
    총 요청: {http_reqs.get('count', '-')}건
</p>

<table>
    <tr><th>항목</th><th>결과</th></tr>
    <tr><td>전체 판정</td><td>{overall_badge}</td></tr>
    <tr><td>요청 실패율</td><td>{fmt_pct(http_req_failed.get('value', 0))} {threshold_badges(http_req_failed)}</td></tr>
    <tr><td>응답 검증(Checks) 통과율</td><td>{fmt_pct(checks_rate)} ({checks.get('passes', 0)}/{checks_total})</td></tr>
    <tr><td>전체 응답속도 p95</td><td>{fmt_ms(http_req_duration.get('p(95)'))} {threshold_badges(http_req_duration)}</td></tr>
</table>

{breakdown_html}
{insights_html}
</section>
"""
    return anchor, overall_pass, section_html


def build_overview_table(results):
    pass_badge = '<span class="badge pass">합격</span>'
    fail_badge = '<span class="badge fail">불합격</span>'
    rows = "\n".join(
        f'<tr><td><a href="#{anchor}">{html.escape(label)}</a></td>'
        f'<td>{pass_badge if passed else fail_badge}</td></tr>'
        for label, anchor, passed in results
    )
    return f"""
<h2>전체 요약</h2>
<table>
<tr><th>테스트 유형</th><th>판정</th></tr>
{rows}
</table>
"""


def render_sections_doc(labeled_data, title):
    results = []
    sections_html = []
    for label, data in labeled_data:
        anchor, passed, section_html = render_section(label, data)
        results.append((label, anchor, passed))
        sections_html.append(section_html)

    overview_html = build_overview_table(results) if len(results) > 1 else ""
    return f"{overview_html}\n{''.join(sections_html)}"


# ---------------------------------------------------------------------------
# compare mode: same test over time, one row per metric, deltas vs baseline
# ---------------------------------------------------------------------------

def extract_stats(data):
    metrics = data.get("metrics", {})
    stats = {}

    http_req_failed = metrics.get("http_req_failed", {})
    stats["error_rate"] = http_req_failed.get("value", 0.0)

    hrd = metrics.get("http_req_duration", {})
    stats["overall_avg"] = hrd.get("avg")
    stats["overall_p95"] = hrd.get("p(95)")

    checks = metrics.get("checks", {})
    total = checks.get("passes", 0) + checks.get("fails", 0)
    stats["checks_rate"] = (checks.get("passes", 0) / total) if total else 1.0

    stats["overall_pass"] = not any_threshold_failed(metrics)

    for name, entry in find_duration_breakdown(metrics):
        stats[f"{name}__avg"] = entry.get("avg")
        stats[f"{name}__p95"] = entry.get("p(95)")

    return stats


def compare_cell(value, baseline, higher_is_better, fmt):
    if value is None:
        return "-"
    text = fmt(value)
    if baseline is None:
        return text
    diff = value - baseline
    if diff == 0:
        return text
    pct = (diff / abs(baseline) * 100) if baseline else None
    if pct is not None and abs(pct) < 2.0:
        return text  # within noise threshold, don't call it out
    improved = (diff > 0) if higher_is_better else (diff < 0)
    arrow = "▲" if diff > 0 else "▼"
    cls = "improve" if improved else "regress"
    pct_text = f" ({pct:+.1f}%)" if pct is not None else ""
    return f'<span class="{cls}">{text} {arrow}{pct_text}</span>'


COMPARE_FIXED_ROWS = [
    ("error_rate", "요청 실패율", False, fmt_pct),
    ("overall_p95", "전체 응답속도 p95", False, fmt_ms),
    ("overall_avg", "전체 응답속도 평균", False, fmt_ms),
    ("checks_rate", "응답 검증 통과율", True, fmt_pct),
]


def build_compare_summary(run_labels, stats_list):
    """Plain-language summary of baseline vs the most recent run."""
    if len(stats_list) < 2:
        return ""
    baseline = stats_list[0]
    latest = stats_list[-1]
    lines = []
    for key, label, higher_is_better, fmt in COMPARE_FIXED_ROWS:
        b, v = baseline.get(key), latest.get(key)
        if b is None or v is None or b == 0:
            continue
        pct = (v - b) / abs(b) * 100
        if abs(pct) < 2.0:
            continue
        improved = (v > b) if higher_is_better else (v < b)
        word = "개선" if improved else "악화"
        lines.append(f"<li><b>{html.escape(label)}</b>: {fmt(b)} → {fmt(v)} ({pct:+.1f}%, {word})</li>")
    if not lines:
        return '<div class="note"><b>요약</b><p>기준({}) 대비 최신({}) 결과에 2% 이상 유의미한 변화가 없습니다.</p></div>'.format(
            html.escape(run_labels[0]), html.escape(run_labels[-1])
        )
    return f'<div class="note"><b>요약 — {html.escape(run_labels[0])} → {html.escape(run_labels[-1])}</b><ul>{"".join(lines)}</ul></div>'


def render_compare_doc(labeled_data, title):
    run_labels = [label for label, _ in labeled_data]
    stats_list = [extract_stats(data) for _, data in labeled_data]
    baseline_stats = stats_list[0]

    dyn_names = []
    seen = set()
    for stats in stats_list:
        for key in stats:
            if key.endswith("__avg"):
                base = key[:-5]
                if base not in seen:
                    seen.add(base)
                    dyn_names.append(base)
    dyn_names.sort()

    dyn_rows = []
    for name in dyn_names:
        dyn_rows.append((f"{name}__avg", f"{name} 평균", False, fmt_ms))
        dyn_rows.append((f"{name}__p95", f"{name} p95", False, fmt_ms))

    all_rows = COMPARE_FIXED_ROWS + dyn_rows

    header = "".join(f"<th>{html.escape(l)}</th>" for l in run_labels)
    body_rows = []
    for key, label, higher_is_better, fmt in all_rows:
        baseline_val = baseline_stats.get(key)
        cells = []
        for i, stats in enumerate(stats_list):
            val = stats.get(key)
            if i == 0:
                cells.append(f"<td>{fmt(val) if val is not None else '-'}</td>")
            else:
                cells.append(f"<td>{compare_cell(val, baseline_val, higher_is_better, fmt)}</td>")
        body_rows.append(f"<tr><td>{html.escape(label)}</td>{''.join(cells)}</tr>")

    pass_badge = '<span class="badge pass">합격</span>'
    fail_badge = '<span class="badge fail">불합격</span>'
    verdicts = "".join(
        f'<th>{pass_badge if s["overall_pass"] else fail_badge}</th>'
        for s in stats_list
    )

    summary_html = build_compare_summary(run_labels, stats_list)

    return f"""
<table>
<tr><th>지표</th>{header}</tr>
<tr><td>전체 판정</td>{verdicts}</tr>
{''.join(body_rows)}
</table>
<p class="meta">첫 번째 컬럼({html.escape(run_labels[0])})을 기준으로 비교합니다. 초록색은 개선, 빨간색은 악화입니다
(▲는 값 증가, ▼는 값 감소 — 지표 성격에 따라 증가가 개선일 수도 있습니다). 2% 미만 변화는 표시하지 않습니다.</p>
{summary_html}
"""


def parse_input_arg(raw):
    """Accept either 'path.json' (label inferred from filename) or 'label=path.json'."""
    if "=" in raw:
        label, path = raw.split("=", 1)
        return label, Path(path)
    path = Path(raw)
    label = re.sub(r"-summary$", "", path.stem)
    return label, path


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--input", required=True, nargs="+",
        help="one or more k6 --summary-export JSON paths, e.g. 'smoke=k6/reports/smoke-summary.json' "
             "or just a path (label inferred from filename)",
    )
    parser.add_argument(
        "--mode", choices=["sections", "compare"], default="sections",
        help="sections: one section per input (different test types). "
             "compare: one comparison table across inputs (same test over time, first input = baseline).",
    )
    parser.add_argument("--output", required=True, help="output HTML path")
    parser.add_argument("--title", default="k6 성능 테스트 리포트", help="report title")
    args = parser.parse_args()

    labeled_data = []
    for raw in args.input:
        label, path = parse_input_arg(raw)
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        labeled_data.append((label, data))

    if args.mode == "compare":
        body = render_compare_doc(labeled_data, args.title)
    else:
        body = render_sections_doc(labeled_data, args.title)

    doc = f"""<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>{html.escape(args.title)}</title>
<style>{STYLE}</style>
</head>
<body>

<h1>{html.escape(args.title)}</h1>
<p class="meta">생성 시각: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>

{body}

<div class="footer">
    원본 데이터: k6 <code>--summary-export</code> JSON 기반으로 자동 생성됨
</div>

</body>
</html>
"""

    with open(args.output, "w", encoding="utf-8") as f:
        f.write(doc)

    print(f"리포트 생성 완료: {args.output} ({args.mode} 모드, {len(labeled_data)}개 입력)")


if __name__ == "__main__":
    sys.exit(main())
