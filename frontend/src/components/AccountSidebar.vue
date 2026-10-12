<script setup>
import { legacyPageUrl } from '../api'

defineProps({ active: { type: String, required: true } })
</script>

<template>
  <aside class="sidebar">
    <h2>My Account</h2>
    <nav class="sidebar-nav" aria-label="My account">
      <a href="/profile?tab=profile" :class="['nav-item', { active: active === 'profile' }]">
        <i class="ri-user-line"></i><span>Profile Info</span>
      </a>
      <a href="/customer/orders?status=ALL" :class="['nav-item', { active: active === 'orders' }]">
        <i class="ri-shopping-bag-3-line"></i><span>My Orders</span>
      </a>
      <a href="/profile?tab=addresses" :class="['nav-item', { active: active === 'addresses' }]">
        <i class="ri-map-pin-line"></i><span>Addresses</span>
      </a>
      <a href="/public/wallet/wallet.jsp" class="nav-item">
        <i class="ri-wallet-line"></i><span>Wallet</span>
      </a>
      <a href="/profile?tab=settings" :class="['nav-item', { active: active === 'settings' }]">
        <i class="ri-settings-3-line"></i><span>Settings</span>
      </a>
    </nav>
    <div class="sidebar-footer">
      <a href="/index.jsp" class="btn-logout home-link"><i class="ri-home-4-line"></i> Back to Home</a>
      <a :href="legacyPageUrl('/logout')" class="btn-logout"><i class="ri-logout-box-line"></i> Logout</a>
    </div>
  </aside>
</template>

<style scoped>
.sidebar { position: fixed; z-index: 100; top: 0; bottom: 0; left: 0; display: flex; width: 250px; flex-direction: column; padding: 30px 20px; border-right: 1px solid rgba(255,255,255,.6); background: rgba(255,255,255,.8); backdrop-filter: blur(20px); }
.sidebar h2 { margin: 0 0 40px; color: #e68a00; font-size: 1.5rem; text-align: center; }
.sidebar-nav { display: flex; flex-direction: column; gap: 10px; }
.nav-item { display: flex; align-items: center; gap: 10px; padding: 15px 20px; border-radius: 15px; color: #555; font-weight: 600; text-decoration: none; transition: .2s; }
.nav-item:hover,.nav-item.active { background: linear-gradient(45deg,#ff9500,#ffcc00); color: white; box-shadow: 0 5px 15px rgba(255,149,0,.3); }
.sidebar-footer { display: grid; gap: 10px; margin-top: auto; }
.btn-logout { display: block; padding: 12px; border-radius: 15px; background: #333; color: white; text-align: center; text-decoration: none; transition: .2s; }
.btn-logout:hover { background: #555; }
.home-link { background: #555; }
@media (max-width: 900px) and (min-width: 681px) { .sidebar { width: 210px; padding: 25px 14px; } .nav-item { padding: 12px 10px; } }
@media (max-width: 680px) { .sidebar { position: relative; inset: auto; width: 100%; height: auto; padding: 14px; border-right: 0; border-bottom: 1px solid rgba(255,255,255,.6); } .sidebar h2 { margin: 0 0 10px; font-size: 1.2rem; } .sidebar-nav { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 6px; } .nav-item { padding: 10px; border-radius: 10px; font-size: .85rem; } .sidebar-footer { grid-template-columns: repeat(2,minmax(0,1fr)); margin-top: 10px; } .btn-logout { padding: 10px 6px; border-radius: 10px; font-size: .85rem; } }
</style>
