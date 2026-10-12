<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, legacyPageUrl } from '../api'
import AdminBackground from '../components/AdminBackground.vue'

const dashboard = ref({
  totalUsers: 0,
  activeSessions: 0,
  dailyVisits: 0,
  revenue: 0,
  visits: [],
  userDistribution: [0, 0, 0],
  logs: [],
  users: [],
  adminProfile: null,
  transactions: { paid: [], shipped: [], completed: [], cancelled: [] },
})
const currentTab = ref('dashboard')
const loading = ref(true)
const error = ref('')
const chartError = ref('')
const globalMessage = ref('')
const department = ref('')
const settingsMessage = ref('')
const settingsMessageType = ref('')
const savingSettings = ref(false)
const transactionsOpen = ref(false)
const userModalOpen = ref(false)
const selectedUser = ref(null)
const userProfile = ref(null)
const userProfileError = ref('')
const userProfileLoading = ref(false)

const transactionGroups = [
  { key: 'paid', label: 'PAID Orders', titleClass: 'title-paid', empty: 'No PAID orders found.' },
  { key: 'shipped', label: 'SHIPPED Orders', titleClass: 'title-shipped', empty: 'No SHIPPED orders found.' },
  { key: 'completed', label: 'COMPLETED Orders', titleClass: 'title-completed', empty: 'No COMPLETED orders found.' },
  { key: 'cancelled', label: 'CANCELLED Orders', titleClass: 'title-cancelled', empty: 'No CANCELLED orders found.' },
]

const userProfileRows = computed(() => {
  const profile = userProfile.value || {}
  if (selectedUser.value?.role === 'CUSTOMER') {
    return [
      ['Full Name', profile.fullName || '-'],
      ['Email', profile.email || '-'],
      ['Phone', profile.phone || '-'],
      ['Default Address', profile.address || 'No address set'],
    ]
  }
  if (selectedUser.value?.role === 'MERCHANT') {
    return [
      ['Store Name', profile.storeName || '-'],
      ['License', profile.license || '-'],
      ['Contact', profile.contact || '-'],
    ]
  }
  return [
    ['Department', profile.department || '-'],
    ['Level', profile.level ?? '-'],
  ]
})

let chartScript
let trafficChart
let userChart

function loadChartJs() {
  if (window.Chart) return Promise.resolve(window.Chart)
  if (!chartScript) {
    chartScript = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = 'https://cdn.jsdelivr.net/npm/chart.js'
      script.onload = () => resolve(window.Chart)
      script.onerror = () => reject(new Error('Chart.js could not be loaded.'))
      document.head.appendChild(script)
    })
  }
  return chartScript
}

async function drawCharts() {
  try {
    const Chart = await loadChartJs()
    await nextTick()
    trafficChart?.destroy()
    userChart?.destroy()
    trafficChart = new Chart(document.getElementById('trafficChart').getContext('2d'), {
      type: 'line',
      data: {
        labels: dashboard.value.visits.map((visit) => visit.label),
        datasets: [{
          label: 'Visitors',
          data: dashboard.value.visits.map((visit) => visit.count),
          borderColor: '#FF3B30',
          backgroundColor: 'rgba(255, 59, 48, 0.1)',
          tension: 0.4,
          fill: true,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true } },
      },
    })
    userChart = new Chart(document.getElementById('userChart').getContext('2d'), {
      type: 'doughnut',
      data: {
        labels: ['Customers', 'Merchants', 'Admins'],
        datasets: [{
          data: dashboard.value.userDistribution,
          backgroundColor: ['#FF9500', '#FFCC00', '#FF3B30'],
          borderWidth: 0,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' } },
      },
    })
  } catch {
    chartError.value = 'Charts could not be loaded.'
  }
}

async function loadDashboard() {
  try {
    const session = await getCurrentUser()
    if (!session.authenticated || session.role !== 'ADMIN') {
      window.location.replace(legacyPageUrl('/login'))
      return
    }

    const response = await fetch(`${legacyBase}/admin/dashboard`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace(legacyPageUrl('/login'))
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load the admin dashboard.')
    dashboard.value = result.data
    department.value = result.data.adminProfile?.department || ''
    globalMessage.value = result.data.globalMessage || ''
    document.title = 'Admin Dashboard - PrimeGo'
    loading.value = false
    await drawCharts()
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load the admin dashboard.'
  } finally {
    loading.value = false
  }
}

async function submitSettings(event, action) {
  const body = new URLSearchParams(new FormData(event.currentTarget))
  body.set('action', action)
  savingSettings.value = true
  settingsMessage.value = ''
  try {
    const response = await fetch(`${legacyBase}/admin/dashboard`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      credentials: 'include',
      body,
    })
    const result = await response.json().catch(() => null)
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to save settings.')
    settingsMessage.value = result.data.settingsMessage
    settingsMessageType.value = result.data.settingsMessageType
    department.value = result.data.department || ''
    currentTab.value = 'settings'
    if (action === 'changePassword' && settingsMessageType.value === 'success') event.currentTarget.reset()
  } catch (saveError) {
    settingsMessage.value = saveError.message || 'Unable to save settings.'
    settingsMessageType.value = 'error'
  } finally {
    savingSettings.value = false
  }
}

async function viewUserDetails(user) {
  selectedUser.value = user
  userProfile.value = null
  userProfileError.value = ''
  userProfileLoading.value = true
  userModalOpen.value = true
  try {
    const query = new URLSearchParams({ userId: user.id, role: user.role })
    const response = await fetch(`${legacyBase}/admin/api/user-details?${query}`, { credentials: 'include' })
    const profile = await response.json().catch(() => null)
    if (!response.ok) throw new Error(profile?.error || profile?.msg || 'Unable to load user details.')
    userProfile.value = profile
    if (profile?.error) userProfileError.value = profile.error
  } catch (loadError) {
    userProfileError.value = loadError.message || 'Unable to load user details.'
  } finally {
    userProfileLoading.value = false
  }
}

function closeUserModal() {
  userModalOpen.value = false
}

function closeTransactionsModal() {
  transactionsOpen.value = false
}

function userInitial(username) {
  return username?.charAt(0)?.toUpperCase() || '?'
}

function amount(value) {
  return `RM ${value ?? '0.00'}`
}

onMounted(loadDashboard)
onBeforeUnmount(() => {
  trafficChart?.destroy()
  userChart?.destroy()
})
</script>

<template>
  <div class="admin-dashboard-page">
    <AdminBackground />

    <aside class="sidebar">
      <h2>PrimeGo Admin</h2>
      <button
        v-for="tab in [
          { id: 'dashboard', label: 'Dashboard' },
          { id: 'users', label: 'User Management' },
          { id: 'settings', label: 'Settings' },
        ]"
        :key="tab.id"
        class="nav-item"
        :class="{ active: currentTab === tab.id }"
        type="button"
        @click="currentTab = tab.id"
      >
        <span>{{ tab.label }}</span>
      </button>
      <a class="nav-item" href="/admin/product/review/list"><span>Product Review</span></a>
      <a class="nav-item" :href="legacyPageUrl('/admin/wallet/admin_approval.jsp')"><span>Funds Approval</span></a>
      <a class="nav-item" href="/admin/merchant/create"><span>Merchant Create</span></a>

      <div class="sidebar-footer">
        <a class="btn-logout back-home" :href="legacyPageUrl('/index.jsp')">Back to Home</a>
        <a class="btn-logout" :href="legacyPageUrl('/logout')">Logout</a>
      </div>
    </aside>

    <main class="main-content">
      <div v-if="loading" class="glass-panel page-message" aria-live="polite">Loading dashboard...</div>
      <div v-else-if="error" class="glass-panel page-message error-message" role="alert">{{ error }}</div>
      <template v-else>
        <section v-show="currentTab === 'dashboard'" class="tab-content">
          <div class="header-section"><h1>Dashboard Overview</h1></div>
          <div class="metrics-grid">
            <div class="glass-panel metric-card">
              <span class="metric-title">Total Users</span>
              <span class="metric-value">{{ dashboard.totalUsers }}</span>
              <span class="metric-trend">↑ 12% vs last week</span>
            </div>
            <div class="glass-panel metric-card">
              <span class="metric-title">Active Sessions</span>
              <span class="metric-value">{{ dashboard.activeSessions }}</span>
              <span class="metric-trend">Currently Online</span>
            </div>
            <div class="glass-panel metric-card">
              <span class="metric-title">Daily Visits</span>
              <span class="metric-value">{{ dashboard.dailyVisits }}</span>
              <span class="metric-trend">↑ 5% vs yesterday</span>
            </div>
            <button class="glass-panel metric-card transaction-metric" type="button" @click="transactionsOpen = true">
              <span class="metric-title">Total Transactions</span>
              <span class="metric-value">{{ dashboard.revenue }}</span>
              <span class="metric-trend">↑ 8% vs last month</span>
            </button>
          </div>

          <div class="charts-grid">
            <div class="glass-panel chart-panel">
              <h3>Traffic Overview</h3>
              <div class="chart-container"><canvas id="trafficChart"></canvas></div>
              <p v-if="chartError" class="chart-error">{{ chartError }}</p>
            </div>
            <div class="glass-panel chart-panel">
              <h3>User Distribution</h3>
              <div class="chart-container"><canvas id="userChart"></canvas></div>
            </div>
          </div>

          <div class="glass-panel logs-section">
            <h3>System Logs</h3>
            <div class="table-container">
              <table class="data-table">
                <thead><tr><th>Level</th><th>Message</th><th>Time</th></tr></thead>
                <tbody>
                  <tr v-if="dashboard.logs.length === 0"><td colspan="3" class="empty-row">No system logs found.</td></tr>
                  <tr v-for="(log, index) in dashboard.logs" :key="`${log.time}-${index}`">
                    <td><span :class="['log-level', `level-${log.level}`]">{{ log.level }}</span></td>
                    <td>{{ log.message }}</td>
                    <td>{{ log.time }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <section v-show="currentTab === 'users'" class="tab-content">
          <div class="header-section"><h1>User Management</h1></div>
          <div class="glass-panel">
            <div class="table-container">
              <table class="data-table">
                <thead>
                  <tr><th>ID</th><th>Username</th><th>Role</th><th>Status</th><th>Created At</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  <tr v-if="dashboard.users.length === 0"><td colspan="6" class="empty-row">No users found.</td></tr>
                  <tr v-for="user in dashboard.users" :key="user.id">
                    <td>#{{ user.id }}</td>
                    <td>
                      <div class="username-cell">
                        <div class="username-avatar">{{ userInitial(user.username) }}</div>
                        {{ user.username }}
                      </div>
                    </td>
                    <td><span :class="['role-badge', `role-${user.role}`]">{{ user.role }}</span></td>
                    <td><span :class="user.status === 1 ? 'status-active' : 'status-inactive'">● {{ user.status === 1 ? 'Active' : 'Inactive' }}</span></td>
                    <td>{{ user.createdAt }}</td>
                    <td class="actions-cell">
                      <button class="detail-button" type="button" @click="viewUserDetails(user)">Detail</button>
                      <button class="delete-button" type="button">Delete</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <section v-show="currentTab === 'settings'" class="tab-content">
          <div class="header-section"><h1>System Settings</h1></div>
          <div v-if="settingsMessage" :class="['settings-message', settingsMessageType]" role="status">{{ settingsMessage }}</div>

          <div class="glass-panel settings-panel">
            <h3>Profile Settings</h3>
            <form @submit.prevent="submitSettings($event, 'updateProfile')">
              <div class="form-field">
                <label for="admin-department">Department</label>
                <input id="admin-department" v-model="department" type="text" name="department">
              </div>
              <button class="settings-button profile-button" type="submit" :disabled="savingSettings">Save Changes</button>
            </form>
          </div>

          <div class="glass-panel settings-panel">
            <h3>Security Settings</h3>
            <form @submit.prevent="submitSettings($event, 'changePassword')">
              <div class="password-grid">
                <div class="form-field full-width">
                  <label for="current-password">Current Password</label>
                  <input id="current-password" type="password" name="currentPassword" autocomplete="current-password" required>
                </div>
                <div class="form-field">
                  <label for="new-password">New Password</label>
                  <input id="new-password" type="password" name="newPassword" autocomplete="new-password" required>
                </div>
                <div class="form-field">
                  <label for="confirm-password">Confirm New Password</label>
                  <input id="confirm-password" type="password" name="confirmPassword" autocomplete="new-password" required>
                </div>
              </div>
              <button class="settings-button password-button" type="submit" :disabled="savingSettings">Change Password</button>
            </form>
          </div>
        </section>
      </template>
    </main>

    <div v-if="userModalOpen" class="modal-overlay" @click.self="closeUserModal">
      <section class="modal-content user-modal" role="dialog" aria-modal="true" aria-labelledby="user-modal-title">
        <button class="close-btn" type="button" aria-label="Close user details" @click="closeUserModal">&times;</button>
        <h2 id="user-modal-title">User Details</h2>
        <div v-if="selectedUser" class="account-info">
          <h3>Account Info</h3>
          <div class="detail-row"><strong>User ID:</strong><span>#{{ selectedUser.id }}</span></div>
          <div class="detail-row"><strong>Username:</strong><span>{{ selectedUser.username }}</span></div>
          <div class="detail-row"><strong>Role:</strong><span>{{ selectedUser.role }}</span></div>
          <div class="detail-row"><strong>Status:</strong><span :class="selectedUser.status === 1 ? 'status-active' : 'status-inactive'">{{ selectedUser.status === 1 ? 'Active' : 'Inactive' }}</span></div>
          <div class="detail-row"><strong>Created At:</strong><span>{{ selectedUser.createdAt }}</span></div>
        </div>
        <div class="extended-info" aria-live="polite">
          <p v-if="userProfileLoading" class="muted">Loading extended profile details...</p>
          <p v-else-if="userProfileError" class="profile-error">{{ userProfileError }}</p>
          <template v-else>
            <h3>{{ selectedUser?.role }} Profile</h3>
            <div v-for="[label, value] in userProfileRows" :key="label" class="detail-row" :class="{ 'address-row': label === 'Default Address' }">
              <strong>{{ label }}:</strong><span>{{ value }}</span>
            </div>
          </template>
        </div>
      </section>
    </div>

    <div v-if="transactionsOpen" class="modal-overlay" @click.self="closeTransactionsModal">
      <section class="modal-content modal-lg" role="dialog" aria-modal="true" aria-labelledby="transactions-title">
        <button class="close-btn" type="button" aria-label="Close transaction details" @click="closeTransactionsModal">&times;</button>
        <div class="modal-header"><h2 id="transactions-title">Total Transactions Detail</h2></div>
        <section v-for="group in transactionGroups" :key="group.key" class="transaction-section">
          <h3 :class="['section-title', group.titleClass]">{{ group.label }}</h3>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Order ID</th><th>Customer ID</th><th>Amount</th>
                  <th v-if="group.key === 'shipped'">Tracking</th><th>Date</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="dashboard.transactions[group.key].length === 0">
                  <td :colspan="group.key === 'shipped' ? 5 : 4" class="empty-row">{{ group.empty }}</td>
                </tr>
                <tr v-for="order in dashboard.transactions[group.key]" :key="order.ordersId">
                  <td>#{{ order.ordersId }}</td><td>User #{{ order.customerId }}</td><td>{{ amount(order.totalAmount) }}</td>
                  <td v-if="group.key === 'shipped'">{{ order.trackingNumber }}</td><td>{{ order.createdAt }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </section>
    </div>

    <div v-if="globalMessage" class="modal-overlay global-overlay" @click.self="globalMessage = ''">
      <section class="modal-content global-modal" role="dialog" aria-modal="true" aria-labelledby="global-title">
        <button class="close-btn" type="button" aria-label="Close message" @click="globalMessage = ''">&times;</button>
        <div class="success-mark"><i class="ri-checkbox-circle-fill"></i></div>
        <h2 id="global-title">Success!</h2>
        <p>{{ globalMessage }}</p>
        <button class="global-ok" type="button" @click="globalMessage = ''">OK, Got it</button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard-page,.admin-dashboard-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.admin-dashboard-page { position: relative; z-index: 1; display: flex; min-height: 100vh; color: #333; }
.sidebar { position: fixed; z-index: 2; top: 0; left: 0; display: flex; flex-direction: column; width: 250px; height: 100vh; padding: 30px 20px; border-right: 1px solid rgba(255,255,255,.6); background: rgba(255,255,255,.8); backdrop-filter: blur(20px); }
.sidebar h2 { margin: 0 0 40px; color: #d63031; font-size: 1.5rem; text-align: center; }
.nav-item { display: flex; align-items: center; width: 100%; margin-bottom: 10px; padding: 15px 20px; border: 0; border-radius: 15px; background: transparent; color: #555; cursor: pointer; font-size: 1rem; font-weight: 600; text-align: left; text-decoration: none; transition: .3s; }
.nav-item:hover,.nav-item.active { background: linear-gradient(45deg,#FF3B30,#FF9500); box-shadow: 0 5px 15px rgba(255,59,48,.3); color: white; }
.nav-item span { margin-left: 10px; }
.sidebar-footer { margin-top: auto; }
.btn-logout { display: block; width: 100%; margin-bottom: 10px; padding: 12px; border-radius: 15px; background: #333; color: white; text-align: center; text-decoration: none; transition: .3s; }
.btn-logout:hover { background: #555; }
.back-home { background: #555; }
.main-content { flex-grow: 1; width: calc(100% - 250px); min-width: 0; margin-left: 250px; padding: 40px; }
.header-section { display: flex; align-items: center; justify-content: space-between; margin-bottom: 30px; }
.header-section h1 { display: inline-block; padding: 10px 30px; border: 1px solid rgba(255,255,255,.5); border-radius: 50px; background: rgba(255,255,255,.6); box-shadow: 0 4px 15px rgba(0,0,0,.05); color: #d63031; font-size: 2.5rem; backdrop-filter: blur(10px); }
.tab-content { animation: fadeIn .5s; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.glass-panel { margin-bottom: 30px; padding: 25px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); box-shadow: 0 8px 32px rgba(0,0,0,.1); backdrop-filter: blur(20px); }
.metrics-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(250px,1fr)); gap: 20px; margin-bottom: 30px; }
.metric-card { display: flex; flex-direction: column; align-items: flex-start; min-width: 0; text-align: left; }
.transaction-metric { width: 100%; border: 0; color: inherit; cursor: pointer; }
.metric-title { margin-bottom: 5px; color: #666; font-size: .9rem; }
.metric-value { color: #2d3436; font-size: 2rem; font-weight: 700; }
.metric-trend { color: #27ae60; font-size: .8rem; font-weight: 600; }
.charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 30px; }
.chart-panel h3,.logs-section h3 { margin: 0 0 14px; }
.chart-container { position: relative; width: 100%; height: 300px; }
.chart-error { margin-top: 8px; color: #c0392b; font-size: .85rem; }
.table-container { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th,.data-table td { padding: 15px; border-bottom: 1px solid rgba(0,0,0,.05); text-align: left; vertical-align: middle; }
.data-table th { color: #666; font-weight: 600; white-space: nowrap; }
.data-table tr:last-child td { border-bottom: none; }
.log-level { padding: 4px 8px; border-radius: 4px; font-size: .8rem; font-weight: 600; }
.level-INFO { background: #e3f2fd; color: #1976d2; }
.level-WARN { background: #fff3e0; color: #f57c00; }
.level-ERROR { background: #ffebee; color: #d32f2f; }
.role-badge { display: inline-block; padding: 5px 10px; border-radius: 15px; font-size: .85rem; font-weight: 600; }
.role-ADMIN { background: #ffebee; color: #d32f2f; }
.role-MERCHANT { background: #fff8e1; color: #fbc02d; }
.role-CUSTOMER { background: #e3f2fd; color: #1976d2; }
.username-cell { display: flex; align-items: center; min-width: 150px; }
.username-avatar { display: flex; flex: 0 0 30px; align-items: center; justify-content: center; width: 30px; height: 30px; margin-right: 10px; border-radius: 50%; background: #eee; color: #555; font-weight: 700; }
.status-active { color: #27ae60; font-weight: 600; white-space: nowrap; }
.status-inactive { color: #d63031; font-weight: 600; white-space: nowrap; }
.actions-cell { white-space: nowrap; }
.detail-button,.delete-button { margin-left: 5px; padding: 5px 10px; border: 1px solid #0984e3; border-radius: 5px; background: #e3f2fd; color: #0984e3; cursor: pointer; }
.delete-button { border-color: #ffcccc; background: #fff5f5; color: #d63031; }
.empty-row { padding: 22px !important; color: #666; text-align: center !important; }
.settings-message { margin-bottom: 20px; padding: 15px; border: 1px solid; border-radius: 10px; }
.settings-message.success { border-color: #c8e6c9; background: #e8f5e9; color: #2e7d32; }
.settings-message.error { border-color: #ffcdd2; background: #ffebee; color: #c62828; }
.settings-panel h3 { margin: 0 0 20px; color: #333; }
.form-field { min-width: 0; margin-bottom: 20px; }
.form-field label { display: block; margin-bottom: 8px; color: #666; font-weight: 500; }
.form-field input { width: 100%; min-height: 44px; padding: 12px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; outline: none; background: rgba(255,255,255,.8); color: #333; }
.form-field input:focus { border-color: #d63031; box-shadow: 0 0 0 3px rgba(214,48,49,.12); }
.password-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.full-width { grid-column: span 2; }
.settings-button { padding: 12px 30px; border: 0; border-radius: 8px; background: #333; color: white; cursor: pointer; font-weight: 600; }
.password-button { background: #d63031; }
.settings-button:disabled { cursor: wait; opacity: .65; }
.page-message { margin-top: 0; }
.error-message { color: #c62828; }
.modal-overlay { position: fixed; z-index: 10; inset: 0; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(0,0,0,.5); backdrop-filter: blur(5px); }
.modal-content { position: relative; width: min(400px,90vw); max-height: 85vh; overflow-y: auto; padding: 30px; border: 1px solid rgba(255,255,255,.5); border-radius: 20px; background: rgba(255,255,255,.95); box-shadow: 0 20px 50px rgba(0,0,0,.3); animation: modalPop .3s cubic-bezier(.34,1.56,.64,1); }
@keyframes modalPop { from { transform: scale(.8); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.modal-content h2 { margin: 0 0 20px; color: #333; }
.close-btn { position: absolute; top: 20px; right: 25px; display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: 0; border-radius: 50%; background: rgba(0,0,0,.05); color: #555; cursor: pointer; font-size: 1.2rem; transition: .2s; }
.close-btn:hover { transform: rotate(90deg); background: #ff5e55; color: white; }
.account-info { padding-bottom: 12px; border-bottom: 2px solid #eee; }
.account-info h3,.extended-info h3 { margin: 0 0 10px; color: #333; font-size: 1rem; }
.extended-info { margin-top: 15px; }
.detail-row { display: grid; grid-template-columns: 120px minmax(0,1fr); gap: 8px; margin-bottom: 7px; line-height: 1.45; overflow-wrap: anywhere; }
.detail-row strong { color: #555; }
.address-row { display: block; margin-top: 10px; padding: 10px; border-radius: 8px; background: #f9f9f9; }
.address-row strong { display: block; margin-bottom: 5px; }
.muted { color: #666; font-style: italic; }
.profile-error { display: inline-block; padding: 5px; border-radius: 4px; background: #e74c3c; color: white; overflow-wrap: anywhere; }
.modal-lg { width: min(900px,95vw); }
.modal-header { margin-bottom: 25px; padding-bottom: 15px; border-bottom: 1px solid rgba(0,0,0,.05); }
.modal-header h2 { margin: 0; color: #2c3e50; }
.transaction-section { margin-bottom: 25px; padding: 20px; border: 1px solid rgba(0,0,0,.02); border-radius: 15px; background: white; box-shadow: 0 4px 15px rgba(0,0,0,.03); }
.transaction-section:last-child { margin-bottom: 0; }
.section-title { display: flex; align-items: center; margin: 0 0 15px; padding-bottom: 8px; border-bottom: 2px solid; font-size: 1.1rem; font-weight: 600; }
.section-title::before { display: inline-block; width: 4px; height: 18px; margin-right: 10px; border-radius: 2px; content: ''; }
.title-paid { border-bottom-color: rgba(39,174,96,.1); color: #27ae60; }
.title-paid::before { background: #27ae60; }
.title-shipped { border-bottom-color: rgba(41,128,185,.1); color: #2980b9; }
.title-shipped::before { background: #2980b9; }
.title-completed { border-bottom-color: rgba(142,68,173,.1); color: #8e44ad; }
.title-completed::before { background: #8e44ad; }
.title-cancelled { border-bottom-color: rgba(192,57,43,.1); color: #c0392b; }
.title-cancelled::before { background: #c0392b; }
.global-overlay { z-index: 11; }
.global-modal { width: min(350px,90vw); text-align: center; }
.success-mark { margin-bottom: 15px; color: #2ecc71; font-size: 3rem; }
.success-mark i { font-family: 'remixicon' !important; font-style: normal; }
.global-modal h2 { margin-bottom: 10px; }
.global-modal p { margin-bottom: 20px; color: #666; overflow-wrap: anywhere; }
.global-ok { padding: 10px 25px; border: 0; border-radius: 10px; background: #2ecc71; color: white; cursor: pointer; font-weight: 600; }
@media (max-width: 800px) {
  .admin-dashboard-page { display: block; }
  .sidebar { position: relative; width: 100%; height: auto; padding: 16px; flex-direction: row; flex-wrap: wrap; }
  .sidebar h2 { width: 100%; margin: 0 0 10px; }
  .nav-item { width: auto; margin: 0 4px 6px 0; padding: 10px 12px; font-size: .9rem; }
  .sidebar-footer { display: flex; width: 100%; gap: 8px; margin-top: 8px; }
  .btn-logout { margin: 0; }
  .main-content { width: 100%; margin: 0; padding: 24px 18px; }
  .charts-grid { grid-template-columns: minmax(0,1fr); }
  .header-section h1 { padding: 9px 22px; font-size: 1.8rem; }
}
@media (max-width: 520px) {
  .metrics-grid { grid-template-columns: minmax(0,1fr); gap: 10px; }
  .glass-panel { padding: 18px; }
  .password-grid { grid-template-columns: minmax(0,1fr); gap: 0; }
  .full-width { grid-column: auto; }
  .transaction-section { padding: 14px; }
  .modal-content { padding: 24px 18px; }
  .close-btn { top: 16px; right: 16px; }
  .detail-row { grid-template-columns: 105px minmax(0,1fr); }
}
</style>
