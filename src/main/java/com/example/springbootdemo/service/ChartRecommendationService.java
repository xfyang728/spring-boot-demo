package com.example.springbootdemo.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChartRecommendationService {

    public ChartRecommendation recommend(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return new ChartRecommendation("empty", "无数据", null, null);
        }

        Map<String, Class<?>> columnTypes = analyzeColumnTypes(data.get(0));
        List<String> numericColumns = new ArrayList<>();
        List<String> dateColumns = new ArrayList<>();
        List<String> categoryColumns = new ArrayList<>();

        for (Map.Entry<String, Class<?>> entry : columnTypes.entrySet()) {
            String colName = entry.getKey();
            Class<?> type = entry.getValue();

            if (Number.class.isAssignableFrom(type)) {
                numericColumns.add(colName);
            } else if (isDateColumn(colName, data.get(0))) {
                dateColumns.add(colName);
            } else {
                categoryColumns.add(colName);
            }
        }

        int rowCount = data.size();

        if (!dateColumns.isEmpty() && !numericColumns.isEmpty()) {
            return recommendTimeSeriesChart(dateColumns.get(0), numericColumns.get(0), rowCount);
        }

        if (!categoryColumns.isEmpty() && !numericColumns.isEmpty()) {
            return recommendCategoryChart(categoryColumns.get(0), numericColumns.get(0), rowCount);
        }

        if (numericColumns.size() == 1 && rowCount <= 10) {
            return recommendPieChart(numericColumns.get(0), rowCount);
        }

        if (rowCount > 50) {
            return new ChartRecommendation("line", "趋势折线图", dateColumns.isEmpty() ? categoryColumns.get(0) : dateColumns.get(0), numericColumns.get(0));
        }

        return new ChartRecommendation("table", "数据表格", null, null);
    }

    private ChartRecommendation recommendTimeSeriesChart(String dateColumn, String numericColumn, int rowCount) {
        String chartType = rowCount > 50 ? "line" : "bar";
        String title = chartType.equals("line") ? "时间趋势图" : "时间对比图";

        return new ChartRecommendation(chartType, title, dateColumn, numericColumn);
    }

    private ChartRecommendation recommendCategoryChart(String categoryColumn, String numericColumn, int rowCount) {
        String chartType = rowCount <= 10 ? "bar" : "bar";
        String title = "分类对比图";

        return new ChartRecommendation(chartType, title, categoryColumn, numericColumn);
    }

    private ChartRecommendation recommendPieChart(String numericColumn, int rowCount) {
        return new ChartRecommendation("pie", "占比分布图", null, numericColumn);
    }

    private Map<String, Class<?>> analyzeColumnTypes(Map<String, Object> sampleRow) {
        Map<String, Class<?>> columnTypes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : sampleRow.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                columnTypes.put(entry.getKey(), Object.class);
            } else {
                columnTypes.put(entry.getKey(), getUnderlyingClass(value));
            }
        }

        return columnTypes;
    }

    private Class<?> getUnderlyingClass(Object value) {
        if (value instanceof Number) {
            if (value instanceof Double || value instanceof Float) {
                return Double.class;
            }
            return Integer.class;
        }
        return value.getClass();
    }

    private boolean isDateColumn(String columnName, Map<String, Object> sampleRow) {
        Object value = sampleRow.get(columnName);
        if (value == null) {
            return false;
        }

        String strValue = value.toString();
        return strValue.matches("\\d{4}-\\d{2}-\\d{2}.*") ||
               strValue.matches("\\d{4}/\\d{2}/\\d{2}.*") ||
               columnName.toLowerCase().contains("date") ||
               columnName.toLowerCase().contains("时间") ||
               columnName.toLowerCase().contains("日期");
    }

    public Map<String, Object> generateEChartsConfig(ChartRecommendation recommendation, List<Map<String, Object>> data) {
        Map<String, Object> config = new HashMap<>();

        if ("empty".equals(recommendation.getType())) {
            return config;
        }

        config.put("title", Map.of("text", recommendation.getTitle()));

        if ("pie".equals(recommendation.getType())) {
            config.put("tooltip", Map.of("trigger", "item"));
        } else {
            config.put("tooltip", Map.of("trigger", "axis"));
        }

        if ("line".equals(recommendation.getType()) || "bar".equals(recommendation.getType())) {
            config.put("xAxis", Map.of("type", "category", "data", extractAxisData(data, recommendation.getxAxis())));
            config.put("yAxis", Map.of("type", "value"));
            config.put("series", List.of(
                Map.of(
                    "type", recommendation.getType(),
                    "data", extractSeriesData(data, recommendation.getxAxis(), recommendation.getyAxis()),
                    "smooth", true
                )
            ));
        } else if ("pie".equals(recommendation.getType())) {
            config.put("legend", Map.of("orient", "vertical", "right", "right"));
            config.put("series", List.of(
                Map.of(
                    "type", "pie",
                    "radius", "55%",
                    "center", List.of("40%", "50%"),
                    "data", extractPieData(data, recommendation.getxAxis(), recommendation.getyAxis()),
                    "label", Map.of(
                        "show", true,
                        "position", "outside",
                        "formatter", "{b}: {c}",
                        "color", "#333"
                    ),
                    "labelLine", Map.of(
                        "show", true,
                        "lineStyle", Map.of("color", "#999"),
                        "smooth", 0.2
                    ),
                    "emphasis", Map.of(
                        "itemStyle", Map.of("shadowBlur", 10, "shadowOffsetX", 0, "shadowColor", "rgba(0, 0, 0, 0.5)"),
                        "label", Map.of("show", true)
                    )
                )
            ));
        }

        config.put("grid", Map.of("left", "3%", "right", "4%", "bottom", "3%", "containLabel", true));

        return config;
    }

    private List<Object> extractAxisData(List<Map<String, Object>> data, String xAxisColumn) {
        List<Object> axisData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            if (xAxisColumn != null) {
                axisData.add(row.get(xAxisColumn));
            }
        }
        return axisData;
    }

    private List<Object> extractSeriesData(List<Map<String, Object>> data, String xAxisColumn, String yAxisColumn) {
        List<Object> seriesData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            if (yAxisColumn != null) {
                Object value = row.get(yAxisColumn);
                if (value instanceof Number) {
                    seriesData.add(((Number) value).doubleValue());
                } else {
                    seriesData.add(value);
                }
            }
        }
        return seriesData;
    }

    private List<Map<String, Object>> extractPieData(List<Map<String, Object>> data, String nameColumn, String valueColumn) {
        List<Map<String, Object>> pieData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Object value = row.get(valueColumn);
            Object name = nameColumn != null ? row.get(nameColumn) : "未知";

            pieData.add(Map.of(
                "name", name,
                "value", value instanceof Number ? ((Number) value).doubleValue() : value
            ));
        }
        return pieData;
    }

    public static class ChartRecommendation {
        private String type;
        private String title;
        private String xAxis;
        private String yAxis;

        public ChartRecommendation(String type, String title, String xAxis, String yAxis) {
            this.type = type;
            this.title = title;
            this.xAxis = xAxis;
            this.yAxis = yAxis;
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getxAxis() {
            return xAxis;
        }

        public String getyAxis() {
            return yAxis;
        }
    }
}
