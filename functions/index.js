const functions = require('firebase-functions');

exports.googleOAuthCallback = functions.https.onRequest((req, res) => {
  const { code, state, error } = req.query;

  if (error || !code) {
    const q = error ? 'error=' + encodeURIComponent(error) : 'error=access_denied';
    return sendRedirect(res, q);
  }

  const query = 'code=' + encodeURIComponent(code) +
    (state ? '&state=' + encodeURIComponent(state) : '');
  sendRedirect(res, query);
});

function sendRedirect(res, query) {
  const intentUrl = 'intent://callback?' + query +
    '#Intent;scheme=s-oauth;package=tj.safarali.schat;end';
  const html = '<!DOCTYPE html>\n' +
    '<html><head><meta charset="utf-8"></head>\n' +
    '<body><script>location.href="' + intentUrl + '";</script></body>\n' +
    '</html>';
  res.set('Content-Type', 'text/html; charset=utf-8');
  res.status(200).send(html);
}
