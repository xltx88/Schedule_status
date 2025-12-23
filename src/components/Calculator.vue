<template>
  <div class="calculator-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">资料分析计算器</span>
          <el-button type="primary" link @click="clearAll">清空重置</el-button>
        </div>
      </template>
      
      <div class="calc-content">
        <div class="calc-row">
          <div class="label-col">
            <span class="label">基期量 (A)</span>
            <span class="dynamic-val" v-if="finalValues.base !== null">{{ formatNumber(finalValues.base) }}</span>
          </div>
          <el-input v-model="inputs.base" placeholder="输入数值或算式，如 550+50" clearable>
          </el-input>
        </div>

        <div class="calc-row">
          <div class="label-col">
            <span class="label">现期量 (B)</span>
            <span class="dynamic-val" v-if="finalValues.current !== null">{{ formatNumber(finalValues.current) }}</span>
          </div>
          <el-input v-model="inputs.current" placeholder="输入数值或算式" clearable>
          </el-input>
        </div>

        <div class="calc-row">
          <div class="label-col">
            <span class="label">增长率 (R)</span>
            <span class="dynamic-val" v-if="finalValues.rate !== null">{{ formatNumber(finalValues.rate) }}%</span>
          </div>
          <el-input v-model="inputs.rate" placeholder="输入百分比数值，如 15 代表 15%" clearable>
          </el-input>
        </div>

        <div class="calc-row">
          <div class="label-col">
            <span class="label">增长量 (X)</span>
            <span class="dynamic-val" v-if="finalValues.growth !== null">{{ formatNumber(finalValues.growth) }}</span>
          </div>
          <el-input v-model="inputs.growth" placeholder="输入数值或算式" clearable>
          </el-input>
        </div>
      </div>
      
      <div class="tips">
        <el-alert
          title="使用说明"
          type="info"
          :closable="false"
          description="1. 支持加减乘除运算，例如在基期量输入 '550+50'。
2. 任意输入两个变量，系统将自动计算其余两个变量。
3. 增长率请输入百分比前的数值，例如 15.5 代表 15.5%。"
          show-icon
        />
      </div>
    </el-card>

    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span class="title">比重计算器</span>
          <el-button type="primary" link @click="clearProportion">清空重置</el-button>
        </div>
      </template>

      <div class="section-title">1. 基础比重计算 (输入任意两项求第三项)</div>
      <div class="calc-content">
        <div class="calc-row">
          <div class="label-col">
            <span class="label">部分 (A)</span>
            <span class="dynamic-val" v-if="finalPropValues.part !== null">{{ formatNumber(finalPropValues.part) }}</span>
          </div>
          <el-input v-model="propInputs.part" placeholder="输入数值" clearable />
        </div>
        <div class="calc-row">
          <div class="label-col">
            <span class="label">总量 (B)</span>
            <span class="dynamic-val" v-if="finalPropValues.total !== null">{{ formatNumber(finalPropValues.total) }}</span>
          </div>
          <el-input v-model="propInputs.total" placeholder="输入数值" clearable />
        </div>
        <div class="calc-row">
          <div class="label-col">
            <span class="label">占比 (P%)</span>
            <span class="dynamic-val" v-if="finalPropValues.val !== null">{{ formatNumber(finalPropValues.val) }}%</span>
          </div>
          <el-input v-model="propInputs.val" placeholder="输入百分比，如 25" clearable />
        </div>
      </div>

      <el-divider />

      <div class="section-title">2. 基期比重与趋势 (输入现期数据与增长率)</div>
      <div class="calc-content">
        <div class="calc-row-group">
          <div class="calc-row half">
            <div class="label-col-small">现期部分</div>
            <el-input v-model="basePropInputs.part" placeholder="数值" />
          </div>
          <div class="calc-row half">
            <div class="label-col-small">现期总量</div>
            <el-input v-model="basePropInputs.total" placeholder="数值" />
          </div>
        </div>
        <div class="calc-row-group">
          <div class="calc-row half">
            <div class="label-col-small">部分增长率%</div>
            <el-input v-model="basePropInputs.partRate" placeholder="如 15" />
          </div>
          <div class="calc-row half">
            <div class="label-col-small">总量增长率%</div>
            <el-input v-model="basePropInputs.totalRate" placeholder="如 10" />
          </div>
        </div>
        
        <div class="result-box" v-if="basePropResult">
          <div class="result-item">
            <span class="label">基期比重:</span>
            <span class="value">{{ formatNumber(basePropResult.value) }}%</span>
          </div>
          <div class="result-item">
            <span class="label">变化趋势:</span>
            <span class="value" :class="{'up': basePropResult.trend === '上升', 'down': basePropResult.trend === '下降'}">
              {{ basePropResult.trend }}
            </span>
          </div>
          <div class="result-item">
            <span class="label">比重变化:</span>
            <span class="value">{{ formatNumber(Math.abs(basePropResult.diff)) }} 个百分点</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span class="title">平均数与年均增长</span>
          <el-button type="primary" link @click="clearAverage">清空重置</el-button>
        </div>
      </template>

      <div class="section-title">1. 平均数及其增长 (输入总量、份数及增长率)</div>
      <div class="calc-content">
        <div class="calc-row-group">
          <div class="calc-row half">
            <div class="label-col-small">总量 (A)</div>
            <el-input v-model="avgInputs.amount" placeholder="数值" />
          </div>
          <div class="calc-row half">
            <div class="label-col-small">份数 (B)</div>
            <el-input v-model="avgInputs.count" placeholder="数值" />
          </div>
        </div>
        <div class="calc-row-group">
          <div class="calc-row half">
            <div class="label-col-small">总量增长率%</div>
            <el-input v-model="avgInputs.amountRate" placeholder="如 15" />
          </div>
          <div class="calc-row half">
            <div class="label-col-small">份数增长率%</div>
            <el-input v-model="avgInputs.countRate" placeholder="如 5" />
          </div>
        </div>
        
        <div class="result-box" v-if="avgResult">
          <div class="result-item">
            <span class="label">现期平均数:</span>
            <span class="value">{{ formatNumber(avgResult.currentAvg) }}</span>
          </div>
          <div class="result-item">
            <span class="label">基期平均数:</span>
            <span class="value">{{ formatNumber(avgResult.baseAvg) }}</span>
          </div>
          <div class="result-item">
            <span class="label">平均数增长率:</span>
            <span class="value">{{ formatNumber(avgResult.avgRate) }}%</span>
          </div>
          <div class="result-item">
            <span class="label">变化趋势:</span>
            <span class="value" :class="{'up': avgResult.trend === '上升', 'down': avgResult.trend === '下降'}">
              {{ avgResult.trend }}
            </span>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="section-title">2. 年均增长率 (输入初期、末期及年份差)</div>
      <div class="calc-content">
        <div class="calc-row">
          <div class="label-col">
            <span class="label">初期值 (Initial)</span>
          </div>
          <el-input v-model="cagrInputs.initial" placeholder="输入数值" clearable />
        </div>
        <div class="calc-row">
          <div class="label-col">
            <span class="label">末期值 (Final)</span>
          </div>
          <el-input v-model="cagrInputs.final" placeholder="输入数值" clearable />
        </div>
        <div class="calc-row">
          <div class="label-col">
            <span class="label">年份差 (n)</span>
          </div>
          <el-input v-model="cagrInputs.years" placeholder="输入年份差，如 5" clearable />
        </div>
        
        <div class="result-box" v-if="cagrResult !== null">
          <div class="result-item" style="width: 100%">
            <span class="label">年均增长率:</span>
            <span class="value">{{ formatNumber(cagrResult) }}%</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'CalculatorTool'
}
</script>

<script setup>
import { reactive, computed } from 'vue'

const inputs = reactive({
  base: '',
  current: '',
  rate: '',
  growth: ''
})

const evaluate = (str) => {
  if (!str) return null
  try {
    // Allow numbers, operators, dot, parens. Remove anything else.
    const sanitized = str.replace(/[^0-9+\-*/.()]/g, '')
    if (!sanitized) return null
    // eslint-disable-next-line no-new-func
    const result = new Function('return ' + sanitized)()
    return isFinite(result) ? result : null
  } catch (e) {
    return null
  }
}

const parsedValues = computed(() => {
  return {
    base: evaluate(inputs.base),
    current: evaluate(inputs.current),
    rate: evaluate(inputs.rate),
    growth: evaluate(inputs.growth)
  }
})

const finalValues = computed(() => {
  const p = parsedValues.value
  
  // Start with parsed values
  let A = p.base
  let B = p.current
  let r = p.rate
  let x = p.growth
  
  const has = (val) => val !== null && val !== undefined && !isNaN(val)
  
  // Logic to calculate missing values based on available pairs
  // Priority: User input > Calculated
  
  // 1. A & B -> x, r
  if (has(A) && has(B)) {
    if (!has(x)) x = B - A
    if (!has(r) && A !== 0) r = ((B - A) / A) * 100
  }
  // 2. A & x -> B, r
  else if (has(A) && has(x)) {
    if (!has(B)) B = A + x
    if (!has(r) && A !== 0) r = (x / A) * 100
  }
  // 3. A & r -> B, x
  else if (has(A) && has(r)) {
    if (!has(x)) x = A * (r / 100)
    if (!has(B)) B = A * (1 + r / 100)
  }
  // 4. B & x -> A, r
  else if (has(B) && has(x)) {
    if (!has(A)) A = B - x
    if (!has(r) && (B - x) !== 0) r = (x / (B - x)) * 100
  }
  // 5. B & r -> A, x
  else if (has(B) && has(r)) {
    // B = A * (1 + r/100) => A = B / (1 + r/100)
    if (!has(A)) A = B / (1 + r / 100)
    if (!has(x)) x = B - (B / (1 + r / 100))
  }
  // 6. x & r -> A, B
  else if (has(x) && has(r) && r !== 0) {
    // x = A * (r/100) => A = x / (r/100)
    if (!has(A)) A = x / (r / 100)
    if (!has(B)) B = A + x
  }
  
  return {
    base: A,
    current: B,
    rate: r,
    growth: x
  }
})

const formatNumber = (num) => {
  if (num === null || num === undefined || isNaN(num)) return '-'
  if (Number.isInteger(num)) return num
  return parseFloat(num.toFixed(4))
}

const propInputs = reactive({
  part: '',
  total: '',
  val: ''
})

const basePropInputs = reactive({
  part: '',
  total: '',
  partRate: '',
  totalRate: ''
})

const avgInputs = reactive({
  amount: '',
  count: '',
  amountRate: '',
  countRate: ''
})

const cagrInputs = reactive({
  initial: '',
  final: '',
  years: ''
})

const parsedPropValues = computed(() => {
  return {
    part: evaluate(propInputs.part),
    total: evaluate(propInputs.total),
    val: evaluate(propInputs.val)
  }
})

const finalPropValues = computed(() => {
  const p = parsedPropValues.value
  let part = p.part
  let total = p.total
  let val = p.val
  
  const has = (v) => v !== null && v !== undefined && !isNaN(v)

  if (has(part) && has(total) && total !== 0) {
    if (!has(val)) val = (part / total) * 100
  } else if (has(part) && has(val) && val !== 0) {
    if (!has(total)) total = part / (val / 100)
  } else if (has(total) && has(val)) {
    if (!has(part)) part = total * (val / 100)
  }

  return { part, total, val }
})

const basePropResult = computed(() => {
  const A = evaluate(basePropInputs.part)
  const B = evaluate(basePropInputs.total)
  const a = evaluate(basePropInputs.partRate)
  const b = evaluate(basePropInputs.totalRate)
  
  if (A === null || B === null || a === null || b === null || B === 0 || (1 + a/100) === 0) return null
  
  const baseProp = (A / B) * ((1 + b / 100) / (1 + a / 100)) * 100
  
  let trend = '不变'
  if (a > b) trend = '上升'
  else if (a < b) trend = '下降'
  
  const currentProp = (A / B) * 100
  
  return {
    value: baseProp,
    trend: trend,
    diff: currentProp - baseProp
  }
})

const avgResult = computed(() => {
  const A = evaluate(avgInputs.amount)
  const B = evaluate(avgInputs.count)
  const a = evaluate(avgInputs.amountRate)
  const b = evaluate(avgInputs.countRate)
  
  // At least A and B are needed for current average
  if (A === null || B === null || B === 0) return null
  
  const currentAvg = A / B
  
  let baseAvg = null
  let avgRate = null
  let trend = '未知'
  
  // If rates are provided
  if (a !== null && b !== null) {
    // Base Average = (A/B) * ((1+b)/(1+a))
    if ((1 + a/100) !== 0) {
      baseAvg = (A / B) * ((1 + b / 100) / (1 + a / 100))
    }
    
    // Avg Growth Rate = (a-b)/(1+b)
    if ((1 + b/100) !== 0) {
      avgRate = ((a - b) / (1 + b / 100))
    }
    
    if (a > b) trend = '上升'
    else if (a < b) trend = '下降'
    else trend = '不变'
  }
  
  return {
    currentAvg,
    baseAvg,
    avgRate,
    trend
  }
})

const cagrResult = computed(() => {
  const init = evaluate(cagrInputs.initial)
  const final = evaluate(cagrInputs.final)
  const n = evaluate(cagrInputs.years)
  
  if (init === null || final === null || n === null || init === 0 || n === 0) return null
  
  // Formula: (Final/Initial)^(1/n) - 1
  try {
    const ratio = final / init
    if (ratio < 0) return null // Complex number handling not supported
    const r = Math.pow(ratio, 1 / n) - 1
    return r * 100
  } catch (e) {
    return null
  }
})

const clearAverage = () => {
  avgInputs.amount = ''
  avgInputs.count = ''
  avgInputs.amountRate = ''
  avgInputs.countRate = ''
  cagrInputs.initial = ''
  cagrInputs.final = ''
  cagrInputs.years = ''
}



const clearProportion = () => {
  propInputs.part = ''
  propInputs.total = ''
  propInputs.val = ''
  basePropInputs.part = ''
  basePropInputs.total = ''
  basePropInputs.partRate = ''
  basePropInputs.totalRate = ''
}

const clearAll = () => {
  inputs.base = ''
  inputs.current = ''
  inputs.rate = ''
  inputs.growth = ''
}
</script>

<style scoped>
.calculator-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: bold;
}
.calc-content {
  margin: 20px 0;
}
.calc-row {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
}
.label-col {
  width: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin-right: 15px;
}
.label {
  font-weight: bold;
  color: #606266;
  font-size: 14px;
}
.dynamic-val {
  font-size: 20px;
  color: #409EFF;
  font-weight: bold;
  margin-top: 5px;
}
.tips {
  margin-top: 30px;
}
.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}
.calc-row-group {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}
.calc-row.half {
  flex: 1;
  margin-bottom: 0;
}
.label-col-small {
  width: 100px;
  font-size: 14px;
  color: #606266;
  margin-right: 10px;
  white-space: nowrap;
}
.result-box {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-top: 20px;
  display: flex;
  justify-content: space-around;
}
.result-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.result-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
  margin-top: 5px;
}
.result-item .value.up {
  color: #F56C6C;
}
.result-item .value.down {
  color: #67C23A;
}
</style>
