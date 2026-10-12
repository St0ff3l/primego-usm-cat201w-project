<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase } from '../api'
import MerchantBackground from '../components/MerchantBackground.vue'
import MerchantSidebar from '../components/MerchantSidebar.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const currentUser = ref(null)
const dashboard = ref(null)
const loading = ref(true)
const error = ref('')

async function loadDashboard() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!['MERCHANT', 'ADMIN'].includes(user.role)) {
      window.location.replace('/public/login.jsp')
      return
    }
    currentUser.value = user

    const response = await fetch(legacyBase + '/api/merchant/dashboard', {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load seller center data.')
    dashboard.value = result.data
    document.title = 'Seller Center - PrimeGo'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load seller center data.'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="merchant-page">
    <PrimegoHeader :current-user="currentUser" :merchant-route="true" />
    <MerchantBackground />
    <div class="layout-container">
      <MerchantSidebar active="dashboard" />
      <main class="main-content">
        <div v-if="loading" class="dashboard-state" aria-live="polite">Loading seller center...</div>
        <div v-else-if="error" class="dashboard-error" role="alert">{{ error }}</div>
        <template v-else-if="dashboard">
          <section class="shop-header-card">
            <div class="shop-profile">
              <div class="shop-avatar">S</div>
              <div class="shop-meta"><h1>{{ dashboard.username }}'s Store</h1></div>
            </div>
            <a class="shop-btn" href="/merchant/product/product_management.jsp">Manage Products</a>
          </section>

          <section class="metrics-bar" aria-label="Store order and inventory summary">
            <a class="metric-card" href="/merchant/order/order_management?filter=to_ship"><div class="metric-val">{{ dashboard.toShipCount }}</div><div class="metric-label">To Ship</div></a>
            <a class="metric-card" href="/merchant/order/order_management?filter=shipped"><div class="metric-val">{{ dashboard.shippedCount }}</div><div class="metric-label">Shipped</div></a>
            <a class="metric-card" href="/merchant/order/order_management?filter=completed"><div class="metric-val">{{ dashboard.completedCount }}</div><div class="metric-label">Completed</div></a>
            <a class="metric-card" href="/merchant/product/product_management.jsp"><div class="metric-val">{{ dashboard.lowStockCount }}</div><div class="metric-label">Low Stock</div></a>
          </section>

          <section class="todo-section">
            <div class="panel">
              <div class="panel-header"><h2 class="panel-title">Quick Actions</h2></div>
              <div class="todo-item stock-warning">
                <div class="todo-left">
                  <div class="todo-icon"><i class="ri-error-warning-line"></i></div>
                  <div class="todo-text">
                    <h3>Stock Warning</h3>
                    <p>{{ dashboard.lowStockCount }} items are low in stock (≤ 5)</p>
                    <div v-if="dashboard.lowStockItems.length" class="low-stock-list">
                      <div v-for="product in dashboard.lowStockItems" :key="product.productId" class="low-stock-item">
                        <div class="low-stock-left">
                          <a class="low-stock-name" :href="'/merchant/product/product_edit.jsp?id=' + product.productId" :title="'Edit ' + product.name">{{ product.name }}</a>
                          <div class="low-stock-meta">Stock: <span :class="['low-stock-stock', { 'stock-zero': product.stock <= 0 }]">{{ product.stock }}</span></div>
                        </div>
                        <div class="low-stock-actions"><a class="btn-restock-mini" :href="'/merchant/product/product_edit.jsp?id=' + product.productId"><i class="ri-edit-line"></i> Restock</a></div>
                      </div>
                      <div v-if="dashboard.hasMoreLowStock" class="more-stock">...and more</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="panel">
              <div class="panel-header"><h2 class="panel-title">Shortcuts</h2></div>
              <div class="todo-item shortcut-item">
                <div class="todo-left">
                  <div class="todo-icon"><i class="ri-add-circle-line"></i></div>
                  <div class="todo-text"><h3>Publish Product</h3><p>Add a new product to your store</p></div>
                </div>
                <a href="/merchant/product/publish.jsp" class="todo-action">Publish</a>
              </div>
            </div>
          </section>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.merchant-page { position: relative; min-height: 100vh; overflow-x: hidden; padding-top: 90px; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #2d3436; --primary: #ff9500; --primary-hover: #e68600; --secondary: #ff5e55; --text-dark: #2d3436; --text-gray: #636e72; --glass-bg: rgba(255,255,255,.95); --card-radius: 16px; --card-shadow: 0 10px 30px rgba(0,0,0,.05); --border-color: #dfe6e9; }
.layout-container { position: relative; z-index: 1; display: grid; max-width: 1400px; grid-template-columns: 240px minmax(0,1fr); align-items: start; gap: 30px; margin: 30px auto; padding: 0 20px; }
.main-content { display: flex; min-width: 0; flex-direction: column; gap: 25px; }
.dashboard-state,.dashboard-error { padding: 28px; border-radius: 16px; background: rgba(255,255,255,.9); color: #636e72; text-align: center; }
.dashboard-error { background: #f8d7da; color: #721c24; }
.shop-header-card { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 30px; padding: 30px; border: 1px solid rgba(255,255,255,.9); border-radius: 16px; background: rgba(255,255,255,.55); box-shadow: 0 10px 30px rgba(0,0,0,.1),0 4px 6px rgba(0,0,0,.05); backdrop-filter: blur(25px); }
.shop-profile { display: flex; min-width: 0; align-items: center; gap: 20px; }
.shop-avatar { display: flex; width: 80px; height: 80px; flex: 0 0 80px; align-items: center; justify-content: center; border-radius: 50%; background: #ffeaa7; color: var(--primary); font-size: 2.5rem; font-weight: 700; }
.shop-meta h1 { margin: 0; font-size: 1.5rem; font-weight: 700; overflow-wrap: anywhere; }
.shop-btn { flex: 0 0 auto; padding: 10px 25px; border: 2px solid var(--text-dark); border-radius: 30px; background: transparent; color: inherit; cursor: pointer; font: inherit; font-weight: 600; text-decoration: none; transition: .3s; }
.shop-btn:hover { background: var(--text-dark); color: white; }
.metrics-bar { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 20px; margin-bottom: 30px; }
.metric-card { position: relative; display: flex; min-height: 112px; flex-direction: column; align-items: center; justify-content: center; overflow: hidden; padding: 22px; border-radius: 16px; background: linear-gradient(135deg,#6c5ce7,#a29bfe); box-shadow: 0 10px 20px rgba(108,92,231,.2); color: white; text-align: center; text-decoration: none; transition: transform .3s; }
.metric-card:hover { transform: translateY(-5px); }
.metric-card:nth-child(2) { background: linear-gradient(135deg,#0984e3,#74b9ff); box-shadow: 0 10px 20px rgba(9,132,227,.2); }
.metric-card:nth-child(3) { background: linear-gradient(135deg,#00b894,#55efc4); box-shadow: 0 10px 20px rgba(0,184,148,.2); }
.metric-card:nth-child(4) { background: linear-gradient(135deg,#fdcb6e,#ffeaa7); box-shadow: 0 10px 20px rgba(253,203,110,.2); color: #d35400; }
.metric-val { margin-bottom: 5px; font-size: 2.5rem; font-weight: 700; line-height: 1; }
.metric-label { font-size: .95rem; font-weight: 500; opacity: .9; }
.todo-section { display: grid; grid-template-columns: 1.5fr 1fr; gap: 25px; }
.panel { min-width: 0; padding: 25px; border: 1px solid rgba(255,255,255,.9); border-radius: 16px; background: rgba(255,255,255,.55); box-shadow: 0 10px 30px rgba(0,0,0,.1),0 4px 6px rgba(0,0,0,.05); backdrop-filter: blur(25px); }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.panel-title { margin: 0; color: var(--text-dark); font-size: 1.2rem; font-weight: 700; }
.todo-item { display: flex; align-items: center; justify-content: space-between; padding: 15px 0; border-bottom: 1px solid #f1f2f6; }
.todo-item:last-child { border-bottom: none; }
.todo-left { display: flex; min-width: 0; flex: 1; align-items: center; gap: 15px; }
.todo-icon { display: flex; width: 40px; height: 40px; flex: 0 0 40px; align-items: center; justify-content: center; border-radius: 10px; background: #fff4e6; color: var(--primary); font-size: 1.2rem; }
.todo-text { min-width: 0; flex: 1; }
.todo-text h3 { margin: 0 0 2px; font-size: 1rem; font-weight: 600; }
.todo-text p { margin: 0; color: #b2bec3; font-size: .85rem; }
.todo-action { flex: 0 0 auto; color: var(--primary); font-size: .9rem; font-weight: 600; text-decoration: none; }
.todo-item.stock-warning { align-items: flex-start; flex-direction: column; }
.todo-item.stock-warning .todo-left { width: 100%; align-items: flex-start; }
.todo-item.stock-warning .todo-text { width: 100%; }
.low-stock-list { display: flex; width: 100%; flex-direction: column; gap: 8px; margin-top: 10px; }
.low-stock-item { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 12px; border: 1px solid rgba(255,255,255,.9); border-radius: 12px; background: rgba(255,255,255,.7); box-shadow: 0 6px 14px rgba(0,0,0,.06); backdrop-filter: blur(10px); }
.low-stock-left { display: flex; min-width: 0; flex-direction: column; gap: 2px; }
.low-stock-name { max-width: 100%; overflow: hidden; color: var(--text-dark); font-weight: 600; text-decoration: none; text-overflow: ellipsis; white-space: nowrap; }
.low-stock-meta { color: var(--text-gray); font-size: .85rem; }
.low-stock-stock { color: var(--primary); font-weight: 800; }
.stock-zero { color: #ef4444; }
.low-stock-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 10px; }
.btn-restock-mini { display: inline-flex; align-items: center; gap: 6px; padding: 7px 12px; border-radius: 999px; background: #111; color: white; font-size: .85rem; font-weight: 700; text-decoration: none; transition: transform .2s,background .2s; white-space: nowrap; }
.btn-restock-mini:hover { transform: translateY(-1px); background: var(--primary); }
.more-stock { margin-top: 2px; color: var(--text-gray); font-size: .85rem; }
.shortcut-item { border-bottom: 0; }
@media(max-width:1024px) { .layout-container { grid-template-columns: 80px minmax(0,1fr); } .metrics-bar { grid-template-columns: repeat(2,minmax(0,1fr)); } .todo-section { grid-template-columns: 1fr; } }
@media(max-width:680px) { .layout-container { grid-template-columns: 80px minmax(0,1fr); gap: 12px; margin: 16px auto; padding: 0 10px; } .main-content { gap: 14px; } .shop-header-card { align-items: flex-start; flex-direction: column; margin-bottom: 14px; padding: 18px; } .shop-profile { gap: 12px; } .shop-avatar { width: 56px; height: 56px; flex-basis: 56px; font-size: 1.8rem; } .shop-meta h1 { font-size: 1.15rem; } .shop-btn { padding: 8px 14px; } .metrics-bar { gap: 10px; margin-bottom: 14px; } .metric-card { min-height: 92px; padding: 14px 8px; } .metric-val { font-size: 1.8rem; } .metric-label { font-size: .8rem; } .panel { padding: 16px 12px; } .panel-title { font-size: 1.05rem; } .todo-left { gap: 8px; } .todo-icon { width: 32px; height: 32px; flex-basis: 32px; font-size: 1rem; } .todo-text h3 { font-size: .9rem; } .todo-text p { font-size: .78rem; } .low-stock-item { align-items: flex-start; flex-direction: column; } .low-stock-actions { width: 100%; } }
</style>
