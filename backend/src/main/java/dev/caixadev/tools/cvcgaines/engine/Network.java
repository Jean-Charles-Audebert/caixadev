package dev.caixadev.tools.cvcgaines.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reseau arborescent de gaines (soufflage ou reprise) raccorde a un
 * ventilateur.
 *
 * <ul>
 *   <li>Un seul troncon racine (sans parent) : celui raccorde au ventilateur.</li>
 *   <li>Les debits sont portes par les <b>terminaux</b> (troncons sans
 *       enfant) et additionnes vers l'amont.</li>
 *   <li>Le chemin critique est le chemin ventilateur -> terminal de plus forte
 *       perte de charge : c'est la pression que le ventilateur doit fournir au
 *       reseau. Pour chaque autre terminal, l'ecart avec le chemin critique est
 *       la perte de charge a ajouter (registre) pour equilibrer le reseau.</li>
 * </ul>
 */
public final class Network {

    /**
     * @param id             identifiant unique du troncon
     * @param parentId       troncon amont, {@code null} pour la racine
     * @param lengthM        longueur, m
     * @param shape          section ; {@code null} si elle doit etre dimensionnee
     * @param roughnessM     rugosite, m
     * @param fittings       accessoires du troncon
     * @param terminalFlowM3s debit du terminal, m3/s (obligatoire sur un terminal,
     *                       interdit sur un troncon qui a des enfants)
     */
    public record Node(String id, String parentId, double lengthM, DuctShape shape, double roughnessM,
                       List<Fitting> fittings, Double terminalFlowM3s) {
        public Node {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("identifiant de troncon obligatoire");
            }
            Units.requireNonNegative(lengthM, "longueur du troncon " + id);
            Units.requireNonNegative(roughnessM, "rugosite du troncon " + id);
            fittings = fittings == null ? List.of() : List.copyOf(fittings);
        }
    }

    public record NodeResult(String id, String parentId, double flowM3s, DuctShape shape,
                             Sizing.Result sizing, SectionResult section) {
    }

    /**
     * @param terminalId      terminal en bout de chemin
     * @param path            troncons de la racine au terminal
     * @param pressureDropPa  perte de charge cumulee, Pa
     * @param imbalancePa     ecart avec le chemin critique, Pa (0 pour le chemin critique)
     */
    public record PathResult(String terminalId, List<String> path, double pressureDropPa, double imbalancePa) {
    }

    public record Result(double totalFlowM3s, String criticalTerminalId, double criticalPressureDropPa,
                         List<NodeResult> sections, List<PathResult> paths) {
    }

    private Network() {
    }

    /** Calcule un reseau dont toutes les sections sont renseignees. */
    public static Result calculate(List<Node> nodes, Air air) {
        return calculate(nodes, air, null);
    }

    /**
     * Calcule un reseau ; les troncons sans section sont d'abord dimensionnes
     * selon {@code sizing} (obligatoire dans ce cas).
     */
    public static Result calculate(List<Node> nodes, Air air, Sizing.Criteria sizing) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("le reseau doit comporter au moins un troncon");
        }
        Map<String, Node> byId = new LinkedHashMap<>();
        for (Node n : nodes) {
            if (byId.put(n.id(), n) != null) {
                throw new IllegalArgumentException("identifiant de troncon en double : " + n.id());
            }
        }
        Map<String, List<String>> children = new HashMap<>();
        String root = null;
        for (Node n : nodes) {
            if (n.parentId() == null) {
                if (root != null) {
                    throw new IllegalArgumentException("plusieurs troncons racines : " + root + ", " + n.id());
                }
                root = n.id();
            } else {
                if (!byId.containsKey(n.parentId())) {
                    throw new IllegalArgumentException("troncon " + n.id() + " : parent inconnu " + n.parentId());
                }
                children.computeIfAbsent(n.parentId(), k -> new ArrayList<>()).add(n.id());
            }
        }
        if (root == null) {
            throw new IllegalArgumentException("aucun troncon racine (troncon sans parent)");
        }

        // Ordre racine -> feuilles ; un troncon non atteint appartient a un cycle.
        List<String> order = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>(List.of(root));
        Set<String> seen = new HashSet<>();
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            seen.add(id);
            order.add(id);
            queue.addAll(children.getOrDefault(id, List.of()));
        }
        if (seen.size() != byId.size()) {
            List<String> orphan = byId.keySet().stream().filter(id -> !seen.contains(id)).toList();
            throw new IllegalArgumentException("troncons non relies a la racine (cycle) : " + orphan);
        }

        // Debits : feuilles d'abord.
        Map<String, Double> flow = new HashMap<>();
        for (int i = order.size() - 1; i >= 0; i--) {
            Node n = byId.get(order.get(i));
            List<String> kids = children.getOrDefault(n.id(), List.of());
            if (kids.isEmpty()) {
                if (n.terminalFlowM3s() == null) {
                    throw new IllegalArgumentException("terminal " + n.id() + " : debit obligatoire");
                }
                flow.put(n.id(), Units.requirePositive(n.terminalFlowM3s(), "debit du terminal " + n.id()));
            } else {
                if (n.terminalFlowM3s() != null) {
                    throw new IllegalArgumentException("troncon " + n.id()
                            + " : un debit ne se saisit que sur un terminal (il est calcule ici)");
                }
                flow.put(n.id(), kids.stream().mapToDouble(flow::get).sum());
            }
        }

        Map<String, NodeResult> results = new LinkedHashMap<>();
        for (String id : order) {
            Node n = byId.get(id);
            double q = flow.get(id);
            DuctShape shape = n.shape();
            Sizing.Result sized = null;
            if (shape == null) {
                if (sizing == null) {
                    throw new IllegalArgumentException("troncon " + id + " : section absente et aucun critere de dimensionnement");
                }
                sized = Sizing.size(q, n.roughnessM(), sizing, air);
                shape = sized.shape();
            }
            SectionResult section = new Section(q, n.lengthM(), shape, n.roughnessM(), n.fittings()).calculate(air);
            results.put(id, new NodeResult(id, n.parentId(), q, shape, sized, section));
        }

        // Chemins racine -> terminal.
        List<PathResult> raw = new ArrayList<>();
        for (String id : order) {
            if (!children.containsKey(id)) {
                List<String> path = new ArrayList<>();
                double dp = 0;
                for (String cur = id; cur != null; cur = byId.get(cur).parentId()) {
                    path.add(0, cur);
                    dp += results.get(cur).section().totalPressureDropPa();
                }
                raw.add(new PathResult(id, List.copyOf(path), dp, 0));
            }
        }
        PathResult critical = raw.stream()
                .max((a, b) -> Double.compare(a.pressureDropPa(), b.pressureDropPa()))
                .orElseThrow();
        List<PathResult> paths = raw.stream()
                .map(p -> new PathResult(p.terminalId(), p.path(), p.pressureDropPa(),
                        critical.pressureDropPa() - p.pressureDropPa()))
                .toList();

        return new Result(flow.get(root), critical.terminalId(), critical.pressureDropPa(),
                List.copyOf(results.values()), paths);
    }
}
