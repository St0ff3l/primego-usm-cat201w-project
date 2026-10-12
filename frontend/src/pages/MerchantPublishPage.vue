<script setup>
import { onMounted, ref } from 'vue'
import { getCategories, getCurrentUser, legacyBase } from '../api'
import MerchantBackground from '../components/MerchantBackground.vue'
import MerchantSidebar from '../components/MerchantSidebar.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'
import { createImagesUploader } from '../utils/imagesUploader'

const currentUser = ref(null)
const categories = ref([])
const loading = ref(true)
const error = ref('')
const categoryId = ref('')
const submitError = ref('')
const submitting = ref(false)

async function loadPage() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated || !['MERCHANT', 'ADMIN'].includes(user.role)) {
      window.location.replace('/public/login.jsp')
      return
    }
    currentUser.value = user

    const [loadedCategories] = await Promise.all([
      getCategories(),
      createImagesUploader('#photo-upload-container', {
        inputName: 'productImage',
        placeholderImg: `${legacyBase}/assets/images/product-placeholder.svg`,
      }),
    ])
    categories.value = loadedCategories
    document.title = 'Publish Product - Seller Center'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load the product form.'
  } finally {
    loading.value = false
  }
}

function cancel() {
  window.history.back()
}

async function publishProduct(event) {
  submitError.value = ''
  submitting.value = true
  try {
    const response = await fetch(`${legacyBase}/product_add_action`, {
      method: 'POST',
      headers: { Accept: 'application/json' },
      credentials: 'include',
      body: new FormData(event.currentTarget),
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to publish this product.')
    window.location.assign(result.data.redirectUrl || '/merchant/product/product_management.jsp?msg=pending_review')
  } catch (submitFailure) {
    submitError.value = submitFailure.message || 'Unable to publish this product.'
  } finally {
    submitting.value = false
  }
}

onMounted(loadPage)
</script>

<template>
  <div class="merchant-publish-page">
    <PrimegoHeader :current-user="currentUser" :merchant-route="true" />
    <MerchantBackground />
    <div class="layout-container">
      <MerchantSidebar active="publish" />
      <main>
        <div v-if="loading" class="form-card page-state" aria-live="polite">Loading product form...</div>
        <div v-else-if="error" class="form-card page-state" role="alert">{{ error }}</div>
        <form v-show="!loading && !error" class="form-card" @submit.prevent="publishProduct">
          <div class="page-header">
            <h2>Publish New Product</h2>
            <p>Fill in the details below to add a new item to your store.</p>
          </div>
          <p v-if="submitError" class="page-state" role="alert">{{ submitError }}</p>

          <div class="form-content-grid">
            <div class="left-col">
              <div class="form-group">
                <label class="form-label" for="productName">Product Name <span>*</span></label>
                <input id="productName" type="text" name="productName" class="form-input" placeholder="e.g. Vintage Leather Jacket" required />
              </div>

              <div class="form-group">
                <label class="form-label" for="category">Category <span>*</span></label>
                <select id="category" v-model="categoryId" name="categoryId" class="form-select" required>
                  <option value="" disabled>Select a category</option>
                  <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
                </select>
              </div>

              <div class="row-group">
                <div class="form-group">
                  <label class="form-label" for="price">Price (RM) <span>*</span></label>
                  <input id="price" type="number" name="price" class="form-input" placeholder="0.00" step="0.01" required />
                </div>
                <div class="form-group">
                  <label class="form-label" for="stock">Stock <span>*</span></label>
                  <input id="stock" type="number" name="stock" class="form-input" placeholder="1" required />
                </div>
              </div>

              <div class="form-group">
                <label class="form-label" for="whatsapp">
                  <i class="ri-whatsapp-line" style="font-size: 1.1rem; vertical-align: middle; margin-right: 4px; color: #25D366"></i>
                  WhatsApp Contact <span>*</span>
                </label>
                <input id="whatsapp" type="text" name="contactWhatsapp" class="form-input" placeholder="e.g. 60123456789" required />
              </div>
              <div class="form-group" style="margin-bottom: 0">
                <label class="form-label" for="description">Description</label>
                <textarea id="description" name="description" class="form-textarea" rows="5" placeholder="Describe your item..."></textarea>
              </div>
            </div>

            <div class="right-col">
              <label class="form-label">Product Images <span>*</span></label>
              <div id="photo-upload-container"></div>
            </div>
          </div>

          <div class="btn-container">
            <button type="button" class="btn-cancel" @click="cancel">Cancel</button>
            <button type="submit" class="btn-submit" :disabled="submitting">{{ submitting ? 'Publishing...' : 'Publish Now' }}</button>
          </div>
        </form>
      </main>
    </div>
  </div>
</template>

<style scoped>
.merchant-publish-page { position: relative; min-height: 100vh; overflow-x: hidden; padding-top: 90px; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #2d3436; --primary: #ff9500; --primary-hover: #e68600; --secondary: #ff5e55; --text-dark: #2d3436; --text-gray: #636e72; --card-radius: 16px; --pg-glass-bg-055: rgba(255,255,255,.55); --pg-glass-border: rgba(255,255,255,.9); --pg-glass-shadow: 0 10px 30px rgba(0,0,0,.1),0 4px 6px rgba(0,0,0,.05); --pg-glass-blur: 25px; --iu-primary: var(--primary); --iu-bg: rgba(255,255,255,.4); --iu-border: #dfe6e9; }
.merchant-publish-page,.merchant-publish-page * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.merchant-publish-page i[class^='ri-'],.merchant-publish-page i[class*=' ri-'] { font-family: 'remixicon' !important; font-style: normal; -webkit-font-smoothing: antialiased; line-height: 1; vertical-align: middle; display: inline-block; }
.layout-container { position: relative; z-index: 1; display: grid; max-width: 1400px; grid-template-columns: 240px 1fr; align-items: start; gap: 30px; margin: 25px auto; padding: 0 20px; }
.form-card { width: 100%; padding: 30px 40px; border: 1px solid var(--pg-glass-border); border-radius: var(--card-radius); background: var(--pg-glass-bg-055); box-shadow: var(--pg-glass-shadow); backdrop-filter: blur(var(--pg-glass-blur)); -webkit-backdrop-filter: blur(var(--pg-glass-blur)); opacity: 0; transform: translateY(22px); animation: pgFadeInUp .6s cubic-bezier(.2,.8,.2,1) forwards; }
@keyframes pgFadeInUp { from { opacity: 0; transform: translateY(22px); } to { opacity: 1; transform: translateY(0); } }
.page-header { margin-bottom: 25px; padding-bottom: 15px; border-bottom: 1px solid rgba(0,0,0,.06); }
.page-header h2 { margin-bottom: 5px; color: var(--text-dark); font-size: 1.6rem; font-weight: 700; }
.page-header p { color: var(--text-gray); font-size: .85rem; }
.form-content-grid { display: grid; grid-template-columns: 1.8fr 1.2fr; gap: 40px; }
.form-group { margin-bottom: 18px; }
.row-group { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.form-label { display: block; margin-bottom: 6px; color: var(--text-dark); font-size: .9rem; font-weight: 600; }
.form-label span { margin-left: 2px; color: var(--secondary); }
.form-input,.form-select,.form-textarea { width: 100%; padding: 10px 15px; border: 2px solid rgba(0,0,0,.06); border-radius: 10px; outline: none; background: rgba(255,255,255,.55); color: var(--text-dark); font-size: .9rem; transition: .3s; }
.form-input:focus,.form-select:focus,.form-textarea:focus { border-color: var(--primary); background: rgba(255,255,255,.75); box-shadow: 0 0 0 4px rgba(255,149,0,.1); }
.right-col { display: flex; flex-direction: column; }
.btn-container { display: flex; justify-content: flex-end; gap: 15px; margin-top: 20px; padding-top: 20px; border-top: 1px solid rgba(0,0,0,.06); }
.btn-cancel { padding: 10px 30px; border: 1px solid rgba(0,0,0,.12); border-radius: 30px; background: rgba(255,255,255,.7); color: var(--text-gray); cursor: pointer; font-weight: 600; transition: .3s; }
.btn-cancel:hover { border-color: var(--text-dark); color: var(--text-dark); }
.btn-submit { padding: 10px 40px; border: 0; border-radius: 30px; background: var(--primary); box-shadow: 0 5px 15px rgba(255,149,0,.3); color: white; cursor: pointer; font-weight: 600; transition: .3s; }
.btn-submit:hover { transform: translateY(-2px); background: var(--primary-hover); }
.page-state { color: var(--text-gray); text-align: center; }
@media (max-width:1024px) { .layout-container { grid-template-columns: 80px 1fr; } .form-content-grid { grid-template-columns: 1fr; } }
</style>
