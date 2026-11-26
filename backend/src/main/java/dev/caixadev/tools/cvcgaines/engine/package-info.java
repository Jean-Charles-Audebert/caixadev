/**
 * Moteur de calcul aeraulique de l'outil {@code cvc-gaines} (ADR-0010).
 *
 * <p>Regles du package :
 * <ul>
 *   <li><b>Java pur</b> : aucune dependance hors {@code java.*} (ni Spring, ni
 *       Jakarta, ni Lombok, ni JPA). Verifie par {@code EnginePurityTest}. Le
 *       moteur reste ainsi extractible en bibliotheque ou en service.</li>
 *   <li><b>Unites SI</b> partout : m, m2, m3/s, Pa, kg/m3, Pa.s, m2/s, degres
 *       Celsius pour les temperatures saisies. Les conversions (m3/h, mm)
 *       se font a la frontiere ({@link Units}).</li>
 *   <li><b>Objets immuables</b> ({@code record}) ; une entree invalide leve
 *       {@link IllegalArgumentException} avec un message explicite.</li>
 *   <li><b>Valeurs sourcees</b> : chaque coefficient ou table cite sa
 *       reference dans le code.</li>
 * </ul>
 */
package dev.caixadev.tools.cvcgaines.engine;
