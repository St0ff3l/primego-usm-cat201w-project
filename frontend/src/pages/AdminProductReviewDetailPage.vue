<script setup>
import { onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, productImageUrl } from '../api'
import AdminBackground from '../components/AdminBackground.vue'

const product = ref(null)
const images = ref([])
const loading = ref(true)
const error = ref('')
const rejectionReason = ref('')
const productId = new URLSearchParams(window.location.search).get('productId') || ''
const actionMessage = new URLSearchParams(window.location.search).get('msg')

async function loadProduct() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated || user.role !== 'ADMIN') {
      window.location.replace('/login')
      return
    }

    const query = new URLSearchParams({ productId })
    const response = await fetch(`${legacyBase}/admin/product/review?${query}`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/login')
      return
    }
    if (response.status === 404) {
      error.value = 'Product not found.'
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load this product.')
    product.value = result.data.product
    images.value = result.data.images
    document.title = 'Review Product - Admin'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load this product.'
  } finally {
    loading.value = false
  }
}

async function reviewProduct(action) {
  try {
    const response = await fetch(`${legacyBase}/admin/product/review/action`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      credentials: 'include',
      body: new URLSearchParams({ productId, action, reason: rejectionReason.value }),
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/login')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to update product review.')
    if (result.data.redirect) window.location.assign(result.data.redirect)
    else error.value = result.data.message || 'Unable to update product review.'
  } catch (actionError) {
    error.value = actionError.message || 'Unable to update product review.'
  }
}

onMounted(loadProduct)
</script>

<template>
  <div class="admin-product-review-detail-page">
    <AdminBackground />
    <div class="top">
      <div class="title"><i class="ri-eye-line"></i> Review Product</div>
      <a href="/admin/product/review/list"><i class="ri-arrow-left-line"></i> Back to Queue</a>
    </div>

    <div class="panel">
      <div v-if="actionMessage === 'approved'" class="message message-success">Product approved.</div>
      <div v-else-if="actionMessage === 'rejected'" class="message message-error">Product rejected.</div>
      <div v-if="loading" class="message" aria-live="polite">Loading product...</div>
      <div v-else-if="error || !product" class="message message-error" role="alert">{{ error || 'Product not found.' }}</div>

      <div v-else class="grid">
        <div>
          <div class="field"><div class="label">Product</div><div class="value">{{ product.productName }}</div></div>
          <div class="field"><div class="label">Audit Status</div><div class="value"><span class="status">{{ product.auditStatus }}</span></div></div>
          <div class="field"><div class="label">Audit Message</div><div class="value">{{ product.auditMessage || '-' }}</div></div>
          <div class="field"><div class="label">Merchant</div><div class="value">{{ product.merchantName }}</div></div>
          <div class="field"><div class="label">Category</div><div class="value">{{ product.categoryName }}</div></div>
          <div class="field"><div class="label">Price</div><div class="value">RM {{ product.productPrice }}</div></div>
          <div class="field"><div class="label">Stock</div><div class="value">{{ product.productStockQuantity }}</div></div>
          <div class="field"><div class="label">Description</div><div class="value">{{ product.productDescription || '-' }}</div></div>
        </div>

        <div>
          <div class="label images-label">Images</div>
          <div class="images">
            <img v-for="image in images" :key="image.imageId" :src="productImageUrl(image.imageUrl)" alt="product image"
              @error="$event.currentTarget.onerror = null; $event.currentTarget.src = '/assets/images/product-placeholder.svg'" />
            <div v-if="images.length === 0" class="no-images">No images.</div>
          </div>

          <hr class="separator" />

          <form v-if="product.auditStatus === 'PENDING'" @submit.prevent>
            <div class="field">
              <div class="label">Reject reason (only required for reject)</div>
              <textarea v-model="rejectionReason" class="input" rows="4" placeholder="Optional message to merchant..."></textarea>
            </div>
            <div class="actions">
              <button class="btn btn-reject" type="button" @click="reviewProduct('reject')">Reject</button>
              <button class="btn btn-approve" type="button" @click="reviewProduct('approve')">Approve</button>
            </div>
          </form>
          <div v-else class="message" :class="product.auditStatus === 'APPROVED' ? 'message-success' : 'message-error'">
            This product has been {{ product.auditStatus.toLowerCase() }}.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-product-review-detail-page { position: relative; z-index: 1; min-height: 100vh; padding: 40px; color: #333; }
.admin-product-review-detail-page,.admin-product-review-detail-page * { box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.admin-product-review-detail-page i[class^='ri-'],.admin-product-review-detail-page i[class*=' ri-'] { font-family: 'remixicon' !important; font-style: normal; -webkit-font-smoothing: antialiased; line-height: 1; vertical-align: middle; display: inline-block; }
.top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 30px; }
.title { padding: 10px 30px; border: 1px solid rgba(255,255,255,.5); border-radius: 50px; background: rgba(255,255,255,.6); box-shadow: 0 4px 15px rgba(0,0,0,.05); backdrop-filter: blur(10px); color: #d63031; font-size: 2.2rem; font-weight: 700; }
.title i { margin-right: 10px; vertical-align: middle; }
.top a { display: inline-flex; align-items: center; gap: 8px; padding: 12px 20px; border-radius: 30px; background: #333; box-shadow: 0 4px 10px rgba(0,0,0,.1); color: white; font-weight: 600; text-decoration: none; transition: all .3s ease; }
.top a:hover { transform: translateY(-2px); background: #555; box-shadow: 0 6px 15px rgba(0,0,0,.15); }
.panel { overflow: hidden; padding: 30px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.7); box-shadow: 0 8px 32px rgba(0,0,0,.1); backdrop-filter: blur(20px); }
.grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 40px; }
.field { margin-bottom: 20px; }
.label { margin-bottom: 6px; color: #666; font-size: .9rem; font-weight: 600; letter-spacing: .5px; text-transform: uppercase; }
.value { color: #2d3436; font-size: 1.1rem; font-weight: 600; line-height: 1.5; }
.images-label { margin-bottom: 10px; }
.images { display: grid; grid-template-columns: repeat(auto-fill,minmax(100px,1fr)); gap: 15px; }
.images img { width: 100%; aspect-ratio: 1; border: 1px solid rgba(255,255,255,.8); border-radius: 15px; box-shadow: 0 4px 10px rgba(0,0,0,.05); object-fit: cover; transition: transform .3s ease; }
.images img:hover { transform: scale(1.05); }
.no-images { grid-column: 1/-1; color: #666; }
.status { display: inline-block; padding: 6px 12px; border: 1px solid #ffe0b2; border-radius: 20px; background: #fff3e0; color: #ef6c00; font-size: .9rem; font-weight: 700; }
.actions { display: flex; justify-content: flex-end; gap: 15px; margin-top: 25px; }
.btn { display: inline-flex; align-items: center; gap: 5px; padding: 12px 25px; border: 0; border-radius: 30px; cursor: pointer; font-size: 1rem; font-weight: 600; transition: all .3s ease; }
.btn-approve { background: linear-gradient(45deg,#2ecc71,#27ae60); box-shadow: 0 4px 10px rgba(46,204,113,.3); color: white; }
.btn-approve:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(46,204,113,.4); }
.btn-reject { background: linear-gradient(45deg,#e74c3c,#c0392b); box-shadow: 0 4px 10px rgba(231,76,60,.3); color: white; }
.btn-reject:hover { transform: translateY(-2px); box-shadow: 0 6px 15px rgba(231,76,60,.4); }
.input { width: 100%; padding: 15px; border: 1px solid rgba(0,0,0,.1); border-radius: 15px; outline: none; background: rgba(255,255,255,.5); font-family: inherit; font-size: 1rem; transition: all .3s; }
.input:focus { border-color: #d63031; outline: none; background: white; box-shadow: 0 0 0 3px rgba(214,48,49,.1); }
.message { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; padding: 15px 20px; border-radius: 15px; font-weight: 600; }
.message-success { border: 1px solid #c3e6cb; background: #d4edda; color: #155724; }
.message-error { border: 1px solid #f5c6cb; background: #f8d7da; color: #721c24; }
.separator { margin: 16px 0; border: 0; border-top: 1px solid rgba(0,0,0,.06); }
@media(max-width:1024px) { .grid { grid-template-columns: 1fr; } }
@media(max-width:760px) { .admin-product-review-detail-page { padding: 20px; } .top { align-items: flex-start; gap: 16px; flex-direction: column; } .title { font-size: 1.5rem; } .panel { padding: 20px; } }
</style>
