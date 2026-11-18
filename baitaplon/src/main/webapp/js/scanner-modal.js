/**
 * SLMS Barcode & QR Code Scanner Modal
 * Hỗ trợ quét mã trực tiếp bằng Webcam/Camera máy tính và liên kết Camera Điện Thoại từ xa (Remote Scanner)
 */

(function () {
  let targetInputId = null;
  let onScanCallback = null;
  let activeTab = "direct"; // "direct" | "phone"
  let html5Scanner = null;
  let currentSessionId = null;
  let isPolling = false;
  let pollAbortController = null;
  let availableCameras = [];
  let currentCameraIndex = 0;

  // Phát âm thanh beep xác nhận khi quét thành công (Web Audio API)
  function playScanChime() {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(1400, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(1800, ctx.currentTime + 0.08);
      gain.gain.setValueAtTime(0.35, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.12);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.12);
    } catch (e) {}

    if (navigator.vibrate) {
      try { navigator.vibrate([80, 40, 80]); } catch (e) {}
    }
  }

  // Khởi tạo khung HTML Modal nếu chưa có trong trang
  function ensureScannerModalHtml() {
    if (document.getElementById("slms-scanner-modal")) return;

    const modalHtml = `
      <div id="slms-scanner-modal" class="modal-overlay" style="z-index:9999;" onclick="if(event.target===this)window.closeScannerModal()">
        <div class="modal-box" style="max-width:580px;width:95%;padding:20px;max-height:92vh;overflow-y:auto;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;border-bottom:1px solid #e2e8f0;padding-bottom:10px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:22px;">📷</span>
              <h3 style="margin:0;font-size:17px;color:#1e293b;">Quét Mã Vạch / Mã Sách</h3>
            </div>
            <div style="display:flex;align-items:center;gap:10px;">
              <a href="barcode-samples.html" target="_blank" class="btn btn-secondary btn-sm" style="font-size:11px;padding:4px 8px;text-decoration:none;" title="Mở trang danh sách mã vạch mẫu để quét thử">
                📄 Mở bảng mã mẫu ↗
              </a>
              <button type="button" class="btn btn-secondary btn-sm" onclick="window.closeScannerModal()" style="font-size:15px;padding:4px 10px;">✕</button>
            </div>
          </div>

          <!-- Tabs chọn chế độ quét -->
          <div style="display:flex;gap:8px;margin-bottom:16px;background:#f1f5f9;padding:4px;border-radius:8px;">
            <button type="button" id="tab-btn-direct" class="btn btn-sm" onclick="window.switchScannerMode('direct')" style="flex:1;background:#fff;font-weight:700;color:#2563eb;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
              📷 Camera Thiết Bị (Webcam)
            </button>
            <button type="button" id="tab-btn-phone" class="btn btn-sm" onclick="window.switchScannerMode('phone')" style="flex:1;background:transparent;color:#64748b;font-weight:600;">
              📱 Dùng Điện Thoại Quét
            </button>
          </div>

          <!-- TAB 1: Quét trực tiếp bằng Camera máy tính / Webcam -->
          <div id="scanner-pane-direct">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;flex-wrap:wrap;gap:8px;">
              <div style="font-size:12px;color:#64748b;">
                Hướng camera vào mã vạch (Barcode) hoặc mã QR trên sách
              </div>
              <div style="display:flex;gap:6px;align-items:center;">
                <select id="scanner-cam-select" onchange="window.onCameraSelectChange()" style="font-size:12px;padding:4px 8px;border-radius:6px;border:1px solid #cbd5e1;max-width:200px;">
                  <option value="">Đang tìm camera...</option>
                </select>
                <button type="button" class="btn btn-secondary btn-sm" onclick="window.switchDirectCamera()" title="Đổi camera">🔄</button>
              </div>
            </div>

            <div style="position:relative;background:#000;border-radius:10px;overflow:hidden;min-height:260px;display:flex;align-items:center;justify-content:center;">
              <div id="slms-reader-viewport" style="width:100% !important;border:none !important;"></div>
              
              <div id="scanner-laser-overlay" style="position:absolute;top:0;left:0;right:0;bottom:0;pointer-events:none;display:flex;justify-content:center;align-items:center;">
                <div style="width:80%;height:160px;border:2px solid rgba(59,130,246,0.8);border-radius:10px;position:relative;box-shadow:0 0 0 4000px rgba(0,0,0,0.35);">
                  <div style="position:absolute;top:0;left:0;right:0;height:2px;background:#ef4444;box-shadow:0 0 8px #ef4444;animation:slmsLaser 2s infinite ease-in-out;"></div>
                </div>
              </div>
            </div>

            <div style="margin-top:12px;display:flex;justify-content:flex-end;align-items:center;flex-wrap:wrap;gap:8px;">
              <div>
                <label for="scanner-file-picker" class="btn btn-secondary btn-sm" style="cursor:pointer;margin:0;">
                  🖼️ Quét từ ảnh trên máy
                </label>
                <input type="file" id="scanner-file-picker" accept="image/*" style="display:none;" onchange="window.scanBarcodeFromFile(event)">
              </div>
            </div>

            <div id="scanner-direct-msg" style="margin-top:10px;font-size:13px;padding:8px 12px;border-radius:6px;display:none;"></div>
          </div>

          <!-- TAB 2: Quét bằng Camera Điện Thoại (Máy quét không dây) -->
          <div id="scanner-pane-phone" style="display:none;">
            <div style="background:#eff6ff;padding:12px 14px;border-radius:8px;border:1px solid #bfdbfe;margin-bottom:14px;font-size:13px;color:#1e40af;line-height:1.5;">
              💡 <strong>Biến camera điện thoại thành máy quét không dây:</strong><br>
              1. Dùng Zalo hoặc Camera điện thoại quét mã QR bên dưới để mở trang quét.<br>
              2. Trên điện thoại, nhấn nút <strong>"📸 BẤM ĐỂ MỞ CAMERA QUÉT MÃ"</strong> để chụp & quét mã sách tức thì!
            </div>

            <div style="display:flex;gap:18px;align-items:center;flex-wrap:wrap;justify-content:center;padding:10px 0;">
              <div id="slms-phone-qr" style="padding:10px;background:#fff;border-radius:8px;border:1px solid #e2e8f0;display:flex;justify-content:center;"></div>
              
              <div style="flex:1;min-width:240px;">
                <div style="font-size:12px;font-weight:600;color:#64748b;margin-bottom:4px;">Địa chỉ kết nối trên điện thoại:</div>
                <div style="display:flex;gap:6px;margin-bottom:10px;">
                  <input type="text" id="scanner-pairing-url" readonly style="font-family:monospace;font-size:12px;padding:6px 8px;width:100%;border-radius:6px;border:1px solid #cbd5e1;background:#f8fafc;">
                  <button type="button" class="btn btn-secondary btn-sm" onclick="window.copyPairingUrl()">Copy</button>
                </div>

                <div style="font-size:12px;color:#64748b;margin-bottom:6px;">Chọn IP mạng Wi-Fi của máy tính:</div>
                <select id="scanner-ip-select" onchange="window.onIpSelectChange()" style="width:100%;font-size:12px;padding:6px 8px;border-radius:6px;border:1px solid #cbd5e1;margin-bottom:12px;"></select>

                <div id="phone-sync-status" style="font-size:13px;font-weight:600;color:#2563eb;display:flex;align-items:center;gap:6px;">
                  <span class="scanner-pulse-dot"></span> Đang chờ điện thoại quét kết nối...
                </div>
              </div>
            </div>

            <!-- Danh sách mã nhận được từ điện thoại -->
            <div style="margin-top:14px;border-top:1px dashed #cbd5e1;padding-top:12px;">
              <div style="font-size:12px;font-weight:700;color:#334155;margin-bottom:8px;display:flex;justify-content:space-between;">
                <span>Mã nhận được từ điện thoại:</span>
                <span id="phone-received-count" style="color:#2563eb;">0 mã</span>
              </div>
              <div id="phone-received-list" style="max-height:100px;overflow-y:auto;display:flex;flex-direction:column;gap:4px;">
                <div style="font-size:12px;color:#94a3b8;font-style:italic;">Chưa nhận mã nào từ điện thoại trong phiên này</div>
              </div>
            </div>
          </div>

          <!-- Thanh đáy dùng chung cho cả 2 tab: Tuỳ chọn và nút đóng -->
          <div style="margin-top:16px;padding-top:12px;border-top:1px solid #e2e8f0;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
            <label style="display:inline-flex;align-items:center;gap:6px;cursor:pointer;font-size:12px;color:#475569;user-select:none;">
              <input type="checkbox" id="scanner-continuous-cb"> Giữ popup mở để quét liên tục nhiều cuốn
            </label>
            <button type="button" class="btn btn-secondary btn-sm" onclick="window.closeScannerModal()" style="font-size:12px;padding:5px 14px;">
              Đóng (ESC)
            </button>
          </div>
        </div>
      </div>

      <style>
        @keyframes slmsLaser {
          0% { top: 5%; opacity: 0.8; }
          50% { top: 95%; opacity: 1; }
          100% { top: 5%; opacity: 0.8; }
        }
        .scanner-pulse-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #2563eb;
          display: inline-block;
          animation: slmsPulse 1.5s infinite;
        }
        @keyframes slmsPulse {
          0% { transform: scale(0.9); opacity: 0.6; }
          50% { transform: scale(1.3); opacity: 1; box-shadow: 0 0 6px #2563eb; }
          100% { transform: scale(0.9); opacity: 0.6; }
        }
      </style>
    `;

    document.body.insertAdjacentHTML("beforeend", modalHtml);
  }

  // Mở Scanner Modal
  window.openScannerModal = function (inputId, options = {}) {
    targetInputId = inputId;
    onScanCallback = options.onScan || null;

    ensureScannerModalHtml();

    const modal = document.getElementById("slms-scanner-modal");
    if (modal) {
      modal.style.display = "flex";
      modal.classList.add("open");
    }

    // Khởi tạo tab trực tiếp trước
    window.switchScannerMode(activeTab || "direct");
  };

  // Đóng Modal
  window.closeScannerModal = function () {
    const modal = document.getElementById("slms-scanner-modal");
    if (modal) {
      modal.classList.remove("open", "active");
      modal.style.display = "none";
    }

    // Dừng camera trực tiếp nếu đang chạy
    stopDirectScanner();

    // Dừng long polling nếu đang chạy
    stopPhonePolling();
  };

  // Lắng nghe phím ESC để đóng modal
  window.addEventListener("keydown", function (e) {
    if (e.key === "Escape" || e.keyCode === 27) {
      const modal = document.getElementById("slms-scanner-modal");
      if (modal && (modal.classList.contains("open") || modal.style.display === "flex")) {
        window.closeScannerModal();
      }
    }
  });

  // Chuyển chế độ Quét trực tiếp <-> Quét điện thoại
  window.switchScannerMode = function (mode) {
    activeTab = mode;
    const directPane = document.getElementById("scanner-pane-direct");
    const phonePane = document.getElementById("scanner-pane-phone");
    const tabDirect = document.getElementById("tab-btn-direct");
    const tabPhone = document.getElementById("tab-btn-phone");

    if (mode === "direct") {
      if (directPane) directPane.style.display = "block";
      if (phonePane) phonePane.style.display = "none";
      if (tabDirect) {
        tabDirect.style.background = "#fff";
        tabDirect.style.color = "#2563eb";
        tabDirect.style.fontWeight = "700";
        tabDirect.style.boxShadow = "0 1px 3px rgba(0,0,0,0.1)";
      }
      if (tabPhone) {
        tabPhone.style.background = "transparent";
        tabPhone.style.color = "#64748b";
        tabPhone.style.fontWeight = "600";
        tabPhone.style.boxShadow = "none";
      }
      stopPhonePolling();
      startDirectScanner();
    } else {
      if (directPane) directPane.style.display = "none";
      if (phonePane) phonePane.style.display = "block";
      if (tabPhone) {
        tabPhone.style.background = "#fff";
        tabPhone.style.color = "#2563eb";
        tabPhone.style.fontWeight = "700";
        tabPhone.style.boxShadow = "0 1px 3px rgba(0,0,0,0.1)";
      }
      if (tabDirect) {
        tabDirect.style.background = "transparent";
        tabDirect.style.color = "#64748b";
        tabDirect.style.fontWeight = "600";
        tabDirect.style.boxShadow = "none";
      }
      stopDirectScanner();
      startPhoneSync();
    }
  };

  // ==================== XỬ LÝ QUÉT CAMERA TRỰC TIẾP ====================

  async function startDirectScanner() {
    const msg = document.getElementById("scanner-direct-msg");
    if (msg) msg.style.display = "none";

    if (typeof Html5Qrcode === "undefined") {
      if (msg) {
        msg.textContent = "Thư viện camera chưa sẵn sàng. Hãy kiểm tra lại file html5-qrcode.min.js.";
        msg.style.background = "#fee2e2";
        msg.style.color = "#991b1b";
        msg.style.display = "block";
      }
      return;
    }

    try {
      availableCameras = await Html5Qrcode.getCameras();
      const select = document.getElementById("scanner-cam-select");

      if (!availableCameras || availableCameras.length === 0) {
        if (msg) {
          msg.textContent = "Không tìm thấy webcam / camera trên thiết bị này. Hãy dùng tab 'Dùng Điện Thoại Quét' hoặc 'Quét từ ảnh'!";
          msg.style.background = "#fef3c7";
          msg.style.color = "#92400e";
          msg.style.display = "block";
        }
        return;
      }

      if (select) {
        select.innerHTML = availableCameras.map((c, i) => `
          <option value="${c.id}">${c.label || ('Camera ' + (i + 1))}</option>
        `).join("");
      }

      // Ưu tiên camera sau nếu trên điện thoại
      currentCameraIndex = 0;
      availableCameras.forEach((c, idx) => {
        const lbl = (c.label || "").toLowerCase();
        if (lbl.includes("back") || lbl.includes("rear") || lbl.includes("environment")) {
          currentCameraIndex = idx;
        }
      });

      if (select) select.selectedIndex = currentCameraIndex;
      startCameraWithId(availableCameras[currentCameraIndex].id);
    } catch (e) {
      console.warn("Lỗi truy cập camera:", e);
      if (msg) {
        msg.textContent = "Chưa cấp quyền Camera cho trình duyệt hoặc máy không có camera. Hãy cấp quyền hoặc dùng tab 'Dùng Điện Thoại Quét'!";
        msg.style.background = "#fee2e2";
        msg.style.color = "#991b1b";
        msg.style.display = "block";
      }
    }
  }

  let nativeBarcodeDetector = null;
  let nativeDetectAnimationId = null;

  if (typeof window !== "undefined" && "BarcodeDetector" in window) {
    try {
      nativeBarcodeDetector = new BarcodeDetector({
        formats: ["qr_code", "code_128", "code_39", "ean_13", "ean_8", "upc_a", "upc_e"]
      });
    } catch (e) {
      console.log("Native BarcodeDetector không khả dụng, dùng ZXing fallback", e);
    }
  }

  function startNativeDetectionLoop(videoElem) {
    if (!nativeBarcodeDetector || !videoElem) return;
    let isDetecting = false;

    async function tick() {
      if (!html5Scanner || videoElem.paused || videoElem.ended || !videoElem.videoWidth) {
        nativeDetectAnimationId = requestAnimationFrame(tick);
        return;
      }

      if (!isDetecting) {
        isDetecting = true;
        try {
          const detected = await nativeBarcodeDetector.detect(videoElem);
          if (detected && detected.length > 0 && detected[0].rawValue) {
            handleBarcodeScanned(detected[0].rawValue, "direct");
            isDetecting = false;
            return;
          }
        } catch (err) {}
        isDetecting = false;
      }
      nativeDetectAnimationId = requestAnimationFrame(tick);
    }

    tick();
  }

  function startCameraWithId(cameraId) {
    stopDirectScanner();

    html5Scanner = new Html5Qrcode("slms-reader-viewport", { experimentalFeatures: { useBarCodeDetectorIfSupported: true } });
    const config = {
      fps: 25,
      qrbox: (viewfinderWidth, viewfinderHeight) => {
        const minEdge = Math.min(viewfinderWidth, viewfinderHeight);
        return {
          width: Math.floor(Math.min(viewfinderWidth * 0.9, 360)),
          height: Math.floor(Math.min(minEdge * 0.9, 360))
        };
      },
      videoConstraints: {
        focusMode: "continuous",
        width: { min: 640, ideal: 1280, max: 1920 },
        height: { min: 480, ideal: 720, max: 1080 }
      }
    };

    html5Scanner.start(
      cameraId,
      config,
      (decodedText) => {
        handleBarcodeScanned(decodedText, "direct");
      },
      () => {}
    ).then(() => {
      // Chạy vòng lặp phát hiện bằng phần cứng nếu trình duyệt hỗ trợ
      const videoElem = document.querySelector("#slms-reader-viewport video");
      if (videoElem && nativeBarcodeDetector) {
        startNativeDetectionLoop(videoElem);
      }
    }).catch(err => {
      console.warn("Lỗi start html5Scanner:", err);
    });
  }

  function stopDirectScanner() {
    if (nativeDetectAnimationId) {
      cancelAnimationFrame(nativeDetectAnimationId);
      nativeDetectAnimationId = null;
    }
    if (html5Scanner) {
      try {
        if (html5Scanner.isScanning) {
          html5Scanner.stop().catch(() => {}).then(() => {
            try { html5Scanner.clear(); } catch (e) {}
          });
        }
      } catch (e) {}
      html5Scanner = null;
    }
  }

  window.switchDirectCamera = function () {
    if (availableCameras.length <= 1) return;
    currentCameraIndex = (currentCameraIndex + 1) % availableCameras.length;
    const select = document.getElementById("scanner-cam-select");
    if (select) select.selectedIndex = currentCameraIndex;
    startCameraWithId(availableCameras[currentCameraIndex].id);
  };

  window.onCameraSelectChange = function () {
    const select = document.getElementById("scanner-cam-select");
    if (!select || !select.value) return;
    startCameraWithId(select.value);
  };

  window.scanBarcodeFromFile = async function (event) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;

    // Bước 1: Dùng Native BarcodeDetector (giải mã ảnh lớn trong 5ms)
    if (nativeBarcodeDetector) {
      try {
        const detected = await nativeBarcodeDetector.detect(file);
        if (detected && detected.length > 0 && detected[0].rawValue) {
          handleBarcodeScanned(detected[0].rawValue, "file");
          event.target.value = "";
          return;
        }
      } catch (eNative) {}
    }

    if (!html5Scanner) {
      html5Scanner = new Html5Qrcode("slms-reader-viewport", { experimentalFeatures: { useBarCodeDetectorIfSupported: true } });
    }

    // Quét lần 2: Tự động downscale ảnh lớn & bù Quiet Zone
    tryEnhanceFileAndScan(file);
    event.target.value = "";
  };

  function tryEnhanceFileAndScan(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
      const img = new Image();
      img.onload = async function() {
        try {
          // Downscale ảnh lớn (chống quá tải bộ nhớ và tăng tỷ lệ bắt mã)
          const maxDim = 1200;
          const scale = Math.min(1, maxDim / Math.max(img.width, img.height));
          const targetW = Math.round(img.width * scale);
          const targetH = Math.round(img.height * scale);

          const padX = 50;
          const padY = 30;
          const canvas = document.createElement("canvas");
          canvas.width = targetW + padX * 2;
          canvas.height = targetH + padY * 2;
          const ctx = canvas.getContext("2d");
          ctx.fillStyle = "#ffffff";
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, padX, padY, targetW, targetH);

          // Thử Native detector trên canvas
          if (nativeBarcodeDetector) {
            try {
              const bcs = await nativeBarcodeDetector.detect(canvas);
              if (bcs && bcs.length > 0 && bcs[0].rawValue) {
                handleBarcodeScanned(bcs[0].rawValue, "file");
                return;
              }
            } catch (e1) {}
          }

          const blob = await new Promise(res => canvas.toBlob(res, "image/png"));
          if (blob) {
            try {
              const text = await html5Scanner.scanFile(new File([blob], "padded.png", { type: "image/png" }), false);
              if (text) {
                handleBarcodeScanned(text, "file");
                return;
              }
            } catch (e2) {}
          }

          // Lần 3: Tăng tương phản trắng đen
          const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
          const d = imgData.data;
          for (let i = 0; i < d.length; i += 4) {
            const gray = 0.299 * d[i] + 0.587 * d[i+1] + 0.114 * d[i+2];
            const contrast = gray < 135 ? 0 : 255;
            d[i] = contrast; d[i+1] = contrast; d[i+2] = contrast;
          }
          ctx.putImageData(imgData, 0, 0);

          if (nativeBarcodeDetector) {
            try {
              const bcs3 = await nativeBarcodeDetector.detect(canvas);
              if (bcs3 && bcs3.length > 0 && bcs3[0].rawValue) {
                handleBarcodeScanned(bcs3[0].rawValue, "file");
                return;
              }
            } catch (e3) {}
          }

          const blob2 = await new Promise(res => canvas.toBlob(res, "image/png"));
          if (blob2) {
            try {
              const text2 = await html5Scanner.scanFile(new File([blob2], "contrast.png", { type: "image/png" }), false);
              if (text2) {
                handleBarcodeScanned(text2, "file");
                return;
              }
            } catch (e4) {}
          }

          alert("Không nhận diện được mã vạch trong ảnh. Hãy chụp rõ nét hoặc quét mã QR vuông!");
        } catch (err) {
          alert("Không nhận diện được mã vạch trong ảnh. Vui lòng chụp rõ hơn!");
        }
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  // ==================== XỬ LÝ LIÊN KẾT ĐIỆN THOẠI (REMOTE) ====================

  let currentIpList = [];
  let currentServerPort = 8080;

  async function startPhoneSync() {
    stopPhonePolling();

    try {
      const resp = await fetch("/api/scanner/session");
      const res = await resp.json();
      if (!res.success || !res.data) return;

      const data = res.data;
      currentSessionId = data.sessionId;
      currentIpList = data.localIps || [];
      currentServerPort = data.serverPort || 8080;

      // Cập nhật danh sách IP
      const ipSelect = document.getElementById("scanner-ip-select");
      if (ipSelect) {
        // Tự động thêm hostname hiện tại nếu người dùng truy cập bằng IP/Domain
        const currentHost = window.location.hostname;
        const allIps = [currentHost, ...currentIpList.filter(ip => ip !== currentHost)];
        const uniqueIps = [...new Set(allIps)].filter(Boolean);

        ipSelect.innerHTML = uniqueIps.map(ip => `
          <option value="${ip}">${ip} (${ip === "localhost" || ip === "127.0.0.1" ? "Cùng máy tính" : "Mạng LAN / Wi-Fi"})</option>
        `).join("");

        // Ưu tiên chọn IP LAN nếu đang xem từ máy tính
        const lanIp = uniqueIps.find(ip => ip !== "localhost" && ip !== "127.0.0.1");
        if (lanIp && currentHost === "localhost") {
          ipSelect.value = lanIp;
        } else {
          ipSelect.value = currentHost;
        }
      }

      updatePairingUrlAndQr();

      // Bắt đầu vòng lặp long polling chờ nhận mã
      isPolling = true;
      pollAbortController = new AbortController();
      pollLoop();
    } catch (e) {
      console.error("Lỗi tạo phiên quét điện thoại:", e);
    }
  }

  function updatePairingUrlAndQr() {
    const ipSelect = document.getElementById("scanner-ip-select");
    const selectedIp = ipSelect ? ipSelect.value : (window.location.hostname || "localhost");
    const port = window.location.port || currentServerPort || 8080;
    const protocol = window.location.protocol || "http:";

    const pairingUrl = `${protocol}//${selectedIp}:${port}/scanner.html?session=${encodeURIComponent(currentSessionId)}`;

    const urlInput = document.getElementById("scanner-pairing-url");
    if (urlInput) urlInput.value = pairingUrl;

    const qrContainer = document.getElementById("slms-phone-qr");
    if (qrContainer) {
      qrContainer.innerHTML = "";
      if (typeof QRCode !== "undefined") {
        new QRCode(qrContainer, {
          text: pairingUrl,
          width: 170,
          height: 170,
          colorDark: "#1e293b",
          colorLight: "#ffffff",
          correctLevel: QRCode.CorrectLevel.M
        });
      } else {
        qrContainer.innerHTML = `<a href="${pairingUrl}" target="_blank" style="font-size:12px;color:#2563eb;">Mở link quét</a>`;
      }
    }
  }

  window.onIpSelectChange = function () {
    updatePairingUrlAndQr();
  };

  window.copyPairingUrl = function () {
    const input = document.getElementById("scanner-pairing-url");
    if (input) {
      input.select();
      navigator.clipboard.writeText(input.value).then(() => {
        if (typeof showToast === "function") {
          showToast("Đã sao chép liên kết máy quét điện thoại!", "success");
        } else {
          alert("Đã sao chép liên kết!");
        }
      });
    }
  };

  async function pollLoop() {
    while (isPolling && currentSessionId) {
      try {
        const resp = await fetch(`/api/scanner/poll?session=${encodeURIComponent(currentSessionId)}`, {
          signal: pollAbortController ? pollAbortController.signal : undefined
        });

        if (!resp.ok) {
          await new Promise(r => setTimeout(r, 2000));
          continue;
        }

        const res = await resp.json();
        if (res.success && res.data) {
          const d = res.data;
          const statusDiv = document.getElementById("phone-sync-status");

          if (d.status === "connected") {
            if (statusDiv) {
              statusDiv.innerHTML = `<span style="color:#16a34a;">✓</span> Đã kết nối với điện thoại (${d.device || 'Mobile'})! Sẵn sàng nhận mã.`;
              statusDiv.style.color = "#16a34a";
            }
            if (typeof showToast === "function") {
              showToast("📱 Điện thoại đã kết nối máy quét thành công!", "success");
            }
          } else if (d.status === "received" && d.barcode) {
            handleBarcodeScanned(d.barcode, "phone");
          }
        }
      } catch (err) {
        if (!isPolling) break;
        await new Promise(r => setTimeout(r, 2000));
      }
    }
  }

  function stopPhonePolling() {
    isPolling = false;
    if (pollAbortController) {
      try { pollAbortController.abort(); } catch (e) {}
      pollAbortController = null;
    }
  }

  // ==================== XỬ LÝ KHI NHẬN ĐƯỢC MÃ ====================

  let phoneReceivedList = [];

  function handleBarcodeScanned(barcode, source) {
    if (!barcode) return;
    const cleanCode = barcode.trim();

    // Âm thanh bíp & rung
    playScanChime();

    // Điền vào ô input mục tiêu
    if (targetInputId) {
      const input = document.getElementById(targetInputId);
      if (input) {
        input.value = cleanCode;
        // Bắn sự kiện input và change để hệ thống tự động tìm kiếm sách
        input.dispatchEvent(new Event("input", { bubbles: true }));
        input.dispatchEvent(new Event("change", { bubbles: true }));

        // Hiệu ứng viền xanh nhấp nháy trên ô nhập
        input.style.transition = "all 0.3s";
        input.style.borderColor = "#16a34a";
        input.style.boxShadow = "0 0 0 3px rgba(22, 163, 74, 0.35)";
        setTimeout(() => {
          input.style.borderColor = "";
          input.style.boxShadow = "";
        }, 1200);
      }
    }

    // Gọi callback nếu có
    if (typeof onScanCallback === "function") {
      try { onScanCallback(cleanCode); } catch (e) {}
    }

    // Hiển thị toast thông báo
    const srcText = source === "phone" ? "từ điện thoại" : "từ camera";
    if (typeof showToast === "function") {
      showToast(`✓ Đã quét thành công ${srcText}: ${cleanCode}`, "success");
    }

    // Cập nhật danh sách mã nhận được trong modal điện thoại
    if (source === "phone") {
      const now = new Date();
      const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
      phoneReceivedList.unshift({ barcode: cleanCode, time: timeStr });

      const listDiv = document.getElementById("phone-received-list");
      const countSpan = document.getElementById("phone-received-count");
      if (countSpan) countSpan.textContent = `${phoneReceivedList.length} mã`;
      if (listDiv) {
        listDiv.innerHTML = phoneReceivedList.slice(0, 5).map(item => `
          <div style="display:flex;justify-content:space-between;padding:5px 8px;background:#f8fafc;border-radius:4px;font-size:12px;border:1px solid #e2e8f0;">
            <span style="font-family:monospace;font-weight:700;color:#2563eb;">${item.barcode}</span>
            <span style="color:#64748b;">${item.time}</span>
          </div>
        `).join("");
      }
    }

    // Tự động đóng modal sau khi quét thành công (trừ khi người dùng chọn quét liên tục)
    const keepOpen = document.getElementById("scanner-continuous-cb")?.checked;
    if (!keepOpen) {
      setTimeout(() => {
        window.closeScannerModal();
      }, 250);
    }
  }

})();

