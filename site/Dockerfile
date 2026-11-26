# Site vitrine statique (caixadev.dev), servi derriere Traefik (T29).
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html style.css contact.js /usr/share/nginx/html/
COPY images/ /usr/share/nginx/html/images/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
