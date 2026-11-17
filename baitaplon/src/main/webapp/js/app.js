/**
 * SLMS - Common JavaScript Utility & Sidebar Renderer
 */

const API_BASE = "";

// Gọi API với xử lý chung
async function fetchAPI(endpoint, options = {}) {
  try {
    const headers = {
      "Content-Type": "application/json",
      ...(options.headers || {})
    };
    const staffToken = sessionStorage.getItem("slms_staff_token");
    if (staffToken) {
      headers.Authorization = "Bearer " + staffToken;
    }
    const res = await fetch(API_BASE + endpoint, {
      ...options,
      headers
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

// Đã tắt toàn bộ alert / toast ở góc dưới màn hình khi thực hiện action theo yêu cầu
function showToast(message, type = "success") {
  // Không hiển thị alert / toast ở góc dưới màn hình
  console.log(`[SLMS ${type}]: ${message}`);
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
function renderSidebar(activeKey, activeSubKey = "borrow") {
  const sidebar = document.querySelector(".sidebar");
  if (!sidebar) return;

  const menu = [
    { key: "books", label: "Quản lý Sách", icon: "📚", href: "books.html" },
    {
      key: "loans",
      label: "Mượn / Trả Sách",
      icon: "🔄",
      href: "loans.html",
      children: [
        { key: "borrow", label: "Lập Phiếu Mượn Sách", href: "loans.html?tab=borrow", tabId: "borrow-tab" },
        { key: "return", label: "Nhận Trả Sách & Sự Cố", href: "loans.html?tab=return", tabId: "return-tab" },
        { key: "list", label: "Danh Sách Phiếu Mượn", href: "loans.html?tab=list", tabId: "list-tab" }
      ]
    },
    { key: "readers", label: "Quản lý Độc giả", icon: "👥", href: "readers.html" },
    { key: "fines", label: "Hóa đơn Phạt", icon: "💰", href: "fines.html" },
    { key: "reservations", label: "Đặt trước Sách", icon: "🔖", href: "reservations.html" },
    { key: "dashboard", label: "Dashboard Thống kê", icon: "📊", href: "dashboard.html" },
    { key: "rules", label: "Nội quy & Chính sách", icon: "📜", href: "rules.html" },
    { key: "client", label: "Cổng độc giả", icon: "🧑‍🎓", href: "client/login.html" }
  ];

  const staffName = sessionStorage.getItem("slms_staff_name") || "Nhân viên";
  const staffRole = sessionStorage.getItem("slms_staff_role") || "";

  let html = `
    <div class="brand" style="cursor: pointer;" onclick="window.location.href='books.html'">
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
    const hasChildren = item.children && item.children.length > 0;

    if (!hasChildren) {
      html += `
        <li class="nav-item ${isActive}">
          <a href="${item.href}">
            <span>${item.label}</span>
          </a>
        </li>
      `;
    } else {
      // Menu cha có sub-menu con (Mượn / Trả Sách)
      html += `
        <li class="nav-item has-sub ${isActive}">
          <a href="${item.href}" class="nav-parent" onclick="handleMenuParentClick(event, '${item.key}')">
            <span>${item.label}</span>
            <span class="sub-arrow">${isActive ? '▾' : '▸'}</span>
          </a>
          <ul class="nav-submenu ${isActive ? 'show' : ''}" id="submenu-${item.key}">
      `;
      item.children.forEach(sub => {
        const isSubActive = isActive && (sub.key === activeSubKey);
        html += `
          <li class="nav-subitem ${isSubActive ? 'active' : ''}" data-tab="${sub.tabId}" data-key="${sub.key}">
            <a href="${sub.href}" onclick="handleSubItemClick(event, '${sub.tabId}', '${sub.key}')">
              <span>${sub.label}</span>
            </a>
          </li>
        `;
      });
      html += `
          </ul>
        </li>
      `;
    }
  });

  html += `
    </ul>
    <div class="sidebar-footer">
      <div style="color:#e2e8f0;font-weight:600;margin-bottom:2px;">${staffName}</div>
      <div style="font-size:11px;color:#94a3b8;margin-bottom:8px;">${staffRole}</div>
      <button type="button" class="btn btn-secondary btn-sm" style="width:100%;margin-bottom:8px;" onclick="staffLogout()">Đăng xuất</button>
      <div>Hệ Thống Thư Viện UTC</div>
    </div>
  `;

  sidebar.innerHTML = html;
}

function handleMenuParentClick(event, menuKey) {
  const currentPath = window.location.pathname.toLowerCase();
  if (currentPath.includes("loans.html") || currentPath.endsWith("/loans") || currentPath.endsWith("loans")) {
    event.preventDefault();
    if (typeof switchTab === "function") {
      switchTab("borrow-tab");
    }
  }
}

function handleSubItemClick(event, tabId, subKey) {
  const currentPath = window.location.pathname.toLowerCase();
  if (currentPath.includes("loans.html") || currentPath.endsWith("/loans") || currentPath.endsWith("loans")) {
    event.preventDefault();
    if (typeof switchTab === "function") {
      switchTab(tabId);
    }
  }
}

function staffLogout() {
  const token = sessionStorage.getItem("slms_staff_token");
  if (token) {
    fetch("/api/auth/logout", {
      method: "POST",
      headers: { Authorization: "Bearer " + token }
    }).catch(() => {});
  }
  sessionStorage.removeItem("slms_staff_token");
  sessionStorage.removeItem("slms_staff_role");
  sessionStorage.removeItem("slms_staff_name");
  window.location.href = "staff-login.html";
}

// Soft gate: trang admin yêu cầu đăng nhập staff (trừ staff-login)
(function guardStaffPages() {
  const path = window.location.pathname || "";
  if (path.endsWith("staff-login.html") || path.includes("/client/")) return;
  if (!sessionStorage.getItem("slms_staff_token")) {
    // Cho phép xem tạm khi demo — bỏ comment dòng dưới nếu muốn bắt buộc login
    // window.location.href = "staff-login.html";
  }
})();

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

// ==================== TỰ ĐỘNG ĐÓNG POP-UP (MODAL) KHI NHẤP RA NGOÀI ====================
// Đóng tất cả pop-up (modal) khi người dùng nhấp ra ngoài phần nội dung (nhấp vào modal-overlay)
document.addEventListener("click", (e) => {
  if (e.target && e.target.classList && e.target.classList.contains("modal-overlay")) {
    e.target.classList.remove("open");
  }
});

// Đóng modal khi nhấn phím Escape
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    document.querySelectorAll(".modal-overlay.open").forEach(modal => {
      modal.classList.remove("open");
    });
  }
});



