<template>
  <div class="ai-sql-app">
    <header class="app-header">
      <div class="header-left">
        <h1>🤖 AI SQL 查询助手</h1>
      </div>
      <div class="header-right">
        <span class="status-indicator" :class="{ online: isBackendOnline }"></span>
        <span class="status-text">{{ isBackendOnline ? '服务正常' : '服务离线' }}</span>
      </div>
    </header>

    <main class="app-main">
      <aside class="left-panel">
        <section class="panel ai-input-section">
          <h2>自然语言查询</h2>
          <textarea
            v-model="userQuestion"
            placeholder="例如：查询每天的订单数量和销售额"
            class="question-input"
            rows="4"
          ></textarea>
          <button @click="generateSql" class="btn btn-primary" :disabled="isGenerating">
            <span v-if="isGenerating">生成中...</span>
            <span v-else>✨ 生成 SQL</span>
          </button>
        </section>

        <section class="panel quick-actions">
          <h3>快捷查询</h3>
          <div class="quick-buttons">
            <button @click="applyQuickQuery('daily_orders')" class="quick-btn">每日订单</button>
            <button @click="applyQuickQuery('monthly_sales')" class="quick-btn">月度销售</button>
            <button @click="applyQuickQuery('province_stats')" class="quick-btn">省份统计</button>
            <button @click="applyQuickQuery('dealer_ranking')" class="quick-btn">经销商排行</button>
          </div>
        </section>

        <section class="panel chart-recommendation" v-if="chartRecommendation">
          <h3>📊 智能图表推荐</h3>
          <div class="recommendation-info">
            <span class="chart-type-badge">{{ chartRecommendation.type }}</span>
            <span class="chart-title">{{ chartRecommendation.title }}</span>
          </div>
          <div class="chart-type-selector">
            <button v-for="t in chartTypes" :key="t" :class="{ active: selectedChartType === t }" @click="changeChartType(t)">{{ t }}</button>
          </div>
        </section>
      </aside>

      <section class="center-panel">
        <div class="panel sql-editor-panel">
          <div class="panel-header">
            <h2>SQL 编辑器</h2>
            <div class="editor-toolbar">
              <button @click="formatSql" class="toolbar-btn" title="格式化">🎨 格式化</button>
              <button @click="validateSql" class="toolbar-btn" title="验证">✓ 验证</button>
              <button @click="copySql" class="toolbar-btn" title="复制">📋 复制</button>
            </div>
          </div>
          <textarea
            v-model="sql"
            class="sql-editor"
            placeholder="SELECT * FROM car_sale WHERE ..."
            spellcheck="false"
          ></textarea>
          <div v-if="validationResult" class="validation-result" :class="{ valid: validationResult.valid, invalid: !validationResult.valid }">
            <span v-if="validationResult.valid">✅ SQL 验证通过</span>
            <span v-else>❌ {{ validationResult.errors?.join(', ') }}</span>
          </div>
        </div>

        <div class="action-bar">
          <button @click="executeQuery" class="btn btn-execute" :disabled="!sql || isExecuting">
            <span v-if="isExecuting">执行中...</span>
            <span v-else>▶ 执行查询</span>
          </button>
          <span v-if="executionTime" class="execution-time">⏱ {{ executionTime }}ms</span>
          <span v-if="rowCount" class="row-count">📊 {{ rowCount }} 条记录</span>
        </div>

        <div class="panel results-panel">
          <div class="panel-header">
            <h2>查询结果</h2>
            <div class="view-toggle">
              <button @click="viewMode = 'table'" :class="{ active: viewMode === 'table' }">表格</button>
              <button @click="viewMode = 'chart'" :class="{ active: viewMode === 'chart' }">图表</button>
            </div>
          </div>

          <div v-if="viewMode === 'table'" class="results-table-container">
            <table v-if="queryResults.length" class="results-table">
              <thead>
                <tr>
                  <th v-for="col in resultColumns" :key="col">{{ col }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in queryResults" :key="idx">
                  <td v-for="col in resultColumns" :key="col">{{ row[col] }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="no-data">暂无数据</div>
          </div>

          <div v-if="viewMode === 'chart'" class="chart-container">
            <div id="chart" ref="chartRef" class="echarts-instance"></div>
          </div>
        </div>
      </section>

      <aside class="right-panel">
        <section class="panel logs-panel">
          <h3>📜 执行日志</h3>
          <div class="logs-list">
            <div v-for="(log, idx) in recentLogs" :key="idx" class="log-item" :class="(log.status || 'UNKNOWN').toLowerCase()">
              <span class="log-status">{{ log.status || 'UNKNOWN' }}</span>
              <span class="log-sql">{{ log.sqlText ? log.sqlText.substring(0, 30) : 'N/A' }}...</span>
              <span class="log-time">{{ log.executionTimeMs || 0 }}ms</span>
            </div>
            <div v-if="!recentLogs.length" class="no-logs">暂无日志</div>
          </div>
        </section>

        <section class="panel stats-panel">
          <h3>📈 统计信息</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalExecutions || 0 }}</span>
              <span class="stat-label">总执行次数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.errorRate ? (stats.errorRate * 100).toFixed(1) + '%' : '0%' }}</span>
              <span class="stat-label">错误率</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.avgExecutionTimeMs || 0 }}ms</span>
              <span class="stat-label">平均耗时</span>
            </div>
          </div>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed, watch } from "vue";
import axios from "axios";
import * as echarts from "echarts";

const userQuestion = ref("");
const sql = ref("");
const isGenerating = ref(false);
const isExecuting = ref(false);
const isBackendOnline = ref(false);
const viewMode = ref("table");
const chartRef = ref(null);

const queryResults = ref([]);
const chartRecommendation = ref(null);
const chartTypes = ["bar", "line", "pie"];
const selectedChartType = ref("bar");
const validationResult = ref(null);
const executionTime = ref(0);
const rowCount = ref(0);
const recentLogs = ref([]);
const stats = reactive({
  totalExecutions: 0,
  errorRate: 0,
  avgExecutionTimeMs: 0
});

let chartInstance = null;

const resultColumns = computed(() => {
  if (queryResults.value.length) {
    return Object.keys(queryResults.value[0]);
  }
  return [];
});

const quickQueries = {
  daily_orders: "查询每天的订单数量，按日期排序",
  monthly_sales: "按月统计销售额和订单数量",
  province_stats: "统计每个省份的销售总额",
  dealer_ranking: "查询销售额前10名的经销商"
};

async function checkBackendStatus() {
  try {
    await axios.post("http://localhost:7878/ai/validate", {
      sql: "SELECT 1"
    });
    isBackendOnline.value = true;
  } catch (e) {
    isBackendOnline.value = false;
  }
}

async function generateSql() {
  if (!userQuestion.value) return;
  isGenerating.value = true;
  try {
    const response = await axios.post("http://localhost:7878/ai/query", {
      result: userQuestion.value
    });
    if (response.data.success) {
      sql.value = response.data.data.sql;
      chartRecommendation.value = response.data.chartRecommendation;
      selectedChartType.value = chartRecommendation.value?.type || "bar";
    } else {
      alert("生成失败: " + response.data.error?.message);
    }
  } catch (e) {
    console.error("Generate error:", e);
    alert("请求失败: " + e.message);
  } finally {
    isGenerating.value = false;
  }
}

async function executeQuery() {
  if (!sql.value) return;
  isExecuting.value = true;
  executionTime.value = 0;
  rowCount.value = 0;
  try {
    const response = await axios.post("http://localhost:7878/ai/execute", {
      sql: sql.value
    });
    if (response.data.success) {
      queryResults.value = response.data.data.results || [];
      executionTime.value = response.data.data.executionTimeMs || 0;
      rowCount.value = response.data.data.rowCount || 0;
      chartRecommendation.value = response.data.chartRecommendation;
      selectedChartType.value = response.data.chartRecommendation?.type || "bar";
      if (viewMode.value === "chart") {
        await nextTick();
        renderChart();
      }
      await loadLogs();
      await loadStats();
    } else {
      alert("查询失败: " + response.data.error?.message);
    }
  } catch (e) {
    console.error("Query error:", e);
    alert("请求失败: " + e.message);
  } finally {
    isExecuting.value = false;
  }
}

async function validateSql() {
  if (!sql.value) return;
  try {
    const response = await axios.post("http://localhost:7878/ai/validate", {
      sql: sql.value
    });
    validationResult.value = response.data;
  } catch (e) {
    console.error("Validate error:", e);
  }
}

function formatSql() {
  let formatted = sql.value
    .replace(/\s+/g, " ")
    .replace(/SELECT/gi, "SELECT\n  ")
    .replace(/FROM/gi, "\nFROM")
    .replace(/WHERE/gi, "\nWHERE")
    .replace(/GROUP BY/gi, "\nGROUP BY")
    .replace(/ORDER BY/gi, "\nORDER BY")
    .replace(/LIMIT/gi, "\nLIMIT")
    .replace(/AND/gi, "\n  AND")
    .replace(/OR/gi, "\n  OR");
  sql.value = formatted.trim();
}

function copySql() {
  navigator.clipboard.writeText(sql.value);
}

function applyQuickQuery(key) {
  userQuestion.value = quickQueries[key];
}

function renderChart() {
  if (!chartRef.value) return;

  if (chartInstance) {
    chartInstance.dispose();
  }

  chartInstance = echarts.init(chartRef.value);
  
  const type = selectedChartType.value;
  const data = queryResults.value;
  const rec = chartRecommendation.value;

  if (!data || !data.length || !rec) {
    chartInstance.setOption({});
    return;
  }

  const config = {
    title: { text: rec.title }
  };

  if (type === "pie") {
    const pieData = data.map(row => ({
      name: row[rec.xAxis] || "未知",
      value: row[rec.yAxis]
    }));

    config.tooltip = { trigger: "item" };
    config.legend = { orient: "vertical", right: "right" };
    config.series = [{
      type: "pie",
      radius: "55%",
      center: ["40%", "50%"],
      data: pieData,
      label: {
        show: true,
        position: "outside",
        formatter: "{b}: {c}",
        color: "#333"
      },
      labelLine: {
        show: true,
        lineStyle: { color: "#999" },
        smooth: 0.2
      },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: "rgba(0, 0, 0, 0.5)" },
        label: { show: true }
      }
    }];
  } else {
    const xData = data.map(row => row[rec.xAxis]);
    const yData = data.map(row => row[rec.yAxis]);

    config.tooltip = { trigger: "axis" };
    config.xAxis = { type: "category", data: xData };
    config.yAxis = { type: "value" };
    config.grid = { left: "3%", right: "4%", bottom: "3%", containLabel: true };
    config.series = [{
      type: type,
      data: yData,
      smooth: type === "line"
    }];
  }

  chartInstance.setOption(config);
}

function changeChartType(type) {
  selectedChartType.value = type;
  renderChart();
}

async function loadLogs() {
  try {
    const response = await axios.get("http://localhost:7878/api/logs/recent", {
      params: { limit: 10 }
    });
    recentLogs.value = response.data || [];
  } catch (e) {
    console.error("Load logs error:", e);
  }
}

async function loadStats() {
  try {
    const response = await axios.get("http://localhost:7878/api/logs/statistics");
    Object.assign(stats, response.data);
  } catch (e) {
    console.error("Load stats error:", e);
  }
}

watch(viewMode, async (newMode) => {
  if (newMode === "chart" && chartRecommendation.value) {
    await nextTick();
    renderChart();
  }
});

onMounted(() => {
  checkBackendStatus();
  loadLogs();
  loadStats();
  setInterval(() => {
    checkBackendStatus();
    loadLogs();
    loadStats();
  }, 30000);
});
</script>

<style scoped>
.ai-sql-app {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.app-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ef4444;
}

.status-indicator.online {
  background: #22c55e;
}

.app-main {
  display: grid;
  grid-template-columns: 280px 1fr 260px;
  gap: 16px;
  padding: 16px;
  flex: 1;
  overflow: hidden;
}

.panel {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.panel h2, .panel h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #1f2937;
}

.panel h2 {
  font-size: 18px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-header h2 {
  margin: 0;
}

.left-panel, .right-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.question-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  margin-bottom: 12px;
  font-family: inherit;
}

.question-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  width: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-execute {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
  padding: 12px 32px;
  font-size: 16px;
}

.btn-execute:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(17, 153, 142, 0.4);
}

.quick-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.quick-btn {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-btn:hover {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.chart-recommendation {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.chart-recommendation h3 {
  color: white;
}

.recommendation-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chart-type-badge {
  display: inline-block;
  padding: 4px 8px;
  background: rgba(255,255,255,0.2);
  border-radius: 4px;
  font-size: 12px;
  text-transform: uppercase;
}

.chart-title {
  font-size: 14px;
  font-weight: 500;
}

.chart-type-selector {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.chart-type-selector button {
  padding: 6px 16px;
  border: 1px solid rgba(255,255,255,0.5);
  background: rgba(255,255,255,0.1);
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.chart-type-selector button.active {
  background: rgba(255,255,255,0.9);
  color: #f5576c;
  border-color: rgba(255,255,255,0.9);
}

.center-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.sql-editor-panel {
  flex-shrink: 0;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
}

.toolbar-btn {
  padding: 4px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f9fafb;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.sql-editor {
  width: 100%;
  min-height: 120px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  resize: vertical;
  background: #1e1e1e;
  color: #d4d4d4;
}

.sql-editor:focus {
  outline: none;
  border-color: #667eea;
}

.validation-result {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
}

.validation-result.valid {
  background: #d1fae5;
  color: #065f46;
}

.validation-result.invalid {
  background: #fee2e2;
  color: #991b1b;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 16px;
}

.execution-time, .row-count {
  font-size: 13px;
  color: #6b7280;
}

.results-panel {
  flex: 1;
  min-height: 300px;
  display: flex;
  flex-direction: column;
}

.view-toggle {
  display: flex;
  gap: 4px;
}

.view-toggle button {
  padding: 4px 12px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  font-size: 12px;
  cursor: pointer;
}

.view-toggle button:first-child {
  border-radius: 4px 0 0 4px;
}

.view-toggle button:last-child {
  border-radius: 0 4px 4px 0;
}

.view-toggle button.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.results-table-container {
  flex: 1;
  overflow: auto;
}

.results-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.results-table th, .results-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.results-table th {
  background: #f9fafb;
  font-weight: 600;
  color: #374151;
  position: sticky;
  top: 0;
}

.results-table tr:hover {
  background: #f9fafb;
}

.no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #9ca3af;
  font-size: 14px;
}

.chart-container {
  flex: 1;
  min-height: 300px;
}

.echarts-instance {
  width: 100%;
  height: 100%;
  min-height: 300px;
}

.logs-panel {
  flex: 1;
  overflow: hidden;
}

.logs-list {
  max-height: 200px;
  overflow-y: auto;
}

.log-item {
  display: grid;
  grid-template-columns: 60px 1fr 50px;
  gap: 8px;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 4px;
  font-size: 11px;
  background: #f9fafb;
}

.log-item.error {
  background: #fee2e2;
}

.log-item.success {
  background: #d1fae5;
}

.log-status {
  font-weight: 600;
}

.log-sql {
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-time {
  color: #9ca3af;
}

.no-logs {
  text-align: center;
  color: #9ca3af;
  padding: 20px;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
}
</style>
