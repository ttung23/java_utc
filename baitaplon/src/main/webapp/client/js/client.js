/**
 * SLMS Client Portal - Auth token, API, sidebar
 */

const API_BASE = "";
const TOKEN_KEY = "slms_token";
const SESSION_KEY = "slms_reader_id";
const SESSION_NAME_KEY = "slms_reader_name";
const ROLE_KEY = "slms_role";

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

// Đã tắt toàn bộ alert / toast ở góc dưới màn hình khi thực hiện action theo yêu cầu
function showToast(message, type = "success") {
  // Không hiển thị alert / toast ở góc dưới màn hình
  console.log(`[SLMS Client ${type}]: ${message}`);
}

function formatCurrency(amount) {
  if (amount === undefined || amount === null) return "0 đ";
  return Number(amount).toLocaleString("vi-VN") + " đ";
}

function formatDateTime(dateStr) {
  if (!dateStr) return "-";
  return String(dateStr).replace("T", " ");
}

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

function renderClientSidebar(activeKey) {
  document.body.classList.add("client-body");
  const sidebar = document.querySelector(".sidebar");
  if (!sidebar) return;

  const readerId = getReaderId();
  const readerName = getReaderName();
  const menu = [
    { key: "home", label: "Trang của tôi", icon: "🏠", href: "index.html" },
    { key: "catalog", label: "Tra cứu sách", icon: "📚", href: "catalog.html" },
    { key: "loans", label: "Sách đang mượn", icon: "📖", href: "loans.html" },
    { key: "reservations", label: "Đặt trước", icon: "🔖", href: "reservations.html" },
    { key: "fines", label: "Tiền phạt", icon: "💰", href: "fines.html" },
    { key: "rules", label: "Nội quy & Chính sách", icon: "📜", href: "rules.html" },
    { key: "profile", label: "Hồ sơ & mật khẩu", icon: "👤", href: "profile.html" }
  ];

  let html = `
    <div class="brand">
      <div class="brand-icon">📖</div>
      <div>
        <div class="brand-title">SLMS Reader</div>
        <div class="brand-subtitle">Cổng độc giả</div>
      </div>
    </div>
    <ul class="nav-menu">
  `;
  menu.forEach((item) => {
    const isActive = item.key === activeKey ? "active" : "";
    html += `<li class="nav-item ${isActive}"><a href="${item.href}"><span style="font-size:18px;">${item.icon}</span><span>${item.label}</span></a></li>`;
  });
  html += `
    </ul>
    <div class="sidebar-footer">
      <div style="margin-bottom:8px;">
        <div style="color:#e2e8f0;font-weight:600;">${readerName || "Độc giả"}</div>
        <div style="font-size:11px;color:#94a3b8;">${readerId || ""}</div>
      </div>
      <button type="button" class="btn btn-secondary btn-sm" style="width:100%;margin-bottom:8px;" onclick="logout()">Đăng xuất</button>
      <a href="../staff-login.html" style="color:#94a3b8;font-size:12px;text-decoration:none;">Đăng nhập thủ thư →</a>
    </div>
  `;
  sidebar.innerHTML = html;
}

(function initAutoReload() {
  let initialVersion = null;
  let offline = false;
  setInterval(async () => {
    try {
      const res = await fetch("/api/version?_t=" + Date.now());
      if (res.ok) {
        const ver = await res.text();
        if (initialVersion === null) initialVersion = ver;
        else if (initialVersion !== ver) window.location.reload();
        if (offline) {
          offline = false;
          window.location.reload();
        }
      }
    } catch (e) {
      offline = true;
    }
  }, 1000);
})();

// Đóng modal khi click ra ngoài hoặc bấm Escape
document.addEventListener("click", (e) => {
  if (e.target && e.target.classList && e.target.classList.contains("modal-overlay")) {
    e.target.classList.remove("open");
  }
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    document.querySelectorAll(".modal-overlay.open").forEach(m => m.classList.remove("open"));
  }
});

