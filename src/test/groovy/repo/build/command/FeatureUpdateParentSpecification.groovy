package repo.build.command

import repo.build.MavenFeature
import repo.build.RepoBuild
import repo.build.RepoBuildException
import spock.lang.Specification

class FeatureUpdateParentSpecification extends Specification {

    def setup() {
        GroovyMock(MavenFeature, global: true)
    }

    def "without args"() {
        def repoBuild = new RepoBuild('feature-update-parent')

        when:
        repoBuild.execute()

        then:
        thrown(RepoBuildException)
    }

    def "without parent"() {
        def repoBuild = new RepoBuild('feature-update-parent', '-f', 'feature/1')

        when:
        repoBuild.execute()

        then:
        1 * MavenFeature.updateParent(_, 'feature/1', 'parent', false, true, _)
    }

    def "without featureBranch"() {
        def repoBuild = new RepoBuild('feature-update-parent', '-P', 'parent1')

        when:
        repoBuild.execute()

        then:
        thrown(RepoBuildException)
    }

    def "with featureBranch with parent"() {
        def repoBuild = new RepoBuild('feature-update-parent', '-f', 'feature/1', '-P', 'parent1')

        when:
        repoBuild.execute()

        then:
        1 * MavenFeature.updateParent(_, 'feature/1', 'parent1', false, true, _)
    }

    def "with parallel with featureBranch with parent"() {
        def repoBuild = new RepoBuild('feature-update-parent', '-j', '2', '-f', 'feature/1', '-P', 'parent1')

        when:
        repoBuild.execute()

        then:
        1 * MavenFeature.updateParent(_, 'feature/1', 'parent1', false, true, _)
    }

}