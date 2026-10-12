<script setup>
import { legacyPageUrl } from '../api'

defineProps({ active: { type: String, default: 'dashboard' } })
</script>

<template>
  <div class="pg-merchant-sidebar">
    <aside class="sidebar-card" aria-label="Seller center">
      <div class="menu-group">
        <div class="menu-group-title">Overview</div>
        <a :class="['menu-item', { 'active-view': active === 'dashboard' }]" href="/merchant/merchant_dashboard.jsp" title="Dashboard">
          <span class="pg-mi" aria-hidden="true"><svg viewBox="0 0 24 24"><rect x="3" y="3" width="8" height="8" rx="2"></rect><rect x="13" y="3" width="8" height="5" rx="2"></rect><rect x="13" y="10" width="8" height="11" rx="2"></rect><rect x="3" y="13" width="8" height="8" rx="2"></rect></svg></span>
          <span>Dashboard</span>
        </a>
      </div>
      <div class="menu-group">
        <div class="menu-group-title">Manage</div>
        <a :class="['menu-item', { 'active-view': active === 'products' }]" href="/merchant/product/product_management.jsp" title="Products">
          <span class="pg-mi" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><path d="M3.3 7l8.7 5 8.7-5"></path><path d="M12 22V12"></path></svg></span>
          <span>Products</span>
        </a>
        <a :class="['menu-item', { 'active-view': active === 'publish' }]" href="/merchant/product/publish.jsp" title="Publish">
          <span class="pg-mi" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 5v14"></path><path d="M5 12h14"></path></svg></span>
          <span>Publish</span>
        </a>
        <a :class="['menu-item', { 'active-view': active === 'order' }]" href="/merchant/order/order_management" title="Orders">
          <span class="pg-mi" aria-hidden="true"><svg viewBox="0 0 24 24"><rect x="4" y="3" width="16" height="18" rx="2"></rect><path d="M8 7h8"></path><path d="M8 11h8"></path><path d="M8 15h6"></path></svg></span>
          <span>Orders</span>
        </a>
        <a :class="['menu-item', { 'active-view': active === 'wallet' }]" href="/public/wallet/wallet.jsp" title="Wallet">
          <span class="pg-mi" aria-hidden="true"><svg viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line></svg></span>
          <span>Wallet</span>
        </a>
      </div>
      <div class="menu-group logout-group">
        <a class="menu-item menu-logout" :href="legacyPageUrl('/logout')" title="Logout">
          <span class="pg-mi" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><path d="M16 17l5-5-5-5"></path><path d="M21 12H9"></path></svg></span>
          <span>Logout</span>
        </a>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.pg-merchant-sidebar { --pg-merchant-primary: var(--primary,#ff9500); --pg-merchant-secondary: var(--secondary,#ff5e55); --pg-merchant-text-dark: var(--text-dark,#2d3436); --pg-merchant-text-gray: var(--text-gray,#636e72); --pg-merchant-card-radius: var(--card-radius,16px); --pg-merchant-card-shadow: var(--card-shadow,0 10px 30px rgba(0,0,0,.05)); }
.pg-merchant-sidebar,.pg-merchant-sidebar *,.pg-merchant-sidebar *::before,.pg-merchant-sidebar *::after { box-sizing: border-box; font-family: Poppins,sans-serif; }
.sidebar-card { position: sticky; top: 100px; display: flex; height: calc(100vh - 130px); flex-direction: column; padding: 26px 18px; border-radius: var(--pg-merchant-card-radius); background: rgba(255,255,255,.98); box-shadow: var(--pg-merchant-card-shadow); }
.menu-group { display: flex; flex-direction: column; }
.menu-group + .menu-group { margin-top: 18px; }
.menu-group-title { margin: 0 12px 10px; color: #b2bec3; font-size: .78rem; font-weight: 800; letter-spacing: 1.6px; text-transform: uppercase; }
.menu-item { position: relative; display: flex; width: auto; align-items: center; gap: 12px; margin: 4px 6px; padding: 12px 14px; border: 0; border-radius: 14px; background: transparent; color: var(--pg-merchant-text-dark); cursor: pointer; font-size: .98rem; font-weight: 650; line-height: 1; text-align: left; text-decoration: none; transition: transform .2s ease,background .2s ease,color .2s ease,box-shadow .2s ease; }
.pg-mi { display: inline-flex; width: 30px; height: 30px; flex: 0 0 30px; align-items: center; justify-content: center; border-radius: 10px; background: rgba(0,0,0,.04); color: var(--pg-merchant-text-gray); transition: background .2s ease,color .2s ease; }
.pg-mi svg { display: block; width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.pg-merchant-sidebar svg { display: block !important; visibility: visible !important; opacity: 1 !important; }
.pg-merchant-sidebar svg path,.pg-merchant-sidebar svg rect,.pg-merchant-sidebar svg circle,.pg-merchant-sidebar svg line,.pg-merchant-sidebar svg polyline,.pg-merchant-sidebar svg polygon { fill: none !important; stroke: currentColor !important; stroke-width: 2 !important; opacity: 1 !important; visibility: visible !important; vector-effect: non-scaling-stroke; }
.menu-item:hover { transform: translateX(4px); background: #fff4e6; color: var(--pg-merchant-primary); }
.menu-item:hover .pg-mi { background: rgba(255,149,0,.14); color: var(--pg-merchant-primary); }
.menu-item.active-view { transform: none; background: linear-gradient(45deg,var(--pg-merchant-primary),var(--pg-merchant-secondary)); box-shadow: 0 8px 22px rgba(255,94,85,.25); color: white; }
.menu-item.active-view .pg-mi { background: rgba(255,255,255,.18); color: white; }
.menu-item.active-view::after { position: absolute; top: 50%; left: 10px; width: 34px; height: 34px; transform: translateY(-50%); border-radius: 12px; background: rgba(255,255,255,.14); content: ''; pointer-events: none; }
.logout-group { margin-top: auto !important; }
.menu-item.menu-logout { color: #d63031; }
.menu-item.menu-logout .pg-mi { background: rgba(214,48,49,.08); color: #d63031; }
.menu-item.menu-logout:hover { transform: translateX(4px); background: rgba(214,48,49,.1); color: #d63031; }
.menu-item.menu-logout:hover .pg-mi { background: rgba(214,48,49,.16); color: #d63031; }
@media(max-width:1024px) { .sidebar-card { align-items: center; padding: 20px 10px; } .menu-item span:not(.pg-mi),.menu-group-title { display: none; } .menu-item { margin: 6px 0; padding: 12px; } }
</style>
