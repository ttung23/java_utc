/**
 * SLMS - Common JavaScript Utility & Sidebar Renderer
 */

const API_BASE = "";

// Gọi API với xử lý chung
async function fetchAPI(endpoint, options = {}) {
  try {
    const res = await fetch(API_BASE + endpoint, {
      headers: {
        "Content-Type": "application/json",
        ...(options.headers || {})
      },
      ...options
    });

    const json = await res.json();
    if (!json.success) {
      throw new Error(json.message || "Đã xảy ra lỗi");
    }
    return json.data;
  } catch (err) {
    showToast(err.message, "danger");
    throw err;
  }
}

// Hiển thị Toast thông báo góc phải màn hình
function showToast(message, type = "success") {
  let toastContainer = document.getElementById("toast-container");
  if (!toastContainer) {
    toastContainer = document.createElement("div");
    toastContainer.id = "toast-container";
    toastContainer.style.cssText = "position:fixed;bottom:24px;right:24px;z-index:9999;display:flex;flex-direction:column;gap:10px;";
    document.body.appendChild(toastContainer);
  }

  const toast = document.createElement("div");
  const bg = type === "success" ? "#16a34a" : type === "danger" ? "#dc2626" : "#d97706";
  toast.style.cssText = `background:${bg};color:#fff;padding:12px 20px;border-radius:8px;box-shadow:0 10px 15px -3px rgba(0,0,0,0.1);font-size:14px;font-weight:500;display:flex;align-items:center;gap:10px;animation:slideIn 0.3s ease;`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : '⚠'}</span> <span>${message}</span>`;

  toastContainer.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transition = "opacity 0.4s";
    setTimeout(() => toast.remove(), 400);
  }, 4000);
}

// Định dạng tiền tệ VNĐ
function formatCurrency(amount) {
  if (amount === undefined || amount === null) return "0 đ";
  return Number(amount).toLocaleString("vi-VN") + " đ";
}

// Định dạng ngày giờ
function formatDateTime(dateStr) {
  if (!dateStr) return "-";
  return dateStr.replace("T", " ");
}

// Render Sidebar động cho tất cả các trang
function renderSidebar(activeKey) {
  const sidebar = document.querySelector(".sidebar");
  if (!sidebar) return;

  const menu = [
    { key: "dashboard", label: "Dashboard Thống kê", icon: "📊", href: "index.html" },
    { key: "loans", label: "Mượn / Trả Sách (UC)", icon: "🔄", href: "loans.html" },
    { key: "books", label: "Quản lý Sách & Bản sao", icon: "📚", href: "books.html" },
    { key: "readers", label: "Quản lý Độc giả", icon: "👥", href: "readers.html" },
    { key: "fines", label: "Hóa đơn Phạt", icon: "💰", href: "fines.html" },
    { key: "reservations", label: "Đặt trước Sách", icon: "🔖", href: "reservations.html" }
  ];

  let html = `
    <div class="brand">
      <div class="brand-icon">🏛️</div>
      <div>
        <div class="brand-title">SLMS Library</div>
        <div class="brand-subtitle">Smart Library System</div>
      </div>
    </div>
    <ul class="nav-menu">
  `;

  menu.forEach(item => {
    const isActive = item.key === activeKey ? "active" : "";
    html += `
      <li class="nav-item ${isActive}">
        <a href="${item.href}">
          <span style="font-size:18px;">${item.icon}</span>
          <span>${item.label}</span>
        </a>
      </li>
    `;
  });

  html += `
    </ul>
    <div class="sidebar-footer">
      <div>Hệ Thống Thư Viện UTC</div>
      <div style="font-size:11px;color:#64748b;margin-top:2px;">Phiên bản Java & Oracle DB</div>
    </div>
  `;

  sidebar.innerHTML = html;
}

// ==================== AUTO LIVE RELOAD ====================
(function initAutoReload() {
  let initialVersion = null;
  let offline = false;

  setInterval(async () => {
    try {
      const res = await fetch("/api/version?_t=" + Date.now());
      if (res.ok) {
        const ver = await res.text();
        if (initialVersion === null) {
          initialVersion = ver;
        } else if (initialVersion !== ver) {
          console.log("🔥 [LiveReload] Phát hiện server cập nhật bản build mới. Đang tự động refresh...");
          window.location.reload();
        }
        if (offline) {
          offline = false;
          window.location.reload();
        }
      }
    } catch (e) {
      // Server đang reboot trong quá trình rebuild
      offline = true;
    }
  }, 1000);
})();


