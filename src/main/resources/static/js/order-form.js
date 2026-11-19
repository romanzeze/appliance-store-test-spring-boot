(function () {
    var body = document.getElementById('order-rows');
    var template = document.getElementById('order-row-template');
    if (!body || !template) {
        return;
    }
    function reindex() {
        Array.prototype.forEach.call(body.querySelectorAll('tr'), function (row, index) {
            Array.prototype.forEach.call(row.querySelectorAll('[data-field]'), function (input) {
                input.name = 'rows[' + index + '].' + input.getAttribute('data-field');
            });
        });
    }
    document.getElementById('add-row').addEventListener('click', function () {
        body.appendChild(template.content.cloneNode(true));
        reindex();
    });
    body.addEventListener('click', function (event) {
        var button = event.target.closest('.remove-row');
        if (button && body.querySelectorAll('tr').length > 1) {
            button.closest('tr').remove();
            reindex();
        }
    });
    document.getElementById('order-form').addEventListener('submit', reindex);
    reindex();
})();
