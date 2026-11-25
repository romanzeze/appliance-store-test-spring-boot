document.addEventListener('submit', function (event) {
    var message = event.target.getAttribute('data-confirm');
    if (message && !window.confirm(message)) {
        event.preventDefault();
    }
});

document.addEventListener('click', function (event) {
    var button = event.target.closest('[data-toggle-password]');
    if (!button) {
        return;
    }
    var input = button.parentElement.querySelector('input');
    var visible = input.type === 'text';
    input.type = visible ? 'password' : 'text';
    button.textContent = visible ? button.getAttribute('data-show') : button.getAttribute('data-hide');
});

document.addEventListener('change', function (event) {
    var select = event.target.closest('select[data-auto-submit]');
    if (select && select.form) {
        select.form.submit();
    }
});

document.addEventListener('input', function (event) {
    var input = event.target.closest('input[data-card-mask]');
    if (!input) {
        return;
    }
    var digits = input.value.replace(/\D/g, '').slice(0, 19);
    input.value = digits.replace(/(.{4})/g, '$1 ').trim();
});

document.querySelectorAll('input[data-card-mask]').forEach(function (input) {
    var digits = input.value.replace(/\D/g, '').slice(0, 19);
    input.value = digits.replace(/(.{4})/g, '$1 ').trim();
});
