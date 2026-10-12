<script setup>
import { onMounted, ref } from 'vue'
import { getCategories, getCurrentUser, getProducts, legacyBase, productImageUrl } from '../api'
import LoginCheckModal from '../components/LoginCheckModal.vue'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const categories = ref([])
const products = ref([])
const currentUser = ref(null)
const loading = ref(true)
const error = ref('')
const loginModalOpen = ref(false)

function scrollToProducts() {
  document.getElementById('products')?.scrollIntoView({ behavior: 'smooth' })
}

function openProduct(productId) {
  window.location.href = '/customer/product/product_detail.jsp?id=' + encodeURIComponent(productId)
}

function addToCart(productId) {
  if (!currentUser.value) {
    loginModalOpen.value = true
    return
  }
  window.location.href = legacyBase + '/cart_action?action=add&productId=' + encodeURIComponent(productId) + '&quantity=1'
}

function imageFailed(event) {
  event.target.src = '/assets/images/product-placeholder.svg'
  event.target.onerror = null
}

async function loadHome() {
  const [categoryResult, productResult, userResult] = await Promise.allSettled([
    getCategories(),
    getProducts({ inStockOnly: true, pageNum: 1, pageSize: 100 }),
    getCurrentUser(),
  ])

  if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value
  if (userResult.status === 'fulfilled' && userResult.value.authenticated) {
    currentUser.value = userResult.value
    if (currentUser.value.role === 'MERCHANT') {
      window.location.replace('/merchant/merchant_dashboard.jsp')
      return
    }
  }
  if (productResult.status === 'fulfilled') products.value = productResult.value.rows
  else error.value = 'Unable to load products. Please try again later.'
  loading.value = false
}

onMounted(loadHome)
</script>

<template>
  <div class="home-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
    <PrimegoHeader :categories="categories" :current-user="currentUser" />

    <section class="hero glass-panel">
      <h1>Welcome to PrimeGo<br />Premium Marketplace</h1>
      <p>Your destination for high-quality first-hand products.</p>
      <button class="btn btn-primary" type="button" @click="scrollToProducts">Start Shopping</button>
    </section>

    <section id="products" class="section-container">
      <h2 class="section-title">Featured Products</h2>
      <div v-if="loading" class="product-grid" aria-live="polite" aria-busy="true">
        <div v-for="item in 4" :key="item" class="product-card glass-panel loading-card"></div>
      </div>
      <div v-else-if="error" class="empty-products" role="alert">
        <h3>Unable to load products.</h3>
        <p>{{ error }}</p>
      </div>
      <div v-else-if="products.length" class="product-grid">
        <article v-for="product in products" :key="product.id" class="product-card glass-panel" @click="openProduct(product.id)">
          <div class="product-img-container">
            <img :src="productImageUrl(product.imageUrl)" :alt="product.name" @error="imageFailed" />
          </div>
          <div class="product-details">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-price">RM {{ Number(product.price).toFixed(2) }}</p>
            <p class="product-description">{{ (product.description || '').length > 50 ? product.description.slice(0, 50) + '...' : product.description }}</p>
            <button class="btn btn-add" type="button" @click.stop="addToCart(product.id)">Add to Cart</button>
          </div>
        </article>
      </div>
      <div v-else class="empty-products">
        <h3>No products available at the moment.</h3>
        <p>Please check back later!</p>
      </div>
    </section>

    <footer>
      <p>PrimeGo E-Commerce | USM CAT201 Project</p>
      <p class="footer-note">Developed using Vue 3, CSS, and Java.</p>
    </footer>
    <LoginCheckModal :open="loginModalOpen" @close="loginModalOpen = false" />
  </div>
</template>

<style scoped>
.home-page { min-height: 100vh; position: relative; overflow: hidden; color: #333; }
.background-blob { position: fixed; border-radius: 50%; z-index: -1; opacity: 1; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); pointer-events: none; }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5), inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7), inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5), inset -40px -40px 80px rgba(160,82,45,.3); }
.glass-panel { background: rgba(255,255,255,.7); backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,.6); border-radius: 20px; box-shadow: 0 8px 32px rgba(0,0,0,.1), inset 0 0 0 1px rgba(255,255,255,.5); }
.hero { max-width: 1200px; margin: 140px auto 50px; padding: 60px 40px; text-align: center; }
.hero h1 { margin: 0 0 20px; color: #2d3436; font-size: 3.5rem; line-height: 1.2; }
.hero p { margin: 0 0 30px; color: #636e72; font-size: 1.2rem; }
.btn { padding: 12px 30px; border: 0; border-radius: 30px; text-decoration: none; font-weight: 600; transition: all .3s ease; cursor: pointer; }
.btn-primary { background: linear-gradient(45deg,#ff3b30,#ff9500); color: white; box-shadow: 0 5px 15px rgba(255,59,48,.4); }
.btn-primary:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(255,59,48,.6); }
.section-container { max-width: 1200px; margin: 0 auto 50px; padding: 0 20px; }
.section-title { margin: 0 0 30px; padding-left: 15px; border-left: 5px solid #ffcc00; color: #2d3436; font-size: 2rem; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(260px,1fr)); gap: 25px; }
.product-card { overflow: hidden; display: flex; flex-direction: column; cursor: pointer; transition: transform .3s,box-shadow .3s; }
.product-card:hover { transform: translateY(-10px); box-shadow: 0 15px 35px rgba(0,0,0,.15); }
.product-img-container { width: 100%; aspect-ratio: 1; overflow: hidden; display: flex; align-items: center; justify-content: center; background: #f5f6fa; }
.product-img-container img { width: 100%; height: 100%; object-fit: cover; }
.product-details { padding: 10px; display: flex; flex-direction: column; flex-grow: 1; }
.product-name { margin: 0 0 2px; font-size: 1rem; font-weight: 600; }
.product-price { margin: 0 0 5px; color: #ff3b30; font-size: 1.1rem; font-weight: 700; }
.product-description { min-height: 34px; margin: 0 0 8px; color: #666; font-size: .85rem; line-height: 1.3; }
.btn-add { width: 100%; margin-top: auto; position: relative; z-index: 2; border: 2px solid #333; border-radius: 30px; background: transparent; color: #333; }
.btn-add:hover { background: #333; color: white; }
.loading-card { min-height: 330px; animation: pulse 1.2s ease-in-out infinite alternate; }
@keyframes pulse { from { opacity: .55; } to { opacity: .9; } }
.empty-products { grid-column: 1 / -1; padding: 40px; color: #666; text-align: center; }
.empty-products h3 { margin: 0 0 8px; }
.empty-products p { margin: 0; }
footer { margin-top: 50px; padding: 30px; background: #2d3436; color: white; text-align: center; }
footer p { margin: 0; }
.footer-note { margin-top: 8px; opacity: .7; font-size: .8rem; }
@media (max-width: 760px) { .hero { margin: 100px 14px 40px; padding: 45px 20px; } .hero h1 { font-size: clamp(2rem,9vw,3rem); } .hero p { font-size: 1rem; } .section-container { padding: 0 14px; } .section-title { font-size: 1.6rem; } .product-grid { grid-template-columns: repeat(auto-fill,minmax(150px,1fr)); gap: 12px; } .product-details { padding: 8px; } .product-name { font-size: .9rem; } .product-price { font-size: 1rem; } .product-description { font-size: .76rem; } .btn-add { padding: 9px 10px; } }
</style>
