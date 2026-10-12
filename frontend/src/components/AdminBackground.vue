<template>
  <div class="admin-background" aria-hidden="true">
    <div class="background-blob blob-red"></div>
    <div class="background-blob blob-yellow"></div>
    <div class="background-blob blob-orange"></div>
  </div>
</template>

<style scoped>
.admin-background { position: fixed; z-index: 0; inset: 0; overflow: hidden; pointer-events: none; background: linear-gradient(to bottom,#f0f2f5,#e0e5ec); }
.background-blob { position: absolute; border-radius: 50%; opacity: 1; filter: drop-shadow(30px 40px 50px rgba(0,0,0,.2)); }
.blob-red { top: -200px; left: -200px; width: 750px; height: 650px; transform: rotate(-10deg); background: linear-gradient(145deg,#ff5e55,#d92e25); box-shadow: inset 10px 10px 30px rgba(255,255,255,.5),inset -20px -20px 60px rgba(139,0,0,.4); }
.blob-yellow { top: -250px; right: -100px; width: 900px; height: 700px; transform: rotate(30deg); background: #fff; }
.blob-orange { bottom: -650px; left: -600px; width: 1800px; height: 950px; transform: rotate(-10deg); background: #fff; }
</style>
