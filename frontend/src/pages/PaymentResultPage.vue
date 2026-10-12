<script setup>
const success = window.location.pathname.endsWith('payment_success.jsp')
const params = new URLSearchParams(window.location.search)
const orderIds = params.get('orderIds') || params.get('orderId')
const errorMessage = params.get('errorMessage') || 'Something went wrong with your order processing. Please try again.'

function tryAgain() {
  window.history.back()
}
</script>

<template>
  <main class="payment-result-page">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>

    <section v-if="success" class="result-card success-card">
      <div class="icon-circle success-icon"><i class="ri-check-line"></i></div>
      <h1>Payment Successful!</h1>
      <p>
        <template v-if="orderIds && orderIds.includes(',')">
          The following orders have been placed successfully:<br />
          <strong class="order-id-list">#{{ orderIds.replaceAll(',', ', #') }}</strong>
        </template>
        <template v-else-if="orderIds">
          Order <strong>#{{ orderIds }}</strong> has been placed successfully.
        </template>
        <template v-else>Your order has been placed successfully.</template>
        <br />The sellers will process your items shortly.
      </p>
      <div v-if="orderIds?.includes(',')" class="shipment-note">
        <strong><i class="ri-information-fill"></i> Multiple Shipments</strong>
        <span>Since you purchased items from different merchants, your order has been split into separate shipments automatically.</span>
      </div>
      <a href="/index.jsp" class="result-button success-button">Continue Shopping</a>
      <a href="/customer/orders?status=ALL" class="result-button outline-button">View My Orders</a>
    </section>

    <section v-else class="result-card failed-card">
      <div class="icon-circle failed-icon"><i class="ri-close-line"></i></div>
      <h1>Payment Failed</h1>
      <p>{{ errorMessage }}</p>
      <button type="button" class="result-button failed-button" @click="tryAgain">Try Again</button>
      <a href="/index.jsp" class="result-button outline-button">Back to Home</a>
    </section>
  </main>
</template>

<style scoped>
.payment-result-page { position: relative; display: flex; min-height: 100vh; align-items: center; justify-content: center; overflow: hidden; padding: 24px; color: #333; }
.background-blob { position: fixed; z-index: -1; border-radius: 50%; pointer-events: none; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { top: -200px; left: -200px; width: 750px; height: 650px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5),inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { top: -250px; right: -100px; width: 900px; height: 700px; transform: rotate(30deg); background: linear-gradient(145deg,#ffdb4d,#e6b800); box-shadow: inset 10px 10px 40px rgba(255,255,255,.7),inset -30px -30px 60px rgba(184,134,11,.3); }
.blob-orange { bottom: -650px; left: -600px; width: 1800px; height: 950px; transform: rotate(-10deg); background: linear-gradient(145deg,#ffad33,#e68a00); box-shadow: inset 15px 15px 50px rgba(255,255,255,.5),inset -40px -40px 80px rgba(160,82,45,.3); }
.result-card { z-index: 1; width: 450px; max-width: 100%; padding: 48px 30px; border: 1px solid rgba(255,255,255,.6); border-radius: 30px; background: rgba(255,255,255,.75); backdrop-filter: blur(25px); text-align: center; box-shadow: 0 20px 50px rgba(0,0,0,.1); }
.failed-card { width: 400px; }
.icon-circle { display: grid; width: 80px; height: 80px; place-items: center; margin: 0 auto 20px; border-radius: 50%; font-size: 40px; }
.success-icon { background: #d1fae5; color: #10b981; }
.failed-icon { background: #fee2e2; color: #ef4444; }
.result-card h1 { margin: 0 0 10px; color: #2d3436; font-size: 1.5rem; }
.result-card p { margin: 0 0 20px; color: #666; font-size: .95rem; line-height: 1.5; }
.order-id-list { color: #2d3436; font-size: 1.1rem; overflow-wrap: anywhere; }
.shipment-note { display: flex; flex-direction: column; gap: 4px; margin-bottom: 30px; padding: 12px; border-left: 4px solid #3498db; border-radius: 10px; background: #eef2f7; color: #666; font-size: .85rem; line-height: 1.45; text-align: left; }
.shipment-note strong { color: #333; }
.shipment-note i { color: #3498db; }
.result-button { display: block; width: 100%; margin-bottom: 10px; padding: 15px; border-radius: 15px; cursor: pointer; font: inherit; font-weight: 600; text-align: center; text-decoration: none; transition: .2s; }
.success-button { border: 0; background: linear-gradient(45deg,#10b981,#34d399); color: white; box-shadow: 0 5px 15px rgba(16,185,129,.3); }
.failed-button { border: 0; background: linear-gradient(45deg,#ef4444,#f87171); color: white; box-shadow: 0 5px 15px rgba(239,68,68,.3); }
.success-button:hover,.failed-button:hover { transform: translateY(-2px); }
.outline-button { border: 2px solid #ccc; color: #555; }
.outline-button:hover { background: rgba(255,255,255,.6); }
@media (max-width: 480px) { .result-card { padding: 36px 22px; } .result-card h1 { font-size: 1.3rem; } }
</style>
