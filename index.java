// script.js — улучшенная логика: каталог, создание, хранение в localStorage,
// смена статуса (цикл New -> Processing -> Done) и удаление заказа.
// Защита от битых данных в localStorage и аккуратная генерация ID.

const servicesList = [
  {id: 's1', title: 'Дизайн логотипа', desc: 'Базовый логотип, 2 варианта', price: '1500₴'},
  {id: 's2', title: 'Верстка сайта', desc: 'Адаптивная верстка до 5 страниц', price: '4000₴'},
  {id: 's3', title: 'SEO-оптимизация', desc: 'Аудит + рекомендации', price: '2000₴'}
];

const STATUS = [
  {name: 'Новый', cls: 'new'},
  {name: 'В обработке', cls: 'processing'},
  {name: 'Выполнен', cls: 'done'}
];

const $ = sel => document.querySelector(sel);
const $$ = sel => Array.from(document.querySelectorAll(sel));

function safeParse(raw) {
  try {
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    console.warn('Ошибка парсинга localStorage:', e);
    return null;
  }
}

function loadOrders() {
  const raw = localStorage.getItem('orders_demo');
  const parsed = safeParse(raw);
  if (!Array.isArray(parsed)) return [];
  // Валидация простая: обеспечим поля
  return parsed.map(o => ({
    id: o.id || ('ORD' + Date.now().toString().slice(-6)),
    name: o.name || 'Не указано',
    email: o.email || '',
    phone: o.phone || '',
    serviceId: o.serviceId || '',
    serviceTitle: o.serviceTitle || '',
    notes: o.notes || '',
    status: o.status || STATUS[0].name,
    statusClass: o.statusClass || STATUS[0].cls,
    created: o.created || Date.now()
  }));
}

function saveOrders(orders) {
  localStorage.setItem('orders_demo', JSON.stringify(orders));
}

function genId() {
  // Надёжнее ID: ORD + timestamp + случайные 3 цифры (короче коллизий)
  return 'ORD' + Date.now().toString(36).slice(-6) + Math.floor(Math.random()*1000).toString().padStart(3,'0');
}

function renderServices() {
  const sContainer = $('#services');
  const select = $('#service');
  if (!sContainer || !select) return;
  sContainer.innerHTML = '';
  select.innerHTML = '<option value="">Выберите услугу</option>';
  servicesList.forEach(s => {
    const card = document.createElement('div');
    card.className = 'card';
    card.innerHTML = `<h3>${escapeHtml(s.title)}</h3><p>${escapeHtml(s.desc)}</p><div class="price">${escapeHtml(s.price)}</div>`;
    sContainer.appendChild(card);

    const opt = document.createElement('option');
    opt.value = s.id;
    opt.textContent = `${s.title} — ${s.price}`;
    select.appendChild(opt);
  });
}

function escapeHtml(str) {
  if (!str && str !== 0) return '';
  return String(str)
    .replaceAll('&','&amp;')
    .replaceAll('<','&lt;')
    .replaceAll('>','&gt;')
    .replaceAll('"','&quot;')
    .replaceAll("'",'&#039;');
}

function renderOrders() {
  const orders = loadOrders();
  const container = $('#orders');
  if (!container) return;
  if (orders.length === 0) {
    container.innerHTML = '<p class="muted">Заказов пока нет.</p>';
    return;
  }
  container.innerHTML = '';
  // показываем самые новые сверху
  orders.slice().reverse().forEach(o => {
    const el = document.createElement('div');
    el.className = 'order-item';
    el.dataset.id = o.id;
    // кнопки: смена статуса (dropdown-like) и удаление
    el.innerHTML = `
      <div>
        <div style="font-weight:700">${escapeHtml(o.name)} — ${escapeHtml(o.serviceTitle)}</div>
        <div style="color:#6b7280;font-size:13px">${escapeHtml(o.email)} • ${new Date(o.created).toLocaleString()}</div>
        ${o.notes ? `<div style="margin-top:6px;color:#374151;font-size:13px">Комментарий: ${escapeHtml(o.notes)}</div>` : ''}
      </div>
      <div style="text-align:right;min-width:140px">
        <div style="display:flex;flex-direction:column;align-items:flex-end;gap:6px">
          <button class="btn-status" data-action="toggle-status" title="Поменять статус">${escapeHtml(o.status)}</button>
          <div style="display:flex;gap:6px;align-items:center">
            <small style="color:#6b7280">ID: ${escapeHtml(o.id)}</small>
            <button class="btn-delete" data-action="delete" title="Удалить заказ">Удалить</button>
          </div>
        </div>
      </div>
    `;
    // присвоим класс статуса к элементу статуса (для стилизации через .statusX, если нужно)
    const btnStatus = el.querySelector('.btn-status');
    if (btnStatus) btnStatus.classList.add(o.statusClass || STATUS[0].cls);

    container.appendChild(el);
  });
}

// найти индекс следующего статуса
function nextStatusIndex(currentName) {
  const idx = STATUS.findIndex(s => s.name === currentName);
  return idx === -1 ? 0 : (idx + 1) % STATUS.length;
}

function toggleOrderStatus(orderId) {
  const orders = loadOrders();
  const idx = orders.findIndex(o => o.id === orderId);
  if (idx === -1) return;
  const cur = orders[idx];
  const nextIdx = nextStatusIndex(cur.status);
  cur.status = STATUS[nextIdx].name;
  cur.statusClass = STATUS[nextIdx].cls;
  saveOrders(orders);
  renderOrders();
}

function deleteOrder(orderId) {
  if (!confirm('Удалить заказ? Это действие нельзя отменить.')) return;
  let orders = loadOrders();
  orders = orders.filter(o => o.id !== orderId);
  saveOrders(orders);
  renderOrders();
}

function initForm() {
  const form = $('#orderForm');
  const clear = $('#clearBtn');
  if (!form) return;

  form.addEventListener('submit', e => {
    e.preventDefault();
    const name = ($('#name')?.value || '').trim();
    const email = ($('#email')?.value || '').trim();
    const phone = ($('#phone')?.value || '').trim();
    const serviceId = $('#service')?.value || '';
    const notes = ($('#notes')?.value || '').trim();

    if (!name || !email || !serviceId) {
      alert('Пожалуйста, заполните обязательные поля: имя, email, услуга.');
      return;
    }

    const svc = servicesList.find(s => s.id === serviceId);
    const orders = loadOrders();
    const order = {
      id: genId(),
      name,
      email,
      phone,
      serviceId,
      serviceTitle: svc ? svc.title : 'Не указано',
      notes,
      status: STATUS[0].name,
      statusClass: STATUS[0].cls,
      created: Date.now()
    };
    orders.push(order);
    saveOrders(orders);
    renderOrders();
    form.reset();
    // Нежный ненавязчивый alert
    setTimeout(() => alert('Заказ создан! В демо он сохраняется в вашем браузере (localStorage).'), 50);
  });

  if (clear) {
    clear.addEventListener('click', () => {
      if (confirm('Очистить форму?')) form.reset();
    });
  }
}

// Делегирование кликов для кнопок в списке заказов
function initOrdersActions() {
  const container = $('#orders');
  if (!container) return;
  container.addEventListener('click', (e) => {
    const action = e.target.dataset?.action;
    if (!action) return;
    const parent = e.target.closest('.order-item');
    if (!parent) return;
    const id = parent.dataset.id;
    if (!id) return;

    if (action === 'toggle-status') {
      toggleOrderStatus(id);
    } else if (action === 'delete') {
      deleteOrder(id);
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  renderServices();
  initForm();
  renderOrders();
  initOrdersActions();
});
function copyNumber() {
  navigator.clipboard.writeText("5375**** ****1234");
  alert("Номер скопійовано");
}
