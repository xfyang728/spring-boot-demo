package com.example.springbootdemo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class SchemaDiscoveryService {

    private final JdbcTemplate jdbcTemplate;
    private Map<String, List<ColumnInfo>> tableSchemaCache = new HashMap<>();
    private long lastCacheTime = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    public SchemaDiscoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        refreshSchemaCache();
    }

    public void refreshSchemaCache() {
        tableSchemaCache.clear();
        List<String> tables = discoverTables();
        for (String table : tables) {
            tableSchemaCache.put(table, discoverColumns(table));
        }
        lastCacheTime = System.currentTimeMillis();
    }

    public List<String> discoverTables() {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT table_name FROM information_schema.tables " +
                     "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : results) {
                tables.add((String) row.get("table_name"));
            }
        } catch (Exception e) {
            System.err.println("Failed to discover tables: " + e.getMessage());
            tables.add("car_sale");
        }
        return tables;
    }

    public List<ColumnInfo> discoverColumns(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        String sql = "SELECT " +
                     "  c.column_name, " +
                     "  c.data_type, " +
                     "  c.character_maximum_length, " +
                     "  c.numeric_precision, " +
                     "  c.numeric_scale, " +
                     "  c.is_nullable, " +
                     "  col_description((SELECT oid FROM pg_class WHERE relname = ?), " +
                     "    (SELECT attnum FROM pg_attribute WHERE attrelid = (SELECT oid FROM pg_class WHERE relname = ?) AND attname = c.column_name)) as column_description " +
                     "FROM information_schema.columns c " +
                     "WHERE c.table_schema = 'public' AND c.table_name = ? " +
                     "ORDER BY c.ordinal_position";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, tableName, tableName, tableName);
            for (Map<String, Object> row : results) {
                ColumnInfo info = new ColumnInfo();
                info.setName((String) row.get("column_name"));
                info.setDataType(buildDataTypeDescription(row));
                info.setDescription(buildDescription(row));
                columns.add(info);
            }
        } catch (Exception e) {
            System.err.println("Failed to discover columns for table " + tableName + ": " + e.getMessage());
            columns = getDefaultColumns();
        }
        return columns;
    }

    private String buildDataTypeDescription(Map<String, Object> row) {
        String dataType = (String) row.get("data_type");
        Object maxLength = row.get("character_maximum_length");
        Object precision = row.get("numeric_precision");
        Object scale = row.get("numeric_scale");

        if (maxLength != null) {
            return dataType + "(" + maxLength + ")";
        } else if (precision != null) {
            String prec = precision.toString();
            if (scale != null && !"0".equals(scale.toString())) {
                return dataType + "(" + prec + "," + scale + ")";
            }
            return dataType + "(" + prec + ")";
        }
        return dataType;
    }

    private String buildDescription(Map<String, Object> row) {
        String columnName = ((String) row.get("column_name")).toLowerCase();
        String dataType = (String) row.get("data_type");
        StringBuilder desc = new StringBuilder();

        if ("province".equals(columnName)) {
            desc.append("省份/地区");
        } else if ("date".equals(columnName) || "day".equals(columnName)) {
            desc.append("日期 (YYYY-MM-DD HH:MI:SS格式)");
        } else if ("value".equals(columnName) || "amount".equals(columnName) || "sales".equals(columnName)) {
            desc.append("金额/销售额");
        } else if ("count".equals(columnName) || "num".equals(columnName) || "quantity".equals(columnName)) {
            desc.append("数量");
        } else if ("price".equals(columnName)) {
            desc.append("价格");
        } else if ("discount".equals(columnName)) {
            desc.append("折扣率");
        } else if ("config".equals(columnName) || "configuration".equals(columnName)) {
            desc.append("配置 (高配/中配/低配)");
        } else if ("dealer".equals(columnName)) {
            desc.append("经销商/经销商名称");
        } else if ("type".equals(columnName) || "model".equals(columnName) || "car_model".equals(columnName)) {
            desc.append("车型/商品类型");
        } else if ("buy".equals(columnName) || "insurance".equals(columnName)) {
            desc.append("是否购买 (是/否)");
        } else if ("id".equals(columnName) || "order_id".equals(columnName)) {
            desc.append("唯一标识ID");
        } else if ("name".equals(columnName)) {
            desc.append("名称/姓名");
        } else if ("email".equals(columnName)) {
            desc.append("邮箱");
        } else if ("status".equals(columnName)) {
            desc.append("状态");
        } else if ("created_at".equals(columnName) || "updated_at".equals(columnName)) {
            desc.append("创建/更新时间");
        } else if ("character varying".equals(dataType)) {
            desc.append("字符串");
        } else if ("integer".equals(dataType) || "bigint".equals(dataType)) {
            desc.append("整数");
        } else if ("numeric".equals(dataType) || "real".equals(dataType) || "double precision".equals(dataType)) {
            desc.append("数值");
        } else if ("date".equals(dataType)) {
            desc.append("日期");
        } else if ("timestamp without time zone".equals(dataType) || "timestamp with time zone".equals(dataType)) {
            desc.append("日期时间");
        } else if ("boolean".equals(dataType)) {
            desc.append("布尔值");
        } else {
            desc.append(dataType);
        }

        String nullable = (String) row.get("is_nullable");
        if ("YES".equals(nullable)) {
            desc.append(", 可空");
        }

        return desc.toString();
    }

    private List<ColumnInfo> getDefaultColumns() {
        List<ColumnInfo> columns = new ArrayList<>();
        String[] names = {"日期", "订单id", "车型", "配置", "是否购买车险", "经销商", "省份", "订单数量", "销售额", "折扣"};
        String[] types = {"varchar(19)", "varchar(50)", "varchar(100)", "varchar(10)", "varchar(2)", "varchar(100)", "varchar(50)", "int4", "numeric(10,2)", "numeric(5,2)"};
        String[] descs = {"订单日期 YYYY-MM-DD HH:MI:SS", "唯一订单标识", "汽车型号", "车型配置: 高配/中配/低配", "是/否", "经销商名称", "省份名称", "订单数量", "销售金额", "折扣率"};

        for (int i = 0; i < names.length; i++) {
            ColumnInfo info = new ColumnInfo();
            info.setName(names[i]);
            info.setDataType(types[i]);
            info.setDescription(descs[i]);
            columns.add(info);
        }
        return columns;
    }

    public String getSchemaDescription() {
        if (System.currentTimeMillis() - lastCacheTime > CACHE_TTL_MS) {
            refreshSchemaCache();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("数据库信息：\n");

        for (Map.Entry<String, List<ColumnInfo>> entry : tableSchemaCache.entrySet()) {
            String tableName = entry.getKey();
            List<ColumnInfo> columns = entry.getValue();

            sb.append("- 表名: ").append(tableName).append("\n");
            sb.append("- 字段定义:\n");

            for (ColumnInfo col : columns) {
                sb.append("  * ")
                  .append(col.getName())
                  .append(" (")
                  .append(col.getDataType())
                  .append(", 描述: ")
                  .append(col.getDescription())
                  .append(")\n");
            }
        }
        return sb.toString();
    }

    public String getTableName() {
        if (tableSchemaCache.isEmpty()) {
            return "car_sale";
        }
        return tableSchemaCache.keySet().iterator().next();
    }

    public static class ColumnInfo {
        private String name;
        private String dataType;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}