<template>
  <div class="idiom-list-container">
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索成语..."
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
        style="width: 300px; margin-right: 10px;"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      
      <el-button-group>
        <el-button type="primary" plain @click="fetchRandom(10)">随机10个</el-button>
        <el-button type="primary" plain @click="fetchRandom(20)">随机20个</el-button>
        <el-button type="primary" plain @click="fetchRandom(30)">随机30个</el-button>
      </el-button-group>
    </div>

    <div class="idiom-cards">
      <div 
        v-for="(item, index) in idioms" 
        :key="item.id" 
        class="idiom-card"
        @click="toggleExpand(item.id)"
      >
        <div class="idiom-header">
          <span class="idiom-index">{{ (currentPage - 1) * pageSize + index + 1 }}、</span>
          <span class="idiom-word">{{ item.word }}</span>
          <el-tag 
            v-if="item.partOfSpeech" 
            :type="getTagType(item.partOfSpeech)" 
            size="small" 
            effect="light" 
            class="idiom-tag"
          >
            {{ item.partOfSpeech }}
          </el-tag>
          <span class="idiom-colon">：</span>
          <span class="idiom-definition">{{ item.definition }}</span>
        </div>
        
        <div v-if="expandedIds.has(item.id)" class="idiom-details" @click.stop>
          <p v-if="item.synonyms">
            <strong>近义词：</strong>
            <el-tag
              v-for="(tag, idx) in splitWords(item.synonyms)"
              :key="'syn-' + idx"
              type="primary"
              effect="light"
              class="word-tag"
              size="small"
            >
              {{ tag }}
            </el-tag>
          </p>
          <p v-if="item.antonyms">
            <strong>反义词：</strong>
            <el-tag
              v-for="(tag, idx) in splitWords(item.antonyms)"
              :key="'ant-' + idx"
              type="danger"
              effect="light"
              class="word-tag"
              size="small"
            >
              {{ tag }}
            </el-tag>
          </p>
          <p v-if="item.example"><strong>例句：</strong>{{ item.example }}</p>
        </div>
      </div>
    </div>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[30, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import axios from 'axios'
import { API_BASE_URL } from '../config'

const idioms = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(30)
const total = ref(0)
const expandedIds = reactive(new Set())
const isRandomMode = ref(false)

const fetchIdioms = async () => {
  try {
    isRandomMode.value = false
    const response = await axios.get(`${API_BASE_URL}/idioms`, {
      params: { 
        keyword: searchKeyword.value,
        page: currentPage.value - 1, // Spring Data JPA uses 0-based page index
        size: pageSize.value
      }
    })
    // Handle Page<ChineseDictionary> response structure
    if (response.data && response.data.content) {
      idioms.value = response.data.content
      total.value = response.data.totalElements
    } else {
      // Fallback if backend returns list directly (should not happen after update)
      idioms.value = response.data
      total.value = response.data.length
    }
  } catch (error) {
    console.error('Failed to fetch idioms:', error)
  }
}

const fetchRandom = async (limit) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/idioms/random`, {
      params: { limit }
    })
    idioms.value = response.data
    total.value = response.data.length
    isRandomMode.value = true
    currentPage.value = 1
    pageSize.value = limit
  } catch (error) {
    console.error('Failed to fetch random idioms:', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchIdioms()
}

const handleSizeChange = (val) => {
  if (isRandomMode.value) {
    // If in random mode, changing size essentially means fetching new random set or just resetting to normal mode?
    // Let's reset to normal mode for consistency or re-fetch random if user wants?
    // Usually pagination controls shouldn't be used in random mode, or should just act as "refresh".
    // For simplicity, let's switch back to normal list if size changes via pagination
    pageSize.value = val
    fetchIdioms()
  } else {
    pageSize.value = val
    fetchIdioms()
  }
}

const handleCurrentChange = (val) => {
  if (isRandomMode.value) {
    // Random mode doesn't really have pages, but if we want to support "next batch", we could call fetchRandom again.
    // But standard pagination logic implies offset. 
    // Let's just switch back to normal list if page changes, as random list is a one-off snapshot.
    currentPage.value = val
    fetchIdioms()
  } else {
    currentPage.value = val
    fetchIdioms()
  }
}

const toggleExpand = (id) => {
  if (expandedIds.has(id)) {
    expandedIds.delete(id)
  } else {
    expandedIds.add(id)
  }
}

const getTagType = (pos) => {
  if (!pos) return 'info' // Gray for null/empty
  if (pos.includes('褒义')) return 'success' // Green
  if (pos.includes('贬义')) return 'danger' // Red
  if (pos.includes('中性')) return 'info' // Gray
  return 'info'
}

const splitWords = (text) => {
  if (!text) return []
  // Split by Chinese or English comma
  return text.split(/,|，/).map(s => s.trim()).filter(s => s)
}

onMounted(() => {
  fetchIdioms()
})
</script>

<style scoped>
.idiom-list-container {
  padding: 20px;
  background-color: #f5f7fa; /* Light gray background for contrast */
  min-height: 100vh;
}

.search-bar {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.idiom-cards {
  max-width: 1200px;
  margin: 0 auto;
}

.idiom-card {
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 15px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.idiom-card:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
}

.idiom-header {
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
}

.idiom-index {
  font-weight: bold;
  color: #303133;
}

.idiom-word {
  font-weight: bold;
  font-size: 18px;
  color: #303133;
  margin-right: 8px;
}

.idiom-tag {
  margin-right: 5px;
  vertical-align: text-bottom;
}

.idiom-colon {
  margin-right: 5px;
  font-weight: bold;
}

.idiom-definition {
  color: #606266;
}

.idiom-details {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #ebeef5;
  background-color: #fcfcfc;
  padding: 15px;
  border-radius: 4px;
}

.idiom-details p {
  margin: 8px 0;
  line-height: 1.6;
  color: #606266;
}

.word-tag {
  margin-right: 8px;
  margin-bottom: 5px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  background-color: #fff;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}
</style>
