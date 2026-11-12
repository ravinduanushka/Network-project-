// ==========================================================================
// FraudShield Interactive Scripts
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  // 1. Language Dropdown Toggle
  const langBtn = document.getElementById('langBtn');
  const langDropdown = document.getElementById('langDropdown');
  const currentLang = document.getElementById('currentLang');
  const langItems = document.querySelectorAll('.lang-item');

  if (langBtn && langDropdown) {
    langBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isExpanded = langBtn.getAttribute('aria-expanded') === 'true';
      langBtn.setAttribute('aria-expanded', !isExpanded);
      langDropdown.classList.toggle('show');
    });

    langItems.forEach(item => {
      item.addEventListener('click', (e) => {
        e.stopPropagation();
        langItems.forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        currentLang.textContent = item.getAttribute('data-lang');
        langBtn.setAttribute('aria-expanded', 'false');
        langDropdown.classList.remove('show');
      });
    });

    document.addEventListener('click', () => {
      langBtn.setAttribute('aria-expanded', 'false');
      langDropdown.classList.remove('show');
    });
  }

  // 2. Modal Handlers
  const actionModal = document.getElementById('actionModal');
  const signUpBtn = document.getElementById('signUpBtn');
  const getStartedBtn = document.getElementById('getStartedBtn');
  const closeModalBtn = document.getElementById('closeModalBtn');
  const modalCloseAction = document.getElementById('modalCloseAction');
  const modalTitle = document.getElementById('modalTitle');

  function openModal(title) {
    if (actionModal) {
      if (modalTitle) modalTitle.textContent = title;
      actionModal.classList.add('show');
      document.body.style.overflow = 'hidden';
    }
  }

  function closeModal() {
    if (actionModal) {
      actionModal.classList.remove('show');
      document.body.style.overflow = '';
    }
  }

  if (signUpBtn) {
    signUpBtn.addEventListener('click', () => {
      window.location.href = 'login.html';
    });
  }

  if (getStartedBtn) {
    getStartedBtn.addEventListener('click', () => {
      window.location.href = 'login.html';
    });
  }

  if (closeModalBtn) closeModalBtn.addEventListener('click', closeModal);
  if (modalCloseAction) modalCloseAction.addEventListener('click', closeModal);

  if (actionModal) {
    actionModal.addEventListener('click', (e) => {
      if (e.target === actionModal) closeModal();
    });
  }

  // 3. Interactive Fraud Alert Trigger & Animation
  const fraudToast = document.getElementById('fraudBlockedToast');
  const simulateAlertBtn = document.getElementById('simulateAlertBtn');

  function triggerFraudAlertAnimation() {
    if (!fraudToast) return;
    
    // Quick pulse animation
    fraudToast.style.transition = 'all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
    fraudToast.style.transform = 'scale(1.15) translateY(-8px)';
    fraudToast.style.boxShadow = '0 30px 60px rgba(220, 38, 38, 0.45)';
    
    setTimeout(() => {
      fraudToast.style.transform = 'scale(1) translateY(0)';
      fraudToast.style.boxShadow = '';
    }, 600);
  }

  if (simulateAlertBtn) {
    simulateAlertBtn.addEventListener('click', (e) => {
      e.preventDefault();
      closeModal();
      setTimeout(triggerFraudAlertAnimation, 300);
    });
  }

  if (fraudToast) {
    fraudToast.addEventListener('click', () => {
      triggerFraudAlertAnimation();
    });
  }

  // 4. Parallax micro-interaction on mouse move
  const stage = document.querySelector('.visual-stage');
  const glassCard = document.getElementById('glassCard');
  const phoneMockup = document.getElementById('phoneMockup');

  if (stage && glassCard && phoneMockup && window.innerWidth > 960) {
    stage.addEventListener('mousemove', (e) => {
      const rect = stage.getBoundingClientRect();
      const x = (e.clientX - rect.left) / rect.width - 0.5;
      const y = (e.clientY - rect.top) / rect.height - 0.5;

      glassCard.style.transform = `rotate(${ -10 + x * 10 }deg) skewX(2deg) translateY(${ y * -15 }px)`;
      phoneMockup.style.transform = `perspective(1000px) rotateY(${ x * 8 }deg) rotateX(${ -y * 8 }deg)`;
    });

    stage.addEventListener('mouseleave', () => {
      glassCard.style.transform = 'rotate(-10deg) skewX(2deg) translateY(0px)';
      phoneMockup.style.transform = 'perspective(1000px) rotateY(0deg) rotateX(0deg)';
    });
  }

  // 5. App Interactive Tab Clicks inside phone
  const navTabs = document.querySelectorAll('.nav-tab');
  navTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      navTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
    });
  });

  const quickBtns = document.querySelectorAll('.quick-act-btn');
  quickBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const label = btn.querySelector('.act-label')?.textContent;
      if (label === 'Shield') {
        triggerFraudAlertAnimation();
      } else {
        openModal(`Banking Action: ${label}`);
      }
    });
  });
});
