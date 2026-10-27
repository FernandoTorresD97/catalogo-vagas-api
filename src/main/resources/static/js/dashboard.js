(() => {
  const PAGE_SIZE = 8;
  const state = { page: 0, totalPages: 0, totalElements: 0, requestId: 0, query: '', contractType: '', workMode: '', location: '', sortBy: 'PUBLISHED_AT', direction: 'DESC' };
  const $ = (selector) => document.querySelector(selector);
  const list = $('#job-list');
  let searchTimer;
  let authUser = null;
  let authMode = 'login';
  let pendingApplicationJobId = null;
  let currentDetailJobId = null;
  let currentApplicationUrl = null;

  const text = (value) => String(value ?? '');
  const escapeHtml = (value) => text(value).replace(/[&<>"']/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
  const currency = (value) => value == null ? '' : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(value);
  const dateLabel = (value) => {
    if (!value) return 'Data não informada';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Data não informada';
    const days = Math.floor((Date.now() - date.getTime()) / 86400000);
    if (days <= 0) return 'Publicada hoje';
    if (days === 1) return 'Publicada ontem';
    if (days < 7) return `Há ${days} dias`;
    return `Publicada em ${new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' }).format(date)}`;
  };
  const labels = {
    modes: { REMOTE: 'Remoto', HYBRID: 'Híbrido', ON_SITE: 'Presencial' },
    contracts: { CLT: 'CLT', PJ: 'Pessoa jurídica', INTERN: 'Estágio', OTHER: 'Outro' }
  };
  const locationLabel = (job) => [job.city, job.state].filter(Boolean).join(', ') || 'Localidade não informada';
  const csrf = async () => {
    const response = await fetch('/api/v1/auth/csrf', { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('Não foi possível preparar a sessão segura.');
    return response.json();
  };
  const sendJson = async (url, body) => {
    const token = await csrf();
    return fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', [token.headerName]: token.token }, body: JSON.stringify(body) });
  };
  function updateAuthUI() {
    $('#auth-open').hidden = Boolean(authUser);
    $('#account-label').hidden = !authUser;
    $('#logout-button').hidden = !authUser;
    $('#account-label').textContent = authUser ? `Olá, ${authUser.fullName.split(' ')[0]}` : '';
  }
  async function loadAuth() {
    try {
      const response = await fetch('/api/v1/auth/me', { headers: { Accept: 'application/json' } });
      if (response.ok) { const result = await response.json(); authUser = result.authenticated ? result : null; }
    } catch (_) { authUser = null; }
    updateAuthUI();
  }
  function openAuth(mode = 'login', pendingJobId = null) {
    authMode = mode;
    pendingApplicationJobId = pendingJobId;
    $('#auth-title').textContent = mode === 'register' ? 'Crie sua conta' : 'Entre na sua conta';
    $('#auth-name-wrap').hidden = mode !== 'register';
    $('#auth-name').required = mode === 'register';
    $('#auth-password').autocomplete = mode === 'register' ? 'new-password' : 'current-password';
    $('#auth-submit').textContent = mode === 'register' ? 'Criar conta' : 'Entrar';
    $('#auth-switch').textContent = mode === 'register' ? 'Já tem conta? Entrar' : 'Ainda não tem conta? Criar uma';
    $('#auth-message').textContent = '';
    $('#auth-backdrop').hidden = false;
    $('#auth-email').focus();
  }
  function closeAuth() { $('#auth-backdrop').hidden = true; }
  async function reportApplication(jobId) {
    if (!authUser) { openAuth('login', jobId); return; }
    const button = document.querySelector('#report-application');
    if (button) button.disabled = true;
    try {
      const response = await sendJson(`/api/v1/jobs/${encodeURIComponent(jobId)}/applications`, {});
      if (!response.ok) throw new Error(response.status === 401 ? 'Sua sessão expirou. Entre novamente.' : 'Não foi possível registrar agora.');
      const stats = await response.json();
      showToast('Candidatura informada. Obrigado por atualizar!');
      renderEngagement(jobId, stats, currentApplicationUrl);
    } catch (error) { showToast(error.message); }
    finally { if (button) button.disabled = false; }
  }
  const salaryLabel = (job) => {
    if (job.salaryMin == null && job.salaryMax == null) return 'Faixa não divulgada';
    if (job.salaryMin != null && job.salaryMax != null) return `${currency(job.salaryMin)} – ${currency(job.salaryMax)}`;
    return job.salaryMin != null ? `A partir de ${currency(job.salaryMin)}` : `Até ${currency(job.salaryMax)}`;
  };

  function setDate() {
    $('#today-label').textContent = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: '2-digit', month: 'long' }).format(new Date()).replace('.', '');
  }

  function currentFilters() {
    state.query = $('#search-input').value.trim();
    state.contractType = $('#contract-filter').value;
    state.workMode = $('#mode-filter').value;
    state.location = $('#location-filter').value.trim();
    const [sortBy, direction] = $('#sort-filter').value.split(':');
    state.sortBy = sortBy;
    state.direction = direction;
  }

  function requestParams() {
    const params = new URLSearchParams({ page: String(state.page), size: String(PAGE_SIZE), sortBy: state.sortBy, direction: state.direction });
    if (state.query) params.set('q', state.query);
    if (state.contractType) params.set('contractType', state.contractType);
    if (state.workMode) params.set('workMode', state.workMode);
    if (state.location) {
      if (/^[a-z]{2}$/i.test(state.location)) params.set('state', state.location.toUpperCase());
      else params.set('city', state.location);
    }
    return params;
  }

  function syncAddress() {
    const params = requestParams();
    params.delete('page');
    params.delete('size');
    if (state.sortBy === 'PUBLISHED_AT' && state.direction === 'DESC') {
      params.delete('sortBy');
      params.delete('direction');
    }
    if (state.page > 0) params.set('page', String(state.page));
    const suffix = params.toString();
    history.replaceState(null, '', suffix ? `/?${suffix}` : '/');
  }

  function renderLoading() {
    $('#empty-state').hidden = true;
    list.hidden = false;
    list.classList.add('is-loading');
    list.innerHTML = '<div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div>';
  }

  function renderCards(jobs) {
    list.classList.remove('is-loading');
    if (!jobs.length) {
      list.hidden = true;
      $('#empty-state').hidden = false;
      return;
    }
    $('#empty-state').hidden = true;
    list.hidden = false;
    list.innerHTML = jobs.map((job) => {
      const salary = salaryLabel(job);
      const mode = labels.modes[job.workMode] || 'Modelo a combinar';
      const contract = labels.contracts[job.contractType] || job.contractType || 'Contrato a combinar';
      return `<article class="job-card" tabindex="0" role="button" data-id="${escapeHtml(job.id)}" aria-label="Ver detalhes da vaga ${escapeHtml(job.title)}">
        <span class="job-accent"></span>
        <div class="job-main">
          <div class="job-topline"><span class="job-id">#${escapeHtml(job.id)}</span><span class="job-dot"></span><span class="job-company">${escapeHtml(job.company)}</span></div>
          <h3 class="job-title">${escapeHtml(job.title)}</h3>
          <p class="job-summary">${escapeHtml(job.summary || 'Oportunidade aberta para novos talentos.')}</p>
          <div class="job-tags"><span class="job-tag">${escapeHtml(contract)}</span><span class="job-tag mode">${escapeHtml(mode)}</span><span class="job-tag salary">${escapeHtml(salary)}</span></div>
        </div>
        <div class="job-side"><span class="job-location"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 10.2c0 5-7 10-7 10s-7-5-7-10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2.2"/></svg>${escapeHtml(locationLabel(job))}</span><span class="job-date">${escapeHtml(dateLabel(job.publishedAt))}</span><span class="job-open">Ver detalhes <span>→</span></span></div>
      </article>`;
    }).join('');
    list.querySelectorAll('.job-card').forEach((card) => {
      card.addEventListener('click', () => openDetails(card.dataset.id));
      card.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); openDetails(card.dataset.id); }
      });
    });
  }

  function renderFilters() {
    const host = $('#active-filters');
    const chips = [];
    if (state.query) chips.push(['q', `Busca: ${state.query}`]);
    if (state.contractType) chips.push(['contractType', labels.contracts[state.contractType] || state.contractType]);
    if (state.workMode) chips.push(['workMode', labels.modes[state.workMode] || state.workMode]);
    if (state.location) chips.push(['location', state.location]);
    host.innerHTML = chips.map(([key, label]) => `<span class="filter-chip">${escapeHtml(label)}<button type="button" data-filter="${key}" aria-label="Remover filtro ${escapeHtml(label)}">×</button></span>`).join('');
    host.querySelectorAll('button').forEach((button) => button.addEventListener('click', () => {
      const target = { q: '#search-input', contractType: '#contract-filter', workMode: '#mode-filter', location: '#location-filter' }[button.dataset.filter];
      if (target) $(target).value = '';
      state.page = 0;
      loadJobs();
    }));
  }

  function renderSummary(page) {
    state.totalElements = Number(page.totalElements || 0);
    state.totalPages = Number(page.totalPages || 0);
    const start = state.totalElements === 0 ? 0 : state.page * PAGE_SIZE + 1;
    const end = Math.min((state.page + 1) * PAGE_SIZE, state.totalElements);
    $('#total-count').textContent = state.totalElements.toLocaleString('pt-BR');
    $('#page-count').textContent = `${state.totalElements} ${state.totalElements === 1 ? 'resultado' : 'resultados'}`;
    $('#results-summary').textContent = state.totalElements ? `Exibindo ${start}–${end} de ${state.totalElements} oportunidades` : 'Nenhuma oportunidade neste recorte';
    $('#page-indicator').textContent = `${Math.min(state.page + 1, Math.max(state.totalPages, 1))} / ${Math.max(state.totalPages, 1)}`;
    $('#previous-page').disabled = state.page <= 0;
    $('#next-page').disabled = state.totalPages === 0 || state.page + 1 >= state.totalPages;
    $('#insight-title').textContent = state.totalElements ? `${state.totalElements} ${state.totalElements === 1 ? 'oportunidade encontrada' : 'oportunidades encontradas'}.` : 'A busca está afinada.';
    $('#insight-copy').textContent = state.totalElements ? 'Cada resultado pode ser um novo começo. Abra uma vaga para conhecer os detalhes.' : 'Nenhum resultado com essa combinação. Experimente ajustar seus filtros.';
  }

  async function loadJobs() {
    currentFilters();
    const requestId = ++state.requestId;
    renderFilters();
    renderLoading();
    syncAddress();
    try {
      const response = await fetch(`/api/v1/jobs?${requestParams()}`, { headers: { Accept: 'application/json' } });
      if (!response.ok) throw new Error(`A API respondeu ${response.status}`);
      const page = await response.json();
      if (requestId !== state.requestId) return;
      renderSummary(page);
      renderCards(page.content || []);
    } catch (error) {
      if (requestId !== state.requestId) return;
      list.classList.remove('is-loading');
      list.hidden = false;
      list.innerHTML = `<div class="error-card"><strong>Não conseguimos carregar as vagas.</strong><span>${escapeHtml(error.message || 'Verifique sua conexão e tente de novo.')}</span><button type="button" id="retry-load">Tentar novamente →</button></div>`;
      $('#retry-load').addEventListener('click', loadJobs);
      $('#page-count').textContent = 'conexão interrompida';
      $('#total-count').textContent = '—';
      $('#insight-title').textContent = 'O catálogo está temporariamente indisponível.';
      $('#insight-copy').textContent = 'Tente novamente em alguns instantes.';
    }
  }

  function openDetails(id) {
    currentDetailJobId = String(id);
    $('#detail-backdrop').hidden = false;
    const panel = $('#detail-panel');
    panel.classList.add('open');
    panel.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
    $('#detail-content').innerHTML = '<p class="detail-kicker">CARREGANDO OPORTUNIDADE</p><div class="skeleton"></div>';
    fetch(`/api/v1/jobs/${encodeURIComponent(id)}`, { headers: { Accept: 'application/json' } })
      .then((response) => { if (!response.ok) throw new Error(response.status === 404 ? 'Esta vaga não está mais disponível.' : 'Não foi possível carregar os detalhes.'); return response.json(); })
      .then(renderDetails)
      .catch((error) => { $('#detail-content').innerHTML = `<p class="detail-kicker">OPA, ALGO MUDOU</p><h2>Não deu para abrir essa vaga.</h2><div class="detail-section"><p>${escapeHtml(error.message)}</p></div>`; });
  }

  async function renderDetails(job) {
    if (String(job.id) !== currentDetailJobId) return;
    currentApplicationUrl = job.applicationUrl || null;
    const contract = labels.contracts[job.contractType] || job.contractType || 'A combinar';
    const mode = labels.modes[job.workMode] || 'A combinar';
    const technologies = Array.isArray(job.technologies) && job.technologies.length
      ? `<div class="technology-list">${job.technologies.map((tech) => `<span>${escapeHtml(tech)}</span>`).join('')}</div>`
      : '<p>Conhecimentos alinhados à descrição da oportunidade.</p>';
    $('#detail-content').innerHTML = `<p class="detail-kicker">OPORTUNIDADE #${escapeHtml(job.id)}</p><h2 id="detail-title">${escapeHtml(job.title)}</h2><p class="detail-company">${escapeHtml(job.company)}</p>
      <div class="detail-tags"><span class="job-tag">${escapeHtml(contract)}</span><span class="job-tag mode">${escapeHtml(mode)}</span><span class="job-tag salary">${escapeHtml(salaryLabel(job))}</span></div>
      <section class="detail-section"><h3>Sobre a oportunidade</h3><p>${escapeHtml(job.description || job.summary || 'Descrição não informada.')}</p></section>
      <section class="detail-section"><h3>Conhecimentos</h3>${technologies}</section>
      <div class="detail-meta"><div><span>LOCALIDADE</span><strong>${escapeHtml(locationLabel(job))}</strong></div><div><span>PUBLICADA</span><strong>${escapeHtml(dateLabel(job.publishedAt))}</strong></div></div>
      <section id="engagement-area" class="engagement-box"><p class="detail-kicker">INTERESSE NESTA VAGA</p><div class="engagement-counts"><div><strong>—</strong><span>cliques em candidatar</span></div><div><strong>—</strong><span>candidaturas informadas</span></div></div><p class="apply-unavailable">Carregando informações…</p></section>`;
    fetch(`/api/v1/jobs/${encodeURIComponent(job.id)}/engagement`, { headers: { Accept: 'application/json' } })
      .then((response) => response.ok ? response.json() : Promise.reject(new Error('Não foi possível carregar as informações.')))
      .then((stats) => { if (String(job.id) === currentDetailJobId) renderEngagement(job.id, stats, job.applicationUrl); })
      .catch(() => {});
  }

  function renderEngagement(jobId, stats, applicationUrl) {
    const area = $('#engagement-area');
    if (!area) return;
    const clicks = Number(stats.applyClicks || 0);
    const reported = Number(stats.selfReportedApplications || 0);
    area.innerHTML = `<p class="detail-kicker">INTERESSE NESTA VAGA</p><div class="engagement-counts"><div><strong>${clicks.toLocaleString('pt-BR')}</strong><span>cliques em candidatar</span></div><div><strong>${reported.toLocaleString('pt-BR')}</strong><span>candidaturas informadas</span></div></div>${applicationUrl ? '' : '<p class="apply-unavailable">O link de candidatura ainda não foi informado.</p>'}<div class="apply-actions">${applicationUrl ? `<a class="apply-link" href="/api/v1/jobs/${encodeURIComponent(jobId)}/apply" target="_blank" rel="noopener noreferrer">Ir para candidatura ↗</a>` : ''}<button id="report-application" class="report-button" type="button" ${stats.reportedByCurrentUser ? 'disabled' : ''}>${stats.reportedByCurrentUser ? 'Você já informou' : 'Já me candidatei'}</button></div><p class="apply-unavailable">As candidaturas são informadas pelos próprios candidatos.</p>`;
    $('#report-application').addEventListener('click', () => reportApplication(jobId));
  }

  function showToast(message) {
    const toast = $('#toast');
    toast.textContent = message;
    toast.classList.add('show');
    window.setTimeout(() => toast.classList.remove('show'), 2800);
  }

  function closeDetails() {
    const panel = $('#detail-panel');
    panel.classList.remove('open');
    panel.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
    window.setTimeout(() => { $('#detail-backdrop').hidden = true; }, 260);
  }

  function clearFilters() {
    $('#search-input').value = '';
    $('#contract-filter').value = '';
    $('#mode-filter').value = '';
    $('#location-filter').value = '';
    $('#sort-filter').value = 'PUBLISHED_AT:DESC';
    state.page = 0;
    loadJobs();
  }

  $('#search-input').addEventListener('input', () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => { state.page = 0; loadJobs(); }, 280);
  });
  $('#search-input').addEventListener('keydown', (event) => { if (event.key === 'Enter') { window.clearTimeout(searchTimer); state.page = 0; loadJobs(); } });
  ['#contract-filter', '#mode-filter', '#sort-filter'].forEach((selector) => $(selector).addEventListener('change', () => { state.page = 0; loadJobs(); }));
  $('#location-filter').addEventListener('input', () => { window.clearTimeout(searchTimer); searchTimer = window.setTimeout(() => { state.page = 0; loadJobs(); }, 350); });
  $('#location-filter').addEventListener('keydown', (event) => { if (event.key === 'Enter') { window.clearTimeout(searchTimer); state.page = 0; loadJobs(); } });
  $('#clear-filters').addEventListener('click', clearFilters);
  $('#empty-clear').addEventListener('click', clearFilters);
  $('#previous-page').addEventListener('click', () => { if (state.page > 0) { state.page -= 1; loadJobs(); window.scrollTo({ top: 0, behavior: 'smooth' }); } });
  $('#next-page').addEventListener('click', () => { if (state.page + 1 < state.totalPages) { state.page += 1; loadJobs(); window.scrollTo({ top: 0, behavior: 'smooth' }); } });
  $('#detail-close').addEventListener('click', closeDetails);
  $('#detail-backdrop').addEventListener('click', closeDetails);
  $('#auth-open').addEventListener('click', () => openAuth());
  $('#auth-close').addEventListener('click', closeAuth);
  $('#auth-backdrop').addEventListener('click', (event) => { if (event.target === $('#auth-backdrop')) closeAuth(); });
  $('#auth-switch').addEventListener('click', () => openAuth(authMode === 'login' ? 'register' : 'login', pendingApplicationJobId));
  $('#auth-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const message = $('#auth-message');
    const credentials = { email: $('#auth-email').value.trim(), password: $('#auth-password').value };
    try {
      if (authMode === 'register') {
        const registered = await sendJson('/api/v1/auth/register', { ...credentials, fullName: $('#auth-name').value.trim() });
        if (!registered.ok) throw new Error(registered.status === 409 ? 'Este e-mail já possui cadastro.' : 'Confira os dados e tente novamente.');
        authMode = 'login';
        $('#auth-title').textContent = 'Conta criada';
        $('#auth-name-wrap').hidden = true;
        $('#auth-submit').textContent = 'Entrar';
        $('#auth-switch').textContent = 'Ainda não tem conta? Criar uma';
        message.style.color = '#247b62';
        message.textContent = 'Cadastro concluído. Entre com seu e-mail e senha.';
        $('#auth-password').value = '';
        return;
      }
      const response = await sendJson('/api/v1/auth/login', credentials);
      if (!response.ok) throw new Error(response.status === 401 ? 'E-mail ou senha inválidos.' : 'Não foi possível entrar agora.');
      authUser = await response.json();
      updateAuthUI();
      const pendingId = pendingApplicationJobId;
      pendingApplicationJobId = null;
      closeAuth();
      if (pendingId) await reportApplication(pendingId);
      else if (document.querySelector('#engagement-area')) {
        const id = document.querySelector('.detail-kicker').textContent.match(/#(\d+)/)?.[1];
        if (id) fetch(`/api/v1/jobs/${id}/engagement`).then((r) => r.json()).then((stats) => renderEngagement(id, stats, currentApplicationUrl));
      }
    } catch (error) { message.style.color = ''; message.textContent = error.message; }
  });
  $('#logout-button').addEventListener('click', async () => {
    const token = await csrf();
    const response = await fetch('/api/v1/auth/logout', { method: 'POST', headers: { [token.headerName]: token.token } });
    if (response.ok) { authUser = null; updateAuthUI(); showToast('Você saiu da sua conta.'); }
  });
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeDetails(); });
  window.addEventListener('popstate', () => {
    const params = new URLSearchParams(location.search);
    $('#search-input').value = params.get('q') || '';
    $('#contract-filter').value = params.get('contractType') || '';
    $('#mode-filter').value = params.get('workMode') || '';
    $('#location-filter').value = params.get('state') || params.get('city') || '';
    const sort = params.get('sortBy');
    const direction = params.get('direction');
    const sortValue = `${sort}:${direction}`;
    if (sort && direction && Array.from($('#sort-filter').options).some((option) => option.value === sortValue)) $('#sort-filter').value = sortValue;
    state.page = Number(params.get('page') || 0);
    loadJobs();
  });

  setDate();
  loadAuth();
  const initial = new URLSearchParams(location.search);
  $('#search-input').value = initial.get('q') || '';
  $('#contract-filter').value = initial.get('contractType') || '';
  $('#mode-filter').value = initial.get('workMode') || '';
  $('#location-filter').value = initial.get('state') || initial.get('city') || '';
  const initialSort = `${initial.get('sortBy')}:${initial.get('direction')}`;
  if (Array.from($('#sort-filter').options).some((option) => option.value === initialSort)) $('#sort-filter').value = initialSort;
  state.page = Number(initial.get('page') || 0);
  loadJobs();
})();
