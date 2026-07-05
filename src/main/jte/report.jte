@import com.audaque.model.DataSummary
@import com.audaque.model.ColumnStat
@import java.util.Map

@param DataSummary summary
@param String analysisText

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>数据分析报告</title>
    <style>
        :root {
            --bg: #f5f7fa;
            --card-bg: #ffffff;
            --text: #2c3e50;
            --text-secondary: #7f8c8d;
            --accent: #3498db;
            --accent-light: #eaf4fd;
            --success: #27ae60;
            --success-bg: #eafaf1;
            --warning: #e67e22;
            --warning-bg: #fef5e7;
            --danger: #e74c3c;
            --danger-bg: #fdedec;
            --border: #e1e8ed;
            --shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            --radius: 8px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans SC", sans-serif;
            background: var(--bg);
            color: var(--text);
            line-height: 1.6;
            padding: 24px;
        }

        .container {
            max-width: 1100px;
            margin: 0 auto;
        }

        h1 {
            font-size: 28px;
            font-weight: 700;
            color: var(--text);
            margin-bottom: 8px;
        }

        .subtitle {
            color: var(--text-secondary);
            font-size: 14px;
            margin-bottom: 28px;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 16px;
            margin-bottom: 28px;
        }

        .stat-card {
            background: var(--card-bg);
            border-radius: var(--radius);
            padding: 20px;
            box-shadow: var(--shadow);
            border-left: 4px solid var(--accent);
        }

        .stat-card .stat-label {
            font-size: 13px;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 6px;
        }

        .stat-card .stat-value {
            font-size: 28px;
            font-weight: 700;
            color: var(--text);
        }

        .section {
            background: var(--card-bg);
            border-radius: var(--radius);
            box-shadow: var(--shadow);
            padding: 24px;
            margin-bottom: 24px;
        }

        .section h2 {
            font-size: 20px;
            font-weight: 600;
            margin-bottom: 16px;
            padding-bottom: 10px;
            border-bottom: 2px solid var(--accent-light);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 14px;
        }

        thead th {
            background: var(--accent-light);
            color: var(--accent);
            font-weight: 600;
            text-align: left;
            padding: 12px 14px;
            border-bottom: 2px solid var(--accent);
            white-space: nowrap;
        }

        thead th:first-child {
            border-radius: var(--radius) 0 0 0;
        }

        thead th:last-child {
            border-radius: 0 var(--radius) 0 0;
        }

        tbody td {
            padding: 10px 14px;
            border-bottom: 1px solid var(--border);
        }

        tbody tr:hover {
            background: var(--bg);
        }

        tbody tr:last-child td {
            border-bottom: none;
        }

        .badge {
            display: inline-block;
            padding: 3px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
        }

        .badge-success {
            background: var(--success-bg);
            color: var(--success);
        }

        .badge-warning {
            background: var(--warning-bg);
            color: var(--warning);
        }

        .badge-danger {
            background: var(--danger-bg);
            color: var(--danger);
        }

        .quality-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 0;
            border-bottom: 1px solid var(--border);
        }

        .quality-item:last-child {
            border-bottom: none;
        }

        .quality-label {
            font-weight: 500;
        }

        .analysis-text {
            white-space: pre-wrap;
            font-size: 14px;
            line-height: 1.8;
            color: var(--text);
            background: var(--bg);
            padding: 18px;
            border-radius: var(--radius);
            border-left: 4px solid var(--accent);
        }

        footer {
            text-align: center;
            color: var(--text-secondary);
            font-size: 12px;
            margin-top: 32px;
            padding-top: 16px;
            border-top: 1px solid var(--border);
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>数据分析报告</h1>
        <p class="subtitle">生成时间：${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}</p>

        <section class="section">
            <h2>数据总览</h2>
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-label">总行数</div>
                    <div class="stat-value">${summary.totalRows()}</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">有效行数</div>
                    <div class="stat-value">${summary.validRows()}</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">空行数</div>
                    <div class="stat-value">${summary.emptyRows()}</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">总列数</div>
                    <div class="stat-value">${summary.columnCount()}</div>
                </div>
                <div class="stat-card">
                    <div class="stat-label">列名</div>
                    <div class="stat-value" style="font-size:14px;word-break:break-all;">${String.join(", ", summary.columnNames())}</div>
                </div>
            </div>
        </section>

        <section class="section">
            <h2>各列统计信息</h2>
            @if(summary.columnStats().isEmpty())
                <p style="color:var(--text-secondary);">无列统计信息。</p>
            @else
                <div style="overflow-x:auto;">
                    <table>
                        <thead>
                            <tr>
                                <th>列名</th>
                                <th>有效值数</th>
                                <th>均值</th>
                                <th>中位数</th>
                                <th>标准差</th>
                                <th>最小值</th>
                                <th>最大值</th>
                                <th>缺失值数</th>
                                <th>异常值数</th>
                            </tr>
                        </thead>
                        <tbody>
                            @for(var entry : summary.columnStats().entrySet())
                                <tr>
                                    <td><strong>${entry.getKey()}</strong></td>
                                    <td>${entry.getValue().count()}</td>
                                    <td>${String.format("%.4f", entry.getValue().mean())}</td>
                                    <td>${String.format("%.4f", entry.getValue().median())}</td>
                                    <td>${String.format("%.4f", entry.getValue().stdDev())}</td>
                                    <td>${String.format("%.4f", entry.getValue().min())}</td>
                                    <td>${String.format("%.4f", entry.getValue().max())}</td>
                                    <td>
                                        @if(entry.getValue().missingCount() > 0)
                                            <span class="badge @if(((double) entry.getValue().missingCount() / (entry.getValue().count() + entry.getValue().missingCount()) * 100) > 10) badge-warning @else badge-success @endif">${entry.getValue().missingCount()} (${String.format("%.1f", (double) entry.getValue().missingCount() / (entry.getValue().count() + entry.getValue().missingCount()) * 100)}%)</span>
                                        @else
                                            <span class="badge badge-success">0</span>
                                        @endif
                                    </td>
                                    <td>
                                        @if(entry.getValue().outlierCount() > 0)
                                            <span class="badge badge-danger">${entry.getValue().outlierCount()}</span>
                                        @else
                                            <span class="badge badge-success">0</span>
                                        @endif
                                    </td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
            @endif
        </section>

        !{var emptyRatio = summary.totalRows() > 0 ? (double) summary.emptyRows() / summary.totalRows() * 100 : 0;}
        !{var validRatio = summary.totalRows() > 0 ? (double) summary.validRows() / summary.totalRows() * 100 : 0;}

        <section class="section">
            <h2>数据质量报告</h2>
            <div class="quality-item">
                <span class="quality-label">空行占比</span>
                <span>
                    <span class="badge @if(emptyRatio < 5) badge-success @elseif(emptyRatio < 15) badge-warning @else badge-danger @endif">@if(emptyRatio < 5) 良好 @elseif(emptyRatio < 15) 一般 @else 较差 @endif</span>
                    <span style="margin-left:8px;color:var(--text-secondary);">${String.format("%.2f", emptyRatio)}% (${summary.emptyRows()}/${summary.totalRows()})</span>
                </span>
            </div>
            <div class="quality-item">
                <span class="quality-label">有效数据占比</span>
                <span>
                    <span class="badge @if(validRatio > 95) badge-success @elseif(validRatio > 80) badge-warning @else badge-danger @endif">@if(validRatio > 95) 良好 @elseif(validRatio > 80) 一般 @else 较差 @endif</span>
                    <span style="margin-left:8px;color:var(--text-secondary);">${String.format("%.2f", validRatio)}% (${summary.validRows()}/${summary.totalRows()})</span>
                </span>
            </div>

            <div class="quality-item">
                <span class="quality-label">缺失值总数</span>
                <span>
                    @if(summary.columnStats().values().stream().mapToLong(v -> v.missingCount()).sum() > 0)
                        <span class="badge badge-warning">${summary.columnStats().values().stream().mapToLong(v -> v.missingCount()).sum()}</span>
                    @else
                        <span class="badge badge-success">无缺失</span>
                    @endif
                </span>
            </div>
            <div class="quality-item">
                <span class="quality-label">异常值总数</span>
                <span>
                    @if(summary.columnStats().values().stream().mapToLong(v -> v.outlierCount()).sum() > 0)
                        <span class="badge badge-danger">${summary.columnStats().values().stream().mapToLong(v -> v.outlierCount()).sum()}</span>
                    @else
                        <span class="badge badge-success">无异常值</span>
                    @endif
                </span>
            </div>
        </section>

        <section class="section">
            <h2>分析结论</h2>
            @if(analysisText == null || analysisText.isEmpty())
                <p style="color:var(--text-secondary);">暂无分析结论。</p>
            @else
                <div class="analysis-text">$unsafe{analysisText}</div>
            @endif
        </section>

        <footer>
            <p>本报告由数据分析自动报告生成工具自动生成</p>
        </footer>
    </div>
</body>
</html>
