// app.js
document.addEventListener('DOMContentLoaded', () => {
    // 1. Theme Initialization
    const initTheme = () => {
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            document.documentElement.setAttribute('data-theme', 'dark');
            const themeBtn = document.getElementById('theme-toggle');
            if (themeBtn) themeBtn.innerHTML = '<i class="bi bi-sun"></i>';
        }
    };
    initTheme();

    // Theme Toggle
    const themeToggle = document.getElementById('theme-toggle');
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            themeToggle.innerHTML = newTheme === 'dark' ? '<i class="bi bi-sun"></i>' : '<i class="bi bi-moon"></i>';
        });
    }

    // 2. Sidebar Toggle
    const sidebarToggle = document.getElementById('sidebar-toggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', () => {
            if (window.innerWidth <= 992) {
                document.body.classList.toggle('sidebar-open');
            } else {
                document.body.classList.toggle('sidebar-collapsed');
            }
        });
    }

    // 3. CSRF setup for AJAX
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (window.jQuery && csrfToken && csrfHeader) {
        $.ajaxSetup({
            beforeSend: function(xhr) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        });
    }

    // 4. WebSocket Setup (if SockJS and STOMP are available)
    if (window.SockJS && window.Stomp) {
        let stompClient = null;
        
        const connectWs = () => {
            const socket = new SockJS('/ws');
            stompClient = Stomp.over(socket);
            stompClient.debug = null; // Disable debug logs in prod
            
            stompClient.connect({}, (frame) => {
                console.log('Connected to WS');
                
                // Subscribe to user notifications
                stompClient.subscribe('/user/queue/notifications', (message) => {
                    const notification = JSON.parse(message.body);
                    showToast(notification.title, notification.message, 'info');
                    updateNotificationBadge();
                });
                
                // If on conversation page, subscribe to thread
                const convIdElem = document.getElementById('current-conversation-id');
                if (convIdElem && convIdElem.value) {
                    stompClient.subscribe('/topic/conversations/' + convIdElem.value, (message) => {
                        appendMessageToUI(JSON.parse(message.body));
                    });
                }
            }, (error) => {
                console.error('WS Error:', error);
                setTimeout(connectWs, 5000); // Auto-reconnect
            });
        };
        
        connectWs();
    }

    // 5. Toast System
    window.showToast = (title, message, type = 'info') => {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'position-fixed top-0 end-0 p-3';
            container.style.zIndex = '1055';
            document.body.appendChild(container);
        }

        const iconMap = {
            'success': 'bi-check-circle text-success',
            'error': 'bi-exclamation-circle text-danger',
            'warning': 'bi-exclamation-triangle text-warning',
            'info': 'bi-info-circle text-primary'
        };

        const id = 'toast-' + Date.now();
        const toastHTML = `
            <div id="${id}" class="toast glass-card mb-3 animate-slide-up" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="toast-header border-bottom-0 bg-transparent">
                    <i class="bi ${iconMap[type]} me-2"></i>
                    <strong class="me-auto">${title}</strong>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            </div>
        `;
        
        container.insertAdjacentHTML('beforeend', toastHTML);
        const toastEl = document.getElementById(id);
        const toast = new bootstrap.Toast(toastEl, { delay: 5000 });
        toast.show();
        
        toastEl.addEventListener('hidden.bs.toast', () => {
            toastEl.remove();
        });
    };

    // Show flash messages from server if they exist in DOM
    const flashMessages = document.querySelectorAll('.flash-message');
    flashMessages.forEach(msg => {
        const type = msg.dataset.type || 'info';
        const title = type.charAt(0).toUpperCase() + type.slice(1);
        showToast(title, msg.textContent, type);
    });

    // 6. Delete Confirmation
    const deleteForms = document.querySelectorAll('.form-delete');
    deleteForms.forEach(form => {
        form.addEventListener('submit', (e) => {
            if (!confirm('Are you sure you want to delete this record? This cannot be undone.')) {
                e.preventDefault();
            }
        });
    });
});

// Helper for UI
function updateNotificationBadge() {
    const badge = document.getElementById('notification-badge');
    if (badge) {
        let count = parseInt(badge.textContent || '0');
        badge.textContent = count + 1;
        badge.classList.remove('d-none');
    }
}

function appendMessageToUI(msg) {
    const chatContainer = document.getElementById('chat-messages');
    if (!chatContainer) return;
    
    const isOutbound = msg.senderType === 'USER' || msg.senderType === 'COUNSELOR';
    const alignClass = isOutbound ? 'outbound' : 'inbound';
    
    const html = `
        <div class="message ${alignClass} animate-fade-in">
            <div class="fw-bold small mb-1">${isOutbound ? 'You' : msg.senderName || 'Lead'}</div>
            <div>${msg.content}</div>
            <div class="text-end" style="font-size: 0.65rem; opacity: 0.7; margin-top: 4px;">Just now</div>
        </div>
    `;
    
    chatContainer.insertAdjacentHTML('beforeend', html);
    chatContainer.scrollTop = chatContainer.scrollHeight;
}
