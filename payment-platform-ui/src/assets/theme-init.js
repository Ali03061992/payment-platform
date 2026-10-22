(function () {
  try {
    var t = null;
    try { t = localStorage.getItem('pp-theme'); } catch (e) {}
    if (t !== 'dark' && t !== 'light') {
      t = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
    }
    document.documentElement.setAttribute('data-theme', t);
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', t === 'dark' ? '#0f172a' : '#12243A');
    var raw = null;
    try { raw = localStorage.getItem('pp-accents'); } catch (e) {}
    if (raw) {
      var a = JSON.parse(raw);
      var isHex = function (v) { return typeof v === 'string' && /^#[0-9a-fA-F]{6}$/.test(v); };
      if (a && a.length >= 2 && isHex(a[0]) && isHex(a[1])) {
        var c1 = a[0].toLowerCase(), c2 = a[1].toLowerCase();
        var shade = function (hex, amt) {
          var n = hex.substring(1);
          var r = parseInt(n.substring(0, 2), 16), g = parseInt(n.substring(2, 4), 16), b = parseInt(n.substring(4, 6), 16);
          var mx = amt < 0 ? 0 : 255, p = Math.abs(amt) / 100;
          var to = function (v) { return Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0'); };
          return '#' + to((mx - r) * p + r) + to((mx - g) * p + g) + to((mx - b) * p + b);
        };
        var rgba = function (hex, alpha) {
          var n = hex.substring(1);
          return 'rgba(' + parseInt(n.substring(0, 2), 16) + ', ' + parseInt(n.substring(2, 4), 16) + ', ' + parseInt(n.substring(4, 6), 16) + ', ' + alpha + ')';
        };
        var s = document.documentElement.style;
        s.setProperty('--theme-primary', c1);
        s.setProperty('--theme-primary-dark', shade(c1, -18));
        s.setProperty('--theme-primary-soft', rgba(c1, 0.12));
        s.setProperty('--theme-secondary', c2);
        s.setProperty('--theme-secondary-dark', shade(c2, -18));
        s.setProperty('--theme-secondary-soft', rgba(c2, 0.12));
        s.setProperty('--grad-theme', 'linear-gradient(135deg, ' + c1 + ' 0%, ' + c2 + ' 100%)');
      }
    }
  } catch (e) {}
})();
