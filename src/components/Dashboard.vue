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
                    <el-button 
                      v-if="showSettleButton" 
                      type="warning" 
                      size="small" 
                      style="margin-left: 10px;"
                      @click="settleDailyTask"
                    >
                      结束今日任务
                    </el-button>
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
                        <el-button 
                          v-if="task.userId !== null"
                          type="primary" 
                          :icon="Edit" 
                          circle 
                          size="small" 
                          style="margin-left: 5px; flex-shrink: 0;" 
                          @click.stop="openRecordDialog(task)"
                        />
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
              <div class="timeline-stats" v-if="timelineStats">
                <div class="stat-item">
                  <span class="label">上午 (13:00前)</span>
                  <span class="value">{{ formatDurationMs(timelineStats.morning) }}</span>
                </div>
                <div class="stat-item">
                  <span class="label">下午 (13:00-18:00)</span>
                  <span class="value">{{ formatDurationMs(timelineStats.afternoon) }}</span>
                </div>
                <div class="stat-item">
                  <span class="label">晚上 (18:00后)</span>
                  <span class="value">{{ formatDurationMs(timelineStats.evening) }}</span>
                </div>
              </div>
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
                    :editable="false"
                    class="compact-date-picker"
                  />
                </div>
              </template>
              <div ref="pieChartRef" style="height: 350px;"></div>
            </el-card>

            <el-card class="box-card">
              <template #header>
                <div class="card-header trend-header">
                  <span>时长趋势</span>
                  <div class="date-range-container">
                    <el-date-picker
                      v-model="lineDateRange[0]"
                      type="date"
                      placeholder="开始"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      @change="handleDateRangeChange"
                      :clearable="false"
                      :editable="false"
                      class="compact-date-picker"
                    />
                    <span class="range-separator">至</span>
                    <el-date-picker
                      v-model="lineDateRange[1]"
                      type="date"
                      placeholder="结束"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      @change="handleDateRangeChange"
                      :clearable="false"
                      :editable="false"
                      class="compact-date-picker"
                    />
                  </div>
                </div>
              </template>
              <div ref="lineChartRef" style="height: 350px;"></div>
              
              <div class="ranking-stats-container" v-if="rankingStats">
                <div class="stat-item">
                  <div class="stat-title">今日排名</div>
                  <div class="stat-value">{{ rankingStats.todayRank }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-title">累计时长</div>
                  <div class="stat-value">{{ rankingStats.totalDuration }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-title">昨日排名</div>
                  <div class="stat-list">
                    <div v-for="user in rankingStats.yesterdayTop3" :key="user.rank">
                      <span class="rank-num">{{ user.rank }}.</span>
                      <span class="rank-name">{{ user.name }}</span>
                    </div>
                    <div v-if="!rankingStats.yesterdayTop3 || rankingStats.yesterdayTop3.length === 0" class="no-data">暂无数据</div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
            </el-row>
          </el-tab-pane>
          <el-tab-pane label="高频成语" name="idioms" lazy>
            <IdiomList />
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>

    <el-dialog v-model="showGoalDialog" title="设置每日目标" :width="dialogWidth">
      <span>每日目标时长 (小时):</span>
      <el-input-number v-model="newGoal" :min="1" :max="24" style="width: 100%; margin-top: 10px;" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showGoalDialog = false">取消</el-button>
          <el-button type="primary" @click="updateGoal">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="showRecordDialog" title="任务设置" :width="dialogWidth">
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <span>是否将该任务记录到时长趋势？</span>
        <el-switch
          v-model="tempRecordTagValue"
          active-color="#13ce66"
          inactive-color="#909399"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showRecordDialog = false">取消</el-button>
          <el-button type="primary" @click="saveRecordTag">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="showTimeEditDialog" title="修改时间记录" :width="dialogWidth">
      <el-form :model="editingTimeRecord" label-width="80px">
        <el-form-item label="日期">
          <el-input v-model="editingTimeRecord.date" disabled />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker
            v-model="editingTimeRecord.startTime"
            placeholder="选择开始时间"
            format="HH:mm:ss"
            value-format="x"
            style="width: 100%"
            :editable="false"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker
            v-model="editingTimeRecord.endTime"
            placeholder="选择结束时间"
            format="HH:mm:ss"
            value-format="x"
            style="width: 100%"
            :editable="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showTimeEditDialog = false">取消</el-button>
          <el-button type="primary" @click="preSaveTimeRecord">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="showConfirmEditDialog" title="确认修改" :width="dialogWidth">
      <div style="padding: 10px;">
        <p>确认将时间记录修改为：</p>
        <p>开始时间: {{ confirmEditData.startTime.split(' ')[0] }} <strong>{{ confirmEditData.startTime.split(' ')[1] }}</strong></p>
        <p>结束时间: {{ confirmEditData.endTime.split(' ')[0] }} <strong>{{ confirmEditData.endTime.split(' ')[1] }}</strong></p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showConfirmEditDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmSaveTimeRecord">确认修改</el-button>
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
import dayjs from 'dayjs'
import IdiomList from './IdiomList.vue'
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

const handleDateRangeChange = () => {
  if (lineDateRange.value[0] > lineDateRange.value[1]) {
    const temp = lineDateRange.value[0]
    lineDateRange.value[0] = lineDateRange.value[1]
    lineDateRange.value[1] = temp
  }
  fetchLineData()
  fetchRankings()
}

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

const dialogWidth = ref('30%')

const updateDialogWidth = () => {
  dialogWidth.value = window.innerWidth <= 768 ? '90%' : '30%'
}

onMounted(async () => {
  updateDialogWidth()
  window.addEventListener('resize', updateDialogWidth)
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
  fetchRankings()

  // Resize charts on window resize
  window.addEventListener('resize', handleResize)
  
  // Start timer
  timer = setInterval(updateTimer, 1000)

  // Start sync timer (every 10 seconds)
  syncTimer = setInterval(syncUserStatus, 10000)

  // Immediate sync to get latest status
  await syncUserStatus()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateDialogWidth)
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
  const now = new Date();
  const hour = now.getHours();
  // Show settle button if hour >= 23 or hour < 4
  showSettleButton.value = (hour >= 23 || hour < 4) && currentTask.value !== null;

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

const showSettleButton = ref(false)

const settleDailyTask = async () => {
  try {
    await axios.post(`${API_URL}/settle`, {
      userId: props.user.id
    })
    ElMessage.success('当日任务已结算')
    // Force sync to update UI
    await syncUserStatus()
    // Refresh charts
    fetchPieData()
    fetchLineData()
    fetchTimelineData()
  } catch (error) {
    ElMessage.error('结束失败: ' + (error.response?.data?.message || error.message))
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
    
    const isMobile = window.innerWidth <= 768
    const legendOption = isMobile ? {
      bottom: 0,
      left: 'center',
      orient: 'horizontal'
    } : {
      orient: 'vertical',
      left: 'left'
    }

    // Adjust chart position on mobile to make room for multi-line legend at bottom
    const seriesCenter = isMobile ? ['50%', '40%'] : ['50%', '50%']
    const seriesRadius = isMobile ? '35%' : '50%'

    const option = {
       tooltip: { 
         trigger: 'item',
         confine: true,
         formatter: function(params) {
           const formatted = params.data.formatted || formatDurationMs(params.data.value)
           return `${params.name}: ${formatted} (${params.percent}%)`
         }
       },
       legend: legendOption,
       series: [
         {
           name: '任务时长',
           type: 'pie',
           radius: seriesRadius,
           center: seriesCenter,
           startAngle: 90, // Start from 12 o'clock
           avoidLabelOverlap: true,
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

const timelineStats = ref({
  morning: 0,
  afternoon: 0,
  evening: 0
})

const fetchTimelineData = async () => {
  try {
    const res = await axios.get(`${API_URL}/timeline`, {
      params: { userId: props.user.id, date: pieDate.value }
    })
    
    const data = res.data
    
    // Calculate Morning/Afternoon/Evening stats
    const baseDate = dayjs(pieDate.value)
    // Morning: 04:00 - 13:00
    const morningStart = baseDate.hour(4).minute(0).second(0).millisecond(0).valueOf()
    const morningEnd = baseDate.hour(13).minute(0).second(0).millisecond(0).valueOf()
    
    // Afternoon: 13:00 - 18:00
    const afternoonStart = morningEnd
    const afternoonEnd = baseDate.hour(18).minute(0).second(0).millisecond(0).valueOf()
    
    // Evening: 18:00 - 04:00 (next day)
    const eveningStart = afternoonEnd
    const eveningEnd = baseDate.add(1, 'day').hour(4).minute(0).second(0).millisecond(0).valueOf()
    
    let m = 0, a = 0, e = 0
    
    if (data && data.length > 0) {
      data.forEach(item => {
        // Only count tasks with recordsTag = true (or equivalent logic from backend)
        if (!item.recordsTag) return

        const start = item.startTime
        const end = item.endTime
        
        // Overlap with Morning
        m += Math.max(0, Math.min(end, morningEnd) - Math.max(start, morningStart))
        
        // Overlap with Afternoon
        a += Math.max(0, Math.min(end, afternoonEnd) - Math.max(start, afternoonStart))
        
        // Overlap with Evening
        e += Math.max(0, Math.min(end, eveningEnd) - Math.max(start, eveningStart))
      })
    }
    
    timelineStats.value = {
      morning: m,
      afternoon: a,
      evening: e
    }

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
          item.duration,
          item.id // Index 4
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
    
    // Long press logic (200ms)
    let pressTimer = null;
    
    timelineChart.off('click');
    timelineChart.off('mousedown');
    timelineChart.off('mouseup');
    timelineChart.off('mouseout');

    timelineChart.on('mousedown', (params) => {
      if (props.user.canEditTime && params.data && params.data.value && params.data.value[4] !== 'CURRENT') {
        pressTimer = setTimeout(() => {
          // Hide tooltip to prevent overlap
          timelineChart.dispatchAction({
            type: 'hideTip'
          });
          openTimeEditDialog(params.data);
          pressTimer = null;
        }, 200);
      }
    });

    const cancelPress = () => {
      if (pressTimer) {
        clearTimeout(pressTimer);
        pressTimer = null;
      }
    };

    timelineChart.on('mouseup', cancelPress);
    timelineChart.on('mouseout', cancelPress);
  } catch (error) {
    console.error(error)
  }
}

const showTimeEditDialog = ref(false)
const showConfirmEditDialog = ref(false)
const editingTimeRecord = ref({ id: null, startTime: null, endTime: null, date: '' })
const confirmEditData = ref({ startTime: '', endTime: '', rawStartTime: 0, rawEndTime: 0 })

const openTimeEditDialog = (data) => {
  const start = data.value[1]
  const end = data.value[2]
  const dateStr = dayjs(start).format('YYYY-MM-DD')
  
  editingTimeRecord.value = {
    id: data.value[4],
    startTime: start, // timestamp
    endTime: end,   // timestamp
    date: dateStr,
    originalStartTime: start,
    originalEndTime: end
  }
  showTimeEditDialog.value = true
}

const preSaveTimeRecord = () => {
  // Calculate new timestamps based on the selected time and original date
  // Note: editingTimeRecord.startTime/endTime are bound to el-time-picker which returns Date object or timestamp depending on config
  // But here we use value-format="x" (timestamp) in the template for time-picker too?
  // Let's check the template update. I will use value-format="x" for time-picker to keep it simple.
  // However, time-picker with value-format="x" returns timestamp relative to 1970-01-01 usually? 
  // No, Element Plus time-picker with value-format="x" returns timestamp.
  // But wait, if I use time-picker, I want to preserve the date of the record.
  // If I use el-time-picker, it picks a time.
  
  // Better approach:
  // In openTimeEditDialog, I have full timestamps.
  // In the template, I will use el-time-picker bound to these timestamps.
  // When user changes time, the timestamp updates.
  // BUT el-time-picker might change the date part to today or 1970 if not careful?
  // Actually, if I bind a timestamp to el-time-picker, it converts to Date.
  // When it emits update, it emits a Date (or formatted string).
  // If I use value-format="x", it emits a timestamp.
  // Does it preserve the original date?
  // Usually el-time-picker operates on the time part of the provided date.
  // Let's assume it does. If not, we fix it.
  
  // Actually, to be safe, let's reconstruct the timestamp using the original date and the new time components.
  const originalDate = dayjs(editingTimeRecord.value.date)
  const newStart = dayjs(editingTimeRecord.value.startTime)
  const newEnd = dayjs(editingTimeRecord.value.endTime)
  
  // Combine original date with new time
  const finalStart = originalDate
    .hour(newStart.hour())
    .minute(newStart.minute())
    .second(newStart.second())
    .millisecond(0) // Reset ms to 0 for cleanliness
    .valueOf()
    
  // For end time, handle potential day crossing if needed?
  // The user said "cannot modify date".
  // If the original record was 23:00 - 01:00 (+1 day), the date displayed is likely the start date.
  // If user changes 01:00 to 02:00, it should probably still be +1 day?
  // But "cannot modify date" is strict.
  // If I force the date to be `originalDate`, then 01:00 becomes 01:00 on the start day (which is before start time).
  // This is tricky.
  // Let's assume for now we just take the time and apply it to the date of the *original timestamp*.
  // So if start was Day 1, we apply new time to Day 1.
  // If end was Day 2, we apply new time to Day 2.
  
  // So we need to keep track of the original full timestamps to know which "Day" they belong to.
  // But `editingTimeRecord.startTime` is being mutated by the picker.
  // I should store `originalStartTime` and `originalEndTime` in `openTimeEditDialog`.
  
  // Let's update `openTimeEditDialog` to store original timestamps separately if needed.
  // Actually, `editingTimeRecord` is reactive.
  // If I bind `v-model` to it, it changes.
  // So I can't know the original date from `editingTimeRecord.startTime` if the picker changed it (e.g. to 1970).
  
  // Let's rely on the fact that I will use `el-time-picker` without `value-format` (so it uses Date object)
  // OR use `value-format` but ensure I handle the date.
  
  // Let's try this:
  // 1. `openTimeEditDialog` saves `originalStartDay` and `originalEndDay` (YYYY-MM-DD strings).
  // 2. `preSaveTimeRecord` takes the time from the picker's value and combines with `originalStartDay`/`originalEndDay`.
  
  const startDay = dayjs(editingTimeRecord.value.originalStartTime).format('YYYY-MM-DD')
  const endDay = dayjs(editingTimeRecord.value.originalEndTime).format('YYYY-MM-DD')
  
  const sTime = dayjs(editingTimeRecord.value.startTime)
  const eTime = dayjs(editingTimeRecord.value.endTime)
  
  const finalStartTs = dayjs(startDay + ' ' + sTime.format('HH:mm:ss')).valueOf()
  const finalEndTs = dayjs(endDay + ' ' + eTime.format('HH:mm:ss')).valueOf()
  
  confirmEditData.value = {
    startTime: dayjs(finalStartTs).format('YYYY-MM-DD HH:mm:ss'),
    endTime: dayjs(finalEndTs).format('YYYY-MM-DD HH:mm:ss'),
    rawStartTime: finalStartTs,
    rawEndTime: finalEndTs
  }
  
  showConfirmEditDialog.value = true
}

const confirmSaveTimeRecord = async () => {
  try {
    await axios.put(`${API_URL}/records/${editingTimeRecord.value.id}`, {
      userId: props.user.id,
      startTime: confirmEditData.value.rawStartTime,
      endTime: confirmEditData.value.rawEndTime
    })
    showConfirmEditDialog.value = false
    showTimeEditDialog.value = false
    ElMessage.success('时间记录已更新')
    fetchTimelineData()
    fetchLineData()
    fetchPieData()
  } catch (error) {
    ElMessage.error('更新失败: ' + (error.response?.data?.message || error.message))
  }
}

const rankingStats = ref(null)

const fetchRankings = async () => {
  try {
    const res = await axios.get(`${API_URL}/stats/rankings`, {
      params: { 
        userId: props.user.id,
        startDate: lineDateRange.value ? lineDateRange.value[0] : null,
        endDate: lineDateRange.value ? lineDateRange.value[1] : null
      }
    })
    rankingStats.value = res.data
  } catch (error) {
    console.error('Failed to fetch rankings', error)
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

const showRecordDialog = ref(false)
const currentEditingTask = ref(null)
const tempRecordTagValue = ref(true)

const openRecordDialog = (task) => {
  currentEditingTask.value = task
  // If recordsTag is undefined/null, default to true (1) as per backend logic
  tempRecordTagValue.value = task.recordsTag !== false 
  showRecordDialog.value = true
}

const saveRecordTag = async () => {
  if (!currentEditingTask.value) return
  try {
    await axios.put(`${API_URL}/${currentEditingTask.value.id}/records-tag`, {
      recordsTag: tempRecordTagValue.value
    })
    // Update local state
    const task = tasks.value.find(t => t.id === currentEditingTask.value.id)
    if (task) {
      task.recordsTag = tempRecordTagValue.value
    }
    showRecordDialog.value = false
    ElMessage.success('设置已保存')
    // Refresh charts if needed
    fetchLineData()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

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
.ranking-stats-container {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.stat-item {
  text-align: center;
}
.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}
.stat-list {
  text-align: left;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}
.rank-num {
  color: #909399;
  margin-right: 5px;
}
.no-data {
  font-size: 14px;
  color: #C0C4CC;
  font-weight: normal;
}

.timeline-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.timeline-stats .stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-stats .label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.timeline-stats .value {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.date-range-container {
  display: flex;
  align-items: center;
}
.compact-date-picker {
  width: 135px !important;
}
.range-separator {
  margin: 0 5px;
}
@media (max-width: 768px) {
  /* Trend Header Specific Styles */
  .trend-header {
    display: flex;
    align-items: center; /* Vertically center the title */
  }
  .trend-header .date-range-container {
    display: flex;
    flex-direction: column; /* Stack dates vertically */
    align-items: center;
    margin-left: auto; /* Push to right */
  }
  .trend-header .compact-date-picker {
    width: 140px !important; /* Wider width for stacked items */
  }
  .trend-header .range-separator {
    margin: 2px 0;
  }

  /* General Mobile Adjustments */
  .compact-date-picker {
    width: 130px !important; /* Default width for single line (Statistics) */
  }
  
  /* Restore input styles but keep them slightly compact */
  :deep(.el-input__wrapper) {
    padding-left: 8px !important;
    padding-right: 8px !important;
  }
  :deep(.el-input__inner) {
    text-align: center;
    padding: 0 !important;
  }
}
</style>
