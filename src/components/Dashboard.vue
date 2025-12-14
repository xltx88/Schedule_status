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
          <el-col :xs="24" :md="8">
            <el-card class="box-card" style="margin-bottom: 20px;">
              <template #header>
                <div class="card-header">
                  <span>任务列表</span>
                  <div class="current-status">
                    当前状态: 
                    <el-tag v-if="currentTask" type="success" effect="dark">{{ currentTask.name }}</el-tag>
                    <el-tag v-else type="info">无任务</el-tag>
                    <span v-if="currentTask" style="margin-left: 10px; font-weight: bold;">{{ currentTaskDuration }}</span>
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
                      style="flex-grow: 1; justify-content: flex-start;"
                    >
                      {{ task.name }}
                    </el-button>
                    <el-popconfirm 
                      v-if="task.userId !== null" 
                      title="确定删除吗?" 
                      @confirm="deleteTask(task)"
                    >
                      <template #reference>
                        <el-button 
                          type="danger" 
                          :icon="Delete" 
                          circle 
                          size="small" 
                          style="margin-left: 10px;" 
                          @click.stop
                        />
                      </template>
                    </el-popconfirm>
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
          <el-col :xs="24" :md="16">
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
import { ref, onMounted, defineProps, defineEmits, nextTick, onUnmounted } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['logout', 'update-user'])

const tasks = ref([])
const currentTask = ref(null)
const newTaskName = ref('')
const pieDate = ref(new Date().toISOString().split('T')[0])
const lineDateRange = ref([
  new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  new Date().toISOString().split('T')[0]
])
const currentTaskDuration = ref('00:00:00')

const pieChartRef = ref(null)
const lineChartRef = ref(null)
let pieChart = null
let lineChart = null
let timer = null

const API_URL = `${window.location.protocol}//${window.location.hostname}:58081/api/tasks`

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
  
  // Start timer
  timer = setInterval(updateTimer, 1000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (timer) clearInterval(timer)
})

const updateTimer = () => {
  if (props.user.currentTaskId && props.user.currentTaskStartTime) {
    const diff = Date.now() - props.user.currentTaskStartTime
    if (diff >= 0) {
      const seconds = Math.floor(diff / 1000)
      const h = Math.floor(seconds / 3600)
      const m = Math.floor((seconds % 3600) / 60)
      const s = seconds % 60
      currentTaskDuration.value = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
    }
  } else {
    currentTaskDuration.value = '00:00:00'
  }
}

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

const deleteTask = async (task) => {
  try {
    const res = await axios.delete(`${API_URL}/${task.id}?userId=${props.user.id}`)
    tasks.value = tasks.value.filter(t => t.id !== task.id)
    
    // If backend returns updated user (meaning current task was deleted and switched)
    if (res.data && res.data.currentTaskId) {
      emit('update-user', res.data)
      // Find the new task (likely Leave task)
      // We might need to re-fetch tasks if the new task is not in the list (e.g. Leave task might be hidden or default)
      // Assuming Leave task (ID 1) is always in the list or we can find it.
      // If Leave task is not in the list, we might need to fetch tasks again or handle it.
      // But usually Leave task is ID 1.
      
      // Re-fetch tasks to be safe, or just find from existing if we know Leave is there.
      // Let's try to find it first.
      let newCurrent = tasks.value.find(t => t.id === res.data.currentTaskId)
      
      if (!newCurrent) {
         // If not found (maybe it was filtered out or not loaded), fetch tasks again
         await fetchTasks()
         newCurrent = tasks.value.find(t => t.id === res.data.currentTaskId)
      }
      
      currentTask.value = newCurrent || null
    } else if (currentTask.value?.id === task.id) {
       // Fallback if backend didn't return user but we deleted current task (shouldn't happen with new backend logic)
       currentTask.value = null
    }
    
    ElMessage.success('任务已删除')
  } catch (error) {
     ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
  }
}

const switchTask = async (task) => {
  if (currentTask.value?.id === task.id) return
  
  try {
    const res = await axios.post(`${API_URL}/switch`, {
      userId: props.user.id,
      taskId: task.id
    })
    
    currentTask.value = task
    emit('update-user', res.data)
    
    ElMessage.success(`已切换到: ${task.name}`)
  } catch (error) {
    ElMessage.error('切换任务失败')
  }
}

const logout = () => {
  emit('logout')
}

// Chart initialization and fetching logic...
const initCharts = () => {
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
  }
  if (lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)
  }
}

const formatDurationMs = (ms) => {
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}小时${m}分`
  return `${m}分${sec}秒`
}

const fetchPieData = async () => {
  try {
    const res = await axios.get(`${API_URL}/stats/pie`, {
      params: { userId: props.user.id, date: pieDate.value }
    })
    
    const option = {
       tooltip: { 
         trigger: 'item',
         formatter: function(params) {
           const formatted = params.data.formatted || formatDurationMs(params.data.value)
           return `${params.name}: ${formatted} (${params.percent}%)`
         }
       },
       legend: { orient: 'vertical', left: 'left' },
       series: [
         {
           name: '任务时长',
           type: 'pie',
           radius: '50%',
           startAngle: 90, // Start from 12 o'clock
           data: res.data.data,
           label: {
             formatter: function(params) {
               const formatted = params.data.formatted || formatDurationMs(params.data.value)
               return `${params.name}\n${formatted}`
             }
           },
           emphasis: {
             itemStyle: {
               shadowBlur: 10,
               shadowOffsetX: 0,
               shadowColor: 'rgba(0, 0, 0, 0.5)'
             }
           }
         }
       ]
     }
    pieChart.setOption(option)
  } catch (error) {
    console.error(error)
  }
}

const fetchLineData = async () => {
  try {
    const res = await axios.get(`${API_URL}/stats/line`, {
      params: { 
        userId: props.user.id, 
        startDate: lineDateRange.value[0],
        endDate: lineDateRange.value[1]
      }
    })
    
    const option = {
      tooltip: { 
        trigger: 'axis',
        formatter: function(params) {
          // params is an array of series data for the axis point
          let result = params[0].name + '<br/>';
          params.forEach(item => {
            const formatted = formatDurationMs(item.value);
            result += `${item.marker} ${item.seriesName || '时长'}: ${formatted}<br/>`;
          });
          return result;
        }
      },
      xAxis: { type: 'category', data: res.data.dates },
      yAxis: { 
        type: 'value', 
        name: '时长', 
        axisLabel: { 
          formatter: (val) => {
            // Simplify Y-axis label to hours if large, or minutes
            const h = val / 3600000;
            if (h >= 1) return h.toFixed(1) + 'h';
            return (val / 60000).toFixed(0) + 'm';
          } 
        } 
      },
      series: [
        {
          data: res.data.durations,
          type: 'line',
          smooth: true,
          name: '时长'
        }
      ]
    }
    lineChart.setOption(option)
  } catch (error) {
    console.error(error)
  }
}
</script>

<style scoped>
.dashboard-container {
  height: 100vh;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.task-list {
  margin-bottom: 20px;
}
.task-item {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
.task-btn {
  width: 100%;
  text-align: left;
}
.add-task {
  margin-top: 20px;
}
</style>
