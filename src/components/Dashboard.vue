<template>
  <div class="dashboard-container">
    <el-container>
      <el-header height="auto">
        <div class="header-content">
          <div class="user-info">
            <h2>你好, {{ user.username }}</h2>
          </div>
          
          <div class="checkin-status" v-if="checkInStatus">
             <div class="status-circles">
               <el-tooltip 
                 v-for="(status, index) in checkInStatus.statusList" 
                 :key="index"
                 :content="status.date"
                 placement="top"
                 effect="light"
               >
                 <div class="status-item">
                   <div class="circle" :class="{ 'success': status.metGoal, 'fail': !status.metGoal }">
                     <span v-if="status.metGoal">✓</span>
                     <span v-else>✕</span>
                   </div>
                 </div>
               </el-tooltip>
               <div class="status-item">
                 <el-button circle :icon="Edit" @click="showGoalDialog = true" class="edit-btn"></el-button>
               </div>
             </div>
          </div>

          <el-button type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <el-tabs v-model="activeTab" class="dashboard-tabs">
          <el-tab-pane label="工作台" name="dashboard">
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
                <el-scrollbar max-height="440px">
                  <draggable 
                    v-model="tasks" 
                    item-key="id" 
                    @end="onDragEnd"
                    class="task-list-container"
                    :delay="200"
                    :delay-on-touch-only="true"
                  >
                    <template #item="{ element: task }">
                      <div 
                        class="task-item" 
                        :class="{ 'global-task': task.userId === null, 'user-task': task.userId !== null }"
                        style="cursor: move;"
                      >
                        <el-button 
                          class="task-btn" 
                          :type="currentTask?.id === task.id ? 'primary' : 'default'"
                          :plain="currentTask?.id !== task.id"
                          @click="switchTask(task)"
                          style="flex-grow: 1; justify-content: flex-start; overflow: hidden; text-overflow: ellipsis;"
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
                              style="margin-left: 5px; flex-shrink: 0;" 
                              @click.stop
                            />
                          </template>
                        </el-popconfirm>
                      </div>
                    </template>
                  </draggable>
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

            <!-- Timeline Chart (Moved to Left Column & Vertical) -->
            <el-card class="box-card" style="margin-bottom: 20px;">
              <template #header>
                <div class="card-header">
                  <span>今日时间轴</span>
                </div>
              </template>
              <div ref="timelineChartRef" style="height: 400px;"></div>
            </el-card>
          </el-col>

          <!-- Statistics Section -->
          <el-col :xs="24" :md="16">
            <el-card class="box-card" style="margin-bottom: 20px;">
              <template #header>
                <div class="card-header">
                  <span>时长统计</span>
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
          </el-tab-pane>
          <el-tab-pane label="高频成语" name="idioms" lazy>
            <IdiomList />
          </el-tab-pane>
          <el-tab-pane label="资料分析" name="calculator" lazy>
            <Calculator />
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>

    <el-dialog v-model="showGoalDialog" title="设置每日目标" width="30%">
      <span>每日目标时长 (小时):</span>
      <el-input-number v-model="newGoal" :min="1" :max="24" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showGoalDialog = false">取消</el-button>
          <el-button type="primary" @click="updateGoal">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'DashboardComponent'
}
</script>

<script setup>
import { ref, reactive, onMounted, nextTick, onUnmounted, watch } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Delete, Edit } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import IdiomList from './IdiomList.vue'
import Calculator from './Calculator.vue'
import { API_BASE_URL } from '../config'

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['logout', 'update-user'])

const activeTab = ref(localStorage.getItem('activeTab') || 'dashboard')

watch(activeTab, (newVal) => {
  localStorage.setItem('activeTab', newVal)
})

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
const timelineChartRef = ref(null)
let pieChart = null
let lineChart = null
let timelineChart = null
let timer = null
let syncTimer = null

const API_URL = `${API_BASE_URL}/tasks`
const AUTH_API_URL = `${API_BASE_URL}/auth`

onMounted(async () => {
  await fetchTasks()
  await fetchCheckInStatus()
  
  // Set current task from user data if available
  if (props.user.currentTaskId) {
    currentTask.value = tasks.value.find(t => t.id === props.user.currentTaskId) || null
  }

  await nextTick()
  initCharts()
  fetchPieData()
  fetchLineData()
  fetchTimelineData()

  // Resize charts on window resize
  window.addEventListener('resize', handleResize)
  
  // Start timer
  timer = setInterval(updateTimer, 1000)

  // Start sync timer (every 10 seconds)
  syncTimer = setInterval(syncUserStatus, 10000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (timer) clearInterval(timer)
  if (syncTimer) clearInterval(syncTimer)
})

const syncUserStatus = async () => {
  // Only sync if page is visible
  if (document.hidden) return

  try {
    const res = await axios.get(`${AUTH_API_URL}/user/${props.user.id}`)
    const remoteUser = res.data
    
    // Check if task changed
    if (remoteUser.currentTaskId !== props.user.currentTaskId || 
        remoteUser.currentTaskStartTime !== props.user.currentTaskStartTime) {
      
      emit('update-user', remoteUser)
      
      // Update local current task
      const newTask = tasks.value.find(t => t.id === remoteUser.currentTaskId)
      if (newTask) {
        currentTask.value = newTask
      } else {
        // If task not found (e.g. new task added on another device), refresh tasks
        await fetchTasks()
        currentTask.value = tasks.value.find(t => t.id === remoteUser.currentTaskId) || null
      }
      
      // Refresh charts as data might have changed
      fetchPieData()
      fetchLineData()
      fetchTimelineData()
    }
  } catch (error) {
    console.error('Sync failed', error)
  }
}

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
  timelineChart?.resize()
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

    // Refresh charts
    fetchPieData()
    fetchLineData()
    fetchTimelineData()
  } catch (error) {
    ElMessage.error('切换任务失败')
  }
}

const onDragEnd = async () => {
  const taskIds = tasks.value.map(t => t.id)
  try {
    await axios.post(`${API_URL}/order`, {
      userId: props.user.id,
      taskIds: taskIds
    })
  } catch (error) {
    ElMessage.error('保存排序失败')
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
  if (timelineChartRef.value) {
    timelineChart = echarts.init(timelineChartRef.value)
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
           data: res.data.data.map(item => ({
             ...item,
             itemStyle: { color: getColorForTask(item.name) }
           })),
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

const fetchTimelineData = async () => {
  try {
    const res = await axios.get(`${API_URL}/timeline`, {
      params: { userId: props.user.id, date: pieDate.value }
    })
    
    const data = res.data
    if (!data || data.length === 0) {
      timelineChart.clear()
      return
    }

    const startTime = data[0].startTime
    const endTime = data[data.length - 1].endTime
    
    const seriesData = data.map(item => {
      return {
        name: item.taskName,
        value: [
          0, // Category index
          item.startTime,
          item.endTime,
          item.duration
        ],
        itemStyle: {
          color: getColorForTask(item.taskName)
        }
      }
    })

    // Custom render function for vertical bars
    const renderItem = (params, api) => {
      var categoryIndex = api.value(0);
      // Switch X and Y for vertical layout
      // X is category (fixed width), Y is time
      var start = api.coord([categoryIndex, api.value(1)]);
      var end = api.coord([categoryIndex, api.value(2)]);
      var width = api.size([1, 0])[0] * 0.6; // Bar width
      
      var rectShape = echarts.graphic.clipRectByRect({
        x: start[0] - width / 2,
        y: end[1], // In screen coordinates, Y increases downwards. end time is larger (lower on screen if not inverted)
                   // But ECharts time axis usually puts earlier time at bottom or top depending on inverse.
                   // Let's rely on coord.
        width: width,
        height: start[1] - end[1] // Height is difference in Y
      }, {
        x: params.coordSys.x,
        y: params.coordSys.y,
        width: params.coordSys.width,
        height: params.coordSys.height
      });
      
      // Re-calculate rect because clipRectByRect might be tricky with inverted axis or direction
      // Simpler approach:
      var x = start[0] - width / 2;
      var y = end[1];
      var h = start[1] - end[1];
      
      // Ensure height is positive
      if (h < 0) {
          y = start[1];
          h = -h;
      }

      return {
        type: 'rect',
        transition: ['shape'],
        shape: {
            x: x,
            y: y,
            width: width,
            height: h,
            r: [10, 10, 10, 10] // Rounded corners for cylinder look
        },
        style: api.style()
      };
    }

    const option = {
      tooltip: {
        formatter: function (params) {
          return params.marker + params.name + '<br/>' + 
                 new Date(params.value[1]).toLocaleTimeString() + ' - ' + 
                 new Date(params.value[2]).toLocaleTimeString() + '<br/>' +
                 '时长: ' + formatDurationMs(params.value[3]);
        }
      },
      grid: {
        left: '15%',
        right: '10%',
        top: '5%',
        bottom: '5%'
      },
      yAxis: {
        type: 'time',
        min: startTime,
        max: endTime,
        inverse: true, // Time goes down
        axisLabel: {
          formatter: function(val) {
            const date = new Date(val);
            return `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`;
          }
        }
      },
      xAxis: {
        data: ['Timeline'],
        show: false
      },
      series: [{
        type: 'custom',
        renderItem: renderItem,
        itemStyle: {
          opacity: 0.9,
          shadowBlur: 5,
          shadowColor: 'rgba(0,0,0,0.1)'
        },
        encode: {
          x: 0,
          y: [1, 2]
        },
        data: seriesData
      }]
    };
    
    timelineChart.setOption(option);
  } catch (error) {
    console.error(error)
  }
}

// Macaron colors palette (15+ colors)
const macaronColors = [
  '#FFB7B2', // Macaron Red
  '#FFDAC1', // Macaron Orange
  '#E2F0CB', // Macaron Green
  '#B5EAD7', // Macaron Mint
  '#C7CEEA', // Macaron Purple
  '#F8BBD0', // Pink
  '#E1BEE7', // Purple
  '#D1C4E9', // Deep Purple
  '#C5CAE9', // Indigo
  '#BBDEFB', // Blue
  '#B3E5FC', // Light Blue
  '#B2EBF2', // Cyan
  '#B2DFDB', // Teal
  '#C8E6C9', // Green
  '#DCEDC8', // Light Green
  '#F0F4C3', // Lime
  '#FFF9C4', // Yellow
  '#FFECB3', // Amber
  '#FFE0B2', // Orange
  '#FFCCBC'  // Deep Orange
];

const taskColorMap = reactive({})
let nextColorIndex = 0

const getColorForTask = (name) => {
  if (taskColorMap[name]) {
    return taskColorMap[name]
  }
  const color = macaronColors[nextColorIndex % macaronColors.length]
  taskColorMap[name] = color
  nextColorIndex++
  return color
}

const checkInStatus = ref(null)
const showGoalDialog = ref(false)
const newGoal = ref(8)

const fetchCheckInStatus = async () => {
  try {
    const res = await axios.get(`${API_URL}/checkin`, {
      params: { userId: props.user.id }
    })
    checkInStatus.value = res.data
    newGoal.value = res.data.dailyGoal
  } catch (error) {
    console.error(error)
  }
}

const updateGoal = async () => {
  try {
    await axios.post(`${AUTH_API_URL}/user/${props.user.id}/goal`, {
      goal: newGoal.value
    })
    showGoalDialog.value = false
    fetchCheckInStatus()
    ElMessage.success('目标已更新')
  } catch (error) {
    ElMessage.error('更新失败')
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
  min-height: 60px;
  padding: 10px 0;
}
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 10px;
  }
  .checkin-status {
    margin: 10px 0;
    width: 100%;
    justify-content: center;
  }
}
.checkin-status {
  display: flex;
  align-items: center;
  overflow-x: auto;
  margin: 0 20px;
}
.status-circles {
  display: flex;
  gap: 10px;
  flex-wrap: wrap; /* Allow wrapping on small screens */
  justify-content: center; /* Center items */
}
.status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 16px;
  cursor: pointer; /* Indicate interactivity */
}
.circle.success {
  background-color: #67C23A;
}
.circle.fail {
  background-color: #F56C6C;
}
.date-label {
  font-size: 10px;
  color: #909399;
  margin-top: 2px;
}
.edit-btn {
  width: 30px;
  height: 30px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.task-list {
  margin-bottom: 20px;
}
.task-list-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.task-item {
  margin-bottom: 0; /* Gap handles spacing */
  display: flex;
  align-items: center;
  box-sizing: border-box;
}
.global-task {
  width: 100%;
}
.user-task {
  width: calc(50% - 5px);
}
.task-btn {
  width: 100%;
  text-align: left;
}
.add-task {
  margin-top: 20px;
}
</style>
