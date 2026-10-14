<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase } from '../api'
import AdminBackground from '../components/AdminBackground.vue'

const products = ref([])
const currentFilter = ref(new URLSearchParams(window.location.search).get('filter') === 'history' ? 'history' : 'pending')
const loading = ref(true)
const error = ref('')
function filterUrl(filter) {
  return `/admin/product/review/list?filter=${filter}`
}

function badgeClass(status) {
  return { PENDING: 'badge-pending', APPROVED: 'badge-approved', REJECTED: 'badge-rejected' }[status] || 'badge-pending'
}

async function loadProducts() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated || user.role !== 'ADMIN') {
      window.location.replace('/login')
      return
    }

    const query = new URLSearchParams({ filter: currentFilter.value })
    const response = await fetch(`${legacyBase}/admin/product/review/list?${query}`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/login')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load products.')
    currentFilter.value = result.data.currentFilter
    products.value = result.data.products
    document.title = 'Product Review - Admin'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load products.'
  } finally {
    loading.value = false
  }
}

onMounted(loadProducts)
</script>

<template>
  <div class="admin-product-review-list-page">
    <AdminBackground />
    <div class="header">
      <div class="title"><i class="ri-shield-check-line"></i> Product Review Queue</div>
      <div class="actions">
        <a href="/admin/dashboard"><i class="ri-arrow-left-line"></i> Back to Dashboard</a>
      </div>
    </div>

    <div class="tabs">
      <a :href="filterUrl('pending')" :class="['tab', { active: currentFilter === 'pending' }]">Pending Review</a>
      <a :href="filterUrl('history')" :class="['tab', { active: currentFilter === 'history' }]">Review History</a>
    </div>

    <div class="panel">
      <div v-if="loading" class="empty" aria-live="polite">Loading products...</div>
      <div v-else-if="error" class="empty" role="alert">{{ error }}</div>
      <div v-else class="table-container">
        <table>
          <thead>
            <tr>
              <th>ID</th><th>Product</th><th>Merchant</th><th>Category</th><th>Status</th>
              <th>{{ currentFilter === 'history' ? 'Reviewed At' : 'Created At' }}</th><th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="products.length === 0"><td colspan="7" class="empty">No products found.</td></tr>
            <tr v-for="product in products" :key="product.productId">
              <td>{{ product.productId }}</td>
              <td>
                <div class="product-name">{{ product.productName }}</div>
                <span v-if="product.auditStatus === 'PENDING'" :class="['badge', 'product-type', product.hasBeenApproved ? 'badge-update' : 'badge-new']">
                  {{ product.hasBeenApproved ? 'Modification' : 'New Listing' }}
                </span>
              </td>
              <td>{{ product.merchantName }}</td>
              <td>{{ product.categoryName }}</td>
              <td><span :class="['badge', badgeClass(product.auditStatus)]">{{ product.auditStatus }}</span></td>
              <td>{{ currentFilter === 'history' ? product.productUpdatedAt : product.productCreatedAt }}</td>
              <td>
                <a class="btn btn-view" :href="`/admin/product/review?productId=${product.productId}`">
                  {{ currentFilter === 'history' ? 'View' : 'Review' }}
                </a>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-product-review-list-page { position: relative; z-index: 1; min-height: 100vh; padding: 40px; color: #333; }
.admin-product-review-list-page,.admin-product-review-list-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.admin-product-review-list-page i[class^='ri-'],.admin-product-review-list-page i[class*=' ri-'] { font-family: 'remixicon' !important; font-style: normal; -webkit-font-smoothing: antialiased; line-height: 1; vertical-align: middle; display: inline-block; }
.header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 30px; }
.title { padding: 10px 30px; border: 1px solid rgba(255,255,255,.5); border-radius: 50px; background: rgba(255,255,255,.6); box-shadow: 0 4px 15px rgba(0,0,0,.05); backdrop-filter: blur(10px); color: #d63031; font-size: 2.2rem; font-weight: 700; }
.title i { margin-right: 10px; vertical-align: middle; }
.actions a { display: inline-flex; align-items: center; gap: 8px; padding: 12px 20px; border-radius: 30px; background: #333; box-shadow: 0 4px 10px rgba(0,0,0,.1); color: white; font-weight: 600; text-decoration: none; transition: all .3s ease; }
.actions a:hover { transform: translateY(-2px); background: #555; box-shadow: 0 6px 15px rgba(0,0,0,.15); }
.tabs { display: flex; gap: 15px; margin-bottom: 20px; }
.tab { padding: 10px 20px; border: 1px solid transparent; border-radius: 25px; background: rgba(255,255,255,.5); color: #666; font-weight: 600; text-decoration: none; transition: all .3s; }
.tab:hover { background: rgba(255,255,255,.8); }
.tab.active { background: #333; box-shadow: 0 4px 10px rgba(0,0,0,.2); color: white; }
.panel { overflow: hidden; padding: 30px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); box-shadow: 0 8px 32px rgba(0,0,0,.1); backdrop-filter: blur(20px); }
.table-container { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th,td { padding: 15px; border-bottom: 1px solid rgba(0,0,0,.06); text-align: left; }
th { color: #666; font-size: .95rem; font-weight: 700; letter-spacing: .5px; text-transform: uppercase; }
tr:last-child td { border-bottom: none; }
tr:hover td { background: rgba(255,255,255,.3); }
.badge { display: inline-block; padding: 6px 12px; border-radius: 20px; font-size: .85rem; font-weight: 600; }
.badge-pending { border: 1px solid #ffe0b2; background: #fff3e0; color: #ef6c00; }
.badge-approved { border: 1px solid #c8e6c9; background: #e8f5e9; color: #2e7d32; }
.badge-rejected { border: 1px solid #ffcdd2; background: #ffebee; color: #c62828; }
.badge-new { border: 1px solid #bbdefb; background: #e3f2fd; color: #1976d2; }
.badge-update { border: 1px solid #e1bee7; background: #f3e5f5; color: #7b1fa2; }
.product-name { font-weight: 600; }
.product-type { margin-top: 5px; font-size: .75rem; }
.btn { display: inline-flex; align-items: center; gap: 5px; padding: 8px 16px; border-radius: 20px; font-size: .9rem; font-weight: 600; text-decoration: none; transition: all .3s ease; }
.btn-view { background: linear-gradient(45deg,#ff3b30,#ff9500); box-shadow: 0 4px 10px rgba(255,59,48,.3); color: white; }
.btn-view:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(255,59,48,.4); }
.empty { padding: 40px; color: #666; font-size: 1.1rem; text-align: center; }
@media(max-width:760px) { .admin-product-review-list-page { padding: 20px; } .header { align-items: flex-start; gap: 16px; flex-direction: column; } .title { font-size: 1.5rem; } .panel { padding: 16px; } }
</style>
