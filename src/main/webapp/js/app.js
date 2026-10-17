document.addEventListener('DOMContentLoaded', () => {

  // Show / hide password
  document.querySelectorAll('.eye').forEach(btn => btn.addEventListener('click', () => {
    const input = btn.parentElement.querySelector('input');
    input.type = input.type === 'password' ? 'text' : 'password';
  }));

  // Close flash message
  document.querySelectorAll('.flash button').forEach(btn => {
    btn.onclick = () => btn.parentElement.remove();
  });

  // Drag & drop + image preview
  const drop = document.querySelector('.drop');
  if (drop) {
    const file = drop.querySelector('input[type=file]');
    const preview = document.getElementById('preview');

    const showImage = f => {
      if (!f) return;
      if (!/\.(png|bmp)$/i.test(f.name)) {
        alert('Only PNG or BMP images are supported.');
        file.value = '';
        return;
      }
      const img = preview.querySelector('img');
      img.src = URL.createObjectURL(f);
      preview.classList.remove('empty');
      preview.querySelector('.fname').textContent = f.name + ' (' + (f.size / 1048576).toFixed(1) + ' MB)';

      // Capacity estimate (embed page only)
      const cap = document.getElementById('capacity');
      if (cap) {
        const probe = new Image();
        probe.onload = () => {
          cap.textContent = 'Up to ' + Math.floor(probe.width * probe.height * 3 / 8 / 1024) + ' KB (approx.)';
        };
        probe.src = img.src;
      }
    };

    drop.addEventListener('click', e => { if (e.target.tagName !== 'INPUT') file.click(); });
    file.addEventListener('change', () => showImage(file.files[0]));
    ['dragover', 'dragenter'].forEach(ev => drop.addEventListener(ev, e => {
      e.preventDefault();
      drop.classList.add('over');
    }));
    ['dragleave', 'drop'].forEach(ev => drop.addEventListener(ev, e => {
      e.preventDefault();
      drop.classList.remove('over');
    }));
    drop.addEventListener('drop', e => {
      file.files = e.dataTransfer.files;
      showImage(file.files[0]);
    });
  }

  // Message character counter (embed page)
  const msg = document.getElementById('message');
  const count = document.getElementById('count');
  if (msg && count) {
    msg.addEventListener('input', () => {
      count.textContent = msg.value.length + '/' + msg.maxLength;
    });
  }

  // Client-side validation
  document.querySelectorAll('form[data-validate]').forEach(form => {
    form.addEventListener('submit', e => {
      const pw = form.querySelector('[name=password]');
      const cpw = form.querySelector('[name=confirmPassword]');
      const fileInput = form.querySelector('input[type=file]');
      let error = '';

      if (fileInput && !fileInput.files.length) error = 'Please select an image.';
      else if (pw && pw.value.length < 6) error = 'Password must be at least 6 characters.';
      else if (cpw && pw.value !== cpw.value) error = 'Passwords do not match.';

      if (error) {
        e.preventDefault();
        alert(error);
      }
    });
  });

  // Reset button
  document.querySelectorAll('[data-reset]').forEach(btn => btn.addEventListener('click', () => {
    const form = btn.closest('form');
    form.reset();
    const preview = document.getElementById('preview');
    if (preview) preview.classList.add('empty');
    if (count) count.textContent = '0/' + (msg ? msg.maxLength : 0);
  }));

});