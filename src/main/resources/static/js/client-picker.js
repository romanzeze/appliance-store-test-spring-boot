(function () {
    var root = document.getElementById('client-picker');
    if (!root) {
        return;
    }
    var hidden = root.querySelector('input[type=hidden]');
    var query = document.getElementById('client-query');
    var results = document.getElementById('client-results');
    var pager = document.getElementById('client-pager');
    var selected = document.getElementById('client-selected');
    var page = 0;
    var timer = null;

    function load() {
        fetch(root.dataset.url + '?query=' + encodeURIComponent(query.value) + '&page=' + page, {
            headers: {'Accept': 'application/json'},
            credentials: 'same-origin'
        }).then(function (response) {
            return response.json();
        }).then(render);
    }

    function render(data) {
        results.textContent = '';
        pager.textContent = '';
        if (!data.items.length) {
            var empty = document.createElement('div');
            empty.className = 'text-muted small';
            empty.textContent = root.dataset.empty;
            results.appendChild(empty);
            return;
        }
        data.items.forEach(function (client) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'list-group-item list-group-item-action';
            button.textContent = client.name + ' (' + client.email + ')';
            if (String(client.id) === hidden.value) {
                button.classList.add('active');
            }
            button.addEventListener('click', function () {
                hidden.value = client.id;
                selected.textContent = client.name + ' (' + client.email + ')';
                selected.classList.remove('d-none');
                query.classList.remove('is-invalid');
                load();
            });
            results.appendChild(button);
        });
        if (data.totalPages > 1) {
            pager.appendChild(pageButton('‹', data.page > 0 ? data.page - 1 : null));
            var label = document.createElement('span');
            label.className = 'small text-muted';
            label.textContent = (data.page + 1) + ' / ' + data.totalPages;
            pager.appendChild(label);
            pager.appendChild(pageButton('›', data.page + 1 < data.totalPages ? data.page + 1 : null));
        }
    }

    function pageButton(text, target) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-sm btn-outline-secondary';
        button.textContent = text;
        button.disabled = target === null;
        button.addEventListener('click', function () {
            page = target;
            load();
        });
        return button;
    }

    query.addEventListener('input', function () {
        clearTimeout(timer);
        timer = setTimeout(function () {
            page = 0;
            load();
        }, 250);
    });
    query.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') {
            event.preventDefault();
        }
    });
    load();
})();
