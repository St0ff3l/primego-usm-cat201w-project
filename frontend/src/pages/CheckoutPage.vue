<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { getCategories, getCurrentUser, legacyBase, productImageUrl } from '../api'
import PrimegoHeader from '../components/PrimegoHeader.vue'

const checkout = ref(null)
const categories = ref([])
const currentUser = ref(null)
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const pinError = ref('')
const pinModalOpen = ref(false)
const noPinModalOpen = ref(false)
const pinDigits = ref(['', '', '', '', '', ''])
const pinInputs = ref([])
const isBuyNow = computed(() => checkout.value?.isBuyNow ?? false)
const subtotal = computed(() => checkout.value?.items.reduce((sum, item) => {
  return sum + Number(item.product.price) * item.quantity
}, 0) ?? 0)
const total = computed(() => subtotal.value + Number(checkout.value?.shippingFee || 0))

function imageFailed(event) {
  event.target.src = '/assets/images/product-placeholder.svg'
  event.target.onerror = null
}

function updateQuantity(item, event) {
  const max = Number(item.product.stockQuantity)
  const value = Number(event.target.value)
  item.quantity = Math.min(max, Math.max(1, Number.isInteger(value) ? value : 1))
  event.target.value = item.quantity
}

function openPinModal() {
  pinError.value = ''
  pinModalOpen.value = true
  nextTick(() => pinInputs.value[0]?.focus())
}

function handlePayClick() {
  if (!checkout.value?.defaultAddress?.fullAddress) {
    error.value = 'Please select a delivery address.'
    return
  }
  error.value = ''
  if (checkout.value.hasPin) openPinModal()
  else noPinModalOpen.value = true
}

function updatePinDigit(index, event) {
  const digit = event.target.value.replace(/\D/g, '').slice(-1)
  pinDigits.value[index] = digit
  event.target.value = digit
  if (digit && index < pinInputs.value.length - 1) pinInputs.value[index + 1]?.focus()
}

function handlePinKeydown(index, event) {
  if (event.key === 'Backspace' && !pinDigits.value[index] && index > 0) {
    pinInputs.value[index - 1]?.focus()
  }
  if (event.key === 'Enter') confirmPayment()
}

function closePinModal() {
  pinModalOpen.value = false
  pinError.value = ''
  pinDigits.value = ['', '', '', '', '', '']
}

async function confirmPayment() {
  const paymentPin = pinDigits.value.join('')
  if (paymentPin.length !== 6) {
    pinError.value = 'Please enter all 6 digits.'
    return
  }

  submitting.value = true
  pinError.value = ''
  const body = new URLSearchParams({
    isBuyNow: String(isBuyNow.value),
    paymentPin,
    fullName: checkout.value.defaultAddress.recipientName,
    address: checkout.value.defaultAddress.fullAddress,
    phone: checkout.value.defaultAddress.phone,
  })
  if (isBuyNow.value) {
    body.set('productId', checkout.value.productId)
    body.set('quantity', checkout.value.items[0].quantity)
  } else {
    checkout.value.items.forEach((item) => {
      body.append('selectedProductIds', item.product.id)
      body.append('selectedQuantities', item.quantity)
    })
  }

  try {
    const response = await fetch(legacyBase + '/placeOrder', {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      credentials: 'include',
      body,
    })
    const result = await response.json()
    if (response.status === 401) {
      window.location.href = '/public/login.jsp'
      return
    }
    if (result.data?.redirectUrl) {
      window.location.href = result.data.redirectUrl
      return
    }
    if (!response.ok || result.code !== 200) {
      pinError.value = result.msg || 'Order processing failed. Please try again.'
      pinDigits.value = ['', '', '', '', '', '']
      nextTick(() => pinInputs.value[0]?.focus())
      return
    }
    window.location.href = '/customer/order/payment_success.jsp'
  } catch {
    pinError.value = 'Unable to connect. Please try again.'
  } finally {
    submitting.value = false
  }
}

async function loadCheckout() {
  const [categoryResult, userResult] = await Promise.allSettled([getCategories(), getCurrentUser()])
  if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value
  if (userResult.status === 'fulfilled' && userResult.value.authenticated) currentUser.value = userResult.value

  try {
    const response = await fetch(`${legacyBase}/api/checkout${window.location.search}`, {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    const result = await response.json().catch(() => null)
    if (response.status === 401) {
      window.location.replace('/public/login.jsp')
      return
    }
    if (!result) throw new Error('Unable to connect to the checkout service. Please try again.')
    if (!response.ok || result.code !== 200) throw new Error(result.msg || 'Unable to load this order.')
    checkout.value = result.data
    document.title = 'Confirm Order - PrimeGo'
  } catch (loadError) {
    error.value = loadError.message || 'Unable to load your order. Please try again.'
  } finally {
    loading.value = false
  }
}

onMounted(loadCheckout)
</script>

<template>
  <div class="checkout-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
    <PrimegoHeader :categories="categories" :current-user="currentUser" />

    <main class="checkout-container">
      <div v-if="loading" class="checkout-state" aria-live="polite">Loading your order...</div>
      <div v-else-if="error && !checkout" class="checkout-state" role="alert">
        <p>{{ error }}</p>
        <a href="/customer/order/cart.jsp">Return to cart</a>
      </div>
      <div v-else-if="checkout" class="checkout-grid">
        <section class="checkout-left">
          <div class="checkout-card">
            <h2>Order Items ({{ checkout.items.length }})</h2>
            <div v-for="item in checkout.items" :key="item.product.id" class="order-item">
              <div class="item-img">
                <img :src="productImageUrl(item.product.imageUrl)" :alt="item.product.name" @error="imageFailed" />
              </div>
              <div class="item-copy">
                <h3>{{ item.product.name }}</h3>
                <div class="quantity-row">
                  <label :for="'quantity-' + item.product.id">Quantity:</label>
                  <input
                    :id="'quantity-' + item.product.id"
                    v-model.number="item.quantity"
                    type="number"
                    min="1"
                    :max="item.product.stockQuantity"
                    @change="updateQuantity(item, $event)"
                  />
                  <span>(Max: {{ item.product.stockQuantity }})</span>
                </div>
                <p class="item-total">RM {{ (Number(item.product.price) * item.quantity).toFixed(2) }}</p>
              </div>
            </div>
          </div>

          <div class="checkout-card">
            <h2>Delivery Method</h2>
            <div class="delivery-option"><div class="option-box selected"><i class="ri-truck-line"></i> Shipping</div></div>
            <div v-if="checkout.defaultAddress" class="address-grid">
              <article class="address-card selected">
                <div class="address-name">
                  {{ checkout.defaultAddress.recipientName }}
                  <span>Default</span>
                </div>
                <div class="address-phone">{{ checkout.defaultAddress.phone }}</div>
                <div class="address-detail">{{ checkout.defaultAddress.fullAddress }}</div>
              </article>
              <a href="/profile?tab=addresses" class="add-address">
                <span aria-hidden="true">+</span>
                <span>Add New Address</span>
              </a>
            </div>
            <div v-else class="missing-address">
              You don't have a default address. Please add one.
              <a href="/profile?tab=addresses">Add an address</a>
            </div>
          </div>
        </section>

        <aside class="checkout-card summary-card">
          <h2>Summary</h2>
          <div class="wallet-info">Wallet Balance: RM {{ Number(checkout.walletBalance).toFixed(2) }}</div>
          <div class="summary-row"><span>Subtotal</span><span>RM {{ subtotal.toFixed(2) }}</span></div>
          <div class="summary-row">
            <span>Delivery ({{ checkout.merchantCount }} Merchant{{ checkout.merchantCount > 1 ? 's' : '' }})</span>
            <span>RM {{ Number(checkout.shippingFee).toFixed(2) }}</span>
          </div>
          <div class="total-row"><span>Total</span><span>RM {{ total.toFixed(2) }}</span></div>
          <button class="btn-pay" type="button" :disabled="submitting" @click="handlePayClick">
            {{ submitting ? 'Processing...' : 'Pay Now' }}
          </button>
          <p v-if="error" class="checkout-error" role="alert">{{ error }}</p>
        </aside>
      </div>
    </main>

    <div v-if="pinModalOpen" class="modal-overlay" @keydown.esc="closePinModal">
      <section class="modal-box" role="dialog" aria-modal="true" aria-labelledby="pin-title">
        <div class="modal-icon"><i class="ri-lock-line"></i></div>
        <h2 id="pin-title">Enter Payment PIN</h2>
        <p class="modal-copy">Please enter your 6-digit PIN to confirm.</p>
        <p v-if="pinError" class="pin-error" role="alert">{{ pinError }}</p>
        <div class="pin-input-container">
          <input
            v-for="(_, index) in pinDigits"
            :key="index"
            :ref="(element) => { pinInputs[index] = element }"
            :value="pinDigits[index]"
            class="pin-digit"
            type="password"
            inputmode="numeric"
            autocomplete="one-time-code"
            maxlength="1"
            :aria-label="'PIN digit ' + (index + 1)"
            @input="updatePinDigit(index, $event)"
            @keydown="handlePinKeydown(index, $event)"
          />
        </div>
        <div class="modal-actions">
          <button type="button" class="btn-modal-cancel" :disabled="submitting" @click="closePinModal">Cancel</button>
          <button type="button" class="btn-modal-confirm" :disabled="submitting" @click="confirmPayment">
            {{ submitting ? 'Confirming...' : 'Confirm' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="noPinModalOpen" class="modal-overlay" @click.self="noPinModalOpen = false" @keydown.esc="noPinModalOpen = false">
      <section class="modal-box" role="dialog" aria-modal="true" aria-labelledby="no-pin-title">
        <div class="modal-icon"><i class="ri-error-warning-line"></i></div>
        <h2 id="no-pin-title">No PIN Set</h2>
        <p class="modal-copy">To protect your wallet, you must set a <strong>Payment PIN</strong> before purchasing.</p>
        <a href="/profile?tab=settings&pinAction=true" class="btn-modal-setup">Set PIN Now</a>
        <button type="button" class="btn-dismiss" @click="noPinModalOpen = false">Cancel</button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.checkout-page { min-height: 100vh; position: relative; overflow-x: hidden; color: #333; }
.background-blob { position: fixed; border-radius: 50%; z-index: -1; pointer-events: none; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { width: 750px; height: 650px; top: -200px; left: -200px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5),inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { width: 900px; height: 700px; top: -250px; right: -100px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7),inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { width: 1800px; height: 950px; bottom: -650px; left: -600px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.checkout-container { max-width: 1000px; margin: 140px auto 50px; padding: 0 20px; }
.checkout-grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: 30px; align-items: start; }
.checkout-left { min-width: 0; }
.checkout-card { padding: 25px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,.6); border-radius: 20px; background: rgba(255,255,255,.65); backdrop-filter: blur(20px); }
.checkout-card h2 { margin: 0 0 20px; padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,.1); color: #2d3436; font-size: 1.2rem; }
.order-item { display: flex; align-items: center; gap: 15px; margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px dashed #eee; }
.order-item:last-child { margin: 0; padding: 0; border-bottom: 0; }
.item-img { width: 80px; height: 80px; flex: 0 0 80px; overflow: hidden; border-radius: 12px; background: rgba(255,255,255,.5); }
.item-img img { width: 100%; height: 100%; object-fit: cover; }
.item-copy { min-width: 0; }
.item-copy h3 { margin: 0 0 6px; font-size: 1rem; overflow-wrap: anywhere; }
.quantity-row { display: flex; align-items: center; gap: 8px; color: #666; font-size: .9rem; }
.quantity-row input { width: 64px; padding: 5px; border: 1px solid #ccc; border-radius: 5px; }
.quantity-row input:disabled { background: #f4f4f4; color: #777; }
.quantity-row span { color: #999; font-size: .8rem; }
.item-total { margin: 6px 0 0; color: #ff3b30; font-weight: 700; }
.delivery-option { display: flex; gap: 15px; margin-bottom: 15px; }
.option-box { flex: 1; padding: 15px; border-radius: 12px; background: rgba(255,255,255,.5); text-align: center; }
.option-box.selected { color: #ff3b30; font-weight: 600; }
.address-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(220px,1fr)); gap: 15px; margin-top: 15px; }
.address-card { position: relative; display: flex; min-height: 120px; flex-direction: column; justify-content: center; padding: 15px; border: 2px solid #ff3b30; border-radius: 15px; background: rgba(255,59,48,.05); overflow-wrap: anywhere; }
.address-card.selected::after { content: '✓'; position: absolute; top: 10px; right: 10px; display: grid; width: 20px; height: 20px; place-items: center; border-radius: 50%; background: #ff3b30; color: white; font-size: 12px; }
.address-name { padding-right: 24px; color: #2d3436; font-size: 1.05rem; font-weight: 700; }
.address-name span { margin-left: 5px; padding: 1px 5px; border: 1px solid #ff3b30; border-radius: 4px; color: #ff3b30; font-size: .75rem; font-weight: 400; }
.address-phone,.address-detail { margin-top: 5px; color: #666; font-size: .9rem; line-height: 1.4; }
.address-phone { color: #888; }
.add-address { display: flex; min-height: 120px; flex-direction: column; align-items: center; justify-content: center; border: 2px dashed #ccc; border-radius: 15px; background: rgba(255,255,255,.3); color: #888; text-decoration: none; transition: .2s; }
.add-address span:first-child { font-size: 24px; }
.add-address:hover { border-color: #ff3b30; background: rgba(255,255,255,.8); color: #ff3b30; }
.missing-address { display: flex; flex-wrap: wrap; align-items: center; gap: 5px 12px; padding: 10px; color: #666; }
.missing-address a { color: #d63031; font-weight: 600; }
.summary-card { margin-bottom: 20px; }
.wallet-info { margin-bottom: 15px; padding: 10px; border: 1px solid #bbf7d0; border-radius: 10px; background: #f0fdf4; color: #15803d; font-weight: 600; text-align: center; }
.summary-row,.total-row { display: flex; justify-content: space-between; gap: 12px; }
.summary-row { margin-bottom: 10px; color: #555; }
.summary-row span:last-child,.total-row span:last-child { white-space: nowrap; }
.total-row { margin-top: 20px; padding-top: 20px; border-top: 2px dashed #ccc; color: #2d3436; font-size: 1.2rem; font-weight: 700; }
.total-row span:last-child { color: #ff3b30; }
.btn-pay { width: 100%; margin-top: 20px; padding: 15px; border: 0; border-radius: 15px; background: linear-gradient(45deg,#ff3b30,#ff9500); color: white; cursor: pointer; font-size: 1.05rem; font-weight: 600; box-shadow: 0 5px 15px rgba(255,59,48,.3); transition: .2s; }
.btn-pay:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(255,59,48,.5); }
.btn-pay:disabled { background: #ccc; cursor: wait; box-shadow: none; }
.checkout-error { margin: 12px 0 0; color: #c62828; font-size: .9rem; text-align: center; }
.checkout-state { padding: 30px; color: #555; text-align: center; }
.checkout-state a { color: #d63031; font-weight: 600; }
.modal-overlay { position: fixed; z-index: 2000; inset: 0; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(0,0,0,.5); backdrop-filter: blur(8px); }
.modal-box { width: 400px; max-width: 100%; padding: 32px; border-radius: 20px; background: white; text-align: center; box-shadow: 0 10px 40px rgba(0,0,0,.2); }
.modal-icon { margin-bottom: 8px; color: #ff9500; font-size: 2.8rem; }
.modal-box h2 { margin: 0 0 6px; color: #333; font-size: 1.2rem; }
.modal-copy { margin: 0; color: #666; font-size: .9rem; line-height: 1.5; }
.pin-error { margin: 12px 0 0; padding: 8px; border-radius: 8px; background: #ffebee; color: #d32f2f; font-size: .88rem; font-weight: 600; }
.pin-input-container { display: flex; justify-content: center; gap: 10px; margin: 24px 0; }
.pin-digit { width: 45px; height: 50px; border: 1px solid #ccc; border-radius: 10px; color: #333; font-size: 1.4rem; font-weight: 700; text-align: center; }
.pin-digit:focus { outline: none; border-color: #ff9500; box-shadow: 0 0 0 4px rgba(255,149,0,.2); }
.modal-actions { display: flex; gap: 10px; }
.btn-modal-confirm,.btn-modal-cancel { width: 100%; padding: 12px; border-radius: 10px; cursor: pointer; font-weight: 600; }
.btn-modal-confirm { border: 0; background: #ff9500; color: white; }
.btn-modal-cancel { border: 1px solid #ccc; background: white; color: #666; }
.btn-modal-confirm:disabled,.btn-modal-cancel:disabled { opacity: .6; cursor: wait; }
.btn-modal-setup { display: inline-block; margin-top: 20px; padding: 12px 30px; border-radius: 30px; background: #ff9500; color: white; font-weight: 600; text-decoration: none; }
.btn-dismiss { display: block; margin: 15px auto 0; border: 0; background: transparent; color: #999; cursor: pointer; }
@media (max-width: 800px) { .checkout-grid { grid-template-columns: 1fr; gap: 0; } .summary-card { grid-row: 1; } }
@media (max-width: 600px) { .checkout-container { margin: 100px auto 30px; padding: 0 14px; } .checkout-card { padding: 18px; } .order-item { align-items: flex-start; gap: 10px; } .item-img { width: 64px; height: 64px; flex-basis: 64px; } .quantity-row { flex-wrap: wrap; font-size: .82rem; } .quantity-row span { width: 100%; margin-left: 72px; } .address-grid { grid-template-columns: 1fr; } .pin-input-container { gap: 6px; } .pin-digit { width: min(45px,calc((100vw - 100px) / 6)); height: 48px; } }
</style>
