#!/bin/sh
# Sauvegarde et restauration PostgreSQL (T17).
#
# Execute dans le conteneur `backup` (image postgres:16-alpine, memes outils
# clients que le serveur). Connexion par les variables standard libpq :
# PGHOST, PGUSER, PGPASSWORD, PGDATABASE.
#
# Usage :
#   pg-backup.sh loop              sauvegarde immediate, puis toutes les
#                                  BACKUP_INTERVAL_SECONDS (defaut 86400)
#   pg-backup.sh once              une sauvegarde, puis sortie
#   pg-backup.sh list              liste des sauvegardes
#   pg-backup.sh verify <fichier>  restaure dans une base temporaire vierge,
#                                  compte les lignes, puis supprime la base
#   pg-backup.sh restore <fichier> REMPLACE le contenu de $PGDATABASE
#                                  (demande CONFIRM=yes)
#
# Les sauvegardes sont au format custom de pg_dump (-Fc, compresse),
# nommees <base>-<AAAAMMJJ-HHMMSS>Z.dump, conservees BACKUP_RETENTION_DAYS
# jours (defaut 14).

set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
INTERVAL="${BACKUP_INTERVAL_SECONDS:-86400}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGHOST:?PGHOST is required}"
: "${PGUSER:?PGUSER is required}"

# Fichiers lisibles uniquement par root (le conteneur) : une sauvegarde
# contient les hachages de mots de passe et les jetons de refresh.
umask 077

log() { printf '%s [backup] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }

resolve() {
  # Accepte un nom de fichier seul ou un chemin complet.
  case "$1" in
    /*) printf '%s' "$1" ;;
    *)  printf '%s/%s' "$BACKUP_DIR" "$1" ;;
  esac
}

backup_once() {
  mkdir -p "$BACKUP_DIR"
  ts="$(date -u +%Y%m%d-%H%M%S)Z"
  final="$BACKUP_DIR/${PGDATABASE}-${ts}.dump"
  tmp="$final.partial"

  log "debut de la sauvegarde de ${PGDATABASE}"
  pg_dump --format=custom --no-owner --no-privileges --file="$tmp"
  # Une archive illisible ne doit jamais remplacer une sauvegarde valide.
  pg_restore --list "$tmp" > /dev/null
  mv "$tmp" "$final"
  log "sauvegarde OK : $(basename "$final") ($(du -h "$final" | cut -f1))"

  # Rotation : uniquement nos propres fichiers, jamais la derniere sauvegarde.
  find "$BACKUP_DIR" -maxdepth 1 -type f -name "${PGDATABASE}-*.dump" \
       -mtime +"$RETENTION_DAYS" ! -path "$final" -print -delete \
    | while read -r f; do log "supprime (> ${RETENTION_DAYS} j) : $(basename "$f")"; done
  rm -f "$BACKUP_DIR"/*.partial
}

verify() {
  file="$(resolve "$1")"
  [ -s "$file" ] || { log "fichier absent ou vide : $file"; exit 1; }
  testdb="restore_check_$(date -u +%s)"
  log "restauration de $(basename "$file") dans la base temporaire ${testdb}"
  createdb "$testdb"
  trap 'dropdb --if-exists "$testdb" >/dev/null 2>&1 || true' EXIT
  pg_restore --no-owner --no-privileges --exit-on-error --dbname="$testdb" "$file"
  log "tables restaurees (nombre de lignes) :"
  for t in $(psql --dbname="$testdb" -At -c "
      SELECT quote_ident(schemaname) || '.' || quote_ident(relname)
      FROM pg_stat_user_tables ORDER BY 1;"); do
    printf '  %-40s %s\n' "$t" "$(psql --dbname="$testdb" -At -c "SELECT count(*) FROM $t;")"
  done
  log "verification OK : la sauvegarde se restaure sur une base vierge"
}

restore() {
  file="$(resolve "$1")"
  [ -s "$file" ] || { log "fichier absent ou vide : $file"; exit 1; }
  if [ "${CONFIRM:-}" != "yes" ]; then
    log "restore REMPLACE le contenu de ${PGDATABASE}. Relancer avec CONFIRM=yes."
    exit 2
  fi
  log "sauvegarde de securite avant restauration"
  backup_once
  log "restauration de $(basename "$file") dans ${PGDATABASE}"
  pg_restore --no-owner --no-privileges --clean --if-exists --exit-on-error \
             --single-transaction --dbname="$PGDATABASE" "$file"
  log "restauration OK"
}

cmd="${1:-loop}"
case "$cmd" in
  loop)
    log "planification : toutes les ${INTERVAL} s, retention ${RETENTION_DAYS} j"
    while :; do
      backup_once || log "ECHEC de la sauvegarde (nouvel essai au prochain cycle)"
      sleep "$INTERVAL"
    done
    ;;
  once)    backup_once ;;
  list)    ls -lh "$BACKUP_DIR"/"${PGDATABASE}"-*.dump 2>/dev/null || log "aucune sauvegarde" ;;
  verify)  verify "${2:?usage: verify <fichier>}" ;;
  restore) restore "${2:?usage: restore <fichier>}" ;;
  *) echo "usage: $0 {loop|once|list|verify <fichier>|restore <fichier>}" >&2; exit 64 ;;
esac
