// Adresse de contact assemblee cote client (limite la collecte par les robots).
// Fichier separe plutot que script en ligne : la CSP de la vitrine n'autorise
// que les scripts servis par le site (script-src 'self').
(function () {
  var user = "contact";
  var domain = "caixadev.dev";
  var link = document.getElementById("email-link");
  if (!link) return;
  link.href = "mailto:" + user + "@" + domain;
  link.textContent = user + "@" + domain;
})();
