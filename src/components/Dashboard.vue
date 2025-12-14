<template>
  <div class="dashboard-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <div class="user-info">
            <h2>你好, {{ user.username }}</h2>
          </div>
          <el-button type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <el-row :gutter="20">
          <!-- Task Management Section -->
          <el-col :span="8">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>任务列表</span>
                  <div class="current-status">
                    当前状态: 
                    <el-tag v-if="currentTask" type="success" effect="dark">{{ currentTask.name }}</el-tag>
                    <el-tag v-else type="info">无任务</el-tag>
                  </div>
                </div>
              </template>
              
              <div class="task-list">
                <el-scrollbar height="400px">
                  <div v-for="task in tasks" :key="task.id" class="task-item">
                    <el-button 
                      class="task-btn" 
                      :type="currentTask?.id === task.id ? 'primary' : 'default'"
                      :plain="currentTask?.id !== task.id"
                      @click="switchTask(task)"
                    >
                      {{ task.name }}
                    </el-button>
                  </div>
                </el-scrollbar>
              </div>

              <div class="add-task">
                <el-input v-model="newTaskName" placeholder="新任务名称" class="input-with-select">
                  <template #append>
                    <el-button @click="addTask">添加</el-button>
                  </template>
                </el-input>
              </div>
            </el-card>
          </el-col>

          <!-- Statistics Section -->
          <el-col :span="16">
            <el-card class="box-card" style="margin-bottom: 20px;">
              <template #header>
                <div class="card-header">
                  <span>今日任务时长统计</span>
                  <el-date-picker
                    v-model="pieDate"
                    type="date"
                    placeholder="选择日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    @change="fetchPieData"
                    :clearable="false"
                  />
                </div>
              </template>
              <div ref="pieChartRef" style="height: 350px;"></div>
            </el-card>

            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>时长趋势</span>
                  <el-date-picker
                    v-model="lineDateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    @change="fetchLineData"
                    :clearable="false"
                  />
                </div>
              </template>
              <div ref="lineChartRef" style="height: 350px;"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'DashboardComponent'
}
</script>

<script setup>
import { ref, onMounted, defineProps, defineEmits, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['logout'])

const tasks = ref([])
const currentTask = ref(null)
const newTaskName = ref('')
const pieDate = ref(new Date().toISOString().split('T')[0])
const lineDateRange = ref([
  new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  new Date().toISOString().split('T')[0]
])

const pieChartRef = ref(null)
const lineChartRef = ref(null)
let pieChart = null
let lineChart = null

const API_URL = 'http://localhost:8081/api/tasks'

onMounted(async () => {
  await fetchTasks()
  
  // Set current task from user data if available
  if (props.user.currentTaskId) {
    currentTask.value = tasks.value.find(t => t.id === props.user.currentTaskId) || null
  }

  await nextTick()
  initCharts()
  fetchPieData()
  fetchLineData()

  // Resize charts on window resize
  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  pieChart?.resize()
  lineChart?.resize()
}

const fetchTasks = async () => {
  try {
    const res = await axios.get(`${API_URL}?userId=${props.user.id}`)
    tasks.value = res.data
  } catch (error) {
    ElMessage.error('获取任务列表失败')
  }
}

const addTask = async () => {
  if (!newTaskName.value.trim()) return
  try {
    const res = await axios.post(API_URL, {
      name: newTaskName.value,
      userId: props.user.id
    })
    tasks.value.push(res.data)
    newTaskName.value = ''
    ElMessage.success('添加成功')
  } catch (error) {
    ElMessage.error('添加任务失败')
  }
}

const switchTask = async (task) => {
  if (currentTask.value?.id === task.id) return
  
  try {
    await axios.post(`${API_URL}/switch`, {
      userId: props.user.id,
      taskId: task.id
    })
    currentTask.value = task
    
    // eslint-disable-next-line vue/no-mutating-props
    props.user.currentTaskId = task.id
    // eslint-disable-next-line vue/no-mutating-props
    props.user.currentTaskStartTime = Date.now()
    localStorage.setItem('user', JSON.stringify(props.user))
    
    ElMessage.success(`已切换到: ${task.name}`)
    // Refresh today's stats as the previous task session just ended
    setTimeout(fetchPieData, 500) 
  } catch (error) {
    ElMessage.error('切换任务失败')
  }
}

const initCharts = () => {
  pieChart = echarts.init(pieChartRef.value)
  lineChart = echarts.init(lineChartRef.value)
}

const fetchPieData = async () => {
  if (!pieDate.value) return
  try {
    const res = await axios.get(`${API_URL}/stats/pie`, {
      params: { userId: props.user.id, date: pieDate.value }
    })
    updatePieChart(res.data.data)
  } catch (error) {
    console.error(error)
  }
}

const updatePieChart = (data) => {
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%) <br/> {a}' 
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '时长',
        type: 'pie',
        radius: '50%',
        data: data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          formatter: function(params) {
            return `${params.name}\n${params.data.formatted}`
          }
        }
      }
    ]
  }
  pieChart.setOption(option)
}

const fetchLineData = async () => {
  if (!lineDateRange.value) return
  try {
    const res = await axios.get(`${API_URL}/stats/line`, {
      params: { 
        userId: props.user.id, 
        startDate: lineDateRange.value[0], 
        endDate: lineDateRange.value[1] 
      }
    })
    updateLineChart(res.data)
  } catch (error) {
    console.error(error)
  }
}

const updateLineChart = (data) => {
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: function (params) {
        const val = params[0].value;
        const hours = (val / 3600000).toFixed(2);
        return `${params[0].name}<br/>时长: ${hours} 小时`;
      }
    },
    xAxis: {
      type: 'category',
      data: data.dates
    },
    yAxis: {
      type: 'value',
      name: '时长 (小时)',
      axisLabel: {
        formatter: (value) => (value / 3600000).toFixed(1)
      }
    },
    series: [
      {
        data: data.durations,
        type: 'line',
        smooth: true,
        areaStyle: {}
      }
    ]
  }
  lineChart.setOption(option)
}

const logout = () => {
  emit('logout')
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  padding: 0 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  height: 60px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.current-status {
  font-size: 14px;
  color: #606266;
}
.task-list {
  margin-bottom: 20px;
}
.task-item {
  margin-bottom: 10px;
}
.task-btn {
  width: 100%;
  text-align: left;
  justify-content: flex-start;
}
.add-task {
  margin-top: 10px;
}
.box-card {
  height: 100%;
}
</style>
