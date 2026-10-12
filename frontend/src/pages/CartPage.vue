<script setup>
import { computed, onMounted, ref } from 'vue'
import { cartAction, getCategories, getCurrentUser, legacyPageUrl, productImageUrl } from '../api'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const items = ref([])
const selectedProductIds = ref([])
const categories = ref([])
const currentUser = ref(null)
const error = ref('')
const loading = ref(true)
const pendingRemoval = ref(null)

const selectedTotal = computed(() => items.value.reduce((total, item) => {
  return selectedProductIds.value.includes(item.product.id) ? total + Number(item.totalPrice) : total
}, 0))

function updateCart(data, preserveSelection = true) {
  const oldSelection = selectedProductIds.value
  items.value = data.items || []
  selectedProductIds.value = preserveSelection
    ? oldSelection.filter((id) => items.value.some((item) => item.product.id === id && item.product.stockQuantity > 0))
    : items.value.filter((item) => item.product.stockQuantity > 0).map((item) => item.product.id)
  error.value = data.error || ''
}

function toggleItem(productId, checked) {
  selectedProductIds.value = checked
    ? [...selectedProductIds.value, productId]
    : selectedProductIds.value.filter((id) => id !== productId)
}

async function loadCart() {
  loading.value = true
  try {
    const [cart, categoryResult, userResult] = await Promise.all([
      cartAction('view'),
      getCategories().catch(() => []),
      getCurrentUser().catch(() => null),
    ])
    updateCart(cart, false)
    categories.value = categoryResult
    if (userResult?.authenticated) currentUser.value = userResult
  } catch {
    error.value = 'Unable to load your cart. Please try again.'
  } finally {
    loading.value = false
  }
}

async function updateQuantity(item, event) {
  const quantity = Number(event.target.value)
  if (!Number.isInteger(quantity) || quantity < 1) {
    event.target.value = item.quantity
    return
  }
  try {
    updateCart(await cartAction('update', { productId: item.product.id, quantity }))
  } catch {
    error.value = 'Unable to update the quantity. Please try again.'
    event.target.value = item.quantity
  }
}

async function removeItem() {
  if (!pendingRemoval.value) return
  try {
    updateCart(await cartAction('remove', { productId: pendingRemoval.value.product.id }))
  } catch {
    error.value = 'Unable to remove this item. Please try again.'
  } finally {
    pendingRemoval.value = null
  }
}

function checkout() {
  if (!selectedProductIds.value.length) {
    window.alert('Please select at least one item to checkout.')
    return
  }
  const form = document.createElement('form')
  form.method = 'post'
  form.action = legacyPageUrl('/customer/order/order_confirmation.jsp')
  selectedProductIds.value.forEach((id) => {
    const input = document.createElement('input')
    input.type = 'hidden'
    input.name = 'selectedProductIds'
    input.value = id
    form.appendChild(input)
  })
  document.body.appendChild(form)
  form.submit()
}

function imageFailed(event) {
  event.target.src = '/assets/images/product-placeholder.svg'
  event.target.onerror = null
}

onMounted(loadCart)
</script>

<template>
  <div class="cart-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
    <PrimegoHeader :categories="categories" :current-user="currentUser" />

    <main class="container">
      <h1 class="page-title">My Cart <span>({{ items.length }} Items)</span></h1>
      <div v-if="error" class="cart-error" role="alert"><i class="ri-error-warning-line"></i>{{ error }}</div>

      <div v-if="loading" class="empty-cart" aria-live="polite">Loading cart...</div>
      <div v-else-if="items.length" class="cart-list" id="cartList">
        <div
          v-for="item in items"
          :key="item.product.id"
          :class="['cart-item', { 'out-of-stock': item.product.stockQuantity <= 0 }]"
        >
          <input
            class="item-checkbox"
            type="checkbox"
            :value="item.product.id"
            :checked="selectedProductIds.includes(item.product.id)"
            :disabled="item.product.stockQuantity <= 0"
            :aria-label="'Select ' + item.product.name + ' for checkout'"
            @change="toggleItem(item.product.id, $event.target.checked)"
          />
          <div class="item-img">
            <img :src="productImageUrl(item.product.imageUrl)" :alt="item.product.name" @error="imageFailed" />
            <div v-if="item.product.stockQuantity <= 0" class="sold-out">Sold Out</div>
          </div>
          <div class="item-info">
            <a :href="'/customer/product/product_detail.jsp?id=' + item.product.id" class="item-title">{{ item.product.name }}</a>
            <div class="item-meta">
              <label>
                Quantity:
                <input
                  class="quantity-input"
                  type="number"
                  min="1"
                  :max="item.product.stockQuantity"
                  :value="item.quantity"
                  :disabled="item.product.stockQuantity <= 0"
                  @change="updateQuantity(item, $event)"
                />
              </label>
              <span :class="['stock-note', { low: item.product.stockQuantity < 5 }]">
                <strong v-if="item.product.stockQuantity <= 0">Out of Stock</strong>
                <template v-else>Stock: {{ item.product.stockQuantity }} available</template>
              </span>
            </div>
            <div class="item-price">RM {{ Number(item.totalPrice).toFixed(2) }}</div>
          </div>
          <button class="btn-delete" type="button" :aria-label="'Remove ' + item.product.name" @click="pendingRemoval = item">×</button>
        </div>
      </div>
      <div v-else class="empty-cart">
        <h3>Your cart is empty</h3>
        <p>Go find some treasures!</p>
        <a href="/index.jsp" class="btn-checkout start-shopping">Start Shopping</a>
      </div>
    </main>

    <div class="checkout-bar">
      <div class="total-info">Total: <span>RM {{ selectedTotal.toFixed(2) }}</span></div>
      <button type="button" class="btn-checkout" @click="checkout">Checkout</button>
    </div>

    <div v-if="pendingRemoval" class="modal-overlay" @click.self="pendingRemoval = null" @keydown.esc="pendingRemoval = null">
      <section class="modal-content" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <div class="modal-header">
          <h2 id="confirm-title">Remove Item</h2>
          <button class="close-modal" type="button" aria-label="Close" @click="pendingRemoval = null">×</button>
        </div>
        <div class="modal-body">
          <p>Are you sure you want to remove this item from your cart?</p>
          <div class="modal-actions">
            <button class="btn-login-modal" type="button" @click="removeItem">Remove</button>
            <button class="btn-cancel-modal" type="button" @click="pendingRemoval = null">Cancel</button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.cart-page { min-height: 100vh; position: relative; overflow-x: hidden; padding-bottom: 110px; color: #333; }
.background-blob { position: fixed; border-radius: 50%; z-index: -1; opacity: 1; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); pointer-events: none; }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5),inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7),inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.container { max-width: 1000px; margin: 140px auto 0; padding: 0 20px; }
.page-title { margin: 0 0 30px; padding-left: 15px; border-left: 5px solid #ff9500; color: #2d3436; font-size: 2rem; font-weight: 700; }
.page-title span { color: #666; font-size: 1rem; font-weight: 400; }
.cart-error { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; padding: 15px; border: 1px solid #ef9a9a; border-radius: 10px; background: #ffebee; color: #c62828; }
.cart-list { display: flex; flex-direction: column; gap: 20px; }
.cart-item { display: flex; align-items: center; gap: 20px; padding: 20px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.6); backdrop-filter: blur(20px); transition: transform .3s,background .3s; }
.cart-item:hover { transform: translateX(5px); background: rgba(255,255,255,.8); }
.cart-item.out-of-stock { opacity: .6; background: #f9f9f9; }
.item-checkbox { width: 20px; height: 20px; flex: 0 0 20px; accent-color: #ff3b30; cursor: pointer; }
.item-img { position: relative; display: flex; width: 100px; height: 100px; flex: 0 0 100px; align-items: center; justify-content: center; overflow: hidden; border-radius: 12px; background: #eee; }
.item-img img { width: 100%; height: 100%; object-fit: cover; }
.sold-out { position: absolute; padding: 5px 10px; border-radius: 4px; background: rgba(0,0,0,.7); color: white; font-size: .8rem; font-weight: 700; }
.item-info { flex: 1; min-width: 0; }
.item-title { display: inline-block; margin-bottom: 5px; color: inherit; font-size: 1.1rem; font-weight: 600; text-decoration: none; }
.item-title:hover { color: #ff9500; }
.item-meta { display: flex; flex-direction: column; gap: 5px; margin-top: 5px; color: #666; font-size: .9rem; }
.item-meta label { display: flex; align-items: center; gap: 10px; }
.quantity-input { width: 60px; padding: 5px; border: 1px solid #ccc; border-radius: 5px; }
.stock-note { color: #7f8c8d; font-size: .85rem; }
.stock-note.low,.stock-note strong { color: #e74c3c; }
.item-price { margin-top: 5px; color: #ff3b30; font-size: 1.2rem; font-weight: 700; }
.btn-delete { display: flex; width: 35px; height: 35px; flex: 0 0 35px; align-items: center; justify-content: center; border: 1px solid #ddd; border-radius: 50%; background: white; color: #666; cursor: pointer; font-size: 1.2rem; line-height: 1; transition: .2s; }
.btn-delete:hover { border-color: #ff3b30; background: #ff3b30; color: white; }
.empty-cart { padding: 50px; color: #666; text-align: center; }
.empty-cart h3 { margin: 0 0 8px; }
.empty-cart p { margin: 0; }
.checkout-bar { position: fixed; z-index: 999; bottom: 30px; left: 50%; display: flex; width: 90%; max-width: 1000px; align-items: center; justify-content: space-between; padding: 15px 30px; transform: translateX(-50%); border-radius: 50px; background: #2d3436; color: white; box-shadow: 0 10px 30px rgba(0,0,0,.3); }
.total-info span { color: #ff9500; font-size: 1.3rem; font-weight: 700; }
.btn-checkout { padding: 10px 30px; border: 0; border-radius: 30px; background: linear-gradient(45deg,#ff3b30,#ff9500); color: white; cursor: pointer; font-size: 1rem; font-weight: 600; text-decoration: none; box-shadow: 0 4px 15px rgba(255,59,48,.4); }
.start-shopping { display: inline-block; margin-top: 20px; }
.modal-overlay { position: fixed; z-index: 2000; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.5); backdrop-filter: blur(5px); }
.modal-content { width: 90%; max-width: 400px; padding: 25px; border-radius: 15px; background: white; box-shadow: 0 10px 30px rgba(0,0,0,.2); animation: modalFadeIn .3s ease; }
.modal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 1px solid #eee; }
.modal-header h2 { margin: 0; color: #2d3436; font-size: 1.15rem; }
.close-modal { border: 0; background: transparent; color: #aaa; cursor: pointer; font-size: 1.5rem; }
.close-modal:hover { color: #333; }
.modal-body p { margin: 0 0 20px; color: #666; }
.modal-actions { display: flex; gap: 10px; }
.btn-login-modal,.btn-cancel-modal { display: flex; flex: 1; align-items: center; justify-content: center; padding: 10px; border: 0; border-radius: 8px; cursor: pointer; font-weight: 600; }
.btn-login-modal { background: #ff3b30; color: white; }
.btn-cancel-modal { background: #f1f2f6; color: #333; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 680px) { .container { margin-top: 100px; padding: 0 14px; } .page-title { font-size: 1.5rem; } .cart-item { gap: 10px; padding: 12px; } .item-img { width: 72px; height: 72px; flex-basis: 72px; } .item-title { font-size: .95rem; } .item-meta { font-size: .82rem; } .item-price { font-size: 1rem; } .btn-delete { width: 32px; height: 32px; flex-basis: 32px; } .checkout-bar { bottom: 12px; width: calc(100% - 24px); padding: 12px 16px; } .btn-checkout { padding: 9px 18px; } }
</style>
