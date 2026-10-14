<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCurrentUser, legacyBase, productImageUrl } from '../api'
import ImagesUploader from '../components/ImagesUploader.vue'
import MerchantBackground from '../components/MerchantBackground.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const currentUser = ref(null)
const product = ref(null)
const categories = ref([])
const images = ref([])
const currentIndex = ref(0)
const imageVisible = ref(true)
const loading = ref(true)
const error = ref('')

const galleryImages = computed(() => images.value.length ? images.value : [{ url: null }])
const uploaderImages = computed(() => images.value.map((image) => ({ ...image, url: productImageUrl(image.url) })))
const currentImageUrl = computed(() => productImageUrl(galleryImages.value[currentIndex.value]?.url))

function selectImage(index) {
  imageVisible.value = false
  currentIndex.value = index
  window.setTimeout(() => { imageVisible.value = true }, 150)
}

function prevImage() {
  const index = currentIndex.value === 0 ? galleryImages.value.length - 1 : currentIndex.value - 1
  selectImage(index)
}

function nextImage() {
  const index = currentIndex.value === galleryImages.value.length - 1 ? 0 : currentIndex.value + 1
  selectImage(index)
}

function imageFailed(event) {
  event.currentTarget.onerror = null
  event.currentTarget.src = '/assets/images/product-placeholder.svg'
}

function redirectToProducts(errorCode) {
  window.location.replace(`/merchant/product/product_management.jsp?error=${errorCode}`)
}

async function loadProduct() {
  try {
    const user = await getCurrentUser()
    if (!user.authenticated || user.role !== 'MERCHANT') {
      window.location.replace('/public/login.jsp')
      return
    }
    currentUser.value = user

    const productId = new URLSearchParams(window.location.search).get('id')
    const response = await fetch(`${legacyBase}/api/merchant/product-edit?id=${encodeURIComponent(productId || '')}`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (response.status === 403) {
      redirectToProducts('unauthorized')
      return
    }
    if (response.status === 400 || response.status === 404) {
      redirectToProducts('notfound')
      return
    }
    if (!response.ok || result?.code !== 200) throw new Error(result?.msg || 'Unable to load this product.')

    product.value = result.data.product
    categories.value = result.data.categories
    images.value = result.data.images
    document.title = 'Edit Product - PrimeGo'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load this product.'
  } finally {
    loading.value = false
  }
}

onMounted(loadProduct)
</script>

<template>
  <div class="merchant-product-edit-page">
    <PrimegoHeader :current-user="currentUser" :merchant-route="true" />
    <MerchantBackground />
    <div class="container">
      <div v-if="loading" class="page-state" aria-live="polite">Loading product...</div>
      <div v-else-if="error" class="page-state" role="alert">{{ error }}</div>
      <form v-if="product" v-show="!loading && !error" :action="`${legacyBase}/product_action`" method="post" enctype="multipart/form-data">
        <input type="hidden" name="action" value="update" />
        <input type="hidden" name="productId" :value="product.productId" />

        <div class="layout-container">
          <div class="gallery-card">
            <div class="gallery-title">Preview</div>
            <div class="main-image-container">
              <button type="button" class="carousel-btn prev-btn" aria-label="Previous image" @click="prevImage"><i class="ri-arrow-left-s-line"></i></button>
              <img :src="currentImageUrl" alt="Product Image" class="carousel-img" :style="{ opacity: imageVisible ? 1 : 0 }" @error="imageFailed" />
              <button type="button" class="carousel-btn next-btn" aria-label="Next image" @click="nextImage"><i class="ri-arrow-right-s-line"></i></button>
            </div>
            <div class="carousel-indicators">
              <div
                v-for="(_, index) in galleryImages"
                :key="index"
                :class="['indicator', { active: index === currentIndex }]"
                role="button"
                tabindex="0"
                :aria-label="`Show image ${index + 1}`"
                @click="selectImage(index)"
                @keydown.enter.prevent="selectImage(index)"
              ></div>
            </div>
            <p class="gallery-caption">This is the customer view.</p>
          </div>

          <div class="main-content-area">
            <div class="page-header">
              <h2>Edit Product</h2>
              <span class="badge">ID: {{ product.productId }}</span>
            </div>

            <div class="form-left-col">
              <div class="glass-card">
                <div class="info-grid">
                  <div class="form-group col-span-2">
                    <label class="form-label" for="productName">Product Name</label>
                    <input id="productName" v-model="product.productName" type="text" name="productName" class="form-control" required />
                  </div>

                  <div class="form-group">
                    <label class="form-label" for="categoryId">Category</label>
                    <select id="categoryId" v-model="product.categoryId" name="categoryId" class="form-control">
                      <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
                    </select>
                  </div>

                  <div class="form-group">
                    <label class="form-label" for="productStatus">Status</label>
                    <select id="productStatus" v-model="product.productStatus" name="productStatus" class="form-control">
                      <option value="ON_SALE">On Sale</option>
                      <option value="OFF_SALE">Off Sale</option>
                    </select>
                  </div>

                  <div class="form-group">
                    <label class="form-label" for="productPrice">Price (RM)</label>
                    <input id="productPrice" v-model="product.productPrice" type="number" name="productPrice" class="form-control" step="0.01" min="0" required />
                  </div>

                  <div class="form-group">
                    <label class="form-label" for="productStock">Stock</label>
                    <input id="productStock" v-model="product.productStockQuantity" type="number" name="productStock" class="form-control" step="1" min="0" required />
                  </div>

                  <div class="form-group col-span-2">
                    <label class="form-label" for="contactWhatsapp"><i class="ri-whatsapp-line whatsapp-icon"></i> WhatsApp Contact</label>
                    <input id="contactWhatsapp" v-model="product.contactWhatsapp" type="text" name="contactWhatsapp" class="form-control" placeholder="e.g. 60123456789" required />
                  </div>
                </div>
              </div>

              <div class="glass-card desc-card">
                <label class="form-label" for="productDescription">Description</label>
                <textarea id="productDescription" v-model="product.productDescription" name="productDescription" class="form-control" placeholder="Product details..."></textarea>
              </div>
            </div>

            <div class="form-right-col">
              <div class="glass-card upload-card">
                <label class="form-label upload-label">Manage Images</label>
                <p class="upload-description">Drag images to reorder. First one is Primary.</p>
                <ImagesUploader input-name="newImages" :initial-images="uploaderImages" delete-input-name="deleteImageIds" sort-input-name="imageSortOrder" :placeholder-img="productImageUrl('assets/images/product-placeholder.svg')" />
              </div>
            </div>

            <div class="action-bar">
              <a href="/merchant/product/product_management.jsp" class="btn-action btn-cancel">Cancel</a>
              <button type="submit" class="btn-action btn-save"><i class="ri-save-line"></i> Save Changes</button>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.merchant-product-edit-page { position: relative; min-height: 100vh; overflow-x: hidden; padding-top: 100px; padding-bottom: 50px; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); color: #2d3436; --primary: #ff9500; --primary-hover: #e68600; --secondary: #ff5e55; --text-dark: #2d3436; --text-gray: #636e72; --input-bg: #fff; --card-radius: 16px; --pg-glass-bg: rgba(255,255,255,.85); --pg-glass-border: rgba(255,255,255,.9); --pg-glass-shadow: 0 10px 30px rgba(0,0,0,.05); --iu-primary: var(--primary); --iu-bg: #f8f9fa; }
.merchant-product-edit-page,.merchant-product-edit-page * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Poppins',sans-serif; }
.merchant-product-edit-page i[class^='ri-'],.merchant-product-edit-page i[class*=' ri-'] { font-family: 'remixicon' !important; font-style: normal; -webkit-font-smoothing: antialiased; line-height: 1; vertical-align: middle; display: inline-block; }
.container { position: relative; z-index: 1; max-width: 1600px; margin: 0 auto; padding: 0 30px; }
.layout-container { display: grid; grid-template-columns: 400px 1fr; align-items: start; gap: 30px; }
.gallery-card { position: sticky; top: 110px; padding: 20px; border: 1px solid var(--pg-glass-border); border-radius: var(--card-radius); background: var(--pg-glass-bg); box-shadow: var(--pg-glass-shadow); }
.gallery-title { margin-bottom: 10px; color: var(--text-gray); font-weight: 600; }
.main-image-container { position: relative; display: flex; height: 400px; align-items: center; justify-content: center; overflow: hidden; margin-bottom: 15px; border: 1px solid #eee; border-radius: 12px; background: white; }
.carousel-img { max-width: 100%; max-height: 100%; object-fit: contain; transition: opacity .3s ease; }
.carousel-btn { position: absolute; z-index: 10; top: 50%; display: flex; width: 40px; height: 40px; align-items: center; justify-content: center; transform: translateY(-50%); border: 0; border-radius: 50%; background: rgba(255,255,255,.9); box-shadow: 0 4px 10px rgba(0,0,0,.15); color: var(--text-dark); cursor: pointer; font-size: 1.2rem; transition: all .2s; }
.carousel-btn:hover { background: var(--primary); color: white; }
.prev-btn { left: 10px; }
.next-btn { right: 10px; }
.carousel-indicators { display: flex; justify-content: center; gap: 8px; margin-top: 10px; }
.indicator { width: 8px; height: 8px; border-radius: 50%; background: #ccc; cursor: pointer; transition: .3s; }
.indicator.active { transform: scale(1.3); background: var(--primary); }
.indicator:focus-visible { outline: 2px solid var(--primary); outline-offset: 3px; }
.gallery-caption { margin-top: 15px; color: #b2bec3; font-size: .8rem; text-align: center; }
.main-content-area { display: grid; grid-template-columns: 1.2fr .8fr; gap: 25px; }
.glass-card { padding: 25px; border: 1px solid var(--pg-glass-border); border-radius: var(--card-radius); background: var(--pg-glass-bg); box-shadow: var(--pg-glass-shadow); backdrop-filter: blur(20px); }
.form-left-col { display: flex; flex-direction: column; gap: 20px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.col-span-2 { grid-column: span 2; }
.form-label { display: block; margin-bottom: 6px; color: var(--text-dark); font-size: .85rem; font-weight: 600; }
.form-control { width: 100%; padding: 10px 15px; border: 1px solid #dfe6e9; border-radius: 10px; outline: none; background: var(--input-bg); font-size: .95rem; transition: .3s; }
.form-control:focus { border-color: var(--primary); box-shadow: 0 0 0 4px rgba(255,149,0,.1); }
.desc-card { display: flex; flex-grow: 1; flex-direction: column; }
textarea.form-control { flex-grow: 1; min-height: 200px; resize: vertical; line-height: 1.6; }
.form-right-col { display: flex; height: 100%; flex-direction: column; }
.upload-card { display: flex; height: 100%; flex-direction: column; }
.upload-label { margin-bottom: 12px; }
.upload-description { margin-bottom: 15px; color: var(--text-gray); font-size: .8rem; }
.form-right-col :deep(.iu-upload-area) { min-height: 400px; flex-grow: 1; }
.whatsapp-icon { margin-right: 4px; color: #25d366; font-size: 1.1rem; }
.action-bar { display: flex; grid-column: span 2; justify-content: flex-end; gap: 15px; margin-top: 10px; padding-top: 20px; border-top: 1px solid rgba(0,0,0,.05); }
.btn-action { display: flex; align-items: center; padding: 12px 35px; border: 0; border-radius: 50px; cursor: pointer; font-size: 1rem; font-weight: 600; text-decoration: none; transition: .3s; }
.btn-cancel { background: #f1f2f6; color: var(--text-gray); }
.btn-cancel:hover { background: #dfe6e9; color: var(--text-dark); }
.btn-save { background: var(--primary); box-shadow: 0 5px 15px rgba(255,149,0,.3); color: white; }
.btn-save:hover { transform: translateY(-2px); background: var(--primary-hover); }
.btn-save i { margin-right: 6px; }
.page-header { display: flex; grid-column: span 2; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h2 { font-size: 1.6rem; font-weight: 700; }
.badge { padding: 5px 12px; border-radius: 12px; background: #e3f2fd; color: #1976d2; font-size: .85rem; font-weight: 600; }
.page-state { position: relative; z-index: 1; max-width: 700px; margin: 40px auto; padding: 30px; border: 1px solid rgba(255,255,255,.9); border-radius: var(--card-radius); background: var(--pg-glass-bg); color: var(--text-gray); text-align: center; }
@media (max-width:1200px) { .layout-container { grid-template-columns: 1fr; } .main-content-area { grid-template-columns: 1fr; } .gallery-card { position: static; } }
</style>
