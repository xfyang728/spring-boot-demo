<template>
  <div class="sql-editor">
    <div class="editor-header">
      <h3>SQL 编辑器</h3>
      <div class="editor-actions">
        <button @click="formatSql" class="btn btn-secondary">格式化</button>
        <button @click="validateSql" class="btn btn-secondary" :disabled="!sql">验证</button>
        <button @click="executeSql" class="btn btn-primary" :disabled="!sql">执行</button>
      </div>
    </div>

    <div class="editor-content">
      <textarea
        ref="editorRef"
        v-model="sql"
        @input="onSqlChange"
        @keydown.ctrl.enter="executeSql"
        class="sql-textarea"
        placeholder="输入或编辑 SQL 语句..."
        spellcheck="false"
      ></textarea>

      <div v-if="validationResult" class="validation-result">
        <div v-if="validationResult.valid" class="validation-success">
          ✅ SQL 验证通过
        </div>
        <div v-else class="validation-error">
          ❌ {{ validationResult.errors?.join(', ') }}
        </div>
        <div v-if="validationResult.warnings?.length" class="validation-warnings">
          ⚠️ {{ validationResult.warnings?.join(', ') }}
        </div>
        <div v-if="validationResult.suggestions?.length" class="validation-suggestions">
          💡 {{ validationResult.suggestions?.join(', ') }}
        </div>
      </div>
    </div>

    <div class="editor-footer">
      <span class="sql-length">字符数: {{ sql.length }}</span>
      <span class="sql-hint">Ctrl+Enter 执行</span>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'SqlEditor',
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'execute'],
  data() {
    return {
      sql: this.modelValue,
      validationResult: null,
      isValidating: false,
      isExecuting: false
    };
  },
  watch: {
    modelValue(val) {
      if (val !== this.sql) {
        this.sql = val;
      }
    }
  },
  methods: {
    onSqlChange() {
      this.$emit('update:modelValue', this.sql);
    },

    async validateSql() {
      if (!this.sql) return;

      this.isValidating = true;
      try {
        const response = await axios.post('http://localhost:7878/ai/validate', {
          sql: this.sql
        });
        this.validationResult = response.data;
        this.$emit('validation', this.validationResult);
      } catch (error) {
        this.validationResult = {
          valid: false,
          errors: [error.message]
        };
      } finally {
        this.isValidating = false;
      }
    },

    async executeSql() {
      if (!this.sql) return;

      this.isExecuting = true;
      try {
        await this.$emit('execute', this.sql);
      } finally {
        this.isExecuting = false;
      }
    },

    formatSql() {
      let formatted = this.sql
        .replace(/\s+/g, ' ')
        .replace(/SELECT/gi, 'SELECT\n  ')
        .replace(/FROM/gi, '\nFROM')
        .replace(/WHERE/gi, '\nWHERE')
        .replace(/GROUP BY/gi, '\nGROUP BY')
        .replace(/ORDER BY/gi, '\nORDER BY')
        .replace(/LIMIT/gi, '\nLIMIT')
        .replace(/AND/gi, '\n  AND')
        .replace(/OR/gi, '\n  OR');

      this.sql = formatted.trim();
      this.$emit('update:modelValue', this.sql);
    },

    setSql(sql) {
      this.sql = sql;
      this.$emit('update:modelValue', this.sql);
    },

    clear() {
      this.sql = '';
      this.validationResult = null;
      this.$emit('update:modelValue', '');
    }
  }
};
</script>

<style scoped>
.sql-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  font-family: 'Courier New', monospace;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #dcdfe6;
  background: #f5f7fa;
}

.editor-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #409eff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #66b1ff;
}

.btn-secondary {
  background: #e4e7ed;
  color: #606266;
}

.btn-secondary:hover:not(:disabled) {
  background: #ecf5ff;
  color: #409eff;
}

.editor-content {
  position: relative;
}

.sql-textarea {
  width: 100%;
  min-height: 200px;
  padding: 16px;
  border: none;
  resize: vertical;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  outline: none;
}

.sql-textarea::placeholder {
  color: #c0c4cc;
}

.validation-result {
  padding: 12px 16px;
  border-top: 1px solid #dcdfe6;
  font-size: 13px;
}

.validation-success {
  color: #67c23a;
  margin-bottom: 4px;
}

.validation-error {
  color: #f56c6c;
  margin-bottom: 4px;
}

.validation-warnings {
  color: #e6a23c;
  margin-bottom: 4px;
}

.validation-suggestions {
  color: #909399;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 1px solid #dcdfe6;
  background: #f5f7fa;
  font-size: 12px;
  color: #909399;
}
</style>
