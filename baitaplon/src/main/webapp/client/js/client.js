/**
 * SLMS - SMART LIBRARY MANAGEMENT SYSTEM (CLIENT PORTAL)
 * Core Client Utilities, Cart State, Modals & Components
 */

const API_BASE = "";
const TOKEN_KEY = "slms_token";
const SESSION_KEY = "slms_reader_id";
const SESSION_NAME_KEY = "slms_reader_name";
const ROLE_KEY = "slms_role";
const CART_STORAGE_KEY = "slms_borrow_cart";
const CART_MAX_ITEMS = 5;

/* ==========================================================================
   Auth & Session Functions
   ========================================================================== */
function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

function getReaderId() {
  return sessionStorage.getItem(SESSION_KEY);
}

function getReaderName() {
  return sessionStorage.getItem(SESSION_NAME_KEY) || "";
}

function setSession(authData) {
  sessionStorage.setItem(TOKEN_KEY, authData.token);
  sessionStorage.setItem(SESSION_KEY, authData.userId);
  sessionStorage.setItem(SESSION_NAME_KEY, authData.displayName || "");
  sessionStorage.setItem(ROLE_KEY, authData.role || "READER");
}

function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(SESSION_KEY);
  sessionStorage.removeItem(SESSION_NAME_KEY);
  sessionStorage.removeItem(ROLE_KEY);
}

function requireLogin() {
  const id = getReaderId();
  const token = getToken();
  if (!id || !token) {
    window.location.href = "login.html";
    return null;
  }
  return id;
}

async function logout() {
  try {
    await fetchAPI("/api/auth/logout", { method: "POST" });
  } catch (e) {
    /* ignore */
  }
  clearSession();
  window.location.href = "login.html";
}

/* ==========================================================================
   API Helper
   ========================================================================== */
async function fetchAPI(endpoint, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };
  const token = getToken();
  if (token) {
    headers.Authorization = "Bearer " + token;
  }

  try {
    const res = await fetch(API_BASE + endpoint, { ...options, headers });
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

/* ==========================================================================
   Toast Notification System (Top-Right)
   ========================================================================== */
function showToast(message, type = "success") {
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `cl-toast ${type}`;
  
  let icon = "✓";
  if (type === "danger") icon = "✕";
  else if (type === "warning") icon = "⚠";
  else if (type === "info") icon = "ℹ";

  toast.innerHTML = `
    <span style="font-size:16px;">${icon}</span>
    <span style="flex:1;">${escapeHtml(message)}</span>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(50px)";
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function formatCurrency(amount) {
  if (amount === undefined || amount === null) return "0 VNĐ";
  return Number(amount).toLocaleString("vi-VN") + " VNĐ";
}

function formatDateTime(dateStr) {
  if (!dateStr) return "-";
  return String(dateStr).replace("T", " ");
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

/* ==========================================================================
   Borrow Cart Manager (localStorage)
   ========================================================================== */
const BorrowCart = {
  getItems() {
    try {
      const data = localStorage.getItem(CART_STORAGE_KEY);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  },

  addItem(book) {
    const items = this.getItems();
    if (items.some(i => i.isbn === book.isbn)) {
      showToast(`Sách "${book.title}" đã có trong giỏ mượn!`, "warning");
      return false;
    }
    if (items.length >= CART_MAX_ITEMS) {
      showToast(`Giỏ mượn đã đạt giới hạn tối đa ${CART_MAX_ITEMS} cuốn (BR-05)!`, "warning");
      return false;
    }
    items.push({
      isbn: book.isbn,
      title: book.title,
      author: book.author || "",
      categoryName: book.categoryName || "",
      shelfLocation: book.shelfLocation || "",
      availableCopies: book.availableCopies || 0,
      totalCopies: book.totalCopies || 0,
      coverUrl: book.coverUrl || "",
      addedAt: new Date().toISOString()
    });
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
    updateCartBadges();
    showToast(`Đã thêm "${book.title}" vào giỏ mượn`, "success");
    return true;
  },

  removeItem(isbn) {
    let items = this.getItems();
    items = items.filter(i => i.isbn !== isbn);
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
    updateCartBadges();
    showToast("Đã xóa sách khỏi giỏ", "info");
  },

  clear() {
    localStorage.removeItem(CART_STORAGE_KEY);
    updateCartBadges();
  },

  count() {
    return this.getItems().length;
  },

  hasItem(isbn) {
    return this.getItems().some(i => i.isbn === isbn);
  }
};

function updateCartBadges() {
  const count = BorrowCart.count();
  const badges = document.querySelectorAll(".cart-badge-count");
  badges.forEach(b => {
    b.textContent = count;
    b.style.display = count > 0 ? "grid" : "none";
  });

  const floatCart = document.getElementById("float-cart-bar");
  if (floatCart) {
    if (count > 0) {
      floatCart.classList.add("show");
      const numElem = document.getElementById("float-cart-num");
      if (numElem) numElem.textContent = count;
    } else {
      floatCart.classList.remove("show");
    }
  }
}

/* ==========================================================================
   Common Layout Rendering (Header, Floating Bar, Footer)
   ========================================================================== */
function renderClientHeader(activeKey) {
  const headerElem = document.querySelector(".cl-header");
  if (!headerElem) return;

  const readerId = getReaderId();
  const readerName = getReaderName();
  const isLoggedIn = Boolean(readerId && getToken());

  const navItems = [
    { key: "home", label: "Trang chủ", href: "index.html" },
    { key: "catalog", label: "Tra cứu & Danh mục", href: "catalog.html" },
    { key: "new", label: "Sách mới", href: "catalog.html?sort=new" },
    { key: "loans", label: "Phiếu mượn", href: "loans.html" },
    { key: "reservations", label: "Đặt trước", href: "reservations.html" },
    { key: "fines", label: "Tiền phạt", href: "fines.html" },
    { key: "rules", label: "Quy định mượn trả", href: "rules.html" },
    { key: "profile", label: "Hồ sơ thẻ", href: "profile.html" }
  ];

  let navHtml = "";
  navItems.forEach(item => {
    const active = item.key === activeKey ? "active" : "";
    navHtml += `<a href="${item.href}" class="${active}">${item.label}</a>`;
  });

  let authSection = "";
  if (isLoggedIn) {
    const initial = (readerName || readerId || "U").charAt(0).toUpperCase();
    authSection = `
      <div class="cl-user-wrapper">
        <button type="button" class="cl-user-chip" id="user-chip-toggle" onclick="toggleUserDropdown(event)">
          <span class="avatar">${initial}</span>
          <span class="meta">
            <strong>${escapeHtml(readerName || readerId)}</strong>
            <span>${escapeHtml(readerId)} · <span class="status-active">Active</span></span>
          </span>
        </button>
        <div class="cl-dropdown-menu" id="user-dropdown-menu">
          <a href="profile.html" class="cl-dropdown-item">👤 Thông tin tài khoản &amp; thẻ</a>
          <a href="loans.html" class="cl-dropdown-item">📖 Phiếu mượn của tôi</a>
          <a href="reservations.html" class="cl-dropdown-item">🔖 Sách đặt trước</a>
          <a href="fines.html" class="cl-dropdown-item">💰 Tiền phạt &amp; Hóa đơn</a>
          <div class="cl-dropdown-divider"></div>
          <button type="button" class="cl-dropdown-item" style="color:var(--cl-danger);" onclick="logout()">🚪 Đăng xuất</button>
        </div>
      </div>
    `;
  } else {
    authSection = `
      <div class="cl-guest-actions">
        <a href="login.html" class="btn btn-secondary btn-sm">Đăng nhập</a>
        <a href="register.html" class="btn btn-primary btn-sm">Đăng ký</a>
      </div>
    `;
  }

  headerElem.innerHTML = `
    <div class="cl-header-inner">
      <a href="index.html" class="cl-brand">
        <span class="cl-brand-mark">📖</span>
        <span class="cl-brand-text">
          <strong>THƯ VIỆN SỐ</strong>
          <span>Đại học Giao thông vận tải</span>
        </span>
      </a>

      <nav class="cl-nav">
        ${navHtml}
      </nav>

      <div class="cl-header-actions">
        <div class="cl-header-search">
          <span class="icon-search">🔍</span>
          <input type="text" id="header-search-input" placeholder="Tìm tài liệu, ISBN..." 
                 onkeydown="if(event.key==='Enter'){ const q = this.value.trim(); window.location.href='catalog.html'+(q?'?q='+encodeURIComponent(q):''); }">
          <span class="shortcut-kbd">⌘K</span>
        </div>

        <a href="borrow-cart.html" class="cl-icon-btn" title="Giỏ mượn tài liệu">
          🛒
          <span class="cl-badge cart-badge-count" style="display:none;">0</span>
        </a>

        <a href="profile.html" class="cl-icon-btn" title="Thông báo">
          🔔
          ${isLoggedIn ? '<span class="cl-badge-dot"></span>' : ''}
        </a>

        ${authSection}
      </div>
    </div>
  `;

  // Close dropdown on outside click
  document.addEventListener("click", (e) => {
    const menu = document.getElementById("user-dropdown-menu");
    const chip = document.getElementById("user-chip-toggle");
    if (menu && chip && !chip.contains(e.target) && !menu.contains(e.target)) {
      menu.classList.remove("show");
    }
  });

  // Global shortcut ⌘K / Ctrl+K
  document.addEventListener("keydown", (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
      e.preventDefault();
      const input = document.getElementById("header-search-input");
      if (input) input.focus();
    }
  });

  updateCartBadges();
}

function toggleUserDropdown(e) {
  e.stopPropagation();
  const menu = document.getElementById("user-dropdown-menu");
  if (menu) menu.classList.toggle("show");
}

function renderFloatingCartBar() {
  if (document.getElementById("float-cart-bar")) return;

  const bar = document.createElement("div");
  bar.id = "float-cart-bar";
  bar.className = "float-cart";
  bar.innerHTML = `
    <div class="float-cart-left">
      <div class="float-cart-icon">🛒</div>
      <div>
        <div class="t">Giỏ mượn: <span id="float-cart-num">0</span> cuốn</div>
        <div class="s">Tối đa 5 cuốn (Quy tắc BR-05 &amp; BR-12)</div>
      </div>
    </div>
    <a href="borrow-cart.html" class="float-cart-btn">
      Xem Giỏ &amp; Hẹn Nhận Sách →
    </a>
  `;
  document.body.appendChild(bar);
  updateCartBadges();
}

function renderClientFooter() {
  const footerElem = document.querySelector(".cl-footer");
  if (!footerElem) return;

  footerElem.innerHTML = `
    <div class="cl-footer-inner">
      <div>
        <div class="cl-brand" style="margin-bottom:14px;">
          <span class="cl-brand-mark">📖</span>
          <span class="cl-brand-text">
            <strong>THƯ VIỆN SỐ</strong>
            <span>Đại học Giao thông vận tải</span>
          </span>
        </div>
        <p style="margin-bottom:12px;">
          Hệ thống thư viện thông minh cung cấp tài nguyên học thuật, cơ sở dữ liệu số và dịch vụ mượn trả
          phục vụ giảng viên, nghiên cứu sinh và toàn thể sinh viên Trường Đại học Giao thông vận tải (UTC).
        </p>
        <span class="badge badge-info" style="font-size:11px;">🛡 Hệ thống chứng nhận ISO 9001:2015</span>
      </div>

      <div>
        <h4>Cơ sở phục vụ &amp; Thời gian</h4>
        <p>📍 <strong>Cơ sở 1:</strong> Số 3 Phố Cầu Giấy, P. Láng Thượng, Q. Đống Đa, Hà Nội</p>
        <p style="margin-top:6px;">📍 <strong>Cơ sở 2:</strong> 450-451 Đường Lê Văn Việt, P. Tăng Nhơn Phú A, TP. Thủ Đức, TP.HCM</p>
        <p style="margin-top:8px;color:var(--cl-navy);font-weight:700;">
          🕒 Giờ mở cửa: 07:30 – 20:30 (Thứ 2 – Thứ 7)
        </p>
      </div>

      <div>
        <h4>Hỗ trợ độc giả</h4>
        <ul>
          <li><a href="rules.html">Quy định &amp; Chính sách mượn trả</a></li>
          <li><a href="catalog.html">Tra cứu danh mục OPAC trực tuyến</a></li>
          <li><a href="loans.html">Gia hạn phiếu mượn online</a></li>
          <li><a href="reservations.html">Đặt trước giáo trình &amp; học liệu</a></li>
          <li><a href="fines.html">Thanh toán nợ phạt quá hạn</a></li>
        </ul>
      </div>

      <div>
        <h4>Liên hệ thủ thư</h4>
        <ul>
          <li>📞 <strong>Hotline:</strong> (024) 3766 3311</li>
          <li>✉️ <strong>Email:</strong> thuvien@utc.edu.vn</li>
          <li>💬 <strong>Trực tuyến:</strong> 08:00 – 17:00 các ngày trong tuần</li>
          <li>🏛️ <a href="../staff-login.html" style="color:#2563eb;font-weight:600;">Cổng dành cho Thủ thư / Quản trị →</a></li>
        </ul>
      </div>
    </div>

    <div class="cl-footer-bottom">
      <div>© 2024 - 2026 Trung tâm Thông tin Thư viện - Đại học Giao thông vận tải. Bảo lưu mọi quyền.</div>
      <div class="links">
        <a href="rules.html">Điều khoản sử dụng</a>
        <a href="rules.html">Chính sách bảo mật</a>
        <a href="catalog.html">Sơ đồ kho sách</a>
      </div>
    </div>
  `;
}

/* ==========================================================================
   Book Detail Modal Popup (03-book-detail.png)
   ========================================================================== */
async function openBookDetailModal(isbn) {
  let modalBackdrop = document.getElementById("book-detail-modal");
  if (!modalBackdrop) {
    modalBackdrop = document.createElement("div");
    modalBackdrop.id = "book-detail-modal";
    modalBackdrop.className = "modal-backdrop";
    modalBackdrop.innerHTML = `
      <div class="modal-dialog">
        <button type="button" class="modal-close-btn" onclick="closeBookDetailModal()">✕</button>
        <div class="modal-body" id="book-detail-modal-body">
          <div style="text-align:center;padding:40px;">Đang tải thông tin sách...</div>
        </div>
      </div>
    `;
    document.body.appendChild(modalBackdrop);

    modalBackdrop.addEventListener("click", (e) => {
      if (e.target === modalBackdrop) closeBookDetailModal();
    });
  }

  const modalBody = document.getElementById("book-detail-modal-body");
  modalBody.innerHTML = `<div style="text-align:center;padding:40px;">Đang tải thông tin chi tiết ấn phẩm...</div>`;
  modalBackdrop.classList.add("show");

  try {
    const books = await fetchAPI("/api/books");
    const book = books.find(b => b.isbn === isbn);
    if (!book) {
      modalBody.innerHTML = `<div style="text-align:center;padding:40px;color:var(--cl-danger);">Không tìm thấy thông tin sách với ISBN: ${escapeHtml(isbn)}</div>`;
      return;
    }

    const [avail, copies] = await Promise.all([
      fetchAPI("/api/books/availability?isbn=" + encodeURIComponent(isbn)).catch(() => ({ availableCount: 0, totalCopies: 0 })),
      fetchAPI("/api/books/copies?isbn=" + encodeURIComponent(isbn)).catch(() => [])
    ]);

    const availableCount = avail.availableCount || 0;
    const totalCopies = avail.totalCopies || copies.length;
    const isAvailable = availableCount > 0;
    const shelf = book.shelfLocation || "Khu A - Tầng 2";
    const ddc = book.classificationCode || "005.1";

    let copiesRows = "";
    if (copies.length === 0) {
      copiesRows = `<tr><td colspan="5" style="text-align:center;color:var(--cl-muted);">Chưa có dữ liệu bản sao vật lý</td></tr>`;
    } else {
      copiesRows = copies.map((c, idx) => {
        const isAvail = (c.status || "").toUpperCase() === "AVAILABLE";
        const statusBadge = isAvail
          ? `<span class="badge badge-success">Sẵn sàng</span>`
          : `<span class="badge badge-warning">${escapeHtml(c.status || "Đang mượn")}</span>`;

        let actionBtn = "-";
        if (isAvail) {
          actionBtn = `<button class="btn btn-primary btn-sm" onclick="BorrowCart.addItem({isbn:'${book.isbn}', title:'${escapeHtml(book.title)}', author:'${escapeHtml(book.author)}', shelfLocation:'${escapeHtml(shelf)}'}); closeBookDetailModal();">Mượn bản này</button>`;
        }
        return `
          <tr>
            <td><strong>${escapeHtml(c.barcode || "BC-" + (idx+1))}</strong></td>
            <td>${escapeHtml(c.shelfLocation || shelf)}</td>
            <td>Tốt (Mới 98%)</td>
            <td>${statusBadge}</td>
            <td>${actionBtn}</td>
          </tr>
        `;
      }).join("");
    }

    modalBody.innerHTML = `
      <div class="cl-breadcrumb" style="margin-bottom:12px;">
        <span>Trang chủ</span> <span class="sep">/</span>
        <span>Kho học liệu OPAC</span> <span class="sep">/</span>
        <span class="current">${escapeHtml(book.title)}</span>
      </div>

      <div class="bd-top">
        <div>
          <div class="bd-cover">
            <span style="font-size:32px;margin-bottom:8px;">📖</span>
            <div style="font-size:14px;font-weight:800;padding:0 8px;">${escapeHtml(book.title)}</div>
            <div style="font-size:12px;opacity:.8;margin-top:4px;">${escapeHtml(book.author || "UTC Press")}</div>
          </div>
          <div style="margin-top:14px;background:#f8fafc;border:1px solid var(--cl-border);border-radius:10px;padding:12px;font-size:12px;color:#475569;line-height:1.5;">
            🛡 <strong>Quy chuẩn mượn (BR-05/BR-09):</strong><br>
            • Tối đa 5 cuốn / lần mượn (14 ngày).<br>
            • Gia hạn trực tuyến tối đa 02 lần nếu không ai đặt trước.
          </div>
        </div>

        <div class="bd-main">
          <div style="display:flex;gap:8px;align-items:center;margin-bottom:6px;">
            <span class="badge badge-default">DDC: ${escapeHtml(ddc)}</span>
            <span class="badge badge-info">ISBN: ${escapeHtml(book.isbn)}</span>
            ${isAvailable ? '<span class="badge badge-success">Có sẵn trên kệ</span>' : '<span class="badge badge-danger">Hết bản sao</span>'}
          </div>

          <h1>${escapeHtml(book.title)}</h1>
          <div class="bd-authors">Tác giả: <strong>${escapeHtml(book.author || "Nhiều tác giả")}</strong> · NXB: <strong>${escapeHtml(book.publisher || "ĐHQG / GTVT")}</strong> (${book.publishYear || "2023"})</div>

          <div class="bd-meta-grid">
            <div>
              <div class="k">Thể loại</div>
              <div class="v">${escapeHtml(book.categoryName || "Chuyên ngành")}</div>
            </div>
            <div>
              <div class="k">Vị trí lưu kho</div>
              <div class="v">${escapeHtml(shelf)}</div>
            </div>
            <div>
              <div class="k">Tồn kho sẵn sàng</div>
              <div class="v" style="color:var(--cl-blue);">${availableCount} / ${totalCopies} bản</div>
            </div>
            <div>
              <div class="k">Thời hạn mượn</div>
              <div class="v">14 Ngày</div>
            </div>
          </div>

          <div class="bd-avail-banner">
            <div>
              <div style="font-weight:700;font-size:14px;">Trạng thái lưu thông trực tiếp: ${isAvailable ? 'Đang có sẵn' : 'Tạm hết'}</div>
              <div style="font-size:12px;opacity:.9;">Hiện có ${availableCount} bản mượn về nhà, phục vụ sinh viên &amp; giảng viên.</div>
            </div>
            <div style="display:flex;gap:8px;">
              ${isAvailable
                ? `<button class="btn btn-secondary btn-sm" onclick="BorrowCart.addItem({isbn:'${book.isbn}', title:'${escapeHtml(book.title)}', author:'${escapeHtml(book.author)}', shelfLocation:'${escapeHtml(shelf)}'}); closeBookDetailModal();">🛒 Thêm vào giỏ</button>`
                : `<button class="btn btn-secondary btn-sm" onclick="reserveBookModal('${book.isbn}')">🔖 Đặt trước</button>`
              }
            </div>
          </div>

          <div style="margin-bottom:18px;">
            <h4 style="font-size:14px;color:var(--cl-navy);margin-bottom:6px;">Tóm tắt &amp; Giới thiệu tài liệu</h4>
            <p style="font-size:13px;color:#475569;line-height:1.6;">
              Tài liệu chuyên khảo được biên soạn phục vụ công tác giảng dạy, học tập và nghiên cứu tại Trường Đại học Giao thông vận tải. Sách cung cấp kiến thức nền tảng và chuyên sâu, đi kèm ví dụ thực tiễn, sơ đồ kiến trúc và bài tập ứng dụng.
            </p>
          </div>

          <div>
            <h4 style="font-size:14px;color:var(--cl-navy);margin-bottom:8px;">Danh mục bản sao vật lý tại quầy (Item Holdings)</h4>
            <div style="border:1px solid var(--cl-border);border-radius:10px;overflow:hidden;">
              <table class="bd-copy-table">
                <thead>
                  <tr>
                    <th>Mã vạch (Barcode)</th>
                    <th>Vị trí lưu kho</th>
                    <th>Tình trạng</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  ${copiesRows}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    `;
  } catch (err) {
    modalBody.innerHTML = `<div style="text-align:center;padding:40px;color:var(--cl-danger);">${escapeHtml(err.message)}</div>`;
  }
}

function closeBookDetailModal() {
  const modal = document.getElementById("book-detail-modal");
  if (modal) modal.classList.remove("show");
}

async function reserveBookModal(isbn) {
  const readerId = getReaderId();
  if (!readerId) {
    showToast("Vui lòng đăng nhập để đặt trước sách!", "warning");
    setTimeout(() => { window.location.href = "login.html"; }, 1200);
    return;
  }
  try {
    await fetchAPI("/api/reservations", {
      method: "POST",
      body: JSON.stringify({ readerId, isbn })
    });
    showToast("Đặt trước thành công! Hệ thống sẽ thông báo khi có sách.", "success");
    closeBookDetailModal();
  } catch (e) {
    /* error handled in fetchAPI */
  }
}

/* ==========================================================================
   Auto Reload for Dev Mode
   ========================================================================== */
(function initAutoReload() {
  let initialVersion = null;
  setInterval(async () => {
    try {
      const res = await fetch("/api/version?_t=" + Date.now());
      if (res.ok) {
        const ver = await res.text();
        if (initialVersion === null) initialVersion = ver;
        else if (initialVersion !== ver) window.location.reload();
      }
    } catch {}
  }, 2500);
})();
