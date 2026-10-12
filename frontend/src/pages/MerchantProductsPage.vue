<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, productImageUrl } from '../api'
import MerchantBackground from '../components/MerchantBackground.vue'
import MerchantSidebar from '../components/MerchantSidebar.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const currentUser = ref(null)
const products = ref([])
const searchTerm = ref('')
const statusFilter = ref('all')
const loading = ref(true)
const error = ref('')

function productStatus(product) {
  if (product.auditStatus === 'PENDING') return { label: 'Under Review', className: 'status-review', value: 'review' }
  if (product.auditStatus === 'REJECTED') return { label: 'Rejected', className: 'status-rejected', value: 'rejected' }
  if (product.productStockQuantity <= 0) return { label: 'Out of Stock', className: 'status-out-stock', value: 'out_stock' }
  if (product.productStatus === 'OFF_SALE') return { label: 'Inactive', className: 'status-inactive', value: 'inactive' }
  return { label: 'Active', className: 'status-active', value: 'active' }
}

function matchesFilters(product) {
  const term = searchTerm.value.toLowerCase().trim()
  const category = product.categoryName ?? 'Uncategorized'
  const name = product.productName ?? ''
  const matchesSearch = !term || name.toLowerCase().includes(term) || category.toLowerCase().includes(term)
  const matchesStatus = statusFilter.value === 'all' || statusFilter.value === productStatus(product).value
  return matchesSearch && matchesStatus
}

function productEditUrl(product) {
  return `/merchant/product/product_edit.jsp?id=${product.productId}`
}

function openProduct(product) {
  window.location.href = productEditUrl(product)
}

function imageFailed(event) {
  event.currentTarget.onerror = null
  event.currentTarget.src = '/assets/images/product-placeholder.svg'
}

async function loadProducts() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated || !['MERCHANT', 'ADMIN'].includes(user.role)) {
      window.location.replace('/public/login.jsp')
      return
    }
    currentUser.value = user

    const response = await fetch(`${legacyBase}/api/merchant/products`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401 || response.status === 403) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load your products.')
    products.value = result.data.products
    document.title = 'Product Manager - Seller Center'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load your products.'
  } finally {
    loading.value = false
  }
}

onMounted(loadProducts)
</script>

<template>
  <div class="merchant-products-page">
    <PrimegoHeader :current-user="currentUser" :merchant-route="true" />
    <MerchantBackground />
    <div class="layout-container">
      <MerchantSidebar active="products" />
      <main class="main-content">
        <div class="page-header">
          <div>
            <h1><i class="ri-box-3-fill" style="color: var(--primary)"></i> My Products</h1>
            <p>Manage your inventory, prices and listings.</p>
          </div>
          <a href="/merchant/product/publish.jsp" class="btn-publish"><i class="ri-add-line"></i> Publish New</a>
        </div>

        <div class="toolbar">
          <div class="search-box">
            <i class="ri-search-line"></i>
            <input v-model="searchTerm" type="text" placeholder="Search by name, ID or category..." />
          </div>
          <select v-model="statusFilter" aria-label="Filter by status">
            <option value="all">All Status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
            <option value="out_stock">Out of Stock</option>
            <option value="review">Under Review</option>
            <option value="rejected">Rejected</option>
          </select>
        </div>

        <div class="product-grid">
          <div v-if="loading" class="empty-state" aria-live="polite">Loading products...</div>
          <div v-else-if="error" class="empty-state" role="alert">{{ error }}</div>
          <div v-else-if="products.length === 0" class="empty-state">
            <span class="empty-icon">📦</span>
            <h3>No products yet</h3>
            <p>Click "Publish New" to start selling!</p>
          </div>
          <div
            v-for="(product, index) in products"
            :key="product.productId"
            class="product-card"
            :style="{ animationDelay: `${0.2 + index * 0.1}s`, display: matchesFilters(product) ? 'flex' : 'none' }"
            role="link"
            tabindex="0"
            @click="openProduct(product)"
            @keydown.enter.prevent="openProduct(product)"
          >
            <span class="status-badge" :class="productStatus(product).className">{{ productStatus(product).label }}</span>
            <div class="card-img-container">
              <img
                :src="productImageUrl(product.primaryImageUrl)"
                :alt="product.productName"
                @error="imageFailed"
              />
            </div>
            <div class="card-info">
              <div class="card-category">{{ product.categoryName ?? 'Uncategorized' }}</div>
              <h3 class="card-title">{{ product.productName }}</h3>
              <div class="card-footer">
                <span class="card-price">RM {{ Number(product.productPrice).toFixed(2) }}</span>
                <div class="card-actions" @click.stop>
                  <button class="btn-edit" type="button" title="Edit" @click="openProduct(product)">
                    <i class="ri-edit-line"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.merchant-products-page { position: relative; min-height: 100vh; overflow-x: hidden; padding-top: 90px; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #2d3436; --primary: #ff9500; --primary-hover: #e68600; --secondary: #ff5e55; --text-dark: #2d3436; --text-gray: #636e72; --glass-bg: rgba(255,255,255,.95); --card-radius: 16px; --card-shadow: 0 10px 30px rgba(0,0,0,.05); --border-color: #dfe6e9; }
.merchant-products-page,.merchant-products-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.merchant-products-page i[class^='ri-'],.merchant-products-page i[class*=' ri-'] { font-family: 'remixicon' !important; font-style: normal; -webkit-font-smoothing: antialiased; line-height: 1; vertical-align: middle; display: inline-block; }
.layout-container { position: relative; z-index: 1; display: grid; max-width: 1400px; grid-template-columns: 240px minmax(0,1fr); align-items: start; gap: 30px; margin: 30px auto; padding: 0 20px; }
.main-content { display: flex; min-width: 0; flex-direction: column; gap: 25px; }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display: flex; align-items: center; justify-content: space-between; padding: 25px 30px; border: 1px solid rgba(255,255,255,.9); border-radius: var(--card-radius); background: rgba(255,255,255,.55); box-shadow: 0 10px 30px rgba(0,0,0,.1),0 4px 6px rgba(0,0,0,.05); backdrop-filter: blur(25px); opacity: 0; animation: fadeInUp .6s cubic-bezier(.2,.8,.2,1) forwards; }
.page-header h1 { display: flex; align-items: center; gap: 10px; margin-bottom: 5px; font-size: 1.5rem; }
.page-header p { color: var(--text-gray); font-size: .9rem; }
.toolbar { display: flex; align-items: center; gap: 15px; padding: 15px 20px; border: 1px solid rgba(255,255,255,.9); border-radius: var(--card-radius); background: rgba(255,255,255,.55); box-shadow: 0 10px 30px rgba(0,0,0,.1),0 4px 6px rgba(0,0,0,.05); backdrop-filter: blur(25px); opacity: 0; animation: fadeInUp .6s cubic-bezier(.2,.8,.2,1) .1s forwards; }
.btn-publish { display: flex; align-items: center; gap: 8px; padding: 10px 24px; border: 0; border-radius: 30px; background: var(--text-dark); box-shadow: 0 4px 12px rgba(0,0,0,.15); color: white; cursor: pointer; font-weight: 600; text-decoration: none; transition: transform .2s; }
.btn-publish:hover { transform: translateY(-2px); background: black; }
.search-box { position: relative; flex: 1; }
.search-box input { width: 100%; padding: 12px 15px 12px 40px; border: 1px solid var(--border-color); border-radius: 10px; outline: none; transition: .3s; }
.search-box input:focus { border-color: var(--primary); }
.search-box i { position: absolute; top: 50%; left: 15px; transform: translateY(-50%); color: #b2bec3; }
.toolbar select { padding: 10px; border: 1px solid #dfe6e9; border-radius: 10px; outline: none; cursor: pointer; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(240px,1fr)); gap: 20px; }
.product-card { position: relative; display: flex; flex-direction: column; overflow: hidden; border: 1px solid rgba(255,255,255,.9); border-radius: 20px; background: rgba(255,255,255,.55); box-shadow: 0 10px 30px rgba(0,0,0,.1),0 4px 6px rgba(0,0,0,.05); backdrop-filter: blur(25px); cursor: pointer; opacity: 0; animation: fadeInUp .6s cubic-bezier(.2,.8,.2,1) forwards; transition: transform .3s ease,box-shadow .3s ease,border-color .3s ease; }
.product-card:hover { transform: translateY(-5px); border-color: rgba(255,149,0,.12); box-shadow: 0 15px 35px rgba(0,0,0,.1); }
.product-card:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }
.card-img-container { position: relative; display: flex; height: 180px; align-items: center; justify-content: center; overflow: hidden; background: rgba(255,255,255,.25); backdrop-filter: blur(8px); }
.card-img-container img { width: 100%; height: 100%; object-fit: cover; }
.status-badge { position: absolute; z-index: 2; top: 10px; right: 10px; padding: 4px 10px; border-radius: 12px; font-size: .75rem; font-weight: 600; }
.status-active { background: #ecfdf5; color: #10b981; }
.status-inactive { background: #fef2f2; color: #ef4444; }
.status-review { background: #fff4e6; color: #ff9500; }
.status-out-stock { background: #f3f4f6; color: #636e72; }
.status-rejected { background: #ffebeb; color: #c0392b; }
.card-info { display: flex; flex-grow: 1; flex-direction: column; padding: 15px; }
.card-category { margin-bottom: 5px; color: var(--text-gray); font-size: .8rem; }
.card-title { flex-grow: 1; margin-bottom: 10px; font-size: 1rem; font-weight: 600; line-height: 1.4; }
.card-footer { display: flex; align-items: center; justify-content: space-between; margin-top: auto; padding-top: 10px; border-top: 1px solid #f1f2f6; }
.card-price { color: var(--primary); font-size: 1.2rem; font-weight: 700; }
.btn-edit { display: flex; width: 36px; height: 36px; align-items: center; justify-content: center; border: 0; border-radius: 50%; background: #f3f4f6; color: var(--text-dark); cursor: pointer; font-size: 1.1rem; transition: .2s; }
.btn-edit:hover { background: var(--primary); color: white; }
.empty-state { grid-column: 1/-1; padding: 60px 20px; color: var(--text-gray); text-align: center; }
.empty-icon { display: block; margin-bottom: 15px; font-size: 4rem; opacity: .5; }
@media (max-width:1024px) { .layout-container { grid-template-columns: 80px minmax(0,1fr); } }
</style>
