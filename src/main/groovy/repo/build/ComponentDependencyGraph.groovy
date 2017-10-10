package repo.build

import groovy.transform.CompileStatic
import org.jgrapht.DirectedGraph
import org.jgrapht.alg.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator
import repo.build.maven.MavenArtifactRef
import repo.build.maven.MavenComponent

/**
 */
@CompileStatic
class ComponentDependencyGraph {
    private final Map<MavenArtifactRef, MavenComponent> componentsMap
    private final DirectedGraph<MavenComponent, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class)
    private final Map<MavenComponent, Set<MavenComponent>> cycleRefs = new HashMap<>()

    ComponentDependencyGraph(Map<MavenArtifactRef, MavenComponent> componentsMap) {
        this.componentsMap = componentsMap
    }

    static ComponentDependencyGraph build(Map<MavenArtifactRef, MavenComponent> componentsMap) {
        ComponentDependencyGraph result = new ComponentDependencyGraph(componentsMap)
        for (MavenComponent c : componentsMap.values()) {
            result.add(c)
        }
        return result
    }

    private void add(MavenComponent component) {
        if (graph.addVertex(component)) {
            // рекурсивно добавляем
            addAllDependencies(component)
        }
    }

    private void addAllDependencies(MavenComponent component) {
        if (component.parent) {
            // add parent ref
            addComponentRef(component, component.parent)
        }
        // for all modules
        for (def m : component.getModules()) {
            // add dependencies refs
            for (def ref : m.getDependencies()) {
                addComponentRef(component, ref)
            }
        }
    }

    private void addComponentRef(MavenComponent component, MavenArtifactRef ref) {
        MavenComponent refComponent = componentsMap.get(ref)
        if (refComponent) {
            add(refComponent)
            DefaultEdge e = graph.addEdge(component, refComponent)
            if (hasCycles()) {
                graph.removeEdge(e)
                if (!cycleRefs.containsKey(component)) {
                    cycleRefs.put(component, new HashSet<MavenComponent>())
                }
                cycleRefs.get(component).add(refComponent)
            }
        }
    }

    boolean hasCycles() {
        CycleDetector<MavenComponent, DefaultEdge> cycleDetector = new CycleDetector<>(graph)
        return cycleDetector.detectCycles()
    }

    List<MavenComponent> sort() {
        TopologicalOrderIterator<MavenComponent, DefaultEdge> i = new TopologicalOrderIterator<>(graph)
        List<MavenComponent> items = new ArrayList<>()
        while (i.hasNext()) {
            items.add(i.next())
        }
        Collections.reverse(items)
        return items
    }

    Map<MavenComponent, Set<MavenComponent>> getCycleRefs() {
        return cycleRefs
    }

}
