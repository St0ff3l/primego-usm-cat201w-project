<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCategories, getCurrentUser, getProduct, legacyBase, productImageUrl } from '../api'
import LoginCheckModal from '../components/LoginCheckModal.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const product = ref(null)
const categories = ref([])
const currentUser = ref(null)
const currentIndex = ref(0)
const loginModalOpen = ref(false)
const modal = ref({ open: false, title: '', message: '', type: 'error' })
const productId = new URLSearchParams(window.location.search).get('id')

const images = computed(() => {
  if (product.value?.imageUrls?.length) return product.value.imageUrls
  if (product.value?.imageUrl) return [product.value.imageUrl]
  return ['']
})

const currentImage = computed(() => productImageUrl(images.value[currentIndex.value]))
const sellerInitial = computed(() => (product.value?.merchantName || 'S').charAt(0).toUpperCase())
const whatsappDigits = computed(() => (product.value?.contactWhatsapp || '').replace(/[^0-9]/g, ''))
const statusDisplay = computed(() => (product.value?.productStatus || '').replace(/_/g, ' '))

function changeImage(offset) {
  currentIndex.value = (currentIndex.value + offset + images.value.length) % images.value.length
}

function imageFailed(event) {
  event.target.src = '/assets/images/product-placeholder.svg'
  event.target.onerror = null
}

function showMessage(title, message, type) {
  modal.value = { open: true, title, message, type }
}

function addToCart() {
  if (!currentUser.value) {
    loginModalOpen.value = true
    return
  }
  fetch(legacyBase + '/cart_action?action=add&productId=' + encodeURIComponent(product.value.id), {
    credentials: 'include',
  }).then((response) => {
    if (response.ok) showMessage('Success!', 'Product successfully added to your cart.', 'success')
    else showMessage('Oops', 'Something went wrong. Please try again.', 'error')
  }).catch(() => showMessage('Error', 'Network error. Please check your connection.', 'error'))
}

function buyNow() {
  if (!currentUser.value) {
    loginModalOpen.value = true
    return
  }
  window.location.href = '/customer/order/order_confirmation.jsp?productId=' + encodeURIComponent(product.value.id)
}

async function loadProduct() {
  if (!productId) {
    window.location.replace('/')
    return
  }
  const results = await Promise.allSettled([getProduct(productId), getCategories(), getCurrentUser()])
  if (results[0].status !== 'fulfilled') {
    window.location.replace('/')
    return
  }
  product.value = results[0].value
  if (results[1].status === 'fulfilled') categories.value = results[1].value
  if (results[2].status === 'fulfilled' && results[2].value.authenticated) currentUser.value = results[2].value
  document.title = (product.value.name || 'Product') + ' - PrimeGo'
}

onMounted(loadProduct)
</script>

<template>
  <div class="product-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
    <PrimegoHeader :categories="categories" :current-user="currentUser" />

    <div v-if="!product" class="page-loading" aria-live="polite">Loading product...</div>
    <div v-else class="container">
      <div class="product-wrapper">
        <section class="gallery-section" aria-label="Product images">
          <div class="main-image-container">
            <button type="button" class="carousel-btn prev-btn" aria-label="Previous image" @click="changeImage(-1)">
              <i class="ri-arrow-left-s-line"></i>
            </button>
            <img :src="currentImage" :alt="product.name" class="carousel-img" @error="imageFailed" />
            <button type="button" class="carousel-btn next-btn" aria-label="Next image" @click="changeImage(1)">
              <i class="ri-arrow-right-s-line"></i>
            </button>
          </div>
          <div class="carousel-indicators">
            <button
              v-for="(image, index) in images"
              :key="image + index"
              :class="['indicator', { active: index === currentIndex }]"
              type="button"
              :aria-label="'Show product image ' + (index + 1)"
              @click="currentIndex = index"
            >
              <img :src="productImageUrl(image)" alt="" @error="imageFailed" />
            </button>
          </div>
        </section>

        <section class="info-section">
          <span class="condition-tag"><i class="ri-shield-check-line"></i> {{ statusDisplay }}</span>
          <h1 class="product-title">{{ product.name }}</h1>
          <div class="price-tag">RM {{ Number(product.price).toFixed(2) }}</div>

          <div class="seller-card">
            <div class="seller-avatar">{{ sellerInitial }}</div>
            <div class="seller-copy">
              <div class="seller-name">{{ product.merchantName || '' }}</div>
              <div class="seller-label">Official Merchant</div>
              <div v-if="product.contactWhatsapp" class="seller-contact">
                <i class="ri-whatsapp-fill"></i><span>{{ product.contactWhatsapp }}</span>
              </div>
            </div>
            <a v-if="whatsappDigits" :href="'https://wa.me/' + whatsappDigits" target="_blank" rel="noopener noreferrer" class="btn-whatsapp">
              Chat <i class="ri-arrow-right-up-line"></i>
            </a>
          </div>

          <div class="description-box">
            <h3>Description</h3>
            <p>{{ product.description || 'No description provided.' }}</p>
          </div>

          <div class="action-group">
            <button type="button" class="btn-action btn-cart" @click="addToCart">
              <i class="ri-shopping-cart-2-line"></i> Add to Cart
            </button>
            <button type="button" class="btn-action btn-buy" @click="buyNow">Buy Now</button>
          </div>
          <div class="verified-note"><i class="ri-shield-flash-line"></i> PrimeGo Verified Listing</div>
        </section>
      </div>
    </div>

    <div v-if="modal.open" class="custom-modal-overlay" @click.self="modal.open = false">
      <div class="custom-modal-box">
        <div :class="['modal-icon-wrap', 'icon-' + modal.type]">
          <i :class="modal.type === 'success' ? 'ri-checkbox-circle-line' : 'ri-error-warning-line'"></i>
        </div>
        <h3 class="modal-title">{{ modal.title }}</h3>
        <p class="modal-message">{{ modal.message }}</p>
        <button type="button" class="modal-btn" @click="modal.open = false">OK, Got it</button>
      </div>
    </div>
    <LoginCheckModal :open="loginModalOpen" @close="loginModalOpen = false" />
  </div>
</template>

<style scoped>
.product-page { min-height: 100vh; position: relative; overflow: hidden; color: #2d3436; padding-top: 100px; }
.background-blob { position: fixed; border-radius: 50%; z-index: -1; pointer-events: none; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5), inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7), inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5), inset -40px -40px 80px rgba(160,82,45,.3); }
.container { max-width: 1200px; margin: 0 auto; padding: 20px; }
.page-loading { padding: 100px 20px; text-align: center; }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.product-wrapper { display: grid; grid-template-columns: 1.2fr 1fr; gap: 40px; animation: fadeInUp .6s ease forwards; }
.gallery-section { position: sticky; top: 120px; align-self: start; }
.main-image-container { position: relative; height: 500px; margin-bottom: 20px; display: flex; align-items: center; justify-content: center; overflow: hidden; background: rgba(255,255,255,.7); backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,.9); border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,.05); }
.carousel-img { max-width: 100%; max-height: 100%; object-fit: contain; transition: opacity .3s ease; }
.carousel-btn { position: absolute; z-index: 10; top: 50%; transform: translateY(-50%); width: 45px; height: 45px; display: flex; align-items: center; justify-content: center; border: 0; border-radius: 50%; background: rgba(255,255,255,.9); color: #2d3436; box-shadow: 0 4px 10px rgba(0,0,0,.15); cursor: pointer; font-size: 1.5rem; opacity: 0; transition: all .2s; }
.main-image-container:hover .carousel-btn, .carousel-btn:focus-visible { opacity: 1; }
.carousel-btn:hover { background: #ff9500; color: white; }
.prev-btn { left: 20px; }
.next-btn { right: 20px; }
.carousel-indicators { display: flex; justify-content: center; gap: 10px; margin-top: 15px; }
.indicator { width: 50px; height: 50px; overflow: hidden; padding: 0; border: 2px solid transparent; border-radius: 10px; background: transparent; cursor: pointer; opacity: .6; transition: .3s; }
.indicator img { width: 100%; height: 100%; object-fit: cover; }
.indicator.active { border-color: #ff9500; opacity: 1; transform: scale(1.1); }
.info-section { padding: 40px; border: 1px solid rgba(255,255,255,.9); border-radius: 20px; background: rgba(255,255,255,.7); backdrop-filter: blur(20px); box-shadow: 0 10px 30px rgba(0,0,0,.05); }
.condition-tag { display: inline-block; margin-bottom: 15px; padding: 6px 16px; border-radius: 50px; background: #fff4e5; color: #ff9500; font-size: .85rem; font-weight: 600; }
.product-title { margin: 0 0 10px; font-size: 2.2rem; font-weight: 700; line-height: 1.2; }
.price-tag { margin-bottom: 25px; color: #ff9500; font-size: 2.4rem; font-weight: 800; }
.seller-card { display: flex; align-items: center; gap: 15px; margin-bottom: 30px; padding: 15px; border-radius: 15px; background: rgba(255,255,255,.4); }
.seller-avatar { width: 50px; height: 50px; flex: 0 0 50px; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #ff9500; color: white; font-size: 1.2rem; font-weight: 700; }
.seller-copy { min-width: 0; }
.seller-name { font-size: 1.05rem; font-weight: 600; }
.seller-label { color: #636e72; font-size: .8rem; }
.seller-contact { display: flex; align-items: center; gap: 4px; margin-top: 4px; color: #25d366; font-size: .9rem; font-weight: 500; }
.btn-whatsapp { display: flex; align-items: center; gap: 8px; margin-left: auto; padding: 10px 20px; border-radius: 30px; background: #25d366; color: white; text-decoration: none; font-size: .95rem; font-weight: 600; box-shadow: 0 4px 12px rgba(37,211,102,.3); transition: all .3s ease; }
.btn-whatsapp:hover { transform: translateY(-2px); background: #128c7e; color: white; box-shadow: 0 6px 15px rgba(37,211,102,.4); }
.description-box { margin: 30px 0; padding-top: 20px; border-top: 1px solid rgba(0,0,0,.05); }
.description-box h3 { margin: 0 0 10px; font-size: 1.1rem; }
.description-box p { margin: 0; color: #636e72; line-height: 1.7; white-space: pre-line; }
.action-group { display: flex; gap: 15px; }
.btn-action { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 18px; border: 0; border-radius: 16px; font-size: 1.1rem; font-weight: 700; text-align: center; text-decoration: none; cursor: pointer; transition: all .3s; }
.btn-cart { background: #2d3436; color: white; box-shadow: 0 10px 20px rgba(0,0,0,.1); }
.btn-cart:hover { transform: translateY(-2px); background: #000; box-shadow: 0 15px 25px rgba(0,0,0,.15); }
.btn-buy { background: #ff9500; color: white; box-shadow: 0 10px 20px rgba(255,149,0,.2); }
.btn-buy:hover { transform: translateY(-2px); background: #e68600; box-shadow: 0 15px 25px rgba(255,149,0,.3); }
.verified-note { display: flex; align-items: center; gap: 8px; margin-top: 30px; color: #636e72; font-size: .85rem; }
.verified-note i { color: #10b981; font-size: 1.2rem; }
.custom-modal-overlay { position: fixed; inset: 0; z-index: 9999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); backdrop-filter: blur(5px); padding: 20px; }
.custom-modal-box { width: 400px; max-width: 90%; padding: 30px; border-radius: 20px; background: #fff; text-align: center; box-shadow: 0 20px 60px rgba(0,0,0,.2); animation: modalFadeIn .25s ease; }
.modal-icon-wrap { width: 60px; height: 60px; margin: 0 auto 15px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 30px; }
.icon-error { background: #ffebee; color: #ef5350; }
.icon-success { background: #e8f5e9; color: #66bb6a; }
.modal-title { margin: 0 0 10px; color: #333; font-size: 1.2rem; font-weight: 700; }
.modal-message { margin: 0 0 25px; color: #666; font-size: .95rem; line-height: 1.5; }
.modal-btn { padding: 10px 30px; border: 0; border-radius: 50px; background: #2d3436; color: white; font-weight: 600; cursor: pointer; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 768px) { .product-wrapper { grid-template-columns: 1fr; gap: 20px; } .gallery-section { position: static; } .container { padding: 12px; } .main-image-container { height: min(90vw,500px); } .carousel-btn { opacity: 1; } .info-section { padding: 25px 20px; } .product-title { font-size: 1.7rem; } .price-tag { font-size: 2rem; } .seller-card { gap: 10px; flex-wrap: wrap; } .btn-whatsapp { margin-left: auto; padding: 8px 12px; } .btn-action { padding: 14px 8px; font-size: .95rem; } }
</style>
