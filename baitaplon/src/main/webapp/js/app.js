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
    {
      key: "books",
      label: "Quản lý Sách",
      icon: "📚",
      href: "books.html",
      children: [
        { key: "list", label: "Danh Sách Đầu Sách", href: "books.html" },
        { key: "categories", label: "Quản Lý Thể Loại", href: "categories.html" },
        { key: "shelves", label: "Quản Lý Kệ Sách", href: "shelves.html" }
      ]
    },
    {
      key: "loans",
      label: "Mượn / Trả Sách",
      icon: "🔄",
      href: "loans.html",
      children: [
        { key: "borrow", label: "Lập Phiếu Mượn Sách", href: "loans.html?tab=borrow", tabId: "borrow-tab" },
        { key: "return", label: "Nhận Trả Sách", href: "loans.html?tab=return", tabId: "return-tab" },
        { key: "list", label: "Danh Sách Phiếu Mượn", href: "loans.html?tab=list", tabId: "list-tab" }
      ]
    },
    { key: "readers", label: "Quản lý Độc giả", icon: "👥", href: "readers.html" },
    { key: "fines", label: "Hóa đơn Phạt", icon: "💰", href: "fines.html" },
    { key: "reservations", label: "Đặt trước Sách", icon: "🔖", href: "reservations.html" },
    // { key: "barcodes", label: "Tem & Mã Vạch Mẫu", icon: "🏷️", href: "barcode-samples.html" },
    { key: "dashboard", label: "Dashboard Thống kê", icon: "📊", href: "dashboard.html" },
    // { key: "rules", label: "Nội quy & Chính sách", icon: "📜", href: "rules.html" },
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
      <div style="color:#cbd5e1;font-weight:600;margin-bottom:2px;">SLMS • UTC Library</div>
      <div style="font-size:11px;color:#64748b;">Hệ Thống Thư Viện UTC</div>
    </div>
  `;

  sidebar.innerHTML = html;
  renderAppHeader();
}

function handleMenuParentClick(event, menuKey) {
  const currentPath = window.location.pathname.toLowerCase();
  if (menuKey === "loans" && (currentPath.includes("loans.html") || currentPath.endsWith("/loans") || currentPath.endsWith("loans"))) {
    event.preventDefault();
    if (typeof switchTab === "function") {
      switchTab("borrow-tab");
    }
  }
}

function handleSubItemClick(event, tabId, subKey) {
  const currentPath = window.location.pathname.toLowerCase();
  if (tabId && (currentPath.includes("loans.html") || currentPath.endsWith("/loans") || currentPath.endsWith("loans"))) {
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
    }).catch(() => { });
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

// ==================== TỰ ĐỘNG ĐÓNG POP-UP (MODAL) VÀ DROPDOWN KHI NHẤP RA NGOÀI ====================
// Đóng tất cả pop-up (modal) khi người dùng nhấp ra ngoài phần nội dung (nhấp vào modal-overlay)
document.addEventListener("click", (e) => {
  // Đóng user dropdown menu
  const userMenuContainer = document.getElementById("user-menu-container");
  if (userMenuContainer && !userMenuContainer.contains(e.target)) {
    const dropdown = document.getElementById("user-dropdown-menu");
    if (dropdown) dropdown.classList.remove("show");
    userMenuContainer.classList.remove("open");
  }

  // Đóng modal overlay khi bấm ra ngoài
  if (e.target && e.target.classList && e.target.classList.contains("modal-overlay")) {
    e.target.classList.remove("open");
    e.target.classList.remove("active");
  }
});

// Đóng modal và dropdown khi nhấn phím Escape
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    document.querySelectorAll(".modal-overlay.open, .modal-overlay.active").forEach(modal => {
      modal.classList.remove("open");
      modal.classList.remove("active");
    });
    const dropdown = document.getElementById("user-dropdown-menu");
    if (dropdown) dropdown.classList.remove("show");
    const userMenuContainer = document.getElementById("user-menu-container");
    if (userMenuContainer) userMenuContainer.classList.remove("open");
  }
});

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

// ==================== GLOBAL APP HEADER (ULTRA ADMIN STYLE) ====================
function renderAppHeader() {
  const mainWrapper = document.querySelector(".main-wrapper");
  if (!mainWrapper) return;

  const staffName = sessionStorage.getItem("slms_staff_name") || "Alan Wilson";
  const staffRole = sessionStorage.getItem("slms_staff_role") || "Thủ thư Quản trị";
  let staffAvatar = sessionStorage.getItem("slms_staff_avatar");

  // Nếu chưa có avatar trong session, tự động chọn avatar mẫu tương ứng với vai trò
  if (!staffAvatar || staffAvatar.trim() === "") {
    if ((staffRole || "").toUpperCase().includes("ADMIN")) {
      staffAvatar = "images/avatar-admin.svg";
    } else {
      staffAvatar = "images/avatar-staff.svg";
    }
  }

  let header = document.getElementById("app-header");
  const avatarFallback = `this.onerror=null;this.src='images/avatar-admin.svg';`;

  const headerHtml = `
    <div class="header-left">
      <button type="button" class="header-icon-btn" id="sidebar-toggle-btn" title="Thu gọn / Mở rộng Menu" onclick="toggleSidebar()">
        <span style="font-size:18px;">☰</span>
      </button>
      <div class="header-badges">
      </div>
    </div>

    <div class="header-right">
      <!-- User profile menu (đẩy ra ngoài cùng bên phải) -->
      <div class="user-menu-container" id="user-menu-container">
        <button type="button" class="user-profile-trigger" id="user-profile-trigger" onclick="toggleUserDropdown(event)">
          <div class="user-avatar" title="${escapeHtml(staffName)}">
            <img id="header-avatar-img" src="${escapeHtml(staffAvatar)}" alt="Avatar" class="user-avatar-img" onerror="${avatarFallback}">
          </div>
          <span class="user-name">${escapeHtml(staffName)}</span>
          <span class="user-caret">▾</span>
        </button>

        <!-- Dropdown Menu dạng Ultra Admin -->
        <div class="user-dropdown-menu" id="user-dropdown-menu">
          <div class="dropdown-header-info">
            <div class="dropdown-user-name">${escapeHtml(staffName)}</div>
            <div class="dropdown-user-role">${escapeHtml(staffRole)}</div>
          </div>
          <div class="dropdown-divider"></div>
          <a href="javascript:void(0)" class="dropdown-item" onclick="openProfileModal()">
            <span class="dropdown-icon">👤</span>
            <span>Profile (Tài khoản)</span>
          </a>
          <a href="rules.html" class="dropdown-item">
            <span class="dropdown-icon">ℹ️</span>
            <span>Help (Chính sách)</span>
          </a>
          <a href="javascript:void(0)" class="dropdown-item" onclick="openSettingsModal()">
            <span class="dropdown-icon">🔧</span>
            <span>Settings (Cài đặt)</span>
          </a>
          <div class="dropdown-divider"></div>
          <a href="javascript:void(0)" class="dropdown-item logout" onclick="staffLogout()">
            <span class="dropdown-icon">🔒</span>
            <span>Logout</span>
          </a>
        </div>
      </div>
    </div>
  `;

  if (!header) {
    header = document.createElement("header");
    header.className = "app-header";
    header.id = "app-header";
    header.innerHTML = headerHtml;
    mainWrapper.prepend(header);
  } else {
    header.innerHTML = headerHtml;
  }

  injectUserModals(staffName, staffRole, staffAvatar);
}

function injectUserModals(staffName, staffRole, staffAvatar) {
  let profileModal = document.getElementById("profile-modal");
  if (profileModal) {
    const avatarImg = document.getElementById("profile-modal-avatar-preview");
    if (avatarImg) avatarImg.src = staffAvatar || "images/avatar-admin.svg";
    const nameEl = document.getElementById("profile-modal-user-name");
    if (nameEl) nameEl.textContent = staffName;
    const roleEl = document.getElementById("profile-modal-user-role");
    if (roleEl) roleEl.textContent = staffRole;
    const inputAvatar = document.getElementById("input-staff-avatar");
    if (inputAvatar) inputAvatar.value = staffAvatar || "";
    return;
  }

  const modalsHtml = `
    <!-- Modal Profile -->
    <div id="profile-modal" class="modal-overlay">
      <div class="modal-box" style="max-width:440px;" onclick="event.stopPropagation()">
        <div class="modal-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3 class="modal-title" style="margin:0;font-size:18px;">Thông Tin Cá Nhân (Profile)</h3>
          <button type="button" class="btn btn-secondary btn-sm" onclick="closeModal('profile-modal')">✕</button>
        </div>
        <div class="modal-body" style="text-align:center;padding:12px 0;">
          <div style="width:72px;height:72px;margin:0 auto 12px auto;border-radius:50%;overflow:hidden;border:2px solid #3b82f6;display:flex;align-items:center;justify-content:center;background:#f1f5f9;">
            <img id="profile-modal-avatar-preview" src="${escapeHtml(staffAvatar)}" alt="Avatar" style="width:100%;height:100%;object-fit:cover;" onerror="this.onerror=null;this.src='images/avatar-admin.svg';">
          </div>
          <h4 id="profile-modal-user-name" style="font-size:18px;font-weight:700;color:#0f172a;margin-bottom:4px;">${escapeHtml(staffName)}</h4>
          <div id="profile-modal-user-role" style="display:inline-block;padding:3px 14px;background:#eff6ff;color:#2563eb;font-weight:600;font-size:12px;border-radius:999px;margin-bottom:14px;">
            ${escapeHtml(staffRole)}
          </div>

          <!-- Đổi ảnh avatar -->
          <div class="form-group" style="margin-bottom:16px;text-align:left;">
            <label style="font-size:12px;font-weight:600;color:#475569;display:block;margin-bottom:4px;">Ảnh đại diện (Đường dẫn / URL):</label>
            <div style="display:flex;gap:8px;">
              <input type="text" id="input-staff-avatar" value="${escapeHtml(staffAvatar)}" placeholder="images/avatar-admin.svg hoặc URL..." style="flex:1;padding:6px 10px;font-size:12.5px;border:1px solid #cbd5e1;border-radius:6px;">
              <button type="button" class="btn btn-primary btn-sm" onclick="saveStaffAvatar()">Lưu Avatar</button>
            </div>
            <div style="font-size:11px;color:#94a3b8;margin-top:4px;">
              Gợi ý: <code>images/avatar-admin.svg</code> hoặc <code>images/avatar-staff.svg</code>
            </div>
          </div>

          <div style="text-align:left;background:#f8fafc;border-radius:8px;padding:12px 16px;border:1px solid #e2e8f0;font-size:13px;line-height:1.8;">
            <div><strong>Đơn vị:</strong> Thư viện Đại học Giao thông Vận tải (UTC)</div>
            <div><strong>Mã cán bộ:</strong> CB-UTC-2026</div>
            <div><strong>Trạng thái:</strong> <span style="color:#16a34a;font-weight:600;">● Đang làm việc (Online)</span></div>
            <div><strong>Quyền truy cập:</strong> Quản lý toàn quyền hệ thống SLMS</div>
          </div>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:10px;margin-top:16px;">
          <button class="btn btn-secondary" onclick="closeModal('profile-modal')">Đóng</button>
          <button class="btn btn-danger" onclick="staffLogout()">Đăng Xuất</button>
        </div>
      </div>
    </div>

    <!-- Modal Settings -->
    <div id="settings-modal" class="modal-overlay">
      <div class="modal-box" style="max-width:460px;" onclick="event.stopPropagation()">
        <div class="modal-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3 class="modal-title" style="margin:0;font-size:18px;">Cài Đặt Hệ Thống (Settings)</h3>
          <button type="button" class="btn btn-secondary btn-sm" onclick="closeModal('settings-modal')">✕</button>
        </div>
        <div class="modal-body" style="padding:8px 0;">
          <div class="form-group" style="margin-bottom:16px;">
            <label style="font-size:13px;font-weight:600;display:block;margin-bottom:6px;">Chế độ giao diện</label>
            <select style="width:100%;padding:8px 12px;border:1px solid #cbd5e1;border-radius:6px;font-size:13px;">
              <option selected>Sáng (Light Mode - Chuẩn Dashboard)</option>
              <option disabled>Tối (Dark Mode - Đang phát triển)</option>
            </select>
          </div>
          <div class="form-group" style="margin-bottom:16px;">
            <label style="font-size:13px;font-weight:600;display:block;margin-bottom:6px;">Cơ sở dữ liệu kết nối</label>
            <input type="text" value="Oracle Database 23ai Free (localhost:1521/FREE)" readonly style="width:100%;padding:8px 12px;border:1px solid #cbd5e1;border-radius:6px;background:#f1f5f9;color:#64748b;font-size:13px;">
          </div>
          <div class="form-group" style="margin-bottom:16px;">
            <label style="font-size:13px;font-weight:600;display:block;margin-bottom:6px;">Trạng thái hệ thống</label>
            <div style="font-size:13px;color:#16a34a;background:#f0fdf4;padding:8px 12px;border-radius:6px;border:1px solid #bbf7d0;">
              ✓ HTTP Server & Auto-Reload đang hoạt động bình thường
            </div>
          </div>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:10px;margin-top:16px;">
          <button class="btn btn-primary" onclick="closeModal('settings-modal')">Xác Nhận</button>
        </div>
      </div>
    </div>
  `;

  document.body.insertAdjacentHTML("beforeend", modalsHtml);
}

async function saveStaffAvatar() {
  const input = document.getElementById("input-staff-avatar");
  const newAvatar = (input ? input.value : "").trim();
  if (!newAvatar) {
    alert("Vui lòng nhập đường dẫn hoặc URL ảnh đại diện!");
    return;
  }

  try {
    const token = sessionStorage.getItem("slms_staff_token");
    if (token) {
      await fetchAPI("/api/auth/avatar", {
        method: "PUT",
        body: JSON.stringify({ avatar: newAvatar })
      });
    }
  } catch (e) {
    console.warn("Lưu avatar vào backend thất bại (hoặc chưa đăng nhập):", e);
  }

  sessionStorage.setItem("slms_staff_avatar", newAvatar);

  // Cập nhật ngay lập tức ảnh trên header và trong modal
  const headerImg = document.getElementById("header-avatar-img");
  if (headerImg) headerImg.src = newAvatar;
  const previewImg = document.getElementById("profile-modal-avatar-preview");
  if (previewImg) previewImg.src = newAvatar;

  alert("Đã cập nhật ảnh đại diện thành công!");
}

function toggleSidebar() {
  document.body.classList.toggle("sidebar-collapsed");
}

function toggleUserDropdown(event) {
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }
  const container = document.getElementById("user-menu-container");
  const dropdown = document.getElementById("user-dropdown-menu");
  if (container && dropdown) {
    const isShown = dropdown.classList.contains("show");
    if (isShown) {
      dropdown.classList.remove("show");
      container.classList.remove("open");
    } else {
      dropdown.classList.add("show");
      container.classList.add("open");
    }
  }
}

function openProfileModal() {
  const dropdown = document.getElementById("user-dropdown-menu");
  const container = document.getElementById("user-menu-container");
  if (dropdown) dropdown.classList.remove("show");
  if (container) container.classList.remove("open");
  const modal = document.getElementById("profile-modal");
  if (modal) modal.classList.add("open");
}

function openSettingsModal() {
  const dropdown = document.getElementById("user-dropdown-menu");
  const container = document.getElementById("user-menu-container");
  if (dropdown) dropdown.classList.remove("show");
  if (container) container.classList.remove("open");
  const modal = document.getElementById("settings-modal");
  if (modal) modal.classList.add("open");
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove("open");
    modal.classList.remove("active");
  }
}

// ==================== PHÂN TRANG (PAGINATION) ====================
window._paginationCallbacks = window._paginationCallbacks || {};

function paginateItems(items, currentPage, pageSize) {
  if (!items || items.length === 0) return [];
  const validPage = Math.max(1, currentPage || 1);
  const size = Math.max(1, pageSize || 10);
  const start = (validPage - 1) * size;
  return items.slice(start, start + size);
}

function renderPaginationControls({ containerId, totalItems, currentPage, pageSize, onPageChange }) {
  const container = document.getElementById(containerId);
  if (!container) return;

  if (totalItems <= 0) {
    container.innerHTML = "";
    return;
  }

  const pSize = Math.max(1, pageSize || 10);
  const totalPages = Math.ceil(totalItems / pSize) || 1;
  const currPage = Math.min(Math.max(1, currentPage || 1), totalPages);

  const startItem = (currPage - 1) * pSize + 1;
  const endItem = Math.min(currPage * pSize, totalItems);

  window._paginationCallbacks[containerId] = (p, ps) => {
    if (typeof onPageChange === "function") {
      onPageChange(p, ps || pSize);
    }
  };

  // Tạo danh sách số trang có dấu ba chấm
  let pages = [];
  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) pages.push(i);
  } else {
    pages.push(1);
    if (currPage > 3) pages.push("...");

    let startP = Math.max(2, currPage - 1);
    let endP = Math.min(totalPages - 1, currPage + 1);

    if (currPage <= 3) {
      startP = 2;
      endP = 4;
    } else if (currPage >= totalPages - 2) {
      startP = totalPages - 3;
      endP = totalPages - 1;
    }

    for (let i = startP; i <= endP; i++) {
      pages.push(i);
    }

    if (currPage < totalPages - 2) pages.push("...");
    pages.push(totalPages);
  }

  let controlsHtml = `
    <button class="page-btn" ${currPage === 1 ? "disabled" : ""} 
      onclick="window._paginationCallbacks['${containerId}'](${currPage - 1})">
      &laquo; Trước
    </button>
  `;

  pages.forEach(p => {
    if (p === "...") {
      controlsHtml += `<span class="page-ellipsis">&hellip;</span>`;
    } else {
      controlsHtml += `
        <button class="page-btn ${p === currPage ? "active" : ""}" 
          onclick="window._paginationCallbacks['${containerId}'](${p})">
          ${p}
        </button>
      `;
    }
  });

  controlsHtml += `
    <button class="page-btn" ${currPage === totalPages ? "disabled" : ""} 
      onclick="window._paginationCallbacks['${containerId}'](${currPage + 1})">
      Sau &raquo;
    </button>
  `;

  const html = `
    <div class="pagination-wrapper">
      <div class="pagination-info">
        <span>Hiển thị <strong>${startItem} - ${endItem}</strong> trên tổng số <strong>${totalItems}</strong> mục</span>
        <span style="color: var(--border);">|</span>
        <label style="display:inline-flex;align-items:center;gap:6px;font-weight:normal;color:var(--text-muted);cursor:pointer;">
          <span>Số dòng/trang:</span>
          <select class="page-size-select" onchange="window._paginationCallbacks['${containerId}'](1, parseInt(this.value))">
            <option value="5" ${pSize === 5 ? "selected" : ""}>5</option>
            <option value="10" ${pSize === 10 ? "selected" : ""}>10</option>
            <option value="20" ${pSize === 20 ? "selected" : ""}>20</option>
            <option value="50" ${pSize === 50 ? "selected" : ""}>50</option>
          </select>
        </label>
      </div>
      <div class="pagination-controls">
        ${controlsHtml}
      </div>
    </div>
  `;

  container.innerHTML = html;
}

// Tự động khởi tạo Header khi DOM sẵn sàng
document.addEventListener("DOMContentLoaded", () => {
  renderAppHeader();
});



