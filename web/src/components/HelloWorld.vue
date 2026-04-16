<template>
  <h1>AI生成SQL和图表</h1>
  <button @click="add">count is: {{ count }}</button>
  <p>Edit <code>components/HelloWorld.vue</code> to test hot module replacement.</p>

  <textarea
    id="input"
    rows="1"
    placeholder="输入你的问题或需求"
    class="scroll-display-none"
    style="height: 55px; width: 600px"
    v-model="msg"
  ></textarea>
  <button @click="generateSql" style="height: 25px">generate</button>
  <br />
  <textarea
    id="sqltext"
    class="scroll-display-none"
    style="height: 90px; width: 800px"
    v-model="sql"
  ></textarea>
  <button @click="queryData" style="height: 25px">queryData</button>
  <br />
  <textarea
    id="datatext"
    class="scroll-display-none"
    style="height: 90px; width: 800px"
    v-model="datatext"
  ></textarea>
  <button @click="showChart" style="height: 25px">showChart</button>
  <br />
  <div id="chart" class="echarts"></div>
</template>

<script setup>
import { reactive, onMounted, ref } from "vue";
import axios from "axios";
import * as echarts from "echarts";

let msg = ref(
    "表名:car_sale; 字段定义:date(日期) id(订单id) type() config buy dealer province count(订单数量) value discount 日期 订单id 依据字段定义生成各天订单数量的查询sql,只输出一句sql"
  ),
  sql = ref("Hello World!"),
  dataResp = ref("datatext!"),
  datatext = ref("datatext!"),
  count = ref(0);

function add() {
  count.value++;
}

async function askLlama(val) {
  const data = {
    model: "llama3.2:3b",
    stream: false,
    messages: [
      {
        role: "system",
        content:
          "你是一个SQL生成工具，你的任务是为用户提供postgresql的sql建议,只输出sql语句。",
      },
      { role: "user", content: val },
    ],
  };
  try {
    const ret = await axios.post("http://localhost:11434/api/chat", data, {
      headers: {
        "Content-Type": "application/json",
      },
    });
    const response = ret.data;
    if (response && response.message) {
      sql.value = response.message.content;
    } else if (response && response.error) {
      sql.value = "Error: " + response.error;
    }
  } catch (err) {
    console.error("API Error:", err);
    sql.value = "Error: " + err.message;
  }
}

function generateSql() {
  askLlama(msg.value);
}

async function dbQuery(querySql) {
  const data = {
    result: querySql,
  };
  const ret = await axios.post("http://127.0.0.1:7878/ai/query", data, {
    headers: {
      "Content-Type": "application/json",
    },
  });
  const response = ret.data;
  dataResp.value = response;
  datatext.value = JSON.stringify(response);
}

function queryData() {
  dbQuery(sql.value);
}

function showChart() {
  var chartDom = document.getElementById("chart");
  var myChart = echarts.init(chartDom);
  var option;

  option = {
    legend: {},
    tooltip: {},
    dataset: {
      // 提供一份数据。
      source: dataResp.value,
    },
    // 声明一个 X 轴，类目轴（category）。默认情况下，类目轴对应到 dataset 第一列。
    xAxis: { type: 'category' },
    // 声明一个 Y 轴，数值轴。
    yAxis: {},
    // 声明多个 bar 系列，默认情况下，每个系列会自动对应到 dataset 的每一列。
    series: { type: "bar" },
  };

  option && myChart.setOption(option);
}

// 这个要在页面载入时执行，无需 `return` 出去
const init = () => {
  console.log("init");
};

onMounted(() => {
  init();
});
</script>
<style lang="css" scoped>
.echarts {
  width: 100vw;
  height: 50vw;
}
</style>
